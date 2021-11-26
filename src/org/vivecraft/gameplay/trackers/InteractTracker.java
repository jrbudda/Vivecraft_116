package org.vivecraft.gameplay.trackers;

import java.util.HashSet;

import org.vivecraft.api.NetworkHelper;
import org.vivecraft.api.VRData;
import org.vivecraft.provider.ControllerType;
import org.vivecraft.provider.openvr_jna.MCOpenVR;
import org.vivecraft.reflection.MCReflection;
import org.vivecraft.reflection.MCReflection.ReflectionMethod;
import org.vivecraft.render.RenderPass;
import org.vivecraft.render.VRFirstPersonArmSwing;
import org.vivecraft.settings.VRHotkeys;
import org.vivecraft.settings.VRSettings;
import org.vivecraft.utils.math.Quaternion;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class InteractTracker extends Tracker{

	public boolean[] bukkit= new boolean[2];
	public int hotbar = -1;

	public InteractTracker(Minecraft mc) {
		super(mc);
	}

	public boolean isActive(ClientPlayerEntity p){
		if(mc.playerController == null) return false;
		if(p == null) return false;
		if(!p.isAlive()) return false;
		if(p.isSleeping()) return false;
		Minecraft mc = Minecraft.getInstance();
		if (mc.vrSettings.seated)
			return false;
		if(p.isActiveItemStackBlocking() && hotbar < 0){
			return false; 
		}
		if (mc.bowTracker.isNotched())
			return false;
		return true;    
	}

	public BlockRayTraceResult[] inBlockHit = new BlockRayTraceResult[2];
	BlockPos[] inBlockPos = new BlockPos[2];
	Entity[] inEntity = new Entity[2];
	private EntityRayTraceResult[] inEntityHit = new EntityRayTraceResult[2];
	private boolean[] inCamera = new boolean[2];
	private boolean[] inHandheldCamera = new boolean[2];
	boolean[] active = new boolean[2];
	boolean[] wasactive = new boolean[2];

	@Override
	public void reset(ClientPlayerEntity player) {
		for(int c =0 ;c<2;c++){
			reset(player, c);
		}
	}

	private void reset(ClientPlayerEntity player, int c) {
		if (inCamera[c] && VRHotkeys.isMovingThirdPersonCam() && VRHotkeys.getMovingThirdPersonCamTriggerer() == VRHotkeys.Triggerer.INTERACTION && VRHotkeys.getMovingThirdPersonCamController() == c)
			VRHotkeys.stopMovingThirdPersonCam();
		if (inHandheldCamera[c] && mc.cameraTracker.isMoving() && mc.cameraTracker.getMovingController() == c && !mc.cameraTracker.isQuickMode())
			mc.cameraTracker.stopMoving();
		inBlockPos[c] = null;
		inBlockHit[c] = null;
		inEntity[c] = null;
		inEntityHit[c] = null;
		inCamera[c] = false;
		inHandheldCamera[c] = false;
		active[c] = false;
		mc.vr.getInputAction(mc.vr.keyVRInteract).setEnabled(ControllerType.values()[c], false);
	}

	private HashSet<Class> rightClickable = null;

	@SuppressWarnings("unused")
	public void doProcess(ClientPlayerEntity player){ //on tick

		if(rightClickable == null) {
			//compile a list of blocks that explicitly declare OnBlockActivated (right click)
			rightClickable = new HashSet<Class>();
			for (Object b : Registry.BLOCK) {
				Class c = b.getClass();
				try { // constructor throws an exception if method doesn't exist
					ReflectionMethod r = new MCReflection.ReflectionMethod(c, MCReflection.BlockState_OnBlockActivated, BlockState.class, World.class, BlockPos.class, PlayerEntity.class, Hand.class, BlockRayTraceResult.class);
					rightClickable.add(c);
				} catch (Throwable e) {
				}
				c = c.getSuperclass();
				try {
					ReflectionMethod r = new MCReflection.ReflectionMethod(c, MCReflection.BlockState_OnBlockActivated, BlockState.class, World.class, BlockPos.class, PlayerEntity.class, Hand.class, BlockRayTraceResult.class);
					rightClickable.add(c);
				} catch (Throwable e) {
				}
			}
			rightClickable.remove(Block.class);
			rightClickable.remove(AbstractBlock.class);
			rightClickable.remove(AbstractBlock.AbstractBlockState.class);
		}

		Vector3d forward = new Vector3d(0,0,-1);

		for(int c =0 ;c<2;c++){
			if ((inCamera[c] || inHandheldCamera[c]) && mc.vr.keyVRInteract.isKeyDown(ControllerType.values()[c]))
				continue;

			reset(player, c);

			if(c == 0) {
				if(hotbar >= 0) {
					active[c] = true;
				}
			}

			Vector3d hmdPos = mc.vrPlayer.vrdata_world_pre.getHeadPivot();
			Vector3d handPos = mc.vrPlayer.vrdata_world_pre.getController(c).getPosition();
			Vector3d handDirection = mc.vrPlayer.vrdata_world_pre.getHand(c).getCustomVector(forward);
			ItemStack is = player.getHeldItem(c==0?Hand.MAIN_HAND:Hand.OFF_HAND);
			Item item = null;

			if (!active[c] && (mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_MIXED_REALITY || mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_THIRD_PERSON) && mc.vrSettings.mixedRealityRenderCameraModel) {
				VRData.VRDevicePose camData = mc.vrPlayer.vrdata_world_pre.getEye(RenderPass.THIRD);
				Vector3d camPos = camData.getPosition();
				camPos = camPos.subtract(camData.getCustomVector(new Vector3d(0, 0, -1)).scale(0.15f));
				camPos = camPos.subtract(camData.getCustomVector(new Vector3d(0, -1, 0)).scale(0.05f));

				if (handPos.distanceTo(camPos) < 0.15f) {
					inCamera[c] = true;
					active[c] = true;
				}
			}

			if (!active[c] && mc.cameraTracker.isVisible() && !mc.cameraTracker.isQuickMode()) {
				VRData.VRDevicePose camData = mc.vrPlayer.vrdata_world_pre.getEye(RenderPass.CAMERA);
				Vector3d camPos = camData.getPosition();
				camPos = camPos.subtract(camData.getCustomVector(new Vector3d(0, 0, -1)).scale(0.08f));

				if (handPos.distanceTo(camPos) < 0.11f) {
					inHandheldCamera[c] = true;
					active[c] = true;
				}
			}

			if(!active[c]) {

				int bx = (int) MathHelper.floor(handPos.x);
				int by = (int) MathHelper.floor(handPos.y);
				int bz = (int) MathHelper.floor(handPos.z);

				Vector3d extWeapon = new Vector3d(
						handPos.x + handDirection.x * (-.1),
						handPos.y + handDirection.y * (-.1),
						handPos.z + handDirection.z * (-.1));

				AxisAlignedBB weaponBB = new AxisAlignedBB(handPos, extWeapon);


				inEntityHit[c] = ProjectileHelper.rayTraceEntities(mc.getRenderViewEntity(), hmdPos, handPos, weaponBB, (e) ->
				{
					return !e.isSpectator() && e.canBeCollidedWith()  && !(e == mc.getRenderViewEntity().getRidingEntity());
				}, 0);

				if(inEntityHit[c]!=null) {
					Entity hitEntity = inEntityHit[c].getEntity();
					inEntity[c] = hitEntity;
					active[c] = true;
				}
			}

			if(!active[c]) {
				BlockPos bp =null;
				bp = new BlockPos(handPos);
				BlockState block = mc.world.getBlockState(bp);
				//	Material material = block.getMaterial();

				BlockRayTraceResult hit = block.getRenderShapeTrue(mc.world, bp).rayTrace(hmdPos, handPos, bp);
				inBlockPos[c] = bp;
				inBlockHit[c] = hit;		     

				active[c] = hit !=null && (rightClickable.contains(block.getBlock().getClass()) || 
						rightClickable.contains(block.getBlock().getClass().getSuperclass()));

				bukkit[c] = false;
				if(!active[c] && is.getItem() == Items.BUCKET) {
					if(block.getMaterial().isLiquid()) {
						active[c] = true;
						bukkit[c] = true;
					}
				}			
			}

			if(!wasactive[c] && active[c]) {
				mc.vr.triggerHapticPulse(c, 250);
			}

			mc.vr.getInputAction(mc.vr.keyVRInteract).setEnabled(ControllerType.values()[c], active[c]);

			wasactive[c] = active[c];
		}
	}

	public boolean isInteractActive(int controller) {
		return active[controller];
	}

	public boolean isInCamera() {
		return inCamera[0] || inCamera[1];
	}

	public boolean isInHandheldCamera() {
		return inHandheldCamera[0] || inHandheldCamera[1];
	}

	public void processBindings() {
		for(int c =0 ;c<2;c++){
			if(mc.vr.keyVRInteract.isPressed(ControllerType.values()[c])) {
				if (!active[c]) 
					continue; //how tho?
				Hand hand = Hand.values()[c];
				boolean success = false;
				
				if(hotbar >= 0 && hotbar < 9 && mc.player.inventory.currentItem != hotbar && hand == Hand.MAIN_HAND) {
					mc.player.inventory.currentItem = hotbar;
					success = true;
				}
				else if(hotbar == 9 && hand == Hand.MAIN_HAND) {
					mc.player.connection.sendPacket(new CPlayerDiggingPacket(CPlayerDiggingPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
					success = true;
				}
				else if (inCamera[c]) {
					VRHotkeys.startMovingThirdPersonCam(c, VRHotkeys.Triggerer.INTERACTION);
					success = true;
				}
				else if (inHandheldCamera[c]) {
					mc.cameraTracker.startMoving(c);
					success = true;
				}
				else if(inEntityHit[c]!=null) {     
					success = true;
					if (!mc.playerController.interactWithEntity(mc.player, inEntity[c], inEntityHit[c], hand).isSuccessOrConsume())
					 if (!mc.playerController.interactWithEntity(mc.player, inEntity[c], hand).isSuccessOrConsume()) {
							success = false;
					 }		
				}
				else if (inBlockHit[c]!=null) {
					success = mc.playerController.func_217292_a(mc.player, (ClientWorld) mc.player.world, hand, inBlockHit[c]).isSuccessOrConsume();
				} else if (bukkit[c]) {
					success =mc.playerController.processRightClick(mc.player, (ClientWorld) mc.player.world, hand).isSuccessOrConsume();
				}
				
				if(success){
					mc.player.swingArm(hand, VRFirstPersonArmSwing.Interact);
					mc.vr.triggerHapticPulse(c, 750);	
				}
			}
		}
	}
}

