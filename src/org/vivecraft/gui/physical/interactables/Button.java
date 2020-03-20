package org.vivecraft.gui.physical.interactables;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.entity.model.RendererModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.model.Model;

import org.vivecraft.utils.Convert;
import org.vivecraft.utils.Quaternion;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import org.vivecraft.utils.Utils;

public abstract class Button implements Interactable {
	public boolean enabled=true;
	float scale=1/64f;
	ItemStack displayItem;
	ModelButton model;
	public boolean isDown;
	public boolean isTouched;
	public boolean sticky=false;
	public boolean toggle=false;

	public Button(ItemStack displayItem){
		this.displayItem=displayItem;
		this.model=new ModelButton(displayItem);
	}

	public Vec3d position=Vec3d.ZERO;
	public Quaternion rotation=new Quaternion();

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void render(double partialTicks, int renderLayer) {
		model.render();
	}

	@Override
	public Vec3d getPosition(double partialTicks) {
		Vec3d pos=position;
		if (isDown){
			pos=pos.add(rotation.multiply(new Vec3d(0,-0.015,0)));
		}else if(isTouched){
			pos=pos.add(rotation.multiply(new Vec3d(0,0.01,0)));
		}
		return pos;
	}

	@Override
	public Quaternion getRotation(double partialTicks) {
		return rotation;
	}


	@Override
	public void touch() {
		isTouched=true;
	}

	@Override
	public void untouch() {
		isTouched=false;
	}

	@Override
	public void click(int button) {
		if(toggle) {
			isDown = !isDown;
		}else{
			isDown = true;
		}
	}

	@Override
	public void unclick(int button) {
		if(!sticky && !toggle) {
			isDown = false;
		}
	}


	@Override
	public AxisAlignedBB getBoundingBox() {
		return new AxisAlignedBB(-5*scale,-3*scale,0*scale,5*scale,3*scale,4*scale).grow(0.05);
	}

	public class ModelButton extends EntityModel {
		public ResourceLocation TEXTURE = new ResourceLocation("vivecraft:textures/blocks/inv_button.png");
		
		RendererModel button;
		ItemStack displayItem;
		ItemRenderer renderItem=Minecraft.getInstance().getItemRenderer();

		public ModelButton(ItemStack displayItem){

			this.displayItem=displayItem;
			button=new RendererModel(this,0,0).setTextureSize(64,64);
			button.addBox(-5,0,-3,10,4,6);
		}


		public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
			button.render(scale);
			GlStateManager.pushMatrix();
			float itScale=1/16f;
			GlStateManager.translatef(0,scale*4,0);
			GlStateManager.scalef(itScale,itScale,itScale);
			Utils.glRotate(new Quaternion(90,0,0));
			renderItem.renderItem(displayItem, TransformType.FIXED);
			GlStateManager.popMatrix();
		}

		public void render(){
			Minecraft.getInstance().getTextureManager().bindTexture(TEXTURE);
			render(null,0f,0f,0f,0f,0f,scale);
		}
	}
}
