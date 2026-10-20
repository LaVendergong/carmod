package com.carmod.block;

import com.carmod.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class TrafficLightBlockEntity extends BlockEntity {
	private static final int[] TIMES = {5, 10, 15, 20, 30, 60};
	private static final int YELLOW_TICKS = 40;

	private int seconds = 10;
	private int ticksLeft;

	public TrafficLightBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.TRAFFIC_LIGHT_BE, pos, state);
	}

	public void serverTick() {
		if (!(this.getLevel() instanceof ServerLevel level)) {
			return;
		}
		if (this.ticksLeft <= 0) {
			this.ticksLeft = this.duration(this.getBlockState());
		}
		this.ticksLeft--;
		if (this.ticksLeft > 0) {
			return;
		}
		BlockState state = this.getBlockState();
		TrafficLightBlock.Lamp next = state.getValue(TrafficLightBlock.LAMP).next();
		int seconds = this.seconds;
		BlockPos pos = this.getBlockPos();
		level.setBlock(pos, state.setValue(TrafficLightBlock.LAMP, next), Block.UPDATE_CLIENTS);
		if (level.getBlockEntity(pos) instanceof TrafficLightBlockEntity light) {
			light.seconds = seconds;
			light.ticksLeft = light.duration(next);
			light.setChanged();
		}
	}

	public void cycleTime(Player player) {
		int next = TIMES[0];
		for (int time : TIMES) {
			if (time > this.seconds) {
				next = time;
				break;
			}
		}
		this.seconds = next;
		this.ticksLeft = this.seconds * 20;
		this.setChanged();
		player.sendOverlayMessage(Component.translatable("block.carmod.traffic_light.time", this.seconds));
	}

	private int duration(BlockState state) {
		return this.duration(state.getValue(TrafficLightBlock.LAMP));
	}

	private int duration(TrafficLightBlock.Lamp lamp) {
		return lamp == TrafficLightBlock.Lamp.YELLOW ? YELLOW_TICKS : this.seconds * 20;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("Seconds", this.seconds);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.seconds = input.getIntOr("Seconds", 10);
	}
}
