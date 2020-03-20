package org.vivecraft.gameplay.trackers;

import java.util.Random;

import org.vivecraft.api.NetworkHelper;
import org.vivecraft.gameplay.VRMovementStyle;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.utils.OpenVRUtil;
import org.vivecraft.utils.Quaternion;
import org.vivecraft.utils.Vector3;
import org.vivecraft.utils.Angle;
import org.vivecraft.utils.Matrix4f;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;

public class TeleportTracker extends Tracker{
    private float teleportEnergy;
    private Vec3d movementTeleportDestination = new Vec3d(0.0,0.0,0.0);
    private Direction movementTeleportDestinationSideHit;
    public double movementTeleportProgress;
    public double movementTeleportDistance;
    private Vec3d[] movementTeleportArc = new Vec3d[50];
    public int movementTeleportArcSteps = 0;
    public double lastTeleportArcDisplayOffset = 0;
    public VRMovementStyle vrMovementStyle = new VRMovementStyle();

	public TeleportTracker(Minecraft mc) {
		super(mc);
	}

	public float getTeleportEnergy () {return teleportEnergy;	}

    public boolean isAiming(){
    	return movementTeleportProgress > 0;
    }
    
    public Vec3d getDestination(){
    	return movementTeleportDestination;
    }
    
	public boolean isActive(ClientPlayerEntity p){
		if(p == null) return false;
		if(mc.playerController == null) return false;
		if(!p.isAlive()) return false;
		if(p.isSleeping()) return false;
		return true;
	}


	@Override
	public void reset(ClientPlayerEntity player) {
		movementTeleportDestination=new Vec3d(0,0,0);
		movementTeleportArcSteps = 0;
		movementTeleportProgress = 0;
	}

