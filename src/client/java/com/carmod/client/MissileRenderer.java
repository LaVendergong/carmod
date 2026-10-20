package com.carmod.client;

import com.carmod.CarMod;
import com.carmod.entity.MissileEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class MissileRenderer extends EntityRenderer<MissileEntity, CarRenderState> {
	private static final Identifier TEXTURE = CarMod.id("textures/entity/helicopter.png");
	private static final float SCALE = 2.4F;
	private final MissileModel model;

	public MissileRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new MissileModel(MissileModel.createBodyLayer().bakeRoot());
		this.shadowRadius = 0.4F;
	}

	@Override
	public CarRenderState createRenderState() {
		return new CarRenderState();
	}

	@Override
	public void extractRenderState(MissileEntity entity, CarRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.yRot = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
		state.tilt = Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot());
	}

	@Override
	public void submit(CarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
		poseStack.mulPose(Axis.XP.rotationDegrees(state.tilt));
		poseStack.scale(-SCALE, -SCALE, SCALE);
		poseStack.translate(0.0F, -1.501F, 0.0F);
		collector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
		poseStack.popPose();
	}
}
