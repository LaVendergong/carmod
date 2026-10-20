package com.carmod.item;

import java.util.function.Consumer;

import com.carmod.entity.NukeEntity;
import com.carmod.registry.ModEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class NukeItem extends Item {
	public NukeItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		var pos = context.getClickedPos().relative(context.getClickedFace());
		BlockState state = level.getBlockState(pos);
		if (!state.canBeReplaced()) {
			return InteractionResult.FAIL;
		}
		NukeEntity nuke = new NukeEntity(ModEntities.NUKE, level);
		nuke.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		if (!level.noCollision(nuke)) {
			return InteractionResult.FAIL;
		}
		Player player = context.getPlayer();
		if (player != null) {
			nuke.setOwner(player.getUUID());
		}
		if (!level.isClientSide()) {
			level.addFreshEntity(nuke);
			if (player == null || !player.getAbilities().instabuild) {
				context.getItemInHand().shrink(1);
			}
			if (player != null) {
				player.sendOverlayMessage(Component.translatable("message.carmod.nuke.armed"));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
		lines.accept(Component.translatable("item.carmod.nuke.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
