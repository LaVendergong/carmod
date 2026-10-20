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
 * Wheel loader, after a Caterpillar 906H side view: big tires, glass cab, rear hood, lift arms and a wide bucket.
 * 16 pixels are one block. +X is the driver's side, -Z is the front.
 */
public class LoaderModel extends EntityModel<CarRenderState> {
	private final ModelPart arms;
	private final ModelPart bucket;
	private final ModelPart wheelFl;
	private final ModelPart wheelFr;
	private final ModelPart wheelBl;
	private final ModelPart wheelBr;

	public LoaderModel(ModelPart root) {
		super(root, RenderTypes::armorCutoutNoCull);
		this.arms = root.getChild("arms");
		this.bucket = this.arms.getChild("bucket");
		this.wheelFl = root.getChild("wheel_fl");
		this.wheelFr = root.getChild("wheel_fr");
		this.wheelBl = root.getChild("wheel_bl");
		this.wheelBr = root.getChild("wheel_br");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		box(root, "front_frame", 0, 0, -11, 7, -30, 22, 5, 28);
		box(root, "rear_hood", 0, 0, -13, 8, 2, 26, 14, 26);
		box(root, "counterweight", 140, 96, -11, 4, 26, 22, 5, 6);
		box(root, "rear_grille", 140, 96, -8, 12, 28, 16, 8, 2);
		box(root, "cab", 0, 80, -12, 14, -6, 24, 16, 16);
		box(root, "cab_roof", 0, 80, -13, 29, -8, 26, 3, 20);
		box(root, "windshield", 128, 0, -10, 18, -7, 20, 11, 1);
		box(root, "glass_left", 128, 0, 11, 18, -4, 1, 11, 12);
		box(root, "glass_right", 128, 0, -12, 18, -4, 1, 11, 12);
		box(root, "rear_window", 128, 0, -8, 18, 10, 16, 8, 1);
		box(root, "exhaust", 140, 96, 8, 22, 16, 3, 12, 3);
		box(root, "axle_front", 0, 0, -16, 6, -19, 32, 3, 5);
		box(root, "axle_rear", 0, 0, -16, 6, 15, 32, 3, 5);
		box(root, "fender_fl", 0, 0, 10, 13, -23, 10, 2, 14);
		box(root, "fender_fr", 0, 0, -20, 13, -23, 10, 2, 14);
		box(root, "fender_bl", 0, 0, 10, 13, 11, 10, 2, 14);
		box(root, "fender_br", 0, 0, -20, 13, 11, 10, 2, 14);
		box(root, "ladder", 0, 80, 12, 6, 4, 2, 12, 3);
		box(root, "headlight_l", 200, 0, 4, 10, -31, 5, 3, 2);
		box(root, "headlight_r", 200, 0, -9, 10, -31, 5, 3, 2);
		box(root, "stripe", 200, 16, -14, 14, 8, 1, 3, 18);

		// Pivot sits beside the cab, about at the top of the front tire. -Z reaches the bucket.
		PartDefinition arms = root.addOrReplaceChild("arms", CubeListBuilder.create(), PartPose.offset(0.0F, 10.0F, -6.0F));
		arms.addOrReplaceChild("arm_left", CubeListBuilder.create().texOffs(0, 0).addBox(11.0F, -3.0F, -26.0F, 5.0F, 5.0F, 26.0F), PartPose.ZERO);
		arms.addOrReplaceChild("arm_right", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -3.0F, -26.0F, 5.0F, 5.0F, 26.0F), PartPose.ZERO);
		arms.addOrReplaceChild("ram_left", CubeListBuilder.create().texOffs(0, 0).addBox(7.0F, 1.0F, -22.0F, 2.0F, 2.0F, 20.0F), PartPose.ZERO);
		arms.addOrReplaceChild("ram_right", CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, 1.0F, -22.0F, 2.0F, 2.0F, 20.0F), PartPose.ZERO);
		arms.addOrReplaceChild("link", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -8.0F, -24.0F, 4.0F, 6.0F, 4.0F), PartPose.ZERO);

		PartDefinition bucket = arms.addOrReplaceChild("bucket", CubeListBuilder.create(), PartPose.offset(0.0F, 2.0F, -24.0F));
		bucket.addOrReplaceChild("back", CubeListBuilder.create().texOffs(0, 140).addBox(-18.0F, 0.0F, -2.0F, 36.0F, 12.0F, 4.0F), PartPose.ZERO);
		bucket.addOrReplaceChild("floor", CubeListBuilder.create().texOffs(0, 140).addBox(-18.0F, 8.0F, -18.0F, 36.0F, 4.0F, 16.0F), PartPose.ZERO);
		bucket.addOrReplaceChild("side_left", CubeListBuilder.create().texOffs(0, 140).addBox(15.0F, 2.0F, -18.0F, 3.0F, 12.0F, 16.0F), PartPose.ZERO);
		bucket.addOrReplaceChild("side_right", CubeListBuilder.create().texOffs(0, 140).addBox(-18.0F, 2.0F, -18.0F, 3.0F, 12.0F, 16.0F), PartPose.ZERO);
		bucket.addOrReplaceChild("lip", CubeListBuilder.create().texOffs(0, 140).addBox(-19.0F, 10.0F, -20.0F, 38.0F, 2.0F, 4.0F), PartPose.ZERO);

		wheel(root, "wheel_fl", 15, -16);
		wheel(root, "wheel_fr", -15, -16);
		wheel(root, "wheel_bl", 15, 18);
		wheel(root, "wheel_br", -15, 18);
		return LayerDefinition.create(mesh, 256, 256);
	}

	private static void box(PartDefinition root, String name, int u, int v, float x, float bottom, float z, float width, float height, float depth) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, -(bottom + height), z, width, height, depth), PartPose.offset(0.0F, 24.0F, 0.0F));
	}

	private static void wheel(PartDefinition root, String name, float x, float z) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(140, 48).addBox(-3.0F, -7.0F, -7.0F, 6.0F, 14.0F, 14.0F), PartPose.offset(x, 17.0F, z));
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
		// Positive xRot tips the arms and the cutting edge down onto the road.
		this.arms.xRot = state.bucket ? 0.4F : -0.5F;
		this.bucket.xRot = state.bucket ? 0.35F : -0.45F;
	}
}
