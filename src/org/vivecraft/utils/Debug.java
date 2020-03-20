package org.vivecraft.utils;

import java.awt.Color;
import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class Debug {
	Vec3d root;
	Quaternion rotation;
	public static boolean isEnabled=true;

	static Polygon cross=new Polygon(6);
	static Polygon arrowHead=new Polygon(8);
	static {
		cross.colors[0]=new Color(0,0,0,0);
		cross.vertices[0]=new Vec3d(0,-0.1,0);
		cross.vertices[1]=new Vec3d(0,0.1,0);
		cross.colors[2]=new Color(0,0,0,0);

		cross.vertices[2]=new Vec3d(0,0,-0.1);
		cross.vertices[3]=new Vec3d(0,0,0.1);
		cross.colors[4]=new Color(0,0,0,0);

		cross.vertices[4]=new Vec3d(-0.1,0,0);
		cross.vertices[5]=new Vec3d(0.1,0,0);

		arrowHead.colors[0]=new Color(0,0,0,0);
		arrowHead.vertices[0]=new Vec3d(0,0,0);
		arrowHead.vertices[1]=new Vec3d(-0.05,-0.05,0);

		arrowHead.colors[2]=new Color(0,0,0,0);
		arrowHead.vertices[2]=new Vec3d(0,0,0);
		arrowHead.vertices[3]=new Vec3d(0.05,-0.05,0);

		arrowHead.colors[4]=new Color(0,0,0,0);
		arrowHead.vertices[4]=new Vec3d(0,0,0);
		arrowHead.vertices[5]=new Vec3d(0,-0.05,-0.05);

		arrowHead.colors[6]=new Color(0,0,0,0);
		arrowHead.vertices[6]=new Vec3d(0,0,0);
		arrowHead.vertices[7]=new Vec3d(0,-0.05,0.05);
	}

	public Debug(Vec3d root){
		this.root=root;
		this.rotation=new Quaternion();
	}
	public Debug(Vec3d root, Quaternion rotation){
		this.root=root;
		this.rotation=rotation;
	}

	public void drawPoint(Vec3d point, Color color){
		point=rotation.multiply(point);
		Vec3d global=root.add(point);
		Polygon poly=cross.offset(global);
		for (int i = 0; i < poly.colors.length; i++) {
			if(poly.colors[i]==null)
				poly.colors[i]=color;
		}
		renderer.toDraw.add(poly);
	}

	public void drawVector(Vec3d start, Vec3d direction, Color color){
		Polygon poly=new Polygon(2);
		
		start=rotation.multiply(start);
		direction=rotation.multiply(direction);
		
		poly.vertices[0]=root.add(start);
		poly.colors[0]=new Color(0,0,0,0);

		poly.vertices[1]=root.add(start).add(direction);
		poly.colors[1]=color;

		Quaternion rot=Quaternion.createFromToVector(new Vector3(0,1,0),new Vector3(direction.normalize()));
		Polygon arrow=arrowHead.rotated(rot).offset(root.add(start).add(direction));

		for (int i = 0; i < arrow.colors.length; i++) {
			if(arrow.colors[i]==null)
				arrow.colors[i]=color;
		}

		renderer.toDraw.add(poly);
		renderer.toDraw.add(arrow);
	}

	public void drawLine(Vec3d start, Vec3d end, Color color){
		start=rotation.multiply(start);
		end=rotation.multiply(end);
		
		Polygon poly=new Polygon(2);

		poly.vertices[0]=root.add(start);
		poly.colors[0]=new Color(0,0,0,0);

		poly.vertices[1]=root.add(end);
		poly.colors[1]=color;

		renderer.toDraw.add(poly);
	}
	
	public void drawBoundingBox(AxisAlignedBB box, Color color){
		Polygon poly=new Polygon(16);
		Vec3d[] lower=new Vec3d[4];
		Vec3d[] upper=new Vec3d[4];
		int index=0;
		
		lower[0]=new Vec3d(box.minX,box.minY,box.minZ);
		lower[1]=new Vec3d(box.minX,box.minY,box.maxZ);
		lower[2]=new Vec3d(box.maxX,box.minY,box.maxZ);
		lower[3]=new Vec3d(box.maxX,box.minY,box.minZ);
		
		upper[0]=new Vec3d(box.minX,box.maxY,box.minZ);
		upper[1]=new Vec3d(box.minX,box.maxY,box.maxZ);
		upper[2]=new Vec3d(box.maxX,box.maxY,box.maxZ);
		upper[3]=new Vec3d(box.maxX,box.maxY,box.minZ);
		
		
		for (int i = 0; i < 4; i++) {
			lower[i]=root.add(rotation.multiply(lower[i]));
			upper[i]=root.add(rotation.multiply(upper[i]));
		}
		
		for (int i = 0; i < 5; i++) {
			if(i==0)
				poly.colors[index]=new Color(0,0,0,0);
			else
				poly.colors[index]=color;
			poly.vertices[index]=lower[i%4];
			index++;
		}
		
		for (int i = 0; i < 5; i++) {
			poly.colors[index]=color;
			poly.vertices[index]=upper[i%4];
			index++;
		}
		
		for (int i = 1; i < 4; i++) {
			poly.vertices[index]=lower[i];
			poly.colors[index]=new Color(0,0,0,0);
			index++;
			poly.vertices[index]=upper[i];
			poly.colors[index]=color;
			index++;
		}
		renderer.toDraw.add(poly);
	}

	static class Polygon{
		public Polygon(int size){
			vertices=new Vec3d[size];
			colors=new Color[size];
		}
		Vec3d [] vertices;
		Color [] colors;

		public Polygon offset(Vec3d offset){
			Polygon pol=new Polygon(vertices.length);
			for (int i = 0; i < vertices.length; i++) {
				pol.vertices[i]=vertices[i].add(offset);
				pol.colors[i]=colors[i];
			}
			return pol;
		}

		public Polygon rotated(Quaternion quat){
			Polygon pol=new Polygon(vertices.length);
			for (int i = 0; i < vertices.length; i++) {
				pol.vertices[i]=quat.multiply(new Vector3(vertices[i])).toVec3d();
				pol.colors[i]=colors[i];
			}
			return pol;
		}
	}


	private static DebugRendererManual renderer=new DebugRendererManual();
	public static DebugRendererManual getRenderer() {
		return renderer;
	}

	public static class DebugRendererManual implements DebugRenderer.IDebugRenderer{
		public boolean manualClearing=false;

		ArrayList<Polygon> toDraw=new ArrayList<>();
	
		public void render(float partialTicks, long finishTimeNano) {

			PlayerEntity entityplayer = Minecraft.getInstance().player;
			double d0 = entityplayer.lastTickPosX + (entityplayer.posX - entityplayer.lastTickPosX) * (double)partialTicks;
			double d1 = entityplayer.lastTickPosY + (entityplayer.posY - entityplayer.lastTickPosY) * (double)partialTicks;
			double d2 = entityplayer.lastTickPosZ + (entityplayer.posZ - entityplayer.lastTickPosZ) * (double)partialTicks;

			//GlStateManager.enableBlend();
			//GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			GlStateManager.lineWidth(5.0F);
			GlStateManager.disableTexture();
			GlStateManager.disableLighting();
			GlStateManager.depthMask(false);
			GlStateManager.disableDepthTest();

			Tessellator tessellator=Tessellator.getInstance();
			BufferBuilder buffer=tessellator.getBuffer();
			buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);


			for (Polygon polygon: toDraw) {
				for(int i=0; i<polygon.vertices.length; i++){
					renderVertex(buffer,polygon.vertices[i],polygon.colors[i],d0,d1,d2);
				}
			}

			tessellator.draw();

			GlStateManager.depthMask(true);
			GlStateManager.enableTexture();
			GlStateManager.enableLighting();
			//GlStateManager.disableBlend();

			GlStateManager.enableDepthTest();

			if(!manualClearing)
				toDraw.clear();
		}

		public void clear(){
			toDraw.clear();
		}

		void renderVertex(BufferBuilder buffer, Vec3d vert, Color color, double offX, double offY, double offZ){
			buffer.pos(vert.x-offX,vert.y-offY,vert.z-offZ).color(color.getRed(),color.getGreen(),color.getBlue(),color.getAlpha()).endVertex();
		}

		@Override
		public void render(long p_217676_1_) {
			// TODO Auto-generated method stub
			
		}
	}
}
