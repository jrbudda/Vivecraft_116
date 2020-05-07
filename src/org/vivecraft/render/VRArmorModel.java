package org.vivecraft.render;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.IHasArm;
import net.minecraft.client.renderer.entity.model.IHasHead;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public class VRArmorModel<T extends LivingEntity> extends BipedModel<T> implements IHasArm, IHasHead
{
	public VRArmorModel(float p_i305_1_) {
		super(p_i305_1_);
	}

	@Override
	public void setRotationAngles(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks,
			float netHeadYaw, float headPitch) {
		//Sometimes its best just to do nothing.
		//this is handled in VRPlayerModel.
		
    	PlayerModelController.RotInfo rotInfo = PlayerModelController.getInstance().getRotationsForPlayer(((PlayerEntity)entityIn).getUniqueID());
    	if(rotInfo == null) return;
    	if(rotInfo.seated) {
    		float tempx = this.bipedHead.rotateAngleX;
    		float tempy = this.bipedHead.rotateAngleY;
    		float tempz = this.bipedHead.rotateAngleZ;
    		super.setRotationAngles(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    		this.bipedHead.rotateAngleX = tempx;
    		this.bipedHead.rotateAngleY = tempy;
    		this.bipedHead.rotateAngleZ = tempz;
    	}
		
	}
}
