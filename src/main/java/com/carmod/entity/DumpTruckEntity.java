package com.carmod.entity;

import java.util.List;

import com.carmod.DumpCargo;
import com.carmod.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class DumpTruckEntity extends CarEntity {
	public static final float WIDTH = 2.8F;
	public static final float HEIGHT = 2.6F;
	public static final float TOP_SPEED = 0.8F;

	private static final EntityDataAccessor<Integer> DUMP = SynchedEntityData.defineId(DumpTruckEntity.class, EntityDataSerializers.INT);

	private final SimpleContainer cargo = new SimpleContainer(DumpCargo.SLOTS);
	public float bedLift;
	public float bedLiftO;
	private int dumpCooldown;
	private int pickupSound;
	private int fullNotice;

	public DumpTruckEntity(EntityType<? extends DumpTruckEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public float topSpeed() {
		return TOP_SPEED;
	}

	@Override
	protected float reverseSpeed() {
		return 0.22F;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DUMP, 0);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.dumpCooldown > 0) {
			this.dumpCooldown--;
		}
		if (this.pickupSound > 0) {
			this.pickupSound--;
		}
		if (this.fullNotice > 0) {
			this.fullNotice--;
		}
		if (!this.level().isClientSide() && this.entityData.get(DUMP) > 0) {
			this.entityData.set(DUMP, this.entityData.get(DUMP) - 1);
		}
		this.bedLiftO = this.bedLift;
		float target = this.entityData.get(DUMP) > 0 ? 1.0F : 0.0F;
		this.bedLift += (target - this.bedLift) * 0.45F;
		if (!this.level().isClientSide() && Math.abs(this.speed) > 0.05F) {
			this.collect((ServerLevel) this.level());
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
				this.getX() + leftX * 0.3 + forwardX * 0.35,
				this.getY() + 0.72,
				this.getZ() + leftZ * 0.3 + forwardZ * 0.35
		);
	}

	/** Tips the bed. A container behind the tailgate is filled first; the rest lands in a pile. */
	public void dump(ServerLevel level, ServerPlayer player) {
		if (this.dumpCooldown > 0) {
			return;
		}
		this.dumpCooldown = 8;
		if (this.cargo.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.carmod.dump.empty"));
			return;
		}
		List<ItemStack> spilled = DumpCargo.pour(this.cargo, this.findBin(level));
		double yaw = Math.toRadians(this.getYRot());
		double backX = Math.sin(yaw);
		double backZ = -Math.cos(yaw);
		double x = this.getX() + backX * 2.6;
		double y = this.getY() + 0.8;
		double z = this.getZ() + backZ * 2.6;
		for (ItemStack stack : spilled) {
			ItemEntity item = new ItemEntity(level, x, y, z, stack, backX * 0.12, 0.22, backZ * 0.12);
			item.setPickUpDelay(40);
			level.addFreshEntity(item);
		}
		this.entityData.set(DUMP, 16);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GRAVEL_BREAK, SoundSource.NEUTRAL, 0.7F, 0.85F);
		player.sendOverlayMessage(Component.translatable("message.carmod.dump.done"));
	}

	private void collect(ServerLevel level) {
		boolean picked = false;
		boolean blocked = false;
		for (ItemEntity item : level.getEntities(EntityTypes.ITEM, this.scoop(), entity -> !entity.hasPickUpDelay())) {
			ItemStack stack = item.getItem();
			int before = stack.getCount();
			if (before <= 0) {
				continue;
			}
			ItemStack left = this.cargo.addItem(stack);
			if (left.isEmpty()) {
				item.discard();
				picked = true;
			} else if (left.getCount() != before) {
				item.setItem(left);
				picked = true;
			} else {
				blocked = true;
			}
		}
		if (picked && this.pickupSound == 0) {
			level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.4F, 0.9F);
			this.pickupSound = 5;
		} else if (blocked && !picked && this.fullNotice == 0 && this.getControllingPassenger() instanceof ServerPlayer player) {
			player.sendOverlayMessage(Component.translatable("message.carmod.dump.full"));
			this.fullNotice = 40;
		}
	}

	private AABB scoop() {
		return this.getBoundingBox().inflate(16.0, 8.0, 16.0);
	}

	private Container findBin(ServerLevel level) {
		double yaw = Math.toRadians(this.getYRot());
		double backX = Math.sin(yaw);
		double backZ = -Math.cos(yaw);
		double sideX = Math.cos(yaw);
		double sideZ = Math.sin(yaw);
		for (double along : new double[] { 2.0, 2.7, 3.4 }) {
			for (double side : new double[] { -0.8, 0.0, 0.8 }) {
				double x = this.getX() + backX * along + sideX * side;
				double z = this.getZ() + backZ * along + sideZ * side;
				for (int up = 0; up <= 1; up++) {
					Container bin = this.containerAt(level, BlockPos.containing(x, this.getY() + up, z));
					if (bin != null) {
						return bin;
					}
				}
			}
		}
		return null;
	}

	private Container containerAt(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof ChestBlock chest) {
			return ChestBlock.getContainer(chest, state, level, pos, true);
		}
		BlockEntity blockEntity = level.getBlockEntity(pos);
		return blockEntity instanceof Container container ? container : null;
	}

	private void dropCargo(ServerLevel level) {
		if (!level.getGameRules().get(GameRules.ENTITY_DROPS)) {
			return;
		}
		for (ItemStack stack : DumpCargo.pour(this.cargo, null)) {
			this.spawnAtLocation(level, stack);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (this.isRemoved() || !(source.getEntity() instanceof Player player) || player.isSpectator()) {
			return false;
		}
		this.dropCargo(level);
		return super.hurtServer(level, source, damage);
	}

	@Override
	public Component getDisplayName() {
		return this.hasCustomName() ? super.getDisplayName() : Component.translatable("entity.carmod.dump_truck.killer");
	}

	@Override
	protected Item getDropItem() {
		return ModItems.DUMP_TRUCK;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		this.cargo.storeAsItemList(output.list("Cargo", ItemStack.OPTIONAL_CODEC));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.cargo.fromItemList(input.listOrEmpty("Cargo", ItemStack.OPTIONAL_CODEC));
	}
}
