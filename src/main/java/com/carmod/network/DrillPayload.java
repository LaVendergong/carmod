package com.carmod.network;

import com.carmod.CarMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DrillPayload(int entityId, int angle) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<DrillPayload> TYPE = new CustomPacketPayload.Type<>(CarMod.id("drill"));
	public static final StreamCodec<ByteBuf, DrillPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, DrillPayload::entityId,
			ByteBufCodecs.VAR_INT, DrillPayload::angle,
			DrillPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
