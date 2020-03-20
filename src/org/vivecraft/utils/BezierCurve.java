package org.vivecraft.utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class BezierCurve {
	public BezierCurve(Node[] nodes, boolean circular){
		this.nodes.addAll(Arrays.asList(nodes));
		this.circular=circular;
	}

	public BezierCurve(boolean circular){
		this.circular=circular;
	}

	public ArrayList<Node> nodes=new ArrayList<>();

	/**  Whether we should draw another curve from the last Node to the first   */
	boolean circular;


	Vec3d getIntermediate(Node n1, Node n2, double perc){
		Vec3d p0=n1.vertex;
		Vec3d p1=n1.controlOut;
		Vec3d p2=n2.controlIn;
		Vec3d p3=n2.vertex;

		return p0.scale(Math.pow(1-perc,3))
				.add( p1.scale(3*Math.pow(1-perc,2)*perc) )
				.add( p2.scale(3*(1-perc) * Math.pow(perc,2)) )
				.add( p3.scale(Math.pow(perc,3)));
	}

	/**
	 * returns the intermediate Point on the path at {@code perc}%
	 * of the path. This assumes equidistant Nodes, meaning close distances have
	 * the same amount of intermediate Vertices as longer paths between Nodes.
	 *
	 * @param perc position on Path in interval [0,1] (inclusive interval)
	 * */
	public Vec3d getPointOnPath(double perc){
		//first node is counted as another virtual node if we are circular
		int nodeCount=circular? nodes.size() : nodes.size()-1;

		double exactIndex=perc * nodeCount;
		int lowerIndex=((int) Math.floor(exactIndex)) % nodes.size();
		int upperIndex=((int) Math.ceil(exactIndex)) % nodes.size();

		if (lowerIndex==upperIndex){
			return nodes.get(lowerIndex).vertex;
		}

		Node node1=nodes.get(lowerIndex);
		Node node2=nodes.get(upperIndex);

		return getIntermediate(node1,node2,exactIndex-lowerIndex);
	}

	public Vec3d[] getLinearInterpolation(int verticesPerNode){
		if (nodes.size()==0)
			return new Vec3d[0];

		int totalVertices=verticesPerNode * (circular? nodes.size():nodes.size()-1) +1;

		Vec3d[] out=new Vec3d[totalVertices];
		for (int i = 0; i < totalVertices; i++) {
			double perc=(((double) i)/Math.max(1,totalVertices-1));
			out[i]=getPointOnPath(perc);
		}

		return out;
	}


	public void render(int vertexCount, Color c, float partialTicks){
		PlayerEntity entityplayer = Minecraft.getInstance().player;
		double d0 = entityplayer.lastTickPosX + (entityplayer.posX - entityplayer.lastTickPosX) * (double)partialTicks;
		double d1 = entityplayer.lastTickPosY + (entityplayer.posY - entityplayer.lastTickPosY) * (double)partialTicks;
		double d2 = entityplayer.lastTickPosZ + (entityplayer.posZ - entityplayer.lastTickPosZ) * (double)partialTicks;

		GlStateManager.disableTexture();
		GlStateManager.disableLighting();
		GlStateManager.depthMask(false);
		//GlStateManager.disableCull();

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();


		bufferbuilder.begin(3, DefaultVertexFormats.POSITION_COLOR);

		Vec3d[] vertices=getLinearInterpolation(vertexCount/nodes.size());

		for (int i = 0; i < vertices.length; i++) {
			renderVertex(bufferbuilder,vertices[i],c,d0,d1,d2);
		}
		tessellator.draw();

		GlStateManager.enableLighting();
		GlStateManager.enableTexture();
		GlStateManager.depthMask(true);
		//GlStateManager.enableCull();
	}

	void renderVertex(BufferBuilder buffer, Vec3d vert, Color color, double offX, double offY, double offZ){
		buffer.pos(vert.x-offX,vert.y-offY,vert.z-offZ).color(color.getRed(),color.getGreen(),color.getBlue(),color.getAlpha()).endVertex();
	}

	public static class Node{
		public Node(Vec3d vertex, Vec3d controlIn, Vec3d controlOut){
			this.vertex=vertex;
			this.controlIn=controlIn;
			this.controlOut=controlOut;
		}

		public Node(Vec3d vertex, Vec3d controlDir, double controlLenIn, double controlLenOut ){
			this(vertex,
					vertex.add(controlDir.normalize().scale(-controlLenIn)),
					vertex.add(controlDir.normalize().scale(controlLenOut)));
		}

		Vec3d vertex;
		Vec3d controlIn;
		Vec3d controlOut;
	}
}
