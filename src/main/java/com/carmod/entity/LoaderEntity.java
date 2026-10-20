package com.carmod.entity;

import com.carmod.LoaderGrade;
import com.carmod.registry.ModItems;

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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class LoaderEntity extends CarEntity {
	public static final float SCALE = 1.5F;
	public static final float WIDTH = 2.6F * SCALE;
	public static final float HEIGHT = 2.5F * SCALE;
	public static final float TOP_SPEED = 0.7F;

	private static final EntityDataAccessor<Boolean> BUCKET = SynchedEntityData.defineId(LoaderEntity.class, EntityDataSerializers.BOOLEAN);

	private int scrapeSound;

	public LoaderEntity(EntityType<? extends LoaderEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public float topSpeed() {
		return TOP_SPEED;
	}

	@Override
	protected boolean climbs() {
		return !this.bucketDown();
	}

	@Override
	protected float reverseSpeed() {
		return 0.18F;
	}

	@Override
	protected void positionRider(Entity passenger, MoveFunction callback) {
		double yaw = Math.toRadians(this.getYRot());
		double leftX = Math.cos(yaw);
		double leftZ = Math.sin(yaw);
		double forwardX = -Math.sin(yaw);
		double forwardZ = Math.cos(yaw);
		double seatSide = 0.2;
		double seatBack = 0.15;
		callback.accept(
				passenger,
				this.getX() + leftX * seatSide - forwardX * seatBack,
				this.getY() + 1.25,
				this.getZ() + leftZ * seatSide - forwardZ * seatBack
		);
	}

	public boolean bucketDown() {
		return this.entityData.get(BUCKET);
	}

	public void setBucketDown(boolean down) {
		this.entityData.set(BUCKET, down);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(BUCKET, false);
	}

	@Override
	public void tick() {
		if (this.scrapeSound > 0) {
			this.scrapeSound--;
		}
		super.tick();
	}

	@Override
	protected void preparePath() {
		if (!this.bucketDown() || (this.speed <= 0.02F && !this.isPushing())) {
			return;
		}
		if (this.level().isClientSide()) {
			this.predict();
			return;
		}
		if (this.level() instanceof ServerLevel level) {
			this.grade(level, this.getX(), this.getY(), this.getZ(), this.getYRot());
		}
	}

	/** Clears the blade's box at the position from before this tick, matching the driving client. */
	public void gradeDriven(ServerLevel level, double x, double y, double z, float yaw, float speed) {
		this.grade(level, LoaderGrade.originX(x, yaw, speed), y, LoaderGrade.originZ(z, yaw, speed), yaw);
	}

	private void predict() {
		BlockState air = Blocks.AIR.defaultBlockState();
		for (BlockPos pos : LoaderGrade.targets(this.getX(), this.getY(), this.getZ(), this.getYRot())) {
			if (LoaderGrade.canScrape(this.level().getBlockState(pos), this.level(), pos)) {
				this.level().setBlock(pos, air, Block.UPDATE_ALL);
			}
		}
	}

	private void grade(ServerLevel level, double x, double y, double z, float yaw) {
		boolean broke = false;
		boolean drop = level.getGameRules().get(GameRules.BLOCK_DROPS);
		ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
		for (BlockPos pos : LoaderGrade.targets(x, y, z, yaw)) {
			BlockState state = level.getBlockState(pos);
			if (!LoaderGrade.canScrape(state, level, pos)) {
				continue;
			}
			if (drop) {
				Block.dropResources(state, level, pos, level.getBlockEntity(pos), this, tool);
			}
			broke |= level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
		if (broke && this.scrapeSound == 0) {
			level.playSound(null, x, y, z, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 0.8F, 1.0F);
			this.scrapeSound = 5;
		}
	}

	@Override
	public Component getDisplayName() {
		return this.hasCustomName() ? super.getDisplayName() : Component.translatable("entity.carmod.loader.killer");
	}

	@Override
	protected Item getDropItem() {
		return ModItems.LOADER;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Bucket", this.bucketDown());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setBucketDown(input.getBooleanOr("Bucket", false));
	}
}
