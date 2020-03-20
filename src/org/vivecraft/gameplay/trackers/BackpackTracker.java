package org.vivecraft.gameplay.trackers;

import org.vivecraft.gameplay.OpenVRPlayer;
import org.vivecraft.provider.MCOpenVR;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;


public class BackpackTracker extends Tracker {
	public boolean[] wasIn = new boolean[2];
	public boolean[] hystersis = new boolean[2];

	public int previousSlot = 0;

	public BackpackTracker(Minecraft mc) {
		super(mc);
	}

	public boolean isActive(ClientPlayerEntity p){
		Minecraft mc = Minecraft.getInstance();
		if(mc.vrSettings.seated) return false;
		if(!mc.vrSettings.backpackSwitching) return false;
		if(p == null) return false;
		if(mc.playerController == null) return false;
		if(!p.isAlive()) return false;
		if(p.isSleeping()) return false;
		if(mc.bowTracker.isDrawing) return false;
		return true;
	}

	
	private Vec3d down = new Vec3d(0, -1, 0);
	
	public void doProcess(ClientPlayerEntity player){
		OpenVRPlayer provider = mc.vrPlayer;

		Vec3d hmdPos=provider.vrdata_room_pre.getHeadRear();

		for(int c=0; c<2; c++) {
			Vec3d controllerPos = provider.vrdata_room_pre.getController(c).getPosition();//.add(provider.getCustomControllerVector(c, new Vec3(0, 0, -0.1)));
			Vec3d controllerDir = provider.vrdata_room_pre.getHand(c).getDirection();
			Vec3d hmddir = provider.vrdata_room_pre.hmd.getDirection();
			Vec3d delta = hmdPos.subtract(controllerPos);
			double dot = controllerDir.dotProduct(down);
			double dotDelta = delta.dotProduct(hmddir);
			
			boolean below  = ((Math.abs(hmdPos.y - controllerPos.y)) < 0.25);
			boolean behind = (dotDelta > 0); 
			boolean aimdown = (dot > .6);
			
			boolean zone = below && behind && aimdown;
			
			Minecraft mc = Minecraft.getInstance();
			if (zone){
				if(!wasIn[c]){
					if(c==0){ //mainhand
						if((mc.climbTracker.isGrabbingLadder() && 
								mc.climbTracker.isClaws(mc.player.getHeldItemMainhand()))){}
						else{
						if(player.inventory.currentItem != 0){
							previousSlot = player.inventory.currentItem;
							player.inventory.currentItem = 0;	
						} else {
							player.inventory.currentItem = previousSlot;
							previousSlot = 0;
						}}
					}
					else { //offhand
						if((mc.climbTracker.isGrabbingLadder() && 
								mc.climbTracker.isClaws(mc.player.getHeldItemOffhand()))){}
						else {
							if (mc.vrSettings.physicalGuiEnabled) {
								mc.physicalGuiManager.toggleInventoryBag();
							} else
								player.connection.sendPacket(new CPlayerDiggingPacket(CPlayerDiggingPacket.Action.SWAP_HELD_ITEMS, BlockPos.ZERO, Direction.DOWN));
						}
					}
					MCOpenVR.triggerHapticPulse(c, 1500);
					wasIn[c] = true;
					hystersis[c] = true;
				}
			} else {
				if(hystersis[c]) {
					wasIn[c] = !behind && !aimdown;
					hystersis[c] = wasIn[c];
				} else {
					wasIn[c] = false;
				}
			}
		}
}

}
