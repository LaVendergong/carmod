package com.carmod.block;

public final class RoadPlan {
	public static final int MAX_DISTANCE = 128;
	public static final int STEP_TICKS = 10;
	public static final int HEADROOM = 3;
	public static final int TREE_CLEAR = 16;

	public enum Kind {
		AIR,
		SOFT,
		ROAD,
		TREE,
		FLUID,
		HARD
	}

	public record Cell(int cost, boolean clear, boolean fill, boolean stop) {
		private static Cell blocked() {
			return new Cell(0, false, false, true);
		}
	}

	private RoadPlan() {
	}

	// below and under are ignored: the road is placed at the machine's own height and may hang in the air.
	public static Cell pave(Kind at, Kind below, Kind under) {
		if (at == Kind.FLUID || at == Kind.HARD) {
			return Cell.blocked();
		}
		if (at == Kind.ROAD) {
			return new Cell(0, false, false, false);
		}
		return new Cell(1, at != Kind.AIR, false, false);
	}

	public static Cell enter(Kind at, Kind below, Kind under) {
		if (at == Kind.FLUID || at == Kind.HARD) {
			return Cell.blocked();
		}
		return new Cell(0, at != Kind.AIR, false, false);
	}

	public static Cell boreEnter(boolean cutting, Kind at, Kind below, Kind under) {
		if (cutting && at != Kind.FLUID && at != Kind.AIR && at != Kind.ROAD) {
			at = Kind.AIR;
		}
		return enter(at, below, under);
	}

	public static void selfCheck() {
		Cell dirt = pave(Kind.SOFT, Kind.HARD, Kind.HARD);
		if (dirt.cost() != 1 || !dirt.clear() || dirt.fill() || dirt.stop()) {
			fail("dirt");
		}
		Cell floating = pave(Kind.AIR, Kind.AIR, Kind.AIR);
		if (floating.cost() != 1 || floating.fill() || floating.stop()) {
			fail("floating");
		}
		if (!pave(Kind.HARD, Kind.HARD, Kind.HARD).stop()) {
			fail("blocked");
		}
		if (!pave(Kind.FLUID, Kind.HARD, Kind.HARD).stop() || pave(Kind.AIR, Kind.FLUID, Kind.AIR).stop()) {
			fail("water");
		}
		Cell paved = pave(Kind.ROAD, Kind.HARD, Kind.HARD);
		if (paved.cost() != 0 || paved.clear() || paved.stop()) {
			fail("already paved");
		}
		Cell tree = pave(Kind.TREE, Kind.SOFT, Kind.HARD);
		if (tree.cost() != 1 || !tree.clear() || tree.stop()) {
			fail("tree");
		}
		Cell stepOn = enter(Kind.SOFT, Kind.HARD, Kind.HARD);
		if (stepOn.cost() != 0 || !stepOn.clear() || stepOn.stop()) {
			fail("enter dirt");
		}
		Cell stepHole = enter(Kind.AIR, Kind.AIR, Kind.SOFT);
		if (stepHole.cost() != 0 || stepHole.fill() || stepHole.stop()) {
			fail("enter hole");
		}
		if (!enter(Kind.HARD, Kind.SOFT, Kind.HARD).stop() || enter(Kind.ROAD, Kind.HARD, Kind.HARD).cost() != 0) {
			fail("enter blocked");
		}
		Cell boreIce = boreEnter(true, Kind.HARD, Kind.HARD, Kind.HARD);
		if (boreIce.stop() || boreIce.cost() != 0) {
			fail("bore ice");
		}
		Cell borePuddle = boreEnter(true, Kind.HARD, Kind.FLUID, Kind.FLUID);
		if (borePuddle.stop() || borePuddle.fill() || borePuddle.cost() != 0) {
			fail("bore puddle");
		}
		if (!boreEnter(false, Kind.HARD, Kind.HARD, Kind.HARD).stop()) {
			fail("bore alone");
		}
	}

	private static void fail(String name) {
		throw new IllegalStateException("road plan failed: " + name);
	}
}
