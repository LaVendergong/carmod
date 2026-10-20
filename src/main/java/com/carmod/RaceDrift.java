package com.carmod;

import net.minecraft.util.Mth;

/**
 * Race-car throttle and drift. The nose can lead the direction of travel while the handbrake is held.
 * ponytail: travel yaw eases toward the nose by a fixed fraction. It is not a tire model.
 */
public final class RaceDrift {
	public static final float TOP_SPEED = 2.0F;
	public static final float REVERSE_SPEED = 0.35F;
	public static final float ACCEL = 0.07F;
	public static final float BRAKE = 0.09F;
	public static final float COAST = 0.96F;
	public static final float DRIFT_MIN = 0.45F;
	private static final float GRIP_TURN = 4.2F;
	private static final float DRIFT_TURN = 9.0F;
	private static final float HOLD_GRIP = 0.65F;
	private static final float DRIFT_GRIP = 0.08F;
	private static final float MAX_SLIP = 55.0F;
	private static final float DRIFT_BLEED = 0.996F;

	public record Tick(float speed, float nose, float travel, boolean drifting) {
	}

	private RaceDrift() {
	}

	public static Tick tick(float speed, float nose, float travel, boolean forward, boolean back, boolean left, boolean right, boolean handbrake) {
		if (Math.abs(speed) < 0.02F) {
			travel = nose;
		}
		float turn = (right ? 1.0F : 0.0F) - (left ? 1.0F : 0.0F);
		boolean drifting = handbrake && speed > DRIFT_MIN && turn != 0.0F;
		if (Math.abs(speed) > 0.02F && turn != 0.0F) {
			float strength = Mth.clamp(Math.abs(speed) / TOP_SPEED, 0.35F, 1.0F);
			float rate = drifting ? DRIFT_TURN : GRIP_TURN;
			nose += turn * rate * strength * Math.signum(speed);
		}
		if (forward) {
			speed = Math.min(TOP_SPEED, speed + ACCEL);
		} else if (back) {
			speed = Math.max(-REVERSE_SPEED, speed - BRAKE);
		} else {
			speed *= COAST;
		}
		if (drifting) {
			speed *= DRIFT_BLEED;
		}
		float slip = Mth.wrapDegrees(nose - travel);
		if (drifting) {
			slip = Mth.clamp(slip, -MAX_SLIP, MAX_SLIP);
			nose = travel + slip;
		}
		float grip = drifting ? DRIFT_GRIP : HOLD_GRIP;
		travel += Mth.wrapDegrees(nose - travel) * grip;
		if (Math.abs(speed) < 0.004F) {
			speed = 0.0F;
		}
		return new Tick(speed, nose, travel, drifting);
	}

	public static void selfCheck() {
		Tick launched = new Tick(0.0F, 0.0F, 0.0F, false);
		for (int i = 0; i < 40; i++) {
			launched = tick(launched.speed, launched.nose, launched.travel, true, false, false, false, false);
		}
		if (launched.speed < TOP_SPEED - 0.001F || launched.speed <= CarMotion.MAX_SPEED) {
			throw new IllegalStateException("race car did not outrun the car: " + launched.speed);
		}
		Tick sliding = launched;
		for (int i = 0; i < 12; i++) {
			sliding = tick(sliding.speed, sliding.nose, sliding.travel, true, false, true, false, true);
		}
		float slip = Math.abs(Mth.wrapDegrees(sliding.nose - sliding.travel));
		if (slip < 20.0F || !sliding.drifting || sliding.speed < 1.5F) {
			throw new IllegalStateException("race car did not drift: slip " + slip + " speed " + sliding.speed);
		}
		Tick gripped = sliding;
		for (int i = 0; i < 8; i++) {
			gripped = tick(gripped.speed, gripped.nose, gripped.travel, true, false, false, false, false);
		}
		float left = Math.abs(Mth.wrapDegrees(gripped.nose - gripped.travel));
		if (left >= slip) {
			throw new IllegalStateException("race car did not grip out of the drift");
		}
		Tick held = launched;
		for (int i = 0; i < 20; i++) {
			held = tick(held.speed, held.nose, held.travel, true, false, true, false, false);
		}
		if (Math.abs(Mth.wrapDegrees(held.nose - held.travel)) > 8.0F) {
			throw new IllegalStateException("race car slid without the handbrake");
		}
	}
}
