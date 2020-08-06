package org.vivecraft.gameplay.trackers;

import java.util.Comparator;
import java.util.List;

import org.vivecraft.control.ControllerType;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.reflection.MCReflection;

import net.minecraft.block.BlockState;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.OnAStickItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceContext.BlockMode;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.vector.Vector3d;

public class SwingTracker extends Tracker{

	//VIVECRAFT SWINGING SUPPORT
	private Vector3d[] lastWeaponEndAir = new Vector3d[]{new Vector3d(0, 0, 0), new Vector3d(0,0,0)};
	private boolean[] lastWeaponSolid = new boolean[2];
	public Vector3d[] weaponEnd= new Vector3d[2];

	public boolean[] canact= new boolean[2];

	public int disableSwing = 3;

	public SwingTracker(Minecraft mc) {
		super(mc);
	}

	public boolean isActive(ClientPlayerEntity p){
		if(disableSwing > 0) {
			disableSwing--;
			return false;
		}
		if(mc.playerController == null) return false;
		if(p == null) return false;
		if(!p.isAlive()) return false;
		if(p.isSleeping()) return false;
		Minecraft mc = Minecraft.getInstance();
		if (mc.currentScreen !=null)
			return false;
		if (mc.vrSettings.weaponCollision == 0)
			return false;
		if (mc.vrSettings.weaponCollision == 2)
			return !p.isCreative();
		if (mc.vrSettings.seated)
			return false;
		if(mc.vrSettings.vrFreeMoveMode == mc.vrSettings.FREEMOVE_RUNINPLACE && p.moveForward > 0){
			return false; //dont hit things while RIPing.
		}
		if(p.isActiveItemStackBlocking()){
			return false; //dont hit things while blocking.
		}
		if(mc.jumpTracker.isjumping()) 
			return false;
		return true;    
	}

	public static boolean isTool(Item item) {

		boolean flag = (item instanceof ToolItem ||
				item instanceof ArrowItem ||
				item instanceof HoeItem || 
				item instanceof FishingRodItem || 
				item instanceof OnAStickItem ||
				item instanceof ShearsItem||
				item == Items.BONE ||
				item == Items.BLAZE_ROD||
				item == Items.BAMBOO ||
				item == Items.TORCH ||
				item == Items.REDSTONE_TORCH ||
				item == Items.STICK ||
				item == Items.DEBUG_STICK ||
				item instanceof FlintAndSteelItem);

		return flag;
	}

	Vector3d forward = new Vector3d(0,0,-1);
	double speedthresh = 3.8f;
	
