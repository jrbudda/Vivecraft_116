package org.vivecraft.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;

public class VRHMDLayer<T extends LivingEntity, M extends EntityModel<T>> extends LayerRenderer<T, M>
{
	public VRHMDLayer(IEntityRenderer<T, M> p_i1345_1_) {
		super(p_i1345_1_);
	}

	@Override
	public void render(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, T entitylivingbaseIn,
			float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
			float headPitch) {

		ModelRenderer hmd = ((VRPlayerModel)this.getEntityModel()).vrHMD;
			
		if (hmd.showModel) {
			IVertexBuilder ivertexbuilder = bufferIn.getBuffer(RenderType.getEntitySolid(hmd.getTextureLocation()));
			int i = LivingRenderer.getPackedOverlay(entitylivingbaseIn, 0.0F);
			hmd.render(matrixStackIn, ivertexbuilder, packedLightIn, i);
		}
		
	}
}
