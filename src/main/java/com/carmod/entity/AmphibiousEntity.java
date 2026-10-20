package com.carmod.entity;

import com.carmod.AmphibiousFloat;
import com.carmod.registry.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class AmphibiousEntity extends CarEntity {
	public static final float WIDTH = 2.6F;
	public static final float HEIGHT = 2.3F;
	public static final float TOP_SPEED = 1.0F;

	public AmphibiousEntity(EntityType<? extends AmphibiousEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public float topSpeed() {
		return TOP_SPEED;
	}

	@Override
	protected float reverseSpeed() {
		return 0.25F;
	}

	@Override
	protected float waterDrag() {
		return 1.0F;
	}

	@Override
	protected double floatOn(double vertical) {
		return AmphibiousFloat.vertical(this.getFluidHeight(FluidTags.WATER), this.onGround(), vertical);
	}

	@Override
	public float maxUpStep() {
		return this.isInWater() ? 2.0F : super.maxUpStep();
	}

	@Override
	protected void beforeMove() {
		// Step-up only runs for something standing on a block. A floating hull never qualifies, so the shore stops it.
		if (this.isInWater() && this.speed > 0.02F) {
			this.setOnGround(true);
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
				this.getX() + leftX * 0.3 - forwardX * 0.15,
				this.getY() + 0.62,
				this.getZ() + leftZ * 0.3 - forwardZ * 0.15
		);
	}

	@Override
	public Component getDisplayName() {
		return this.hasCustomName() ? super.getDisplayName() : Component.translatable("entity.carmod.amphibious.killer");
	}

	@Override
	protected Item getDropItem() {
		return ModItems.AMPHIBIOUS;
	}
}
