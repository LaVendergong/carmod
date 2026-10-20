package com.carmod.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * A tracked bore with an auger on the nose. The auger pitches down to the selected angle.
 * 16 pixels are one block. +X is the driver's side, -Z is the front.
 */
public class DrillModel extends EntityModel<CarRenderState> {
	private final ModelPart bit;

	public DrillModel(ModelPart root) {
		super(root, RenderTypes::armorCutoutNoCull);
		this.bit = root.getChild("bit");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		box(root, "body", 0, 0, -14, 4, -20, 28, 12, 44);
		box(root, "cab", 0, 0, -12, 16, 2, 24, 16, 18);
		box(root, "roof", 0, 0, -13, 32, 4, 26, 4, 16);
		box(root, "windshield", 0, 144, -10, 20, 1, 20, 10, 2);
		box(root, "window_left", 0, 144, 12, 20, 6, 2, 8, 10);
		box(root, "window_right", 0, 144, -14, 20, 6, 2, 8, 10);
		box(root, "track_left", 0, 80, 14, 2, -18, 5, 8, 42);
		box(root, "track_right", 0, 80, -19, 2, -18, 5, 8, 42);
		box(root, "stripe_left", 120, 80, 13, 12, -8, 1, 3, 20);
		box(root, "stripe_right", 120, 80, -14, 12, -8, 1, 3, 20);
		box(root, "light_left", 120, 112, 4, 18, -21, 4, 3, 2);
		box(root, "light_right", 120, 112, -8, 18, -21, 4, 3, 2);

		PartDefinition bit = root.addOrReplaceChild("bit", CubeListBuilder.create(), PartPose.offset(0.0F, 14.0F, -20.0F));
		bit.addOrReplaceChild("shaft", CubeListBuilder.create().texOffs(0, 176).addBox(-4.0F, -4.0F, -24.0F, 8.0F, 8.0F, 24.0F), PartPose.ZERO);
		bit.addOrReplaceChild("cone", CubeListBuilder.create().texOffs(0, 176).addBox(-6.0F, -6.0F, -32.0F, 12.0F, 12.0F, 10.0F), PartPose.ZERO);
		bit.addOrReplaceChild("fin_wide", CubeListBuilder.create().texOffs(0, 176).addBox(-7.0F, -1.0F, -22.0F, 14.0F, 2.0F, 8.0F), PartPose.ZERO);
		bit.addOrReplaceChild("fin_tall", CubeListBuilder.create().texOffs(0, 176).addBox(-1.0F, -7.0F, -22.0F, 2.0F, 14.0F, 8.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 256, 256);
	}

	private static void box(PartDefinition root, String name, int u, int v, float x, float bottom, float z, float width, float height, float depth) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, -(bottom + height), z, width, height, depth), PartPose.offset(0.0F, 24.0F, 0.0F));
	}

	@Override
	public void setupAnim(CarRenderState state) {
		super.setupAnim(state);
		this.resetPose();
		this.bit.xRot = state.drill * ((float) Math.PI / 180.0F);
		this.bit.zRot = state.wheelAngle;
	}
}