	public void doProcess(ClientPlayerEntity player){ //on tick


		Random rand = new Random();

        if (teleportEnergy < 100) { teleportEnergy++;}
        
        boolean doTeleport = false;
        Vec3d dest = null;

        boolean bindingTeleport = MCOpenVR.keyTeleport.isKeyDown() && mc.vrPlayer.isTeleportEnabled();
        boolean seatedTeleport = mc.vrSettings.seated && !mc.vrPlayer.getFreeMove() && (player.movementInput.moveForward != 0 || player.movementInput.moveStrafe != 0);

        if ((bindingTeleport || seatedTeleport) && !player.isPassenger())
        {
            dest = movementTeleportDestination;

            if (vrMovementStyle.teleportOnRelease)
            {
                if (player.movementTeleportTimer==0)
                {
                     String sound = vrMovementStyle.startTeleportingSound;
//                    if (sound != null)
//                    {
//                        player.playSound(SoundEvents(sound), vrMovementStyle.startTeleportingSoundVolume,
//                                1.0F / (rand.nextFloat() * 0.4F + 1.2F) + 1.0f * 0.5F);
//                    }
                }
                player.movementTeleportTimer++;
                if (player.movementTeleportTimer > 0)
                {
                    movementTeleportProgress = (float) player.movementTeleportTimer / 1.0f;
                    if (movementTeleportProgress>=1.0f)
                    {
                        movementTeleportProgress = 1.0f;
                    }

                    if (dest.x != 0 || dest.y != 0 || dest.z != 0)
                    {
                        Vec3d eyeCenterPos = mc.vrPlayer.vrdata_world_pre.hmd.getPosition();

                        // cloud of sparks moving past you
                        Vec3d motionDir = dest.add(-eyeCenterPos.x, -eyeCenterPos.y, -eyeCenterPos.z).normalize();
                        Vec3d forward;
						
						forward	= player.getLookVec();

                        Vec3d right = forward.crossProduct(new Vec3d(0, 1, 0));
                        Vec3d up = right.crossProduct(forward);

                        if (vrMovementStyle.airSparkles)
                        {
                            for (int iParticle = 0; iParticle < 3; iParticle++)
                            {
                                double forwardDist = rand.nextDouble() * 1.0 + 3.5;
                                double upDist = rand.nextDouble() * 2.5;
                                double rightDist = rand.nextDouble() * 4.0 - 2.0;

                                Vec3d sparkPos = new Vec3d(eyeCenterPos.x + forward.x * forwardDist,
                                        eyeCenterPos.y + forward.y * forwardDist,
                                        eyeCenterPos.z + forward.z * forwardDist);
                                sparkPos = sparkPos.add(right.x * rightDist, right.y * rightDist, right.z * rightDist);
                                sparkPos = sparkPos.add(up.x * upDist, up.y * upDist, up.z * upDist);

                                double speed = -0.6;
//                                EntityFX particle = new ParticleVRTeleportFX(
//                                        player.world,
//                                        sparkPos.x, sparkPos.y, sparkPos.z,
//                                        motionDir.x * speed, motionDir.y * speed, motionDir.z * speed,
//                                        1.0f);
//                                mc.effectRenderer.addEffect(particle);
                            }
                        }
                    }
                }
            }
            else
            {
                if (player.movementTeleportTimer >= 0 && (dest.x != 0 || dest.y != 0 || dest.z != 0))
                {
                    if (player.movementTeleportTimer == 0)
                    {
//                        String sound = vrMovementStyle.startTeleportingSound;
//                        if (sound != null)
//                        {
//                            player.playSound(SoundEvents.getRegisteredSoundEvent(sound), vrMovementStyle.startTeleportingSoundVolume,
//                                    1.0F / (rand.nextFloat() * 0.4F + 1.2F) + 1.0f * 0.5F);
//                        }
                    }
                    player.movementTeleportTimer++;

                    Vec3d playerPos = new Vec3d(player.posX, player.posY, player.posZ);
                    double dist = dest.distanceTo(playerPos);
                    double progress = (player.movementTeleportTimer * 1.0) / (dist + 3.0);

                    if (player.movementTeleportTimer > 0)
                    {
                        movementTeleportProgress = progress;

                        // spark at dest point
                        if (vrMovementStyle.destinationSparkles)
                        {
                          //  player.world.spawnParticle("instantSpell", dest.x, dest.y, dest.z, 0, 1.0, 0);
                        }

                        // cloud of sparks moving past you
                        Vec3d motionDir = dest.add(-player.posX, -player.posY, -player.posZ).normalize();
                        Vec3d forward = player.getLookVec();
                        Vec3d right = forward.crossProduct(new Vec3d(0, 1, 0));
                        Vec3d up = right.crossProduct(forward);

                        if (vrMovementStyle.airSparkles)
                        {
                            for (int iParticle = 0; iParticle < 3; iParticle++)
                            {
                                double forwardDist = rand.nextDouble() * 1.0 + 3.5;
                                double upDist = rand.nextDouble() * 2.5;
                                double rightDist = rand.nextDouble() * 4.0 - 2.0;
                                Vec3d sparkPos = new Vec3d(player.posX + forward.x * forwardDist,
                                        player.posY + forward.y * forwardDist,
                                        player.posZ + forward.z * forwardDist);
                                sparkPos = sparkPos.add(right.x * rightDist, right.y * rightDist, right.z * rightDist);
                                sparkPos = sparkPos.add(up.x * upDist, up.y * upDist, up.z * upDist);

                                double speed = -0.6;
//                                EntityFX particle = new ParticleVRTeleportFX(
//                                        player.world,
//                                        sparkPos.x, sparkPos.y, sparkPos.z,
//                                        motionDir.x * speed, motionDir.y * speed, motionDir.z * speed,
//                                        1.0f);
//                                mc.effectRenderer.addEffect(particle);
                            }
                        }
                    } else
                    {
                        movementTeleportProgress = 0;
                    }

                    if (progress >= 1.0)
                    {
                        doTeleport = true;
                    }
                }
            }
        }
        else //not holding down Ltrigger
        {
            if (vrMovementStyle.teleportOnRelease && movementTeleportProgress>=1.0f)
            {
                dest = movementTeleportDestination;
                doTeleport = true;
            }
            player.movementTeleportTimer = 0;
            movementTeleportProgress = 0;
        }

        if (doTeleport && dest!=null && (dest.x != 0 || dest.y !=0 || dest.z != 0)) //execute teleport
        {
            movementTeleportDistance = (float)MathHelper.sqrt(dest.squareDistanceTo(player.posX, player.posY, player.posZ));
            boolean playCustomTeleportSound = movementTeleportDistance > 0.0f && vrMovementStyle.endTeleportingSound != null;
            Block block = null;

//            if (playCustomTeleportSound)
//            {
//                String sound = vrMovementStyle.endTeleportingSound;
//                if (sound != null)
//                {
//                    player.playSound(SoundEvents.getRegisteredSoundEvent(sound), vrMovementStyle.endTeleportingSoundVolume, 1.0F);
//                }
//            }

     	   //execute teleport               
            if(!mc.vrPlayer.isTeleportSupported()){
            	String tp = "/tp " + dest.x + " " +dest.y + " " + dest.z;      
            	mc.player.sendChatMessage(tp);
            } else {          
            	if(NetworkHelper.serverSupportsDirectTeleport)	player.teleported = true;
            	player.setLocationAndAngles(dest.x, dest.y, dest.z, player.rotationYaw, player.rotationPitch);
            }

            doTeleportCallback();
            
          //  System.out.println("teleport " + dest.toString());

//            if (playCustomTeleportSound)
//            {
//                String sound = vrMovementStyle.endTeleportingSound;
//                if (sound != null)
//                {
//                    player.playSound(SoundEvents.getRegisteredSoundEvent(sound), vrMovementStyle.endTeleportingSoundVolume, 1.0F);
//                }
//            }
//            else
//            {
                mc.player.stepSound(new BlockPos(dest), dest);
//            }
        }
  
	}
	
