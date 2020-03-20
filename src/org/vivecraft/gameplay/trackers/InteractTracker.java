package org.vivecraft.gameplay.trackers;

import java.lang.reflect.Field;
import java.nio.channels.GatheringByteChannel;
import java.util.HashSet;
import java.util.List;

import javax.swing.RowFilter.ComparisonType;

import org.vivecraft.control.ControllerType;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.utils.MCReflection;
import org.vivecraft.utils.MCReflection.ReflectionMethod;

import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.BambooBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.inventory.HorseInventoryScreen;
import net.minecraft.client.renderer.FirstPersonRenderer.VRFirstPersonArmSwing;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.item.AbstractMapItem;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.CarrotOnAStickItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.item.TridentItem;
import net.minecraft.item.UseAction;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.math.RayTraceContext.BlockMode;
import net.minecraft.util.math.RayTraceContext.FluidMode;

public class InteractTracker extends Tracker{


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
		if(p.isActiveItemStackBlocking()){
			return false; 
		}
		return true;    
	}

	public BlockRayTraceResult[] inBlockHit = new BlockRayTraceResult[2];
	BlockPos[] inBlockPos = new BlockPos[2];
	Entity[] inEntity = new Entity[2];
	public EntityRayTraceResult[] inEntityHit = new EntityRayTraceResult[2];
	boolean[] active = new boolean[2];
	boolean[] wasactive = new boolean[2];
	
	@Override
	public void reset(ClientPlayerEntity player) {
		for(int c =0 ;c<2;c++){
			inBlockPos[c] = null;
			inBlockHit[c] = null;
			inEntity[c] = null;
			inEntityHit[c] = null;
			active[c] = false;
		}
	}

	private HashSet<Class> rightClickable = null;

	public void doProcess(ClientPlayerEntity player){ //on tick
		
		if(rightClickable == null) {
			//compile a list of blocks that explicitly declare OnBlockActivated (right click)
			rightClickable = new HashSet<Class>();
			for (Object b : Registry.BLOCK) {
				Class c = b.getClass();
				try {
					ReflectionMethod r = new MCReflection.ReflectionMethod(c, MCReflection.BlockState_OnBlockActivated, BlockState.class, World.class, BlockPos.class, PlayerEntity.class, Hand.class, BlockRayTraceResult.class);
					rightClickable.add(c);
				} catch (Exception e) {
				}
				c = c.getSuperclass();
				try {
					ReflectionMethod r = new MCReflection.ReflectionMethod(c, MCReflection.BlockState_OnBlockActivated, BlockState.class, World.class, BlockPos.class, PlayerEntity.class, Hand.class, BlockRayTraceResult.class);
					rightClickable.add(c);
				} catch (Exception e) {
				}
			}
		}
		
		rightClickable.remove(Block.class);
		Vec3d forward = new Vec3d(0,0,-1);

		reset(player);
		
		for(int c =0 ;c<2;c++){
		
			Vec3d hmdPos = mc.vrPlayer.vrdata_world_pre.getHeadPivot();
			Vec3d handPos = mc.vrPlayer.vrdata_world_pre.getController(c).getPosition();
			Vec3d handDirection = mc.vrPlayer.vrdata_world_pre.getHand(c).getCustomVector(forward);

			ItemStack is = player.getHeldItem(c==0?Hand.MAIN_HAND:Hand.OFF_HAND);
			Item item = null;

			int bx = (int) MathHelper.floor(handPos.x);
			int by = (int) MathHelper.floor(handPos.y);
			int bz = (int) MathHelper.floor(handPos.z);

			boolean inAnEntity = false;
			boolean insolidBlock = false;

			Vec3d extWeapon = new Vec3d(
					handPos.x + handDirection.x * (-.1),
					handPos.y + handDirection.y * (-.1),
					handPos.z + handDirection.z * (-.1));

			AxisAlignedBB weaponBB = new AxisAlignedBB(
					handPos.x < extWeapon.x ? handPos.x : extWeapon.x  ,
							handPos.y < extWeapon.y ? handPos.y : extWeapon.y  ,
									handPos.z < extWeapon.z ? handPos.z : extWeapon.z  ,
											handPos.x > extWeapon.x ? handPos.x : extWeapon.x  ,
													handPos.y > extWeapon.y ? handPos.y : extWeapon.y  ,
															handPos.z > extWeapon.z ? handPos.z : extWeapon.z  
					);

			
            inEntityHit[c] = ProjectileHelper.func_221273_a(mc.getRenderViewEntity(), hmdPos, handPos, weaponBB, (e) ->
            {
                return !e.isSpectator() && e.canBeCollidedWith()  && !(e == mc.getRenderViewEntity().getRidingEntity());
            }, 0);
            
            if(inEntityHit[c]!=null) {
            	Entity hitEntity = inEntityHit[c].getEntity();
        		inAnEntity = true;
        		inEntity[c] = hitEntity;
        		active[c] = true;
            }

			if(!inAnEntity){
				BlockPos bp =null;
				bp = new BlockPos(handPos);
				BlockState block = mc.world.getBlockState(bp);
				//	Material material = block.getMaterial();

				BlockRayTraceResult hit = block.getRenderShape(mc.world, bp).rayTrace(hmdPos, handPos, bp);
				inBlockPos[c] = bp;
				inBlockHit[c] = hit;		     

				active[c] = hit !=null && (rightClickable.contains(block.getBlock().getClass()) || 
						rightClickable.contains(block.getBlock().getClass().getSuperclass()));
			
			}
			
			if(!wasactive[c] && active[c]) {
				MCOpenVR.triggerHapticPulse(c, 250);
			}
			
			MCOpenVR.getInputAction(MCOpenVR.keyVRInteract).setEnabled(ControllerType.values()[c], active[c]);
			
			wasactive[c] = active[c];
		}
	}

	public boolean isInteractActive(int controller) {
		return active[controller];
	}

	public void processBindings() {
		for(int c =0 ;c<2;c++){
			if(MCOpenVR.keyVRInteract.isPressed(ControllerType.values()[c])) {
				if (!active[c]) continue; //how tho?
				Hand hand = Hand.values()[c];
				if(inEntityHit[c]!=null) {            
					if (mc.playerController.interactWithEntity(mc.player, inEntity[c], inEntityHit[c], hand) != ActionResultType.SUCCESS)
					 if (mc.playerController.interactWithEntity(mc.player, inEntity[c], hand)  != ActionResultType.SUCCESS) {
						 continue;
					 }
                	mc.getFirstPersonRenderer().swingType = VRFirstPersonArmSwing.Interact;
					mc.player.swingArm(hand);
					MCOpenVR.triggerHapticPulse(c, 750);
				}
				else if (inBlockHit[c]!=null) {
					if(	mc.playerController.processRightClickBlock(mc.player, (ClientWorld) mc.player.world, hand, inBlockHit[c]) == ActionResultType.SUCCESS)
					{
	                	mc.getFirstPersonRenderer().swingType = VRFirstPersonArmSwing.Interact;
						mc.player.swingArm(hand);
						MCOpenVR.triggerHapticPulse(c, 750);	
					}
				}
			}
		}
	}
}

