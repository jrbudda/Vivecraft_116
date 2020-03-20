package org.vivecraft.api;

import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.render.PlayerModelController;
import org.vivecraft.render.RenderPass;
import org.vivecraft.utils.Matrix4f;
import org.vivecraft.utils.Utils;
import org.vivecraft.utils.Vector3;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Vec3d;

public class VRData{
	public class VRDevicePose{
		final VRData data;
		final Vec3d pos;
		final Vec3d dir;
		final Matrix4f matrix;
		
		public VRDevicePose(VRData data, Matrix4f matrix, Vec3d pos, Vec3d dir) {
			this.data = data;
			this.matrix = matrix.transposed().transposed(); //poor mans copy.
			this.pos = new Vec3d(pos.x, pos.y, pos.z);
			this.dir = new Vec3d(dir.x, dir.y, dir.z);
		}	
		
		
		public Vec3d getPosition(){
			Vec3d out = pos.scale(worldScale);
			out = out.rotateYaw(data.rotation_radians);
			return out.add(data.origin.x, data.origin.y, data.origin.z);
		}
	
		public Vec3d getDirection() {
			Vec3d out = new Vec3d(dir.x, dir.y, dir.z).rotateYaw(data.rotation_radians);
			return out;
		}
		
		public Vec3d getCustomVector(Vec3d axis) {
			Vector3 v3 = matrix.transform(new Vector3((float)axis.x, (float)axis.y,(float) axis.z));
			Vec3d out =  v3.toVec3d().rotateYaw(data.rotation_radians);
			return out;
		}
		
		public float getYaw() {
			Vec3d dir = getDirection();
			return (float)Math.toDegrees(Math.atan2(-dir.x, dir.z)); 
		}

		public float getPitch() {
			Vec3d dir = getDirection();
			return (float)Math.toDegrees(Math.asin(dir.y/dir.length())); 
		}
		
		public float getRoll() {
			return (float)-Math.toDegrees(Math.atan2(matrix.M[1][0], matrix.M[1][1]));
		}
		
		public Matrix4f getMatrix() {
			Matrix4f rot = Matrix4f.rotationY(rotation_radians);
			return Matrix4f.multiply(rot,matrix);
		}
		
		@Override
		public String toString() {
			return "Device: pos:" + this.getPosition() + " dir: " + this.getDirection(); 
		}
		
	}
	
	public VRDevicePose hmd;
	public VRDevicePose eye0;
	public VRDevicePose eye1;
	public VRDevicePose c0;
	public VRDevicePose c1;
	public VRDevicePose c2;
	public VRDevicePose h0;
	public VRDevicePose h1;
		
	public Vec3d origin;
	public float rotation_radians;
	public float worldScale;
	
	public VRData(Vec3d origin, float walkMul,float worldScale, float rotation) {
		//ok this is where it gets ugly, gonna go straight to mcopenvr and grab shit for copying.
		
		this.origin = origin;
		this.worldScale =worldScale;
		this.rotation_radians = rotation;
		
		Vec3d hmd_raw = MCOpenVR.getCenterEyePosition();
		Vec3d scaledPos = new Vec3d(hmd_raw.x * walkMul, hmd_raw.y, hmd_raw.z * walkMul);
		
		hmd = new VRDevicePose(this, MCOpenVR.hmdRotation, scaledPos, MCOpenVR.getHmdVector()); 
		
		eye0 = new VRDevicePose(this, MCOpenVR.getEyeRotation(RenderPass.LEFT), MCOpenVR.getEyePosition(RenderPass.LEFT).subtract(hmd_raw).add(scaledPos), MCOpenVR.getHmdVector());
		eye1 = new VRDevicePose(this, MCOpenVR.getEyeRotation(RenderPass.RIGHT), MCOpenVR.getEyePosition(RenderPass.RIGHT).subtract(hmd_raw).add(scaledPos), MCOpenVR.getHmdVector());
		
		c0 = new VRDevicePose(this, MCOpenVR.getAimRotation(0),MCOpenVR.getAimSource(0).subtract(hmd_raw).add(scaledPos), MCOpenVR.getAimVector(0));
		c1 = new VRDevicePose(this, MCOpenVR.getAimRotation(1),MCOpenVR.getAimSource(1).subtract(hmd_raw).add(scaledPos), MCOpenVR.getAimVector(1));
		h0 = new VRDevicePose(this, MCOpenVR.getHandRotation(0),MCOpenVR.getAimSource(0).subtract(hmd_raw).add(scaledPos), MCOpenVR.getHandVector(0));
		h1 = new VRDevicePose(this, MCOpenVR.getHandRotation(1),MCOpenVR.getAimSource(1).subtract(hmd_raw).add(scaledPos), MCOpenVR.getHandVector(1));
		c2 = new VRDevicePose(this, MCOpenVR.getAimRotation(2),MCOpenVR.getAimSource(2).subtract(hmd_raw).add(scaledPos), MCOpenVR.getAimVector(2));
	
	}
	
