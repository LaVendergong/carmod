package com.carmod.entity;

import com.carmod.registry.ModEntities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class MissileEntity extends Entity {
	private static final double SPEED = 1.5;
	private static final int ARM_TICKS = 6;
	private static final int MAX_LIFE = 200;

	private int targetId = -1;
	private int life;
	private boolean detonated;

	public MissileEntity(EntityType<? extends MissileEntity> type, Level level) {
		super(type, level);
	}

	public static void launch(ServerLevel level, Vec3 pos, Vec3 velocity, int targetId) {
		MissileEntity missile = new MissileEntity(ModEntities.MISSILE, level);
		missile.setPos(pos.x, pos.y, pos.z);
		missile.setDeltaMovement(velocity);
		missile.targetId = targetId;
		missile.point(velocity);
		level.addFreshEntity(missile);
	}

	public static void launch(ServerLevel level, HelicopterEntity helicopter, LivingEntity target) {
		double yaw = Math.toRadians(helicopter.getYRot());
		double facingX = -Math.sin(yaw);
		double facingZ = Math.cos(yaw);
		launch(level, new Vec3(helicopter.getX() + facingX * 5.0, helicopter.getY() + 0.6, helicopter.getZ() + facingZ * 5.0), new Vec3(facingX * 0.9, -0.35, facingZ * 0.9), target.getId());
		level.playSound(null, helicopter.getX(), helicopter.getY(), helicopter.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 1.2F, 0.65F);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide() || this.detonated) {
			return;
		}
		this.life++;
		Vec3 velocity = this.getDeltaMovement();
		Entity target = this.level().getEntity(this.targetId);
		if (target instanceof LivingEntity living && living.isAlive()) {
			Vec3 aim = living.position().add(0.0, living.getBbHeight() * 0.4, 0.0).subtract(this.position());
			if (aim.lengthSqr() < 2.56) {
				this.detonate();
				return;
			}
			Vec3 desired = aim.normalize().scale(SPEED);
			velocity = velocity.scale(0.55).add(desired.scale(0.45));
			if (velocity.lengthSqr() > SPEED * SPEED) {
				velocity = velocity.normalize().scale(SPEED);
			}
		} else {
			velocity = velocity.add(0.0, -0.04, 0.0);
		}
		this.setDeltaMovement(velocity);
		this.point(velocity);
		this.move(MoverType.SELF, velocity);
		if (this.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 2, 0.0, 0.0, 0.0, 0.01);
			level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		}
		if (this.life > ARM_TICKS && (this.horizontalCollision || this.verticalCollision)) {
			this.detonate();
		} else if (this.life > MAX_LIFE) {
			this.discard();
		}
	}

	private void point(Vec3 velocity) {
		double horizontal = Math.hypot(velocity.x, velocity.z);
		this.setYRot((float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z)));
		this.setXRot((float) Math.toDegrees(Math.atan2(velocity.y, horizontal)));
	}

	private void detonate() {
		if (this.detonated || !(this.level() instanceof ServerLevel level)) {
			return;
		}
		this.detonated = true;
		level.explode(this, this.getX(), this.getY(), this.getZ(), 3.0F, Level.ExplosionInteraction.TNT);
		this.discard();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.targetId = input.getIntOr("Target", -1);
		this.life = input.getIntOr("Life", 0);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putInt("Target", this.targetId);
		output.putInt("Life", this.life);
	}
}
