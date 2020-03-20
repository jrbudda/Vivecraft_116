package org.vivecraft.gameplay.trackers;

import org.vivecraft.gameplay.OpenVRPlayer;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.settings.VRSettings;
import org.vivecraft.utils.Quaternion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class RowTracker extends Tracker{

	public RowTracker(Minecraft mc) {
		super(mc);
	}

	public boolean isActive(ClientPlayerEntity p){
		if(Minecraft.getInstance().vrSettings.seated)
			return false;
		if(!Minecraft.getInstance().vrSettings.realisticRowEnabled)
			return false;
		if(p==null || !p.isAlive())
			return false;
		if(mc.playerController == null) return false;
		if(Minecraft.getInstance().gameSettings.keyBindForward.isKeyDown()) //important
			return false;
		if(!(p.getRidingEntity() instanceof BoatEntity))
			return false;
		if(Minecraft.getInstance().bowTracker.isNotched())
			return false;
		return true;
	}


	Vec3d[] lastUWPs=new Vec3d[2];
	public double[] forces= new double[]{0,0};

	double transmissionEfficiency=0.9;
	
	public boolean isRowing(){
		return ROar + LOar + Foar > 0;
	}
	
	public float LOar, ROar, Foar;
	
	@Override
	public void reset(ClientPlayerEntity player) {
		LOar = 0;
		ROar = 0;
		Foar = 0;
	}
	
	@Override
	public void doProcess (ClientPlayerEntity player){
		double c0move = MCOpenVR.controllerHistory[0].averageSpeed(0.5);
		double c1move = MCOpenVR.controllerHistory[1].averageSpeed(0.5);

		float minspeed = 0.5f;
		float maxspeed = 2;

		ROar = (float) Math.max(c0move - minspeed,0);
		LOar = (float) Math.max(c1move - minspeed,0);
		Foar = ROar > 0 && LOar > 0 ? (ROar + LOar) / 2 : 0;
		if(Foar > maxspeed) Foar = maxspeed;
		if(ROar > maxspeed) ROar = maxspeed;
		if(LOar > maxspeed) LOar = maxspeed;

		//TODO: Backwards paddlin'	
	}


	public void doProcessFinaltransmithastofixthis(ClientPlayerEntity player){

		BoatEntity boat=(BoatEntity) player.getRidingEntity();
		Quaternion boatRot = new Quaternion(boat.rotationPitch, -(boat.rotationYaw % 360f), 0).normalized();


		for (int paddle = 0; paddle <= 1 ; paddle++) {
			if(isPaddleUnderWater(paddle,boat)){
				Vec3d arm2Pad=getArmToPaddleVector(paddle,boat);
				Vec3d attach=getAttachmentPoint(paddle,boat);

				Vec3d underWaterPoint=attach.add(arm2Pad.normalize()).subtract(boat.getPositionVector());


				if(lastUWPs[paddle]!=null){
					Vec3d forceVector=lastUWPs[paddle].subtract(underWaterPoint); //intentionally reverse
					forceVector=forceVector.subtract(boat.getMotion());
					Vec3d forward= boatRot.multiply(new Vec3d(0,0,1));

					//scalar projection onto forward vector
					double force=forceVector.dotProduct(forward)*transmissionEfficiency/5;

					if ((force<0 && forces[paddle]>0) || (force>0 && forces[paddle]<0)){
						forces[paddle]=0;
					}else {
						forces[paddle] = Math.min(Math.max(force,-0.1),0.1);
					}
				}
				lastUWPs[paddle]=underWaterPoint;
			}else{
				forces[paddle]=0;
				lastUWPs[paddle]=null;
			}
		}
		
	}

	Vec3d getArmToPaddleVector(int paddle, BoatEntity boat){

		Vec3d attachAbs=getAttachmentPoint(paddle,boat);
		Vec3d armAbs = getAbsArmPos(paddle==0? 1 : 0);

		return attachAbs.subtract(armAbs);
	}


	Vec3d getAttachmentPoint(int paddle, BoatEntity boat){
		Vec3d attachmentPoint = new Vec3d((paddle==0? 9f: -9f) / 16f, (-5 + 15) / 16f, 3 / 16f); //values from ModelBoat
		Quaternion boatRot = new Quaternion(boat.rotationPitch, -(boat.rotationYaw % 360f), 0).normalized();

		return boat.getPositionVector().add(boatRot.multiply(attachmentPoint));
	}

	Vec3d getAbsArmPos(int side){
		Vec3d arm = MCOpenVR.controllerHistory[side].averagePosition(0.1);
		Quaternion worldRot = new Quaternion(0, VRSettings.inst.vrWorldRotation, 0);

		return OpenVRPlayer.get().roomOrigin.add(worldRot.multiply(arm));
	}

	boolean isPaddleUnderWater(int paddle, BoatEntity boat){

		Vec3d attachAbs=getAttachmentPoint(paddle,boat);
		Vec3d armToPaddle = getArmToPaddleVector(paddle,boat).normalize();

		BlockPos blockPos=new BlockPos(attachAbs.add(armToPaddle));

		return boat.world.getBlockState(blockPos).getMaterial().isLiquid();
	}

}
