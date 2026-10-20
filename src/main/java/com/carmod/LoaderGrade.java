package com.carmod;

import java.util.HashSet;
import java.util.Set;

import com.carmod.entity.LoaderEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blocks a lowered bucket removes. The hitbox is a square and does not rotate,
 * so a turn drives its side into ground the blade has not reached yet.
 * ponytail: the body square is the hitbox plus a small margin. Holes are left alone.
 */
public final class LoaderGrade {
	public static final int HEIGHT = 5;

	private LoaderGrade() {
	}

	public static Set<BlockPos> targets(double x, double y, double z, float yaw) {
		Set<BlockPos> blocks = new HashSet<>();
		addBlade(blocks, x, y, z, yaw);
		addBody(blocks, x, y, z);
		return blocks;
	}

	private static void addBlade(Set<BlockPos> blocks, double x, double y, double z, float yaw) {
		double radians = Math.toRadians(yaw);
		double forwardX = -Math.sin(radians);
		double forwardZ = Math.cos(radians);
		double rightX = Math.cos(radians);
		double rightZ = Math.sin(radians);
		int feet = Mth.floor(y);
		for (int ahead = 0; ahead < 3; ahead++) {
			for (int side = -2; side <= 2; side++) {
				double distance = 1.9 + ahead;
				int bx = Mth.floor(x + forwardX * distance + rightX * side);
				int bz = Mth.floor(z + forwardZ * distance + rightZ * side);
				addColumn(blocks, bx, feet, bz);
			}
		}
	}

	private static void addBody(Set<BlockPos> blocks, double x, double y, double z) {
		double reach = LoaderEntity.WIDTH * 0.5 + 0.8;
		int feet = Mth.floor(y);
		int minX = Mth.floor(x - reach);
		int maxX = Mth.floor(x + reach);
		int minZ = Mth.floor(z - reach);
		int maxZ = Mth.floor(z + reach);
		for (int bx = minX; bx <= maxX; bx++) {
			for (int bz = minZ; bz <= maxZ; bz++) {
				addColumn(blocks, bx, feet, bz);
			}
		}
	}

	private static void addColumn(Set<BlockPos> blocks, int x, int feet, int z) {
		for (int up = 0; up < HEIGHT; up++) {
			blocks.add(new BlockPos(x, feet + up, z));
		}
	}

	public static double originX(double x, float yaw, float speed) {
		return x - (-Math.sin(Math.toRadians(yaw))) * speed;
	}

	public static double originZ(double z, float yaw, float speed) {
		return z - Math.cos(Math.toRadians(yaw)) * speed;
	}

	public static boolean canScrape(BlockState state, BlockGetter level, BlockPos pos) {
		return state.blocksMotion() && state.getDestroySpeed(level, pos) >= 0.0F;
	}

	public static void selfCheck() {
		Set<BlockPos> blocks = targets(0.5, 10.0, 0.5, 0.0F);
		if (!blocks.contains(new BlockPos(0, 10, 2)) || !blocks.contains(new BlockPos(0, 14, 2)) || !blocks.contains(new BlockPos(2, 10, 0)) || !blocks.contains(new BlockPos(-2, 10, 0))) {
			throw new IllegalStateException("loader missed the road in front");
		}
		if (blocks.contains(new BlockPos(0, 15, 0)) || blocks.contains(new BlockPos(0, 9, 0)) || blocks.contains(new BlockPos(4, 10, 0)) || blocks.contains(new BlockPos(0, 10, 5))) {
			throw new IllegalStateException("loader grade box is too big");
		}
		if (Math.abs(originX(0.5, 0.0F, 1.2F) - 0.5) > 0.0001 || Math.abs(originZ(1.7, 0.0F, 1.2F) - 0.5) > 0.0001) {
			throw new IllegalStateException("loader did not rewind to the blocks it drove through");
		}
	}
}
