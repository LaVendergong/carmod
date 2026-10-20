package com.carmod.entity;

import com.carmod.RaceDrift;
import com.carmod.registry.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class RaceEntity extends CarEntity {
	public static final float WIDTH = 2.4F;
	public static final float HEIGHT = 1.8F;

	public float travelYaw;
	public float wheelSteer;
	public float wheelSteerO;
	private boolean travelReady;

	public RaceEntity(EntityType<? extends RaceEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public float topSpeed() {
		return RaceDrift.TOP_SPEED;
	}

	@Override
	protected float reverseSpeed() {
		return RaceDrift.REVERSE_SPEED;
	}

	@Override
	protected void steer() {
		if (!this.travelReady) {
			this.travelYaw = this.getYRot();
			this.travelReady = true;
		}
		this.wheelSteerO = this.wheelSteer;
		RaceDrift.Tick tick = RaceDrift.tick(this.speed, this.getYRot(), this.travelYaw, this.inputForward, this.inputBack, this.inputLeft, this.inputRight, this.handbrake);
		this.speed = tick.speed();
		this.setYRot(tick.nose());
		this.travelYaw = tick.travel();
		float want = (this.inputLeft ? 0.5F : 0.0F) - (this.inputRight ? 0.5F : 0.0F);
		if (tick.drifting()) {
			want *= 1.4F;
		}
		this.wheelSteer += (want - this.wheelSteer) * 0.4F;
	}

	@Override
	protected void drive(double vertical) {
		boolean localDriver = this.getControllingPassenger() instanceof Player player && player.isLocalPlayer();
		float yaw = localDriver ? this.travelYaw : this.getYRot();
		double radians = Math.toRadians(yaw);
		this.setDeltaMovement(-Math.sin(radians) * this.speed, vertical, Math.cos(radians) * this.speed);
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
				this.getX() + leftX * 0.28 - forwardX * 0.1,
				this.getY() + 0.32,
				this.getZ() + leftZ * 0.28 - forwardZ * 0.1
		);
	}

	@Override
	public Component getDisplayName() {
		return this.hasCustomName() ? super.getDisplayName() : Component.translatable("entity.carmod.racer.killer");
	}

	@Override
	protected Item getDropItem() {
		return ModItems.RACER;
	}
}