    public void updateTeleportDestinations(GameRenderer renderer, Minecraft mc, Entity player)
    { //called every frame
        mc.getProfiler().startSection("updateTeleportDestinations");

        // no teleporting if on a server that disallows teleporting

        if (vrMovementStyle.arcAiming)
        {
            movementTeleportDestination=new Vec3d(0,0,0);

            if (movementTeleportProgress>0.0f)
            {
                updateTeleportArc(mc, player);
            }
        }
        else //non-arc modes.
        {
//            Vec3d start = mc.gameRenderer.getControllerRenderPos(1);
//            Vec3d aimDir = mc.vrPlayer.vrdata_world_render.getController(1).getDirection();
//            
//            // setup teleport forwards to the mouse cursor
//            double movementTeleportDistance = 250.0;
//            Vec3d movementTeleportPos = start.addVector(
//                    aimDir.x * movementTeleportDistance,
//                    aimDir.y * movementTeleportDistance,
//                    aimDir.z * movementTeleportDistance);
//            RayTraceResult collision = mc.world.rayTraceBlocks(start, movementTeleportPos, !mc.player.isInWater(), true, false);
//            Vec3d traceDir = start.subtract(movementTeleportPos).normalize();
//            Vec3d reverseEpsilon = new Vec3d(-traceDir.x * 0.02, -traceDir.y * 0.02, -traceDir.z * 0.02);
//
//            // don't update while charging up a teleport
//            if (movementTeleportProgress != 0)
//                return;
//
//            if (collision != null && collision.typeOfHit != Type.MISS)
//            {
//                checkAndSetTeleportDestination(mc, player, start, collision, reverseEpsilon);
//            }
        }
        mc.getProfiler().endSection();
    }

