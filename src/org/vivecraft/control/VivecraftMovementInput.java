package org.vivecraft.control;

import org.vivecraft.gameplay.screenhandlers.KeyboardHandler;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.utils.Utils;
import org.vivecraft.utils.math.Vector2;

import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovementInput;

public class VivecraftMovementInput extends MovementInput {
	private final GameSettings gameSettings;
	private boolean autoSprintActive = false;
	private boolean movementSetByAnalog = false;
	
	public VivecraftMovementInput(GameSettings gameSettings) {
        this.gameSettings = gameSettings;
	}
	
	public static float getMovementAxisValue(KeyBinding keyBinding) {
		VRInputAction action = MCOpenVR.getInputAction(keyBinding);
		return Math.abs(MCOpenVR.getAxis1D(action));
	}

	private float axisToDigitalMovement(float value) {
    	if (value > 0.5f)
    		return 1;
    	if (value < -0.5f)
    		return -1;
    	return 0;
	}
	
	@Override //tick
	public void func_225607_a_(boolean sneaking)
	{       
		this.moveStrafe = 0.0F;
		this.moveForward = 0.0F;
		Minecraft mc = Minecraft.getInstance();

		boolean flag = false;
		if (mc.climbTracker.isClimbeyClimb() && !mc.player.isInWater() && (mc.climbTracker.isGrabbingLadder()))
			flag = true;

		if (!flag && (this.gameSettings.keyBindForward.isKeyDown() || MCOpenVR.keyTeleportFallback.isKeyDown())) {
			++this.moveForward;
			this.forwardKeyDown = true;
		} else {
			this.forwardKeyDown = false;
		}

		if (!flag && this.gameSettings.keyBindBack.isKeyDown()) {
			--this.moveForward;
			this.backKeyDown = true;
		} else {
			this.backKeyDown = false;
		}

		if (!flag && this.gameSettings.keyBindLeft.isKeyDown()) {
			++this.moveStrafe;
			this.leftKeyDown = true;
		} else {
			this.leftKeyDown = false;
		}

		if (!flag && this.gameSettings.keyBindRight.isKeyDown()) {
			--this.moveStrafe;
			this.rightKeyDown = true;
		} else {
			this.rightKeyDown = false;
		}

		boolean setMovement = false;
		float forwardAxis = 0;
		if (!flag && !mc.vrSettings.seated && mc.currentScreen == null && !KeyboardHandler.Showing){
			// override everything

			VRInputAction strafeAction = MCOpenVR.getInputAction(MCOpenVR.keyFreeMoveStrafe);
			VRInputAction rotateAction = MCOpenVR.getInputAction(MCOpenVR.keyFreeMoveRotate);
			Vector2 strafeAxis = MCOpenVR.getAxis2D(strafeAction);
			Vector2 rotateAxis = MCOpenVR.getAxis2D(rotateAction);

			if (strafeAxis.getX() != 0 || strafeAxis.getY() != 0) {
				setMovement = true;
				forwardAxis = strafeAxis.getY();
				if (mc.vrSettings.analogMovement) {
					this.moveForward = strafeAxis.getY();
					this.moveStrafe = -strafeAxis.getX();
				} else {
					this.moveForward = axisToDigitalMovement(strafeAxis.getY());
					this.moveStrafe = axisToDigitalMovement(-strafeAxis.getX());
				}
			} else if (rotateAxis.getY() != 0) {
				setMovement = true;
				forwardAxis = rotateAxis.getY();
				if (mc.vrSettings.analogMovement) {
					this.moveForward = rotateAxis.getY();

					this.moveStrafe = 0;
					this.moveStrafe -= getMovementAxisValue(this.gameSettings.keyBindRight);
					this.moveStrafe += getMovementAxisValue(this.gameSettings.keyBindLeft);
				} else {
					this.moveForward = axisToDigitalMovement(rotateAxis.getY());
				}
			} else if (mc.vrSettings.analogMovement) {
				setMovement = true;
				this.moveForward = 0;
				this.moveStrafe = 0;

				float forward = getMovementAxisValue(this.gameSettings.keyBindForward);
				if (forward == 0) forward = getMovementAxisValue(MCOpenVR.keyTeleportFallback);
				forwardAxis = forward;

				this.moveForward += forward;
				this.moveForward -= getMovementAxisValue(this.gameSettings.keyBindBack);
				this.moveStrafe -= getMovementAxisValue(this.gameSettings.keyBindRight);
				this.moveStrafe += getMovementAxisValue(this.gameSettings.keyBindLeft);

				float deadzone = 0.05f;
				this.moveForward = Utils.applyDeadzone(this.moveForward, deadzone);
				this.moveStrafe = Utils.applyDeadzone(this.moveStrafe, deadzone);
			}

			if (setMovement) {
				movementSetByAnalog = true;

				// just assuming all this below is needed for compatibility.
				this.forwardKeyDown = this.moveForward > 0;
				this.backKeyDown = this.moveForward < 0;
				this.leftKeyDown = this.moveStrafe > 0;
				this.rightKeyDown = this.moveStrafe < 0;
				VRInputAction.setKeyBindState(this.gameSettings.keyBindForward, this.forwardKeyDown);
				VRInputAction.setKeyBindState(this.gameSettings.keyBindBack, this.backKeyDown);
				VRInputAction.setKeyBindState(this.gameSettings.keyBindLeft, this.leftKeyDown);
				VRInputAction.setKeyBindState(this.gameSettings.keyBindRight, this.rightKeyDown);

				if (mc.vrSettings.autoSprint) {
					// Sprint only works for walk forwards obviously
					if (forwardAxis >= mc.vrSettings.autoSprintThreshold) {
						mc.player.setSprinting(true);
						autoSprintActive = true;
						this.moveForward = 1;
					} else if (this.moveForward > 0 && mc.vrSettings.analogMovement) {
						// Adjust range so you can still reach full speed while not sprinting
						this.moveForward = (this.moveForward / mc.vrSettings.autoSprintThreshold) * 1.0f;
					}
				}
			}
		}

		if (!setMovement) {
			if (movementSetByAnalog) {
				VRInputAction.setKeyBindState(this.gameSettings.keyBindForward, false);
				VRInputAction.setKeyBindState(this.gameSettings.keyBindBack, false);
				VRInputAction.setKeyBindState(this.gameSettings.keyBindLeft, false);
				VRInputAction.setKeyBindState(this.gameSettings.keyBindRight, false);
			}
		}
		movementSetByAnalog = setMovement;

		if (autoSprintActive && forwardAxis < mc.vrSettings.autoSprintThreshold) {
			mc.player.setSprinting(false);
			autoSprintActive = false;
		}

		boolean ok = mc.currentScreen == null && (mc.vrPlayer.getFreeMove() || mc.vrSettings.simulateFalling) && !flag;

		// VIVECRAFT DO ok.
		this.jump = this.gameSettings.keyBindJump.isKeyDown() && ok;

		this.sneaking = (mc.sneakTracker.sneakCounter > 0 || mc.sneakTracker.sneakOverride || this.gameSettings.keyBindSneak.isKeyDown())
				&& mc.currentScreen == null;


		if (sneaking)
		{
			this.moveStrafe = (float)((double)this.moveStrafe * 0.3D);
			this.moveForward = (float)((double)this.moveForward * 0.3D);

		}
	}
}
