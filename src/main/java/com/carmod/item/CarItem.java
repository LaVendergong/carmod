package com.carmod.item;

import java.util.function.Consumer;
import java.util.function.Function;

import com.carmod.entity.AmphibiousEntity;
import com.carmod.entity.CarEntity;
import com.carmod.entity.DrillEntity;
import com.carmod.entity.DumpTruckEntity;
import com.carmod.entity.HelicopterEntity;
import com.carmod.entity.JetEntity;
import com.carmod.entity.LoaderEntity;
import com.carmod.entity.RaceEntity;
import com.carmod.registry.ModEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
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

public class CarItem extends Item {
	private final Function<Level, CarEntity> factory;
	private final String tooltipKey;

	private CarItem(Properties properties, Function<Level, CarEntity> factory, String tooltipKey) {
		super(properties);
		this.factory = factory;
		this.tooltipKey = tooltipKey;
	}

	public static CarItem car(Properties properties) {
		return new CarItem(properties, level -> new CarEntity(ModEntities.CAR, level), "item.carmod.car.tooltip");
	}

	public static CarItem loader(Properties properties) {
		return new CarItem(properties, level -> new LoaderEntity(ModEntities.LOADER, level), "item.carmod.loader.tooltip");
	}

	public static CarItem dumpTruck(Properties properties) {
		return new CarItem(properties, level -> new DumpTruckEntity(ModEntities.DUMP_TRUCK, level), "item.carmod.dump_truck.tooltip");
	}

	public static CarItem drill(Properties properties) {
		return new CarItem(properties, level -> new DrillEntity(ModEntities.DRILL, level), "item.carmod.drill.tooltip");
	}

	public static CarItem amphibious(Properties properties) {
		return new CarItem(properties, level -> new AmphibiousEntity(ModEntities.AMPHIBIOUS, level), "item.carmod.amphibious.tooltip");
	}

	public static CarItem racer(Properties properties) {
		return new CarItem(properties, level -> new RaceEntity(ModEntities.RACER, level), "item.carmod.racer.tooltip");
	}

	public static CarItem helicopter(Properties properties) {
		return new CarItem(properties, level -> new HelicopterEntity(ModEntities.HELICOPTER, level), "item.carmod.helicopter.tooltip");
	}

	public static CarItem jet(Properties properties) {
		return new CarItem(properties, level -> new JetEntity(ModEntities.JET, level), "item.carmod.jet.tooltip");
	}

	public static CarItem armedHelicopter(Properties properties) {
		return new CarItem(properties, level -> {
			HelicopterEntity helicopter = new HelicopterEntity(ModEntities.HELICOPTER, level);
			helicopter.setArmed(true);
			return helicopter;
		}, "item.carmod.armed_helicopter.tooltip");
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		var pos = context.getClickedPos().relative(context.getClickedFace());
		BlockState state = level.getBlockState(pos);
		if (!state.canBeReplaced()) {
			return InteractionResult.FAIL;
		}

		ItemStack stack = context.getItemInHand();
		CarEntity car = this.factory.apply(level);
		car.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		car.setYRot(context.getHorizontalDirection().getOpposite().toYRot());
		car.wear = stack.getDamageValue();
		car.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
		if (!level.noCollision(car)) {
			return InteractionResult.FAIL;
		}
		if (!level.isClientSide()) {
			level.addFreshEntity(car);
			Player player = context.getPlayer();
			if (player == null || !player.getAbilities().instabuild) {
				stack.shrink(1);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
		lines.accept(Component.translatable(this.tooltipKey).withStyle(ChatFormatting.GRAY));
	}
}
