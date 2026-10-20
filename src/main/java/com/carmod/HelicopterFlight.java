package com.carmod;

/**
 * Helicopter throttle. Rotors have to be spinning before it will hold altitude.
 * ponytail: vertical speed eases toward climb, hover, or descend. It is not a rotor model.
 */
public final class HelicopterFlight {
	public static final float TOP_SPEED = 0.9F;
	public static final float REVERSE_SPEED = 0.35F;
	public static final float ACCEL = 0.045F;
	public static final float YAW_STEP = 4.0F;
	public static final float FLIGHT_RPM = 0.8F;
	/** Eight seconds from stopped to full rotor speed. Flight is allowed at {@link #FLIGHT_RPM}. */
	public static final float SPOOL_UP = 1.0F / 160.0F;
	public static final float SPOOL_DOWN = 1.0F / 80.0F;
	public static final double CLIMB = 0.26;
	public static final double DESCEND = -0.2;
	private static final double GRAVITY = 0.06;

	public record Step(float speed, float yaw, double vertical) {
	}

	private HelicopterFlight() {
	}

	public static float rpm(float current, boolean running) {
		if (running) {
			return Math.min(1.0F, current + SPOOL_UP);
		}
		return Math.max(0.0F, current - SPOOL_DOWN);
	}

	public static Step tick(float speed, float yaw, double vertical, boolean forward, boolean back, boolean left, boolean right, boolean up, boolean down, boolean powered, boolean onGround) {
		float turn = (right ? 1.0F : 0.0F) - (left ? 1.0F : 0.0F);
		if (turn != 0.0F) {
			yaw += turn * YAW_STEP;
		}
		if (!powered) {
			speed *= 0.9F;
			if (Math.abs(speed) < 0.004F) {
				speed = 0.0F;
			}
			vertical = onGround ? 0.0 : vertical - GRAVITY;
			return new Step(speed, yaw, vertical);
		}
		if (forward) {
			speed = Math.min(TOP_SPEED, speed + ACCEL);
		} else if (back) {
			speed = Math.max(-REVERSE_SPEED, speed - ACCEL);
		} else {
			speed *= 0.94F;
			if (Math.abs(speed) < 0.004F) {
				speed = 0.0F;
			}
		}
		double target = 0.0;
		if (up) {
			target = CLIMB;
		} else if (down && !onGround) {
			target = DESCEND;
		}
		vertical += (target - vertical) * 0.35;
		if (onGround && vertical < 0.0) {
			vertical = 0.0;
		}
		return new Step(speed, yaw, vertical);
	}

	public static void selfCheck() {
		Step falling = new Step(0.0F, 0.0F, 0.0);
		for (int i = 0; i < 10; i++) {
			falling = tick(falling.speed, falling.yaw, falling.vertical, false, false, false, false, false, false, false, false);
		}
		if (falling.vertical > -0.4) {
			throw new IllegalStateException("helicopter rotors-off did not fall: " + falling.vertical);
		}
		Step climb = new Step(0.0F, 0.0F, 0.0);
		for (int i = 0; i < 12; i++) {
			climb = tick(climb.speed, climb.yaw, climb.vertical, false, false, false, false, true, false, true, false);
		}
		if (climb.vertical < 0.15) {
			throw new IllegalStateException("helicopter did not climb: " + climb.vertical);
		}
		Step hover = new Step(0.0F, 0.0F, -0.25);
		for (int i = 0; i < 16; i++) {
			hover = tick(hover.speed, hover.yaw, hover.vertical, false, false, false, false, false, false, true, false);
		}
		if (Math.abs(hover.vertical) > 0.05) {
			throw new IllegalStateException("helicopter did not hover: " + hover.vertical);
		}
		Step sink = new Step(0.0F, 0.0F, 0.0);
		for (int i = 0; i < 12; i++) {
			sink = tick(sink.speed, sink.yaw, sink.vertical, false, false, false, false, false, true, true, false);
		}
		if (sink.vertical > -0.1) {
			throw new IllegalStateException("helicopter did not descend: " + sink.vertical);
		}
		Step cruise = new Step(0.0F, 0.0F, 0.0);
		for (int i = 0; i < 30; i++) {
			cruise = tick(cruise.speed, cruise.yaw, cruise.vertical, true, false, false, false, false, false, true, false);
		}
		if (cruise.speed < 0.7F) {
			throw new IllegalStateException("helicopter did not get going: " + cruise.speed);
		}
		Step yawed = tick(0.0F, 0.0F, 0.0, false, false, true, false, false, false, true, true);
		if (yawed.yaw >= 0.0F) {
			throw new IllegalStateException("helicopter did not yaw left");
		}
		Step parked = tick(0.2F, 0.0F, -0.2, false, false, false, false, false, true, true, true);
		if (parked.vertical != 0.0) {
			throw new IllegalStateException("helicopter sank into the ground");
		}
		Step early = tick(0.0F, 0.0F, 0.0, false, false, false, false, true, false, false, true);
		if (early.vertical != 0.0) {
			throw new IllegalStateException("helicopter lifted before flight speed");
		}
		float spinning = 0.0F;
		for (int i = 0; i < 40; i++) {
			spinning = rpm(spinning, true);
		}
		if (spinning >= FLIGHT_RPM) {
			throw new IllegalStateException("helicopter reached flight speed too soon: " + spinning);
		}
		for (int i = 0; i < 120; i++) {
			spinning = rpm(spinning, true);
		}
		if (spinning < FLIGHT_RPM) {
			throw new IllegalStateException("helicopter never reached flight speed: " + spinning);
		}
		if (!crashes(0.5F, 0.0, true, false, false, false) || crashes(0.1F, 0.0, true, false, false, false)) {
			throw new IllegalStateException("helicopter wall crash is wrong");
		}
		if (crashes(0.1F, -0.2, false, true, false, false) || !crashes(0.1F, -0.6, false, true, false, false)) {
			throw new IllegalStateException("helicopter landing crash is wrong");
		}
		if (!crashes(0.7F, 0.0, false, false, false, true) || crashes(0.3F, 0.0, false, false, false, true)) {
			throw new IllegalStateException("helicopter ram crash is wrong");
		}
	}

	/** A wall hit, a hard landing, or a fast ram. A gentle touch does not count. */
	public static boolean crashes(float speed, double vertical, boolean wall, boolean ground, boolean ceiling, boolean entity) {
		if (wall && Math.abs(speed) >= 0.3F) {
			return true;
		}
		if (ground && vertical <= -0.45) {
			return true;
		}
		if (ceiling && vertical >= 0.2) {
			return true;
		}
		return entity && Math.abs(speed) >= 0.55F;
	}
}
