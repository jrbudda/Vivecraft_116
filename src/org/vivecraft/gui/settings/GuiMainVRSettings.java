/**
  * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package org.vivecraft.gui.settings;

import org.vivecraft.gui.framework.GuiVROptionsBase;
import org.vivecraft.gui.framework.VROptionLayout;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.settings.VRSettings;

import net.minecraft.client.gui.screen.Screen;

public class GuiMainVRSettings extends GuiVROptionsBase 
{    
	private VROptionLayout[] vrAlwaysOptions = new VROptionLayout[]
        {
            new VROptionLayout(GuiHUDSettings.class,VROptionLayout.Position.POS_LEFT,  1,  VROptionLayout.ENABLED, "HUD and GUI Settings..."),
            new VROptionLayout(GuiRenderOpticsSettings.class,VROptionLayout.Position.POS_LEFT,   0, VROptionLayout.ENABLED, "Stereo Rendering..."),
            new VROptionLayout(GuiQuickCommandEditor.class,VROptionLayout.Position.POS_RIGHT,  0, VROptionLayout.ENABLED, "Quick Commands..."),
            new VROptionLayout(GuiOtherHUDSettings.class,VROptionLayout.Position.POS_RIGHT,  1, VROptionLayout.ENABLED, "Chat & Crosshair Settings..."),
            new VROptionLayout(VRSettings.VrOptions.WORLD_SCALE,       	VROptionLayout.Position.POS_LEFT,   6f, VROptionLayout.ENABLED, null),
            new VROptionLayout(VRSettings.VrOptions.WORLD_ROTATION,       VROptionLayout.Position.POS_RIGHT,   6f, VROptionLayout.ENABLED, null),
            new VROptionLayout(VRSettings.VrOptions.PLAY_MODE_SEATED, (button, mousePos) -> {
				GuiMainVRSettings.this.reinit = true;
            	if (minecraft.vrSettings.seated == false) {
					GuiMainVRSettings.this.isConfirm = true;
					return true;
				}
				return false;
			}, VROptionLayout.Position.POS_CENTER,  2,  VROptionLayout.ENABLED, null)
         };

	private VROptionLayout[] vrStandingOptions = new VROptionLayout[]
            {
                new VROptionLayout(GuiStandingSettings.class,VROptionLayout.Position.POS_LEFT,   4f, VROptionLayout.ENABLED, "Locomotion Settings..."),
                new VROptionLayout(GuiRoomscaleSettings.class,VROptionLayout.Position.POS_RIGHT,   4f, VROptionLayout.ENABLED, "Interaction Settings..."),
                new VROptionLayout(GuiVRControls.class,VROptionLayout.Position.POS_LEFT,   5f, VROptionLayout.ENABLED, "Controller Settings..."),
                new VROptionLayout(GuiRadialConfiguration.class,VROptionLayout.Position.POS_RIGHT,   5f, VROptionLayout.ENABLED, "Radial Menu..."),
               // new VROption(VRSettings.VrOptions.REVERSE_HANDS,   VROption.Position.POS_RIGHT,   5f, VROption.ENABLED, null),
            };

	private VROptionLayout[] vrSeatedOptions = new VROptionLayout[]
            {
                    new VROptionLayout(GuiSeatedOptions.class, VROptionLayout.Position.POS_LEFT, 4f, VROptionLayout.ENABLED, "Seated Settings..."),
                    new VROptionLayout(VRSettings.VrOptions.RESET_ORIGIN, (button, mousePos) -> {
                    	resetOrigin();
						return true;
					}, VROptionLayout.Position.POS_RIGHT,   4f, VROptionLayout.ENABLED, null),
            };

	private VROptionLayout[] vrConfirm = new VROptionLayout[]
            {
                    new VROptionLayout((button, mousePos) -> {
						GuiMainVRSettings.this.reinit = true;
						GuiMainVRSettings.this.isConfirm = false;
						return false;
					}, VROptionLayout.Position.POS_RIGHT,  2,  VROptionLayout.ENABLED, "Cancel"),
                    new VROptionLayout((button, mousePos) -> {
						GuiMainVRSettings.this.minecraft.vrSettings.seated = true;
						GuiMainVRSettings.this.settings.saveOptions();
						GuiMainVRSettings.this.reinit = true;
						GuiMainVRSettings.this.isConfirm = false;
						return false;
					}, VROptionLayout.Position.POS_LEFT,   2, VROptionLayout.ENABLED, "OK"),
            };
    
    private boolean isConfirm = false;

    public GuiMainVRSettings(Screen lastScreen) {
		super(lastScreen);
	}

	@Override
    protected void init()
    {
    	if(!isConfirm){
    		vrTitle = "VR Settings";
    		if(minecraft.vrSettings.seated) {
    			super.init(vrSeatedOptions, true);
    		}else {
    			super.init(vrStandingOptions, true);
    			if (minecraft.vrSettings.allowStandingOriginOffset) {
    				super.init(new VROptionLayout[]{new VROptionLayout(VRSettings.VrOptions.RESET_ORIGIN, (button, mousePos) -> {
						resetOrigin();
						return true;
					}, VROptionLayout.Position.POS_LEFT, 7f, VROptionLayout.ENABLED, null)}, false);
				}
    		}
    		super.init(vrAlwaysOptions, false);
			super.addDefaultButtons();
    	}
    	else {
    		vrTitle = "Switching to Seated Mode will disable controller input. Continue?";
    		super.init(vrConfirm, true);
    	}
    }

    @Override
    protected void loadDefaults() {
        minecraft.vrSettings.vrWorldRotation = 0;
        MCOpenVR.seatedRot = 0;
        minecraft.vrSettings.vrWorldScale = 1;
        minecraft.vrSettings.vrWorldRotationIncrement = 45f;
        minecraft.vrSettings.seated = false;
		MCOpenVR.clearOffset();
    }

    protected void resetOrigin() {
		MCOpenVR.resetPosition();
		settings.saveOptions();
		this.minecraft.displayGuiScreen(null);
	}
    
//    @Override
//    protected String[] getTooltipLines(String displayString, int buttonId)
//    {
//        VRSettings.VrOptions e = VRSettings.VrOptions.getEnumOptions(buttonId);
//
//    	if( e != null )
//    	switch(e)
//    	{
//     	case REVERSE_HANDS:
//    		return ;
//        case WORLD_SCALE:
//            return ;
//        case WORLD_ROTATION:
//            return ;
//        case WORLD_ROTATION_INCREMENT:
//            return ;
//        case PLAY_MODE_SEATED:
//            return ;
//        case RESET_ORIGIN:
//                return ;
//            default:
//    		return null;
//    	}
//    	else
//    	switch(buttonId)
//    	{
//            case 201:
//                return new String[] {
//                        "Open this configuration screen to adjust the Player",
//                        "  avatar preferences, select Oculus profiles etc.",
//                        "  Ex: IPD, Player (Eye) Height"
//                };
//            case 202:
//                return new String[] {
//                        "Open this configuration screen to adjust the Heads-",
//                        "Up Display (HUD) overlay properties.",
//                };
//            case 203:
//                return new String[] {
//                        "Open this configuration screen to adjust device",
//                        "calibration settings.",
//                        "  Ex: Initial calibration time"
//                };
//	    	case 205:
//	    		return new String[] {
//	    			"Open this configuration screen to adjust the Head",
//	    			"  Tracker orientation (direction) settings. ",
//	    			"  Ex: Head Tracking Selection (Hydra/Oculus), Prediction"
//	    		};
//	    	case 206:
//	    		return new String[] {
//	    			"Options for how the game is rendered and displayed on",
//	    			"the HMD and desktop mirror."
//	    		};
//	    	case 207:
//	    		return new String[] {
//	    			"Edit a list of commands or chat strings that will",
//	    			"be available in-game in the Quick Commands menu."
//	    		};
//	    	case 208:
//	    		return new String[] {
//	    			"Open this configuration screen to adjust how the ",
//	    			"  character is controlled. ",
//	    			"  Ex: Look/move/aim decouple, joystick sensitivty, " ,
//	    			"     Keyhole width, Mouse-pitch-affects camera" ,
//	    		};
//            case 209:
//                return new String[] {
//                        "Configure the locomotion based settings: movement",
//                        "attributes, VR comfort mode etc..."
//                } ;
//            case 210:
//                return new String[] {
//                        "Options for how the game crosshair displays"
//                } ;
//            case 211:
//                return new String[] {
//                        "Options for Seated Play mode"
//                };
//            case 220:
//                return new String[] {
//                        "Rebind the VR motion controller buttons to in-game",
//                        "actions"
//                } ;
//    		default:
//    			return null;
//    	}
//    }

}