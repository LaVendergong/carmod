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
 * A car hull with a boat bottom and a propeller. Wheels stay on for the land.
 * 16 pixels are one block. +X is the driver's side, -Z is the front.
 */
public class AmphibiousModel extends EntityModel<CarRenderState> {
	private final ModelPart wheelFl;
	private final ModelPart wheelFr;
	private final ModelPart wheelBl;
	private final ModelPart wheelBr;
	private final ModelPart prop;

	public AmphibiousModel(ModelPart root) {
		super(root, RenderTypes::armorCutoutNoCull);
		this.wheelFl = root.getChild("wheel_fl");
		this.wheelFr = root.getChild("wheel_fr");
		this.wheelBl = root.getChild("wheel_bl");
		this.wheelBr = root.getChild("wheel_br");
		this.prop = root.getChild("prop");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		box(root, "hull_front", 0, 0, -16, 1, -34, 32, 6, 32);
		box(root, "hull_rear", 0, 0, -16, 1, -2, 32, 6, 32);
		box(root, "deck_front", 0, 52, -14, 7, -28, 28, 5, 28);
		box(root, "deck_rear", 0, 52, -14, 7, 0, 28, 5, 26);
		box(root, "cab", 0, 100, -12, 12, -6, 24, 18, 18);
		box(root, "roof", 0, 100, -13, 32, -4, 26, 4, 16);
		box(root, "windshield", 160, 0, -10, 20, -8, 20, 8, 2);
		box(root, "snorkel", 0, 100, 8, 28, -2, 3, 10, 3);
		box(root, "stripe", 0, 156, -15, 8, -20, 1, 2, 36);
		wheel(root, "wheel_fl", 16, -22);
		wheel(root, "wheel_fr", -16, -22);
		wheel(root, "wheel_bl", 16, 16);
		wheel(root, "wheel_br", -16, 16);

		PartDefinition prop = root.addOrReplaceChild("prop", CubeListBuilder.create(), PartPose.offset(0.0F, 16.0F, 32.0F));
		prop.addOrReplaceChild("blade_h", CubeListBuilder.create().texOffs(160, 64).addBox(-6.0F, -1.0F, -1.0F, 12.0F, 2.0F, 2.0F), PartPose.ZERO);
		prop.addOrReplaceChild("blade_v", CubeListBuilder.create().texOffs(160, 64).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 256, 256);
	}

	private static void box(PartDefinition root, String name, int u, int v, float x, float bottom, float z, float width, float height, float depth) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, -(bottom + height), z, width, height, depth), PartPose.offset(0.0F, 24.0F, 0.0F));
	}

	private static void wheel(PartDefinition root, String name, float x, float z) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(160, 32).addBox(-2.0F, -5.0F, -5.0F, 4.0F, 10.0F, 10.0F), PartPose.offset(x, 19.0F, z));
	}

	@Override
	public void setupAnim(CarRenderState state) {
		super.setupAnim(state);
		this.resetPose();
		float spin = state.wheelAngle;
		this.wheelFl.xRot = spin;
		this.wheelFr.xRot = spin;
		this.wheelBl.xRot = spin;
		this.wheelBr.xRot = spin;
		this.prop.zRot = spin;
	}
}
