package org.vivecraft.render;

import org.lwjgl.opengl.ARBTextureEnvCombine;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.vivecraft.control.ControllerType;
import org.vivecraft.gameplay.trackers.SwingTracker;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.reflection.MCReflection;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.ItemStack;

public class VRArmRenderer extends PlayerRenderer
{

	public VRArmRenderer(EntityRendererManager p_i1295_1_) {
		super(p_i1295_1_);
	}
	
	public VRArmRenderer(EntityRendererManager p_i1295_1_, boolean small) {
		super(p_i1295_1_, small);
	}

	public void renderRightArm(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, AbstractClientPlayerEntity playerIn)
    {
        this.renderItem(ControllerType.RIGHT,matrixStackIn, bufferIn, combinedLightIn, playerIn, (this.entityModel).bipedRightArm, (this.entityModel).bipedRightArmwear);
    }

    public void renderLeftArm(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, AbstractClientPlayerEntity playerIn)
    {
        this.renderItem(ControllerType.LEFT, matrixStackIn, bufferIn, combinedLightIn, playerIn, (this.entityModel).bipedLeftArm, (this.entityModel).bipedLeftArmwear);
    }

    private void renderItem(ControllerType side, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, AbstractClientPlayerEntity playerIn, ModelRenderer rendererArmIn, ModelRenderer rendererArmwearIn)
    {
    	
        if (MCOpenVR.getInputAction(MCOpenVR.keyVRInteract).isEnabledRaw(side) ||
        		MCOpenVR.keyVRInteract.isKeyDown(side)|| 
        		MCOpenVR.getInputAction(MCOpenVR.keyClimbeyGrab).isEnabledRaw(side) ||
        		MCOpenVR.keyClimbeyGrab.isKeyDown(side)) {
        	GlStateManager.texEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
        	GlStateManager.texEnv(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_COMBINE_RGB_ARB, GL13.GL_MODULATE);
        	GlStateManager.texEnv(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_RGB_SCALE_ARB, 2);      	
        }
        
        PlayerModel<AbstractClientPlayerEntity> playermodel = this.getEntityModel();
		MCReflection.RenderPlayer_setModelVisibilities.invoke(this, playerIn);
        GlStateManager.enableBlend();
        GlStateManager.enableCull();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        
        playermodel.swingProgress = 0.0F;
        playermodel.isSneak = false;
        playermodel.swimAnimation = 0.0F;
      //  playermodel.setRotationAngles(playerIn, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        rendererArmIn.rotateAngleX = 0.0F;
        
        playermodel.bipedLeftArmwear.copyModelAngles(playermodel.bipedLeftArm);
        playermodel.bipedRightArmwear.copyModelAngles(playermodel.bipedRightArm);

        float a = SwingTracker.getItemFade((ClientPlayerEntity) playerIn, ItemStack.EMPTY);
        
        rendererArmIn.render(matrixStackIn, bufferIn.getBuffer(RenderType.getEntityTranslucent(playerIn.getLocationSkin())), combinedLightIn, OverlayTexture.NO_OVERLAY, 1,1,1,a);

        rendererArmwearIn.rotateAngleX = 0.0F;
        rendererArmwearIn.render(matrixStackIn, bufferIn.getBuffer(RenderType.getEntityTranslucent(playerIn.getLocationSkin())), combinedLightIn, OverlayTexture.NO_OVERLAY, 1,1,1,a);
        
    	GlStateManager.texEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_MODULATE);
    	GlStateManager.texEnv(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_RGB_SCALE_ARB, 1);      
        GlStateManager.disableBlend();
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0f);
    }

  
}
