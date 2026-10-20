package com.carmod.network;

import com.carmod.CarMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CarStatePayload(int entityId, float speed, float yaw, double x, double y, double z, boolean ram, boolean bucket, boolean pushing) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CarStatePayload> TYPE = new CustomPacketPayload.Type<>(CarMod.id("car_state"));
	public static final StreamCodec<ByteBuf, CarStatePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, CarStatePayload::entityId,
			ByteBufCodecs.FLOAT, CarStatePayload::speed,
			ByteBufCodecs.FLOAT, CarStatePayload::yaw,
			ByteBufCodecs.DOUBLE, CarStatePayload::x,
			ByteBufCodecs.DOUBLE, CarStatePayload::y,
			ByteBufCodecs.DOUBLE, CarStatePayload::z,
			ByteBufCodecs.BOOL, CarStatePayload::ram,
			ByteBufCodecs.BOOL, CarStatePayload::bucket,
			ByteBufCodecs.BOOL, CarStatePayload::pushing,
			CarStatePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
