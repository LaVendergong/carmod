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
 * Light helicopter: stepped nose, cabin glass, engine deck, tail boom and a two-blade rotor.
 * 16 pixels are one block. +X is the right side, -Z is the front.
 */
public class HelicopterModel extends EntityModel<CarRenderState> {
	private final ModelPart rotor;
	private final ModelPart tailRotor;

	public HelicopterModel(ModelPart root) {
		super(root, RenderTypes::armorCutoutNoCull);
		this.rotor = root.getChild("rotor");
		this.tailRotor = root.getChild("tail_rotor");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		box(root, "belly", 160, 56, -8, 2, -8, 16, 2, 18);
		box(root, "nose_tip", 0, 0, -4, 6, -30, 8, 4, 8);
		box(root, "nose", 0, 0, -7, 5, -22, 14, 7, 12);
		box(root, "cabin_low", 0, 0, -10, 4, -12, 20, 6, 22);
		box(root, "cabin", 0, 0, -9, 10, -10, 18, 7, 16);
		box(root, "roof", 0, 0, -8, 17, -8, 16, 2, 12);
		box(root, "engine", 0, 0, -8, 12, 4, 16, 8, 12);
		box(root, "engine_top", 0, 0, -6, 20, 6, 12, 3, 8);
		box(root, "exhaust", 160, 56, 8, 14, 8, 3, 2, 6);
		box(root, "intake", 0, 58, -5, 21, 7, 10, 2, 6);
		box(root, "windshield", 160, 0, -6, 11, -21, 12, 6, 2);
		box(root, "chin_window", 160, 0, -4, 6, -28, 8, 2, 4);
		box(root, "window_left", 160, 0, 9, 11, -8, 1, 5, 10);
		box(root, "window_right", 160, 0, -10, 11, -8, 1, 5, 10);
		box(root, "door", 0, 58, 9, 6, -6, 1, 8, 1);
		box(root, "boom", 0, 0, -2, 9, 16, 4, 4, 22);
		box(root, "shaft", 0, 0, -1, 13, 16, 2, 1, 20);
		box(root, "fin", 0, 0, -1, 12, 36, 2, 14, 7);
		box(root, "stab", 0, 0, -9, 14, 38, 18, 1, 4);
		box(root, "endplate_l", 0, 0, 8, 13, 38, 1, 4, 4);
		box(root, "endplate_r", 0, 0, -9, 13, 38, 1, 4, 4);
		box(root, "tail_skid", 0, 58, -1, 5, 40, 2, 5, 2);
		box(root, "mast", 0, 58, -1.5F, 23, 8, 3, 6, 3);
		box(root, "antenna", 0, 58, -0.5F, 19, -6, 1, 4, 1);
		box(root, "skid_left", 0, 58, 8, 0, -18, 2, 2, 32);
		box(root, "skid_right", 0, 58, -10, 0, -18, 2, 2, 32);
		box(root, "strut_fl", 0, 58, 7, 2, -10, 2, 4, 2);
		box(root, "strut_bl", 0, 58, 7, 2, 6, 2, 4, 2);
		box(root, "strut_fr", 0, 58, -9, 2, -10, 2, 4, 2);
		box(root, "strut_br", 0, 58, -9, 2, 6, 2, 4, 2);
		box(root, "step", 0, 58, 8, 3, -2, 4, 1, 6);
		box(root, "light_left", 190, 42, 8, 8, -20, 2, 2, 2);
		box(root, "light_right", 202, 42, -10, 8, -20, 2, 2, 2);
		box(root, "light_nose", 214, 42, -2, 5, -30, 4, 2, 2);
		box(root, "light_tail", 214, 42, -1, 16, 42, 2, 2, 2);
		root.addOrReplaceChild("rotor", CubeListBuilder.create()
				.texOffs(0, 100).addBox(-34.0F, -0.4F, -1.2F, 68.0F, 0.8F, 2.4F)
				.texOffs(0, 108).addBox(-3.0F, -1.5F, -3.0F, 6.0F, 2.5F, 6.0F)
				.texOffs(160, 42).addBox(-37.0F, -0.5F, -1.4F, 3.0F, 1.0F, 2.8F)
				.texOffs(160, 42).addBox(34.0F, -0.5F, -1.4F, 3.0F, 1.0F, 2.8F), PartPose.offset(0.0F, -5.0F, 9.0F));
		root.addOrReplaceChild("tail_rotor", CubeListBuilder.create()
				.texOffs(0, 100).addBox(-0.4F, -6.0F, -0.6F, 0.8F, 12.0F, 1.2F)
				.texOffs(0, 108).addBox(-0.8F, -1.2F, -1.2F, 1.6F, 2.4F, 2.4F), PartPose.offset(3.0F, 7.0F, 39.0F));
		box(root, "guard_up", 0, 58, 2, 13, 36, 1, 1, 8);
		box(root, "guard_down", 0, 58, 2, 4, 36, 1, 1, 8);
		return LayerDefinition.create(mesh, 256, 256);
	}

	private static void box(PartDefinition root, String name, int u, int v, float x, float bottom, float z, float width, float height, float depth) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, -(bottom + height), z, width, height, depth), PartPose.offset(0.0F, 24.0F, 0.0F));
	}

	@Override
	public void setupAnim(CarRenderState state) {
		super.setupAnim(state);
		this.resetPose();
		this.rotor.yRot = state.wheelAngle;
		this.tailRotor.xRot = state.wheelAngle * 2.2F;
	}
}
