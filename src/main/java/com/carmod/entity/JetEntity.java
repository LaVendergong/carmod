package com.carmod.entity;

import com.carmod.JetFlight;
import com.carmod.MissileLock;
import com.carmod.registry.ModItems;
import com.carmod.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class JetEntity extends CarEntity {
	public static final float MODEL_SCALE = 2.8F;
	public static final float WIDTH = 3.0F;
	public static final float HEIGHT = 2.0F;

	public float throttle;
	public float pitch;
	public float roll;
	public float rollO;
	public boolean crashPending;
	public int missileCooldown;
	public int cannonCooldown;

	private double flightDrop;
	private boolean exploded;
	private boolean rotateHint;
	private boolean stallHint;

	private static final EntityDataAccessor<Float> SYNC_PITCH = SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> SYNC_ROLL = SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> SYNC_THROTTLE = SynchedEntityData.defineId(JetEntity.class, EntityDataSerializers.FLOAT);

	public JetEntity(EntityType<? extends JetEntity> type, Level level) {
		super(type, level);
	}

	public void syncAttitude(float pitch, float roll, float throttle) {
		this.entityData.set(SYNC_PITCH, pitch);
		this.entityData.set(SYNC_ROLL, roll);
		this.entityData.set(SYNC_THROTTLE, throttle);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SYNC_PITCH, 0.0F);
		builder.define(SYNC_ROLL, 0.0F);
		builder.define(SYNC_THROTTLE, 0.0F);
	}

	@Override
	public float topSpeed() {
		return JetFlight.DIVE_MAX;
	}

	@Override
	protected float reverseSpeed() {
		return 0.0F;
	}

	@Override
	protected float waterDrag() {
		return 1.0F;
	}

	@Override
	protected boolean climbs() {
		return false;
	}

	@Override
	public boolean isFlyingVehicle() {
		return !this.onGround();
	}

	@Override
	public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float damage) {
		if (source.getEntity() == this.getControllingPassenger()) {
			return false;
		}
		return super.hurtServer(level, source, damage);
	}

	private boolean localPilot() {
		return this.getControllingPassenger() instanceof Player player && player.isLocalPlayer();
	}

	/** A one-tick hop off the runway is still the ground. Treating it as flight stalls the nose and shakes the view. */
	private boolean wheelsDown() {
		if (this.onGround()) {
			return true;
		}
		if (Math.abs(this.roll) > 25.0F || this.pitch > 24.0F || this.pitch < -8.0F) {
			return false;
		}
		BlockPos feet = BlockPos.containing(this.getX(), this.getY() - 0.001, this.getZ());
		var ground = feet.below();
		return this.getY() <= ground.getY() + 1.35 && this.level().getBlockState(ground).blocksMotion();
	}

	@Override
	protected void steer() {
		JetFlight.State state = JetFlight.tick(
				new JetFlight.State(this.throttle, this.speed, this.getYRot(), this.pitch, this.roll),
				this.inputForward,
				this.inputBack,
				this.inputLeft,
				this.inputRight,
				this.handbrake,
				this.descend,
				this.wheelsDown()
		);
		this.throttle = state.throttle();
		this.speed = state.speed();
		this.setYRot(state.yaw());
		this.pitch = state.pitch();
		this.roll = state.roll();
	}

	@Override
	protected void drive(double vertical) {
		double yaw = Math.toRadians(this.getYRot());
		double pitch = Math.toRadians(this.pitch);
		double horizontal = this.speed * Math.cos(pitch);
		this.setDeltaMovement(-Math.sin(yaw) * horizontal, vertical, Math.cos(yaw) * horizontal);
	}

	@Override
	protected double floatOn(double vertical) {
		if (this.getControllingPassenger() == null) {
			this.flightDrop = vertical;
			return vertical;
		}
		this.flightDrop = JetFlight.vertical(this.speed, this.pitch, this.wheelsDown());
		return this.flightDrop;
	}

	@Override
	public void tick() {
		this.rollO = this.roll;
		if (this.missileCooldown > 0) {
			this.missileCooldown--;
		}
		if (this.cannonCooldown > 0) {
			this.cannonCooldown--;
		}
		if (!this.localPilot()) {
			this.pitch = this.entityData.get(SYNC_PITCH);
			this.roll = this.entityData.get(SYNC_ROLL);
			this.throttle = this.entityData.get(SYNC_THROTTLE);
		}
		if (this.getControllingPassenger() == null) {
			this.pitch *= 0.92F;
			this.roll *= 0.9F;
			this.throttle *= 0.96F;
		}
		super.tick();
		this.tilt = this.pitch;
		if (this.level().isClientSide()) {
			this.stickToRunway();
		}
		this.hint();
		this.burn();
		this.resetFallDistance();
		Entity passenger = this.getFirstPassenger();
		if (passenger != null) {
			passenger.resetFallDistance();
		}
	}

	private void hint() {
		if (!this.level().isClientSide() || !(this.getControllingPassenger() instanceof Player player) || !player.isLocalPlayer()) {
			return;
		}
		if (this.wheelsDown() && this.speed >= JetFlight.ROTATE && this.pitch < 4.0F) {
			if (!this.rotateHint) {
				this.rotateHint = true;
				player.sendOverlayMessage(Component.translatable("message.carmod.jet.rotate"));
			}
		} else if (!this.wheelsDown()) {
			this.rotateHint = false;
		}
		boolean stall = JetFlight.stalled(this.speed, this.wheelsDown());
		if (stall && !this.stallHint) {
			this.stallHint = true;
			player.sendOverlayMessage(Component.translatable("message.carmod.jet.stall"));
		} else if (!stall) {
			this.stallHint = false;
		}
	}

	private void burn() {
		if (!this.level().isClientSide() || this.throttle < 0.15F) {
			return;
		}
		Vec3 tail = this.forward().scale(-3.4);
		double x = this.getX() + tail.x;
		double y = this.getY() + 0.45 + tail.y;
		double z = this.getZ() + tail.z;
		this.level().addParticle(ParticleTypes.FLAME, x, y, z, tail.x * 0.08, tail.y * 0.08, tail.z * 0.08);
		if (this.throttle > 0.65F) {
			this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, tail.x * 0.15, tail.y * 0.15, tail.z * 0.15);
		}
	}

	public Vec3 forward() {
		double yaw = Math.toRadians(this.getYRot());
		double pitch = Math.toRadians(this.pitch);
		double flat = Math.cos(pitch);
		return new Vec3(-Math.sin(yaw) * flat, Math.sin(pitch), Math.cos(yaw) * flat);
	}

	public void fire(ServerLevel level, int targetId) {
		if (this.missileCooldown > 0 || this.exploded) {
			return;
		}
		LivingEntity target = MissileLock.find(this, 72.0);
		if (target == null || target.getId() != targetId) {
			return;
		}
		this.missileCooldown = 16;
		Vec3 aim = this.forward();
		MissileEntity.launch(level, this.position().add(aim.scale(4.0)).add(0.0, 0.6, 0.0), aim.scale(2.1), target.getId());
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 1.4F, 0.8F);
	}

	public void cannon(ServerLevel level) {
		if (this.cannonCooldown > 0 || this.exploded) {
			return;
		}
		this.cannonCooldown = 4;
		Vec3 aim = this.forward();
		Vec3 from = this.position().add(0.0, 0.7, 0.0).add(aim.scale(3.5));
		Vec3 to = from.add(aim.scale(56.0));
		BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		Vec3 end = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
		for (LivingEntity living : level.getEntities(EntityTypeTest.forClass(LivingEntity.class), new net.minecraft.world.phys.AABB(from, end).inflate(1.1), candidate -> candidate.isAlive() && candidate.getRootVehicle() != this)) {
			Vec3 body = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0);
			if (distanceToSegment(body, from, end) < 1.35) {
				living.hurtServer(level, level.damageSources().explosion(this, this.getControllingPassenger()), 12.0F);
			}
		}
		if (hit.getType() != HitResult.Type.MISS) {
			level.explode(this, end.x, end.y, end.z, 1.0F, Level.ExplosionInteraction.TNT);
		}
		level.playSound(null, from.x, from.y, from.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.NEUTRAL, 0.7F, 1.5F);
	}

	private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
		Vec3 span = end.subtract(start);
		double length = span.lengthSqr();
		if (length < 1.0E-4) {
			return point.distanceTo(start);
		}
		double along = Mth.clamp(point.subtract(start).dot(span) / length, 0.0, 1.0);
		return point.distanceTo(start.add(span.scale(along)));
	}

	/** Collision at speed pops the hitbox off the runway for a tick. Put it back so the camera stays level. */
	private void stickToRunway() {
		if (this.getControllingPassenger() == null || this.flightDrop > 0.02 || Math.abs(this.pitch) > 8.0F) {
			return;
		}
		int x = Mth.floor(this.getX());
		int z = Mth.floor(this.getZ());
		int top = Mth.floor(this.getY() + 0.5);
		Double surface = null;
		for (int y = top + 1; y >= top - 2; y--) {
			BlockPos pos = new BlockPos(x, y, z);
			if (this.level().getBlockState(pos).blocksMotion() && !this.level().getBlockState(pos.above()).blocksMotion()) {
				surface = y + 1.0;
				break;
			}
		}
		if (surface == null || this.getY() < surface - 0.2 || this.getY() > surface + 0.6) {
			return;
		}
		this.setPos(this.getX(), surface, this.getZ());
		this.yo = surface;
		Vec3 motion = this.getDeltaMovement();
		if (motion.y != 0.0) {
			this.setDeltaMovement(motion.x, 0.0, motion.z);
		}
		this.setOnGround(true);
	}

	@Override
	protected void afterMove(boolean blocked) {
		if (this.exploded) {
			return;
		}
		boolean ground = this.verticalCollision && this.flightDrop < 0.0;
		boolean ceiling = !this.onGround() && this.verticalCollision && this.flightDrop > 0.0;
		if (!JetFlight.crashes(this.speed, this.flightDrop, blocked, ground, ceiling, this.isInWater())) {
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
		if (this.speed >= 0.8F) {
			this.explode(level);
		}
	}

	public void explode(ServerLevel level) {
		if (this.exploded) {
			return;
		}
		this.exploded = true;
		double x = this.getX();
		double y = this.getY() + 0.5;
		double z = this.getZ();
		this.ejectPassengers();
		level.explode(this, x, y, z, 4.0F, Level.ExplosionInteraction.TNT);
		this.discard();
	}

	@Override
	public void alignPassenger(Entity passenger) {
		float yaw = this.getYRot();
		passenger.setYRot(yaw);
		passenger.yRotO = this.yRotO;
		passenger.setXRot(-this.pitch);
		passenger.xRotO = this.wheelsDown() ? -this.pitch : -this.tiltO;
		if (passenger instanceof LivingEntity living) {
			living.setYHeadRot(yaw);
			living.yHeadRotO = this.yRotO;
			living.setYBodyRot(yaw);
			living.yBodyRotO = this.yRotO;
		}
	}

	@Override
	protected void positionRider(Entity passenger, MoveFunction callback) {
		Vec3 nose = this.forward();
		Vec3 wing = nose.cross(new Vec3(0.0, 1.0, 0.0));
		if (wing.lengthSqr() < 1.0E-4) {
			wing = new Vec3(1.0, 0.0, 0.0);
		}
		wing = wing.normalize();
		Vec3 up = wing.cross(nose).normalize();
		up = rotateAround(up, nose, this.roll);
		Vec3 seat = nose.scale(0.35).add(up.scale(0.7));
		callback.accept(passenger, this.getX() + seat.x, this.getY() + seat.y, this.getZ() + seat.z);
	}

	private static Vec3 rotateAround(Vec3 vector, Vec3 axis, float degrees) {
		double radians = Math.toRadians(degrees);
		double cos = Math.cos(radians);
		double sin = Math.sin(radians);
		Vec3 unit = axis.normalize();
		return vector.scale(cos).add(unit.cross(vector).scale(sin)).add(unit.scale(unit.dot(vector) * (1.0 - cos)));
	}

	@Override
	protected SoundEvent boardSound() {
		return ModSounds.JET;
	}

	@Override
	protected float boardVolume() {
		return 1.0F;
	}

	@Override
	public Component getDisplayName() {
		return this.hasCustomName() ? super.getDisplayName() : Component.translatable("entity.carmod.jet.killer");
	}

	@Override
	protected Item getDropItem() {
		return ModItems.JET;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putFloat("Throttle", this.throttle);
		output.putFloat("Pitch", this.pitch);
		output.putFloat("Roll", this.roll);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.throttle = input.getFloatOr("Throttle", 0.0F);
		this.pitch = input.getFloatOr("Pitch", 0.0F);
		this.roll = input.getFloatOr("Roll", 0.0F);
		this.syncAttitude(this.pitch, this.roll, this.throttle);
	}
}
