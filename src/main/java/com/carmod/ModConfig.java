package com.carmod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Reads and writes config/carmod.json. Missing keys keep their defaults.
 * A change from the in-game screen is saved immediately and used on the next hit.
 */
public final class ModConfig {
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(ModConfig.class, (InstanceCreator<ModConfig>) type -> new ModConfig())
			.create();
	private static ModConfig current = new ModConfig();

	public int impactVolume = 100;
	public boolean thirdPerson = true;

	private ModConfig() {
	}

	/** 0 is silent. 1 is full volume. */
	public static float impactVolume() {
		return current.impactVolume / 100.0F;
	}

	public static void setImpactVolume(int percent) {
		int clamped = Math.clamp(percent, 0, 100);
		if (clamped == current.impactVolume) {
			return;
		}
		current.impactVolume = clamped;
		save();
	}

	public static boolean thirdPerson() {
		return current.thirdPerson;
	}

	public static void setThirdPerson(boolean enabled) {
		if (enabled == current.thirdPerson) {
			return;
		}
		current.thirdPerson = enabled;
		save();
	}

	public static void load() {
		Path path = path();
		if (!Files.exists(path)) {
			current = new ModConfig();
			save();
			return;
		}
		try {
			String text = Files.readString(path);
			JsonElement element = JsonParser.parseString(text);
			ModConfig read = GSON.fromJson(element, ModConfig.class);
			current = read == null ? new ModConfig() : read;
			int volume = volumeIn(element, read);
			if (current.impactVolume != volume) {
				current.impactVolume = volume;
				save();
			}
		} catch (Exception exception) {
			CarMod.LOGGER.warn("Could not read config, using defaults", exception);
			current = new ModConfig();
		}
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(CarMod.MOD_ID + ".json");
	}

	private static void save() {
		try {
			Files.writeString(path(), GSON.toJson(current));
		} catch (IOException exception) {
			CarMod.LOGGER.warn("Could not write config", exception);
		}
	}

	/** Old configs stored impactSound. A missing volume keeps 100, and a stored false becomes 0. */
	private static int volumeIn(JsonElement element, ModConfig read) {
		int volume = read == null ? 100 : read.impactVolume;
		if (element instanceof JsonObject object && object.has("impactSound") && !object.has("impactVolume")) {
			volume = object.get("impactSound").getAsBoolean() ? 100 : 0;
		}
		return Math.clamp(volume, 0, 100);
	}

	public static void selfCheck() {
		if (volumeIn(JsonParser.parseString("{}"), GSON.fromJson("{}", ModConfig.class)) != 100
				|| volumeIn(JsonParser.parseString("{\"impactSound\":false}"), GSON.fromJson("{\"impactSound\":false}", ModConfig.class)) != 0
				|| volumeIn(JsonParser.parseString("{\"impactSound\":true}"), GSON.fromJson("{\"impactSound\":true}", ModConfig.class)) != 100
				|| volumeIn(JsonParser.parseString("{\"impactVolume\":40}"), GSON.fromJson("{\"impactVolume\":40}", ModConfig.class)) != 40
				|| volumeIn(JsonParser.parseString("{\"impactSound\":false,\"impactVolume\":70}"), GSON.fromJson("{\"impactSound\":false,\"impactVolume\":70}", ModConfig.class)) != 70
				|| volumeIn(JsonParser.parseString("{\"impactVolume\":150}"), GSON.fromJson("{\"impactVolume\":150}", ModConfig.class)) != 100
				|| !GSON.fromJson("{}", ModConfig.class).thirdPerson
				|| GSON.fromJson("{\"thirdPerson\":false}", ModConfig.class).thirdPerson) {
			throw new IllegalStateException("config defaults failed");
		}
	}
}