    private void updateTeleportArc(Minecraft mc, Entity player)
    {
        Vec3d start = mc.vrPlayer.vrdata_world_render.getController(1).getPosition(); //and here i was just thinking there was never a need to use the render positions for logic.
        Vec3d tiltedAim = mc.vrPlayer.vrdata_world_render.getController(1).getDirection(); 
        Matrix4f handRotation = MCOpenVR.getAimRotation(1);
        
        if(mc.vrSettings.seated){
        	start = mc.gameRenderer.getControllerRenderPos(0);
        	tiltedAim = mc.vrPlayer.vrdata_world_render.getController(0).getDirection(); 
        	handRotation =MCOpenVR.getAimRotation(0);
        }
        
        Matrix4f rot = Matrix4f.rotationY(mc.vrPlayer.vrdata_world_render.rotation_radians);
        handRotation = Matrix4f.multiply(rot, handRotation);
        
        // extract hand roll
        Quaternion handQuat = OpenVRUtil.convertMatrix4ftoRotationQuat(handRotation);
        Angle euler = handQuat.toEuler();
        //TODO: use vrdata for this
        
        int maxSteps = 50;
        movementTeleportArc[0] = new Vec3d(
        		start.x,
        		start.y,
        		start.z);
        
        movementTeleportArcSteps = 1;

        // calculate gravity vector for arc
        float gravityAcceleration = 0.098f;
        Matrix4f rollCounter = OpenVRUtil.rotationZMatrix((float)Math.toRadians(-euler.getRoll()));
        Matrix4f gravityTilt = OpenVRUtil.rotationXMatrix((float)Math.PI * -.8f);
        Matrix4f gravityRotation = Matrix4f.multiply(handRotation, rollCounter);
        
        Vector3 forward = new Vector3(0,1,0);
        Vector3 gravityDirection = gravityRotation.transform(forward);
        Vec3d gravity = gravityDirection.negate().toVec3d();
        
        gravity = gravity.scale(gravityAcceleration);

        
     //   gravity.rotateAroundY(this.worldRotationRadians);

        // calculate initial move step	
        float speed = 0.5f;
        Vec3d velocity = new Vec3d(
                tiltedAim.x * speed,
                tiltedAim.y * speed,
                tiltedAim.z * speed);

        Vec3d pos = new Vec3d(start.x, start.y, start.z);
        Vec3d newPos;

        // trace arc
        for (int i=movementTeleportArcSteps;i<maxSteps;i++)
        {
        	if (i*4 > teleportEnergy) {
        		break;
        		}
        	newPos = new Vec3d(
            pos.x + velocity.x,
            pos.y + velocity.y,
            pos.z + velocity.z);

      	
            boolean	water =false;
            if(mc.vrSettings.seated )
            	water = mc.gameRenderer.inwater;
            else{
            	water = !mc.world.getFluidState(new BlockPos(start)).isEmpty();
            }
        	
            //bool params are 'checkcollision' and 'return misses'
         //   RayTraceResult collision = TPrayTraceBlocks(mc.world, pos, newPos, water ? RayTraceContext.FluidMode.NONE : RayTraceContext.FluidMode.ANY , true, false) ;
			BlockRayTraceResult collision = 
					mc.world.rayTraceBlocks(
					new RayTraceContext(
							pos,
							newPos,
							RayTraceContext.BlockMode.COLLIDER, water ? RayTraceContext.FluidMode.ANY : RayTraceContext.FluidMode.ANY,
							mc.player));
            
            if (collision != null && collision.getType() != Type.MISS)
            {
        		
                movementTeleportArc[i] = collision.getHitVec();

                movementTeleportArcSteps = i + 1;

                Vec3d traceDir = pos.subtract(newPos).normalize();
                Vec3d reverseEpsilon = new Vec3d(-traceDir.x * 0.02, -traceDir.y * 0.02, -traceDir.z * 0.02);

                checkAndSetTeleportDestination(mc, player, start, collision, reverseEpsilon);
                                        
    			Vec3d diff = mc.player.getPositionVector().subtract(movementTeleportDestination);
       
        		double yDiff = diff.y;
        		movementTeleportDistance = diff.length();
        		double xzdiff = Math.sqrt(diff.x * diff.x + diff.z*diff.z);
        		
        		boolean ok = true;
        		
            	if(mc.player.isSneaking()) {
            		if(yDiff > 0.2)
            			ok = false;
            	}        	

            	if (!mc.player.abilities.allowFlying && NetworkHelper.isLimitedSurvivalTeleport()) { //survival mode mode
        			if(NetworkHelper.getTeleportDownLimit() > 0 && yDiff > NetworkHelper.getTeleportDownLimit() + 0.2)
        	    		ok = false;
        			else if(NetworkHelper.getTeleportUpLimit() > 0 && -yDiff > NetworkHelper.getTeleportUpLimit() + 0.2)
        	    		ok = false;  			
        			else if(NetworkHelper.getTeleportHorizLimit() > 0 && xzdiff > NetworkHelper.getTeleportHorizLimit() + 0.2)
        	    		ok = false;
            	}
                
            	if(!ok) { //u fail.
            		movementTeleportDestination = new Vec3d(0, 0, 0);
            		movementTeleportDistance = 0;
            	}
            	
                break;
            }

            pos = new Vec3d(newPos.x, newPos.y, newPos.z);


            movementTeleportArc[i] = new Vec3d(
            		newPos.x,
            		newPos.y,
            		newPos.z);

            movementTeleportArcSteps = i + 1;

            velocity = velocity.add(gravity);

        }
    }

    
    private void doTeleportCallback(){ //not really a callback anymore, is it?
        Minecraft mc = Minecraft.getInstance();

        mc.swingTracker.disableSwing = 3;

        if(NetworkHelper.isLimitedSurvivalTeleport()){
          mc.player.addExhaustion((float) (movementTeleportDistance / 16 * 1.2f));    
          
          if (mc.playerController.isNotCreative() && vrMovementStyle.arcAiming){
          	teleportEnergy -= movementTeleportDistance * 4;	
          }       
        }
        
        mc.player.fallDistance = 0.0F;

        mc.player.movementTeleportTimer = -1;
        
    }
	
