package com.carmod.client;

import com.carmod.CarMod;
import com.carmod.entity.AmphibiousEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class AmphibiousRenderer extends EntityRenderer<AmphibiousEntity, CarRenderState> {
	private static final Identifier TEXTURE = CarMod.id("textures/entity/amphibious.png");
	private final AmphibiousModel model;

	public AmphibiousRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new AmphibiousModel(AmphibiousModel.createBodyLayer().bakeRoot());
		this.shadowRadius = 1.5F;
	}

	@Override
	public CarRenderState createRenderState() {
		return new CarRenderState();
	}

	@Override
	public void extractRenderState(AmphibiousEntity entity, CarRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.yRot = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
		state.wheelAngle = entity.wheelAngle;
		state.tilt = Mth.rotLerp(partialTick, entity.tiltO, entity.tilt);
	}

	@Override
	public void submit(CarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
		poseStack.mulPose(Axis.XP.rotationDegrees(state.tilt));
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		poseStack.translate(0.0F, -1.501F, 0.0F);
		collector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
		poseStack.popPose();
	}
}
