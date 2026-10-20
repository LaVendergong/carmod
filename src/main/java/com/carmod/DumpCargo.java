package com.carmod;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Moves a dump truck's cargo into a container, or back out as loose stacks. */
public final class DumpCargo {
	public static final int SLOTS = 27;

	private DumpCargo() {
	}

	public static List<ItemStack> pour(Container from, @Nullable Container into) {
		List<ItemStack> spilled = new ArrayList<>();
		for (int slot = 0; slot < from.getContainerSize(); slot++) {
			ItemStack stack = from.removeItemNoUpdate(slot);
			if (stack.isEmpty()) {
				continue;
			}
			if (into != null) {
				stack = insert(into, stack);
			}
			if (!stack.isEmpty()) {
				spilled.add(stack);
			}
		}
		if (into != null) {
			into.setChanged();
		}
		return spilled;
	}

	public static ItemStack insert(Container dest, ItemStack stack) {
		if (stack.isEmpty()) {
			return stack;
		}
		for (int slot = 0; slot < dest.getContainerSize() && !stack.isEmpty(); slot++) {
			if (!dest.canPlaceItem(slot, stack)) {
				continue;
			}
			ItemStack there = dest.getItem(slot);
			if (there.isEmpty()) {
				int count = Math.min(stack.getCount(), dest.getMaxStackSize(stack));
				dest.setItem(slot, stack.split(count));
			} else if (ItemStack.isSameItemSameComponents(there, stack)) {
				int room = Math.min(there.getMaxStackSize(), dest.getMaxStackSize(there)) - there.getCount();
				int count = Math.min(room, stack.getCount());
				if (count > 0) {
					there.grow(count);
					stack.shrink(count);
					dest.setItem(slot, there);
				}
			}
		}
		return stack;
	}

	public static void selfCheck() {
		int max = new SimpleContainer(1).getMaxStackSize(new ItemStack(Items.COBBLESTONE));
		SimpleContainer truck = new SimpleContainer(2);
		SimpleContainer chest = new SimpleContainer(1);
		truck.setItem(0, new ItemStack(Items.COBBLESTONE, 10));
		truck.setItem(1, new ItemStack(Items.DIRT, 3));
		chest.setItem(0, new ItemStack(Items.COBBLESTONE, max - 4));
		List<ItemStack> spilled = pour(truck, chest);
		if (!truck.isEmpty() || chest.getItem(0).getCount() != max || spilled.size() != 2) {
			throw new IllegalStateException("dump truck did not fill the chest and spill the rest");
		}
		if (!spilled.get(0).is(Items.COBBLESTONE) || spilled.get(0).getCount() != 6 || !spilled.get(1).is(Items.DIRT) || spilled.get(1).getCount() != 3) {
			throw new IllegalStateException("dump truck spilled the wrong stacks");
		}
		SimpleContainer loose = new SimpleContainer(1);
		loose.setItem(0, new ItemStack(Items.DIRT, 3));
		List<ItemStack> dumped = pour(loose, null);
		if (!loose.isEmpty() || dumped.size() != 1 || dumped.get(0).getCount() != 3) {
			throw new IllegalStateException("dump truck did not empty onto the ground");
		}
	}
}
