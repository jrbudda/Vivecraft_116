/**
* Copyright 2013 Mark Browning, StellaArtois
* Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
*/
package org.vivecraft.settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.optifine.Lang;
import org.lwjgl.glfw.GLFW;
import org.vivecraft.api.VRData;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.settings.VRSettings.VrOptions;
import org.vivecraft.utils.LangHelper;
import org.vivecraft.utils.Utils;
import org.vivecraft.utils.math.Angle;
import org.vivecraft.utils.math.Axis;
import org.vivecraft.utils.math.Matrix4f;
import org.vivecraft.utils.math.Quaternion;
import org.vivecraft.utils.math.Vector3;

import com.google.common.util.concurrent.Runnables;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.WinGameScreen;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.math.vector.Vector3d;

public class VRHotkeys {

	static long nextRead = 0;
	static final long COOLOFF_PERIOD_MILLIS = 500;
	static boolean debug = false;

	private static int startController;
	private static VRData.VRDevicePose startControllerPose;
	private static float startCamposX;
	private static float startCamposY;
	private static float startCamposZ;
	private static Quaternion startCamrotQuat;
	private static Triggerer camTriggerer;

	public static boolean handleKeyboardInputs(int key, int scanCode, int action, int modifiers)
	{
		// Support cool-off period for key presses - otherwise keys can get spammed...
		if (nextRead != 0 && System.currentTimeMillis() < nextRead)
			return false;
		Minecraft mc = Minecraft.getInstance();

		// Capture Minecrift key events
		boolean gotKey = false;

		// Debug aim
		if (action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_RIGHT_SHIFT && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL))
		{
			mc.vrSettings.storeDebugAim = true;
			mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("vivecraft.messages.showaim"));
			gotKey = true;
		}

		// Walk up blocks
		if (action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_B && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL))
		{
			mc.vrSettings.walkUpBlocks = !mc.vrSettings.walkUpBlocks;
			mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("vivecraft.messages.walkupblocks", mc.vrSettings.walkUpBlocks ? LangHelper.getYes() : LangHelper.getNo()));
			gotKey = true;
		}

		// Player inertia
		if (action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_I && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL))
		{
			mc.vrSettings.inertiaFactor += 1;
			if (mc.vrSettings.inertiaFactor > VRSettings.INERTIA_MASSIVE)
			mc.vrSettings.inertiaFactor = VRSettings.INERTIA_NONE;
			switch (mc.vrSettings.inertiaFactor)
			{
			case VRSettings.INERTIA_NONE:
				mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("vivecraft.messages.playerinertia", Lang.get("vivecraft.options.inertia.none")));
				break;
			case VRSettings.INERTIA_NORMAL:
				mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("vivecraft.messages.playerinertia", Lang.get("vivecraft.options.inertia.normal")));
				break;
			case VRSettings.INERTIA_LARGE:
				mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("vivecraft.messages.playerinertia", Lang.get("vivecraft.options.inertia.large")));
				break;
			case VRSettings.INERTIA_MASSIVE:
				mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("vivecraft.messages.playerinertia", Lang.get("vivecraft.options.inertia.massive")));
				break;
			}
			gotKey = true;
		}

		// Render full player model or just an disembodied hand...
		/*if (action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_H && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL))
		{
			mc.vrSettings.renderFullFirstPersonModelMode++;
			if (mc.vrSettings.renderFullFirstPersonModelMode > VRSettings.RENDER_FIRST_PERSON_NONE)
			mc.vrSettings.renderFullFirstPersonModelMode = VRSettings.RENDER_FIRST_PERSON_FULL;

				switch (mc.vrSettings.renderFullFirstPersonModelMode)
			{
			case VRSettings.RENDER_FIRST_PERSON_FULL:
				mc.printChatMessage("First person model (RCTRL-H): Full");
				break;
			case VRSettings.RENDER_FIRST_PERSON_HAND:
				mc.printChatMessage("First person model (RCTRL-H): Hand");
				break;
			case VRSettings.RENDER_FIRST_PERSON_NONE:
				mc.printChatMessage("First person model (RCTRL-H): None");
				break;
			}
			gotKey = true;
		}*/
		// VIVE START - hotkeys

		// Testing different movement styles
