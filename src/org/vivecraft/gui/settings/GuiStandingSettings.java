package org.vivecraft.gui.settings;

import java.util.ArrayList;
import java.util.Collections;

import org.vivecraft.gui.framework.GuiVROptionButton;
import org.vivecraft.gui.framework.GuiVROptionsBase;
import org.vivecraft.gui.framework.VROptionEntry;
import org.vivecraft.settings.VRSettings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;

public class GuiStandingSettings extends GuiVROptionsBase
{
	private VROptionEntry[] locomotionSettings = new VROptionEntry[]
			{
					new VROptionEntry(VRSettings.VrOptions.WALK_UP_BLOCKS),
					new VROptionEntry(VRSettings.VrOptions.VEHICLE_ROTATION),
					new VROptionEntry(VRSettings.VrOptions.WALK_MULTIPLIER),
					new VROptionEntry(VRSettings.VrOptions.WORLD_ROTATION_INCREMENT),
					new VROptionEntry(VRSettings.VrOptions.BCB_ON),
					new VROptionEntry(VRSettings.VrOptions.ALLOW_STANDING_ORIGIN_OFFSET),
					new VROptionEntry(VRSettings.VrOptions.FORCE_STANDING_FREE_MOVE, true),
					new VROptionEntry(VRSettings.VrOptions.DUMMY, true),
					new VROptionEntry("Teleport Settings...", (button, mousePos) -> {
						minecraft.displayGuiScreen(new GuiTeleportSettings(this));
						return true;
					}),
					new VROptionEntry("Free Move Settings...", (button, mousePos) -> {
						minecraft.displayGuiScreen(new GuiFreeMoveSettings(this));
						return true;
					})
			};

	public GuiStandingSettings(Screen guiScreen) {
		super(guiScreen);
	}

	@Override
	public void init()
	{
		vrTitle = "Standing Locomotion Settings";

		super.init(locomotionSettings, true);

		super.addDefaultButtons();
	}

	@Override
	protected void loadDefaults() {
		VRSettings vr = minecraft.vrSettings;
		vr.vrAllowCrawling = false;
		vr.vrShowBlueCircleBuddy = true;
		vr.walkMultiplier=1;
		vr.vehicleRotation = true;
		vr.walkUpBlocks = true;
		vr.vrWorldRotationIncrement = 45f;
		vr.allowStandingOriginOffset = false;
		vr.forceStandingFreeMove = false;
	}
}
