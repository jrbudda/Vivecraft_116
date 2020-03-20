package org.vivecraft.gui.settings;

import org.vivecraft.gui.framework.GuiVROptionButton;
import org.vivecraft.gui.framework.GuiVROptionsBase;
import org.vivecraft.settings.VRSettings;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;

public class GuiTeleportSettings extends GuiVROptionsBase {
	private static VRSettings.VrOptions[] teleportSettings = new VRSettings.VrOptions[] {
			VRSettings.VrOptions.SIMULATE_FALLING,
			VRSettings.VrOptions.LIMIT_TELEPORT
	};

	private static VRSettings.VrOptions[] limitedTeleportSettings = new VRSettings.VrOptions[] {
			VRSettings.VrOptions.TELEPORT_UP_LIMIT,
			VRSettings.VrOptions.TELEPORT_DOWN_LIMIT,
			VRSettings.VrOptions.TELEPORT_HORIZ_LIMIT
	};

	public GuiTeleportSettings(Screen guiScreen) {
		super(guiScreen);
	}

	@Override
	public void init()
	{
		vrTitle = "Teleport Settings";

		super.init(teleportSettings, true);
		if (settings.vrLimitedSurvivalTeleport)
			super.init(limitedTeleportSettings, false);

		super.addDefaultButtons();
	}

	@Override
	protected void loadDefaults() {
		VRSettings vrSettings = minecraft.vrSettings;
		vrSettings.vrLimitedSurvivalTeleport = true;
		vrSettings.simulateFalling = true;
		vrSettings.vrTeleportDownLimit = 4;
		vrSettings.vrTeleportUpLimit = 1;
		vrSettings.vrTeleportHorizLimit = 16;
		vrSettings.saveOptions();
		this.reinit = true;
	}

	@Override
	protected void actionPerformed(Widget widget) {
		if(!(widget instanceof GuiVROptionButton)) return;
		GuiVROptionButton button = (GuiVROptionButton) widget;
		if (button.id == VRSettings.VrOptions.LIMIT_TELEPORT.ordinal())
			this.reinit = true;
	}
}
