package org.vivecraft.gameplay.trackers;

import org.vivecraft.gameplay.OpenVRPlayer;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.settings.VRSettings;
import org.vivecraft.utils.Quaternion;
import org.vivecraft.utils.Utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.util.math.Vec3d;

public class HorseTracker extends Tracker {


	public HorseTracker(Minecraft mc) {
		super(mc);
	}

	@Override
	public boolean isActive(ClientPlayerEntity p) {
		if(true) return false;
		if (Minecraft.getInstance().vrSettings.seated)
			return false;
		if (p == null || !p.isAlive())
			return false;
		if(mc.playerController == null) return false;
		if (Minecraft.getInstance().gameSettings.keyBindForward.isKeyDown())
			return false;
		if (!(p.getRidingEntity() instanceof AbstractHorseEntity))
			return false;
		if (Minecraft.getInstance().bowTracker.isNotched())
			return false;

		return true;
	}

	double boostTrigger = 1.4;
	double pullTrigger = 0.8;

	int speedLevel = 0;
	int maxSpeedLevel = 3;

	int coolDownMillis = 500;
	long lastBoostMillis = -1;

	double turnspeed = 6;
	double bodyturnspeed = 0.2;
	double baseSpeed = 0.2;

	HorseEntity horse =null;
	@Override
	public void reset(ClientPlayerEntity player) {
		super.reset(player);
		if (horse!=null)
			horse.setNoAI(false);
	}

	@Override
	public void doProcess(ClientPlayerEntity player) {
		horse = (HorseEntity) player.getRidingEntity();
		horse.setNoAI(true);
		float absYaw = (horse.rotationYaw + 360) % 360;
		float absYawOffset = (horse.renderYawOffset + 360) % 360;

		Vec3d speedLeft = MCOpenVR.controllerHistory[1].netMovement(0.1).scale(1 / 0.1);
		Vec3d speedRight = MCOpenVR.controllerHistory[0].netMovement(0.1).scale(1 / 0.1);
		double speedDown = Math.min(-speedLeft.y, -speedRight.y);

		if (speedDown > boostTrigger) {
			boost();
		}

		Quaternion horseRot = new Quaternion(0, -horse.renderYawOffset, 0);
		Vec3d back = horseRot.multiply(new Vec3d(0, 0, -1));
		Vec3d left = horseRot.multiply(new Vec3d(1, 0, 0));
		Vec3d right = horseRot.multiply(new Vec3d(-1, 0, 0));

		Quaternion worldRot = new Quaternion(0, VRSettings.inst.vrWorldRotation, 0);

		Vec3d posL = OpenVRPlayer.get().roomOrigin.add(worldRot.multiply(MCOpenVR.controllerHistory[1].latest()));
		Vec3d posR = OpenVRPlayer.get().roomOrigin.add(worldRot.multiply(MCOpenVR.controllerHistory[0].latest()));

		double distanceL = posL.subtract(info.leftReinPos).dotProduct(back) + posL.subtract(info.leftReinPos).dotProduct(left);
		double distanceR = posR.subtract(info.rightReinPos).dotProduct(back) + posR.subtract(info.rightReinPos).dotProduct(right);

		if (speedLevel<0)
			speedLevel=0;

		if (distanceL > pullTrigger + 0.3 && distanceR > pullTrigger + 0.3 && Math.abs(distanceR - distanceL) < 0.1) {
			if (speedLevel <= 0 && System.currentTimeMillis() > lastBoostMillis + coolDownMillis) {
				speedLevel=-1;
			} else {
				doBreak();
			}
		} else {
			double pullL = 0, pullR = 0;
			if (distanceL > pullTrigger) {
				pullL = (distanceL - pullTrigger);

			}
			if (distanceR > pullTrigger) {
				pullR = distanceR - pullTrigger;
			}
			horse.rotationYaw = (float) (absYaw + (pullR - pullL) * turnspeed);
		}



	horse.renderYawOffset=(float)Utils.lerpMod(absYawOffset,absYaw,bodyturnspeed,360);
	horse.rotationYawHead=absYaw;

	Vec3d movement = horseRot.multiply(new Vec3d(0, 0, speedLevel * baseSpeed));

	horse.setMotion(movement.x, horse.getMotion().y, movement.z);
}

	boolean boost() {
		if (speedLevel >= maxSpeedLevel) {
			return false;
		}
		if (System.currentTimeMillis() < lastBoostMillis + coolDownMillis) {
			return false;
		}

		System.out.println("Boost");
		speedLevel++;
		lastBoostMillis = System.currentTimeMillis();
		return true;
	}

	boolean doBreak() {
		if (speedLevel <= 0) {
			return false;
		}
		if (System.currentTimeMillis() < lastBoostMillis + coolDownMillis) {
			return false;
		}
		System.out.println("Breaking");

		speedLevel--;
		lastBoostMillis = System.currentTimeMillis();
		return true;
	}

	ModelInfo info = new ModelInfo();

	public ModelInfo getModelInfo() {
		return info;
	}

public class ModelInfo {
	public Vec3d leftReinPos = Vec3d.ZERO;
	public Vec3d rightReinPos = Vec3d.ZERO;
}
}
