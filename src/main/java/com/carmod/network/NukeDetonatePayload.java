package com.carmod.network;

import com.carmod.CarMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record NukeDetonatePayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<NukeDetonatePayload> TYPE = new CustomPacketPayload.Type<>(CarMod.id("nuke_detonate"));
	public static final StreamCodec<ByteBuf, NukeDetonatePayload> CODEC = StreamCodec.unit(new NukeDetonatePayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
