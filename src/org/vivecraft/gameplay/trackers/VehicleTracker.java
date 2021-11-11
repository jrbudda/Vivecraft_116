package org.vivecraft.gameplay.trackers;

import org.vivecraft.api.VRData.VRDevicePose;
import org.vivecraft.provider.openvr_jna.MCOpenVR;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.item.minecart.MinecartEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.item.OnAStickItem;
import net.minecraft.util.math.vector.Vector3d;


public class VehicleTracker extends Tracker {

	public VehicleTracker(Minecraft mc) {
		super(mc);
	}
	
    private float PreMount_World_Rotation;
    public Vector3d Premount_Pos_Room = new Vector3d(0, 0, 0);
	public float vehicleInitialRotation = 0;

	@Override
	public boolean isActive(ClientPlayerEntity p){
		Minecraft mc = Minecraft.getInstance();
		if(p == null) return false;
		if(mc.playerController == null) return false;
		if(!p.isAlive()) return false;
		return true;
	}

	@Override
	public void reset(ClientPlayerEntity player) {
		minecartStupidityCounter = 2;
		super.reset(player);
	}

	public double getVehicleFloor(Entity vehicle, double original) {
	//	if(vehicle instanceof AbstractHorseEntity)
			return original; //horses are fine.
		
//		return vehicle.getPosY();
	}
	
	public int rotationCooldown = 0; 
	
	public static Vector3d getSteeringDirection(ClientPlayerEntity player) {
		Vector3d out = null;
		Entity e = player.getRidingEntity();
		Minecraft mc = Minecraft.getInstance();			
		if (e instanceof AbstractHorseEntity || e instanceof BoatEntity) {
			if (player.moveForward > 0){
				if(mc.vrSettings.vrFreeMoveMode == mc.vrSettings.FREEMOVE_HMD ){
					return mc.vrPlayer.vrdata_world_pre.hmd.getDirection();
				}else{
					return mc.vrPlayer.vrdata_world_pre.getController(0).getDirection();
				}			
			}
		}else if (e instanceof MobEntity) {
			MobEntity el = (MobEntity) e; //pigs and striders
			if (el.canBeSteered()){
				int c = (player.getHeldItemMainhand().getItem() instanceof OnAStickItem) ? 0 : 1;
				VRDevicePose con = mc.vrPlayer.vrdata_world_pre.getController(c);
				return con.getPosition().add(con.getDirection().scale(0.3)).subtract(e.getPositionVec()).normalize();
			}
		}	
		return out;
	}
	
	@Override
	public void doProcess(ClientPlayerEntity player){
		if(!mc.isGamePaused())
		{ //do vehicle rotation, which rotates around a different point.

			if (dismountCooldown > 0) dismountCooldown--;
			if (rotationCooldown > 0) rotationCooldown--;
			
			if(mc.vrSettings.vehicleRotation && mc.player.isPassenger() && rotationCooldown == 0){
				Entity e = mc.player.getRidingEntity();		
				rotationTarget = e.rotationYaw;

				if (e instanceof AbstractHorseEntity && !mc.horseTracker.isActive(mc.player)) {
					AbstractHorseEntity el = (AbstractHorseEntity) e;
					if (el.canBeSteered() && el.isHorseSaddled()){
						return;
					}
					rotationTarget = el.renderYawOffset;
				}else if (e instanceof MobEntity) {
					MobEntity el = (MobEntity) e; //this is just pigs in vanilla
					if (el.canBeSteered()){
						return; 
					}
					rotationTarget = el.renderYawOffset;
				}

				boolean smooth = true;
				float smoothIncrement = 10;

				if(e instanceof MinecartEntity){ //what a pain in my ass
				
					if(shouldMinecartTurnView((MinecartEntity) e)) {
						if(minecartStupidityCounter > 0) 
							minecartStupidityCounter--;
					}
					else
						minecartStupidityCounter = 3;

					rotationTarget =  getMinecartRenderYaw((MinecartEntity) e);

					if(minecartStupidityCounter > 0) { //do nothing
						vehicleInitialRotation = (float) rotationTarget;
					}

					double spd = mineCartSpeed((MinecartEntity) e);
					smoothIncrement = 200 * (float) (spd * spd);
					if (smoothIncrement < 10) smoothIncrement = 10;
	//				System.out.println(spd + " " + smoothIncrement);

				}
											
				float difference = mc.vrPlayer.rotDiff_Degrees((float) rotationTarget, vehicleInitialRotation);
				
				if (smooth) {
					if(difference > smoothIncrement) {
						difference = smoothIncrement;
					}

					if(difference < -smoothIncrement) {
						difference = -smoothIncrement;
					}
				}
		//		System.out.println("start " + vehicleInitialRotation + " end " + rotationTarget + " diff " + difference);
				
				//mc.vrPlayer.rotateOriginAround(difference,  e.getPositionVector());

				mc.vrSettings.vrWorldRotation += difference;
				mc.vrSettings.vrWorldRotation %= 360;
				mc.vr.seatedRot = mc.vrSettings.vrWorldRotation;

				vehicleInitialRotation -= difference;
				vehicleInitialRotation %= 360;


			} else {
				minecartStupidityCounter = 3;
				if(mc.player.isPassenger()){		
					vehicleInitialRotation =  mc.player.getRidingEntity().rotationYaw;				
				}
			}
		}
		
	}
	
