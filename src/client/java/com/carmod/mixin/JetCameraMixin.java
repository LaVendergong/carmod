package com.carmod.mixin;

import com.carmod.entity.JetEntity;

import org.joml.Quaternionf;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Rolls the view with the jet. The seat already points the look along the nose.
 */
@Mixin(Camera.class)
public abstract class JetCameraMixin {
	@Shadow
	private Entity entity;
	@Shadow
	@Final
	private Quaternionf rotation;
	@Shadow
	@Final
	private org.joml.Vector3f forwards;
	@Shadow
	@Final
	private org.joml.Vector3f up;
	@Shadow
	@Final
	private org.joml.Vector3f left;
	@Shadow
	@Final
	private static Vector3fc UP;
	@Shadow
	@Final
	private static Vector3fc LEFT;

	@Shadow
	public abstract float getCameraEntityPartialTicks(DeltaTracker delta);

	@Inject(method = "update", at = @At("RETURN"))
	private void carmod$rollWithJet(DeltaTracker delta, CallbackInfo ci) {
		if (!(this.entity instanceof Player player) || !(player.getVehicle() instanceof JetEntity jet)) {
			return;
		}
		float partial = this.getCameraEntityPartialTicks(delta);
		float rollDeg = Mth.rotLerp(partial, jet.rollO, jet.roll);
		if (Math.abs(rollDeg) < 0.5F) {
			return;
		}
		float roll = rollDeg * ((float) Math.PI / 180.0F);
		this.rotation.premul(new Quaternionf().rotateAxis(roll, this.forwards.x, this.forwards.y, this.forwards.z));
		UP.rotate(this.rotation, this.up);
		LEFT.rotate(this.rotation, this.left);
	}
}
