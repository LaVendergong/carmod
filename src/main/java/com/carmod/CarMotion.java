package com.carmod;

import net.minecraft.util.Mth;

/**
 * Arcade steering shared by the car entity. No world access, so the mod init can self-check it.
 */
public final class CarMotion {
	public static final float MAX_SPEED = 1.2F;
	public static final float REVERSE_SPEED = 0.3F;
	public static final float ACCEL = 0.03F;
	public static final float BRAKE = 0.07F;
	public static final float COAST = 0.94F;
	public static final float LOOK_LIMIT = 120.0F;
	/** Blocks of gap crossed per block/tick of speed. Full speed reaches 6. */
	public static final float GAP_PER_SPEED = 5.0F;
	public static final float HIT_SPEED = 0.5F;
	public static final float HIT_DAMAGE = 6.0F;
	public static final float MAX_HIT_DAMAGE = 14.0F;
	public static final float RAM_DRIVER_DAMAGE = 6.0F;
	public static final int RAM_WEAR = 10;

	public record Step(float speed, float yaw) {
	}

	private CarMotion() {
	}

	public static Step step(float speed, float yaw, boolean forward, boolean back, boolean left, boolean right) {
		float turn = (right ? 1.0F : 0.0F) - (left ? 1.0F : 0.0F);
		if (Math.abs(speed) > 0.02F && turn != 0.0F) {
			float turnStrength = Mth.clamp(Math.abs(speed) / MAX_SPEED, 0.25F, 1.0F);
			yaw += turn * 3.6F * turnStrength * Math.signum(speed);
		}
		if (forward) {
			speed = Math.min(MAX_SPEED, speed + ACCEL);
		} else if (back) {
			speed = Math.max(-REVERSE_SPEED, speed - BRAKE);
		} else {
			speed *= COAST;
		}
		if (Math.abs(speed) < 0.004F) {
			speed = 0.0F;
		}
		return new Step(speed, yaw);
	}

	public static float clampLook(float offset) {
		return Mth.clamp(Mth.wrapDegrees(offset), -LOOK_LIMIT, LOOK_LIMIT);
	}

	public static float gapReach(float speed) {
		return Math.abs(speed) * GAP_PER_SPEED;
	}

	/** A pig or a player counts. A chicken does not. */
	public static boolean countsAsImpact(double width, double height) {
		return width >= 0.8 || height >= 1.5;
	}

	public static float hitDamage(float speed) {
		float along = (Math.abs(speed) - HIT_SPEED) / (MAX_SPEED - HIT_SPEED);
		return Mth.lerp(Mth.clamp(along, 0.0F, 1.0F), HIT_DAMAGE, MAX_HIT_DAMAGE);
	}

	public static float enginePitch(float blocksPerTick) {
		return Mth.lerp(throttle(blocksPerTick), 0.8F, 1.75F);
	}

	public static float engineVolume(float blocksPerTick, boolean occupied) {
		float along = throttle(blocksPerTick);
		if (!occupied && along == 0.0F) {
			return 0.0F;
		}
		return Mth.lerp(along, occupied ? 0.16F : 0.12F, 0.5F);
	}

	private static float throttle(float blocksPerTick) {
		return Mth.clamp(Math.abs(blocksPerTick) / MAX_SPEED, 0.0F, 1.0F);
	}

	public static void selfCheck() {
		Step launched = new Step(0.0F, 0.0F);
		float previous = -1.0F;
		for (int i = 0; i < 80; i++) {
			launched = step(launched.speed, launched.yaw, true, false, false, false);
			if (launched.speed + 0.0001F < previous) {
				throw new IllegalStateException("throttle did not keep speeding up: " + launched.speed);
			}
			previous = launched.speed;
		}
		if (launched.speed < MAX_SPEED - 0.001F) {
			throw new IllegalStateException("car did not reach max speed: " + launched.speed);
		}
		Step turned = step(launched.speed, 0.0F, true, false, true, false);
		if (turned.yaw >= 0.0F) {
			throw new IllegalStateException("car did not steer left");
		}
		Step coasting = launched;
		for (int i = 0; i < 120; i++) {
			coasting = step(coasting.speed, coasting.yaw, false, false, false, false);
		}
		if (coasting.speed != 0.0F) {
			throw new IllegalStateException("car did not coast to a stop");
		}
		if (clampLook(170.0F) != LOOK_LIMIT || clampLook(-170.0F) != -LOOK_LIMIT || clampLook(30.0F) != 30.0F) {
			throw new IllegalStateException("look limit failed");
		}
		if (!(gapReach(MAX_SPEED) > gapReach(MAX_SPEED * 0.5F) && gapReach(MAX_SPEED) > 3.0F)) {
			throw new IllegalStateException("faster speed did not cross farther");
		}
		if (hitDamage(HIT_SPEED) != HIT_DAMAGE || hitDamage(MAX_SPEED) != MAX_HIT_DAMAGE || hitDamage(MAX_SPEED) <= hitDamage(HIT_SPEED)) {
			throw new IllegalStateException("impact damage did not scale with speed");
		}
		if (engineVolume(0.0F, false) != 0.0F || engineVolume(0.0F, true) <= 0.0F || !(enginePitch(0.0F) < enginePitch(MAX_SPEED))) {
			throw new IllegalStateException("engine sound did not follow the throttle");
		}
		if (countsAsImpact(0.4, 0.7) || !countsAsImpact(0.9, 0.9) || !countsAsImpact(0.6, 1.8) || countsAsImpact(0.6, 0.85)) {
			throw new IllegalStateException("impact size check is wrong");
		}
	}
}
