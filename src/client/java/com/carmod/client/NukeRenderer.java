package com.carmod.client;

import com.carmod.CarMod;
import com.carmod.entity.NukeEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class NukeRenderer extends EntityRenderer<NukeEntity, CarRenderState> {
	private static final Identifier TEXTURE = CarMod.id("textures/entity/nuke.png");
	private final NukeModel model;

	public NukeRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new NukeModel(NukeModel.createBodyLayer().bakeRoot());
		this.shadowRadius = 0.4F;
	}

	@Override
	public CarRenderState createRenderState() {
		return new CarRenderState();
	}

	@Override
	public void extractRenderState(NukeEntity entity, CarRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
	}

	@Override
	public void submit(CarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		poseStack.pushPose();
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		poseStack.translate(0.0F, -1.501F, 0.0F);
		collector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
		poseStack.popPose();
	}
}
