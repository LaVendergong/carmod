package com.carmod.registry;

import com.carmod.CarMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	public static final SoundEvent ENGINE = register("engine");
	public static final SoundEvent IGNITION = register("ignition");
	public static final SoundEvent IMPACT = register("impact");
	public static final SoundEvent CRASH = register("crash");
	public static final SoundEvent ROTOR = register("rotor");
	public static final SoundEvent ROTOR_START = register("rotor_start");
	public static final SoundEvent JET = register("jet");

	private ModSounds() {
	}

	private static SoundEvent register(String path) {
		ResourceKey<SoundEvent> key = ResourceKey.create(Registries.SOUND_EVENT, CarMod.id(path));
		return Registry.register(BuiltInRegistries.SOUND_EVENT, key, SoundEvent.createVariableRangeEvent(key.identifier()));
	}

	public static void init() {
	}
}
