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
 * Cab in the middle, bed behind. The bed pivots at the tailgate and lifts toward the cab.
 * 16 pixels are one block. +X is the driver's side, -Z is the front.
 */
public class DumpTruckModel extends EntityModel<CarRenderState> {
	private final ModelPart bed;
	private final ModelPart wheelFl;
	private final ModelPart wheelFr;
	private final ModelPart wheelMl;
	private final ModelPart wheelMr;
	private final ModelPart wheelBl;
	private final ModelPart wheelBr;

	public DumpTruckModel(ModelPart root) {
		super(root, RenderTypes::armorCutoutNoCull);
		this.bed = root.getChild("bed");
		this.wheelFl = root.getChild("wheel_fl");
		this.wheelFr = root.getChild("wheel_fr");
		this.wheelMl = root.getChild("wheel_ml");
		this.wheelMr = root.getChild("wheel_mr");
		this.wheelBl = root.getChild("wheel_bl");
		this.wheelBr = root.getChild("wheel_br");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		box(root, "frame_front", 0, 140, -12, 5, -28, 24, 5, 36);
		box(root, "frame_rear", 0, 140, -12, 5, 8, 24, 5, 40);
		box(root, "hood", 0, 0, -12, 10, -32, 24, 10, 16);
		box(root, "cab", 0, 0, -14, 10, -18, 28, 26, 26);
		box(root, "roof", 0, 0, -15, 36, -16, 30, 4, 22);
		box(root, "windshield", 180, 0, -12, 22, -18, 24, 10, 2);
		box(root, "window_left", 180, 0, 14, 22, -12, 2, 8, 14);
		box(root, "window_right", 180, 0, -16, 22, -12, 2, 8, 14);
		box(root, "grille", 180, 48, -7, 12, -33, 14, 5, 2);
		box(root, "light_left", 180, 28, 4, 14, -33, 5, 4, 3);
		box(root, "light_right", 180, 28, -9, 14, -33, 5, 4, 3);
		box(root, "exhaust", 180, 48, 12, 36, -6, 3, 8, 3);

		PartDefinition bed = root.addOrReplaceChild("bed", CubeListBuilder.create(), PartPose.offset(0.0F, 14.0F, 46.0F));
		bed.addOrReplaceChild("floor", CubeListBuilder.create().texOffs(0, 64).addBox(-16.0F, -4.0F, -38.0F, 32.0F, 4.0F, 38.0F), PartPose.ZERO);
		bed.addOrReplaceChild("side_left", CubeListBuilder.create().texOffs(0, 64).addBox(12.0F, -18.0F, -38.0F, 4.0F, 14.0F, 38.0F), PartPose.ZERO);
		bed.addOrReplaceChild("side_right", CubeListBuilder.create().texOffs(0, 64).addBox(-16.0F, -18.0F, -38.0F, 4.0F, 14.0F, 38.0F), PartPose.ZERO);
		bed.addOrReplaceChild("headboard", CubeListBuilder.create().texOffs(0, 64).addBox(-16.0F, -18.0F, -38.0F, 32.0F, 14.0F, 4.0F), PartPose.ZERO);
		bed.addOrReplaceChild("tailgate", CubeListBuilder.create().texOffs(0, 64).addBox(-12.0F, -16.0F, -2.0F, 24.0F, 12.0F, 3.0F), PartPose.ZERO);
		bed.addOrReplaceChild("stripe_left", CubeListBuilder.create().texOffs(0, 200).addBox(16.0F, -10.0F, -34.0F, 1.0F, 3.0F, 30.0F), PartPose.ZERO);
		bed.addOrReplaceChild("stripe_right", CubeListBuilder.create().texOffs(0, 200).addBox(-17.0F, -10.0F, -34.0F, 1.0F, 3.0F, 30.0F), PartPose.ZERO);

		wheel(root, "wheel_fl", 16, -22);
		wheel(root, "wheel_fr", -16, -22);
		wheel(root, "wheel_ml", 16, 18);
		wheel(root, "wheel_mr", -16, 18);
		wheel(root, "wheel_bl", 16, 32);
		wheel(root, "wheel_br", -16, 32);
		return LayerDefinition.create(mesh, 256, 256);
	}

	private static void box(PartDefinition root, String name, int u, int v, float x, float bottom, float z, float width, float height, float depth) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, -(bottom + height), z, width, height, depth), PartPose.offset(0.0F, 24.0F, 0.0F));
	}

	private static void wheel(PartDefinition root, String name, float x, float z) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(200, 70).addBox(-3.0F, -6.0F, -6.0F, 6.0F, 12.0F, 12.0F), PartPose.offset(x, 18.0F, z));
	}

	@Override
	public void setupAnim(CarRenderState state) {
		super.setupAnim(state);
		this.resetPose();
		float spin = state.wheelAngle;
		this.wheelFl.xRot = spin;
		this.wheelFr.xRot = spin;
		this.wheelMl.xRot = spin;
		this.wheelMr.xRot = spin;
		this.wheelBl.xRot = spin;
		this.wheelBr.xRot = spin;
		// Negative xRot lifts the cab end of the bed. The tailgate stays at the pivot.
		this.bed.xRot = -state.bed * 0.75F;
	}
}
