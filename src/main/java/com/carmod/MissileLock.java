package com.carmod;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

/**
 * Picks a living thing standing on the ground, in front of the helicopter.
 */
public final class MissileLock {
	public static final double RANGE = 40.0;
	private static final double MIN_RANGE = 2.0;
	private static final double CONE = 0.35;

	public record Mark(double x, double y, double z, boolean grounded) {
	}

	private MissileLock() {
	}

	public static LivingEntity find(Entity from) {
		return find(from, RANGE);
	}

	public static LivingEntity find(Entity from, double range) {
		AABB area = from.getBoundingBox().inflate(range);
		List<LivingEntity> found = from.level().getEntities(EntityTypeTest.forClass(LivingEntity.class), area, living ->
				living.isAlive() && living.onGround() && !living.isSpectator() && living.getRootVehicle() != from);
		List<Mark> marks = new ArrayList<>(found.size());
		for (LivingEntity living : found) {
			marks.add(new Mark(living.getX(), living.getY(), living.getZ(), true));
		}
		int index = pick(from.getX(), from.getY(), from.getZ(), from.getYRot(), marks, range);
		return index < 0 ? null : found.get(index);
	}

	/** Facing yaw uses the same axes as the helicopter: -sin(yaw) is X, cos(yaw) is Z. */
	public static int pick(double x, double y, double z, float yaw, List<Mark> marks) {
		return pick(x, y, z, yaw, marks, RANGE);
	}

	public static int pick(double x, double y, double z, float yaw, List<Mark> marks, double range) {
		double facingX = -Math.sin(Math.toRadians(yaw));
		double facingZ = Math.cos(Math.toRadians(yaw));
		int best = -1;
		double bestScore = Double.MAX_VALUE;
		for (int i = 0; i < marks.size(); i++) {
			Mark mark = marks.get(i);
			if (!mark.grounded) {
				continue;
			}
			double dx = mark.x - x;
			double dz = mark.z - z;
			double horizontal = Math.hypot(dx, dz);
			if (horizontal < MIN_RANGE || horizontal > range) {
				continue;
			}
			double dot = (dx * facingX + dz * facingZ) / horizontal;
			if (dot < CONE) {
				continue;
			}
			double score = horizontal - dot * 16.0;
			if (score < bestScore) {
				bestScore = score;
				best = i;
			}
		}
		return best;
	}

	public static void selfCheck() {
		List<Mark> ahead = List.of(
				new Mark(0.0, 10.0, 20.0, true),
				new Mark(0.0, 10.0, -20.0, true),
				new Mark(0.0, 50.0, 20.0, false)
		);
		if (pick(0.0, 30.0, 0.0, 0.0F, ahead) != 0) {
			throw new IllegalStateException("missile lock missed the grounded target in front");
		}
		List<Mark> none = List.of(new Mark(0.0, 10.0, -20.0, true), new Mark(20.0, 10.0, 0.0, true));
		if (pick(0.0, 30.0, 0.0, 0.0F, none) != -1) {
			throw new IllegalStateException("missile lock accepted a target outside the forward cone");
		}
		List<Mark> hill = List.of(new Mark(0.0, 48.0, 16.0, true));
		if (pick(0.0, 30.0, 0.0, 0.0F, hill) != 0) {
			throw new IllegalStateException("missile lock skipped a grounded target above the helicopter");
		}
	}
}
