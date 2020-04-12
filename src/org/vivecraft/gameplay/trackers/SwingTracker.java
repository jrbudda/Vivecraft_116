package org.vivecraft.gameplay.trackers;

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
import net.minecraft.item.CarrotOnAStickItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import net.minecraft.util.math.Vec3d;

public class SwingTracker extends Tracker{

	//VIVECRAFT SWINGING SUPPORT
	private Vec3d[] lastWeaponEndAir = new Vec3d[]{new Vec3d(0, 0, 0), new Vec3d(0,0,0)};
	private boolean[] lastWeaponSolid = new boolean[2];
	private Vec3d[] weaponEnd= new Vec3d[2];
	private Vec3d[] weaponEndlast= new Vec3d[]{new Vec3d(0, 0, 0), new Vec3d(0,0,0)};

	public boolean[] shouldIlookatMyHand= new boolean[2];
	public boolean[] IAmLookingAtMyHand= new boolean[2];

	public int disableSwing = 3;

	public SwingTracker(Minecraft mc) {
		super(mc);
	}

	public boolean isActive(ClientPlayerEntity p){
		if(mc.playerController == null) return false;
		if(p == null) return false;
		if(!p.isAlive()) return false;
		if(p.isSleeping()) return false;
		Minecraft mc = Minecraft.getInstance();
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
				item instanceof CarrotOnAStickItem ||
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

	
	public void doProcess(ClientPlayerEntity player){ //on tick
       
        mc.getProfiler().startSection("updateSwingAttack");
        
        Vec3d forward = new Vec3d(0,0,-1);
        
        for(int c =0 ;c<2;c++){
        	
        	if (mc.climbTracker.isGrabbingLadder(c)) continue;
        	
        	Vec3d handPos = mc.vrPlayer.vrdata_world_pre.getController(c).getPosition();
        	Vec3d handDirection = mc.vrPlayer.vrdata_world_pre.getHand(c).getCustomVector(forward);

        	ItemStack is = player.getHeldItem(c==0?Hand.MAIN_HAND:Hand.OFF_HAND);
        	Item item = null;

        	double speedthresh = 1.8f;
        	float weaponLength;
        	float entityReachAdd;

        	if(is!=null )item = is.getItem();

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
            	entityReachAdd = 1.f;
            	weaponLength = 0.5f;
        		tool = true;
            } else if (item !=null){
            	weaponLength = 0.1f;
            	entityReachAdd = 0.3f;
            } else {
            	weaponLength = 0.0f;
            	entityReachAdd = 0.3f;
            }

        	weaponLength *= mc.vrPlayer.vrdata_world_pre.worldScale;

        	weaponEnd[c] = new Vec3d(
        			handPos.x + handDirection.x * weaponLength,
        			handPos.y + handDirection.y * weaponLength,
        			handPos.z + handDirection.z * weaponLength);     

        	if (disableSwing > 0 ) {
        		disableSwing--;
        		if(disableSwing<0)disableSwing = 0;
        		weaponEndlast[c] = new Vec3d(weaponEnd[c].x,	 weaponEnd[c].y, 	 weaponEnd[c].z);
        		return;
        	}

        	float speed = (float) MCOpenVR.controllerHistory[c].averageSpeed(0.1);

        	weaponEndlast[c] = new Vec3d(weaponEnd[c].x, weaponEnd[c].y, weaponEnd[c].z);

        	boolean inAnEntity = false;
        	boolean insolidBlock = false;
        	boolean canact = speed > speedthresh && !lastWeaponSolid[c];

        	//Check EntityCollisions first    	
        	{// normal reach check.
            	AxisAlignedBB weaponBB = new AxisAlignedBB(
            			Math.min(weaponEnd[c].x, handPos.x),
            			Math.min(weaponEnd[c].y, handPos.y),
            			Math.min(weaponEnd[c].z, handPos.z),
            			Math.max(weaponEnd[c].x, handPos.x),
            			Math.max(weaponEnd[c].y, handPos.y),
            			Math.max(weaponEnd[c].z, handPos.z));
        		List entities = mc.world.getEntitiesWithinAABBExcludingEntity(
        				mc.player, weaponBB);       	
        		for (int e = 0; e < entities.size(); ++e)
        		{
        			Entity hitEntity = (Entity) entities.get(e);
        			if (hitEntity.canBeCollidedWith() && !(hitEntity == mc.getRenderViewEntity().getRidingEntity()) )
        			{       			       			
        				if(canact){
        					Minecraft.getInstance().physicalGuiManager.preClickAction();
        					mc.playerController.attackEntity(player, hitEntity);
        					MCOpenVR.triggerHapticPulse(c, 1000);
        					lastWeaponSolid[c] = true;
        				}
        				inAnEntity = true;
        				break;
        			}
        		}     
        	}
        	
        	if(!inAnEntity) { //extended check vs non players.   
        		
        		Vec3d extWeapon = new Vec3d(
    			handPos.x + handDirection.x * (weaponLength + entityReachAdd),
    			handPos.y + handDirection.y * (weaponLength + entityReachAdd),
    			handPos.z + handDirection.z * (weaponLength + entityReachAdd));
        		
            	AxisAlignedBB weaponBBEXT = new AxisAlignedBB(
            			handPos.x < extWeapon.x ? handPos.x : extWeapon.x  ,
            					handPos.y < extWeapon.y ? handPos.y : extWeapon.y  ,
            							handPos.z < extWeapon.z ? handPos.z : extWeapon.z  ,
            									handPos.x > extWeapon.x ? handPos.x : extWeapon.x  ,
            											handPos.y > extWeapon.y ? handPos.y : extWeapon.y  ,
            													handPos.z > extWeapon.z ? handPos.z : extWeapon.z  
            			);
            	
        		List entities = mc.world.getEntitiesWithinAABBExcludingEntity(
        				mc.player, weaponBBEXT);       	
        		for (int e = 0; e < entities.size(); ++e)
        		{
        			Entity hitEntity = (Entity) entities.get(e);
        			if((hitEntity instanceof PlayerEntity)) continue; 
        			if (hitEntity.canBeCollidedWith() && !(hitEntity == mc.getRenderViewEntity().getRidingEntity()) )
        			{       			       			
        				if(canact){
        					Minecraft.getInstance().physicalGuiManager.preClickAction();
        					mc.playerController.attackEntity(player, hitEntity);
        					MCOpenVR.triggerHapticPulse(c, 1000);
        					lastWeaponSolid[c] = true;
        				}
        				inAnEntity = true;
        				break;
        			}    		
        		}
        	}
        	
        	if(!inAnEntity && !sword){ //block check
        		            	
        		if(mc.climbTracker.isClimbeyClimb()){
        			if(c == 0 && MCOpenVR.keyClimbeyGrab.isKeyDown(ControllerType.RIGHT) || !tool ) continue;
        			if(c == 1 && MCOpenVR.keyClimbeyGrab.isKeyDown(ControllerType.LEFT) || !tool ) continue;
        		}

        		BlockPos bp =null;
        		bp = new BlockPos(weaponEnd[c]);
        		BlockState block = mc.world.getBlockState(bp);
        		Material material = block.getMaterial();

        		// every time end of weapon enters a solid for the first time, trace from our previous air position
        		// and damage the block it collides with... 
        		AxisAlignedBB axisalignedbb = player.getBoundingBox().grow(4.0D, 4.0D, 4.0D);


        		BlockRayTraceResult blockHit = mc.world.rayTraceBlocks(new RayTraceContext(lastWeaponEndAir[c], weaponEnd[c], BlockMode.OUTLINE, FluidMode.NONE, mc.player));

        		EntityRayTraceResult entityHit = ProjectileHelper.rayTraceEntities(player, lastWeaponEndAir[c], weaponEnd[c], axisalignedbb, (p_lambda$getMouseOver$0_0_) ->
        		{
        			return !p_lambda$getMouseOver$0_0_.isSpectator() && p_lambda$getMouseOver$0_0_.canBeCollidedWith();
        		}, 16);   

        		if (blockHit.getType() == Type.BLOCK) {
        			boolean flag = blockHit.getPos().equals(bp); //fix ladder but prolly break everything else.

        			if (flag)
        			{
        				this.shouldIlookatMyHand[c] = false;
        				if (!(material == material.AIR))
        				{
        					if (block.getMaterial().isLiquid()) {
        						if(item == Items.BUCKET) {       						
        							//mc.playerController.onPlayerRightClick(player, player.world,is, col.blockX, col.blockY, col.blockZ, col.sideHit,col.hitVec);
        							this.shouldIlookatMyHand[c] = true;
        							if (IAmLookingAtMyHand[c]){
        								Minecraft.getInstance().physicalGuiManager.preClickAction();
        								if(	Minecraft.getInstance().playerController.processRightClick(player, player.world,c==0?Hand.MAIN_HAND:Hand.OFF_HAND).isSuccessOrConsume()){
        									mc.gameRenderer.itemRenderer.resetEquippedProgress(c==0?Hand.MAIN_HAND:Hand.OFF_HAND);					
        								}
        							}
        						}
        					} else {
        						if(canact && (!mc.vrSettings.realisticClimbEnabled || (!(block.getBlock() instanceof LadderBlock) && !(block.getBlock() instanceof VineBlock)))) { 
        							int p = 3;
        							if(item instanceof HoeItem){
        								mc.physicalGuiManager.preClickAction();
        								mc.playerController.processRightClickBlock(player, (ClientWorld) player.world, c==0 ? Hand.MAIN_HAND:Hand.OFF_HAND, blockHit);
        								
        							} else if(block.getBlock() instanceof NoteBlock) {
    									mc.playerController.onPlayerDamageBlock(blockHit.getPos(), blockHit.getFace());       								
        							} else{
        								p += (speed - speedthresh) / 2;

        								for (int i = 0; i < p; i++)
        								{
        									//set delay to 0
        									clearBlockHitDelay();			
        									boolean test = mc.climbTracker.isGrabbingLadder();
        									//all this comes from plaeyrControllerMP clickMouse and friends.

        									Minecraft.getInstance().physicalGuiManager.preClickAction();
        									//all this does is sets the block you're currently hitting, has no effect in survival mode after that.
        									//but if in creaive mode will clickCreative on the block
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
        							lastWeaponSolid[c] = true;
        						}
        						insolidBlock = true;
        					}
        				}
        			}
        			//mc.playerController.hitVecOverride = null;
        		}
        	}
        	
            if ((!inAnEntity && !insolidBlock ) || lastWeaponEndAir[c].length() ==0)
        	{
        		this.lastWeaponEndAir[c] = new Vec3d(
        				weaponEnd[c].x,
        				weaponEnd[c].y,
        				weaponEnd[c].z
        				);
        		lastWeaponSolid[c] = false;
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
    	
      	if(p.isShiftKeyDown()) 
          	fade =0.75f;
          
          if(p.isActiveItemStackBlocking() && p.getActiveItemStack() != is) 
          	fade =0.75f;

          if(is.getItem() == Items.SHIELD) {
              if (p.isActiveItemStackBlocking())
                  fade = 1;
              else
                  fade = 0.75f;
          }

          if(fade < 0.1) fade = 0.1f;
          if(fade > 1) fade = 1f;
          return fade;
	}
	
}

