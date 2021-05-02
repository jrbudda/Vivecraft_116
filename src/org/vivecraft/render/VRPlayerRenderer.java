package org.vivecraft.render;

import java.util.UUID;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HeadLayer;
import net.minecraft.client.renderer.entity.layers.HeldItemLayer;
import net.minecraft.client.renderer.entity.layers.ParrotVariantLayer;
import net.minecraft.client.renderer.entity.layers.SpinAttackEffectLayer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.UseAction;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class VRPlayerRenderer extends LivingRenderer<AbstractClientPlayerEntity, VRPlayerModel<AbstractClientPlayerEntity>>
{
    public VRPlayerRenderer(EntityRendererManager p_i1296_1_, boolean p_i1296_2_, boolean seated)
    {
        super(p_i1296_1_, new VRPlayerModel<>(0.0F, p_i1296_2_, seated), 0.5F);       
        this.addLayer(new BipedArmorLayer<>(this, new BipedModel(0.5F), new BipedModel(1.0F)));
        //this.addLayer(new VRHMDLayer(this));    
        this.addLayer(new HeldItemLayer<>(this));
        this.addLayer(new ArrowLayer(this));
    //    this.addLayer(new Deadmau5HeadLayer(this));
        this.addLayer(new VRCapeLayer(this));
        this.addLayer(new HeadLayer<>(this));
        this.addLayer(new ElytraLayer<>(this));
        this.addLayer(new ParrotVariantLayer(this));
        this.addLayer(new SpinAttackEffectLayer(this));
        this.addLayer(new BeeStingerLayer(this));
    }

    public void render(AbstractClientPlayerEntity entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn)
    {  	

//    	if (Minecraft.getInstance().currentPass == RenderPass.GUI && entityIn.isUser()) {
//    		//smile for the camera. 		
//    		Matrix4f mat = matrixStackIn.getLast().getMatrix();
//    		double scale = new Vector3d(mat.m00, mat.m01, mat.m02).length();
//    		matrixStackIn.getLast().getMatrix().setIdentity();
//    		matrixStackIn.translate(0.0D, 0.0D, 1000.0D);
//    		matrixStackIn.scale((float)scale, (float)scale, (float)scale);
//    		matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(180.0F));
//    		matrixStackIn.rotate(Vector3f.YP.rotationDegrees(180 + Minecraft.getInstance().vrPlayer.vrdata_world_pre.getBodyYaw()));
//    	} 
    	
    	PlayerModelController.RotInfo rotInfo = PlayerModelController.getInstance().getRotationsForPlayer(((PlayerEntity)entityIn).getUniqueID());
    	if(rotInfo == null) return;
		matrixStackIn.scale(rotInfo.heightScale, rotInfo.heightScale, rotInfo.heightScale);

        this.setModelVisibilities(entityIn);
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
        
		matrixStackIn.scale(1, 1/rotInfo.heightScale, 1); //put back for shadow

    }

    public Vector3d getRenderOffset(AbstractClientPlayerEntity entityIn, float partialTicks)
    {
    	if(entityIn.isActualySwimming())
    		return new Vector3d(0.0D, -0.125D , 0.0D);
    	return Vector3d.ZERO;
    	//i will put u back when i have a need.
//    	float sc = 1;
//    	PlayerModelController.RotInfo rotInfo = PlayerModelController.getInstance().getRotationsForPlayer(((PlayerEntity)entityIn).getUniqueID());
//    	if(rotInfo != null && !rotInfo.seated)
//    		sc = rotInfo.heightScale;
//        return (entityIn.isCrouching() && !entityIn.isActualySwimming()) ? new Vector3d(0.0D, -0.125D * sc, 0.0D) : super.getRenderOffset(entityIn, partialTicks);
    }

    private void setModelVisibilities(AbstractClientPlayerEntity clientPlayer)
    {
        VRPlayerModel<AbstractClientPlayerEntity> playermodel = this.getEntityModel();

        if (clientPlayer.isSpectator())
        {
            playermodel.setVisible(false);
            playermodel.bipedHead.showModel = true;
            playermodel.bipedHeadwear.showModel = true;
        }
        else
        {
            playermodel.setVisible(true);
            playermodel.bipedHeadwear.showModel = clientPlayer.isWearing(PlayerModelPart.HAT);
            playermodel.bipedBodyWear.showModel = clientPlayer.isWearing(PlayerModelPart.JACKET);
            playermodel.bipedLeftLegwear.showModel = clientPlayer.isWearing(PlayerModelPart.LEFT_PANTS_LEG);
            playermodel.bipedRightLegwear.showModel = clientPlayer.isWearing(PlayerModelPart.RIGHT_PANTS_LEG);
            playermodel.bipedLeftArmwear.showModel = clientPlayer.isWearing(PlayerModelPart.LEFT_SLEEVE);
            playermodel.bipedRightArmwear.showModel = clientPlayer.isWearing(PlayerModelPart.RIGHT_SLEEVE);
            playermodel.isSneak = clientPlayer.isCrouching() && !clientPlayer.isActualySwimming();
            BipedModel.ArmPose bipedmodel$armpose = func_241741_a_(clientPlayer, Hand.MAIN_HAND);
            BipedModel.ArmPose bipedmodel$armpose1 = func_241741_a_(clientPlayer, Hand.OFF_HAND);

            if (bipedmodel$armpose.func_241657_a_())
            {
                bipedmodel$armpose1 = clientPlayer.getHeldItemOffhand().isEmpty() ? BipedModel.ArmPose.EMPTY : BipedModel.ArmPose.ITEM;
            }
            if (clientPlayer.getPrimaryHand() == HandSide.RIGHT)
            {
                playermodel.rightArmPose = bipedmodel$armpose;
                playermodel.leftArmPose = bipedmodel$armpose1;
            }
            else
            {
                playermodel.rightArmPose = bipedmodel$armpose1;
                playermodel.leftArmPose = bipedmodel$armpose;
            }
        }
    }

    private static BipedModel.ArmPose func_241741_a_(AbstractClientPlayerEntity p_241741_0_, Hand p_241741_1_)
    {
        ItemStack itemstack = p_241741_0_.getHeldItem(p_241741_1_);

        if (itemstack.isEmpty())
        {
            return BipedModel.ArmPose.EMPTY;
        }
        else
        {
            if (p_241741_0_.getActiveHand() == p_241741_1_ && p_241741_0_.getItemInUseCount() > 0)
            {
                UseAction useaction = itemstack.getUseAction();

                if (useaction == UseAction.BLOCK)
                {
                    return BipedModel.ArmPose.BLOCK;
                }

                if (useaction == UseAction.BOW)
                {
                    return BipedModel.ArmPose.BOW_AND_ARROW;
                }

                if (useaction == UseAction.SPEAR)
                {
                    return BipedModel.ArmPose.THROW_SPEAR;
                }

                if (useaction == UseAction.CROSSBOW && p_241741_1_ == p_241741_0_.getActiveHand())
                {
                    return BipedModel.ArmPose.CROSSBOW_CHARGE;
                }
            }
            else if (!p_241741_0_.isSwingInProgress && itemstack.getItem() == Items.CROSSBOW && CrossbowItem.isCharged(itemstack))
            {
                return BipedModel.ArmPose.CROSSBOW_HOLD;
            }

            return BipedModel.ArmPose.ITEM;
        }
    }

    public ResourceLocation getEntityTexture(AbstractClientPlayerEntity entity)
    {
        return entity.getLocationSkin();
    }

    protected void preRenderCallback(AbstractClientPlayerEntity entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime)
    {
        float f = 0.9375F;
        matrixStackIn.scale(0.9375F, 0.9375F, 0.9375F);
    }

    protected void renderName(AbstractClientPlayerEntity entityIn, ITextComponent displayNameIn, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn)
    {
        double d0 = this.renderManager.squareDistanceTo(entityIn);
        matrixStackIn.push();

        if (d0 < 100.0D)
        {
            Scoreboard scoreboard = entityIn.getWorldScoreboard();
            ScoreObjective scoreobjective = scoreboard.getObjectiveInDisplaySlot(2);

            if (scoreobjective != null)
            {
                Score score = scoreboard.getOrCreateScore(entityIn.getScoreboardName(), scoreobjective);
                super.renderName(entityIn, (new StringTextComponent(Integer.toString(score.getScorePoints()))).appendString(" ").append(scoreobjective.getDisplayName()), matrixStackIn, bufferIn, packedLightIn);
                matrixStackIn.translate(0.0D, (double)(9.0F * 1.15F * 0.025F), 0.0D);
            }
        }

        super.renderName(entityIn, displayNameIn, matrixStackIn, bufferIn, packedLightIn);
        matrixStackIn.pop();
    }

    public void renderRightArm(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, AbstractClientPlayerEntity playerIn)
    {
        this.renderItem(matrixStackIn, bufferIn, combinedLightIn, playerIn, (this.entityModel).bipedRightArm, (this.entityModel).bipedRightArmwear);
    }

    public void renderLeftArm(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, AbstractClientPlayerEntity playerIn)
    {
        this.renderItem(matrixStackIn, bufferIn, combinedLightIn, playerIn, (this.entityModel).bipedLeftArm, (this.entityModel).bipedLeftArmwear);
    }

    private void renderItem(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, AbstractClientPlayerEntity playerIn, ModelRenderer rendererArmIn, ModelRenderer rendererArmwearIn)
    {
        VRPlayerModel<AbstractClientPlayerEntity> playermodel = this.getEntityModel();
        this.setModelVisibilities(playerIn);
        playermodel.swingProgress = 0.0F;
        playermodel.isSneak = false;
        playermodel.swimAnimation = 0.0F;
        playermodel.setRotationAngles(playerIn, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        rendererArmIn.rotateAngleX = 0.0F;
        rendererArmIn.render(matrixStackIn, bufferIn.getBuffer(RenderType.getEntitySolid(playerIn.getLocationSkin())), combinedLightIn, OverlayTexture.NO_OVERLAY);
        rendererArmwearIn.rotateAngleX = 0.0F;
        rendererArmwearIn.render(matrixStackIn, bufferIn.getBuffer(RenderType.getEntityTranslucent(playerIn.getLocationSkin())), combinedLightIn, OverlayTexture.NO_OVERLAY);
    }

    @Override
    protected void applyRotations(AbstractClientPlayerEntity entityLiving, MatrixStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks)
    {
        //VIVECRAFT
        if(this.getEntityModel() instanceof VRPlayerModel && entityLiving instanceof PlayerEntity){
        	UUID uuid = entityLiving.getUniqueID();
        	VRPlayerModel mp = (VRPlayerModel) getEntityModel();
        	double d3 = entityLiving.lastTickPosX + (entityLiving.getPosX() - entityLiving.lastTickPosX) * (double)partialTicks;
        	double d4 = entityLiving.lastTickPosY + (entityLiving.getPosY() - entityLiving.lastTickPosY) * (double)partialTicks;
        	double d5 = entityLiving.lastTickPosZ + (entityLiving.getPosZ() - entityLiving.lastTickPosZ) * (double)partialTicks;
        //	mp.renderPos = new Vector3d(d3, d4, d5);
        	if(PlayerModelController.getInstance().isTracked(uuid)){
        		PlayerModelController.RotInfo rotInfo=PlayerModelController.getInstance().getRotationsForPlayer(uuid);	
        		rotationYaw = (float) Math.toDegrees(rotInfo.getBodyYawRadians());
        	}
        }
        float wasyaw = entityLiving.rotationYaw;
        //
    	
        float f = entityLiving.getSwimAnimation(partialTicks);

        if (entityLiving.isElytraFlying())
        {
            super.applyRotations(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
            float f1 = (float)entityLiving.getTicksElytraFlying() + partialTicks;
            float f2 = MathHelper.clamp(f1 * f1 / 100.0F, 0.0F, 1.0F);

            if (!entityLiving.isSpinAttacking())
            {
                matrixStackIn.rotate(Vector3f.XP.rotationDegrees(f2 * (-90.0F - entityLiving.rotationPitch)));
            }

            Vector3d vec3d = entityLiving.getLook(partialTicks);
            Vector3d vec3d1 = entityLiving.getMotion();
            double d0 = Entity.horizontalMag(vec3d1);
            double d1 = Entity.horizontalMag(vec3d);

            if (d0 > 0.0D && d1 > 0.0D)
            {
                double d2 = (vec3d1.x * vec3d.x + vec3d1.z * vec3d.z) / (Math.sqrt(d0) * Math.sqrt(d1));
                double d3 = vec3d1.x * vec3d.z - vec3d1.z * vec3d.x;
                matrixStackIn.rotate(Vector3f.YP.rotation((float)(Math.signum(d3) * Math.acos(d2))));
            }
        }
        else if (f > 0.0F)
        {
            super.applyRotations(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
            float f3 = entityLiving.isInWater() ? -90.0F - entityLiving.rotationPitch : -90.0F;
            float f4 = MathHelper.lerp(f, 0.0F, f3);
            matrixStackIn.rotate(Vector3f.XP.rotationDegrees(f4));

            if (entityLiving.isActualySwimming())
            {
                matrixStackIn.translate(0.0D, -1.0D, (double)0.3F);
            }
        }
        else
        {
            super.applyRotations(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
        }
    }
        
}