	public VRDevicePose getController(int c){
		return (c == 1 ? c1: (c == 2 ? c2 : c0));
	}
	
	public VRDevicePose getHand(int c){
		return (c == 0 ? h0: h1);
	}
	
	public float getBodyYaw(){
		if(Minecraft.getInstance().vrSettings.seated)
			return hmd.getYaw();
		
		Vec3d v = (c1.getPosition().subtract(c0.getPosition())).normalize().rotateYaw((float) (-Math.PI/2));
		
		if(Minecraft.getInstance().vrSettings.vrReverseHands)
			v = v.scale(-1);

		v = Utils.vecLerp(hmd.getDirection(), v, 0.5);
		
		return (float) Math.toDegrees(Math.atan2(-v.x, v.z)); 		
	}
	
	public float getFacingYaw(){
		if(Minecraft.getInstance().vrSettings.seated)
			return hmd.getYaw();
		
		Vec3d v = (c1.getPosition().subtract(c0.getPosition())).normalize().rotateYaw((float) (-Math.PI/2));
	
		if(Minecraft.getInstance().vrSettings.vrReverseHands)
			return(float) Math.toDegrees(Math.atan2(v.x, -v.z)); 
		else
			return(float) Math.toDegrees(Math.atan2(-v.x, v.z)); 
		}
	
	public Vec3d getHeadPivot() {
		Vec3d eye = hmd.getPosition();
		Vector3 v3 = hmd.getMatrix().transform(new Vector3(0,-.1f, .1f));
		return (new Vec3d(v3.getX()+eye.x, v3.getY()+eye.y, v3.getZ()+eye.z));
	}
	
	public Vec3d getHeadRear() {
		Vec3d eye = hmd.getPosition();
		Vector3 v3 = hmd.getMatrix().transform(new Vector3(0,-.2f, .2f));
		return (new Vec3d(v3.getX()+eye.x, v3.getY()+eye.y, v3.getZ()+eye.z));
	}
	
	public VRDevicePose getEye(RenderPass pass){
		switch(pass){
		case CENTER:
			return hmd;
		case LEFT:
			return eye0;
		case RIGHT:
			return eye1;
		case THIRD:
			return c2;
		}
		return hmd;

	}
	
	@Override
	public String toString() {
		return "data:" + 
				"\r\n \t\t origin: " + this.origin +
				"\r\n \t\t rotation: " + String.format("%.2f", this.rotation_radians) +
				"\r\n \t\t scale: " + String.format("%.2f", this.worldScale) + 
				"\r\n \t\t hmd " + this.hmd + 
				"\r\n \t\t c0 " + this.c0 + 
				"\r\n \t\t c1 " + this.c1 + 
				"\r\n \t\t c2 " + this.c2 ;	
	}
	
	protected Vec3d vecMult(Vec3d in, float factor){
		return new Vec3d(in.x * factor,	in.y * factor, in.z*factor);
	}
	
}