package com.carmod;

import java.util.HashSet;
import java.util.Set;

import com.carmod.entity.DrillEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blocks a drill clears while it bores. The slope is the vehicle hitbox swept along the dig direction.
 * ponytail: fixed 0.5-block steps over a short reach. A longer bore would step by the hitbox size.
 */
public final class DrillPath {
	public static final double REACH = 2.0;
	private static final double MARGIN = 0.35;
	private static final double HEADROOM = 0.5;

	private DrillPath() {
	}

	public static double[] direction(float yaw, int angle, float sign) {
		if (sign == 0.0F) {
			sign = 1.0F;
		}
		double yawRad = Math.toRadians(yaw);
		double pitch = Math.toRadians(angle);
		double horizontal = Math.cos(pitch);
		return new double[] {
				-Math.sin(yawRad) * horizontal * sign,
				-Math.sin(pitch) * sign,
				Math.cos(yawRad) * horizontal * sign
		};
	}

	/** Where the drill was before this tick's move. */
	public static double[] rewind(double x, double y, double z, float yaw, int angle, float speed) {
		double[] dir = direction(yaw, angle, Math.signum(speed));
		double dist = Math.abs(speed);
		return new double[] { x - dir[0] * dist, y - dir[1] * dist, z - dir[2] * dist };
	}

	public static Set<BlockPos> bore(double x, double y, double z, float yaw, int angle, float travel) {
		Set<BlockPos> blocks = new HashSet<>();
		if (angle == 0) {
			return blocks;
		}
		double[] dir = direction(yaw, angle, Math.signum(travel));
		double half = DrillEntity.WIDTH * 0.5 + MARGIN;
		double height = DrillEntity.HEIGHT + HEADROOM;
		for (double t = 0.0; t <= REACH + 0.001; t += 0.5) {
			addBox(blocks, x + dir[0] * t, y + dir[1] * t, z + dir[2] * t, half, height);
		}
		return blocks;
	}

	public static boolean canBore(BlockState state, BlockGetter level, BlockPos pos) {
		return state.blocksMotion() && state.getDestroySpeed(level, pos) >= 0.0F;
	}

	private static void addBox(Set<BlockPos> blocks, double x, double y, double z, double half, double height) {
		int minX = Mth.floor(x - half);
		int maxX = Mth.floor(x + half);
		int minY = Mth.floor(y);
		int maxY = Mth.floor(y + height - 0.001);
		int minZ = Mth.floor(z - half);
		int maxZ = Mth.floor(z + half);
		for (int bx = minX; bx <= maxX; bx++) {
			for (int by = minY; by <= maxY; by++) {
				for (int bz = minZ; bz <= maxZ; bz++) {
					blocks.add(new BlockPos(bx, by, bz));
				}
			}
		}
	}

	public static void selfCheck() {
		double[] down = direction(0.0F, 90, 1.0F);
		double[] mild = direction(0.0F, 15, 1.0F);
		double[] mid = direction(0.0F, 30, 1.0F);
		double[] steep = direction(0.0F, 45, 1.0F);
		if (Math.abs(down[1] + 1.0) > 0.001 || Math.abs(down[0]) > 0.001 || Math.abs(down[2]) > 0.001) {
			throw new IllegalStateException("vertical drill did not point down");
		}
		if (!(mild[1] < 0.0 && mid[1] < mild[1] && steep[1] < mid[1] && down[1] < steep[1])) {
			throw new IllegalStateException("drill slopes are out of order");
		}
		Set<BlockPos> shaft = bore(0.5, 10.0, 0.5, 0.0F, 90, 1.0F);
		if (!shaft.contains(new BlockPos(0, 9, 0)) || !shaft.contains(new BlockPos(0, 8, 0))) {
			throw new IllegalStateException("vertical drill missed the shaft");
		}
		if (shaft.contains(new BlockPos(0, 13, 0)) || shaft.contains(new BlockPos(4, 9, 0)) || !bore(0.5, 10.0, 0.5, 0.0F, 0, 1.0F).isEmpty()) {
			throw new IllegalStateException("drill bore box is wrong");
		}
		if (!bore(0.5, 10.0, 0.5, 0.0F, 45, 1.0F).contains(new BlockPos(0, 8, 2))) {
			throw new IllegalStateException("45 degree drill missed the slope");
		}
		double[] back = rewind(9.5, 9.5, 0.5, 0.0F, 90, 0.5F);
		if (Math.abs(back[1] - 10.0) > 0.001) {
			throw new IllegalStateException("drill did not rewind to the blocks it drove through");
		}
	}
}
