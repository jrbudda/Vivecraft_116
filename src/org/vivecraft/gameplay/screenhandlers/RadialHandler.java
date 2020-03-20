package org.vivecraft.gameplay.screenhandlers;

import java.util.function.Predicate;

import org.vivecraft.api.VRData.VRDevicePose;
import org.vivecraft.control.ControllerType;
import org.vivecraft.gui.GuiRadial;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.utils.OpenVRUtil;
import org.vivecraft.utils.Vector3;
import org.vivecraft.utils.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.Main;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class RadialHandler {
	//
	public static Minecraft mc = Minecraft.getInstance();
	private static boolean Showing = false;
	public static GuiRadial UI = new GuiRadial();
	public static Vec3d Pos_room = new Vec3d(0,0,0);
	public static Matrix4f Rotation_room = new Matrix4f();
	private static boolean lpl, lps, PointedL, PointedR;

	public static Framebuffer Framebuffer = null;

	private static ControllerType activecontroller;
	private static boolean lastPressedClickL, lastPressedClickR, lastPressedShiftL, lastPressedShiftR;
	
	public static boolean setOverlayShowing(boolean showingState, ControllerType controller) {
		if (Main.kiosk) return false;
		if(mc.vrSettings.seated) showingState = false;
		int ret = 1;
		if (showingState) {		
			int i = mc.mainWindow.getScaledWidth();
			int j = mc.mainWindow.getScaledHeight();
			UI.init(Minecraft.getInstance(), i, j);
			Showing = true;
			activecontroller = controller;
			orientOverlay(activecontroller);
		} else {
			Showing = false;
			activecontroller = null;
		}

		return isShowing();
	}

	public static void processGui() {
		
		PointedL = false;
		PointedR = false;
		
		if(!isShowing()) {
			return;
		}
		if(mc.vrSettings.seated) return;
		if(Rotation_room == null) return;
		
		Vec2f tex1 = GuiHandler.getTexCoordsForCursor(Pos_room, Rotation_room, mc.currentScreen, GuiHandler.guiScale, mc.vrPlayer.vrdata_room_pre.getController(1));
		Vec2f tex2 = GuiHandler.getTexCoordsForCursor(Pos_room, Rotation_room, mc.currentScreen, GuiHandler.guiScale, mc.vrPlayer.vrdata_room_pre.getController(0));
	
		float u = tex2.x;
		float v = tex2.y;
		
		if (u<0 || v<0 || u>1 || v>1)
		{
			// offscreen
			UI.cursorX2 = -1.0f;
			UI.cursorY2 = -1.0f;
			PointedR = false;
		}
		else if (UI.cursorX2 == -1.0f)
		{
			UI.cursorX2 = (int) (u * mc.mainWindow.getWidth());
			UI.cursorY2 = (int) (v * mc.mainWindow.getHeight());
			PointedR = true;
		}
		else
		{
			// apply some smoothing between mouse positions
			float newX = (int) (u * mc.mainWindow.getWidth());
			float newY = (int) (v * mc.mainWindow.getHeight());
			UI.cursorX2 = UI.cursorX2 * 0.7f + newX * 0.3f;
			UI.cursorY2 = UI.cursorY2 * 0.7f + newY * 0.3f;
			PointedR = true;
		}
		
		 u = tex1.x;
		 v = tex1.y;
		
		if (u<0 || v<0 || u>1 || v>1)
		{
			// offscreen
			UI.cursorX1 = -1.0f;
			UI.cursorY1 = -1.0f;
			PointedL = false;
		}
		else if (UI.cursorX1 == -1.0f)
		{
			UI.cursorX1 = (int) (u * mc.mainWindow.getWidth());
			UI.cursorY1 = (int) (v * mc.mainWindow.getHeight());
			PointedL = true;
		}
		else
		{
			// apply some smoothing between mouse positions
			float newX = (int) (u * mc.mainWindow.getWidth());
			float newY = (int) (v * mc.mainWindow.getHeight());
			UI.cursorX1 = UI.cursorX1 * 0.7f + newX * 0.3f;
			UI.cursorY1 = UI.cursorY1 * 0.7f + newY * 0.3f;
			PointedL = true;
		}
	}


	public static void orientOverlay(ControllerType controller) {
		if (!isShowing()) return;

		VRDevicePose pose = mc.vrPlayer.vrdata_room_pre.hmd; //normal menu.
		float dist = 2;
		
		int id=0;
		if(controller == ControllerType.LEFT)
			id=1;

		if(mc.vrSettings.radialModeHold) { //open with controller centered, consistent motions.
			pose = mc.vrPlayer.vrdata_room_pre.getController(id);
			dist = 1.2f;
		}

		Matrix4f matrix = new Matrix4f();

		Vec3d v = pose.getPosition();
		Vec3d adj = new Vec3d(0,0,-dist);
		Vec3d e = pose.getCustomVector(adj);
		Pos_room = new Vec3d(
				(e.x / 2 + v.x),
				(e.y / 2 + v.y),
				(e.z / 2 + v.z));

		Vector3 look = new Vector3();
		look.setX((float) (Pos_room.x - v.x));
		look.setY((float) (Pos_room.y - v.y));
		look.setZ((float) (Pos_room.z - v.z));

		float pitch = (float) Math.asin(look.getY()/look.length());
		float yaw = (float) ((float) Math.PI + Math.atan2(look.getX(), look.getZ()));    
		Rotation_room = Matrix4f.rotationY((float) yaw);
		Matrix4f tilt = OpenVRUtil.rotationXMatrix(pitch);	
		Rotation_room = Matrix4f.multiply(Rotation_room, tilt);	

	}

	public static void processBindings() {
		if (!isShowing()) return;

		if (PointedL && GuiHandler.keyKeyboardShift.isPressed(ControllerType.LEFT)) {
			UI.setShift(true);
			lastPressedShiftL = true;
		}
		if (!GuiHandler.keyKeyboardShift.isKeyDown(ControllerType.LEFT) && lastPressedShiftL) {
			UI.setShift(false);
			lastPressedShiftL = false;
		}

		if (PointedR && GuiHandler.keyKeyboardShift.isPressed(ControllerType.RIGHT)) {
				UI.setShift(true);
			lastPressedShiftR = true;
		}
		if (!GuiHandler.keyKeyboardShift.isKeyDown(ControllerType.RIGHT) && lastPressedShiftR) {
				UI.setShift(false);
			lastPressedShiftR = false;
		}
		
		double d0 = Math.min(Math.max((int) UI.cursorX1, 0), mc.mainWindow.getWidth())
				 * (double)mc.mainWindow.getScaledWidth() / (double)mc.mainWindow.getWidth();
		double d1 = Math.min(Math.max((int) UI.cursorY1, 0), mc.mainWindow.getWidth())
				 * (double)mc.mainWindow.getScaledHeight() / (double)mc.mainWindow.getHeight();
		
		double d2 = Math.min(Math.max((int) UI.cursorX2, 0), mc.mainWindow.getWidth())
				 * (double)mc.mainWindow.getScaledWidth() / (double)mc.mainWindow.getWidth();
		double d3 = Math.min(Math.max((int) UI.cursorY2, 0), mc.mainWindow.getWidth())
				 * (double)mc.mainWindow.getScaledHeight() / (double)mc.mainWindow.getHeight();

		if(mc.vrSettings.radialModeHold) {
			if (activecontroller == null)
				return;
			
			if (!MCOpenVR.keyRadialMenu.isKeyDown()) {
				if (activecontroller == ControllerType.LEFT) {
					UI.mouseClicked((int)d0, (int)d1, 0);
				} else {
					UI.mouseClicked((int)d2, (int)d3, 0);
				}
				RadialHandler.setOverlayShowing(false, null);
			}
			
		} else {
			if (PointedL && GuiHandler.keyKeyboardClick.isPressed(ControllerType.LEFT)) {
					UI.mouseClicked((int)d0, (int)d1, 0);
				lastPressedClickL = true;
			}
			if (!GuiHandler.keyKeyboardClick.isKeyDown(ControllerType.LEFT) && lastPressedClickL) {
					UI.mouseReleased((int)d0, (int)d1, 0);
				lastPressedClickL = false;
			}

			if(PointedR && GuiHandler.keyKeyboardClick.isPressed(ControllerType.RIGHT)) {
					UI.mouseClicked((int)d2, (int)d3, 0);
				lastPressedClickR = true;
			}
			if (!GuiHandler.keyKeyboardClick.isKeyDown(ControllerType.RIGHT) && lastPressedClickR) {
					UI.mouseReleased((int)d2, (int)d3, 0);
				lastPressedClickR = false;
			}
		}
	}

	public static boolean isShowing() {
		return Showing;
	}

	public static boolean isUsingController(ControllerType controller) {
		if (controller == ControllerType.LEFT) {
			return PointedL;
		} else {
			return PointedR;
		}
	}
}
