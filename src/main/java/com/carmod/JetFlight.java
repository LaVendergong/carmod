package com.carmod;

import net.minecraft.util.Mth;

/**
 * GTA-style fighter. Throttle sticks. The nose goes where you pitch and roll,
 * a steep bank turns you, and too little speed drops the nose.
 */
public final class JetFlight {
	public static final float CRUISE = 4.8F;
	public static final float DIVE_MAX = 6.0F;
	public static final float ROTATE = 1.35F;
	public static final float STALL = 1.2F;
	private static final float THROTTLE_UP = 0.018F;
	private static final float THROTTLE_DOWN = 0.028F;
	private static final float PITCH_RATE = 2.6F;
	private static final float ROLL_RATE = 8.0F;

	public record State(float throttle, float speed, float yaw, float pitch, float roll) {
	}

	private JetFlight() {
	}

	public static State tick(State state, boolean noseDown, boolean noseUp, boolean left, boolean right, boolean faster, boolean slower, boolean onGround) {
		float throttle = state.throttle;
		if (faster) {
			throttle = Math.min(1.0F, throttle + THROTTLE_UP);
		}
		if (slower) {
			throttle = Math.max(0.0F, throttle - THROTTLE_DOWN);
		}
		boolean airborne = !onGround;
		boolean stall = stalled(state.speed, onGround);
		boolean rotate = onGround && state.speed >= ROTATE && noseUp;
		float pitchInput = (noseUp ? 1.0F : 0.0F) - (noseDown ? 1.0F : 0.0F);
		float rollInput = (right ? 1.0F : 0.0F) - (left ? 1.0F : 0.0F);
		float pitch = state.pitch;
		float roll = state.roll;
		float yaw = state.yaw;
		if (!airborne) {
			roll = 0.0F;
			yaw += rollInput * 3.2F * Math.min(1.0F, Math.max(state.speed, 0.2F));
			pitch = rotate ? approach(pitch, 16.0F, 2.4F) : 0.0F;
		} else if (stall) {
			pitch = approach(pitch, -35.0F, 2.4F);
			roll = approachAngle(roll, 0.0F, 2.0F);
		} else {
			roll = wrap(roll + rollInput * ROLL_RATE);
			double radians = Math.toRadians(roll);
			float cos = (float) Math.cos(radians);
			float sin = (float) Math.sin(radians);
			pitch += pitchInput * PITCH_RATE * cos;
			yaw += pitchInput * PITCH_RATE * sin;
			yaw += sin * 2.4F * (0.4F + state.speed / CRUISE);
			pitch = Math.max(-80.0F, Math.min(80.0F, pitch));
		}
		float speed = state.speed;
		float target = throttle * CRUISE;
		if (airborne && pitch < -6.0F) {
			float dive = Math.min(1.0F, -pitch / 40.0F);
			target = Math.max(target, throttle * CRUISE + dive * (DIVE_MAX - CRUISE));
			target = Math.max(target, speed * 0.15F + dive * DIVE_MAX * 0.9F);
		}
		if (pitch > 8.0F) {
			target *= Math.max(0.42F, 1.0F - (pitch - 8.0F) / 70.0F);
		}
		if (stall) {
			target = Math.min(target, STALL * 0.8F);
		}
		float accel = (onGround ? 0.09F : 0.05F) * (0.35F + throttle);
		if (speed < target) {
			speed = Math.min(target, speed + accel);
		} else {
			speed = Math.max(target, speed - 0.02F);
		}
		if (onGround && throttle < 0.05F) {
			speed *= 0.955F;
		}
		if (speed < 0.01F) {
			speed = 0.0F;
		}
		speed = Math.min(speed, DIVE_MAX);
		return new State(throttle, speed, yaw, pitch, roll);
	}

	public static boolean stalled(float speed, boolean onGround) {
		return !onGround && speed < STALL;
	}

	/** Positive pitch points the nose up and climbs. */
	public static double vertical(float speed, float pitch, boolean onGround) {
		boolean rotate = onGround && speed >= ROTATE && pitch > 4.0F;
		if (onGround && !rotate) {
			return 0.0;
		}
		double velocity = Math.sin(Math.toRadians(pitch)) * speed;
		if (!onGround && speed < STALL) {
			velocity -= 0.2;
		}
		return velocity;
	}

