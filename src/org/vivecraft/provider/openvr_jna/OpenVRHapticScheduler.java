package org.vivecraft.provider.openvr_jna;

import java.util.concurrent.TimeUnit;

import org.vivecraft.provider.ControllerType;
import org.vivecraft.provider.HapticScheduler;
import org.vivecraft.provider.MCVR;

import jopenvr.JOpenVRLibrary;

public class OpenVRHapticScheduler extends HapticScheduler {
	public OpenVRHapticScheduler() {
		super();
	}
	private void triggerHapticPulse(ControllerType controller, float durationSeconds, float frequency, float amplitude) {
		int error = MCOpenVR.get().vrInput.TriggerHapticVibrationAction.apply(MCOpenVR.get().getHapticHandle(controller), 0, durationSeconds, frequency, amplitude, JOpenVRLibrary.k_ulInvalidInputValueHandle);
		if (error != 0)
			System.out.println("Error triggering haptic: " + MCOpenVR.getInputErrorName(error));
	}
	public void queueHapticPulse(ControllerType controller, float durationSeconds, float frequency, float amplitude, float delaySeconds) {
		executor.schedule(() -> triggerHapticPulse(controller, durationSeconds, frequency, amplitude), (long)(delaySeconds * 1000000), TimeUnit.MICROSECONDS);
	}
}
