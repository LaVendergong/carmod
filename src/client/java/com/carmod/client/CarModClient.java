package com.carmod.client;

import java.util.HashMap;
import java.util.Map;

import com.carmod.CarClientHooks;
import com.carmod.MissileLock;
import com.carmod.ModConfig;
import com.carmod.entity.CarEntity;
import com.carmod.entity.DrillEntity;
import com.carmod.entity.DumpTruckEntity;
import com.carmod.entity.HelicopterEntity;
import com.carmod.entity.JetEntity;
import com.carmod.entity.LoaderEntity;
import com.carmod.network.CarStatePayload;
import com.carmod.network.DrillPayload;
import com.carmod.network.DumpPayload;
import com.carmod.network.HelicopterCrashPayload;
import com.carmod.network.HelicopterFirePayload;
import com.carmod.network.JetAttitudePayload;
import com.carmod.network.JetCannonPayload;
import com.carmod.network.JetCrashPayload;
import com.carmod.network.JetFirePayload;
import com.carmod.network.NukeDetonatePayload;
import com.carmod.registry.ModEntities;
import com.carmod.registry.ModSounds;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;

public class CarModClient implements ClientModInitializer {
	private static final Map<Integer, AbstractTickableSoundInstance> ENGINES = new HashMap<>();
	private static final KeyMapping BUCKET = new KeyMapping("key.carmod.bucket", InputConstants.KEY_B, KeyMapping.Category.GAMEPLAY);
	private static final KeyMapping DUMP = new KeyMapping("key.carmod.dump", InputConstants.KEY_V, KeyMapping.Category.GAMEPLAY);
	private static final KeyMapping DRILL = new KeyMapping("key.carmod.drill", InputConstants.KEY_G, KeyMapping.Category.GAMEPLAY);
	private static final KeyMapping MISSILE = new KeyMapping("key.carmod.missile", InputConstants.KEY_R, KeyMapping.Category.GAMEPLAY);
	private static final KeyMapping CANNON = new KeyMapping("key.carmod.cannon", InputConstants.KEY_X, KeyMapping.Category.GAMEPLAY);
	private static final KeyMapping NUKE = new KeyMapping("key.carmod.nuke", InputConstants.KEY_N, KeyMapping.Category.GAMEPLAY);
	private static boolean riding;
	private static boolean cameraOverridden;
	private static CameraType cameraBefore;
	private static boolean helicopterCamera;
	private static double cameraDistanceBefore = 4.0;
	private static final double HELICOPTER_CAMERA = 18.0;
	private static final double JET_CAMERA = 22.0;
	private static int missileWait;
	private static int lockHint;
	private static int cannonWait;

