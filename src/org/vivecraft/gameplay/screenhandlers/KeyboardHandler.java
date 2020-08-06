package org.vivecraft.gameplay.screenhandlers;

import org.vivecraft.control.ControllerType;
import org.vivecraft.gui.GuiKeyboard;
import org.vivecraft.gui.PhysicalKeyboard;
import org.vivecraft.provider.OpenVRUtil;
import org.vivecraft.utils.Utils;
import org.vivecraft.utils.math.Matrix4f;
import org.vivecraft.utils.math.Vector3;

import net.minecraft.client.Minecraft;
import net.minecraft.client.main.Main;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;

public class KeyboardHandler {
	//keyboard
	public static Minecraft mc = Minecraft.getInstance();
	public static boolean Showing = false;
	public static GuiKeyboard UI = new GuiKeyboard();
	public static PhysicalKeyboard physicalKeyboard = new PhysicalKeyboard();
	public static Vector3d Pos_room = new Vector3d(0,0,0);
	public static Matrix4f Rotation_room = new Matrix4f();
	private static boolean lpl, lps, PointedL, PointedR;
	public static boolean keyboardForGui;
	public static Framebuffer Framebuffer = null;
	private static boolean lastPressedClickL, lastPressedClickR, lastPressedShift;

	public static boolean setOverlayShowing(boolean showingState) {
		if (Main.kiosk) return false;
		if(mc.vrSettings.seated) showingState = false;
		int ret = 1;
		if (showingState) {		
            int i = mc.getMainWindow().getScaledWidth();
            int j = mc.getMainWindow().getScaledHeight();          
            if (mc.vrSettings.physicalKeyboard)
				physicalKeyboard.show();
            else
            	UI.init(Minecraft.getInstance(), i, j);
			Showing = true;
      		orientOverlay(mc.currentScreen!=null);
      		RadialHandler.setOverlayShowing(false, null);
      		if (mc.vrSettings.physicalKeyboard && mc.currentScreen != null)
				GuiHandler.onScreenChanged(mc.currentScreen, mc.currentScreen, false);
		} else {
			Showing = false;
		}
		return Showing;
	}

