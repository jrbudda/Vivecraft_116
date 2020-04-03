/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package org.vivecraft.gui.settings;

import java.awt.Color;

import org.vivecraft.gui.framework.GuiVROptionButton;
import org.vivecraft.gui.framework.GuiVROptionsBase;
import org.vivecraft.settings.VRHotkeys;
import org.vivecraft.settings.VRSettings;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;

public class GuiRenderOpticsSettings  extends GuiVROptionsBase
{
    static VRSettings.VrOptions[] monoDisplayOptions = new VRSettings.VrOptions[] {
            VRSettings.VrOptions.MONO_FOV,
            VRSettings.VrOptions.DUMMY,
            VRSettings.VrOptions.FSAA,
    };

    static VRSettings.VrOptions[] openVRDisplayOptions = new VRSettings.VrOptions[] {
            VRSettings.VrOptions.RENDER_SCALEFACTOR,
            VRSettings.VrOptions.MIRROR_DISPLAY,     
            VRSettings.VrOptions.FSAA,
            VRSettings.VrOptions.STENCIL_ON,
			VRSettings.VrOptions.RELOAD_EXTERNAL_CAMERA,
			VRSettings.VrOptions.MIRROR_EYE
    };
    
    static VRSettings.VrOptions[] MROptions = new VRSettings.VrOptions[] {
            VRSettings.VrOptions.MIXED_REALITY_UNITY_LIKE,
            VRSettings.VrOptions.MIXED_REALITY_RENDER_HANDS,
            VRSettings.VrOptions.MIXED_REALITY_KEY_COLOR,
            VRSettings.VrOptions.MIXED_REALITY_FOV,
            VRSettings.VrOptions.MIXED_REALITY_UNDISTORTED,
            VRSettings.VrOptions.MONO_FOV,
            VRSettings.VrOptions.MIXED_REALITY_ALPHA_MASK,
    };
    
    static VRSettings.VrOptions[] UDOptions = new VRSettings.VrOptions[] {
            VRSettings.VrOptions.MONO_FOV,
    };
    
    static VRSettings.VrOptions[] TUDOptions = new VRSettings.VrOptions[] {
            VRSettings.VrOptions.MIXED_REALITY_FOV,
    };

    private float prevRenderScaleFactor;

    public GuiRenderOpticsSettings(Screen par1Screen)
    {
    	super( par1Screen);
		prevRenderScaleFactor = settings.renderScaleFactor;
    }

    @Override
    public void init()
    {
        vrTitle = "Stereo Renderer Settings";

    	{
			VRSettings.VrOptions[] buttons = new VRSettings.VrOptions[openVRDisplayOptions.length];
			System.arraycopy(openVRDisplayOptions, 0, buttons, 0, openVRDisplayOptions.length);
			for (int i = 0; i < buttons.length; i++) {
				VRSettings.VrOptions option = buttons[i];
				if (option == VRSettings.VrOptions.RELOAD_EXTERNAL_CAMERA && (!VRHotkeys.hasExternalCameraConfig() || (minecraft.vrSettings.displayMirrorMode != VRSettings.MIRROR_MIXED_REALITY && minecraft.vrSettings.displayMirrorMode != VRSettings.MIRROR_THIRD_PERSON)))
					buttons[i] = VRSettings.VrOptions.DUMMY;
				if (option == VRSettings.VrOptions.MIRROR_EYE && minecraft.vrSettings.displayMirrorMode != VRSettings.MIRROR_ON_CROPPED && minecraft.vrSettings.displayMirrorMode != VRSettings.MIRROR_ON_SINGLE)
					buttons[i] = VRSettings.VrOptions.DUMMY;
			}
			super.init(buttons, true);
		}

    	if(minecraft.vrSettings.displayMirrorMode == VRSettings.MIRROR_MIXED_REALITY){
//    		GuiSmallButtonEx mr = new GuiSmallButtonEx(0, this.width / 2 - 68, this.height / 6 + 65, "Mixed Reality Options");
//    		mr.enabled = false;
//    		this.buttons.add(mr);
    		VRSettings.VrOptions[] buttons = new VRSettings.VrOptions[MROptions.length];
    		System.arraycopy(MROptions, 0, buttons, 0, MROptions.length);
    		for (int i = 0; i < buttons.length; i++) {
    			VRSettings.VrOptions option = buttons[i];
    			if (option == VRSettings.VrOptions.MONO_FOV && (!minecraft.vrSettings.mixedRealityMRPlusUndistorted || !minecraft.vrSettings.mixedRealityUnityLike))
    				buttons[i] = VRSettings.VrOptions.DUMMY;
    			if (option == VRSettings.VrOptions.MIXED_REALITY_ALPHA_MASK && !minecraft.vrSettings.mixedRealityUnityLike)
    				buttons[i] = VRSettings.VrOptions.DUMMY;
    			if (option == VRSettings.VrOptions.MIXED_REALITY_UNDISTORTED && !minecraft.vrSettings.mixedRealityUnityLike)
    				buttons[i] = VRSettings.VrOptions.DUMMY;
    			if (option == VRSettings.VrOptions.MIXED_REALITY_KEY_COLOR && minecraft.vrSettings.mixedRealityAlphaMask && minecraft.vrSettings.mixedRealityUnityLike)
    				buttons[i] = VRSettings.VrOptions.DUMMY;
    		} 		
    		super.init(buttons, false);
    	}else if(minecraft.vrSettings.displayMirrorMode == VRSettings.MIRROR_FIRST_PERSON ){
    		super.init(UDOptions, false);
    	}else if( minecraft.vrSettings.displayMirrorMode == VRSettings.MIRROR_THIRD_PERSON){
    		super.init(TUDOptions, false);
    	}
    	super.addDefaultButtons();
    }
    
    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
    	super.render(mouseX, mouseY, partialTicks);
    }
    
    @Override
    protected void loadDefaults() {
    	this.settings.renderScaleFactor = 1.0f;
    	this.settings.displayMirrorMode = VRSettings.MIRROR_ON_CROPPED;
    	this.settings.displayMirrorLeftEye = false;
    	this.settings.mixedRealityKeyColor = new Color(0, 0, 0);
    	this.settings.mixedRealityRenderHands = false;
    	this.settings.insideBlockSolidColor = false;
    	this.settings.mixedRealityUnityLike = true;
    	this.settings.mixedRealityMRPlusUndistorted = true;
    	this.settings.mixedRealityAlphaMask = false;
    	this.settings.mixedRealityFov = 40;
    	this.minecraft.gameSettings.fov = 70f;
    	this.settings.useFsaa = true;
    	this.settings.vrUseStencil = true;
        this.minecraft.stereoProvider.reinitFrameBuffers("Defaults Loaded");
    }
    
    @Override
    protected void actionPerformed(Widget widget) {
    	if(!(widget instanceof GuiVROptionButton)) return;
    	GuiVROptionButton button = (GuiVROptionButton) widget;
    	if (button.id == VRSettings.VrOptions.MIRROR_DISPLAY.ordinal() ||
        		button.id == VRSettings.VrOptions.FSAA.ordinal())
        	{
                this.minecraft.stereoProvider.reinitFrameBuffers("Render Setting Changed");
        	}
    }

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		// Hacky way of making the render scale slider only reinit on mouse release
    	if (settings.renderScaleFactor != prevRenderScaleFactor) {
			prevRenderScaleFactor = settings.renderScaleFactor;
			this.minecraft.stereoProvider.reinitFrameBuffers("Render Setting Changed");
		}

		return super.mouseReleased(mouseX, mouseY, button);
	}
}

