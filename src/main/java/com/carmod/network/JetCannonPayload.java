package com.carmod.network;

import com.carmod.CarMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record JetCannonPayload(int entityId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<JetCannonPayload> TYPE = new CustomPacketPayload.Type<>(CarMod.id("jet_cannon"));
	public static final StreamCodec<ByteBuf, JetCannonPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, JetCannonPayload::entityId,
			JetCannonPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
