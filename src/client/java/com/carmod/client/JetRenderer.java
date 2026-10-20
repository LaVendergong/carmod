package com.carmod.client;

import com.carmod.CarMod;
import com.carmod.entity.JetEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class JetRenderer extends EntityRenderer<JetEntity, CarRenderState> {
	private static final Identifier TEXTURE = CarMod.id("textures/entity/jet.png");
	private final JetModel model;

	public JetRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new JetModel(JetModel.createBodyLayer().bakeRoot());
		this.shadowRadius = 2.6F;
	}

	@Override
	public CarRenderState createRenderState() {
		return new CarRenderState();
	}

	@Override
	public void extractRenderState(JetEntity entity, CarRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.yRot = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
		state.tilt = Mth.rotLerp(partialTick, entity.tiltO, entity.tilt);
		state.roll = Mth.rotLerp(partialTick, entity.rollO, entity.roll);
		state.gearDown = entity.onGround();
	}

	@Override
	public void submit(CarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
		poseStack.mulPose(Axis.ZP.rotationDegrees(state.roll));
		poseStack.mulPose(Axis.XP.rotationDegrees(state.tilt));
		poseStack.scale(-JetEntity.MODEL_SCALE, -JetEntity.MODEL_SCALE, JetEntity.MODEL_SCALE);
		poseStack.translate(0.0F, -1.501F, 0.0F);
		collector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
		poseStack.popPose();
	}
}
