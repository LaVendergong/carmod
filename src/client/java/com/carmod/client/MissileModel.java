package com.carmod.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** A short missile. -Z is the nose. Painted from the helicopter texture. */
public class MissileModel extends EntityModel<CarRenderState> {
	public MissileModel(ModelPart root) {
		super(root, RenderTypes::armorCutoutNoCull);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		box(root, "body", 0, 108, -1.5F, 0.0F, -8.0F, 3.0F, 3.0F, 16.0F);
		box(root, "nose", 160, 42, -1.2F, 0.3F, -13.0F, 2.4F, 2.4F, 5.0F);
		box(root, "band", 190, 42, -1.5F, 0.0F, -1.0F, 3.0F, 3.0F, 1.5F);
		box(root, "fin_top", 0, 58, -0.4F, 3.0F, 4.0F, 0.8F, 4.0F, 4.0F);
		box(root, "fin_left", 0, 58, -5.0F, 1.0F, 4.0F, 4.0F, 1.0F, 4.0F);
		box(root, "fin_right", 0, 58, 1.0F, 1.0F, 4.0F, 4.0F, 1.0F, 4.0F);
		return LayerDefinition.create(mesh, 256, 256);
	}

	private static void box(PartDefinition root, String name, int u, int v, float x, float bottom, float z, float width, float height, float depth) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, -(bottom + height), z, width, height, depth), PartPose.offset(0.0F, 24.0F, 0.0F));
	}

	@Override
	public void setupAnim(CarRenderState state) {
		super.setupAnim(state);
		this.resetPose();
	}
}
