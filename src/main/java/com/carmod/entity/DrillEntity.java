package com.carmod.entity;

import com.carmod.DrillPath;
import com.carmod.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DrillEntity extends CarEntity {
	public static final float WIDTH = 2.4F;
	public static final float HEIGHT = 2.4F;
	public static final float TOP_SPEED = 0.55F;
	private static final int[] ANGLES = { 0, 15, 30, 45, 90 };

	private static final EntityDataAccessor<Integer> DRILL = SynchedEntityData.defineId(DrillEntity.class, EntityDataSerializers.INT);

	public float bit;
	public float bitO;
	private int boreSound;
	private double boreY;

	public DrillEntity(EntityType<? extends DrillEntity> type, Level level) {
		super(type, level);
	}

	public static boolean allowed(int angle) {
		for (int value : ANGLES) {
			if (value == angle) {
				return true;
			}
		}
		return false;
	}

	@Override
	public float topSpeed() {
		return TOP_SPEED;
	}

	@Override
	protected float reverseSpeed() {
		return 0.2F;
	}

	@Override
	protected boolean climbs() {
		return this.angle() == 0;
	}

	public int angle() {
		return this.entityData.get(DRILL);
	}

	public void setAngle(int angle) {
		this.entityData.set(DRILL, allowed(angle) ? angle : 0);
	}

	public void cycle() {
		int current = this.angle();
		int next = ANGLES[0];
		for (int i = 0; i < ANGLES.length; i++) {
			if (ANGLES[i] == current) {
				next = ANGLES[(i + 1) % ANGLES.length];
				break;
			}
		}
		this.setAngle(next);
	}

	public String angleKey() {
		return switch (this.angle()) {
			case 15 -> "message.carmod.drill.15";
			case 30 -> "message.carmod.drill.30";
			case 45 -> "message.carmod.drill.45";
			case 90 -> "message.carmod.drill.90";
			default -> "message.carmod.drill.off";
		};
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DRILL, 0);
	}

	@Override
	protected void drive(double vertical) {
		if (this.angle() == 0 || Math.abs(this.speed) <= 0.02F) {
			super.drive(vertical);
			return;
		}
		double[] dir = DrillPath.direction(this.getYRot(), this.angle(), Math.signum(this.speed));
		float along = Math.abs(this.speed);
		this.setDeltaMovement(dir[0] * along, dir[1] * along, dir[2] * along);
	}

	@Override
	public void tick() {
		this.boreY = this.getY();
		super.tick();
		if (this.boreSound > 0) {
			this.boreSound--;
		}
		if (this.angle() != 0) {
			this.wheelAngle += Math.abs(this.speed) * 35.0F;
			if (this.verticalCollision && this.speed > 0.04F && this.getY() >= this.boreY - 0.02) {
				this.speed *= 0.5F;
				this.syncSpeed = this.speed;
			}
		}
		this.bitO = this.bit;
		this.bit += (this.angle() - this.bit) * 0.35F;
	}

	@Override
	protected void preparePath() {
		if (this.angle() == 0 || (Math.abs(this.speed) <= 0.02F && !this.isPushing())) {
			return;
		}
		float travel = Math.abs(this.speed) > 0.02F ? this.speed : 0.02F;
		if (this.level().isClientSide()) {
			this.predict(travel);
			return;
		}
		if (this.level() instanceof ServerLevel level) {
			this.bore(level, this.getX(), this.getY(), this.getZ(), this.getYRot(), travel);
		}
	}

	/** Clears the bore at the position from before this tick, matching the driving client. */
	public void boreDriven(ServerLevel level, double x, double y, double z, float yaw, float speed, boolean pushing) {
		if (this.angle() == 0) {
			return;
		}
		float travel = Math.abs(speed) > 0.02F ? speed : (pushing ? 0.02F : 0.0F);
		if (travel == 0.0F) {
			return;
		}
		double[] at = DrillPath.rewind(x, y, z, yaw, this.angle(), speed);
		this.bore(level, at[0], at[1], at[2], yaw, travel);
	}

	private void predict(float travel) {
		BlockState air = Blocks.AIR.defaultBlockState();
		for (BlockPos pos : DrillPath.bore(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.angle(), travel)) {
			if (DrillPath.canBore(this.level().getBlockState(pos), this.level(), pos)) {
				this.level().setBlock(pos, air, Block.UPDATE_ALL);
			}
		}
	}

	private void bore(ServerLevel level, double x, double y, double z, float yaw, float travel) {
		boolean broke = false;
		boolean drop = level.getGameRules().get(GameRules.BLOCK_DROPS);
		ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
		for (BlockPos pos : DrillPath.bore(x, y, z, yaw, this.angle(), travel)) {
			BlockState state = level.getBlockState(pos);
			if (!DrillPath.canBore(state, level, pos)) {
				continue;
			}
			if (drop) {
				Block.dropResources(state, level, pos, level.getBlockEntity(pos), this, tool);
			}
			broke |= level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
		if (broke && this.boreSound == 0) {
			level.playSound(null, x, y, z, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.6F, 0.7F);
			this.boreSound = 4;
		}
	}

	@Override
	protected void positionRider(Entity passenger, MoveFunction callback) {
		double yaw = Math.toRadians(this.getYRot());
		double leftX = Math.cos(yaw);
		double leftZ = Math.sin(yaw);
		double forwardX = -Math.sin(yaw);
		double forwardZ = Math.cos(yaw);
		callback.accept(
				passenger,
				this.getX() + leftX * 0.15 - forwardX * 0.35,
				this.getY() + 0.5,
				this.getZ() + leftZ * 0.15 - forwardZ * 0.35
		);
	}

	@Override
	public Component getDisplayName() {
		return this.hasCustomName() ? super.getDisplayName() : Component.translatable("entity.carmod.drill.killer");
	}

	@Override
	protected Item getDropItem() {
		return ModItems.DRILL;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Drill", this.angle());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setAngle(input.getIntOr("Drill", 0));
	}
}
