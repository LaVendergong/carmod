package com.carmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public class RoadPaverItem extends BlockItem {
	public RoadPaverItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
		lines.accept(Component.translatable("block.carmod.road_paver.tooltip").withStyle(ChatFormatting.GRAY));
		lines.accept(Component.translatable("block.carmod.road_paver.tooltip2").withStyle(ChatFormatting.GRAY));
	}
}
