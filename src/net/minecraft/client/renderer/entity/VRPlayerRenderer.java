package net.minecraft.client.renderer.entity;

import java.util.UUID;

import org.vivecraft.render.PlayerModelController;
import org.vivecraft.render.RenderPass;
import org.vivecraft.utils.math.Quaternion;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HeadLayer;
import net.minecraft.client.renderer.entity.layers.HeldItemLayer;
import net.minecraft.client.renderer.entity.layers.ParrotVariantLayer;
import net.minecraft.client.renderer.entity.layers.SpinAttackEffectLayer;
import net.minecraft.client.renderer.entity.layers.VRHMDLayer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.VRArmorModel;
import net.minecraft.client.renderer.entity.model.VRPlayerModel;
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
import net.minecraft.util.math.Vec3d;

public class VRPlayerRenderer extends LivingRenderer<AbstractClientPlayerEntity, VRPlayerModel<AbstractClientPlayerEntity>>
{
    public VRPlayerRenderer(EntityRendererManager p_i1295_1_)
    {
        this(p_i1295_1_, false);
    }

    public VRPlayerRenderer(EntityRendererManager p_i1296_1_, boolean p_i1296_2_)
    {
        super(p_i1296_1_, new VRPlayerModel<>(0.0F, p_i1296_2_), 0.5F);       
        BipedArmorLayer layer = new BipedArmorLayer(this, new VRArmorModel<>(0.5f), new VRArmorModel(1.0f));
        this.addLayer(layer);
        ((VRPlayerModel)this.entityModel).armor = layer;    
        this.addLayer(new VRHMDLayer(this));    
        this.addLayer(new HeldItemLayer<>(this));
        this.addLayer(new ArrowLayer(this));
    //    this.addLayer(new Deadmau5HeadLayer(this));
   //     this.addLayer(new CapeLayer(this));
        this.addLayer(new HeadLayer<>(this));
        this.addLayer(new ElytraLayer<>(this));
        this.addLayer(new ParrotVariantLayer(this));
        this.addLayer(new SpinAttackEffectLayer(this));
        this.addLayer(new BeeStingerLayer(this));
    }

    public void render(AbstractClientPlayerEntity entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn)
    {
    	
    	if (Minecraft.getInstance().currentPass == RenderPass.GUI && entityIn.isUser()) {
    		//smile for the camera. 		
    		matrixStackIn.getLast().getMatrix().setIdentity();
    		matrixStackIn.translate(0.0D, 0.0D, 1000.0D);
    		matrixStackIn.scale((float)20, (float)20, (float)20);
    		matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(180.0F));
    		matrixStackIn.rotate(Vector3f.YP.rotationDegrees(180 + Minecraft.getInstance().vrPlayer.vrdata_world_pre.getBodyYaw()));
    	}
    	
        this.setModelVisibilities(entityIn);
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }

    public Vec3d getRenderOffset(AbstractClientPlayerEntity entityIn, float partialTicks)
    {
        return entityIn.isCrouching() ? new Vec3d(0.0D, -0.125D, 0.0D) : super.getRenderOffset(entityIn, partialTicks);
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
            ItemStack itemstack = clientPlayer.getHeldItemMainhand();
            ItemStack itemstack1 = clientPlayer.getHeldItemOffhand();
          
            playermodel.setVisible(true);
            playermodel.bipedHeadwear.showModel = clientPlayer.isWearing(PlayerModelPart.HAT);
            playermodel.bipedBodyWear.showModel = clientPlayer.isWearing(PlayerModelPart.JACKET);
            playermodel.bipedLeftLegwear.showModel = clientPlayer.isWearing(PlayerModelPart.LEFT_PANTS_LEG);
            playermodel.bipedRightLegwear.showModel = clientPlayer.isWearing(PlayerModelPart.RIGHT_PANTS_LEG);
            playermodel.bipedLeftArmwear.showModel = clientPlayer.isWearing(PlayerModelPart.LEFT_SLEEVE);
            playermodel.bipedRightArmwear.showModel = clientPlayer.isWearing(PlayerModelPart.RIGHT_SLEEVE);
            playermodel.isSneak = clientPlayer.isCrouching();
            BipedModel.ArmPose bipedmodel$armpose = this.getArmPose(clientPlayer, itemstack, itemstack1, Hand.MAIN_HAND);
            BipedModel.ArmPose bipedmodel$armpose1 = this.getArmPose(clientPlayer, itemstack, itemstack1, Hand.OFF_HAND);

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

    private BipedModel.ArmPose getArmPose(AbstractClientPlayerEntity playerIn, ItemStack itemStackMain, ItemStack itemStackOff, Hand handIn)
    {
        BipedModel.ArmPose bipedmodel$armpose = BipedModel.ArmPose.EMPTY;
        ItemStack itemstack = handIn == Hand.MAIN_HAND ? itemStackMain : itemStackOff;

        if (!itemstack.isEmpty())
        {
            bipedmodel$armpose = BipedModel.ArmPose.ITEM;

            if (playerIn.getItemInUseCount() > 0)
            {
                UseAction useaction = itemstack.getUseAction();

                if (useaction == UseAction.BLOCK)
                {
                    bipedmodel$armpose = BipedModel.ArmPose.BLOCK;
                }
                else if (useaction == UseAction.BOW)
                {
                    bipedmodel$armpose = BipedModel.ArmPose.BOW_AND_ARROW;
                }
                else if (useaction == UseAction.SPEAR)
                {
                    bipedmodel$armpose = BipedModel.ArmPose.THROW_SPEAR;
                }
                else if (useaction == UseAction.CROSSBOW && handIn == playerIn.getActiveHand())
                {
                    bipedmodel$armpose = BipedModel.ArmPose.CROSSBOW_CHARGE;
                }
            }
            else
            {
                boolean flag3 = itemStackMain.getItem() == Items.CROSSBOW;
                boolean flag = CrossbowItem.isCharged(itemStackMain);
                boolean flag1 = itemStackOff.getItem() == Items.CROSSBOW;
                boolean flag2 = CrossbowItem.isCharged(itemStackOff);

                if (flag3 && flag)
                {
                    bipedmodel$armpose = BipedModel.ArmPose.CROSSBOW_HOLD;
                }

                if (flag1 && flag2 && itemStackMain.getItem().getUseAction(itemStackMain) == UseAction.NONE)
                {
                    bipedmodel$armpose = BipedModel.ArmPose.CROSSBOW_HOLD;
                }
            }
        }

        return bipedmodel$armpose;
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

    protected void renderName(AbstractClientPlayerEntity entityIn, String displayNameIn, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn)
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
                super.renderName(entityIn, score.getScorePoints() + " " + scoreobjective.getDisplayName().getFormattedText(), matrixStackIn, bufferIn, packedLightIn);
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
        	mp.renderPos = new Vec3d(d3, d4, d5);
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

            Vec3d vec3d = entityLiving.getLook(partialTicks);
            Vec3d vec3d1 = entityLiving.getMotion();
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
