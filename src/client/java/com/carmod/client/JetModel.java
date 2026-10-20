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
 * Single-engine fighter in the shape of the GTA Lazer: pointed nose, belly intake,
 * bubble canopy, swept wings with tip missiles, one tail fin. -Z is the nose.
 */
public class JetModel extends EntityModel<CarRenderState> {
	private final ModelPart gear;

	public JetModel(ModelPart root) {
		super(root, RenderTypes::armorCutoutNoCull);
		this.gear = root.getChild("gear");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		box(root, "nose", 0, 0, -2.0F, 5.0F, -30.0F, 4.0F, 3.0F, 8.0F);
		box(root, "body", 0, 0, -4.0F, 3.0F, -22.0F, 8.0F, 5.0F, 28.0F);
		box(root, "tail", 0, 0, -2.2F, 4.0F, 6.0F, 4.4F, 4.0F, 12.0F);
		box(root, "strake", 0, 80, -5.0F, 4.2F, -18.0F, 10.0F, 1.2F, 10.0F);
		box(root, "intake", 0, 80, -3.2F, 1.2F, -8.0F, 6.4F, 2.2F, 12.0F);
		box(root, "wing_left", 0, 80, -26.0F, 4.4F, -6.0F, 22.0F, 1.1F, 14.0F);
		box(root, "wing_right", 0, 80, 4.0F, 4.4F, -6.0F, 22.0F, 1.1F, 14.0F);
		box(root, "canopy", 140, 0, -3.0F, 8.0F, -18.0F, 6.0F, 4.0F, 10.0F);
		box(root, "canopy_frame", 0, 140, -3.2F, 7.6F, -8.2F, 6.4F, 0.6F, 0.8F);
		box(root, "fin", 0, 0, -0.7F, 8.0F, 8.0F, 1.4F, 10.0F, 10.0F);
		box(root, "stab_left", 0, 80, -10.0F, 6.2F, 10.0F, 8.0F, 0.8F, 6.0F);
		box(root, "stab_right", 0, 80, 2.0F, 6.2F, 10.0F, 8.0F, 0.8F, 6.0F);
		box(root, "nozzle", 140, 40, -2.6F, 4.0F, 16.0F, 5.2F, 4.2F, 3.0F);
		box(root, "stripe", 180, 40, -4.2F, 6.6F, -14.0F, 8.4F, 0.5F, 16.0F);
		box(root, "missile_left", 180, 40, -24.0F, 3.4F, -10.0F, 1.6F, 1.6F, 8.0F);
		box(root, "missile_right", 180, 40, 22.4F, 3.4F, -10.0F, 1.6F, 1.6F, 8.0F);
		box(root, "light_left", 180, 60, -26.2F, 4.6F, -6.2F, 1.2F, 0.8F, 1.2F);
		box(root, "light_right", 80, 140, 25.0F, 4.6F, -6.2F, 1.2F, 0.8F, 1.2F);
		PartDefinition gear = root.addOrReplaceChild("gear", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		gearBox(gear, "nose_gear", -0.5F, 0.0F, -16.0F, 1.0F, 3.2F, 1.0F);
		gearBox(gear, "main_left", -3.4F, 0.0F, 0.0F, 1.0F, 3.2F, 1.0F);
		gearBox(gear, "main_right", 2.4F, 0.0F, 0.0F, 1.0F, 3.2F, 1.0F);
		return LayerDefinition.create(mesh, 256, 256);
	}

	private static void box(PartDefinition root, String name, int u, int v, float x, float bottom, float z, float width, float height, float depth) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, -(bottom + height), z, width, height, depth), PartPose.offset(0.0F, 24.0F, 0.0F));
	}

	private static void gearBox(PartDefinition gear, String name, float x, float bottom, float z, float width, float height, float depth) {
		gear.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 140).addBox(x, -(bottom + height), z, width, height, depth), PartPose.ZERO);
	}

	@Override
	public void setupAnim(CarRenderState state) {
		super.setupAnim(state);
		this.resetPose();
		if (!state.gearDown) {
			this.gear.y -= 3.2F;
		}
	}
}
