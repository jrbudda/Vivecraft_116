package org.vivecraft.control;

import org.vivecraft.provider.MCOpenVR;

public enum ControllerType {
	RIGHT,
	LEFT;
	
	public TrackedController getController() {
		return MCOpenVR.controllers[this.ordinal()];
	}
}
