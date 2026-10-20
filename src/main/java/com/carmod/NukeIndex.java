package com.carmod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.carmod.entity.NukeEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

/** Placed warheads. Remembered even after their chunk unloads, so N still works from far away. */
public final class NukeIndex {
	public record Charge(String dimension, int x, int y, int z, UUID owner) {
	}

	private static final Map<String, Charge> CHARGES = new LinkedHashMap<>();
	private static MinecraftServer server;

	private NukeIndex() {
	}

	public static void load(MinecraftServer server) {
		NukeIndex.server = server;
		CHARGES.clear();
		Path file = file(server);
		if (!Files.exists(file)) {
			return;
		}
		try {
			for (String line : Files.readAllLines(file)) {
				Charge charge = parse(line);
				if (charge != null) {
					CHARGES.put(key(charge), charge);
				}
			}
		} catch (IOException exception) {
			CarMod.LOGGER.warn("Could not read placed nukes", exception);
		}
	}

	public static void add(NukeEntity nuke) {
		if (!(nuke.level() instanceof ServerLevel level) || nuke.owner() == null) {
			return;
		}
		BlockPos pos = nuke.blockPosition();
		Charge charge = new Charge(dimensionId(level), pos.getX(), pos.getY(), pos.getZ(), nuke.owner());
		if (charge.equals(CHARGES.put(key(charge), charge))) {
			return;
		}
		save();
	}

	public static void remove(NukeEntity nuke) {
		if (!(nuke.level() instanceof ServerLevel level)) {
			return;
		}
		BlockPos pos = nuke.blockPosition();
		if (CHARGES.remove(key(dimensionId(level), pos.getX(), pos.getY(), pos.getZ())) != null) {
			save();
		}
	}

	public static void detonateNearest(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel here) || server == null) {
			return;
		}
		Charge charge = nearest(new ArrayList<>(CHARGES.values()), dimensionId(here), player.getUUID(), player.getX(), player.getY(), player.getZ());
		if (charge == null) {
			player.sendOverlayMessage(Component.translatable("message.carmod.nuke.none"));
			return;
		}
		ServerLevel level = find(charge.dimension);
		if (level == null) {
			player.sendOverlayMessage(Component.translatable("message.carmod.nuke.none"));
			return;
		}
		level.getChunkAt(new BlockPos(charge.x, charge.y, charge.z));
		AABB box = new AABB(charge.x, charge.y, charge.z, charge.x + 1.0, charge.y + 1.0, charge.z + 1.0);
		for (NukeEntity nuke : level.getEntitiesOfClass(NukeEntity.class, box)) {
			nuke.discard();
		}
		CHARGES.remove(key(charge));
		save();
		NukeBlast.start(level, charge.x + 0.5, charge.y + 0.5, charge.z + 0.5, player);
		player.sendOverlayMessage(Component.translatable("message.carmod.nuke.boom"));
	}

	static Charge nearest(List<Charge> charges, String dimension, UUID owner, double x, double y, double z) {
		Charge best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Charge charge : charges) {
			if (!charge.dimension.equals(dimension) || !charge.owner.equals(owner)) {
				continue;
			}
			double dx = charge.x + 0.5 - x;
			double dy = charge.y + 0.5 - y;
			double dz = charge.z + 0.5 - z;
			double distance = dx * dx + dy * dy + dz * dz;
			if (distance < bestDistance) {
				bestDistance = distance;
				best = charge;
			}
		}
		return best;
	}

	public static void selfCheck() {
		UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID other = UUID.fromString("00000000-0000-0000-0000-000000000002");
		Charge far = new Charge("minecraft:overworld", 0, 64, 1000, owner);
		Charge near = new Charge("minecraft:overworld", 10, 64, 0, owner);
		Charge nether = new Charge("minecraft:the_nether", 0, 64, 0, owner);
		Charge theirs = new Charge("minecraft:overworld", 1, 64, 0, other);
		if (nearest(List.of(far, nether, theirs, near), "minecraft:overworld", owner, 0.0, 64.0, 0.0) != near) {
			throw new IllegalStateException("nuke remote picked the wrong warhead");
		}
	}

	private static ServerLevel find(String dimension) {
		for (ServerLevel level : server.getAllLevels()) {
			if (dimensionId(level).equals(dimension)) {
				return level;
			}
		}
		return null;
	}

	private static String dimensionId(Level level) {
		return level.dimension().identifier().toString();
	}

	private static String key(Charge charge) {
		return key(charge.dimension, charge.x, charge.y, charge.z);
	}

	private static String key(String dimension, int x, int y, int z) {
		return dimension + " " + x + " " + y + " " + z;
	}

	private static Charge parse(String line) {
		String[] parts = line.trim().split(" ");
		if (parts.length != 5) {
			return null;
		}
		try {
			return new Charge(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), UUID.fromString(parts[4]));
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private static void save() {
		if (server == null) {
			return;
		}
		List<String> lines = new ArrayList<>();
		for (Charge charge : CHARGES.values()) {
			lines.add(charge.dimension + " " + charge.x + " " + charge.y + " " + charge.z + " " + charge.owner);
		}
		try {
			Files.write(file(server), lines);
		} catch (IOException exception) {
			CarMod.LOGGER.warn("Could not save placed nukes", exception);
		}
	}

	private static Path file(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("carmod-nukes.txt");
	}
}