	public static boolean crashes(float speed, double vertical, boolean wall, boolean ground, boolean ceiling, boolean water) {
		if (wall && speed >= 0.45F) {
			return true;
		}
		if (ground && vertical <= -0.55) {
			return true;
		}
		if (ceiling && vertical >= 0.35) {
			return true;
		}
		if (water && speed >= 0.45F) {
			return true;
		}
		return false;
	}

	private static float approach(float current, float target, float step) {
		float delta = target - current;
		if (Math.abs(delta) <= step) {
			return target;
		}
		return current + Math.copySign(step, delta);
	}

	private static float approachAngle(float current, float target, float step) {
		return approach(current, current + Mth.wrapDegrees(target - current), step);
	}

	private static float wrap(float degrees) {
		return Mth.wrapDegrees(degrees);
	}

	public static void selfCheck() {
		State roll = new State(0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
		for (int i = 0; i < 140; i++) {
			roll = tick(roll, false, false, false, false, true, false, true);
		}
		if (roll.speed < 3.5F) {
			throw new IllegalStateException("jet stayed slow: " + roll.speed);
		}
		if (vertical(roll.speed, roll.pitch, true) != 0.0) {
			throw new IllegalStateException("jet climbed before the nose was pulled up");
		}
		for (int i = 0; i < 12; i++) {
			roll = tick(roll, false, true, false, false, true, false, true);
		}
		if (roll.pitch < 8.0F || vertical(roll.speed, roll.pitch, true) <= 0.0) {
			throw new IllegalStateException("jet did not rotate: pitch " + roll.pitch);
		}
		State bank = new State(1.0F, CRUISE, 0.0F, 0.0F, 0.0F);
		for (int i = 0; i < 24; i++) {
			bank = tick(bank, false, false, false, true, false, false, false);
		}
		if (Math.abs(bank.roll) < 150.0F) {
			throw new IllegalStateException("jet could not roll inverted: " + bank.roll);
		}
		float held = bank.roll;
		for (int i = 0; i < 8; i++) {
			bank = tick(bank, false, false, false, false, false, false, false);
		}
		if (Math.abs(bank.roll - held) > 1.0F) {
			throw new IllegalStateException("jet did not hold inverted: " + bank.roll);
		}
		State inverted = new State(1.0F, CRUISE, 0.0F, 0.0F, 180.0F);
		for (int i = 0; i < 6; i++) {
			inverted = tick(inverted, false, true, false, false, false, false, false);
		}
		if (inverted.pitch > -8.0F) {
			throw new IllegalStateException("pulling up while inverted did not point at the ground: " + inverted.pitch);
		}
		State dive = new State(1.0F, CRUISE, 0.0F, 0.0F, 0.0F);
		for (int i = 0; i < 36; i++) {
			dive = tick(dive, true, false, false, false, false, false, false);
		}
		if (dive.pitch > -20.0F || dive.speed < CRUISE + 0.3F) {
			throw new IllegalStateException("jet dive did not accelerate: " + dive.speed + " pitch " + dive.pitch);
		}
		State climb = new State(1.0F, CRUISE, 0.0F, 0.0F, 0.0F);
		for (int i = 0; i < 40; i++) {
			climb = tick(climb, false, true, false, false, false, false, false);
		}
		if (climb.pitch < 20.0F || climb.speed > CRUISE - 0.15F) {
			throw new IllegalStateException("jet climb did not bleed speed: " + climb.speed);
		}
		State stall = new State(0.2F, 0.3F, 0.0F, 20.0F, 0.0F);
		for (int i = 0; i < 16; i++) {
			stall = tick(stall, false, true, false, false, false, false, false);
		}
		if (stall.pitch > -10.0F || vertical(stall.speed, stall.pitch, false) >= 0.0) {
			throw new IllegalStateException("jet stall did not drop the nose: " + stall.pitch);
		}
		if (!crashes(1.2F, 0.0, true, false, false, false) || crashes(0.1F, 0.0, true, false, false, false)) {
			throw new IllegalStateException("jet wall crash is wrong");
		}
		if (crashes(1.0F, -0.2, false, true, false, false) || !crashes(1.0F, -0.8, false, true, false, false)) {
			throw new IllegalStateException("jet landing crash is wrong");
		}
	}
}
