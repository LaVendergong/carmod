package com.carmod.network;

import com.carmod.CarMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HelicopterFirePayload(int helicopterId, int targetId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<HelicopterFirePayload> TYPE = new CustomPacketPayload.Type<>(CarMod.id("helicopter_fire"));
	public static final StreamCodec<ByteBuf, HelicopterFirePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, HelicopterFirePayload::helicopterId,
			ByteBufCodecs.VAR_INT, HelicopterFirePayload::targetId,
			HelicopterFirePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
