package org.vivecraft.gui.framework;

import javax.annotation.Nullable;

import org.vivecraft.settings.VRSettings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;

public class GuiVROptionButton extends Button
{
    @Nullable
    protected final VRSettings.VrOptions enumOptions;
    public int id = -1;
    public GuiVROptionButton(int id, int x, int y, String text, IPressable action)
    {
        this(id, x, y, (VRSettings.VrOptions)null, text, action);
    }

    public GuiVROptionButton(int id, int x, int y, @Nullable VRSettings.VrOptions option, String text, IPressable action)
    {
        this(id, x, y, 150, 20, option, text, action);
    }

    public GuiVROptionButton(int id, int x, int y, int width, int height, @Nullable VRSettings.VrOptions option, String text, IPressable action)
    {
        super(x, y, width, height, text, action);
    	this.id = id;
        this.enumOptions = option;

        Minecraft mc = Minecraft.getInstance();
        if (option != null && mc.vrSettings.overrides.hasSetting(option) && mc.vrSettings.overrides.getSetting(option).isValueOverridden())
            this.active = false;
    }

    @Nullable
    public VRSettings.VrOptions getOption()
    {
        return this.enumOptions;
    }
    
    public String[] getToolTip() {
    	if(this.enumOptions == null) {
    		return null;
    	}
    	return this.enumOptions.getToolTip();
    }
}
