package com.carmod.block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.carmod.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class RoadPaverBlockEntity extends BlockEntity {
	private static final Set<Block> SOFT = Set.of(
			Blocks.GRASS_BLOCK,
			Blocks.DIRT,
			Blocks.COARSE_DIRT,
			Blocks.ROOTED_DIRT,
			Blocks.DIRT_PATH,
			Blocks.PODZOL,
			Blocks.FARMLAND,
			Blocks.SAND,
			Blocks.RED_SAND,
			Blocks.GRAVEL);

	private int distance;
	private int width = 3;
	private boolean armed;
	private int cooldown = RoadPlan.STEP_TICKS;

	public RoadPaverBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.ROAD_PAVER_BE, pos, state);
	}

	public void cycleWidth(Player player) {
		this.width = this.width >= 9 ? 1 : this.width + 1;
		this.setChanged();
		player.sendOverlayMessage(Component.translatable("block.carmod.road_paver.width", this.width));
	}

	public void serverTick() {
		if (!(this.getLevel() instanceof ServerLevel level)) {
			return;
		}
		if (this.cooldown > 0) {
			this.cooldown--;
			return;
		}
		if (this.distance >= RoadPlan.MAX_DISTANCE) {
			return;
		}
		BlockPos pos = this.getBlockPos();
		if (level.hasNeighborSignal(pos) && !this.armed) {
			this.armed = true;
			this.setChanged();
		}
		if (!this.armed) {
			return;
		}
		switch (this.step(level)) {
			case MOVED -> {
			}
			case WAIT -> this.cooldown = RoadPlan.STEP_TICKS;
			case STOP -> {
				this.cooldown = RoadPlan.STEP_TICKS;
				if (!level.hasNeighborSignal(pos)) {
					this.armed = false;
					this.setChanged();
				}
			}
		}
	}

	private StepResult step(ServerLevel level) {
		Direction facing = this.getBlockState().getValue(RoadPaverBlock.FACING);
		BlockPos pos = this.getBlockPos();
		Direction side = facing.getClockWise();
		int width = Math.max(1, Math.min(9, this.width));
		int start = -(width / 2);
		BlockPos ahead = pos.relative(facing);
		List<BlockPos> columns = new ArrayList<>();
		for (int i = 0; i < width; i++) {
			int offset = start + i;
			if (offset != 0) {
				columns.add(pos.relative(side, offset));
			}
		}
		List<BlockPos> shields = this.connectedShields(level, pos, facing);
		if (this.outOfWorld(level, ahead, columns, shields, facing)) {
			return StepResult.STOP;
		}
		if (!this.areaLoaded(level, pos, ahead, columns, shields, facing)) {
			return StepResult.WAIT;
		}

		boolean follow = shields.contains(ahead);
		boolean cutting = !shields.isEmpty();
		RoadPlan.Cell leave = RoadPlan.pave(RoadPlan.Kind.AIR, this.kindAt(level, pos.below()), this.kindAt(level, pos.below(2)));
		RoadPlan.Kind aheadKind = follow ? RoadPlan.Kind.AIR : this.kindAt(level, ahead);
		RoadPlan.Cell into = RoadPlan.boreEnter(cutting, aheadKind, this.kindAt(level, ahead.below()), this.kindAt(level, ahead.below(2)));
		List<BlockPos> paving = new ArrayList<>();
		List<RoadPlan.Cell> pavingCells = new ArrayList<>();
		int cost = leave.cost() + into.cost();
		for (BlockPos column : columns) {
			boolean shieldHere = shields.contains(column);
			RoadPlan.Cell cell = shieldHere
					? RoadPlan.pave(RoadPlan.Kind.AIR, this.kindAt(level, column.below()), this.kindAt(level, column.below(2)))
					: this.plan(level, column);
			if (cell.stop() || (!shieldHere && this.blockedHeadroom(level, column, shields))) {
				continue;
			}
			paving.add(column);
			pavingCells.add(cell);
			cost += cell.cost();
		}
		if (leave.stop() || into.stop()) {
			return StepResult.STOP;
		}
		// ponytail: with shields attached, the ice in front is the bore, not a low ceiling. Open-road headroom still applies when boring alone.
		if (!cutting && (this.blockedHeadroom(level, pos, shields) || this.blockedHeadroom(level, ahead, shields))) {
			return StepResult.STOP;
		}
		for (BlockPos shield : shields) {
			if (this.blockedDest(level, shield.relative(facing), shields, pos)) {
				return StepResult.STOP;
			}
		}
		if (cutting && !shields.contains(ahead) && this.blockedDest(level, ahead, shields, pos)) {
			return StepResult.STOP;
		}

		Supply supply = this.takeRoad(level, pos, facing, cost);
		if (supply == Supply.UNLOADED) {
			return StepResult.WAIT;
		}
		if (supply == Supply.MISSING) {
			return StepResult.STOP;
		}

		for (BlockPos shield : shields) {
			this.harvest(level, shield.relative(facing), shield, facing, shields, pos);
		}
		if (cutting && !shields.contains(ahead)) {
			this.harvest(level, ahead, pos, facing, shields, pos);
		}
		this.moveShields(level, shields, facing);
		for (int i = 0; i < paving.size(); i++) {
			BlockPos column = paving.get(i);
			if (level.getBlockState(column).is(ModBlocks.SHIELD) || level.getBlockState(column).is(ModBlocks.ROAD_PAVER)) {
				continue;
			}
			this.pave(level, column, pavingCells.get(i));
		}
		if (follow) {
			if (into.fill() && !level.getBlockState(ahead.below()).is(ModBlocks.ROAD)) {
				level.setBlock(ahead.below(), ModBlocks.ROAD.defaultBlockState(), Block.UPDATE_ALL);
			}
		} else {
			this.open(level, ahead, into);
		}
		if (leave.fill() && !level.getBlockState(pos.below()).is(ModBlocks.ROAD)) {
			level.setBlock(pos.below(), ModBlocks.ROAD.defaultBlockState(), Block.UPDATE_ALL);
		}
		this.clearColumn(level, pos, false);

		BlockState carried = this.getBlockState();
		int nextDistance = this.distance + 1;
		int nextWidth = width;
		level.setBlock(ahead, carried, Block.UPDATE_ALL);
		level.setBlock(pos, ModBlocks.ROAD.defaultBlockState(), Block.UPDATE_ALL);
		if (level.getBlockEntity(ahead) instanceof RoadPaverBlockEntity moved) {
			moved.distance = nextDistance;
			moved.width = nextWidth;
			moved.armed = true;
			moved.cooldown = RoadPlan.STEP_TICKS;
			moved.setChanged();
		}
		return StepResult.MOVED;
	}

	private RoadPlan.Cell plan(ServerLevel level, BlockPos pos) {
		return RoadPlan.pave(this.kindAt(level, pos), this.kindAt(level, pos.below()), this.kindAt(level, pos.below(2)));
	}

	private void pave(ServerLevel level, BlockPos pos, RoadPlan.Cell cell) {
		if (cell.clear()) {
			this.clearColumn(level, pos, true);
		}
		if (cell.fill() && !level.getBlockState(pos.below()).is(ModBlocks.ROAD)) {
			level.setBlock(pos.below(), ModBlocks.ROAD.defaultBlockState(), Block.UPDATE_ALL);
		}
		if (!level.getBlockState(pos).is(ModBlocks.ROAD)) {
			level.setBlock(pos, ModBlocks.ROAD.defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	private void open(ServerLevel level, BlockPos pos, RoadPlan.Cell cell) {
		if (cell.clear()) {
			this.clearColumn(level, pos, true);
		}
		if (cell.fill() && !level.getBlockState(pos.below()).is(ModBlocks.ROAD)) {
			level.setBlock(pos.below(), ModBlocks.ROAD.defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	// ponytail: at most 48 connected shields, and each one only clears the single block directly in front. Stack more shields for a taller face.
	private List<BlockPos> connectedShields(ServerLevel level, BlockPos paverPos, Direction facing) {
		List<BlockPos> found = new ArrayList<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (Direction dir : Direction.values()) {
			BlockPos next = paverPos.relative(dir);
			if (this.forwardOf(paverPos, next, facing) >= 0 && this.isShield(level, next, facing)) {
				queue.add(next);
			}
		}
		Set<BlockPos> seen = new HashSet<>();
		while (!queue.isEmpty() && found.size() < 48) {
			BlockPos current = queue.removeFirst();
			if (!seen.add(current) || this.forwardOf(paverPos, current, facing) < 0 || !this.isShield(level, current, facing)) {
				continue;
			}
			found.add(current);
			for (Direction dir : Direction.values()) {
				queue.add(current.relative(dir));
			}
		}
		found.sort((a, b) -> Integer.compare(this.forwardOf(paverPos, b, facing), this.forwardOf(paverPos, a, facing)));
		return found;
	}

	private boolean isShield(ServerLevel level, BlockPos pos, Direction facing) {
		BlockState state = level.getBlockState(pos);
		return state.is(ModBlocks.SHIELD) && state.getValue(ShieldBlock.FACING) == facing;
	}

	private int forwardOf(BlockPos origin, BlockPos pos, Direction facing) {
		return (pos.getX() - origin.getX()) * facing.getStepX() + (pos.getZ() - origin.getZ()) * facing.getStepZ();
	}

	private boolean blockedDest(ServerLevel level, BlockPos dest, List<BlockPos> shields, BlockPos paverPos) {
		if (dest.equals(paverPos) || shields.contains(dest)) {
			return false;
		}
		BlockState state = level.getBlockState(dest);
		if (state.isAir()) {
			return false;
		}
		if (state.is(ModBlocks.SHIELD) || state.is(ModBlocks.ROAD_PAVER)) {
			return true;
		}
		return state.getDestroySpeed(level, dest) < 0.0F;
	}

	private void harvest(ServerLevel level, BlockPos dest, BlockPos shieldPos, Direction facing, List<BlockPos> shields, BlockPos paverPos) {
		if (dest.equals(paverPos) || shields.contains(dest)) {
			return;
		}
		BlockState state = level.getBlockState(dest);
		if (state.isAir()) {
			return;
		}
		List<ItemStack> drops = new ArrayList<>();
		BlockEntity blockEntity = level.getBlockEntity(dest);
		if (blockEntity instanceof Container container) {
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (!stack.isEmpty()) {
					drops.add(stack.copy());
					container.setItem(slot, ItemStack.EMPTY);
				}
			}
		}
		drops.addAll(Block.getDrops(state, level, dest, blockEntity, null, silkPick(level)));
		level.setBlock(dest, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		this.keep(level, shieldPos, facing, drops, paverPos);
	}

	private static ItemStack silkPick(ServerLevel level) {
		ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
		tool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
		return tool;
	}

	private void keep(ServerLevel level, BlockPos shieldPos, Direction facing, List<ItemStack> drops, BlockPos paverPos) {
		Chest chest = this.findChest(level, shieldPos, facing);
		if (chest == null && !shieldPos.equals(paverPos)) {
			chest = this.findChest(level, paverPos, facing);
		}
		for (ItemStack drop : drops) {
			ItemStack left = chest == null ? drop : insert(chest.container, drop);
			if (!left.isEmpty()) {
				Block.popResource(level, chest == null ? shieldPos : chest.pos, left);
			}
		}
	}

	private void moveShields(ServerLevel level, List<BlockPos> shields, Direction facing) {
		for (BlockPos shield : shields) {
			BlockState state = level.getBlockState(shield);
			if (!state.is(ModBlocks.SHIELD)) {
				continue;
			}
			BlockPos dest = shield.relative(facing);
			level.setBlock(dest, state, Block.UPDATE_ALL);
			level.setBlock(shield, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	private boolean outOfWorld(ServerLevel level, BlockPos ahead, List<BlockPos> columns, List<BlockPos> shields, Direction facing) {
		if (level.isOutsideBuildHeight(ahead)) {
			return true;
		}
		for (BlockPos column : columns) {
			if (level.isOutsideBuildHeight(column)) {
				return true;
			}
		}
		for (BlockPos shield : shields) {
			if (level.isOutsideBuildHeight(shield.relative(facing))) {
				return true;
			}
		}
		return false;
	}

	private boolean areaLoaded(ServerLevel level, BlockPos pos, BlockPos ahead, List<BlockPos> columns, List<BlockPos> shields, Direction facing) {
		if (!level.isLoaded(ahead) || !level.isLoaded(pos.below(2)) || !level.isLoaded(ahead.below(2)) || !level.isLoaded(ahead.above(RoadPlan.HEADROOM))) {
			return false;
		}
		for (BlockPos column : columns) {
			if (!level.isLoaded(column) || !level.isLoaded(column.below(2)) || !level.isLoaded(column.above(RoadPlan.HEADROOM))) {
				return false;
			}
		}
		for (BlockPos shield : shields) {
			if (!level.isLoaded(shield.relative(facing))) {
				return false;
			}
		}
		return true;
	}

	private boolean blockedHeadroom(ServerLevel level, BlockPos surface, List<BlockPos> shields) {
		for (int dy = 1; dy <= RoadPlan.HEADROOM; dy++) {
			BlockPos up = surface.above(dy);
			if (shields.contains(up)) {
				continue;
			}
			RoadPlan.Kind kind = this.kindAt(level, up);
			if (kind == RoadPlan.Kind.HARD || kind == RoadPlan.Kind.FLUID) {
				return true;
			}
		}
		return false;
	}

	// ponytail: only the column above each road cell is cleared, and only 16 blocks up. A wider canopy is left to decay.
	private void clearColumn(ServerLevel level, BlockPos surface, boolean includeSurface) {
		int top = 0;
		for (int dy = 1; dy <= RoadPlan.TREE_CLEAR; dy++) {
			RoadPlan.Kind above = this.kindAt(level, surface.above(dy));
			if (above == RoadPlan.Kind.TREE || (dy <= RoadPlan.HEADROOM && above == RoadPlan.Kind.SOFT)) {
				top = dy;
				continue;
			}
			if (above == RoadPlan.Kind.AIR) {
				continue;
			}
			break;
		}
		for (int dy = top; dy >= 1; dy--) {
			BlockPos up = surface.above(dy);
			RoadPlan.Kind above = this.kindAt(level, up);
			if (above == RoadPlan.Kind.TREE || (dy <= RoadPlan.HEADROOM && above == RoadPlan.Kind.SOFT)) {
				level.destroyBlock(up, true, null, 512);
			}
		}
		if (includeSurface) {
			RoadPlan.Kind at = this.kindAt(level, surface);
			if (at == RoadPlan.Kind.SOFT || at == RoadPlan.Kind.TREE) {
				level.destroyBlock(surface, true, null, 512);
			}
		}
	}

	// ponytail: the chest is found by scanning straight back through air and road, at most 128 blocks. A chest farther than that, or off this line, is ignored.
	private Supply takeRoad(ServerLevel level, BlockPos origin, Direction facing, int count) {
		if (count <= 0) {
			return Supply.OK;
		}
		Direction back = facing.getOpposite();
		for (int i = 1; i <= RoadPlan.MAX_DISTANCE; i++) {
			BlockPos cursor = origin.relative(back, i);
			if (!level.isLoaded(cursor)) {
				return Supply.UNLOADED;
			}
			BlockState state = level.getBlockState(cursor);
			Container container = containerAt(level, cursor, state);
			if (container != null) {
				return remove(container, ModBlocks.ROAD_ITEM, count) ? Supply.OK : Supply.MISSING;
			}
			if (!state.isAir() && !isSurface(state)) {
				return Supply.MISSING;
			}
		}
		return Supply.MISSING;
	}

	private static Container containerAt(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof ChestBlock chest) {
			Container combined = ChestBlock.getContainer(chest, state, level, pos, true);
			if (combined != null) {
				return combined;
			}
		}
		if (level.getBlockEntity(pos) instanceof Container container) {
			return container;
		}
		return null;
	}

	private static boolean remove(Container container, Item item, int count) {
		int found = 0;
		int size = container.getContainerSize();
		for (int slot = 0; slot < size; slot++) {
			ItemStack stack = container.getItem(slot);
			if (stack.getItem() == item) {
				found += stack.getCount();
			}
		}
		if (found < count) {
			return false;
		}
		int left = count;
		for (int slot = 0; slot < size && left > 0; slot++) {
			ItemStack stack = container.getItem(slot);
			if (stack.getItem() != item) {
				continue;
			}
			int take = Math.min(left, stack.getCount());
			container.removeItem(slot, take);
			left -= take;
		}
		return left == 0;
	}

	private RoadPlan.Kind kindAt(ServerLevel level, BlockPos pos) {
		if (level.isOutsideBuildHeight(pos)) {
			return RoadPlan.Kind.HARD;
		}
		return classify(level.getBlockState(pos));
	}

	private static RoadPlan.Kind classify(BlockState state) {
		if (!state.getFluidState().isEmpty()) {
			return RoadPlan.Kind.FLUID;
		}
		if (state.isAir()) {
			return RoadPlan.Kind.AIR;
		}
		if (state.is(ModBlocks.ROAD)) {
			return RoadPlan.Kind.ROAD;
		}
		if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
			return RoadPlan.Kind.TREE;
		}
		if (SOFT.contains(state.getBlock()) || state.canBeReplaced()) {
			return RoadPlan.Kind.SOFT;
		}
		return RoadPlan.Kind.HARD;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("Distance", this.distance);
		output.putInt("Width", this.width);
		output.putBoolean("Armed", this.armed);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.distance = input.getIntOr("Distance", 0);
		this.width = input.getIntOr("Width", 3);
		this.armed = input.getBooleanOr("Armed", false);
	}

	private Chest findChest(ServerLevel level, BlockPos origin, Direction facing) {
		Direction back = facing.getOpposite();
		for (int i = 1; i <= RoadPlan.MAX_DISTANCE; i++) {
			BlockPos cursor = origin.relative(back, i);
			if (!level.isLoaded(cursor)) {
				return null;
			}
			BlockState state = level.getBlockState(cursor);
			Container container = containerAt(level, cursor, state);
			if (container != null) {
				return new Chest(cursor, container);
			}
			if (!state.isAir() && !isSurface(state) && !state.is(ModBlocks.ROAD_PAVER) && !state.is(ModBlocks.SHIELD)) {
				return null;
			}
		}
		return null;
	}

	private static boolean isSurface(BlockState state) {
		return state.is(ModBlocks.ROAD) || state.is(ModBlocks.WHITE_LINE) || state.is(ModBlocks.ZEBRA);
	}

	private static ItemStack insert(Container container, ItemStack stack) {
		for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
			for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
				ItemStack present = container.getItem(slot);
				if (pass == 0) {
					if (present.isEmpty() || !ItemStack.isSameItemSameComponents(present, stack)) {
						continue;
					}
					int room = present.getMaxStackSize() - present.getCount();
					if (room <= 0) {
						continue;
					}
					int move = Math.min(room, stack.getCount());
					present.grow(move);
					stack.shrink(move);
					container.setItem(slot, present);
				} else if (present.isEmpty()) {
					container.setItem(slot, stack.split(Math.min(stack.getMaxStackSize(), stack.getCount())));
				}
			}
		}
		return stack;
	}

	private record Chest(BlockPos pos, Container container) {
	}

	private enum StepResult {
		MOVED,
		WAIT,
		STOP
	}

	private enum Supply {
		OK,
		MISSING,
		UNLOADED
	}
}
