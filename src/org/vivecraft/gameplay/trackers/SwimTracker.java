package org.vivecraft.gameplay.trackers;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Created by Hendrik on 02-Aug-16.
 */
public class SwimTracker extends Tracker {

	Vec3d motion=Vec3d.ZERO;
	double friction=0.9f;

	double lastDist;

	final double riseSpeed=0.005f;
	double swimspeed=1.3f;

	public SwimTracker(Minecraft mc) {
		super(mc);
	}

	public boolean isActive(ClientPlayerEntity p){
		if(mc.vrSettings.seated)
			return false;
		if(!mc.vrSettings.realisticSwimEnabled)
			return false;
		if(mc.currentScreen != null)
			return false;
		if(p==null || !p.isAlive())
			return false;
		if(mc.playerController == null) return false;
		if(!p.isInWater() && !p.isInLava())
			return false;
		if(p.moveForward > 0)
			return false;
		if(p.moveStrafing > 0)
			return false;
		return true;
	}

	public void doProcess(ClientPlayerEntity player){

//		{//float
//			//remove bouyancy for now.
//			Vec3d face = mc.vrPlayer.vrdata_world_pre.hmd.getPosition();
//			float height = (float) (mc.vrPlayer.vrdata_room_pre.hmd.getPosition().y * 0.9);
//			if(height > 1.6)height = 1.6f;
//			Vec3d feets = face.subtract(0,height, 0);
//			double waterLine=256;
//
//			BlockPos bp = new BlockPos(feets);
//			for (int i = 0; i < 4; i++) {
//				Material mat=player.world.getBlockState(bp).getMaterial();
//				if(!mat.isLiquid())
//				{
//					waterLine=bp.getY();
//					break;
//				}
//				bp = bp.up();
//			}
//
//			double percent = (waterLine - feets.y) / (face.y - feets.y);
//
//			if(percent < 0){
//				//how did u get here, drybones?
//				return;
//			}
//
//			if(percent < 0.5 && player.onGround){
//				return;
//				//no diving in the kiddie pool.
//			}
//
//			player.addVelocity(0, 0.018D , 0); //counteract most gravity.
//
//			double neutal = player.collidedHorizontally? 0.5 : 1;
//
//			if(percent > neutal && percent < 2){ //between halfway submerged and 1 body length under.
//				//rise!
//				double buoyancy = 2 - percent;
//				if(player.collidedHorizontally)  player.addVelocity(0, 00.03f, 0);	
//				player.addVelocity(0, 0.0015 + buoyancy/100 , 0);		
//			}

//		}
		{//swim

			Vec3d controllerR= mc.vrPlayer.vrdata_world_pre.getController(0).getPosition();
			Vec3d controllerL= mc.vrPlayer.vrdata_world_pre.getController(1).getPosition();
			
			Vec3d middle= controllerL.subtract(controllerR).scale(0.5).add(controllerR);

			Vec3d hmdPos=mc.vrPlayer.vrdata_world_pre.getHeadPivot().subtract(0,0.3,0);

			Vec3d movedir=middle.subtract(hmdPos).normalize().add(
					mc.vrPlayer.vrdata_world_pre.hmd.getDirection()).scale(0.5);

			Vec3d contollerDir= mc.vrPlayer.vrdata_world_pre.getController(0).getCustomVector(new Vec3d(0,0,-1)).add(
					mc.vrPlayer.vrdata_world_pre.getController(1).getCustomVector(new Vec3d(0,0,-1))).scale(0.5);
			double dirfactor=contollerDir.add(movedir).length()/2;

			double distance= hmdPos.distanceTo(middle);
			double distDelta=lastDist-distance;

			if(distDelta>0){
				Vec3d velo=movedir.scale(distDelta*swimspeed*dirfactor);
				motion=motion.add(velo.scale(0.15));
			}

			lastDist=distance;
			player.setSwimming(motion.length() > 0.3f);
			player.setSprinting(motion.length() > 1.0f);
			player.addVelocity(motion.x,motion.y,motion.z);
			motion=motion.scale(friction);
		}
	}
}
