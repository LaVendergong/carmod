package com.carmod.registry;

import java.util.Set;
import java.util.function.Function;

import com.carmod.CarMod;
import com.carmod.block.RoadBlock;
import com.carmod.block.RoadPaverBlock;
import com.carmod.block.RoadPaverBlockEntity;
import com.carmod.block.ShieldBlock;
import com.carmod.block.TrafficLightBlock;
import com.carmod.block.TrafficLightBlockEntity;
import com.carmod.block.WhiteLineBlock;
import com.carmod.block.ZebraBlock;
import com.carmod.item.DescribedBlockItem;
import com.carmod.item.RoadPaverItem;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
	public static final Block ROAD = registerBlock("road", RoadBlock::new, BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.STONE));
	public static final Block WHITE_LINE = registerBlock("white_line", WhiteLineBlock::new, BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.STONE));
	public static final Block ZEBRA = registerBlock("zebra", ZebraBlock::new, BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.STONE));
	public static final Block TRAFFIC_LIGHT = registerBlock("traffic_light", TrafficLightBlock::new, BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL));
	public static final Block ROAD_PAVER = registerBlock("road_paver", RoadPaverBlock::new, BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL));
	public static final Block SHIELD = registerBlock("shield", ShieldBlock::new, BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL));
	public static final BlockEntityType<RoadPaverBlockEntity> ROAD_PAVER_BE = registerPaverEntity();
	public static final BlockEntityType<TrafficLightBlockEntity> TRAFFIC_LIGHT_BE = registerLightEntity();
	public static final Item ROAD_ITEM = registerItem("road", properties -> new BlockItem(ROAD, properties), new Item.Properties().useBlockDescriptionPrefix());
	public static final Item WHITE_LINE_ITEM = registerItem("white_line", properties -> new BlockItem(WHITE_LINE, properties), new Item.Properties().useBlockDescriptionPrefix());
	public static final Item ZEBRA_ITEM = registerItem("zebra", properties -> new BlockItem(ZEBRA, properties), new Item.Properties().useBlockDescriptionPrefix());
	public static final Item TRAFFIC_LIGHT_ITEM = registerItem("traffic_light", properties -> new DescribedBlockItem(TRAFFIC_LIGHT, properties, "block.carmod.traffic_light.tooltip"), new Item.Properties().useBlockDescriptionPrefix());
	public static final Item ROAD_PAVER_ITEM = registerItem("road_paver", properties -> new RoadPaverItem(ROAD_PAVER, properties), new Item.Properties().useBlockDescriptionPrefix());
	public static final Item SHIELD_ITEM = registerItem("shield", properties -> new DescribedBlockItem(SHIELD, properties, "block.carmod.shield.tooltip"), new Item.Properties().useBlockDescriptionPrefix());

	private ModBlocks() {
	}

	private static Block registerBlock(String path, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, CarMod.id(path));
		Block block = factory.apply(properties.setId(key));
		return Registry.register(BuiltInRegistries.BLOCK, key, block);
	}

	private static BlockEntityType<RoadPaverBlockEntity> registerPaverEntity() {
		ResourceKey<BlockEntityType<?>> key = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, CarMod.id("road_paver"));
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, new BlockEntityType<>(RoadPaverBlockEntity::new, Set.of(ROAD_PAVER)));
	}

	private static BlockEntityType<TrafficLightBlockEntity> registerLightEntity() {
		ResourceKey<BlockEntityType<?>> key = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, CarMod.id("traffic_light"));
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, new BlockEntityType<>(TrafficLightBlockEntity::new, Set.of(TRAFFIC_LIGHT)));
	}

	private static Item registerItem(String path, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, CarMod.id(path));
		Item item = factory.apply(properties.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> {
			output.accept(ROAD_ITEM);
			output.accept(WHITE_LINE_ITEM);
			output.accept(ZEBRA_ITEM);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(output -> {
			output.accept(ROAD_PAVER_ITEM);
			output.accept(SHIELD_ITEM);
			output.accept(TRAFFIC_LIGHT_ITEM);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(ROAD_ITEM);
			output.accept(WHITE_LINE_ITEM);
			output.accept(ZEBRA_ITEM);
			output.accept(TRAFFIC_LIGHT_ITEM);
			output.accept(ROAD_PAVER_ITEM);
			output.accept(SHIELD_ITEM);
		});
	}
}