//		if (Keyboard.getEventKey() == Keyboard.KEY_M && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
//		{
//			// JRBUDDA ADDED all dis.
//			if (mc.vrPlayer.getFreeMoveMode()) {
//				//cycle restricted movement styles
//				if (mc.vrPlayer.useLControllerForRestricedMovement) {
//					mc.vrPlayer.useLControllerForRestricedMovement = false;
//					mc.printChatMessage("Restricted movement mode set to gaze");
//				} else {
//					mc.vrPlayer.useLControllerForRestricedMovement = true;
//					mc.printChatMessage("Restricted movement mode set to left controller");
//				}
//			} else {				
//				OpenVRPlayer vrp = mc.vrPlayer;				
//				// cycle VR movement styles
//				if (vrp.vrMovementStyle.name == "Minimal") vrp.vrMovementStyle.setStyle("Beam");
//				else if (vrp.vrMovementStyle.name == "Beam") vrp.vrMovementStyle.setStyle("Tunnel");
//				else if (vrp.vrMovementStyle.name == "Tunnel") vrp.vrMovementStyle.setStyle("Grapple");
//				else if (vrp.vrMovementStyle.name == "Grapple") vrp.vrMovementStyle.setStyle("Arc");
//				else vrp.vrMovementStyle.setStyle("Minimal");			
//			}
//					
//			gotKey = true;
//		}

		if (action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_R && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL))
		{
			// for testing restricted client mode

			if (mc.vrPlayer.isTeleportOverridden()) {
				mc.vrPlayer.setTeleportOverride(false);
				mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("vivecraft.messages.teleportdisabled"));
			} else {
				mc.vrPlayer.setTeleportOverride(true);
				mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("vivecraft.messages.teleportenabled"));
			}
			
			gotKey = true;
		}
		
		if (action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_HOME && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL))
		{
			snapMRCam(0);
			gotKey = true;
		}
		if (action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_F12 && debug) {
            mc.displayGuiScreen(new WinGameScreen(false, Runnables.doNothing()));
			gotKey = true;
		}
		if((mc.world == null || mc.currentScreen != null) && action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_F5) {
			mc.vrSettings.setOptionValue(VrOptions.MIRROR_DISPLAY);
			mc.notifyMirror(mc.vrSettings.getButtonDisplayString(VrOptions.MIRROR_DISPLAY), false, 3000);
		}
		// VIVE END - hotkeys

		if (gotKey) {
			mc.vrSettings.saveOptions();
		}

		return gotKey;
	}

		
	public static void handleMRKeys() {
		Minecraft mc = Minecraft.getInstance();
		
		boolean gotKey = false;
		
		if (InputMappings.isKeyDown(GLFW.GLFW_KEY_LEFT) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL) && !InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT))
		{
			adjustCamPos(new Vector3(-0.01F, 0, 0));
			gotKey = true;
		}
		if (InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL) && !InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT))
		{
			adjustCamPos(new Vector3(0.01F, 0, 0));
			gotKey = true;
		}
		if (InputMappings.isKeyDown(GLFW.GLFW_KEY_UP) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL) && !InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT))
		{
			adjustCamPos(new Vector3(0, 0, -0.01F));
			gotKey = true;
		}
		if (InputMappings.isKeyDown(GLFW.GLFW_KEY_DOWN) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL) && !InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT))
		{
			adjustCamPos(new Vector3(0, 0, 0.01F));
			gotKey = true;
		}
		if (InputMappings.isKeyDown(GLFW.GLFW_KEY_PAGE_UP) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL) && !InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT))
		{
			adjustCamPos(new Vector3(0, 0.01F, 0));
			gotKey = true;
		}
		if (InputMappings.isKeyDown(GLFW.GLFW_KEY_PAGE_DOWN) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL) && !InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT))
		{
			adjustCamPos(new Vector3(0, -0.01F, 0));
			gotKey = true;
		}

		if (InputMappings.isKeyDown(GLFW.GLFW_KEY_UP) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT))
		{
			adjustCamRot(Axis.PITCH, 0.5F);
			gotKey = true;
		}
		if (InputMappings.isKeyDown(GLFW.GLFW_KEY_DOWN) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT))
		{
			adjustCamRot(Axis.PITCH, -0.5F);
			gotKey = true;
		}
		if (InputMappings.isKeyDown(GLFW.GLFW_KEY_LEFT) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT))
		{
			adjustCamRot(Axis.YAW, 0.5F);
			gotKey = true;
		}
		if (InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT))
		{
			adjustCamRot(Axis.YAW, -0.5F);
			gotKey = true;
		}
		if (InputMappings.isKeyDown(GLFW.GLFW_KEY_PAGE_UP) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT))
		{
			adjustCamRot(Axis.ROLL, 0.5F);
			gotKey = true;
		}
		if (InputMappings.isKeyDown(GLFW.GLFW_KEY_PAGE_DOWN) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT))
		{
			adjustCamRot(Axis.ROLL, -0.5F);
			gotKey = true;
		}

		if (InputMappings.isKeyDown(GLFW.GLFW_KEY_INSERT) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL) && !InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT))
		{
			mc.gameSettings.fov += 1;
			gotKey = true;
		}
		if (InputMappings.isKeyDown(GLFW.GLFW_KEY_DELETE) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL) && !InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT))
		{
			mc.gameSettings.fov -= 1;
			gotKey = true;
		}
		if (InputMappings.isKeyDown(GLFW.GLFW_KEY_INSERT) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT))
		{
			mc.vrSettings.mixedRealityFov += 1;
			gotKey = true;
		}
		if (InputMappings.isKeyDown(GLFW.GLFW_KEY_DELETE) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL) && InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT))
		{
			mc.vrSettings.mixedRealityFov -= 1;
			gotKey = true;
		}
		
		if (gotKey) {
			mc.vrSettings.saveOptions();
			if (MCOpenVR.mrMovingCamActive) {
				Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessage(new StringTextComponent(LangHelper.get("vivecraft.messages.coords", mc.vrSettings.mrMovingCamOffsetX, mc.vrSettings.mrMovingCamOffsetY, mc.vrSettings.mrMovingCamOffsetZ)));
				Angle angle = mc.vrSettings.mrMovingCamOffsetRotQuat.toEuler();
				Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessage(new StringTextComponent(LangHelper.get("vivecraft.messages.angles", angle.getPitch(), angle.getYaw(), angle.getRoll())));
			} else {
				Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessage(new StringTextComponent(LangHelper.get("vivecraft.messages.coords", mc.vrSettings.vrFixedCamposX, mc.vrSettings.vrFixedCamposY, mc.vrSettings.vrFixedCamposZ)));
				Angle angle = mc.vrSettings.vrFixedCamrotQuat.toEuler();
				Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessage(new StringTextComponent(LangHelper.get("vivecraft.messages.angles", angle.getPitch(), angle.getYaw(), angle.getRoll())));
			}
		}
	}

	private static void adjustCamPos(Vector3 offset) {
		Minecraft mc = Minecraft.getInstance();
		if (MCOpenVR.mrMovingCamActive) {
			offset = mc.vrSettings.mrMovingCamOffsetRotQuat.multiply(offset);
			mc.vrSettings.mrMovingCamOffsetX += offset.getX();
			mc.vrSettings.mrMovingCamOffsetY += offset.getY();
			mc.vrSettings.mrMovingCamOffsetZ += offset.getZ();
		} else {
			offset = mc.vrSettings.vrFixedCamrotQuat.inverse().multiply(offset);
			mc.vrSettings.vrFixedCamposX += offset.getX();
			mc.vrSettings.vrFixedCamposY += offset.getY();
			mc.vrSettings.vrFixedCamposZ += offset.getZ();
		}
	}

	private static void adjustCamRot(Axis axis, float degrees) {
		Minecraft mc = Minecraft.getInstance();
		if (MCOpenVR.mrMovingCamActive) {
			mc.vrSettings.mrMovingCamOffsetRotQuat.set(mc.vrSettings.mrMovingCamOffsetRotQuat.rotate(axis, degrees, true));
		} else {
			mc.vrSettings.vrFixedCamrotQuat.set(mc.vrSettings.vrFixedCamrotQuat.rotate(axis, degrees, false));
		}
	}
	
	public static void snapMRCam(int controller) {
		Minecraft mc = Minecraft.getInstance();
		Vector3d c = mc.vrPlayer.vrdata_room_pre.getController(controller).getPosition();
		mc.vrSettings.vrFixedCamposX =(float) c.x;
		mc.vrSettings.vrFixedCamposY =(float) c.y;
		mc.vrSettings.vrFixedCamposZ =(float) c.z;

		Quaternion quat = new Quaternion(Utils.convertOVRMatrix(mc.vrPlayer.vrdata_room_pre.getController(controller).getMatrix()));
		mc.vrSettings.vrFixedCamrotQuat.set(quat);
	}

	public static void updateMovingThirdPersonCam() {
		Minecraft mc = Minecraft.getInstance();

		if (startControllerPose != null) {
			VRData.VRDevicePose controllerPose = mc.vrPlayer.vrdata_room_pre.getController(startController);
			Vector3d startPos = startControllerPose.getPosition();
			Vector3d deltaPos = controllerPose.getPosition().subtract(startPos);

			Matrix4f deltaMatrix = Matrix4f.multiply(controllerPose.getMatrix(), startControllerPose.getMatrix().inverted());
			Vector3 offset = new Vector3(startCamposX - (float)startPos.x, startCamposY - (float)startPos.y, startCamposZ - (float)startPos.z);
			Vector3 offsetRotated = deltaMatrix.transform(offset);

			mc.vrSettings.vrFixedCamposX = startCamposX + (float)deltaPos.x + (offsetRotated.getX() - offset.getX());
			mc.vrSettings.vrFixedCamposY = startCamposY + (float)deltaPos.y + (offsetRotated.getY() - offset.getY());
			mc.vrSettings.vrFixedCamposZ = startCamposZ + (float)deltaPos.z + (offsetRotated.getZ() - offset.getZ());
			mc.vrSettings.vrFixedCamrotQuat.set(startCamrotQuat.multiply(new Quaternion(Utils.convertOVRMatrix(deltaMatrix))));
		}
	}

	public static void startMovingThirdPersonCam(int controller, Triggerer triggerer) {
		Minecraft mc = Minecraft.getInstance();
		startController = controller;
		startControllerPose = mc.vrPlayer.vrdata_room_pre.getController(controller);
		startCamposX = mc.vrSettings.vrFixedCamposX;
		startCamposY = mc.vrSettings.vrFixedCamposY;
		startCamposZ = mc.vrSettings.vrFixedCamposZ;
		startCamrotQuat = mc.vrSettings.vrFixedCamrotQuat.copy();
		camTriggerer = triggerer;
	}

	public static void stopMovingThirdPersonCam() {
		startControllerPose = null;
	}

	public static boolean isMovingThirdPersonCam() {
		return startControllerPose != null;
	}

	public static int getMovingThirdPersonCamController() {
		return startController;
	}

	public static Triggerer getMovingThirdPersonCamTriggerer() {
		return camTriggerer;
	}

	public static void loadExternalCameraConfig() {
		File file = new File("ExternalCamera.cfg");
		if (!file.exists())
			return;

		float x = 0, y = 0, z = 0;
		float rx = 0, ry = 0, rz = 0;
		float fov = 40;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split("=", 2);
				switch (tokens[0]) {
					case "x":
						x = Float.parseFloat(tokens[1]);
						break;
					case "y":
						y = Float.parseFloat(tokens[1]);
						break;
					case "z":
						z = Float.parseFloat(tokens[1]);
						break;
					case "rx":
						rx = Float.parseFloat(tokens[1]);
						break;
					case "ry":
						ry = Float.parseFloat(tokens[1]);
						break;
					case "rz":
						rz = Float.parseFloat(tokens[1]);
						break;
					case "fov":
						fov = Float.parseFloat(tokens[1]);
						break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		Quaternion quat = new Quaternion(rx, ry, rz, mc.vrSettings.externalCameraAngleOrder);

		// Eh just set everything, the fixed pos is overridden by the moving cam anyways
		mc.vrSettings.mrMovingCamOffsetX = x;
		mc.vrSettings.mrMovingCamOffsetY = y;
		mc.vrSettings.mrMovingCamOffsetZ = z;
		mc.vrSettings.mrMovingCamOffsetRotQuat.set(quat);
		mc.vrSettings.vrFixedCamposX = x;
		mc.vrSettings.vrFixedCamposY = y;
		mc.vrSettings.vrFixedCamposZ = z;
		mc.vrSettings.vrFixedCamrotQuat.set(quat);
		mc.vrSettings.mixedRealityFov = fov;
	}

	public static boolean hasExternalCameraConfig() {
		return new File("ExternalCamera.cfg").exists();
	}

	public static enum Triggerer {
		BINDING,
		MENUBUTTON,
		INTERACTION
	}
}