	public void doProcess(ClientPlayerEntity player){ //on tick
		speedthresh = 3.8f;
		mc.getProfiler().startSection("updateSwingAttack");

		for(int c=0;c<2;c++){
			
			if (mc.climbTracker.isGrabbingLadder(c)) continue;

			Vector3d handPos = mc.vrPlayer.vrdata_world_pre.getController(c).getPosition();
			Vector3d handDirection = mc.vrPlayer.vrdata_world_pre.getHand(c).getCustomVector(forward);

			ItemStack is = player.getHeldItem(c==0?Hand.MAIN_HAND:Hand.OFF_HAND);
			Item item = is.getItem();

			float weaponLength;
			float entityReachAdd;

			boolean tool = false;
			boolean sword = false;

			if(item instanceof SwordItem || item instanceof TridentItem){
				sword = true;
				tool = true;    	
			}
			else if (isTool(item)) {
				tool = true;
			}
			//            else if(item !=null && Reflector.forgeExists()){ //tinkers hack
			//            	String t = item.getClass().getSuperclass().getName().toLowerCase();
			//            	//System.out.println(c);
			//            	if (t.contains("weapon") || t.contains("sword")) {
			//            		sword = true;
			//            		tool = true;
			//            	} else 	if 	(t.contains("tool")){
			//            		tool = true;
			//            	}
			//            }    

			if (sword){
				entityReachAdd = 1.8f;
				weaponLength = 0.7f;
				tool = true;
			} else if (tool){
				entityReachAdd = 1.0f;
				weaponLength = 0.5f;
				tool = true;
			} else if (!is.isEmpty()){
				weaponLength = 0.1f;
				entityReachAdd = 0.3f;
			} else {
				weaponLength = 0.0f;
				entityReachAdd = 0.3f;
			}

			weaponLength *= mc.vrPlayer.vrdata_world_pre.worldScale;

			weaponEnd[c] = new Vector3d(
					handPos.x + handDirection.x * weaponLength,
					handPos.y + handDirection.y * weaponLength,
					handPos.z + handDirection.z * weaponLength);     


			boolean inAnEntity = false;

			float speed = (float) MCOpenVR.controllerForwardHistory[c].averageSpeed(0.2);
			canact[c] = speed > speedthresh && !lastWeaponSolid[c];

			//Check EntityCollisions first    	
			{
				AxisAlignedBB weaponBB = new AxisAlignedBB(handPos, weaponEnd[c]);      
				Vector3d extWeapon = new Vector3d(
						handPos.x + handDirection.x * (weaponLength + entityReachAdd),
						handPos.y + handDirection.y * (weaponLength + entityReachAdd),
						handPos.z + handDirection.z * (weaponLength + entityReachAdd));
				AxisAlignedBB weaponBBEXT = new AxisAlignedBB(handPos, extWeapon);     

				List<Entity> mobs = mc.world.getEntitiesWithinAABBExcludingEntity(mc.player, weaponBBEXT);       	      		
				mobs.removeIf(e -> e instanceof PlayerEntity);
				List<Entity> players = mc.world.getEntitiesWithinAABBExcludingEntity(mc.player, weaponBB);
				players.removeIf(e -> !(e instanceof PlayerEntity));

				mobs.addAll(players);
				//mobs.sort((Entity e1, Entity e2) -> Double.compare(e1.getDistanceSq(handPos), e2.getDistanceSq(handPos)));
				//There is no point in the sort due to hitting them all, see next comment.
				
				for (Entity hitEntity : mobs) {
					if (hitEntity.canBeCollidedWith() && !(hitEntity == mc.getRenderViewEntity().getRidingEntity()) )
					{       			       			
						if(canact[c]){
							Minecraft.getInstance().physicalGuiManager.preClickAction();
							mc.playerController.attackEntity(player, hitEntity);
							MCOpenVR.triggerHapticPulse(c, 1000);
							lastWeaponSolid[c] = true;
						}
						inAnEntity = true;
						//break;
						//I'm on the fence about this break.
						//On the one hand, sending multiple attack packets per tick on different entities is not vanilla.
						//On the other hand, being able to attack everything in range all at once with one swing is a little overpowered.
						//On the the other other hand, knowing which entities can actually be hurt due to cooldowns, etc, is a server-side thing..
						//...so it's possible that swinging at a group of enemies will never hit certain ones if we break on the first.
						//It's mostly due to this last reason that I am removing the break and leaving the range shorter than clicking.
					}
				}     
			}

			{ //block check

				//dont hit blocks with sword or same time as hitting entity
				canact[c] = canact[c] && !sword && !inAnEntity; 
				
				if(mc.climbTracker.isClimbeyClimb()){
					if(c == 0 && MCOpenVR.keyClimbeyGrab.isKeyDown(ControllerType.RIGHT) || !tool ) continue;
					if(c == 1 && MCOpenVR.keyClimbeyGrab.isKeyDown(ControllerType.LEFT) || !tool ) continue;
				}

				BlockPos bp = new BlockPos(weaponEnd[c]);
				BlockState block = mc.world.getBlockState(bp);
			
				// every time end of weapon enters a solid for the first time, trace from our previous air position
				// and damage the block it collides with... 
				BlockRayTraceResult blockHit = mc.world.rayTraceBlocks(new RayTraceContext(lastWeaponEndAir[c], weaponEnd[c], BlockMode.OUTLINE, FluidMode.NONE, mc.player));
		
				if(block.isAir() || blockHit.getType() != Type.BLOCK || lastWeaponEndAir[c].length() == 0) { //reset				
					this.lastWeaponEndAir[c] = weaponEnd[c];
					lastWeaponSolid[c] = false;
					continue;
				}
				
				lastWeaponSolid[c] = true;

				boolean flag = blockHit.getPos().equals(bp); //fix ladder
				boolean protectedBlock = mc.vrSettings.realisticClimbEnabled && (block.getBlock() instanceof LadderBlock || block.getBlock() instanceof VineBlock);
				//TODO: maybe blacklist right-clickable blocks?
				
				if (blockHit.getType() == Type.BLOCK && flag) {
					if(canact[c] && !protectedBlock) { 
						int p = 3;
						if(item instanceof HoeItem){
							mc.physicalGuiManager.preClickAction();
							mc.playerController.func_217292_a(player, (ClientWorld) player.world, c==0 ? Hand.MAIN_HAND:Hand.OFF_HAND, blockHit);
						} else if(block.getBlock() instanceof NoteBlock) {
							mc.playerController.onPlayerDamageBlock(blockHit.getPos(), blockHit.getFace());       								
						} else{ //smack it
							p += (speed - speedthresh) / 2;

							for (int i = 0; i < p; i++)
							{
								//set delay to 0
								clearBlockHitDelay();		
								Minecraft.getInstance().physicalGuiManager.preClickAction();

								//all this comes from plaeyrControllerMP clickMouse and friends.
								//all this does is sets the block you're currently hitting, has no effect in survival mode after that.
								//but if in creative mode will clickCreative on the block
								mc.playerController.clickBlock(blockHit.getPos(), blockHit.getFace());

								if(!getIsHittingBlock()) //seems to be the only way to tell it broke.
									break;

								//apply destruction for survival only
								mc.playerController.onPlayerDamageBlock(blockHit.getPos(), blockHit.getFace());

								if(!getIsHittingBlock()) //seems to be the only way to tell it broke.
									break;

								//something effects
								mc.particles.addBlockHitEffects(blockHit.getPos(), blockHit.getFace());

							}
						}
						mc.vrPlayer.blockDust(blockHit.getHitVec().x, blockHit.getHitVec().y, blockHit.getHitVec().z, 3*p, bp, block, 0.6f, 1f);

						MCOpenVR.triggerHapticPulse(c, 250*p);
						//   System.out.println("Hit block speed =" + speed + " mot " + mot + " thresh " + speedthresh) ;            				
					}
				}
			}
		}

		mc.getProfiler().endSection();

	}

