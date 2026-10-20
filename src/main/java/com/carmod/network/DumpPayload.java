package com.carmod.network;

import com.carmod.CarMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DumpPayload(int entityId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<DumpPayload> TYPE = new CustomPacketPayload.Type<>(CarMod.id("dump"));
	public static final StreamCodec<ByteBuf, DumpPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, DumpPayload::entityId,
			DumpPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