	@Override
	public void onInitializeClient() {
		EntityRenderers.register(ModEntities.CAR, CarRenderer::new);
		EntityRenderers.register(ModEntities.LOADER, LoaderRenderer::new);
		EntityRenderers.register(ModEntities.DUMP_TRUCK, DumpTruckRenderer::new);
		EntityRenderers.register(ModEntities.DRILL, DrillRenderer::new);
		EntityRenderers.register(ModEntities.AMPHIBIOUS, AmphibiousRenderer::new);
		EntityRenderers.register(ModEntities.RACER, RaceRenderer::new);
		EntityRenderers.register(ModEntities.HELICOPTER, HelicopterRenderer::new);
		EntityRenderers.register(ModEntities.JET, JetRenderer::new);
		EntityRenderers.register(ModEntities.MISSILE, MissileRenderer::new);
		EntityRenderers.register(ModEntities.NUKE, NukeRenderer::new);
		CarClientHooks.inputReader = car -> {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player == null || player.getVehicle() != car) {
				car.setInput(false, false, false, false);
				car.setHandbrake(false);
				car.setDescend(false);
				return;
			}
			Input keys = player.input.keyPresses;
			var window = Minecraft.getInstance().getWindow();
			boolean descend = InputConstants.isKeyDown(window, InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(window, InputConstants.KEY_RCONTROL);
			car.setInput(keys.left(), keys.right(), keys.forward(), keys.backward());
			car.setHandbrake(keys.jump());
			car.setDescend(descend);
		};
		CarClientHooks.afterLocalDrive = CarModClient::sendState;
		CarClientHooks.impact = car -> playHit(car, ModSounds.IMPACT);
		CarClientHooks.crash = car -> playHit(car, ModSounds.CRASH);
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof PauseScreen)) {
				return;
			}
			Screens.getWidgets(screen).add(Button.builder(Component.translatable("screen.carmod.config"), button ->
					client.setScreenAndShow(new CarConfigScreen(screen)))
					.bounds(4, 4, 100, 20)
					.build());
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null && client.player.getVehicle() instanceof CarEntity car) {
				car.alignPassenger(client.player);
			}
			while (client.gui.screen() == null && BUCKET.consumeClick()) {
				if (client.player != null && client.player.getVehicle() instanceof LoaderEntity loader) {
					loader.setBucketDown(!loader.bucketDown());
					client.player.sendOverlayMessage(Component.translatable(loader.bucketDown() ? "message.carmod.bucket.on" : "message.carmod.bucket.off"));
				}
			}
			while (client.gui.screen() == null && DUMP.consumeClick()) {
				if (client.player != null && client.player.getVehicle() instanceof DumpTruckEntity truck) {
					ClientPlayNetworking.send(new DumpPayload(truck.getId()));
				}
			}
			tickMissile(client);
			tickCannon(client);
			while (client.gui.screen() == null && NUKE.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(new NukeDetonatePayload());
				}
			}
			while (client.gui.screen() == null && DRILL.consumeClick()) {
				if (client.player != null && client.player.getVehicle() instanceof DrillEntity drill) {
					drill.cycle();
					ClientPlayNetworking.send(new DrillPayload(drill.getId(), drill.angle()));
					client.player.sendOverlayMessage(Component.translatable(drill.angleKey()));
				}
			}
			tickCamera(client);
			tickEngines(client);
		});
	}

	private static void tickMissile(Minecraft client) {
		if (missileWait > 0) {
			missileWait--;
		}
		while (client.gui.screen() == null && MISSILE.consumeClick()) {
			if (client.player == null) {
				continue;
			}
			Entity vehicle = client.player.getVehicle();
			if (vehicle instanceof HelicopterEntity helicopter && !helicopter.armed()) {
				client.player.sendOverlayMessage(Component.translatable("message.carmod.helicopter.unarmed"));
				continue;
			}
			if (!(vehicle instanceof HelicopterEntity) && !(vehicle instanceof JetEntity)) {
				continue;
			}
			LivingEntity target = vehicle instanceof JetEntity ? MissileLock.find(vehicle, 72.0) : MissileLock.find(vehicle);
			if (target == null) {
				client.player.sendOverlayMessage(Component.translatable("message.carmod.helicopter.no_target"));
				continue;
			}
			if (missileWait > 0) {
				continue;
			}
			if (vehicle instanceof JetEntity jet) {
				ClientPlayNetworking.send(new JetFirePayload(jet.getId(), target.getId()));
			} else if (vehicle instanceof HelicopterEntity helicopter) {
				ClientPlayNetworking.send(new HelicopterFirePayload(helicopter.getId(), target.getId()));
			}
			client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.65F));
			client.player.sendOverlayMessage(Component.translatable("message.carmod.helicopter.lock", target.getName()));
			missileWait = 16;
		}
		Entity ridden = client.player == null ? null : client.player.getVehicle();
		boolean locking = client.gui.screen() == null && (ridden instanceof HelicopterEntity helicopter && helicopter.armed() || ridden instanceof JetEntity);
		if (!locking) {
			lockHint = 0;
			return;
		}
		if (++lockHint < 20) {
			return;
		}
		lockHint = 0;
		LivingEntity target = ridden instanceof JetEntity ? MissileLock.find(ridden, 72.0) : MissileLock.find(ridden);
		if (target != null) {
			client.player.sendOverlayMessage(Component.translatable("message.carmod.helicopter.lock", target.getName()));
		}
	}

	private static void tickCannon(Minecraft client) {
		if (cannonWait > 0) {
			cannonWait--;
		}
		if (client.gui.screen() != null || client.player == null || !(client.player.getVehicle() instanceof JetEntity jet)) {
			return;
		}
		if (!CANNON.isDown() || cannonWait > 0) {
			return;
		}
		ClientPlayNetworking.send(new JetCannonPayload(jet.getId()));
		client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.FIREWORK_ROCKET_BLAST, 1.45F));
		cannonWait = 4;
	}

	private static void tickCamera(Minecraft client) {
		LocalPlayer player = client.player;
		boolean now = player != null && player.getVehicle() instanceof CarEntity;
		if (now && !riding && ModConfig.thirdPerson()) {
			cameraBefore = client.options.getCameraType();
			client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			cameraOverridden = true;
		} else if (!now && riding && cameraOverridden) {
			client.options.setCameraType(cameraBefore);
			cameraOverridden = false;
		}
		riding = now;
		pullHelicopterCamera(player);
	}

	/** The helicopter is large enough that the normal four-block camera sits inside the tail. */
	private static void pullHelicopterCamera(LocalPlayer player) {
		if (player == null) {
			helicopterCamera = false;
			return;
		}
		var distance = player.getAttribute(Attributes.CAMERA_DISTANCE);
		if (distance == null) {
			return;
		}
		Entity vehicle = player.getVehicle();
		boolean flying = vehicle instanceof HelicopterEntity || vehicle instanceof JetEntity;
		if (flying) {
			if (!helicopterCamera) {
				cameraDistanceBefore = distance.getBaseValue();
				helicopterCamera = true;
			}
			distance.setBaseValue(vehicle instanceof JetEntity ? JET_CAMERA : HELICOPTER_CAMERA);
		} else if (helicopterCamera) {
			distance.setBaseValue(cameraDistanceBefore);
			helicopterCamera = false;
		}
	}

	private static void tickEngines(Minecraft client) {
		if (client.level == null) {
			ENGINES.clear();
			return;
		}
		ENGINES.entrySet().removeIf(entry -> entry.getValue().isStopped());
		for (Entity entity : client.level.entitiesForRendering()) {
			if (!(entity instanceof CarEntity car) || ENGINES.containsKey(car.getId())) {
				continue;
			}
			if (car instanceof JetEntity jet) {
				if (jet.getFirstPassenger() == null && jet.throttle < 0.02F && Math.abs(jet.speed) < 0.05F) {
					continue;
				}
				JetEngineSound engine = new JetEngineSound(jet);
				ENGINES.put(jet.getId(), engine);
				client.getSoundManager().queueTickingSound(engine);
				continue;
			}
			if (car instanceof HelicopterEntity heli) {
				if (heli.getFirstPassenger() == null && heli.rotorSpeed < 0.02F) {
					continue;
				}
				HelicopterRotorSound rotor = new HelicopterRotorSound(heli);
				ENGINES.put(heli.getId(), rotor);
				client.getSoundManager().queueTickingSound(rotor);
				continue;
			}
			float moved = (float) Math.hypot(car.getX() - car.xo, car.getZ() - car.zo);
			if (car.getFirstPassenger() == null && moved < 0.02F) {
				continue;
			}
			CarEngineSound sound = new CarEngineSound(car);
			ENGINES.put(car.getId(), sound);
			client.getSoundManager().queueTickingSound(sound);
		}
	}

	private static void playHit(CarEntity car, SoundEvent sound) {
		float volume = ModConfig.impactVolume();
		if (volume > 0.0F && Minecraft.getInstance().level != null) {
			Minecraft.getInstance().level.playLocalSound(car.getX(), car.getY(), car.getZ(), sound, SoundSource.NEUTRAL, volume, 1.0F, false);
		}
	}

	private static void sendState(CarEntity car) {
		boolean bucket = car instanceof LoaderEntity loader && loader.bucketDown();
		ClientPlayNetworking.send(new CarStatePayload(car.getId(), car.syncSpeed, car.getYRot(), car.getX(), car.getY(), car.getZ(), car.rammedTree, bucket, bucket && car.isPushing()));
		if (car instanceof HelicopterEntity helicopter && helicopter.crashPending) {
			helicopter.crashPending = false;
			ClientPlayNetworking.send(new HelicopterCrashPayload(helicopter.getId()));
		}
		if (car instanceof JetEntity jet) {
			ClientPlayNetworking.send(new JetAttitudePayload(jet.getId(), jet.pitch, jet.roll, jet.throttle));
			if (jet.crashPending) {
				jet.crashPending = false;
				ClientPlayNetworking.send(new JetCrashPayload(jet.getId()));
			}
		}
	}
}
