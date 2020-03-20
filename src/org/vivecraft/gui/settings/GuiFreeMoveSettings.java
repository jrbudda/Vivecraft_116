package org.vivecraft.gui.settings;

import org.vivecraft.gui.framework.GuiVROptionButton;
import org.vivecraft.gui.framework.GuiVROptionsBase;
import org.vivecraft.settings.VRSettings;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;

public class GuiFreeMoveSettings extends GuiVROptionsBase {
	private static VRSettings.VrOptions[] standingSettings = new VRSettings.VrOptions[] {
			VRSettings.VrOptions.FREEMOVE_MODE,
			VRSettings.VrOptions.FOV_REDUCTION,
			VRSettings.VrOptions.INERTIA_FACTOR,
			VRSettings.VrOptions.MOVEMENT_MULTIPLIER,
			VRSettings.VrOptions.AUTO_SPRINT,
			VRSettings.VrOptions.AUTO_SPRINT_THRESHOLD,
			VRSettings.VrOptions.ANALOG_MOVEMENT
	};

	private static VRSettings.VrOptions[] seatedSettings = new VRSettings.VrOptions[] {
			VRSettings.VrOptions.SEATED_HMD,
			VRSettings.VrOptions.FOV_REDUCTION
	};
	
	private static VRSettings.VrOptions[] fovRed = new VRSettings.VrOptions[] {
			VRSettings.VrOptions.FOV_REDUCTION_MIN,
			VRSettings.VrOptions.FOV_REDUCTION_OFFSET
	};

	public GuiFreeMoveSettings(Screen guiScreen) {
		super(guiScreen);
	}

	@Override
	public void init()
	{
		vrTitle = "Free Move Settings";

		if (minecraft.vrSettings.seated)
			super.init(seatedSettings, true);
		else
			super.init(standingSettings, true);

		if(minecraft.vrSettings.useFOVReduction)
			super.init(fovRed,false);
		
		super.addDefaultButtons();
	}

	@Override
	protected void loadDefaults() {
		VRSettings vrSettings = minecraft.vrSettings;
		vrSettings.inertiaFactor = VRSettings.INERTIA_NORMAL;
		vrSettings.movementSpeedMultiplier = 1f;
		vrSettings.vrFreeMoveMode = VRSettings.FREEMOVE_CONTROLLER;
		vrSettings.useFOVReduction = false;
		vrSettings.fovReductionMin = 0.25f;
		vrSettings.fovRedutioncOffset = 0.1f;
		vrSettings.seatedUseHMD = false;
		vrSettings.analogMovement = true;
		vrSettings.autoSprint = true;
		vrSettings.autoSprintThreshold = 0.9f;
	}
	
	@Override
	protected void actionPerformed(Widget widget) {
		if(!(widget instanceof GuiVROptionButton)) return;
		GuiVROptionButton button = (GuiVROptionButton) widget;
		if (button.id == VRSettings.VrOptions.FOV_REDUCTION.ordinal())
			this.reinit = true;
	}
}
