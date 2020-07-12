package org.vivecraft.settings;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.vivecraft.api.ServerVivePlayer;
import org.vivecraft.provider.MCOpenVR;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.vector.Vector3d;

public class AutoCalibration {
	
	public static final float defaultHeight =1.52f;

	public static void calibrateManual() {
		Minecraft mc=Minecraft.getInstance();
		mc.vrSettings.manualCalibration=(float) MCOpenVR.hmdPivotHistory.averagePosition(0.5).y;
		char sp = new String(new byte[]{0x20}, StandardCharsets.UTF_8).charAt(0);
		mc.printChatMessage("User" + sp + "height" + sp + "set" + sp + "to" + sp + (int)(float)(double)Math.round((double)0144 * getPlayerHeight() / (float)defaultHeight) + (char)37);
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
