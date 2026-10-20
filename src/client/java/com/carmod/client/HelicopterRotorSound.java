package com.carmod.client;

import com.carmod.entity.HelicopterEntity;
import com.carmod.registry.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** Rotor loop. Pitch and loudness follow how fast the blades are turning. */
public class HelicopterRotorSound extends AbstractTickableSoundInstance {
	private final HelicopterEntity helicopter;
	private int quiet;

	public HelicopterRotorSound(HelicopterEntity helicopter) {
		super(ModSounds.ROTOR, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
		this.helicopter = helicopter;
		this.looping = true;
		this.delay = 0;
		this.follow();
		this.apply();
		var player = Minecraft.getInstance().player;
		if (player != null && player.getVehicle() == helicopter) {
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
		}
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}

	@Override
	public void tick() {
		if (this.helicopter.isRemoved()) {
			this.stop();
			return;
		}
		this.follow();
		boolean occupied = this.helicopter.getFirstPassenger() != null;
		if (!occupied && this.helicopter.rotorSpeed < 0.02F) {
			this.volume = 0.0F;
			if (++this.quiet > 15) {
				this.stop();
			}
			return;
		}
		this.quiet = 0;
		this.apply();
	}

	private void follow() {
		this.x = this.helicopter.getX();
		this.y = this.helicopter.getY() + 1.5;
		this.z = this.helicopter.getZ();
	}

	private void apply() {
		float rpm = this.helicopter.rotorSpeed;
		this.volume = 0.1F + rpm * 0.85F;
		this.pitch = 0.84F + rpm * 0.16F;
	}
}
