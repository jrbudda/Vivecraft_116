package org.vivecraft.gui.settings;

import java.util.function.BiFunction;

import org.vivecraft.gameplay.screenhandlers.GuiHandler;
import org.vivecraft.gameplay.screenhandlers.KeyboardHandler;
import org.vivecraft.gui.framework.GuiVROptionButton;
import org.vivecraft.gui.framework.GuiVROptionsBase;
import org.vivecraft.gui.framework.VROptionEntry;
import org.vivecraft.settings.VRSettings;
import org.vivecraft.settings.VRSettings.VrOptions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.Vec2f;
import org.apache.commons.lang3.ArrayUtils;

public class GuiHUDSettings extends GuiVROptionsBase
{
    private VROptionEntry[] hudOptions = new VROptionEntry[] {
            new VROptionEntry(VRSettings.VrOptions.HUD_HIDE),
            new VROptionEntry(VRSettings.VrOptions.HUD_LOCK_TO),
			new VROptionEntry(VRSettings.VrOptions.HUD_SCALE),
			new VROptionEntry(VRSettings.VrOptions.HUD_DISTANCE),
            new VROptionEntry(VRSettings.VrOptions.HUD_OCCLUSION),
            //new VROptionEntry(VRSettings.VrOptions.HUD_PITCH),
            //new VROptionEntry(VRSettings.VrOptions.HUD_YAW),
            new VROptionEntry(VRSettings.VrOptions.HUD_OPACITY),
            new VROptionEntry(VRSettings.VrOptions.RENDER_MENU_BACKGROUND),
            new VROptionEntry(VRSettings.VrOptions.TOUCH_HOTBAR),
            new VROptionEntry(VRSettings.VrOptions.MENU_ALWAYS_FOLLOW_FACE, (button, mousePos) -> {
                GuiHandler.onScreenChanged(Minecraft.getInstance().currentScreen, Minecraft.getInstance().currentScreen, false);
                return false;
            }),
            new VROptionEntry(VRSettings.VrOptions.AUTO_OPEN_KEYBOARD),
			new VROptionEntry(VRSettings.VrOptions.PHYSICAL_KEYBOARD, (button, mousePos) -> {
				KeyboardHandler.setOverlayShowing(false);
				return false;
			}),
			new VROptionEntry("Menu World Settings...", (button, mousePos) -> {
				Minecraft.getInstance().displayGuiScreen(new GuiMenuWorldSettings(this));
				return true;
			}),
    };

    public GuiHUDSettings(Screen guiScreen) {
        super(guiScreen);  
    }


    @Override
    public void init()
    {
    	vrTitle = "HUD and GUI Settings";

    	super.init(hudOptions, true);
        super.addDefaultButtons();
    }
    
    @Override
    protected void loadDefaults() {
        this.settings.hudDistance = 1.25f;
        this.settings.hudScale = 1.0f;
        this.settings.hudPitchOffset = -2f;
        this.settings.hudYawOffset = 0f;
        this.settings.hudOpacity = 1f;
        this.settings.menuBackground = false;
        this.settings.vrHudLockMode = settings.HUD_LOCK_HAND;
        this.settings.hudOcclusion = true;
        this.settings.menuAlwaysFollowFace = false;
        this.settings.autoOpenKeyboard = false;
        this.settings.physicalKeyboard = true;
        this.minecraft.gameSettings.hideGUI = false;
    }
}
