package com.carmod;

import com.carmod.block.RoadPlan;
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
import com.carmod.registry.ModBlocks;
import com.carmod.registry.ModEntities;
import com.carmod.registry.ModItems;
import com.carmod.registry.ModSounds;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CarMod implements ModInitializer {
	public static final String MOD_ID = "carmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CarMotion.selfCheck();
		LoaderGrade.selfCheck();
		DrillPath.selfCheck();
		AmphibiousFloat.selfCheck();
		RaceDrift.selfCheck();
		HelicopterFlight.selfCheck();
		JetFlight.selfCheck();
		MissileLock.selfCheck();
		NukeBlast.selfCheck();
		NukeIndex.selfCheck();
		RoadPlan.selfCheck();
		ModConfig.selfCheck();
		ModConfig.load();
		ModSounds.init();
		ModEntities.init();
		ModBlocks.init();
		ModItems.init();
		ServerLifecycleEvents.SERVER_STARTING.register(server -> DumpCargo.selfCheck());
		ServerLifecycleEvents.SERVER_STARTED.register(NukeIndex::load);
		ServerTickEvents.END_SERVER_TICK.register(NukeBlast::tick);
		PayloadTypeRegistry.serverboundPlay().register(CarStatePayload.TYPE, CarStatePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DumpPayload.TYPE, DumpPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DrillPayload.TYPE, DrillPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(HelicopterCrashPayload.TYPE, HelicopterCrashPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(HelicopterFirePayload.TYPE, HelicopterFirePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(JetFirePayload.TYPE, JetFirePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(JetCannonPayload.TYPE, JetCannonPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(JetCrashPayload.TYPE, JetCrashPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(JetAttitudePayload.TYPE, JetAttitudePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(NukeDetonatePayload.TYPE, NukeDetonatePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CarStatePayload.TYPE, (payload, context) -> {
			var player = context.player();
			if (!Double.isFinite(payload.x()) || !Double.isFinite(payload.y()) || !Double.isFinite(payload.z()) || !Float.isFinite(payload.yaw())) {
				return;
			}
			if (!(player.level().getEntity(payload.entityId()) instanceof CarEntity car)) {
				return;
			}
			if (car.getControllingPassenger() != player) {
				return;
			}
			if (player.distanceToSqr(payload.x(), payload.y(), payload.z()) > 64.0) {
				return;
			}
			car.applyDriverState(payload.speed(), payload.yaw());
			if (car instanceof LoaderEntity loader) {
				loader.setBucketDown(payload.bucket());
				if (payload.bucket() && (payload.pushing() || payload.speed() > 0.02F) && player.level() instanceof ServerLevel level) {
					loader.gradeDriven(level, payload.x(), payload.y(), payload.z(), payload.yaw(), payload.speed());
				}
			}
			if (car instanceof DrillEntity drill && player.level() instanceof ServerLevel level) {
				drill.boreDriven(level, payload.x(), payload.y(), payload.z(), payload.yaw(), payload.speed(), payload.pushing());
			}
			if (payload.ram() && Math.abs(payload.speed()) >= car.topSpeed() - 0.05F && player.level() instanceof ServerLevel level) {
				car.smashTrees(level);
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(DumpPayload.TYPE, (payload, context) -> {
			var player = context.player();
			if (!(player.level().getEntity(payload.entityId()) instanceof DumpTruckEntity truck)) {
				return;
			}
			if (truck.getControllingPassenger() != player) {
				return;
			}
			if (player.level() instanceof ServerLevel level) {
				truck.dump(level, player);
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(DrillPayload.TYPE, (payload, context) -> {
			var player = context.player();
			if (!DrillEntity.allowed(payload.angle())) {
				return;
			}
			if (!(player.level().getEntity(payload.entityId()) instanceof DrillEntity drill)) {
				return;
			}
			if (drill.getControllingPassenger() != player) {
				return;
			}
			drill.setAngle(payload.angle());
		});
		ServerPlayNetworking.registerGlobalReceiver(HelicopterCrashPayload.TYPE, (payload, context) -> {
			var player = context.player();
			if (!(player.level().getEntity(payload.entityId()) instanceof HelicopterEntity helicopter)) {
				return;
			}
			if (helicopter.getControllingPassenger() != player || !(player.level() instanceof ServerLevel level)) {
				return;
			}
			helicopter.explode(level);
		});
		ServerPlayNetworking.registerGlobalReceiver(HelicopterFirePayload.TYPE, (payload, context) -> {
			var player = context.player();
			if (!(player.level().getEntity(payload.helicopterId()) instanceof HelicopterEntity helicopter)) {
				return;
			}
			if (helicopter.getControllingPassenger() != player || !(player.level() instanceof ServerLevel level)) {
				return;
			}
			helicopter.fire(level, payload.targetId());
		});
		ServerPlayNetworking.registerGlobalReceiver(JetFirePayload.TYPE, (payload, context) -> {
			var player = context.player();
			if (!(player.level().getEntity(payload.entityId()) instanceof JetEntity jet)) {
				return;
			}
			if (jet.getControllingPassenger() != player || !(player.level() instanceof ServerLevel level)) {
				return;
			}
			jet.fire(level, payload.targetId());
		});
		ServerPlayNetworking.registerGlobalReceiver(JetCannonPayload.TYPE, (payload, context) -> {
			var player = context.player();
			if (!(player.level().getEntity(payload.entityId()) instanceof JetEntity jet)) {
				return;
			}
			if (jet.getControllingPassenger() != player || !(player.level() instanceof ServerLevel level)) {
				return;
			}
			jet.cannon(level);
		});
		ServerPlayNetworking.registerGlobalReceiver(JetCrashPayload.TYPE, (payload, context) -> {
			var player = context.player();
			if (!(player.level().getEntity(payload.entityId()) instanceof JetEntity jet)) {
				return;
			}
			if (jet.getControllingPassenger() != player || !(player.level() instanceof ServerLevel level)) {
				return;
			}
			jet.explode(level);
		});
		ServerPlayNetworking.registerGlobalReceiver(JetAttitudePayload.TYPE, (payload, context) -> {
			var player = context.player();
			if (!(player.level().getEntity(payload.entityId()) instanceof JetEntity jet)) {
				return;
			}
			if (jet.getControllingPassenger() != player) {
				return;
			}
			jet.syncAttitude(payload.pitch(), payload.roll(), payload.throttle());
		});
		ServerPlayNetworking.registerGlobalReceiver(NukeDetonatePayload.TYPE, (payload, context) -> NukeIndex.detonateNearest(context.player()));
		LOGGER.info("Car mod ready");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
