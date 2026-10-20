package com.carmod.entity;

import com.carmod.HelicopterFlight;
import com.carmod.MissileLock;
import com.carmod.registry.ModItems;
import com.carmod.registry.ModSounds;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class HelicopterEntity extends CarEntity {
	public static final float MODEL_SCALE = 3.2F;
	public static final float WIDTH = 4.5F;
	public static final float HEIGHT = 4.0F;

	public float rotorSpeed;
	private float rotorAngle;
	private float nose;
	private float nosePrev;
	private double lift;
	private boolean announced;
	private boolean exploded;
	public boolean crashPending;
	public int missileCooldown;

	private static final EntityDataAccessor<Boolean> ARMED = SynchedEntityData.defineId(HelicopterEntity.class, EntityDataSerializers.BOOLEAN);

	public HelicopterEntity(EntityType<? extends HelicopterEntity> type, Level level) {
		super(type, level);
	}

	public boolean armed() {
		return this.entityData.get(ARMED);
	}

	public void setArmed(boolean armed) {
		this.entityData.set(ARMED, armed);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ARMED, false);
	}

	@Override
	public float topSpeed() {
		return HelicopterFlight.TOP_SPEED;
	}

	@Override
	protected float reverseSpeed() {
		return HelicopterFlight.REVERSE_SPEED;
	}

	@Override
	protected float waterDrag() {
		return 1.0F;
	}

	@Override
	protected boolean climbs() {
		return false;
	}

	private boolean powered() {
		return this.getControllingPassenger() != null && this.rotorSpeed >= HelicopterFlight.FLIGHT_RPM;
	}

	@Override
	protected void steer() {
		HelicopterFlight.Step step = HelicopterFlight.tick(
				this.speed,
				this.getYRot(),
				this.lift,
				this.inputForward,
				this.inputBack,
				this.inputLeft,
				this.inputRight,
				this.handbrake,
				this.descend,
				this.powered(),
				this.onGround()
		);
		this.speed = step.speed();
		this.setYRot(step.yaw());
		this.lift = step.vertical();
	}

	@Override
	protected double floatOn(double vertical) {
		if (!this.powered()) {
			return vertical;
		}
		return this.lift;
	}

	@Override
	public void tick() {
		if (this.missileCooldown > 0) {
			this.missileCooldown--;
		}
		super.tick();
		boolean occupied = this.getControllingPassenger() != null;
		float previous = this.rotorSpeed;
		if (occupied && !this.onGround() && Math.abs(this.speed) + Math.abs(this.getDeltaMovement().y) > 0.08F) {
			this.rotorSpeed = Math.max(this.rotorSpeed, HelicopterFlight.FLIGHT_RPM);
		}
		this.rotorSpeed = HelicopterFlight.rpm(this.rotorSpeed, occupied);
		if (!occupied) {
			this.announced = false;
		} else if (!this.announced && previous < HelicopterFlight.FLIGHT_RPM && this.rotorSpeed >= HelicopterFlight.FLIGHT_RPM && this.level().isClientSide() && this.getControllingPassenger() instanceof Player player && player.isLocalPlayer()) {
			this.announced = true;
			player.sendOverlayMessage(Component.translatable("message.carmod.helicopter.ready"));
		} else if (this.rotorSpeed >= HelicopterFlight.FLIGHT_RPM) {
			this.announced = true;
		}
		this.rotorAngle += this.rotorSpeed * this.rotorSpeed * 3.2F;
		this.wheelAngle = this.rotorAngle;
		float target = 0.0F;
		if (this.speed > 0.05F) {
			target = -18.0F * Mth.clamp(this.speed / HelicopterFlight.TOP_SPEED, 0.0F, 1.0F);
		} else if (this.speed < -0.05F) {
			target = 12.0F;
		}
		this.nosePrev = this.nose;
		this.nose += (target - this.nose) * 0.15F;
		this.tiltO = this.nosePrev;
		this.tilt = this.nose;
	}

	@Override
	protected void afterMove(boolean blocked) {
		if (this.exploded || !HelicopterFlight.crashes(this.syncSpeed, this.lift, blocked, this.verticalCollision && this.lift < 0.0, this.verticalCollision && this.lift > 0.0, false)) {
			return;
		}
		if (this.level() instanceof ServerLevel level) {
			this.explode(level);
		} else {
			this.crashPending = true;
		}
	}

	@Override
	protected void onRammed(ServerLevel level) {
		if (HelicopterFlight.crashes(this.speed, this.lift, false, false, false, true)) {
			this.explode(level);
		}
	}

	public void fire(ServerLevel level, int targetId) {
		if (!this.armed() || this.missileCooldown > 0 || this.exploded) {
			return;
		}
		LivingEntity target = MissileLock.find(this);
		if (target == null || target.getId() != targetId) {
			return;
		}
		this.missileCooldown = 16;
		MissileEntity.launch(level, this, target);
	}

	public void explode(ServerLevel level) {
		if (this.exploded) {
			return;
		}
		this.exploded = true;
		double x = this.getX();
		double y = this.getY() + 1.0;
		double z = this.getZ();
		this.ejectPassengers();
		level.explode(this, x, y, z, 4.0F, Level.ExplosionInteraction.TNT);
		this.discard();
	}

	@Override
	protected void positionRider(Entity passenger, MoveFunction callback) {
		double yaw = Math.toRadians(this.getYRot());
		callback.accept(passenger, this.getX() + Math.cos(yaw) * 0.45, this.getY() + 1.2, this.getZ() + Math.sin(yaw) * 0.45);
	}

	@Override
	protected SoundEvent boardSound() {
		return ModSounds.ROTOR_START;
	}

	@Override
	protected float boardVolume() {
		return 1.0F;
	}

	@Override
	public Component getDisplayName() {
		return this.hasCustomName() ? super.getDisplayName() : Component.translatable("entity.carmod.helicopter.killer");
	}

	@Override
	protected Item getDropItem() {
		return this.armed() ? ModItems.ARMED_HELICOPTER : ModItems.HELICOPTER;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Armed", this.armed());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setArmed(input.getBooleanOr("Armed", false));
	}
}
