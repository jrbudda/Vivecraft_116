package net.minecraft.client.renderer.entity.model;

import org.vivecraft.render.PlayerModelController;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;

public class VRArmorModel<T extends LivingEntity> extends BipedModel<T> implements IHasArm, IHasHead
{
	
	public VRArmorModel(float f) {
		super(f);
	}
	
	@Override
	public void setVisible(boolean visible) {
		// TODO Auto-generated method stub
		super.setVisible(visible);
	}
	
	@Override
    public void render(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale)
    {
		

		float x,y, xls, yls, xrs, yrs;
    	x = this.bipedHead.rotateAngleX;
    	y = this.bipedHead.rotateAngleY;
    	xls = this.bipedLeftArm.rotateAngleX;
    	yls = this.bipedLeftArm.rotateAngleY;
    	xrs = this.bipedRightArm.rotateAngleX;
    	yrs = this.bipedRightArm.rotateAngleY;
    	
        this.setRotationAngles(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        
    	PlayerModelController.RotInfo rotInfo = PlayerModelController.getInstance().getRotationsForPlayer(((PlayerEntity)entityIn).getUniqueID());

    	this.bipedHead.rotateAngleX = x;
    	this.bipedHead.rotateAngleY = y;
    	
    	if(rotInfo !=null && rotInfo.seated == false) {
	    	this.bipedLeftArm.rotateAngleX = xls;
	    	this.bipedLeftArm.rotateAngleY = yls;
	    	this.bipedRightArm.rotateAngleX = xrs;
	    	this.bipedRightArm.rotateAngleY = yrs;
    	}
    	
        GlStateManager.pushMatrix();

        if (this.isChild)
        {
            float f = 2.0F;
            GlStateManager.scalef(0.75F, 0.75F, 0.75F);
            GlStateManager.translatef(0.0F, 16.0F * scale, 0.0F);
            this.bipedHead.render(scale);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.scalef(0.5F, 0.5F, 0.5F);
            GlStateManager.translatef(0.0F, 24.0F * scale, 0.0F);
            this.bipedBody.render(scale);
            this.bipedRightArm.render(scale);
            this.bipedLeftArm.render(scale);
            this.bipedRightLeg.render(scale);
            this.bipedLeftLeg.render(scale);
            this.bipedHeadwear.render(scale);
        }
        else
        {
            if (entityIn.shouldRenderSneaking())
            {
                GlStateManager.translatef(0.0F, 0.2F, 0.0F);
            }

            this.bipedHead.render(scale);
            this.bipedBody.render(scale);
            this.bipedRightArm.render(scale);
            this.bipedLeftArm.render(scale);
            this.bipedRightLeg.render(scale);
            this.bipedLeftLeg.render(scale);
            this.bipedHeadwear.render(scale);
        }

        GlStateManager.popMatrix();
    }
}
