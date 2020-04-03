package net.minecraft.client.renderer.entity.model;

import net.minecraft.entity.LivingEntity;

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
	}
}
