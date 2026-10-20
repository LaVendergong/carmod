package com.carmod.client;

import com.carmod.entity.JetEntity;
import com.carmod.registry.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** Jet whine. Louder and higher as the throttle opens. The rider hears it without distance fade. */
public class JetEngineSound extends AbstractTickableSoundInstance {
	private final JetEntity jet;
	private int quiet;

	public JetEngineSound(JetEntity jet) {
		super(ModSounds.JET, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
		this.jet = jet;
		this.looping = true;
		this.delay = 0;
		this.follow();
		this.apply();
		var player = Minecraft.getInstance().player;
		if (player != null && player.getVehicle() == jet) {
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
		if (this.jet.isRemoved()) {
			this.stop();
			return;
		}
		this.follow();
		boolean running = this.jet.getFirstPassenger() != null || this.jet.throttle > 0.02F || Math.abs(this.jet.speed) > 0.05F;
		if (!running) {
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
		var player = Minecraft.getInstance().player;
		if (player != null && player.getVehicle() == this.jet) {
			// relative sounds treat xyz as an offset from the listener; world coords pan the whine into one ear
			this.x = 0.0;
			this.y = 0.0;
			this.z = 0.0;
			return;
		}
		this.x = this.jet.getX();
		this.y = this.jet.getY() + 0.8;
		this.z = this.jet.getZ();
	}

	private void apply() {
		float throttle = this.jet.throttle;
		this.volume = 0.18F + throttle * 0.9F;
		this.pitch = 0.72F + throttle * 0.62F + Math.min(0.2F, Math.abs(this.jet.speed) * 0.05F);
	}
}
