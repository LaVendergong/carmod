package com.carmod.mixin;

import com.carmod.client.JetRiderPose;
import com.carmod.entity.JetEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Keeps the rider's body glued to the fuselage, including while inverted.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class JetRiderMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("RETURN"))
	private void carmod$rememberJet(LivingEntity entity, LivingEntityRenderState state, float partial, CallbackInfo ci) {
		JetRiderPose extra = (JetRiderPose) (Object) state;
		if (entity.getVehicle() instanceof JetEntity jet) {
			extra.carmod$setJetPose(true, Mth.rotLerp(partial, jet.rollO, jet.roll), Mth.rotLerp(partial, jet.tiltO, jet.tilt));
			state.xRot = 0.0F;
		} else {
			extra.carmod$setJetPose(false, 0.0F, 0.0F);
		}
	}

	@Inject(method = "setupRotations", at = @At("RETURN"))
	private void carmod$poseWithJet(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float scale, CallbackInfo ci) {
		JetRiderPose extra = (JetRiderPose) (Object) state;
		if (!extra.carmod$inJet()) {
			return;
		}
		poseStack.mulPose(Axis.ZP.rotationDegrees(extra.carmod$jetRoll()));
		poseStack.mulPose(Axis.XP.rotationDegrees(extra.carmod$jetPitch()));
	}
}