    // look for a valid place to stand on the block that the trace collided with
    private boolean checkAndSetTeleportDestination(Minecraft mc, Entity player, Vec3d start, BlockRayTraceResult collision, Vec3d reverseEpsilon)
    {

    	BlockPos bp = ((BlockRayTraceResult)collision).getPos();

    	BlockState testClimb = player.world.getBlockState(bp); 	
    	
    	if (!mc.world.getFluidState(bp).isEmpty()){
    		Vec3d hitVec = new Vec3d(collision.getHitVec().x, bp.getY(), collision.getHitVec().z );

    		Vec3d offset = hitVec.subtract(player.posX, player.getBoundingBox().minY, player.posZ);
    		AxisAlignedBB bb = player.getBoundingBox().offset(offset.x, offset.y, offset.z);
    		boolean emptySpotReq = mc.world.isCollisionBoxesEmpty(player,bb);

    		if(!emptySpotReq){
    			Vec3d center = new Vec3d(bp).add(0.5, 0, 0.5);
    			offset = center.subtract(player.posX, player.getBoundingBox().minY, player.posZ);
    			bb = player.getBoundingBox().offset(offset.x, offset.y, offset.z);
    			emptySpotReq = mc.world.isCollisionBoxesEmpty(player,bb);	
    		}
    		float ex = 0;
    		if(mc.vrSettings.seated)ex = 0.5f;
    		if(emptySpotReq){
    			movementTeleportDestination = new Vec3d(bb.getCenter().x,bb.minY+ex, bb.getCenter().z);
    			movementTeleportDestinationSideHit = collision.getFace();
    			return true;
    		}

    	} else if (collision.getFace() != Direction.UP) 
    	{ //sides  		    		
    		//jrbudda require arc hitting top of block.	unless ladder or vine or creative or limits off.

    		if (testClimb.getBlock() instanceof LadderBlock|| testClimb.getBlock() instanceof VineBlock) {
    			Vec3d dest = new Vec3d(bp.getX()+0.5, bp.getY() + 0.5, bp.getZ()+0.5);

    			Block playerblock = mc.world.getBlockState(bp.down()).getBlock();
    			if(playerblock == testClimb.getBlock()) dest = dest.add(0,-1,0);

    			movementTeleportDestination = dest.scale(1);
    			movementTeleportDestinationSideHit = collision.getFace();
    			
    			return true; //really should check if the block above is passable. Maybe later.
    		} else {
    			if (!mc.player.abilities.allowFlying && NetworkHelper.isLimitedSurvivalTeleport())
    				return false; //if creative, check if can hop on top.
    		}
    	}

    	double y = 0;
    	BlockPos hitBlock = collision.getPos().down();

    	for(int k = 0; k<2; k++){

    		testClimb = player.world.getBlockState(hitBlock);
    		if (testClimb.getCollisionShape(mc.world, hitBlock).isEmpty()){
    			hitBlock = hitBlock.up();
    			continue;
    		}
    		
    		double height = testClimb.getCollisionShape(mc.world, hitBlock).getEnd(Axis.Y);
    		
    		Vec3d hitVec = new Vec3d(collision.getHitVec().x, hitBlock.getY() + height, collision.getHitVec().z );
    		Vec3d offset = hitVec.subtract(player.posX, player.getBoundingBox().minY, player.posZ);
    		AxisAlignedBB bb = player.getBoundingBox().offset(offset.x, offset.y, offset.z);
    		double ex = 0;
    		if (testClimb.getBlock() == Blocks.SOUL_SAND) ex = 0.05;

    		boolean emptySpotReq = mc.world.isCollisionBoxesEmpty(player,bb) &&
    				!mc.world.isCollisionBoxesEmpty(player,bb.grow(0, .125 + ex, 0));     

    		if(!emptySpotReq){
    			Vec3d center = new Vec3d(hitBlock).add(0.5, height, 0.5);
    			offset = center.subtract(player.posX, player.getBoundingBox().minY, player.posZ);
    			bb = player.getBoundingBox().offset(offset.x, offset.y, offset.z);
    			emptySpotReq = mc.world.isCollisionBoxesEmpty(player,bb) &&
    					!mc.world.isCollisionBoxesEmpty(player,bb.grow(0, .125 + ex, 0));     	
    		}

    		if(emptySpotReq){
    			Vec3d dest = new Vec3d(bb.getCenter().x, hitBlock.getY() + height, bb.getCenter().z);

    			movementTeleportDestination = dest.scale(1);
    			
    			return true;
    		}

    		hitBlock = hitBlock.up();
    	}

    	return false;
    }