	private double rotationTarget = 0;
	
	public void onStartRiding(Entity vehicle, ClientPlayerEntity player) {
		Minecraft mc = Minecraft.getInstance();
		
		PreMount_World_Rotation = mc.vrPlayer.vrdata_world_pre.rotation_radians;
		Vector3d campos = mc.vrPlayer.vrdata_room_pre.getHeadPivot();
		Premount_Pos_Room = new Vector3d(campos.x, 0, campos.z);
		dismountCooldown = 5;
		//mc.vrPlayer.snapRoomOriginToPlayerEntity(this, false);
		if(mc.vrSettings.vehicleRotation){
			float end = mc.vrPlayer.vrdata_world_pre.hmd.getYaw();
			float start = vehicle.rotationYaw % 360;
			
			vehicleInitialRotation = mc.vrSettings.vrWorldRotation;
			rotationCooldown = 2;
			
			if(vehicle instanceof MinecartEntity)
				return; // dont align player with minecart, it doesn't have a 'front'
			
			float difference = mc.vrPlayer.rotDiff_Degrees(start, end);
	    // 	System.out.println("OnStart " + start + " " + end + " " + difference);
        	mc.vrSettings.vrWorldRotation = (float) (Math.toDegrees(mc.vrPlayer.vrdata_world_pre.rotation_radians) + difference);
        	mc.vrSettings.vrWorldRotation %= 360;
        	mc.vr.seatedRot = mc.vrSettings.vrWorldRotation;

        }
	}
	
	public void onStopRiding(ClientPlayerEntity player) {
        mc.swingTracker.disableSwing = 10;
        mc.sneakTracker.sneakCounter = 0;
        if(mc.vrSettings.vehicleRotation){
       	//I dont wanna do this anymore. 
        //I think its more confusing to get off the thing an not know where you're looking
        //	mc.vrSettings.vrWorldRotation = playerRotation_PreMount;
        //	mc.vr.seatedRot = playerRotation_PreMount;
        }
	}
	
	private int minecartStupidityCounter;
	
	private float getMinecartRenderYaw(MinecartEntity entity){	
		Vector3d spd = new Vector3d(entity.getPosX() - entity.lastTickPosX, entity.getPosY() - entity.lastTickPosY, entity.getPosZ() - entity.lastTickPosZ);
		float spdyaw = (float)Math.toDegrees((Math.atan2(-spd.x, spd.z)));
		if(shouldMinecartTurnView(entity))
			return -180+spdyaw;
		else
			return vehicleInitialRotation;
	}
	
	private double mineCartSpeed(MinecartEntity entity) {
		Vector3d spd = new Vector3d(entity.getMotion().x, 0, entity.getMotion().z);
		return spd.length();
	}
	
	private boolean shouldMinecartTurnView(MinecartEntity entity){	
		Vector3d spd = new Vector3d(entity.getPosX() - entity.lastTickPosX, entity.getPosY() - entity.lastTickPosY, entity.getPosZ() - entity.lastTickPosZ);
		return spd.length() > 0.001;
	}
	
	public int dismountCooldown = 0;
	public boolean canRoomscaleDismount(ClientPlayerEntity player) {
		 return player.moveForward ==0 && player.moveStrafing ==0 && player.isPassenger() && player.getRidingEntity().isOnGround() && dismountCooldown ==0;
	}
	
}
