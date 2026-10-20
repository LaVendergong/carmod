package com.carmod.mixin;

import com.carmod.client.JetRiderPose;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

@Mixin(LivingEntityRenderState.class)
public class JetRiderStateMixin implements JetRiderPose {
	@Unique
	private boolean carmod$inJet;
	@Unique
	private float carmod$jetRoll;
	@Unique
	private float carmod$jetPitch;

	@Override
	public void carmod$setJetPose(boolean inJet, float roll, float pitch) {
		this.carmod$inJet = inJet;
		this.carmod$jetRoll = roll;
		this.carmod$jetPitch = pitch;
	}

	@Override
	public boolean carmod$inJet() {
		return this.carmod$inJet;
	}

	@Override
	public float carmod$jetRoll() {
		return this.carmod$jetRoll;
	}

	@Override
	public float carmod$jetPitch() {
		return this.carmod$jetPitch;
	}
}
