/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package org.vivecraft.gui.settings;

import org.vivecraft.gui.framework.GuiVROptionsBase;
import org.vivecraft.gui.framework.VROptionEntry;
import org.vivecraft.settings.VRSettings;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.TextFormatting;

public class GuiVRControls extends GuiVROptionsBase {
	
	private static VROptionEntry[] controlsSettings = new VROptionEntry[] {
			new VROptionEntry(VRSettings.VrOptions.DUMMY, true),
			new VROptionEntry(VRSettings.VrOptions.DUMMY, true),
			new VROptionEntry(VRSettings.VrOptions.DUMMY, true),
			new VROptionEntry(VRSettings.VrOptions.DUMMY, true),
			new VROptionEntry(VRSettings.VrOptions.DUMMY, true),
			new VROptionEntry(VRSettings.VrOptions.DUMMY, true),
			new VROptionEntry(VRSettings.VrOptions.REVERSE_HANDS),
			new VROptionEntry(VRSettings.VrOptions.RIGHT_CLICK_DELAY),
			new VROptionEntry(VRSettings.VrOptions.ALLOW_ADVANCED_BINDINGS, true)
	};
	
	public GuiVRControls(Screen par1GuiScreen) {
		super(par1GuiScreen);
	}
   
	@Override
    public void init() {
        vrTitle = "Controller Settings";
        super.init(controlsSettings, true);
        super.addDefaultButtons();
    }

	@Override
	public void render(int mouseX, int mouseY, float partialTicks) {
		super.render(mouseX, mouseY, partialTicks);
		this.drawCenteredString(minecraft.fontRenderer, "Bindings are handled by SteamVR Input.", this.width / 2, this.height / 2 - minecraft.fontRenderer.FONT_HEIGHT / 2 - minecraft.fontRenderer.FONT_HEIGHT - 3, 16777215);
		this.drawCenteredString(minecraft.fontRenderer, "Go to Settings > Controllers > Manage Controller Bindings in the dashboard.", this.width / 2, this.height / 2 - minecraft.fontRenderer.FONT_HEIGHT / 2, 16777215);
		this.drawCenteredString(minecraft.fontRenderer, TextFormatting.GOLD + "Steam must be running " + TextFormatting.ITALIC + "before" + TextFormatting.RESET + TextFormatting.GOLD + " SteamVR is started, or bindings will not save.", this.width / 2, this.height / 2 - minecraft.fontRenderer.FONT_HEIGHT / 2 + minecraft.fontRenderer.FONT_HEIGHT + 3, 16777215);
	}
	
		@Override
		protected void loadDefaults() {
			VRSettings vrSettings = minecraft.vrSettings;
			vrSettings.vrReverseHands = false;
			vrSettings.allowAdvancedBindings = false;
			vrSettings.rightclickDelay = 6;
		}
}
