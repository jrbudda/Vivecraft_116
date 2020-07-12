package org.vivecraft.gui.framework;

import java.util.function.BiFunction;

import org.vivecraft.settings.VRSettings;

import net.minecraft.util.math.vector.Vector2f;

public class VROptionEntry {
    public final VRSettings.VrOptions option;
    public final String title;
    public final BiFunction<GuiVROptionButton, Vector2f, Boolean> customHandler;
    public final boolean center;

    public VROptionEntry(String label, BiFunction<GuiVROptionButton, Vector2f, Boolean> customHandler, boolean center) {
        this.option = null;
        this.title = label;
        this.customHandler = customHandler;
        this.center = center;
    }

    public VROptionEntry(String label, BiFunction<GuiVROptionButton, Vector2f, Boolean> customHandler) {
        this.option = null;
        this.title = label;
        this.customHandler = customHandler;
        this.center = false;
    }

    public VROptionEntry(VRSettings.VrOptions option, BiFunction<GuiVROptionButton, Vector2f, Boolean> customHandler, boolean center) {
        this.option = option;
        this.title = null;
        this.customHandler = customHandler;
        this.center = center;
    }

    public VROptionEntry(VRSettings.VrOptions option, BiFunction<GuiVROptionButton, Vector2f, Boolean> customHandler) {
        this.option = option;
        this.title = null;
        this.customHandler = customHandler;
        this.center = false;
    }

    public VROptionEntry(VRSettings.VrOptions option, boolean center) {
        this.option = option;
        this.title = null;
        this.customHandler = null;
        this.center = center;
    }

    public VROptionEntry(VRSettings.VrOptions option) {
        this.option = option;
        this.title = null;
        this.customHandler = null;
        this.center = false;
    }
}
