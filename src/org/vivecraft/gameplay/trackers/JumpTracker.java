package org.vivecraft.gameplay.trackers;

import org.vivecraft.api.NetworkHelper;
import org.vivecraft.gameplay.VRPlayer;
import org.vivecraft.provider.openvr_jna.MCOpenVR;
import org.vivecraft.settings.AutoCalibration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;

public class JumpTracker extends Tracker {

	public Vector3d[] latchStart = new Vector3d[]{new Vector3d(0,0,0), new Vector3d(0,0,0)};
	public Vector3d[] latchStartOrigin = new Vector3d[]{new Vector3d(0,0,0), new Vector3d(0,0,0)};
	public Vector3d[] latchStartPlayer = new Vector3d[]{new Vector3d(0,0,0), new Vector3d(0,0,0)};
	private boolean c0Latched = false;
	private boolean c1Latched = false;

	public JumpTracker(Minecraft mc) {
		super(mc);
	}

	public boolean isClimbeyJump(){
    	if(!this.isActive(Minecraft.getInstance().player)) return false;
    	return(isClimbeyJumpEquipped());
    }
    
    public boolean isClimbeyJumpEquipped(){
    	return(NetworkHelper.serverAllowsClimbey && Minecraft.getInstance().player.isClimbeyJumpEquipped());
    }

	public boolean isActive(ClientPlayerEntity p){
		if(Minecraft.getInstance().vrSettings.seated)
			return false;
		if(!Minecraft.getInstance().vrPlayer.getFreeMove() && !Minecraft.getInstance().vrSettings.simulateFalling)
			return false;
		if(!Minecraft.getInstance().vrSettings.realisticJumpEnabled)
			return false;
		if(p==null || !p.isAlive())
			return false;
		if(mc.playerController == null) return false;
		if(p.isInWater() || p.isInLava() || !p.isOnGround())
			return false;
		if(p.isSneaking() || p.isPassenger())
			return false;

		return true;
	}

	public boolean isjumping(){
		return c1Latched || c0Latched;
	}

	@Override
	public void idleTick(ClientPlayerEntity player) {
		mc.vr.getInputAction(mc.vr.keyClimbeyJump).setEnabled(isClimbeyJumpEquipped() && (this.isActive(player) || (mc.climbTracker.isClimbeyClimbEquipped() && mc.climbTracker.isGrabbingLadder())));
	}

	@Override
	public void reset(ClientPlayerEntity player) {
		c1Latched = false;
		c0Latched = false;
	}

	public void doProcess(ClientPlayerEntity player){

		if(isClimbeyJumpEquipped()){

			VRPlayer provider = mc.vrPlayer;

			boolean[] ok = new boolean[2];

			for(int c=0;c<2;c++){
				ok[c]=	mc.vr.keyClimbeyJump.isKeyDown();
			}

			boolean jump = false;
			if(!ok[0] && c0Latched){ //let go right
				mc.vr.triggerHapticPulse(0, 200);
				jump = true;
			}
			
			Vector3d rpos = mc.vrPlayer.vrdata_room_pre.getController(0).getPosition();
			Vector3d lpos = mc.vrPlayer.vrdata_room_pre.getController(1).getPosition();
			Vector3d now = rpos.add(lpos).scale(0.5);

			if(ok[0] && !c0Latched){ //grabbed right
				latchStart[0] = now;
				latchStartOrigin[0] = mc.vrPlayer.vrdata_world_pre.origin;
				latchStartPlayer[0] = mc.player.getPositionVec();
				mc.vr.triggerHapticPulse(0, 1000);
			}

			if(!ok[1] && c1Latched){ //let go left
				mc.vr.triggerHapticPulse(1, 200);
				jump = true;
			}

			if(ok[1] && !c1Latched){ //grabbed left
				latchStart[1] = now;
				latchStartOrigin[1] = mc.vrPlayer.vrdata_world_pre.origin;
				latchStartPlayer[1] = mc.player.getPositionVec();
				mc.vr.triggerHapticPulse(1, 1000);
			}

			c0Latched = ok[0];
			c1Latched = ok[1];

			int c =0;


			Vector3d delta= now.subtract(latchStart[c]);

			delta = delta.rotateYaw(mc.vrPlayer.vrdata_world_pre.rotation_radians);
			

			if(!jump && isjumping()){ //bzzzzzz
				mc.vr.triggerHapticPulse(0, 200);
				mc.vr.triggerHapticPulse(1, 200);
			}

			if(jump){
				mc.climbTracker.forceActivate = true;

				Vector3d m = (mc.vr.controllerHistory[0].netMovement(0.3)
						.add(mc.vr.controllerHistory[1].netMovement(0.3)));
				
				double sp =  (mc.vr.controllerHistory[0].averageSpeed(0.3) + mc.vr.controllerHistory[1].averageSpeed(0.3)) / 2 ;	
									
				m = m.scale(0.33f * sp);
							
				//cap
				float limit = 0.66f;
				if(m.length() > limit) m = m.scale(limit/m.length());
						
				if (player.isPotionActive(Effects.JUMP_BOOST))
					m=m.scale((player.getActivePotionEffect(Effects.JUMP_BOOST).getAmplifier() + 1.5));
				
				m=m.rotateYaw(mc.vrPlayer.vrdata_world_pre.rotation_radians);
				
				Vector3d pl = mc.player.getPositionVec().subtract(delta);

				if(delta.y < 0 && m.y < 0){

					player.setMotion(
							player.getMotion().x + -m.x * 1.25
							,-m.y,
							player.getMotion().z + -m.z * 1.25);
					
					player.lastTickPosX = pl.x;
					player.lastTickPosY = pl.y;
					player.lastTickPosZ = pl.z;			
					pl = pl.add(player.getMotion());					
					player.setPosition(pl.x, pl.y, pl.z);
					mc.vrPlayer.snapRoomOriginToPlayerEntity(player, false, true);
					mc.player.addExhaustion(.3f);    
					mc.player.setOnGround(false);
				} else {
					mc.vrPlayer.snapRoomOriginToPlayerEntity(player, false, true);
				}
			}else if(isjumping()){
				Vector3d thing = latchStartOrigin[0].subtract(latchStartPlayer[0]).add(mc.player.getPositionVec()).subtract(delta);
				mc.vrPlayer.setRoomOrigin(thing.x, thing.y, thing.z, false);
			}
		}else {
			if(mc.vr.hmdPivotHistory.netMovement(0.25).y > 0.1 &&
					mc.vr.hmdPivotHistory.latest().y-AutoCalibration.getPlayerHeight() > mc.vrSettings.jumpThreshold
					){
				player.jump();
			}			
		}
	}

	public boolean isBoots(ItemStack i) {
		if(i.isEmpty())return false;
		if(!i.hasDisplayName()) return false;
		if((i.getItem() != Items.LEATHER_BOOTS)) return false;
		if(!(i.getTag().getBoolean("Unbreakable"))) return false;
		return (i.getDisplayName() instanceof TranslationTextComponent && ((TranslationTextComponent)i.getDisplayName()).getKey().equals("vivecraft.item.jumpboots")) || i.getDisplayName().getString().equals("Jump Boots");
	}
}
