package org.vivecraft.gameplay.trackers;

import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.settings.VRSettings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.vector.Vector3d;

public class RunTracker extends Tracker {


	public RunTracker(Minecraft mc) {
		super(mc);
	}

	public boolean isActive(ClientPlayerEntity p){
		if(!Minecraft.getInstance().vrPlayer.getFreeMove() || Minecraft.getInstance().vrSettings.seated)
			return false;
		if(Minecraft.getInstance().vrSettings.vrFreeMoveMode != VRSettings.FREEMOVE_RUNINPLACE)
			return false;
		if(p==null || !p.isAlive())
			return false;
		if(mc.playerController == null) return false;
		if(!p.isOnGround() && (p.isInWater() || p.isInLava())) 
			return false;
		if(p.isOnLadder()) 
			return false;
		if(Minecraft.getInstance().bowTracker.isNotched())
			return false;
		return true;
	}

	private double direction = 0;
	private double speed = 0;
	private Vector3d movedir;
	
	public double getYaw(){
		return direction;
	}
	
	public double getSpeed(){
		return speed;
	}

	@Override
	public void reset(ClientPlayerEntity player) {
		speed = 0;
	}

	public void doProcess(ClientPlayerEntity player){

		Vector3d controllerR= mc.vrPlayer.vrdata_world_pre.getController(0).getPosition();
		Vector3d controllerL= mc.vrPlayer.vrdata_world_pre.getController(1).getPosition();
		
		//Vector3d middle= controllerL.subtract(controllerR).scale(0.5).add(controllerR);

		double c0move = MCOpenVR.controllerHistory[0].averageSpeed(.33);
		double c1move = MCOpenVR.controllerHistory[1].averageSpeed(.33);

		if(speed > 0) {
			if(c0move < 0.1 && c1move < 0.1){
				speed = 0;
				return;
			}
		}else {
			if(c0move < 0.6 && c1move < 0.6){
				speed = 0;
				return;
			}
		}
		
		if(Math.abs(c0move - c1move) > .5){
			speed = 0;
			return;
		} //2 hands plz.
			
		
//		Vector3d hmdPos = minecraft.vrPlayer.getHMDPos_World();
//
//		Vector3d Hmddir = minecraft.vrPlayer.getHMDDir_World();
//		Hmddir= new Vector3d(Hmddir.x,0, Hmddir.z).normalize();	
//		
//		if(speed < 0) Hmddir = movedir; //maybe?
//		
//		Vector3d r = MCOpenVR.controllerHistory[0].netMovement(0.33).rotateYaw(minecraft.vrPlayer.worldRotationRadians);
//		Vector3d l = MCOpenVR.controllerHistory[1].netMovement(0.33).rotateYaw(minecraft.vrPlayer.worldRotationRadians);
//
//		r = new Vector3d(r.x,0, r.z).normalize();		
//		l = new Vector3d(l.x,0, l.z).normalize();	
//		
//		double dotr = r.dotProduct(Hmddir);
//		double dotl = l.dotProduct(Hmddir);
//		
//		if(dotr <0) r = r.scale(-1);
//		if(dotl <0) l = l.scale(-1);
//		
//	//	if(Math.abs(dotr) < 0.5 || Math.abs(dotl) < 0.5) return;
//		
//		movedir = new Vector3d((r.x + l.x) / 2, 0, (r.z + l.z) / 2);
//		
//		Vector3d movedir= new  Vector3d(middle.x - hmdPos.x, 0, middle.z - hmdPos.z);
//		//TODO: do this betterer. Use actual controller movement dir in x-z plane.
//		movedir = movedir.normalize();
		
		//todo: skip entries? is this computationally expensive?
//		Vector3d r = MCOpenVR.controllerHistory[0].averagePosition(.25);
//		Vector3d l = MCOpenVR.controllerHistory[1].averagePosition(.25);
//		
//		Vector3d diff = l.subtract(r).rotateYaw(minecraft.getInstance().vrPlayer.worldRotationRadians);
//		
		//double ltor = Math.toDegrees(Math.atan2(-diff.x, diff.z));   
		
		Vector3d v = (mc.vrPlayer.vrdata_world_pre.getController(0).getDirection().add(mc.vrPlayer.vrdata_world_pre.getController(1).getDirection())).scale(0.5f);
		direction =  (float)Math.toDegrees(Math.atan2(-v.x, v.z)); 
		double spd = (c0move + c1move) / 2;	
		this.speed = spd * 1 * 1.3;
		if(this.speed > .1) this.speed = 1.0f;
		if(this.speed > 1.0) this.speed = 1.3f;
	
	}

}
