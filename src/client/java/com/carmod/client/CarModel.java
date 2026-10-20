package com.carmod.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class CarModel extends EntityModel<CarRenderState> {
	private final ModelPart wheelFl;
	private final ModelPart wheelFr;
	private final ModelPart wheelBl;
	private final ModelPart wheelBr;

	public CarModel(ModelPart root) {
		super(root, RenderTypes::armorCutoutNoCull);
		this.wheelFl = root.getChild("wheel_fl");
		this.wheelFr = root.getChild("wheel_fr");
		this.wheelBl = root.getChild("wheel_bl");
		this.wheelBr = root.getChild("wheel_br");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// 16 pixels = 1 block. +X is the driver's side, -Z is the front, bottom is pixels above the ground.
		// texOffs (u, v) must sit on a solid area of textures/entity/car.png at least 2*(width+depth) by (height+depth).
		box(root, "chassis", 0, 0, -15, 3, -42, 30, 6, 72);
		box(root, "hood", 0, 80, -13, 8, -42, 26, 5, 22);
		box(root, "trunk", 100, 80, -13, 8, 20, 26, 8, 12);
		box(root, "bumper_front", 0, 188, -15, 2, -46, 30, 5, 5);
		box(root, "bumper_rear", 0, 188, -15, 2, 31, 30, 5, 5);

		box(root, "door_left", 0, 110, 13, 9, -2, 2, 14, 22);
		box(root, "door_right", 52, 110, -15, 9, -2, 2, 14, 22);
		box(root, "roof", 0, 152, -15, 37, -8, 30, 3, 30);
		box(root, "pillar_fl", 120, 110, 12, 16, -10, 3, 22, 3);
		box(root, "pillar_fr", 120, 110, -15, 16, -10, 3, 22, 3);
		box(root, "pillar_bl", 120, 110, 12, 16, 18, 3, 22, 3);
		box(root, "pillar_br", 120, 110, -15, 16, 18, 3, 22, 3);
		box(root, "header", 140, 110, -12, 34, -10, 24, 4, 3);
		box(root, "rear_lower", 140, 122, -13, 9, 20, 26, 12, 3);
		box(root, "rear_upper", 140, 140, -13, 28, 20, 26, 10, 3);

		box(root, "dash", 124, 152, -11, 12, -12, 22, 7, 6);
		box(root, "steer_up", 210, 140, 5, 18, -5, 1, 8, 1);
		box(root, "steer_across", 210, 140, 2, 21, -5, 7, 1, 1);
		box(root, "light_left", 200, 110, 4, 9, -45, 5, 3, 4);
		box(root, "light_right", 200, 110, -9, 9, -45, 5, 3, 4);
		box(root, "grille", 200, 122, -6, 8, -45, 12, 3, 4);

		box(root, "seat_driver", 124, 176, 2, 9, 2, 8, 3, 10);
		box(root, "seat_driver_back", 168, 176, 2, 12, 10, 8, 12, 3);
		box(root, "seat_passenger", 124, 176, -10, 9, 2, 8, 3, 10);
		box(root, "seat_passenger_back", 168, 176, -10, 12, 10, 8, 12, 3);

		wheel(root, "wheel_fl", 18, -30);
		wheel(root, "wheel_fr", -18, -30);
		wheel(root, "wheel_bl", 18, 16);
		wheel(root, "wheel_br", -18, 16);
		return LayerDefinition.create(mesh, 256, 256);
	}

	private static void box(PartDefinition root, String name, int u, int v, float x, float bottom, float z, float width, float height, float depth) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, -(bottom + height), z, width, height, depth), PartPose.offset(0.0F, 24.0F, 0.0F));
	}

	private static void wheel(PartDefinition root, String name, float x, float z) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(200, 80).addBox(-2.0F, -5.0F, -5.0F, 4.0F, 10.0F, 10.0F), PartPose.offset(x, 19.0F, z));
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
	}
}
