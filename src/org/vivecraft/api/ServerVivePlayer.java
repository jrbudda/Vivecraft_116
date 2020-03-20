package org.vivecraft.api;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.vivecraft.utils.Quaternion;
import org.vivecraft.utils.Vector3;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.Vec3d;

public class ServerVivePlayer {

	public byte[] hmdData;
	public byte[] controller0data;
	public byte[] controller1data;
	public byte[] draw;
	public float worldScale = 1.0f;
	public float height =defaultHeight;
	public static final float defaultHeight =1.65f;

	boolean isTeleportMode;
	boolean isReverseHands;
	boolean isVR = true;

	public ServerPlayerEntity player;

	public ServerVivePlayer(ServerPlayerEntity player) {
		this.player = player;	
	}

	public float getDraw(){
		try {
			if(draw != null){
				ByteArrayInputStream byin = new ByteArrayInputStream(draw);
				DataInputStream da = new DataInputStream(byin);
		
				float draw= da.readFloat();
				
				da.close(); //needed?
				return draw;	
			}else{
			}
		} catch (IOException e) {

		}
	 
		return 0;
	}

	
	public Vec3d getControllerDir(int controller){
		byte[] data = controller0data;
		if(controller == 1) data = controller1data;
		if(this.isSeated()) controller = 0;
		if(data != null){

			ByteArrayInputStream byin = new ByteArrayInputStream(data);
			DataInputStream da = new DataInputStream(byin);

			try {

				boolean reverse = da.readBoolean();

				float lx = da.readFloat();
				float ly = da.readFloat();
				float lz = da.readFloat();

				float w = da.readFloat();
				float x = da.readFloat();
				float y = da.readFloat();
				float z = da.readFloat();
				Vector3 forward = new Vector3(0,0,-1);
				Quaternion q = new Quaternion(w, x, y, z);
				Vector3 out = q.multiply(forward);

				da.close(); //needed?
				return new Vec3d(out.getX(), out.getY(), out.getZ());
			} catch (IOException e) {
				return player.getLookVec();
			}
		}else{
		}

		return player.getLookVec();
	}
	
	public Vec3d getHMDDir(){
		try {
			if(hmdData != null){

				ByteArrayInputStream byin = new ByteArrayInputStream(hmdData);
				DataInputStream da = new DataInputStream(byin);

				boolean isSeated = da.readBoolean();
				float lx = da.readFloat();
				float ly = da.readFloat();
				float lz = da.readFloat();

				float w = da.readFloat();
				float x = da.readFloat();
				float y = da.readFloat();
				float z = da.readFloat();
				Vector3 forward = new Vector3(0,0,-1);
				Quaternion q = new Quaternion(w, x, y, z);
				Vector3 out = q.multiply(forward);


				//System.out.println("("+out.getX()+","+out.getY()+","+out.getZ()+")" + " : W:" + w + " X: "+x + " Y:" + y+ " Z:" + z);
				da.close(); //needed?
				return new Vec3d(out.getX(), out.getY(), out.getZ());
			}else{
			}
		} catch (IOException e) {

		}

		return player.getLookVec();
	}
	
	public Vec3d getHMDPos() {
		try {
			if(hmdData!=null){
				
				ByteArrayInputStream byin = new ByteArrayInputStream(hmdData);
				DataInputStream da = new DataInputStream(byin);
		
				boolean isSeated = da.readBoolean();
				float x = da.readFloat();
				float y = da.readFloat();
				float z = da.readFloat();
				
				da.close(); 
								
				return new Vec3d(x, y, z);
			}else{
			}
		} catch (IOException e) {

		}
	 
		return player.getPositionVector().add(0, 1.62, 0); //why

	}
	
	
	public Vec3d getControllerPos(int c) {
		try {
			if(controller0data != null && controller0data != null){
				
				ByteArrayInputStream byin = new ByteArrayInputStream(c==0?controller0data:controller1data);
				DataInputStream da = new DataInputStream(byin);
		
				boolean rev = da.readBoolean();
				float x = da.readFloat();
				float y = da.readFloat();
				float z = da.readFloat();
				
				da.close(); //needed?
				
				if (this.isSeated()){
					Vec3d dir = this.getHMDDir();
					dir = dir.rotateYaw((float) Math.toRadians(c==0?-35:35));
					dir = new Vec3d(dir.x, 0, dir.z);
					dir = dir.normalize();
					Vec3d out = this.getHMDPos().add(dir.x * 0.3 * worldScale, -0.4* worldScale ,dir.z*0.3* worldScale);
					x = (float) out.x;
					y = (float) out.y;
					z = (float) out.z;
				}
				
				return new Vec3d(x, y, z);
			}else{
			}
		} catch (IOException e) {

		}
	 
		return player.getPositionVector().add(0, 1.62, 0); //why

	}

	public boolean isVR(){
		return this.isVR;
	}
	
	public void setVR(boolean vr){
		this.isVR = vr;
	}
	
	public boolean isSeated(){
		try {
			if(hmdData == null) return false;
			if(hmdData.length <29) return false;//old client.
			
			ByteArrayInputStream byin = new ByteArrayInputStream(hmdData);
			DataInputStream da = new DataInputStream(byin);
	
			boolean seated= da.readBoolean();
			
			da.close(); //needed?
			return seated;
				
		} catch (IOException e) {

		}
	 
		return false;
	}

	public byte[] getUberPacket() {
		PacketBuffer pb = new PacketBuffer(Unpooled.buffer());
		pb.writeLong(player.getUniqueID().getMostSignificantBits());
		pb.writeLong(player.getUniqueID().getLeastSignificantBits());
		pb.writeBytes(hmdData);
		pb.writeBytes(controller0data);
		pb.writeBytes(controller1data);
		pb.writeFloat(worldScale);
		pb.writeFloat(height);

		return pb.array();
	}

}