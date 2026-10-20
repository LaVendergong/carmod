package com.carmod;

/**
 * How deep an amphibious hull sits, and how fast it climbs back to that draft.
 * ponytail: a constant lift toward one waterline. It does not model waves.
 */
public final class AmphibiousFloat {
	public static final double DRAFT = 0.4;
	private static final double LIFT = 0.2;

	private AmphibiousFloat() {
	}

	public static double vertical(double submerged, boolean onGround, double gravityVertical) {
		if (submerged <= 0.0) {
			return gravityVertical;
		}
		double lift = (submerged - DRAFT) * LIFT;
		if (lift < 0.0 && onGround) {
			return gravityVertical;
		}
		return lift;
	}

	public static void selfCheck() {
		if (Math.abs(vertical(DRAFT, false, -1.0)) > 0.0001) {
			throw new IllegalStateException("amphibious hull did not sit at its waterline");
		}
		if (vertical(2.0, false, -1.0) <= 0.0) {
			throw new IllegalStateException("amphibious hull did not rise from deep water");
		}
		if (vertical(0.1, true, 0.0) != 0.0) {
			throw new IllegalStateException("a puddle pulled the hull off the ground");
		}
		if (vertical(0.0, false, -0.06) != -0.06) {
			throw new IllegalStateException("dry ground did not keep gravity");
		}
		if (vertical(0.1, false, -0.06) >= 0.0) {
			throw new IllegalStateException("a hull above the waterline did not settle");
		}
	}
}
