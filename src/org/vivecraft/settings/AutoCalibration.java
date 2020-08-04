package org.vivecraft.settings;

import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.utils.LangHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;

public class AutoCalibration {
	
	public static final float defaultHeight =1.52f;

	public static void calibrateManual() {
		Minecraft mc=Minecraft.getInstance();
		mc.vrSettings.manualCalibration=(float) MCOpenVR.hmdPivotHistory.averagePosition(0.5).y;
		int height = (int)(float)(double)Math.round((double)0144 * getPlayerHeight() / (float)defaultHeight);
		mc.ingameGUI.getChatGUI().printChatMessage(new StringTextComponent(LangHelper.get("vivecraft.messages.heightset", height)));
		mc.vrSettings.saveOptions();
	}

	public static float getPlayerHeight(){
		Minecraft mc=Minecraft.getInstance();	
		float h = defaultHeight;	
		if(mc.vrSettings.seated) return h; //you cant do roomscale crap or calibrate your height, anyway.
		
		if(mc.vrSettings.manualCalibration != -1)
			h= mc.vrSettings.manualCalibration;
		
		return h;	
	}
}
