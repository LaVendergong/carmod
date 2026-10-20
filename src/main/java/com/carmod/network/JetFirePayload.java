package com.carmod.network;

import com.carmod.CarMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record JetFirePayload(int entityId, int targetId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<JetFirePayload> TYPE = new CustomPacketPayload.Type<>(CarMod.id("jet_fire"));
	public static final StreamCodec<ByteBuf, JetFirePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, JetFirePayload::entityId,
			ByteBufCodecs.VAR_INT, JetFirePayload::targetId,
			JetFirePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
