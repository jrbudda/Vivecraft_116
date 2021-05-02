package org.vivecraft.render;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

public class VRPlayerModel<T extends LivingEntity> extends PlayerModel<T>
{
    private final boolean smallArms;
    //VIVE START  
    public ModelRenderer leftShoulder;
    public ModelRenderer rightShoulder;
    public ModelRenderer leftHand;
    public ModelRenderer rightHand;
    public ModelRenderer vrHMD;
    ResourceLocation DIAMOND_HMD = new ResourceLocation("vivecraft:textures/diamond_hmd.png");
    ResourceLocation GOLD_HMD = new ResourceLocation("vivecraft:textures/gold_hmd.png");
    ResourceLocation BLACK_HMD = new ResourceLocation("vivecraft:textures/black_hmd.png");
    PlayerModelController.RotInfo rotInfo;
    public boolean seated;
    private boolean laying;
    
    //VIVE END
    public VRPlayerModel(float p_i125_1_, boolean p_i125_2_, boolean seated)
    {
    	super(p_i125_1_, p_i125_2_);
        this.smallArms = p_i125_2_;

        //Vivecraft
        this.vrHMD = new ModelRenderer(this, 0, 0);
        this.vrHMD.setTextureSize(16, 16);
        this.vrHMD.addBox(-3.5F, -6.0F, -7.5F, 7, 4, 5, p_i125_1_);
        this.vrHMD.setTextureLocation(BLACK_HMD);
      
        this.seated = seated;
        if(seated) return; //begging for a NPE
        
        //Dont use biped arms to prevent unwanted animations.
        this.rightHand = new ModelRenderer(this, 40, 16);
        this.rightHand.addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, p_i125_1_);
        this.rightHand.setRotationPoint(-5.0F, 2.0F + p_i125_1_, 0.0F);
        this.leftHand = new ModelRenderer(this, 40, 16);
        this.leftHand.mirror = true;
        this.leftHand.addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, p_i125_1_);
        this.leftHand.setRotationPoint(5.0F, 2.0F + p_i125_1_, 0.0F);
        
        //
        this.leftHand.setTextureOffset(32, 48+8);
        this.rightHand.setTextureOffset(40, 16+8);
        this.leftHand.cubeList.clear();
        this.rightHand.cubeList.clear();
        
        this.rightShoulder = new ModelRenderer(this, 40, 16);
        this.leftShoulder = new ModelRenderer(this, 32, 48);
        
        this.bipedLeftArmwear.cubeList.clear();
        this.bipedRightArmwear.cubeList.clear();
        this.bipedLeftArmwear.setTextureOffset(48, 48+8);
        this.bipedRightArmwear.setTextureOffset(40, 32+8);
        
        if (p_i125_2_)
        {
            this.leftHand.addBox(-1.0F, -2.0F, -2.0F, 3.0F, 4.0F, 4.0F, p_i125_1_);
            this.rightHand.addBox(-2.0F, -2.0F, -2.0F, 3.0F, 4.0F, 4.0F, p_i125_1_);
            this.bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 3.0F, 4.0F, 4.0F, p_i125_1_ + 0.25F);
            this.bipedRightArmwear.addBox(-2.0F, -2.0F, -2.0F, 3.0F, 4.0F, 4.0F, p_i125_1_ + 0.25F);  
            
	        this.leftShoulder.addBox(-1.0F, -2.0F, -2.0F, 3, 4, 4, p_i125_1_);
            this.rightShoulder.addBox(-2.0F, -2.0F, -2.0F, 3, 4, 4, p_i125_1_);
        }
        else
        {
            this.leftHand.addBox(-1.0F, -2.0F, -2.0F, 4, 4, 4, p_i125_1_);
        	this.rightHand.addBox(-3.0F, -2.0F, -2.0F, 4, 4, 4.0F, p_i125_1_);

            this.bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 4, 4, 4, p_i125_1_ + 0.25F);
            this.bipedRightArmwear.addBox(-3.0F, -2.0F, -2.0F, 4, 4, 4, p_i125_1_ + 0.25F);
	    
	        this.leftShoulder.addBox(-1.0F, -2.0F, -2.0F, 4, 4, 4, p_i125_1_);
            this.rightShoulder.addBox(-3.0F, -2.0F, -2.0F, 4, 4, 4, p_i125_1_);
        }
        //
    }
    
    protected Iterable<ModelRenderer> getBodyParts()
    {
    	if (seated)
    		return super.getBodyParts();
    	else		
    		return Iterables.concat(super.getBodyParts(), ImmutableList.of(this.leftShoulder, this.rightShoulder));
    }

    @Override
    public void setRotationAngles(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
    	super.setRotationAngles(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

    	this.rotInfo = PlayerModelController.getInstance().getRotationsForPlayer(((PlayerEntity)entityIn).getUniqueID());

    	//Lasciate ogne speranza, voi ch'intrate
    	PlayerModelController.RotInfo rotInfo = PlayerModelController.getInstance().getRotationsForPlayer(((PlayerEntity)entityIn).getUniqueID());
    	
    	if(rotInfo == null) return;
    	
    	double handsYOffset =-1.501f * rotInfo.heightScale ;
    //	if(isSneak) handsYOffset -= 0.1325f; //playerRenderer.getRenderOffset

    	float eyaw = (float) Math.toRadians(entityIn.rotationYaw);
    	float hmdYaw = (float) Math.atan2(-rotInfo.headRot.x, -rotInfo.headRot.z); 
    	float hmdPitch = (float) Math.asin(rotInfo.headRot.y/rotInfo.headRot.length()); 
    	float leftControllerYaw = (float) Math.atan2(-rotInfo.leftArmRot.x, -rotInfo.leftArmRot.z); 
    	float leftControllerPitch = (float) Math.asin(rotInfo.leftArmRot.y/rotInfo.leftArmRot.length());           	
    	float rightControllerYaw = (float) Math.atan2(-rotInfo.rightArmRot.x, -rotInfo.rightArmRot.z); 
    	float rightControllerPitch = (float) Math.asin(rotInfo.rightArmRot.y/rotInfo.rightArmRot.length()); 
    	double bodyYaw = rotInfo.getBodyYawRadians();	
	    	
    	//HEAD
    	this.bipedHead.rotateAngleX = (float) -hmdPitch;
    	this.bipedHead.rotateAngleY = (float) (Math.PI - hmdYaw - bodyYaw);
    	//
   	   	
		laying = this.swimAnimation > 0 || (entityIn.isElytraFlying() && !entityIn.isSpinAttacking());

    	if(!seated){
        	rightShoulder.showModel = true;
        	leftShoulder.showModel = true;
        	      	
    		//switcheroo and animateoo
    		if(!rotInfo.reverse){
    			this.rightShoulder.setRotationPoint(-MathHelper.cos(this.bipedBody.rotateAngleY) * 5.0F, smallArms ? 2.5F : 2.0F, MathHelper.sin(this.bipedBody.rotateAngleY) * 5.0F);
    			this.leftShoulder.setRotationPoint(MathHelper.cos(this.bipedBody.rotateAngleY) * 5.0F, smallArms ? 2.5F : 2.0F, -MathHelper.sin(this.bipedBody.rotateAngleY) * 5.0F);   							
    		} else {
    			this.leftShoulder.setRotationPoint(-MathHelper.cos(this.bipedBody.rotateAngleY) * 5.0F, smallArms ? 2.5F : 2.0F, MathHelper.sin(this.bipedBody.rotateAngleY) * 5.0F);
    			this.rightShoulder.setRotationPoint(MathHelper.cos(this.bipedBody.rotateAngleY) * 5.0F, smallArms ? 2.5F : 2.0F, -MathHelper.sin(this.bipedBody.rotateAngleY) * 5.0F);   							
    		}
    		
    		if(this.isSneak) {
    			this.rightShoulder.rotationPointY += 3.2f;
    			this.leftShoulder.rotationPointY += 3.2f;
    		}
    		//
    		
    		//Left Arm
    		Vector3d larm = rotInfo.leftArmPos.add(0,handsYOffset,0);
    		larm = larm.rotateYaw((float)(-Math.PI + bodyYaw));     		      		        		
    		larm = larm.scale(16/rotInfo.heightScale);
    		this.leftHand.setRotationPoint((float)-larm.x, (float)-larm.y, (float)larm.z);          
    		this.leftHand.rotateAngleX=(float) (-leftControllerPitch+ 3*Math.PI/2);
    		this.leftHand.rotateAngleY=(float) (Math.PI - leftControllerYaw - bodyYaw);
    		this.leftHand.rotateAngleZ = 0;
    		switch (this.leftArmPose) {
				case THROW_SPEAR:
					leftHand.rotateAngleX -= Math.PI/2;
					break;
	    		}		
    		//
    		
    		//Left Shoulder
    		Vector3d lsh = new Vector3d(leftShoulder.rotationPointX + larm.x, 
    				leftShoulder.rotationPointY + larm.y,
    				leftShoulder.rotationPointZ - larm.z);
    		
    		float yawls =  (float) (Math.atan2(lsh.x, lsh.z)); 
    		float pitchls = (float)(3*Math.PI/2-Math.asin(lsh.y/lsh.length())); 		
    		
      		leftShoulder.rotateAngleZ = 0;		
      		leftShoulder.rotateAngleX =pitchls;	
    		leftShoulder.rotateAngleY = yawls;
    		if(leftShoulder.rotateAngleY > 0)
        		leftShoulder.rotateAngleY = 0;
    		//
    		
    		//Right arm
    		Vector3d rarm = rotInfo.rightArmPos.add(0,handsYOffset,0);
        	rarm = rarm.rotateYaw((float)(-Math.PI + bodyYaw));      
    	
        	rarm = rarm.scale(16/rotInfo.heightScale); //because.
    		this.rightHand.setRotationPoint((float)-rarm.x, -(float)rarm.y, (float)rarm.z);   
    		this.rightHand.rotateAngleX=(float) (-rightControllerPitch+ 3*Math.PI/2);
    		this.rightHand.rotateAngleY=(float) (Math.PI-rightControllerYaw - bodyYaw);
    		this.rightHand.rotateAngleZ = 0;
    		switch (this.rightArmPose) {
				case THROW_SPEAR:
					rightHand.rotateAngleX -= Math.PI/2;
					break;
	    		}		
    		//
    		//
    		
    		//Right shoulder
    		Vector3d rsh = new Vector3d(rightShoulder.rotationPointX + rarm.x, 
    				rightShoulder.rotationPointY + rarm.y,
    				rightShoulder.rotationPointZ - rarm.z);

    		float yawrs = (float) Math.atan2(rsh.x, rsh.z); 
    		float pitchrs = (float) (3*Math.PI/2-Math.asin(rsh.y/rsh.length())); 	
    		
    		rightShoulder.rotateAngleZ = 0;
    		rightShoulder.rotateAngleX = pitchrs;
    		rightShoulder.rotateAngleY = yawrs;
    		if(rightShoulder.rotateAngleY < 0)
    			rightShoulder.rotateAngleY = 0;
    		//  		

        	if(laying) {
        		this.bipedHead.rotationPointZ = 0;
        		this.bipedHead.rotationPointX = 0f;
        		this.bipedHead.rotationPointY = -16*0.25f;
        		this.bipedHead.rotateAngleX-=Math.PI/2;
        		this.rightShoulder.rotateAngleX-=Math.PI/2;
        		this.leftShoulder.rotateAngleX-=Math.PI/2;
        	} else {
        		this.bipedHead.rotationPointZ = 0;
        		this.bipedHead.rotationPointX = 0;
        		this.bipedHead.rotationPointY = 0;
        	}
    	}

    	this.vrHMD.showModel = true;
    	switch(rotInfo.hmd){
    	case 0:
    		this.vrHMD.showModel = false;
    		break;
    	case 1:
    		this.vrHMD.setTextureLocation(this.BLACK_HMD);
    		break;
    	case 2:
    		this.vrHMD.setTextureLocation(this.GOLD_HMD);
    		break;
    	case 3:
    		this.vrHMD.setTextureLocation(this.DIAMOND_HMD);	
    		break;
    	case 4:
    		this.vrHMD.setTextureLocation(this.DIAMOND_HMD);	
    		break;
    	}

    	this.vrHMD.copyModelAngles(this.bipedHead);
    	this.bipedHeadwear.copyModelAngles(this.bipedHead);  	
    	this.bipedBodyWear.copyModelAngles(this.bipedBody);
    	this.bipedLeftLegwear.copyModelAngles(this.bipedLeftLeg);
    	this.bipedRightLegwear.copyModelAngles(this.bipedRightLeg);
    	if(seated) {
    	this.bipedLeftArmwear.copyModelAngles(this.bipedLeftArm);
    	this.bipedRightArmwear.copyModelAngles(this.bipedRightArm);
    	} else {
        	this.bipedLeftArmwear.copyModelAngles(this.leftHand);
        	this.bipedRightArmwear.copyModelAngles(this.rightHand);
    	}
    	this.bipedBodyWear.copyModelAngles(this.bipedBody);
    }
    
	@Override
	public void setModelAttributes(BipedModel<T> modelIn) {
        super.copyModelAttributesTo(modelIn);
        modelIn.leftArmPose = this.leftArmPose;
        modelIn.rightArmPose = this.rightArmPose;
        modelIn.isSneak = this.isSneak;
        modelIn.bipedHead.copyModelAngles(this.bipedHead);
        modelIn.bipedHeadwear.copyModelAngles(this.bipedHeadwear);
        modelIn.bipedBody.copyModelAngles(this.bipedBody);
        if(this.rightShoulder != null) {
	        modelIn.bipedRightArm.copyModelAngles(this.rightShoulder);
	        modelIn.bipedLeftArm.copyModelAngles(this.leftShoulder);
        } else {
            modelIn.bipedRightArm.copyModelAngles(this.bipedRightArm);
            modelIn.bipedLeftArm.copyModelAngles(this.bipedLeftArm);
        }
        modelIn.bipedRightLeg.copyModelAngles(this.bipedRightLeg);
        modelIn.bipedLeftLeg.copyModelAngles(this.bipedLeftLeg);
	}
	
    @Override
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);
        if(!seated) {
        	this.rightShoulder.showModel = visible;
        	this.leftShoulder.showModel = visible;
        	this.rightHand.showModel = visible;
        	this.leftHand.showModel = visible;
        	this.bipedLeftArm.showModel = false;
         	this.bipedRightArm.showModel = false;
        }
    }
    
    @Override
    protected ModelRenderer getArmForSide(HandSide side)
    {
    	if(seated)
    		return side == HandSide.LEFT ? this.bipedLeftArm : this.bipedRightArm;
    	else
    		return side == HandSide.LEFT ? this.leftHand : this.rightHand;
    }
    
    @Override
    public void translateHand(HandSide sideIn, MatrixStack matrixStackIn)
    {
    	if(!seated) {
	        ModelRenderer modelrenderer = this.getArmForSide(sideIn);

    		if(laying) 
    			matrixStackIn.rotate(Vector3f.XP.rotationDegrees(-90));

	        modelrenderer.translateRotate(matrixStackIn);
    		matrixStackIn.rotate(Vector3f.XP.rotation((float) Math.sin(swingProgress * Math.PI)));

	        matrixStackIn.translate(0, -0.5, 0);

    	} else
    		super.translateHand(sideIn, matrixStackIn);   		
    }

    @Override
    public void render(MatrixStack matrixStackIn, IVertexBuilder bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha)
    {
   		this.bipedBody.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
   		this.bipedBodyWear.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
   		this.bipedLeftLeg.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
   		this.bipedRightLeg.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
   		this.bipedLeftLegwear.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
   		this.bipedRightLegwear.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
   		matrixStackIn.push();
	   		this.bipedHead.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
	   		this.bipedHeadwear.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
	   		this.vrHMD.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
	   		if(seated) {
		   		this.bipedLeftArm.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
		   		this.bipedRightArm.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
	   		} else {
		   		this.leftShoulder.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
		   		this.rightShoulder.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
		   		if(laying) 
		   			matrixStackIn.rotate(Vector3f.XP.rotationDegrees(-90));
		   		this.rightHand.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
		   		this.leftHand.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
	   		}
	   		this.bipedLeftArmwear.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
	   		this.bipedRightArmwear.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
   		matrixStackIn.pop();
    }
}
