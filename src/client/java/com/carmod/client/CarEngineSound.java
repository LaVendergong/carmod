package com.carmod.client;

import com.carmod.CarMotion;
import com.carmod.entity.CarEntity;
import com.carmod.registry.ModSounds;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

public class CarEngineSound extends AbstractTickableSoundInstance {
	private final CarEntity car;
	private int quiet;

	public CarEngineSound(CarEntity car) {
		super(ModSounds.ENGINE, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
		this.car = car;
		this.looping = true;
		this.delay = 0;
		this.follow();
		this.apply(this.moved(), car.getFirstPassenger() != null);
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}

	@Override
	public void tick() {
		if (this.car.isRemoved()) {
			this.stop();
			return;
		}
		this.follow();
		float moved = this.moved();
		boolean occupied = this.car.getFirstPassenger() != null;
		if (!occupied && moved < 0.02F) {
			this.volume = 0.0F;
			if (++this.quiet > 15) {
				this.stop();
			}
			return;
		}
		this.quiet = 0;
		this.apply(moved, occupied);
	}

	private void follow() {
		this.x = this.car.getX();
		this.y = this.car.getY() + 0.6;
		this.z = this.car.getZ();
	}

	private float moved() {
		return (float) Math.hypot(this.car.getX() - this.car.xo, this.car.getZ() - this.car.zo);
	}

	private void apply(float moved, boolean occupied) {
		this.volume = CarMotion.engineVolume(moved, occupied);
		this.pitch = CarMotion.enginePitch(moved);
	}
}
