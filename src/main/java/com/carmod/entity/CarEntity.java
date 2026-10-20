package com.carmod.entity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.carmod.CarClientHooks;
import com.carmod.CarMotion;
import com.carmod.ModConfig;
import com.carmod.registry.ModItems;
import com.carmod.registry.ModSounds;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class CarEntity extends VehicleEntity {
	public static final ResourceKey<DamageType> RAM_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, com.carmod.CarMod.id("ram"));

	public float speed;
	public float syncSpeed;
	public boolean rammedTree;
	public int wear;
	public float wheelAngle;
	public float tilt;
	public float tiltO;
	private float alignedCarYaw = Float.NaN;
	protected boolean inputLeft;
	protected boolean inputRight;
	protected boolean inputForward;
	protected boolean inputBack;
	protected boolean handbrake;
	protected boolean descend;
	private int impactCooldown;

	public CarEntity(EntityType<? extends CarEntity> type, Level level) {
		super(type, level);
		this.blocksBuilding = true;
	}

	public void setInput(boolean left, boolean right, boolean forward, boolean back) {
		this.inputLeft = left;
		this.inputRight = right;
		this.inputForward = forward;
		this.inputBack = back;
	}

	public void setHandbrake(boolean handbrake) {
		this.handbrake = handbrake;
	}

	public void setDescend(boolean descend) {
		this.descend = descend;
	}

	public boolean isPushing() {
		return this.inputForward || this.inputLeft || this.inputRight;
	}

	public float topSpeed() {
		return CarMotion.MAX_SPEED;
	}

	protected float reverseSpeed() {
		return CarMotion.REVERSE_SPEED;
	}

	/** The car uses the shared arcade step. A race car replaces this. */
	protected void steer() {
		CarMotion.Step step = CarMotion.step(this.speed, this.getYRot(), this.inputForward, this.inputBack, this.inputLeft, this.inputRight);
		this.speed = step.speed();
		this.setYRot(step.yaw());
	}

	public void applyDriverState(float speed, float yaw) {
		this.speed = Mth.clamp(speed, -this.reverseSpeed(), this.topSpeed());
		this.setYRot(yaw);
		// Position comes from the vanilla vehicle packet. Setting it here makes that packet move the car a second time.
		double radians = Math.toRadians(yaw);
		this.setDeltaMovement(-Math.sin(radians) * this.speed, this.getDeltaMovement().y, Math.cos(radians) * this.speed);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.impactCooldown > 0) {
			this.impactCooldown--;
		}
		if (this.getHurtTime() > 0) {
			this.setHurtTime(this.getHurtTime() - 1);
		}
		if (this.getDamage() > 0.0F) {
			this.setDamage(this.getDamage() - 1.0F);
		}

		if (this.drivesItself()) {
			boolean drivenLocally = this.getControllingPassenger() instanceof Player player && player.isLocalPlayer();
			if (drivenLocally) {
				CarClientHooks.inputReader.readInto(this);
				this.steer();
			} else {
				this.speed *= 0.9F;
				if (Math.abs(this.speed) < 0.004F) {
					this.speed = 0.0F;
				}
			}
			if (this.isInWater()) {
				this.speed *= this.waterDrag();
			}
			this.speed = Mth.clamp(this.speed, -this.reverseSpeed(), this.topSpeed());

			double vertical = this.getDeltaMovement().y;
			boolean bridging = this.bridgingGap();
			if (!this.isNoGravity() && !this.onGround() && !bridging) {
				vertical -= this.getGravity();
			} else if ((this.onGround() || bridging) && vertical < 0.0) {
				vertical = 0.0;
			}
			this.drive(this.floatOn(vertical));
			this.rammedTree = false;
			this.syncSpeed = this.speed;
			this.preparePath();
			this.beforeMove();
			double beforeY = this.getY();
			this.move(MoverType.SELF, this.getDeltaMovement());
			boolean blocked = this.horizontalCollision && this.getY() <= beforeY + 0.01;
			if (blocked && Math.abs(this.syncSpeed) >= this.topSpeed() - 0.04F && this.treeInFront()) {
				this.rammedTree = true;
				this.speed *= 0.25F;
				if (this.level().isClientSide() && this.impactCooldown == 0 && ModConfig.impactVolume() > 0.0F) {
					this.impactCooldown = 10;
					CarClientHooks.crash.accept(this);
				}
			} else if (blocked) {
				if (this.impactCooldown == 0 && Math.abs(this.syncSpeed) >= CarMotion.HIT_SPEED && !this.rammingSomeone() && (this.treeInFront() || this.largeObstacle())) {
					this.impactCooldown = 10;
					this.playImpact();
				}
				this.speed *= 0.5F;
				this.syncSpeed = this.speed;
			}
			if (drivenLocally) {
				CarClientHooks.afterLocalDrive.accept(this);
			}
			this.afterMove(blocked);
		}

		if (this.level() instanceof ServerLevel server && Math.abs(this.speed) >= CarMotion.HIT_SPEED) {
			this.ramEntities(server);
		}

		this.wheelAngle += (float) Math.hypot(this.getX() - this.xo, this.getZ() - this.zo) * 30.0F;
		this.updateTilt();
		if (this.getControllingPassenger() instanceof Player player) {
			this.alignPassenger(player);
		} else {
			this.alignedCarYaw = Float.NaN;
		}
	}

	/** The car slows down in water. An amphibious hull does not. */
	protected float waterDrag() {
		return 0.8F;
	}

	/** The car sinks. An amphibious hull replaces this while it is in the water. */
	protected double floatOn(double vertical) {
		return vertical;
	}

	/** Runs after the move. The car does nothing. */
	protected void afterMove(boolean blocked) {
	}

	/** Runs after the velocity is chosen and before the move. The car does nothing. */
	protected void beforeMove() {
	}

	/** Horizontal drive. A drill replaces this while boring so it can follow a slope. */
	protected void drive(double vertical) {
		double radians = Math.toRadians(this.getYRot());
		this.setDeltaMovement(-Math.sin(radians) * this.speed, vertical, Math.cos(radians) * this.speed);
	}

	/** A loader clears its blade here. The car does nothing, so driving stays the same. */
	protected void preparePath() {
	}

	/**
	 * Stay level when the far side is inside the speed-based reach.
	 * ponytail: sampled from the center along the drive direction. A wider wheelbase would check axle to axle.
	 */
	private boolean bridgingGap() {
		double reach = CarMotion.gapReach(this.speed);
		if (reach < 0.5) {
			return false;
		}
		double yaw = Math.toRadians(this.getYRot());
		double direction = Math.signum(this.speed);
		double forwardX = -Math.sin(yaw) * direction;
		double forwardZ = Math.cos(yaw) * direction;
		int feet = Mth.floor(this.getY());
		if (this.supported(this.getX(), feet, this.getZ())) {
			return false;
		}
		for (double step = 0.5; step < reach; step += 0.5) {
			if (this.supported(this.getX() + forwardX * step, feet, this.getZ() + forwardZ * step)) {
				return true;
			}
		}
		return this.supported(this.getX() + forwardX * reach, feet, this.getZ() + forwardZ * reach);
	}

	private boolean supported(double x, int feet, double z) {
		BlockPos ground = BlockPos.containing(x, feet - 1.0, z);
		return this.level().getBlockState(ground).blocksMotion() && !this.level().getBlockState(ground.above()).blocksMotion();
	}

	/** A grading loader stays flat. The car still steps up and noses onto a block. */
	protected boolean climbs() {
		return true;
	}

	private void updateTilt() {
		this.tiltO = this.tilt;
		double yaw = Math.toRadians(this.getYRot());
		double forwardX = -Math.sin(yaw);
		double forwardZ = Math.cos(yaw);
		double front = this.surfaceY(this.getX() + forwardX * 2.0, this.getZ() + forwardZ * 2.0);
		double back = this.surfaceY(this.getX() - forwardX * 2.0, this.getZ() - forwardZ * 2.0);
		float target = Mth.clamp((float) Math.toDegrees(Math.atan2(front - back, 4.0)), -20.0F, 28.0F);
		BlockPos hood = BlockPos.containing(this.getX() + forwardX * 1.8, this.getY() + 0.4, this.getZ() + forwardZ * 1.8);
		if (!this.climbs()) {
			target = Math.min(target, 0.0F);
		} else if (this.level().getBlockState(hood).blocksMotion()) {
			target = Math.max(target, 24.0F);
		}
		this.tilt += (target - this.tilt) * 0.35F;
	}

	private double surfaceY(double x, double z) {
		int base = Mth.floor(this.getY());
		for (int offset = 2; offset >= -3; offset--) {
			BlockPos pos = BlockPos.containing(x, base + offset, z);
			if (this.level().getBlockState(pos).blocksMotion() && !this.level().getBlockState(pos.above()).blocksMotion()) {
				return pos.getY() + 1.0;
			}
		}
		return this.getY();
	}

	public void alignPassenger(Entity passenger) {
		float carYaw = this.getYRot();
		float carDelta = Float.isNaN(this.alignedCarYaw) ? 0.0F : Mth.wrapDegrees(carYaw - this.alignedCarYaw);
		this.alignedCarYaw = carYaw;
		float look = CarMotion.clampLook(passenger.getYRot() + carDelta - carYaw);
		float yaw = carYaw + look;
		float yawO = this.yRotO + look;
		passenger.setYRot(yaw);
		passenger.yRotO = yawO;
		if (passenger instanceof LivingEntity living) {
			living.setYHeadRot(yaw);
			living.yHeadRotO = yawO;
			living.setYBodyRot(yaw);
			living.yBodyRotO = yawO;
		}
	}

	@Override
	public void onPassengerTurned(Entity passenger) {
		this.alignPassenger(passenger);
	}

	@Override
	public Component getDisplayName() {
		return this.hasCustomName() ? super.getDisplayName() : Component.translatable("entity.carmod.car.killer");
	}

	private void ramEntities(ServerLevel level) {
		AABB box = this.getBoundingBox().inflate(0.4, 0.2, 0.4);
		var source = level.damageSources().source(RAM_DAMAGE, this);
		float amount = CarMotion.hitDamage(this.speed);
		boolean hit = false;
		for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box)) {
			if (!living.isAlive() || living.getVehicle() == this || !isLarge(living)) {
				continue;
			}
			boolean hurt = living.hurtServer(level, source, amount);
			if (!hurt) {
				continue;
			}
			hit = true;
			if (!living.isAlive() && living.hasCustomName() && !(living instanceof Player)) {
				level.getServer().getPlayerList().broadcastSystemMessage(source.getLocalizedDeathMessage(living), false);
			}
		}
		if (hit) {
			this.playImpact();
			this.onRammed(level);
		}
	}

	/** A helicopter explodes when this ram is fast enough. The car does not. */
	protected void onRammed(ServerLevel level) {
	}

	/**
	 * Breaks logs and leaves in front of a car that hit them at full speed, then slows down, hurts the driver, and wears the car.
	 */
	public void smashTrees(ServerLevel level) {
		if (!this.breakTrees(level)) {
			return;
		}
		this.speed *= 0.25F;
		float volume = ModConfig.impactVolume();
		if (volume > 0.0F) {
			Player driver = this.getControllingPassenger() instanceof Player player ? player : null;
			level.playSound(driver, this.getX(), this.getY(), this.getZ(), ModSounds.CRASH, SoundSource.NEUTRAL, volume, 1.0F);
		}
		var source = level.damageSources().source(RAM_DAMAGE, this);
		if (this.getControllingPassenger() instanceof Player player) {
			player.hurtServer(level, source, CarMotion.RAM_DRIVER_DAMAGE);
		}
		this.wear += CarMotion.RAM_WEAR;
		if (this.wear >= new ItemStack(ModItems.CAR).getMaxDamage()) {
			this.ejectPassengers();
			this.kill(level);
		}
	}

	/**
	 * Fells one whole tree: every connected log, then the leaves around that trunk.
	 * ponytail: stops at 160 logs and 512 leaves. A vanilla tree fits; a giant custom tree is cut off and the rest decays.
	 */
	private boolean breakTrees(ServerLevel level) {
		BlockPos origin = this.frontBlock();
		List<BlockPos> hitLogs = this.logsTouched(level, origin);
		if (hitLogs.isEmpty()) {
			hitLogs = this.findTrunk(level, origin);
		}
		if (hitLogs.isEmpty()) {
			return this.breakTouchedLeaves(level, origin);
		}
		Set<BlockPos> grown = this.growLogs(level, hitLogs);
		Set<BlockPos> trunk = this.hasAdjacentLeaf(level, grown) ? grown : new HashSet<>(hitLogs);
		Set<BlockPos> canopy = trunk == grown ? this.growLeaves(level, trunk) : Set.of();
		return this.destroyAll(level, trunk) | this.destroyAll(level, canopy);
	}

	private List<BlockPos> logsTouched(ServerLevel level, BlockPos origin) {
		List<BlockPos> logs = new ArrayList<>();
		for (BlockPos cursor : BlockPos.betweenClosed(origin.offset(-1, 0, -1), origin.offset(1, 3, 1))) {
			BlockPos pos = cursor.immutable();
			if (level.getBlockState(pos).is(BlockTags.LOGS)) {
				logs.add(pos);
			}
		}
		return logs;
	}

	private List<BlockPos> findTrunk(ServerLevel level, BlockPos origin) {
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		for (BlockPos cursor : BlockPos.betweenClosed(origin.offset(-1, 0, -1), origin.offset(1, 3, 1))) {
			BlockPos pos = cursor.immutable();
			if (level.getBlockState(pos).is(BlockTags.LEAVES) && seen.add(pos)) {
				queue.add(pos);
			}
		}
		List<BlockPos> found = new ArrayList<>();
		int scanned = 0;
		while (!queue.isEmpty() && found.isEmpty() && scanned < 80) {
			BlockPos pos = queue.removeFirst();
			scanned++;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						BlockPos next = pos.offset(dx, dy, dz);
						if (!seen.add(next)) {
							continue;
						}
						BlockState state = level.getBlockState(next);
						if (state.is(BlockTags.LOGS)) {
							found.add(next);
						} else if (state.is(BlockTags.LEAVES)) {
							queue.add(next);
						}
					}
				}
			}
		}
		return found;
	}

	private Set<BlockPos> growLogs(ServerLevel level, List<BlockPos> seeds) {
		Set<BlockPos> logs = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (BlockPos seed : seeds) {
			if (logs.add(seed)) {
				queue.add(seed);
			}
		}
		while (!queue.isEmpty() && logs.size() < 160) {
			BlockPos pos = queue.removeFirst();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0 || logs.size() >= 160) {
							continue;
						}
						BlockPos next = pos.offset(dx, dy, dz);
						if (!logs.contains(next) && level.getBlockState(next).is(BlockTags.LOGS)) {
							logs.add(next);
							queue.add(next);
						}
					}
				}
			}
		}
		return logs;
	}

	private boolean hasAdjacentLeaf(ServerLevel level, Set<BlockPos> logs) {
		for (BlockPos log : logs) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						if (level.getBlockState(log.offset(dx, dy, dz)).is(BlockTags.LEAVES)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private Set<BlockPos> growLeaves(ServerLevel level, Set<BlockPos> logs) {
		Set<BlockPos> leaves = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (BlockPos log : logs) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						BlockPos next = log.offset(dx, dy, dz);
						if (this.nearTrunk(next, logs) && level.getBlockState(next).is(BlockTags.LEAVES) && leaves.add(next)) {
							queue.add(next);
						}
					}
				}
			}
		}
		while (!queue.isEmpty() && leaves.size() < 512) {
			BlockPos pos = queue.removeFirst();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0 || leaves.size() >= 512) {
							continue;
						}
						BlockPos next = pos.offset(dx, dy, dz);
						if (!leaves.contains(next) && this.nearTrunk(next, logs) && level.getBlockState(next).is(BlockTags.LEAVES)) {
							leaves.add(next);
							queue.add(next);
						}
					}
				}
			}
		}
		return leaves;
	}

	private boolean nearTrunk(BlockPos pos, Set<BlockPos> logs) {
		for (BlockPos log : logs) {
			int dx = Math.abs(pos.getX() - log.getX());
			int dy = Math.abs(pos.getY() - log.getY());
			int dz = Math.abs(pos.getZ() - log.getZ());
			if (Math.max(dx, Math.max(dy, dz)) <= 5) {
				return true;
			}
		}
		return false;
	}

	private boolean breakTouchedLeaves(ServerLevel level, BlockPos origin) {
		boolean broke = false;
		for (BlockPos cursor : BlockPos.betweenClosed(origin.offset(-1, 0, -1), origin.offset(1, 3, 1))) {
			BlockPos pos = cursor.immutable();
			if (level.getBlockState(pos).is(BlockTags.LEAVES)) {
				broke |= level.destroyBlock(pos, true, this, 512);
			}
		}
		return broke;
	}

	private boolean destroyAll(ServerLevel level, Set<BlockPos> blocks) {
		boolean broke = false;
		for (BlockPos pos : blocks) {
			broke |= level.destroyBlock(pos, true, this, 512);
		}
		return broke;
	}

	private boolean treeInFront() {
		BlockPos center = this.frontBlock();
		for (BlockPos cursor : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 4, 1))) {
			BlockState state = this.level().getBlockState(cursor);
			if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
				return true;
			}
		}
		return false;
	}

	private boolean rammingSomeone() {
		if (Math.abs(this.syncSpeed) < CarMotion.HIT_SPEED) {
			return false;
		}
		AABB box = this.getBoundingBox().inflate(0.4, 0.2, 0.4);
		for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, box)) {
			if (living.isAlive() && living.getVehicle() != this && isLarge(living)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isLarge(Entity entity) {
		AABB box = entity.getBoundingBox();
		return CarMotion.countsAsImpact(Math.max(box.maxX - box.minX, box.maxZ - box.minZ), box.maxY - box.minY);
	}

	/** A wall at least two blocks wide and two blocks tall. Fences and single pillars do not count. */
	private boolean largeObstacle() {
		BlockPos center = this.frontBlock();
		double yaw = Math.toRadians(this.getYRot());
		int rightX = (int) Math.round(Math.cos(yaw));
		int rightZ = (int) Math.round(Math.sin(yaw));
		if (rightX == 0 && rightZ == 0) {
			return false;
		}
		int columns = 0;
		for (int side = -1; side <= 1; side++) {
			BlockPos column = center.offset(rightX * side, 0, rightZ * side);
			if (this.fullCube(column) && this.fullCube(column.above())) {
				columns++;
			}
		}
		return columns >= 2;
	}

	private boolean fullCube(BlockPos pos) {
		return this.level().getBlockState(pos).isCollisionShapeFullBlock(this.level(), pos);
	}

	private void playImpact() {
		float volume = ModConfig.impactVolume();
		if (volume <= 0.0F) {
			return;
		}
		if (this.level().isClientSide()) {
			CarClientHooks.impact.accept(this);
			return;
		}
		this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.IMPACT, SoundSource.NEUTRAL, volume, 1.0F);
	}

	private BlockPos frontBlock() {
		double yaw = Math.toRadians(this.getYRot());
		double direction = Math.signum(this.speed == 0.0F ? this.syncSpeed : this.speed);
		if (direction == 0.0) {
			direction = 1.0;
		}
		double forwardX = -Math.sin(yaw) * direction;
		double forwardZ = Math.cos(yaw) * direction;
		return BlockPos.containing(this.getX() + forwardX * 1.8, this.getY() + 0.5, this.getZ() + forwardZ * 1.8);
	}

	/**
	 * The riding client owns the driving. An empty car falls on the server.
	 * ponytail: the riding client simulates throttle. The server keeps speed and lets the vanilla vehicle packet move the entity.
	 */
	private boolean drivesItself() {
		if (this.getControllingPassenger() instanceof Player player) {
			return player.isLocalPlayer();
		}
		return !this.level().isClientSide();
	}

	@Override
	public boolean isPickable() {
		return !this.isRemoved();
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean hurtClient(DamageSource source) {
		return !this.isRemoved();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (this.isRemoved() || !(source.getEntity() instanceof Player player) || player.isSpectator()) {
			return false;
		}
		this.ejectPassengers();
		if (player.getAbilities().instabuild) {
			this.discard();
		} else {
			this.destroy(level, this.getDropItem());
		}
		return true;
	}

	protected SoundEvent boardSound() {
		return ModSounds.IGNITION;
	}

	protected float boardVolume() {
		return 0.6F;
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		player.setYRot(this.getYRot());
		player.setYHeadRot(this.getYRot());
		if (!this.level().isClientSide() && player.startRiding(this)) {
			this.level().playSound(null, this.getX(), this.getY(), this.getZ(), this.boardSound(), SoundSource.NEUTRAL, this.boardVolume(), 1.0F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return this.getPassengers().isEmpty() && !this.isRemoved();
	}

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		Entity passenger = this.getFirstPassenger();
		return passenger instanceof LivingEntity living ? living : null;
	}

	@Override
	protected void positionRider(Entity passenger, MoveFunction callback) {
		double yaw = Math.toRadians(this.getYRot());
		double leftX = Math.cos(yaw);
		double leftZ = Math.sin(yaw);
		double forwardX = -Math.sin(yaw);
		double forwardZ = Math.cos(yaw);
		// Driver sits on the left seat, just behind the dashboard. Eye height stays the player's normal height, inside the roof.
		double seatSide = 6.0 / 16.0;
		double seatBack = 4.0 / 16.0;
		callback.accept(
				passenger,
				this.getX() + leftX * seatSide - forwardX * seatBack,
				this.getY() + 0.42,
				this.getZ() + leftZ * seatSide - forwardZ * seatBack
		);
	}

	@Override
	public float maxUpStep() {
		return this.climbs() ? 1.0F : 0.0F;
	}

	@Override
	public boolean isFlyingVehicle() {
		return this.bridgingGap();
	}

	@Override
	protected Item getDropItem() {
		return ModItems.CAR;
	}

	@Override
	public void destroy(ServerLevel level, Item item) {
		this.kill(level);
		if (!level.getGameRules().get(GameRules.ENTITY_DROPS)) {
			return;
		}
		ItemStack stack = new ItemStack(item);
		stack.setDamageValue(Math.min(this.wear, Math.max(0, stack.getMaxDamage() - 1)));
		Component custom = this.getCustomName();
		if (custom != null) {
			stack.set(DataComponents.CUSTOM_NAME, custom);
		}
		this.spawnAtLocation(level, stack);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putFloat("Speed", this.speed);
		output.putInt("Wear", this.wear);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.speed = input.getFloatOr("Speed", 0.0F);
		this.wear = input.getIntOr("Wear", 0);
	}

	@Override
	protected double getDefaultGravity() {
		return 0.06;
	}

	@Override
	protected void checkFallDamage(double fallDistance, boolean onGround, BlockState state, BlockPos pos) {
		if (onGround) {
			this.resetFallDistance();
		}
	}
}
