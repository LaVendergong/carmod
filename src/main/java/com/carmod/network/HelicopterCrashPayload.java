package com.carmod.network;

import com.carmod.CarMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HelicopterCrashPayload(int entityId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<HelicopterCrashPayload> TYPE = new CustomPacketPayload.Type<>(CarMod.id("helicopter_crash"));
	public static final StreamCodec<ByteBuf, HelicopterCrashPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, HelicopterCrashPayload::entityId,
			HelicopterCrashPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
