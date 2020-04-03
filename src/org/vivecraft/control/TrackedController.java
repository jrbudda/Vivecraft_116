package org.vivecraft.control;

import org.vivecraft.provider.MCOpenVR;

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
