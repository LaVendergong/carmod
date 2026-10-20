package com.carmod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.carmod.entity.NukeEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A crater big enough to see from the air. Writes are budgeted so the server keeps ticking. */
public final class NukeBlast {
	public static final int RADIUS = 64;
	/** ponytail: 6000 block writes per tick. A solid underground blast takes a few seconds. Raising this floods the client with chunk packets. */
	private static final int WRITES_PER_TICK = 6000;
	private static final int SCANS_PER_TICK = 80000;
	private static final List<Job> JOBS = new ArrayList<>();

	private NukeBlast() {
	}

	public static boolean contains(int dx, int dy, int dz) {
		return dx * dx + dy * dy + dz * dz <= RADIUS * RADIUS;
	}

	public static void start(ServerLevel level, double x, double y, double z, Player owner) {
		Vec3 center = new Vec3(x, y, z);
		level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 8.0F, 0.5F);
		level.playSound(null, x, y, z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 6.0F, 0.6F);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y + 1.0, z, 6, 2.0, 2.0, 2.0, 0.0);
		hurt(level, center, owner);
		JOBS.add(new Job(level, BlockPos.containing(x, y, z)));
	}

	public static void tick(MinecraftServer server) {
		if (JOBS.isEmpty()) {
			return;
		}
		Iterator<Job> jobs = JOBS.iterator();
		while (jobs.hasNext()) {
			Job job = jobs.next();
			if (job.level.getServer() != server) {
				jobs.remove();
				continue;
			}
			if (job.age < 50) {
				job.level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, job.origin.getX() + 0.5, job.origin.getY() + job.age * 1.2, job.origin.getZ() + 0.5, 6, 3.0, 1.0, 3.0, 0.02);
			}
			if (job.step()) {
				jobs.remove();
			}
		}
	}

	private static void hurt(ServerLevel level, Vec3 center, Player owner) {
		AABB area = new AABB(center, center).inflate(RADIUS);
		var source = level.damageSources().explosion(null, owner);
		for (Entity entity : level.getEntities(EntityTypeTest.forClass(Entity.class), area, entity -> entity.isAlive() && !(entity instanceof NukeEntity))) {
			double distance = entity.position().distanceTo(center);
			if (distance > RADIUS) {
				continue;
			}
			double falloff = 1.0 - distance / RADIUS;
			if (entity instanceof LivingEntity living) {
				living.hurtServer(level, source, (float) Math.max(40.0, 200.0 * falloff));
			} else {
				entity.discard();
				continue;
			}
			Vec3 away = entity.position().subtract(center);
			if (away.lengthSqr() < 1.0E-4) {
				away = new Vec3(0.0, 1.0, 0.0);
			} else {
				away = away.normalize();
			}
			entity.setDeltaMovement(away.scale(1.2 + falloff * 3.0).add(0.0, 0.6 + falloff, 0.0));
			entity.hurtMarked = true;
		}
	}

	public static void selfCheck() {
		if (!contains(0, 0, 0) || !contains(RADIUS, 0, 0) || contains(RADIUS, 1, 0)) {
			throw new IllegalStateException("nuke radius is wrong");
		}
	}

	private static final class Job {
		private final ServerLevel level;
		private final BlockPos origin;
		private final int span = RADIUS * 2 + 1;
		private final int volume = this.span * this.span * this.span;
		private int cursor;
		private int age;

		private Job(ServerLevel level, BlockPos origin) {
			this.level = level;
			this.origin = origin;
		}

		/** @return true when the sphere is finished */
		private boolean step() {
			this.age++;
			int scanned = 0;
			int writes = 0;
			int minY = this.level.getMinY();
			int maxY = this.level.getMaxY();
			while (this.cursor < this.volume && scanned < SCANS_PER_TICK && writes < WRITES_PER_TICK) {
				int index = this.cursor++;
				scanned++;
				int dz = index % this.span - RADIUS;
				int dy = (index / this.span) % this.span - RADIUS;
				int dx = index / (this.span * this.span) - RADIUS;
				if (!contains(dx, dy, dz)) {
					continue;
				}
				int y = this.origin.getY() + dy;
				if (y < minY || y >= maxY) {
					continue;
				}
				BlockPos pos = new BlockPos(this.origin.getX() + dx, y, this.origin.getZ() + dz);
				BlockState state = this.level.getBlockState(pos);
				if (state.isAir() || state.getDestroySpeed(this.level, pos) < 0.0F) {
					continue;
				}
				this.level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
				writes++;
			}
			return this.cursor >= this.volume;
		}
	}
}
