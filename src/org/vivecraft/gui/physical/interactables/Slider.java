package org.vivecraft.gui.physical.interactables;

import org.vivecraft.api.VRData;
import org.vivecraft.gameplay.OpenVRPlayer;
import org.vivecraft.utils.math.Quaternion;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.Model;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.model.ModelRenderer.ModelBox;
import net.minecraft.entity.Entity;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class Slider implements Interactable{
	public boolean enabled=true;
	ModelSlider slider=new ModelSlider(80);

	public Vec3d position=Vec3d.ZERO;
	public Quaternion rotation=new Quaternion();
	Vec3d anchorPos=Vec3d.ZERO;
	Quaternion anchorRotation=new Quaternion();
	Minecraft mc=Minecraft.getInstance();
	public float scale=0.005f;
	boolean holding;

	double slideOffset;

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void render(double partialTicks, int renderLayer) {
		slider.render(scale);

		if (holding){
			double slide=getTargetSlidePos(partialTicks) + slideOffset;
			slider.setSliderPos(Math.min(Math.max(0,slide),1));
		}
	}

	double getTargetSlidePos(double partialTicks){
		VRData data= OpenVRPlayer.get().vrdata_world_render;
		if (data==null)
			data=OpenVRPlayer.get().vrdata_world_pre;

		int mainhand = (mc.gameSettings.mainHand == HandSide.RIGHT) ? 0 : 1;
		Vec3d ctrPos=data.getController(mainhand).getPosition();

		Vec3d sliderPos=getAnchorPos(partialTicks).add(getAnchorRotation(partialTicks).multiply(getPosition(partialTicks)));
		Vec3d dir=getAnchorRotation(partialTicks).multiply(getRotation(partialTicks).multiply(new Vec3d(0,0,-1)));

		double projected=sliderPos.subtract(ctrPos).dotProduct(dir);
		return projected/((slider.length-15)*scale)+0.5;
	}

	@Override
	public Vec3d getPosition(double partialTicks) {
		return position;
	}

	@Override
	public Quaternion getRotation(double partialTicks) {
		return rotation;
	}

	@Override
	public Vec3d getAnchorPos(double partialTicks) {
		return anchorPos;
	}

	@Override
	public Quaternion getAnchorRotation(double partialTicks) {
		return anchorRotation;
	}

	@Override
	public void touch() {}

	@Override
	public void untouch() {
	}

	ArrayList<ScrollListener> listeners=new ArrayList<>();

	public void registerScrollListener(ScrollListener listener){
		listeners.add(listener);
	}
	public void unregisterScrollListener(ScrollListener listener){
		listeners.remove(listener);
	}
	public void notifyScroll(double perc){
		for(ScrollListener listener : listeners){
			listener.onScroll(perc);
		}
	}

	@Override
	public void update(){
		if(holding){
			notifyScroll(getSlidePercent());
		}
	}

	public interface ScrollListener{
		public void onScroll(double perc);
	}


	@Override
	public AxisAlignedBB getBoundingBox() {
		
		ModelBox cube=slider.knob.cubeList.get(0);
		AxisAlignedBB bb= new AxisAlignedBB(scale*cube.posX1,scale*cube.posY1,scale*cube.posZ1,scale*cube.posX2,scale*cube.posY2,scale*cube.posZ2);
		bb=bb.offset(0,0, scale*(slider.sliderPos*(slider.length-15)- slider.length/2f + 15));
		bb=bb.grow(0.1);
		return bb;
	}

	@Override
	public void click(int button) {
		if(!holding) {
			holding = true;
			slideOffset = slider.getSliderPos() - getTargetSlidePos(0);
		}
	}

	@Override
	public void unclick(int button) {
		holding=false;
	}

	public double getSlidePercent(){
		return 1.0-slider.getSliderPos();
	}


	class ModelSlider extends EntityModel {
		public ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");

		ModelRenderer rail;
		ModelRenderer knob;
		int length;

		private double sliderPos =1.0;

		public ModelSlider(int length){
			this.length=length;
			rail=new ModelRenderer(this,0,0).setTextureSize(256,256);
			int[][] texturemapRail=new int[6][];
			texturemapRail[0]=new int[]{141,11,167,12};//top
			texturemapRail[1]=new int[]{143,11,144,12};//bottom
			texturemapRail[2]=new int[]{166,11,167,12};//front
			texturemapRail[3]=new int[]{141,11,142,12};//back
			texturemapRail[4]=new int[]{141,11,142,12};//left
			texturemapRail[5]=new int[]{166,11,167,12};//right
		//	rail.cubeList.add(new ModelBox(rail,texturemapRail,-3,0,-length/2f,6,3, length,0,false));


			knob=new ModelRenderer(this,0,0).setTextureSize(256,256);
			int[][] texturemapKnob=new int[6][];
			texturemapKnob[0]=new int[]{232,0,244,15};//top
			texturemapKnob[1]=new int[]{233,14,234,15};//bottom
			texturemapKnob[2]=new int[]{233,14,234,15};//front
			texturemapKnob[3]=new int[]{232,0,233,1};//back
			texturemapKnob[4]=new int[]{232,0,233,1};//left
			texturemapKnob[5]=new int[]{233,14,234,15};//right
	//		knob.cubeList.add(new ModelBox(knob,texturemapKnob,-6,0.5f,0,12,4,15,0,false));
			setSliderPos(sliderPos);
		}

		public double getSliderPos() {
			return sliderPos;
		}

		public void setSliderPos(double sliderPos) {
			this.sliderPos = sliderPos;
			knob.rotationPointZ=(float) (sliderPos*(length-15)) - length/2f;
		}

//		@Override
//		public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
//			render(scale);
//		}	
		
		void render(float scale){
			Minecraft.getInstance().getTextureManager().bindTexture(TEXTURE);
//			rail.render(scale);
//			knob.render(scale);
		}

		@Override
		public void setRotationAngles(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks,
				float netHeadYaw, float headPitch) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void render(MatrixStack matrixStackIn, IVertexBuilder bufferIn, int packedLightIn, int packedOverlayIn,
				float red, float green, float blue, float alpha) {
			// TODO Auto-generated method stub
			
		}
	}
}