	public static void processGui() {
	
		PointedL = false;
		PointedR = false;
		
		if(!Showing) {
			return;
		}
		if(mc.vrSettings.seated) return;
		if(Rotation_room == null) return;

		if (mc.vrSettings.physicalKeyboard) {
			physicalKeyboard.process();
			return; // Skip the rest of this
		}
		
		Vector2f tex1 = GuiHandler.getTexCoordsForCursor(Pos_room, Rotation_room, mc.currentScreen, GuiHandler.guiScale, mc.vrPlayer.vrdata_room_pre.getController(1));
		Vector2f tex2 = GuiHandler.getTexCoordsForCursor(Pos_room, Rotation_room, mc.currentScreen, GuiHandler.guiScale, mc.vrPlayer.vrdata_room_pre.getController(0));
	
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
			UI.cursorX2 = (int) (u * mc.getMainWindow().getWidth());
			UI.cursorY2 = (int) (v * mc.getMainWindow().getHeight());
			PointedR = true;
		}
		else
		{
			// apply some smoothing between mouse positions
			float newX = (int) (u * mc.getMainWindow().getWidth());
			float newY = (int) (v * mc.getMainWindow().getHeight());
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
			UI.cursorX1 = (int) (u * mc.getMainWindow().getWidth());
			UI.cursorY1 = (int) (v * mc.getMainWindow().getHeight());
			PointedL = true;
		}
		else
		{
			// apply some smoothing between mouse positions
			float newX = (int) (u * mc.getMainWindow().getWidth());
			float newY = (int) (v * mc.getMainWindow().getHeight());
			UI.cursorX1 = UI.cursorX1 * 0.7f + newX * 0.3f;
			UI.cursorY1 = UI.cursorY1 * 0.7f + newY * 0.3f;
			PointedL = true;
		}
	}

	
	public static void orientOverlay(boolean guiRelative) {
		
		keyboardForGui = false;
		if (!Showing) return;
		keyboardForGui = guiRelative;
		
		org.vivecraft.utils.lwjgl.Matrix4f matrix = new org.vivecraft.utils.lwjgl.Matrix4f();
		if (mc.vrSettings.physicalKeyboard) {
			Vector3d pos = mc.vrPlayer.vrdata_room_pre.hmd.getPosition();
			Vector3d offset = new Vector3d(0, -0.5, 0.3);
			offset = offset.rotateYaw((float)Math.toRadians(-mc.vrPlayer.vrdata_room_pre.hmd.getYaw()));
			Pos_room = new Vector3d(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z);

			float yaw = (float)Math.PI + (float)Math.toRadians(-mc.vrPlayer.vrdata_room_pre.hmd.getYaw());
			Rotation_room = Matrix4f.rotationY(yaw);
			Rotation_room = Matrix4f.multiply(Rotation_room, OpenVRUtil.rotationXMatrix((float)Math.PI * 0.8f));
		} else if (guiRelative && GuiHandler.guiRotation_room != null) {
			org.vivecraft.utils.lwjgl.Matrix4f guiRot = Utils.convertOVRMatrix(GuiHandler.guiRotation_room);
			Vector3d guiUp = new Vector3d(guiRot.m10, guiRot.m11, guiRot.m12);
			Vector3d guiFwd = new Vector3d(guiRot.m20, guiRot.m21, guiRot.m22).scale(0.25f);
			guiUp = guiUp.scale(0.80f);
			matrix.translate(new org.vivecraft.utils.lwjgl.Vector3f((float)(GuiHandler.guiPos_room.x - guiUp.x), (float)(GuiHandler.guiPos_room.y - guiUp.y), (float)(GuiHandler.guiPos_room.z - guiUp.z)));
			matrix.translate(new org.vivecraft.utils.lwjgl.Vector3f((float)(guiFwd.x), (float)(guiFwd.y), (float)(guiFwd.z)));
			org.vivecraft.utils.lwjgl.Matrix4f.mul(matrix, guiRot, matrix);
			matrix.rotate((float)Math.toRadians(30), new org.vivecraft.utils.lwjgl.Vector3f(-1, 0, 0)); // tilt it a bit
			Rotation_room =   Utils.convertToOVRMatrix(matrix);
			Pos_room = new Vector3d(Rotation_room.M[0][3],Rotation_room.M[1][3],Rotation_room.M[2][3]);
			Rotation_room.M[0][3] = 0;
			Rotation_room.M[1][3] = 0;
			Rotation_room.M[2][3] = 0;

		} else { //copied from vrplayer.onguiuscreenchanged
			Vector3d v = mc.vrPlayer.vrdata_room_pre.hmd.getPosition();
			Vector3d adj = new Vector3d(0,-0.5,-2);
			Vector3d e = mc.vrPlayer.vrdata_room_pre.hmd.getCustomVector(adj);
			Pos_room = new Vector3d(
					(e.x  / 2 + v.x),
					(e.y / 2 + v.y),
					(e.z / 2 + v.z));

			Vector3d pos = mc.vrPlayer.vrdata_room_pre.hmd.getPosition();
			Vector3 look = new Vector3();
			look.setX((float) (Pos_room.x - pos.x));
			look.setY((float) (Pos_room.y - pos.y));
			look.setZ((float) (Pos_room.z - pos.z));

			float pitch = (float) Math.asin(look.getY()/look.length());
			float yaw = (float) ((float) Math.PI + Math.atan2(look.getX(), look.getZ()));    
			Rotation_room = Matrix4f.rotationY((float) yaw);
		}
	}
	
	public static void processBindings() {
		if (Showing) {
			if (mc.vrSettings.physicalKeyboard) {
				physicalKeyboard.processBindings();
				return;
			}

			double d0 = Math.min(Math.max((int) UI.cursorX1, 0), mc.getMainWindow().getWidth())
					 * (double)mc.getMainWindow().getScaledWidth() / (double)mc.getMainWindow().getWidth();
			double d1 = Math.min(Math.max((int) UI.cursorY1, 0), mc.getMainWindow().getWidth())
					 * (double)mc.getMainWindow().getScaledHeight() / (double)mc.getMainWindow().getHeight();
			
			
			if (PointedL && GuiHandler.keyKeyboardClick.isPressed(ControllerType.LEFT)) {
					UI.mouseClicked((int)d0, (int)d1, 0);
				lastPressedClickL = true;
			}
			if (!GuiHandler.keyKeyboardClick.isKeyDown(ControllerType.LEFT) && lastPressedClickL) {
					UI.mouseReleased((int)d0, (int)d1, 0);
				lastPressedClickL = false;
			}

			d0 = Math.min(Math.max((int) UI.cursorX2, 0), mc.getMainWindow().getWidth())
					 * (double)mc.getMainWindow().getScaledWidth() / (double)mc.getMainWindow().getWidth();
			d1 = Math.min(Math.max((int) UI.cursorY2, 0), mc.getMainWindow().getWidth())
					 * (double)mc.getMainWindow().getScaledHeight() / (double)mc.getMainWindow().getHeight();
			
			if (PointedR && GuiHandler.keyKeyboardClick.isPressed(ControllerType.RIGHT)) {
					UI.mouseClicked((int)d0, (int)d1, 0);
				lastPressedClickR = true;
			}
			if (!GuiHandler.keyKeyboardClick.isKeyDown(ControllerType.RIGHT) && lastPressedClickR) {
					UI.mouseReleased((int)d0, (int)d1, 0);
				lastPressedClickR = false;
			}

			if (GuiHandler.keyKeyboardShift.isPressed()) {
				UI.setShift(true);
				lastPressedShift = true;
				}
			if (!GuiHandler.keyKeyboardShift.isKeyDown() && lastPressedShift) {
				UI.setShift(false);
				lastPressedShift = false;
			}
			}
		}

	public static boolean isUsingController(ControllerType type) {
		if (type == ControllerType.LEFT)
			return PointedL;
		else
			return PointedR;
	}
}
