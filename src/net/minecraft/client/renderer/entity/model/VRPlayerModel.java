package net.minecraft.client.renderer.entity.model;

import org.vivecraft.render.PlayerModelController;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class VRPlayerModel<T extends LivingEntity> extends BipedModel<T>
{
    public final ModelRenderer bipedLeftArmwear;
    public final ModelRenderer bipedRightArmwear;
    public final ModelRenderer bipedLeftLegwear;
    public final ModelRenderer bipedRightLegwear;
    public final ModelRenderer bipedBodyWear;
    private final ModelRenderer bipedCape;
    private final ModelRenderer bipedDeadmau5Head;
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
    //VIVE END
    
    public VRPlayerModel(float p_i63_1_, boolean p_i63_2_)
    {
        super(p_i63_1_, 0.0F, 64, 64);
        this.smallArms = p_i63_2_;
        this.bipedDeadmau5Head = new ModelRenderer(this, 24, 0);
        this.bipedDeadmau5Head.addBox(-3.0F, -6.0F, -1.0F, 6, 6, 1, p_i63_1_);
        this.bipedCape = new ModelRenderer(this, 0, 0);
        this.bipedCape.setTextureSize(64, 32);
        this.bipedCape.addBox(-5.0F, 0.0F, -1.0F, 10, 16, 1, p_i63_1_);
        this.vrHMD = new ModelRenderer(this, 0, 0);
        this.vrHMD.setTextureSize(16, 16);
        this.vrHMD.addBox(-3.5F, -6.0F, -7.5F, 7, 4, 5, p_i63_1_);

        if (p_i63_2_)
        {
            this.bipedLeftArm = new ModelRenderer(this, 32, 48);
            this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 3, 12, 4, p_i63_1_);
            this.bipedLeftArm.setRotationPoint(5.0F, 2.5F, 0.0F);
            this.bipedRightArm = new ModelRenderer(this, 40, 16);
            this.bipedRightArm.addBox(-2.0F, -2.0F, -2.0F, 3, 12, 4, p_i63_1_);
            this.bipedRightArm.setRotationPoint(-5.0F, 2.5F, 0.0F);
            this.bipedLeftArmwear = new ModelRenderer(this, 48, 48);
            this.bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 3, 12, 4, p_i63_1_ + 0.25F);
            this.bipedLeftArmwear.setRotationPoint(5.0F, 2.5F, 0.0F);
            this.bipedRightArmwear = new ModelRenderer(this, 40, 32);
            this.bipedRightArmwear.addBox(-2.0F, -2.0F, -2.0F, 3, 12, 4, p_i63_1_ + 0.25F);
            this.bipedRightArmwear.setRotationPoint(-5.0F, 2.5F, 10.0F);
	        this.rightShoulder = new ModelRenderer(this, 24, 18);
	        this.rightShoulder.addBox(-3.0F, -2.0F, -2.0F, 3, 8, 4, p_i63_1_);
	        this.rightShoulder.setRotationPoint(-5.0F, 2.5F , 0.0F);
	        this.leftShoulder = new ModelRenderer(this, 24, 18);
	        this.leftShoulder.mirror = true;
	        this.leftShoulder.addBox(-1.0F, -2.0F, -2.0F, 3, 8, 4, p_i63_1_);
	        this.leftShoulder.setRotationPoint(5.0F, 2.5F, 0.0F);
        }
        else
        {
            this.bipedLeftArm = new ModelRenderer(this, 32, 48);
            this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, p_i63_1_);
            this.bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
            this.bipedLeftArmwear = new ModelRenderer(this, 48, 48);
            this.bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, p_i63_1_ + 0.25F);
            this.bipedLeftArmwear.setRotationPoint(5.0F, 2.0F, 0.0F);
            this.bipedRightArmwear = new ModelRenderer(this, 40, 32);
            this.bipedRightArmwear.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, p_i63_1_ + 0.25F);
            this.bipedRightArmwear.setRotationPoint(-5.0F, 2.0F, 10.0F);
	        this.rightShoulder = new ModelRenderer(this, 24, 18);
	        this.rightShoulder.addBox(-3.0F, -2.0F, -2.0F, 4, 8, 4, p_i63_1_);
	        this.rightShoulder.setRotationPoint(-5.0F, 2.0F , 0.0F);
	        this.leftShoulder = new ModelRenderer(this, 24, 18);
	        this.leftShoulder.mirror = true;
	        this.leftShoulder.addBox(-1.0F, -2.0F, -2.0F, 4, 8, 4, p_i63_1_);
	        this.leftShoulder.setRotationPoint(5.0F, 2.0F, 0.0F);
        }

        this.bipedLeftLeg = new ModelRenderer(this, 16, 48);
        this.bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, p_i63_1_);
        this.bipedLeftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);
        this.bipedLeftLegwear = new ModelRenderer(this, 0, 48);
        this.bipedLeftLegwear.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, p_i63_1_ + 0.25F);
        this.bipedLeftLegwear.setRotationPoint(1.9F, 12.0F, 0.0F);
        this.bipedRightLegwear = new ModelRenderer(this, 0, 32);
        this.bipedRightLegwear.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, p_i63_1_ + 0.25F);
        this.bipedRightLegwear.setRotationPoint(-1.9F, 12.0F, 0.0F);
        this.bipedBodyWear = new ModelRenderer(this, 16, 32);
        this.bipedBodyWear.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, p_i63_1_ + 0.25F);
        this.bipedBodyWear.setRotationPoint(0.0F, 0.0F, 0.0F);
    }

    public void render(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale)
    {
    	PlayerModelController.RotInfo rotInfo = PlayerModelController.getInstance().getRotationsForPlayer(((PlayerEntity)entityIn).getUniqueID());
        
    	if (rotInfo != null) {
            GlStateManager.scalef(rotInfo.heightScale, rotInfo.heightScale, rotInfo.heightScale);
         //   GlStateManager.translatef(0.0F, -0.75f, 0.0F);
    	}
    	
        super.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        GlStateManager.pushMatrix();

        if (this.isChild)
        {
            float f = 2.0F;
            GlStateManager.scalef(0.5F, 0.5F, 0.5F);
            GlStateManager.translatef(0.0F, 24.0F * scale, 0.0F);
            this.bipedLeftLegwear.render(scale);
            this.bipedRightLegwear.render(scale);
            this.bipedLeftArmwear.render(scale);
            this.bipedRightArmwear.render(scale);
            this.bipedBodyWear.render(scale);
        }
        else
        {
            if (entityIn.shouldRenderSneaking())
            {
                GlStateManager.translatef(0.0F, 0.2F, 0.0F);
            }

            this.bipedLeftLegwear.render(scale);
            this.bipedRightLegwear.render(scale);
            this.bipedLeftArmwear.render(scale);
            this.bipedRightArmwear.render(scale);
            this.bipedBodyWear.render(scale);
        }
        //Vivecraft
        this.leftShoulder.render(scale);
        this.rightShoulder.render(scale);     
        this.vrHMD.render(scale);
        //
        GlStateManager.popMatrix();
    }

    public void renderDeadmau5Head(float scale)
    {
        this.bipedDeadmau5Head.copyModelAngles(this.bipedHead);
        this.bipedDeadmau5Head.rotationPointX = 0.0F;
        this.bipedDeadmau5Head.rotationPointY = 0.0F;
        this.bipedDeadmau5Head.render(scale);
    }

    public void renderCape(float scale)
    {
        this.bipedCape.render(scale);
    }

    /**
     * Sets the model's various rotation angles. For bipeds, par1 and par2 are used for animating the movement of arms
     * and legs, where par1 represents the time(so that arms and legs swing back and forth) and par2 represents how
     * "far" arms and legs can swing at most.
     */
    public void setRotationAngles(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor)
    {
    	super.setRotationAngles(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
   
    	//Lasciate ogne speranza, voi ch'intrate
    	PlayerModelController.RotInfo rotInfo = PlayerModelController.getInstance().getRotationsForPlayer(((PlayerEntity)entityIn).getUniqueID());
    
    	if(rotInfo != null) {

    		double minecraftBullshit = -1.501F * rotInfo.heightScale;

    		Vec3d pos = this.renderPos;
    		float eyaw = (float) Math.toRadians(entityIn.rotationYaw);
    		float hmdYaw = (float) Math.atan2(-rotInfo.headRot.x, -rotInfo.headRot.z); 
    		float hmdPitch = (float) Math.asin(rotInfo.headRot.y/rotInfo.headRot.length()); 

    		float leftControllerYaw = (float) Math.atan2(-rotInfo.leftArmRot.x, -rotInfo.leftArmRot.z); 
    		float leftControllerPitch = (float) Math.asin(rotInfo.leftArmRot.y/rotInfo.leftArmRot.length());           	

    		this.bipedHead.rotateAngleX = (float) -hmdPitch;

    		double bodyYaw = rotInfo.getBodyYawRadians();	

    		//        		this.bipedHead.rotateAngleY = (float) (0);
    		this.bipedHead.rotateAngleY = (float) (Math.PI - hmdYaw - bodyYaw);
    		//        		this.bipedBody.rotateAngleY = (float) (ltor);
    		//        		this.bipedRightLeg.rotateAngleY = (float) (ltor);
    		//        		this.bipedLeftLeg.rotateAngleY = (float) (ltor);
    		//        		this.bipedRightArm.rotateAngleY = (float) (ltor);
    		//        		this.bipedLeftArm.rotateAngleY = (float) (ltor);

    		BipedModel a = this.armor.func_215337_a(EquipmentSlotType.HEAD);
    		a.bipedHead.rotateAngleY = (float) (this.bipedHead.rotateAngleY);  				
    		a.bipedHead.rotateAngleX = this.bipedHead.rotateAngleX;

    		//        		
    		//        		this.armor.getModelFromSlot(EntityEquipmentSlot.CHEST).bipedBody.rotateAngleY = this.bipedBody.rotateAngleY;
    		//        		this.armor.getModelFromSlot(EntityEquipmentSlot.CHEST).bipedBody.rotateAngleX = this.bipedBody.rotateAngleX;
    		//
    		//        		this.armor.getModelFromSlot(EntityEquipmentSlot.LEGS).bipedLeftLeg.rotateAngleY = this.bipedLeftLeg.rotateAngleY;
    		//        		this.armor.getModelFromSlot(EntityEquipmentSlot.LEGS).bipedRightLeg.rotateAngleY = this.bipedRightLeg.rotateAngleY;
    		//        		
    		//        		this.armor.getModelFromSlot(EntityEquipmentSlot.LEGS).bipedLeftLeg.rotateAngleX = this.bipedLeftLeg.rotateAngleX;
    		//        		this.armor.getModelFromSlot(EntityEquipmentSlot.LEGS).bipedRightLeg.rotateAngleX = this.bipedRightLeg.rotateAngleX;	       		
    		//        		

    		//                this.bipedRightLeg.setRotationPoint(-1.9F, 12.0F + 0, 0.0F);
    		//        		Vec3d rotatedrleg = new Vec3d(bipedRightLeg.rotationPointX, bipedRightLeg.rotationPointY, bipedRightLeg.rotationPointZ)
    		//        				.rotateYaw((float) ((float) ltor));
    		//        		this.bipedRightLeg.setRotationPoint((float)rotatedrleg.x, (float)rotatedrleg.y, (float)rotatedrleg.z);
    		//        		
    		//        		this.bipedLeftLeg.setRotationPoint(1.9F, 12.0F + 0, 0.0F);
    		//        		Vec3d rotatedlleg = new Vec3d(bipedLeftLeg.rotationPointX, bipedLeftLeg.rotationPointY, bipedLeftLeg.rotationPointZ)
    		//        				.rotateYaw((float) ((float)ltor));
    		//        		this.bipedLeftLeg.setRotationPoint((float)rotatedlleg.x, (float)rotatedlleg.y, (float)rotatedlleg.z);
    		//        	
    		
    		this.bipedLeftArm.setRotationPoint(5.0F, smallArms ? 2.5F : 2.0F, 0.0F);
    		Vec3d rotatedlarm = new Vec3d(bipedLeftArm.rotationPointX, bipedLeftArm.rotationPointY, bipedLeftArm.rotationPointZ);

    		this.bipedLeftArm.setRotationPoint((float)rotatedlarm.x, (float)rotatedlarm.y, (float)rotatedlarm.z);

    		this.bipedRightArm.setRotationPoint(-5.0F, smallArms ? 2.5F : 2.0F, 0.0F);
    		Vec3d rotatedrarm = new Vec3d(bipedRightArm.rotationPointX, bipedRightArm.rotationPointY, bipedRightArm.rotationPointZ);

    		this.bipedRightArm.setRotationPoint((float)rotatedrarm.x, (float)rotatedrarm.y, (float)rotatedrarm.z);

    		rightShoulder.isHidden = rotInfo.seated;
    		leftShoulder.isHidden = rotInfo.seated;

    		if(!rotInfo.seated){

    			//Vec3d head = rotInfo.Headpos.subtract(pos).addVector(0,minecraftBullshit-0.18f,0);
    			//head = head.scale(-1/scaleFactor);         
    			//this.bipedHead.setRotationPoint((float)head.x, (float)head.y, -(float)head.z);
    			//this.bipedBody.setRotationPoint((float)head.x, (float)head.y, -(float)head.z);
    			//this.bipedBody.scaleY = (float) (rotInfo.Headpos.subtract(pos).y / 1.62f);

    			//this.bipedRightLeg.scaleY = (float) (rotInfo.Headpos.subtract(pos).y / 1.62f);
    			//this.bipedLeftLeg.setRotationPoint((float)head.x, (float)head.y, -(float)head.z);
    			//this.bipedLeftLeg.scaleY = (float) (rotInfo.Headpos.subtract(pos).y / 1.62f);         		

    			float rightControllerYaw = (float) Math.atan2(-rotInfo.rightArmRot.x, -rotInfo.rightArmRot.z); 
    			float rightControllerPitch = (float) Math.asin(rotInfo.rightArmRot.y/rotInfo.rightArmRot.length()); 

    			if(pos !=null){
    				Vec3d larm = rotInfo.leftArmPos.subtract(pos).add(0,minecraftBullshit,0);
    				larm = larm.rotateYaw((float)(-Math.PI + bodyYaw)).add(rotInfo.leftArmRot.scale(-0.2)).scale(-1/scaleFactor);      		      		        		
    				this.bipedLeftArm.setRotationPoint((float)larm.x, (float)larm.y, -(float)larm.z);          
    				this.bipedLeftArm.rotateAngleX=(float) (-leftControllerPitch+ 3*Math.PI/2);
    				this.bipedLeftArm.rotateAngleY=(float) (Math.PI - leftControllerYaw - bodyYaw);

    				Vec3d lsh = new Vec3d(leftShoulder.rotationPointX - larm.x, 
    						leftShoulder.rotationPointY - larm.y,
    						leftShoulder.rotationPointZ - larm.z);

    				float yawls = (float) Math.atan2(-lsh.x, -lsh.z); 
    				float pitchls = (float) Math.asin(lsh.y/lsh.length()); 		
    				leftShoulder.rotateAngleY = (float) (-yawls);
    				leftShoulder.rotateAngleX = (float) (-pitchls+ 3*Math.PI/2);

    				Vec3d rarm = rotInfo.rightArmPos.subtract(pos).add(0,minecraftBullshit,0);
    				rarm = rarm.rotateYaw((float)(-Math.PI + bodyYaw)).add(rotInfo.rightArmRot.scale(-0.2)).scale(-1/scaleFactor);           
    				this.bipedRightArm.setRotationPoint((float)rarm.x, (float)rarm.y, -(float)rarm.z);   
    				this.bipedRightArm.rotateAngleX=(float) (-rightControllerPitch+ 3*Math.PI/2);
    				this.bipedRightArm.rotateAngleY=(float) (Math.PI-rightControllerYaw - bodyYaw);


    				Vec3d rsh = new Vec3d(rightShoulder.rotationPointX - rarm.x, 
    						rightShoulder.rotationPointY - rarm.y,
    						rightShoulder.rotationPointZ - rarm.z);

    				float yawrs = (float) Math.atan2(-rsh.x, -rsh.z); 
    				float pitchrs = (float) Math.asin(rsh.y/rsh.length()); 		
    				rightShoulder.rotateAngleY = (float) (-yawrs);
    				rightShoulder.rotateAngleX = (float) (-pitchrs+ 3*Math.PI/2);


    				//	armor.bipedLeftArm.rotateAngleY = this.leftShoulder.rotateAngleY;  				
    				//	armor.bipedLeftArm.rotateAngleX = this.leftShoulder.rotateAngleX;
    				//	armor.bipedRightArm.rotateAngleY = this.rightShoulder.rotateAngleY;  				
    				//	armor.bipedRightArm.rotateAngleX = this.rightShoulder.rotateAngleX;
    				this.bipedLeftArm.scaleY = 0.5f;
    				this.bipedLeftArmwear.scaleY = 0.5f;
    				this.bipedRightArm.scaleY = 0.5f;
    				this.bipedRightArmwear.scaleY = 0.5f;	

    				if(rotInfo.reverse){
    					this.rightShoulder.setRotationPoint(5.0F, smallArms ? 2.5F : 2.0F, 0.0F);
    					this.leftShoulder.setRotationPoint(-5.0F, smallArms ? 2.5F : 2.0F, 0.0F);
    				} else {
    					this.rightShoulder.setRotationPoint(-5.0F, smallArms ? 2.5F : 2.0F, 0.0F);
    					this.leftShoulder.setRotationPoint(5.0F, smallArms ? 2.5F : 2.0F, 0.0F);
    				}
    				
    	    		BipedModel c = this.armor.func_215337_a(EquipmentSlotType.CHEST);
    	    		c.bipedLeftArm.rotateAngleY = (float) (this.leftShoulder.rotateAngleY);  				
    	    		c.bipedLeftArm.rotateAngleX = this.leftShoulder.rotateAngleX;
    	    		c.bipedRightArm.rotateAngleY = (float) (this.rightShoulder.rotateAngleY);  				
    	    		c.bipedRightArm.rotateAngleX = this.rightShoulder.rotateAngleX;

    			} 
    		}else {
    			this.bipedLeftArm.scaleY = 1;
    			this.bipedLeftArmwear.scaleY = 1;
    			this.bipedRightArm.scaleY =1;
    			this.bipedRightArmwear.scaleY =1;
    			this.bipedLeftArm.setRotationPoint(5.0F, smallArms ? 2.5F : 2.0F, 0.0F);
    			this.bipedRightArm.setRotationPoint(-5.0F, smallArms ? 2.5F : 2.0F, 0.0F);
    		}
    	}

    	this.vrHMD.isHidden = false;
    	switch(rotInfo.hmd){
    	case 0:
    		this.vrHMD.isHidden = true;
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

    	this.bipedHeadwear.copyModelAngles(this.bipedHead);
    	this.vrHMD.copyModelAngles(this.bipedHead);      
    	this.bipedLeftLegwear.copyModelAngles(this.bipedLeftLeg);
    	this.bipedRightLegwear.copyModelAngles(this.bipedRightLeg);
    	this.bipedLeftArmwear.copyModelAngles(this.bipedLeftArm);
    	this.bipedRightArmwear.copyModelAngles(this.bipedRightArm);
    	this.bipedBodyWear.copyModelAngles(this.bipedBody);
    }

    public void setVisible(boolean visible)
    {
        super.setVisible(visible);
        this.bipedLeftArmwear.showModel = visible;
        this.bipedRightArmwear.showModel = visible;
        this.bipedLeftLegwear.showModel = visible;
        this.bipedRightLegwear.showModel = visible;
        this.bipedBodyWear.showModel = visible;
        this.bipedCape.showModel = visible;
        this.bipedDeadmau5Head.showModel = visible;
    }

    public void postRenderArm(float scale, HandSide side)
    {
        ModelRenderer ModelRenderer = this.getArmForSide(side);

        if (this.smallArms)
        {
            float f = 0.5F * (float)(side == HandSide.RIGHT ? 1 : -1);
            ModelRenderer.rotationPointX += f;
            ModelRenderer.postRender(scale);
            ModelRenderer.rotationPointX -= f;
        }
        else
        {
            ModelRenderer.postRender(scale);
        }
    }
}
