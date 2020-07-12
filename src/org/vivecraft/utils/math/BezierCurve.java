package org.vivecraft.utils.math;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;

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


	Vector3d getIntermediate(Node n1, Node n2, double perc){
		Vector3d p0=n1.vertex;
		Vector3d p1=n1.controlOut;
		Vector3d p2=n2.controlIn;
		Vector3d p3=n2.vertex;

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
	public Vector3d getPointOnPath(double perc){
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

	public Vector3d[] getLinearInterpolation(int verticesPerNode){
		if (nodes.size()==0)
			return new Vector3d[0];

		int totalVertices=verticesPerNode * (circular? nodes.size():nodes.size()-1) +1;

		Vector3d[] out=new Vector3d[totalVertices];
		for (int i = 0; i < totalVertices; i++) {
			double perc=(((double) i)/Math.max(1,totalVertices-1));
			out[i]=getPointOnPath(perc);
		}

		return out;
	}


	public void render(int vertexCount, Color c, float partialTicks){
		PlayerEntity entityplayer = Minecraft.getInstance().player;
		double d0 = entityplayer.lastTickPosX + (entityplayer.getPosX() - entityplayer.lastTickPosX) * (double)partialTicks;
		double d1 = entityplayer.lastTickPosY + (entityplayer.getPosY() - entityplayer.lastTickPosY) * (double)partialTicks;
		double d2 = entityplayer.lastTickPosZ + (entityplayer.getPosZ() - entityplayer.lastTickPosZ) * (double)partialTicks;

		GlStateManager.disableTexture();
		GlStateManager.disableLighting();
		GlStateManager.depthMask(false);
		//GlStateManager.disableCull();

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();


		bufferbuilder.begin(3, DefaultVertexFormats.POSITION_COLOR);

		Vector3d[] vertices=getLinearInterpolation(vertexCount/nodes.size());

		for (int i = 0; i < vertices.length; i++) {
			renderVertex(bufferbuilder,vertices[i],c,d0,d1,d2);
		}
		tessellator.draw();

		GlStateManager.enableLighting();
		GlStateManager.enableTexture();
		GlStateManager.depthMask(true);
		//GlStateManager.enableCull();
	}

	void renderVertex(BufferBuilder buffer, Vector3d vert, Color color, double offX, double offY, double offZ){
		buffer.pos(vert.x-offX,vert.y-offY,vert.z-offZ).color(color.getRed(),color.getGreen(),color.getBlue(),color.getAlpha()).endVertex();
	}

	public static class Node{
		public Node(Vector3d vertex, Vector3d controlIn, Vector3d controlOut){
			this.vertex=vertex;
			this.controlIn=controlIn;
			this.controlOut=controlOut;
		}

		public Node(Vector3d vertex, Vector3d controlDir, double controlLenIn, double controlLenOut ){
			this(vertex,
					vertex.add(controlDir.normalize().scale(-controlLenIn)),
					vertex.add(controlDir.normalize().scale(controlLenOut)));
		}

		Vector3d vertex;
		Vector3d controlIn;
		Vector3d controlOut;
	}
}
