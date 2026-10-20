package com.carmod.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class RoadBlock extends Block {
	public static final MapCodec<RoadBlock> CODEC = BlockBehaviour.simpleCodec(RoadBlock::new);

	public RoadBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends RoadBlock> codec() {
		return CODEC;
	}
}
