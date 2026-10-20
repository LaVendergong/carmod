package com.carmod.registry;

import java.util.function.Function;

import com.carmod.CarMod;
import com.carmod.item.CarItem;
import com.carmod.item.NukeItem;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModItems {
	public static final Item CAR = register(ResourceKey.create(Registries.ITEM, CarMod.id("car")), CarItem::car, new Item.Properties().stacksTo(1).durability(100));
	public static final Item LOADER = register(ResourceKey.create(Registries.ITEM, CarMod.id("loader")), CarItem::loader, new Item.Properties().stacksTo(1).durability(100));
	public static final Item DUMP_TRUCK = register(ResourceKey.create(Registries.ITEM, CarMod.id("dump_truck")), CarItem::dumpTruck, new Item.Properties().stacksTo(1).durability(100));
	public static final Item DRILL = register(ResourceKey.create(Registries.ITEM, CarMod.id("drill")), CarItem::drill, new Item.Properties().stacksTo(1).durability(100));
	public static final Item AMPHIBIOUS = register(ResourceKey.create(Registries.ITEM, CarMod.id("amphibious")), CarItem::amphibious, new Item.Properties().stacksTo(1).durability(100));
	public static final Item RACER = register(ResourceKey.create(Registries.ITEM, CarMod.id("racer")), CarItem::racer, new Item.Properties().stacksTo(1).durability(100));
	public static final Item HELICOPTER = register(ResourceKey.create(Registries.ITEM, CarMod.id("helicopter")), CarItem::helicopter, new Item.Properties().stacksTo(1).durability(100));
	public static final Item ARMED_HELICOPTER = register(ResourceKey.create(Registries.ITEM, CarMod.id("armed_helicopter")), CarItem::armedHelicopter, new Item.Properties().stacksTo(1).durability(100));
	public static final Item JET = register(ResourceKey.create(Registries.ITEM, CarMod.id("jet")), CarItem::jet, new Item.Properties().stacksTo(1).durability(100));
	public static final Item NUKE = register(ResourceKey.create(Registries.ITEM, CarMod.id("nuke")), NukeItem::new, new Item.Properties().stacksTo(16));

	private ModItems() {
	}

	private static Item register(ResourceKey<Item> key, Function<Item.Properties, Item> factory, Item.Properties properties) {
		Item item = factory.apply(properties.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(CAR);
			output.accept(LOADER);
			output.accept(DUMP_TRUCK);
			output.accept(DRILL);
			output.accept(AMPHIBIOUS);
			output.accept(RACER);
			output.accept(HELICOPTER);
			output.accept(ARMED_HELICOPTER);
			output.accept(JET);
			output.accept(NUKE);
		});
	}
}
