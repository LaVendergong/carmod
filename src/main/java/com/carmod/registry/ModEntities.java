package com.carmod.registry;

import com.carmod.CarMod;
import com.carmod.entity.AmphibiousEntity;
import com.carmod.entity.CarEntity;
import com.carmod.entity.DrillEntity;
import com.carmod.entity.DumpTruckEntity;
import com.carmod.entity.HelicopterEntity;
import com.carmod.entity.JetEntity;
import com.carmod.entity.LoaderEntity;
import com.carmod.entity.MissileEntity;
import com.carmod.entity.NukeEntity;
import com.carmod.entity.RaceEntity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final EntityType<CarEntity> CAR = register("car", CarEntity::new, 2.6F, 2.5F);
	public static final EntityType<LoaderEntity> LOADER = register("loader", LoaderEntity::new, LoaderEntity.WIDTH, LoaderEntity.HEIGHT);
	public static final EntityType<DumpTruckEntity> DUMP_TRUCK = register("dump_truck", DumpTruckEntity::new, DumpTruckEntity.WIDTH, DumpTruckEntity.HEIGHT);
	public static final EntityType<DrillEntity> DRILL = register("drill", DrillEntity::new, DrillEntity.WIDTH, DrillEntity.HEIGHT);
	public static final EntityType<AmphibiousEntity> AMPHIBIOUS = register("amphibious", AmphibiousEntity::new, AmphibiousEntity.WIDTH, AmphibiousEntity.HEIGHT);
	public static final EntityType<RaceEntity> RACER = register("racer", RaceEntity::new, RaceEntity.WIDTH, RaceEntity.HEIGHT);
	public static final EntityType<HelicopterEntity> HELICOPTER = register("helicopter", HelicopterEntity::new, HelicopterEntity.WIDTH, HelicopterEntity.HEIGHT);
	public static final EntityType<JetEntity> JET = register("jet", JetEntity::new, JetEntity.WIDTH, JetEntity.HEIGHT);
	public static final EntityType<MissileEntity> MISSILE = registerMissile("missile");
	public static final EntityType<NukeEntity> NUKE = registerNuke("nuke");

	private ModEntities() {
	}

	private static <T extends CarEntity> EntityType<T> register(String name, EntityType.EntityFactory<T> factory, float width, float height) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, CarMod.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.of(factory, MobCategory.MISC)
				.sized(width, height)
				.clientTrackingRange(10)
				.updateInterval(1)
				.build(key));
	}

	private static EntityType<MissileEntity> registerMissile(String name) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, CarMod.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.of(MissileEntity::new, MobCategory.MISC)
				.sized(0.5F, 0.5F)
				.clientTrackingRange(8)
				.updateInterval(1)
				.build(key));
	}

	private static EntityType<NukeEntity> registerNuke(String name) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, CarMod.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.of(NukeEntity::new, MobCategory.MISC)
				.sized(0.6F, 1.1F)
				.clientTrackingRange(10)
				.updateInterval(10)
				.build(key));
	}

	public static void init() {
	}
}
