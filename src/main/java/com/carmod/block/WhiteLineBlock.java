package com.carmod.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class WhiteLineBlock extends Block {
	public static final MapCodec<WhiteLineBlock> CODEC = BlockBehaviour.simpleCodec(WhiteLineBlock::new);

	public WhiteLineBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends WhiteLineBlock> codec() {
		return CODEC;
	}
}