	private boolean getIsHittingBlock(){
		return (Boolean)MCReflection.PlayerController_isHittingBlock.get(Minecraft.getInstance().playerController);
	}
	
    // VIVE START - function to allow damaging blocks immediately
	private void clearBlockHitDelay() {
		MCReflection.PlayerController_blockHitDelay.set(Minecraft.getInstance().playerController, 0);
	}
	
	//Get the transparency for held items to indicate attack power or sneaking.
	public static float getItemFade(ClientPlayerEntity p, ItemStack is) {
		float fade = p.getCooledAttackStrength(0)*.75f + .25f;

		if(p.isSneaking()) 
			fade =0.75f;
	
		//see if this helps at all
		if(Minecraft.getInstance().swingTracker.lastWeaponSolid[Minecraft.getInstance().getItemRenderer().ismainhand ? 0 : 1])
			fade -= 0.25f;
		//
		
		if(is != ItemStack.EMPTY) {

			if(p.isActiveItemStackBlocking() && p.getActiveItemStack() != is) 
				fade -= 0.25f;

			if(is.getItem() == Items.SHIELD) {
				if (!p.isActiveItemStackBlocking())
					fade -= 0.25f;
			}
		}

		if(fade < 0.1) fade = 0.1f;
		if(fade > 1) fade = 1f;
		return fade;
	}
	
}

