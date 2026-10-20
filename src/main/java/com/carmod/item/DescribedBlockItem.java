package com.carmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public class DescribedBlockItem extends BlockItem {
	private final String tooltip;

	public DescribedBlockItem(Block block, Properties properties, String tooltip) {
		super(block, properties);
		this.tooltip = tooltip;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
		lines.accept(Component.translatable(this.tooltip).withStyle(ChatFormatting.GRAY));
	}
}