    // rough interpolation between arc locations
    public Vec3d getInterpolatedArcPosition(float progress)
    {
        // not enough points to interpolate or before start
        if (movementTeleportArcSteps == 1 || progress <= 0.0f)
        {
            return new Vec3d(
                    movementTeleportArc[0].x,
                    movementTeleportArc[0].y,
                    movementTeleportArc[0].z);
        }

        // past end of arc
        if (progress>=1.0f)
        {
            return new Vec3d(
                    movementTeleportArc[movementTeleportArcSteps-1].x,
                    movementTeleportArc[movementTeleportArcSteps-1].y,
                    movementTeleportArc[movementTeleportArcSteps-1].z);
        }

        // which two points are we between?
        float stepFloat = progress * (float)(movementTeleportArcSteps - 1);
        int step = (int) Math.floor(stepFloat);

        double deltaX = movementTeleportArc[step+1].x - movementTeleportArc[step].x;
        double deltaY = movementTeleportArc[step+1].y - movementTeleportArc[step].y;
        double deltaZ = movementTeleportArc[step+1].z - movementTeleportArc[step].z;

        float stepProgress = stepFloat - step;

        return new Vec3d(
                movementTeleportArc[step].x + deltaX * stepProgress,
                movementTeleportArc[step].y + deltaY * stepProgress,
                movementTeleportArc[step].z + deltaZ * stepProgress);
    }

}

