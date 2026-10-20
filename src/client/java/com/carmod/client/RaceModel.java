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
 * Low wedge with a rear wing. Front wheels steer. 16 pixels are one block.
 * +X is the driver's side, -Z is the front.
 */
public class RaceModel extends EntityModel<CarRenderState> {
	private final ModelPart wheelFl;
	private final ModelPart wheelFr;
	private final ModelPart wheelBl;
	private final ModelPart wheelBr;

	public RaceModel(ModelPart root) {
		super(root, RenderTypes::armorCutoutNoCull);
		this.wheelFl = root.getChild("wheel_fl");
		this.wheelFr = root.getChild("wheel_fr");
		this.wheelBl = root.getChild("wheel_bl");
		this.wheelBr = root.getChild("wheel_br");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		box(root, "nose", 0, 0, -14, 4, -40, 28, 5, 22);
		box(root, "body", 0, 0, -15, 4, -18, 30, 6, 36);
		box(root, "skirt_left", 0, 48, 14, 3, -16, 2, 4, 34);
		box(root, "skirt_right", 0, 48, -16, 3, -16, 2, 4, 34);
		box(root, "cabin", 0, 48, -12, 10, -8, 24, 10, 16);
		box(root, "roof", 0, 48, -13, 20, -6, 26, 2, 14);
		box(root, "windshield", 150, 0, -11, 12, -10, 22, 8, 2);
		box(root, "stripe", 0, 96, -2, 10, -30, 4, 1, 36);
		box(root, "wing_left", 0, 48, 8, 16, 16, 2, 12, 2);
		box(root, "wing_right", 0, 48, -10, 16, 16, 2, 12, 2);
		box(root, "wing", 0, 48, -16, 26, 14, 32, 2, 8);
		box(root, "light_left", 200, 0, 4, 6, -41, 6, 3, 2);
		box(root, "light_right", 200, 0, -10, 6, -41, 6, 3, 2);
		wheel(root, "wheel_fl", 16, -24);
		wheel(root, "wheel_fr", -16, -24);
		wheel(root, "wheel_bl", 16, 18);
		wheel(root, "wheel_br", -16, 18);
		return LayerDefinition.create(mesh, 256, 256);
	}

	private static void box(PartDefinition root, String name, int u, int v, float x, float bottom, float z, float width, float height, float depth) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, -(bottom + height), z, width, height, depth), PartPose.offset(0.0F, 24.0F, 0.0F));
	}

	private static void wheel(PartDefinition root, String name, float x, float z) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(150, 28).addBox(-2.0F, -5.0F, -5.0F, 4.0F, 10.0F, 10.0F), PartPose.offset(x, 19.0F, z));
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
		this.wheelFl.yRot = state.steer;
		this.wheelFr.yRot = state.steer;
	}
}
