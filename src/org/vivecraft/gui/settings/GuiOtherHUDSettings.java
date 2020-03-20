package org.vivecraft.gui.settings;

import org.vivecraft.gui.framework.GuiVROptionButton;
import org.vivecraft.gui.framework.GuiVROptionsBase;
import org.vivecraft.settings.VRSettings;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;

public class GuiOtherHUDSettings extends GuiVROptionsBase
{
    // VIVE START - hide options not supported by tracked controller UI
    static VRSettings.VrOptions[] hudOptions = new VRSettings.VrOptions[] {
            VRSettings.VrOptions.CROSSHAIR_SCALE,
            VRSettings.VrOptions.RENDER_CROSSHAIR_MODE,
            //VRSettings.VrOptions.CROSSHAIR_ROLL,
            VRSettings.VrOptions.RENDER_BLOCK_OUTLINE_MODE,
            VRSettings.VrOptions.MENU_CROSSHAIR_SCALE,
            VRSettings.VrOptions.CROSSHAIR_OCCLUSION,
            //VRSettings.VrOptions.MAX_CROSSHAIR_DISTANCE_AT_BLOCKREACH,
            VRSettings.VrOptions.CROSSHAIR_SCALES_WITH_DISTANCE,
            VRSettings.VrOptions.CHAT_NOTIFICATIONS
    };

    static VRSettings.VrOptions[] chat = new VRSettings.VrOptions[] {
            VRSettings.VrOptions.CHAT_NOTIFICATION_SOUND      
    };

    public GuiOtherHUDSettings(Screen guiScreen) {
        super( guiScreen );
    }

    @Override
    public void init()
    {
    	vrTitle = "Chat/Crosshair Settings";
    	super.init(hudOptions, true);  
    	if(minecraft.vrSettings.chatNotifications > 1)
    		super.init(chat, false);
    	super.addDefaultButtons();
    }

    @Override
    protected void loadDefaults() {
        this.settings.crosshairScale = 1.0f;
        this.settings.renderBlockOutlineMode = VRSettings.RENDER_BLOCK_OUTLINE_MODE_ALWAYS;
        this.settings.renderInGameCrosshairMode = VRSettings.RENDER_CROSSHAIR_MODE_ALWAYS;
        this.settings.menuCrosshairScale = 1f;
        this.settings.useCrosshairOcclusion = false;
        this.settings.crosshairScalesWithDistance = false;
        this.settings.chatNotifications = VRSettings.CHAT_NOTIFICATIONS_NONE;
        this.settings.chatNotificationSound = "block.note_block.bell";
    }
	@Override
	
	protected void actionPerformed(Widget widget) {
		if(!(widget instanceof GuiVROptionButton)) return;
		GuiVROptionButton button = (GuiVROptionButton) widget;
		if (button.id == VRSettings.VrOptions.CHAT_NOTIFICATIONS.ordinal())
			this.reinit = true;
	}
}
