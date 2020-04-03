package net.minecraft.client.renderer.entity.model;

import org.vivecraft.render.PlayerModelController;
import org.vivecraft.render.RenderPass;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class VRPlayerModel<T extends LivingEntity> extends PlayerModel<T>
{
    private final boolean smallArms;
    //VIVE START  
    public ModelRenderer leftShoulder;
    public ModelRenderer rightShoulder;
    public ModelRenderer vrHMD;
    public Vec3d renderPos;
    ResourceLocation DIAMOND_HMD = new ResourceLocation("vivecraft:textures/diamond_hmd.png");
    ResourceLocation GOLD_HMD = new ResourceLocation("vivecraft:textures/gold_hmd.png");
    ResourceLocation BLACK_HMD = new ResourceLocation("vivecraft:textures/black_hmd.png");
    public BipedArmorLayer armor = null;
    PlayerModelController.RotInfo rotInfo;
    
    //VIVE END
    public VRPlayerModel(float p_i125_1_, boolean p_i125_2_)
    {
    	super(p_i125_1_, p_i125_2_);
        this.smallArms = p_i125_2_;

        //Vivecraft
        this.vrHMD = new ModelRenderer(this, 0, 0);
        this.vrHMD.setTextureSize(16, 16);
        this.vrHMD.addBox(-3.5F, -6.0F, -7.5F, 7, 4, 5, p_i125_1_);
        this.vrHMD.setTextureLocation(BLACK_HMD);
      
        this.bipedLeftArm.setTextureOffset(32, 48+8);
        this.bipedRightArm.setTextureOffset(40, 16+8);
        this.bipedLeftArm.cubeList.clear();
        this.bipedRightArm.cubeList.clear();
        
        this.rightShoulder = new ModelRenderer(this, 40, 16);
        this.leftShoulder = new ModelRenderer(this, 32, 48);
        this.leftShoulder.mirror = true;
        
        this.bipedLeftArmwear.cubeList.clear();
        this.bipedRightArmwear.cubeList.clear();
        this.bipedLeftArmwear.setTextureOffset(48, 48+8);
        this.bipedRightArmwear.setTextureOffset(40, 32+8);
        
        if (p_i125_2_)
        {
            this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 3.0F, 4.0F, 4.0F, p_i125_1_);
            this.bipedRightArm.addBox(-2.0F, -2.0F, -2.0F, 3.0F, 4.0F, 4.0F, p_i125_1_);
            this.bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 3.0F, 4.0F, 4.0F, p_i125_1_ + 0.25F);
            this.bipedRightArmwear.addBox(-2.0F, -2.0F, -2.0F, 3.0F, 4.0F, 4.0F, p_i125_1_ + 0.25F);  
            
	        this.leftShoulder.addBox(-1.0F, -2.0F, -2.0F, 3, 4, 4, p_i125_1_);
            this.rightShoulder.addBox(-2.0F, -2.0F, -2.0F, 3, 4, 4, p_i125_1_);
        }
        else
        {
            this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 4, 4, p_i125_1_);
        	this.bipedRightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 4, 4.0F, p_i125_1_);

            this.bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 4, 4, 4, p_i125_1_ + 0.25F);
            this.bipedRightArmwear.addBox(-3.0F, -2.0F, -2.0F, 4, 4, 4, p_i125_1_ + 0.25F);
	    
	        this.leftShoulder.addBox(-1.0F, -2.0F, -2.0F, 4, 4, 4, p_i125_1_);
            this.rightShoulder.addBox(-3.0F, -2.0F, -2.0F, 4, 4, 4, p_i125_1_);
        }
        //
    }
    
    protected Iterable<ModelRenderer> getBodyParts()
    {
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
    	rotInfo.heightScale = 1;
    	
    	double handsYOffset =-1.501f * rotInfo.heightScale;
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

    	//ARMS (default)
    	this.bipedLeftArm.setRotationPoint(5.0F, smallArms ? 2.5F : 2.0F, 0.0F);
    	Vec3d rotatedlarm = new Vec3d(bipedLeftArm.rotationPointX, bipedLeftArm.rotationPointY, bipedLeftArm.rotationPointZ);
    	this.bipedLeftArm.setRotationPoint((float)rotatedlarm.x, (float)rotatedlarm.y, (float)rotatedlarm.z);
    	this.bipedRightArm.setRotationPoint(-5.0F, smallArms ? 2.5F : 2.0F, 0.0F);
    	Vec3d rotatedrarm = new Vec3d(bipedRightArm.rotationPointX, bipedRightArm.rotationPointY, bipedRightArm.rotationPointZ);
    	this.bipedRightArm.setRotationPoint((float)rotatedrarm.x, (float)rotatedrarm.y, (float)rotatedrarm.z);
    	//
    	
		//switcheroo
		if(rotInfo.reverse){
			this.rightShoulder.setRotationPoint(5.0F, smallArms ? 2.5F : 2.0F, 0.0F);
			this.leftShoulder.setRotationPoint(-5.0F, smallArms ? 2.5F : 2.0F, 0.0F);
		} else {
			this.rightShoulder.setRotationPoint(-5.0F, smallArms ? 2.5F : 2.0F, 0.0F);
			this.leftShoulder.setRotationPoint(5.0F, smallArms ? 2.5F : 2.0F, 0.0F);
		}
		//

    	if(renderPos !=null){
    		
    		//Left Arm
    		Vec3d larm = rotInfo.leftArmPos.subtract(renderPos).add(0,handsYOffset,0);
    	//	larm = larm.rotateYaw((float)(-Math.PI + bodyYaw)).add(rotInfo.leftArmRot.scale(-0.2)).scale(-1/1);    
    		larm = larm.rotateYaw((float)(-Math.PI + bodyYaw));     		      		        		
    		larm = larm.scale(16);
    		this.bipedLeftArm.setRotationPoint((float)-larm.x, (float)-larm.y, (float)larm.z);          
    		this.bipedLeftArm.rotateAngleX=(float) (-leftControllerPitch+ 3*Math.PI/2);
    		this.bipedLeftArm.rotateAngleY=(float) (Math.PI - leftControllerYaw - bodyYaw);
    		//
    		
    		//Left Shoulder
    		Vec3d lsh = new Vec3d(leftShoulder.rotationPointX - larm.x, 
    				leftShoulder.rotationPointY - larm.y,
    				leftShoulder.rotationPointZ - larm.z);
    		
    		float yawls =  (float) ( -Math.atan2(-lsh.x, -lsh.z)); 
    		float pitchls = (float)( -Math.asin(lsh.y/lsh.length()) + 1*Math.PI/2); 		
    		
//    		Float a = (float) (Math.random() * Math.PI*4 - Math.PI*2);
//    		Float b = (float) (Math.random() * Math.PI*4 - Math.PI*2);		
//    		yawls = a;
//    		pitchls = b;
    		
//    		if (yawls > 0)
//    			yawls = 0;
//    		
//    		if (yawls < -Math.PI/2)
//    			yawls = (float) (-Math.PI/2);
    		
    	//	leftShoulder.rotateAngleY = (float) (-(System.currentTimeMillis() % 1000) / 1000f * 2* Math.PI);
    		leftShoulder.rotateAngleY = yawls;
    		
    		//       		if (pitchls > 3*Math.PI)
//       			pitchls = (float) (3*Math.PI);
//    		if (pitchls < 0)
//    			pitchls = 0;

    		leftShoulder.rotateAngleX =pitchls;

    		//
    		
    		//Right arm
    		Vec3d rarm = rotInfo.rightArmPos.subtract(renderPos).add(0,handsYOffset,0);
    	//	rarm = rarm.rotateYaw((float)(-Math.PI + bodyYaw)).add(rotInfo.rightArmRot.scale(-0.2)).scale(-1/1);    
        	rarm = rarm.rotateYaw((float)(-Math.PI + bodyYaw));      
    	
        	rarm = rarm.scale(16); //because.
    		this.bipedRightArm.setRotationPoint((float)-rarm.x, -(float)rarm.y, (float)rarm.z);   
    		this.bipedRightArm.rotateAngleX=(float) (-rightControllerPitch+ 3*Math.PI/2);
    		this.bipedRightArm.rotateAngleY=(float) (Math.PI-rightControllerYaw - bodyYaw);
    		//
    		
    		//Right shoulder
    		Vec3d rsh = new Vec3d(rightShoulder.rotationPointX - rarm.x, 
    				rightShoulder.rotationPointY - rarm.y,
    				rightShoulder.rotationPointZ - rarm.z);

    		float yawrs = (float) -Math.atan2(-rsh.x, -rsh.z); 
    		float pitchrs = (float) (-Math.asin(rsh.y/rsh.length()) + 1*Math.PI/2); 	
    		
    	//	rightShoulder.rotateAngleY = (float) ((System.currentTimeMillis() % 1000) / 1000f * 2* Math.PI);
    		rightShoulder.rotateAngleY = yawrs;

//    		if (yawrs < 0)
//    			yawrs = 0;   		
//    		if (yawrs > Math.PI/2)
//    			yawrs = (float) (Math.PI/2);
//    		
//       		if (pitchrs > 3*Math.PI)
//       			pitchrs = (float) (3*Math.PI);
//    		if (pitchrs < 0)
//    			pitchrs = 0;
    		
    		rightShoulder.rotateAngleX = pitchrs;
    //    	rightShoulder.rotateAngleX = (float) ((System.currentTimeMillis() % 1000) / 1000f * 2* Math.PI);

    	//	System.out.println("lsh: " + Math.toDegrees(leftShoulder.rotateAngleX) + " : " + Math.toDegrees(leftShoulder.rotateAngleY));
    //		System.out.println("rsh: " + Math.toDegrees(rightShoulder.rotateAngleX) + " : " + Math.toDegrees(rightShoulder.rotateAngleY));

    		//  		

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
    	if(this.armor != null) { //wtf
	    	this.armor.getModelFromSlot(EquipmentSlotType.HEAD).bipedHead.copyModelAngles(this.bipedHead);
	    	this.armor.getModelFromSlot(EquipmentSlotType.HEAD).bipedBody.copyModelAngles(this.bipedBody);
	    	
	    	//intentionaly reversed.
	    	this.armor.getModelFromSlot(EquipmentSlotType.HEAD).bipedRightArm.copyModelAngles(this.leftShoulder);
	    	this.armor.getModelFromSlot(EquipmentSlotType.HEAD).bipedLeftArm.copyModelAngles(this.rightShoulder);    
	    	
	    	this.armor.getModelFromSlot(EquipmentSlotType.LEGS).bipedRightLeg.copyModelAngles(this.bipedRightLeg);
	    	this.armor.getModelFromSlot(EquipmentSlotType.LEGS).bipedLeftLeg.copyModelAngles(this.bipedLeftLeg);
	    	
	    	this.armor.getModelFromSlot(EquipmentSlotType.FEET).bipedRightLeg.copyModelAngles(this.bipedRightLeg);
	    	this.armor.getModelFromSlot(EquipmentSlotType.FEET).bipedLeftLeg.copyModelAngles(this.bipedLeftLeg);

    	}
    	this.bipedBodyWear.copyModelAngles(this.bipedBody);
    	this.bipedLeftLegwear.copyModelAngles(this.bipedLeftLeg);
    	this.bipedRightLegwear.copyModelAngles(this.bipedRightLeg);
    	this.bipedLeftArmwear.copyModelAngles(this.bipedLeftArm);
    	this.bipedRightArmwear.copyModelAngles(this.bipedRightArm);
    	this.bipedBodyWear.copyModelAngles(this.bipedBody);
    }

    @Override
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);
        this.rightShoulder.showModel = visible;
        this.leftShoulder.showModel = visible;
    }
    
    @Override
    public void translateHand(HandSide sideIn, MatrixStack matrixStackIn)
    {
        ModelRenderer modelrenderer = this.getArmForSide(sideIn);
        modelrenderer.translateRotate(matrixStackIn);
        //un-scootch
        matrixStackIn.translate(0, -0.5, 0);
        //
    }

}
