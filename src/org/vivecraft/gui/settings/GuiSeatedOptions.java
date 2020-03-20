package org.vivecraft.gui.settings;

import org.vivecraft.gui.framework.GuiVROptionButton;
import org.vivecraft.gui.framework.GuiVROptionsBase;
import org.vivecraft.gui.framework.VROptionEntry;
import org.vivecraft.settings.VRSettings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;

public class GuiSeatedOptions extends GuiVROptionsBase
{
	private VROptionEntry[] seatedOptions = new VROptionEntry[] {
			new VROptionEntry(VRSettings.VrOptions.X_SENSITIVITY),
			new VROptionEntry(VRSettings.VrOptions.Y_SENSITIVITY),
			new VROptionEntry(VRSettings.VrOptions.KEYHOLE),
			new VROptionEntry(VRSettings.VrOptions.SEATED_HUD_XHAIR),
			new VROptionEntry(VRSettings.VrOptions.WALK_UP_BLOCKS),
			new VROptionEntry(VRSettings.VrOptions.WORLD_ROTATION_INCREMENT),
			new VROptionEntry(VRSettings.VrOptions.SEATED_FREE_MOVE, true),
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

	public GuiSeatedOptions(Screen guiScreen) {
		super( guiScreen );
	}

	@Override
	public void init()
	{
		vrTitle = "Seated Settings";


		super.init(seatedOptions, true);


		super.addDefaultButtons();
	}

    @Override
	protected void loadDefaults() {
		VRSettings vrSettings=Minecraft.getInstance().vrSettings;
		vrSettings.keyholeX=15;
		vrSettings.xSensitivity=1;
		vrSettings.ySensitivity=1;
		vrSettings.seatedHudAltMode = false;
		vrSettings.vrWorldRotationIncrement = 45f;
		vrSettings.seatedFreeMove = false;
	}


}
