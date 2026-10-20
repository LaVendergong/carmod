package com.carmod.network;

import com.carmod.CarMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record JetAttitudePayload(int entityId, float pitch, float roll, float throttle) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<JetAttitudePayload> TYPE = new CustomPacketPayload.Type<>(CarMod.id("jet_attitude"));
	public static final StreamCodec<ByteBuf, JetAttitudePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, JetAttitudePayload::entityId,
			ByteBufCodecs.FLOAT, JetAttitudePayload::pitch,
			ByteBufCodecs.FLOAT, JetAttitudePayload::roll,
			ByteBufCodecs.FLOAT, JetAttitudePayload::throttle,
			JetAttitudePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
