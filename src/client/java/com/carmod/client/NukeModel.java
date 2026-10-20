package com.carmod.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** A standing warhead. Ground line is model y=24. */
public class NukeModel extends EntityModel<CarRenderState> {
	public NukeModel(ModelPart root) {
		super(root, RenderTypes::armorCutoutNoCull);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -16.0F, -4.0F, 8.0F, 14.0F, 8.0F), PartPose.offset(0.0F, 24.0F, 0.0F));
		root.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(0, 22).addBox(-2.5F, -20.0F, -2.5F, 5.0F, 4.0F, 5.0F), PartPose.offset(0.0F, 24.0F, 0.0F));
		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public void setupAnim(CarRenderState state) {
		super.setupAnim(state);
		this.resetPose();
	}
}
