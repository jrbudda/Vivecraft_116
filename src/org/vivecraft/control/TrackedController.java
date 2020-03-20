package org.vivecraft.control;

import java.util.List;

import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.utils.Vector2;
import org.vivecraft.utils.Vector3;

import jopenvr.VRControllerAxis_t;
import jopenvr.VRControllerState_t;
import net.minecraft.client.Minecraft;

public class TrackedController {
	protected final ControllerType type;
	
	public TrackedController(ControllerType type) {
		this.type = type;
	}
	
	public int getDeviceIndex() {
		return MCOpenVR.controllerDeviceIndex[type.ordinal()];
	}
	
	public ControllerType getType() {
		return type;
	}
	
	public boolean isTracking() {
		return MCOpenVR.isControllerTracking(type);
	}

	public void triggerHapticPulse(float durationSeconds, float frequency, float amplitude) {
		MCOpenVR.triggerHapticPulse(this.type, durationSeconds, frequency, amplitude);
	}
	
	@Deprecated
	public void triggerHapticPulse(int duration) {
		MCOpenVR.triggerHapticPulse(this.type, duration);
	}
}
