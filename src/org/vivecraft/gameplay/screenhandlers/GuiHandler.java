package org.vivecraft.gameplay.screenhandlers;

import org.vivecraft.api.VRData.VRDevicePose;
import org.vivecraft.control.ControllerType;
import org.vivecraft.control.HandedKeyBinding;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.render.RenderPass;
import org.vivecraft.settings.VRSettings;
import org.vivecraft.utils.InputSimulator;
import org.vivecraft.utils.OpenVRUtil;
import org.vivecraft.utils.Quaternion;
import org.vivecraft.utils.Vector3;

import com.mojang.blaze3d.platform.GlStateManager;

import org.vivecraft.utils.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.EditBookScreen;
import net.minecraft.client.gui.screen.EditSignScreen;
import net.minecraft.client.gui.screen.EnchantmentScreen;
import net.minecraft.client.gui.screen.HopperScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WinGameScreen;
import net.minecraft.client.gui.screen.inventory.AnvilScreen;
import net.minecraft.client.gui.screen.inventory.BeaconScreen;
import net.minecraft.client.gui.screen.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screen.inventory.ChestScreen;
import net.minecraft.client.gui.screen.inventory.CraftingScreen;
import net.minecraft.client.gui.screen.inventory.DispenserScreen;
import net.minecraft.client.gui.screen.inventory.FurnaceScreen;
import net.minecraft.client.gui.screen.inventory.ShulkerBoxScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class GuiHandler {
	public static Minecraft mc = Minecraft.getInstance();
	//TODO: to hell with all these conversions.
	//sets mouse position for currentscreen

	static boolean lastPressedLeftClick;
	static boolean lastPressedRightClick;
	static boolean lastPressedMiddleClick;
	static boolean lastPressedShift;
	static boolean lastPressedCtrl;
	static boolean lastPressedAlt;

	// For mouse menu emulation
	private static double controllerMouseX = -1.0f;
	private static double controllerMouseY = -1.0f;
	public static boolean controllerMouseValid;
	public static int controllerMouseTicks;

	public static float guiScale = 1.0f;
	public static float guiScaleApplied = 1.0f;
	public static Vec3d IPoint = new Vec3d(0, 0, 0);

	public static Vec3d guiPos_room = new Vec3d(0,0,0);
	public static Matrix4f guiRotation_room = new Matrix4f();

	public static float hudScale = 1.0f;
	public static Vec3d hudPos_room = new Vec3d(0,0,0);
	public static Matrix4f hudRotation_room = new Matrix4f();

	public static final KeyBinding keyLeftClick = new KeyBinding("vivecraft.key.guiLeftClick", GLFW.GLFW_KEY_UNKNOWN, "vivecraft.key.category.gui");
	public static final KeyBinding keyRightClick = new KeyBinding("vivecraft.key.guiRightClick", GLFW.GLFW_KEY_UNKNOWN, "vivecraft.key.category.gui");
	public static final KeyBinding keyMiddleClick = new KeyBinding("vivecraft.key.guiMiddleClick", GLFW.GLFW_KEY_UNKNOWN, "vivecraft.key.category.gui");
	public static final KeyBinding keyShift = new KeyBinding("vivecraft.key.guiShift", GLFW.GLFW_KEY_UNKNOWN, "vivecraft.key.category.gui");
	public static final KeyBinding keyCtrl = new KeyBinding("vivecraft.key.guiCtrl", GLFW.GLFW_KEY_UNKNOWN, "vivecraft.key.category.gui");
	public static final KeyBinding keyAlt = new KeyBinding("vivecraft.key.guiAlt", GLFW.GLFW_KEY_UNKNOWN, "vivecraft.key.category.gui");
	public static final KeyBinding keyScrollUp = new KeyBinding("vivecraft.key.guiScrollUp", GLFW.GLFW_KEY_UNKNOWN, "vivecraft.key.category.gui");
	public static final KeyBinding keyScrollDown = new KeyBinding("vivecraft.key.guiScrollDown", GLFW.GLFW_KEY_UNKNOWN, "vivecraft.key.category.gui");
	public static final KeyBinding keyScrollAxis = new KeyBinding("vivecraft.key.guiScrollAxis", GLFW.GLFW_KEY_UNKNOWN, "vivecraft.key.category.gui"); // dummy binding
	public static final HandedKeyBinding keyKeyboardClick = new HandedKeyBinding("vivecraft.key.keyboardClick", GLFW.GLFW_KEY_UNKNOWN, "vivecraft.key.category.keyboard") {
		@Override
		public boolean isPriorityOnController(ControllerType type) {
			if (KeyboardHandler.Showing && !mc.vrSettings.physicalKeyboard) {
				return KeyboardHandler.isUsingController(type);
			}
			return RadialHandler.isShowing() && RadialHandler.isUsingController(type);
		}
	};
	public static final HandedKeyBinding keyKeyboardShift = new HandedKeyBinding("vivecraft.key.keyboardShift", GLFW.GLFW_KEY_UNKNOWN, "vivecraft.key.category.keyboard") {
		@Override
		public boolean isPriorityOnController(ControllerType type) {
			if (KeyboardHandler.Showing) {
				if (mc.vrSettings.physicalKeyboard)
					return true;
				return KeyboardHandler.isUsingController(type);
			}
			return RadialHandler.isShowing() && RadialHandler.isUsingController(type);
		}
	};

	public static Framebuffer guiFramebuffer = null;


	public static void processGui() {
		if(mc.currentScreen == null)return;
		if(mc.vrSettings.seated) return;
		if(guiRotation_room == null) return;

		Vec2f tex = getTexCoordsForCursor(guiPos_room, guiRotation_room, mc.currentScreen, guiScale, mc.vrPlayer.vrdata_room_pre.getController(0));

		float u = tex.x;
		float v = tex.y;

		if (u<0 || v<0 || u>1 || v>1)
		{
			// offscreen
			controllerMouseX = -1.0f;
			controllerMouseY = -1.0f;
		}
		else if (controllerMouseX == -1.0f)
		{
			controllerMouseX = (int) (u * mc.mainWindow.getWidth());
			controllerMouseY = (int) (v * mc.mainWindow.getHeight());
		}
		else
		{
			// apply some smoothing between mouse positions
			float newX = (int) (u * mc.mainWindow.getWidth());
			float newY = (int) (v * mc.mainWindow.getHeight());
			controllerMouseX = controllerMouseX * 0.7f + newX * 0.3f;
			controllerMouseY = controllerMouseY * 0.7f + newY * 0.3f;
		}

		if (controllerMouseX >= 0 && controllerMouseX < mc.mainWindow.getWidth()
				&& controllerMouseY >=0 && controllerMouseY < mc.mainWindow.getHeight())
		{
			// mouse on screen
			double mouseX = Math.min(Math.max((int) controllerMouseX, 0), mc.mainWindow.getWidth());
			double mouseY = Math.min(Math.max((int) controllerMouseY, 0), mc.mainWindow.getHeight());

			int deltaX = 0;//?
			int deltaY = 0;//?

			if (MCOpenVR.controllerDeviceIndex[MCOpenVR.RIGHT_CONTROLLER] != -1)
			{
				InputSimulator.setMousePos(mouseX, mouseY);
				controllerMouseValid = true;
			}
		} else { //mouse off screen
			if(controllerMouseTicks == 0)
				controllerMouseValid = false;
			if(controllerMouseTicks>0)controllerMouseTicks--;
		}
	}

	public static Vec2f getTexCoordsForCursor(Vec3d guiPos_room, Matrix4f guiRotation_room, Screen screen, float guiScale, VRDevicePose controller) {
	
		Vec3d con = controller.getPosition();
		Vector3 controllerPos = new Vector3(con);

		Vec3d controllerdir = controller.getDirection();
		Vector3 cdir = new Vector3((float)controllerdir.x,(float) controllerdir.y,(float) controllerdir.z);
		Vector3 forward = new Vector3(0,0,1);

		Vector3 guiNormal = guiRotation_room.transform(forward);
		Vector3 guiRight = guiRotation_room.transform(new Vector3(1,0,0));
		Vector3 guiUp = guiRotation_room.transform(new Vector3(0,1,0));
		float guiNormalDotControllerDirection = guiNormal.dot(cdir);
		if (Math.abs(guiNormalDotControllerDirection) > 0.00001f)
		{//pointed normal to the GUI
			float guiWidth = 1.0f;		
			float guiHalfWidth = guiWidth * 0.5f;		
			float guiHeight = 1.0f;	
			float guiHalfHeight = guiHeight * 0.5f;

			Vector3 gp = new Vector3();

			gp.setX((float) (guiPos_room.x));// + interPolatedRoomOrigin.x ) ;
			gp.setY((float) (guiPos_room.y));// + interPolatedRoomOrigin.y ) ;
			gp.setZ((float) (guiPos_room.z));// + interPolatedRoomOrigin.z ) ;

			Vector3 guiTopLeft = gp.subtract(guiUp.divide(1.0f / guiHalfHeight)).subtract(guiRight.divide(1.0f/guiHalfWidth));

			float intersectDist = -guiNormal.dot(controllerPos.subtract(guiTopLeft)) / guiNormalDotControllerDirection;
			if (intersectDist > 0) {
				Vector3 pointOnPlane = controllerPos.add(cdir.divide(1.0f / intersectDist));

				Vector3 relativePoint = pointOnPlane.subtract(guiTopLeft);
				float u = relativePoint.dot(guiRight.divide(1.0f / guiWidth));
				float v = relativePoint.dot(guiUp.divide(1.0f / guiWidth));

				float AR = (float) mc.mainWindow.getScaledHeight() / mc.mainWindow.getScaledWidth();

				u = (u - 0.5f) / 1.5f / guiScale + 0.5f;
				v = (v - 0.5f) / AR / 1.5f / guiScale + 0.5f;

				v = 1 - v;

				return new Vec2f(u, v);
			}
		}
		return new Vec2f(-1, -1);
	}

	public static void processBindingsGui() {
		boolean mouseValid = controllerMouseX >= 0 && controllerMouseX < mc.mainWindow.getWidth()
				&& controllerMouseY >=0 && controllerMouseY < mc.mainWindow.getWidth();

			//This is how the MouseHelper do.
			/*double deltaX = (controllerMouseX - lastMouseX)
			 * (double)mc.mainWindow.getScaledWidth() / (double)mc.mainWindow.getWidth();
			double deltaY = (controllerMouseY - lastMouseY)
			 * (double)mc.mainWindow.getScaledHeight() / (double)mc.mainWindow.getHeight();
			double d0 = Math.min(Math.max((int) controllerMouseX, 0), mc.mainWindow.getWidth())
			 * (double)mc.mainWindow.getScaledWidth() / (double)mc.mainWindow.getWidth();
			double d1 = Math.min(Math.max((int) controllerMouseY, 0), mc.mainWindow.getWidth())
			 * (double)mc.mainWindow.getScaledHeight() / (double)mc.mainWindow.getHeight();*/

				//if (keyLeftClick.isKeyDown() && mc.currentScreen != null)
				//	mc.currentScreen.mouseDragged(d0, d1, 0, deltaX, deltaY);//Signals mouse move

				//LMB
		if (keyLeftClick.isPressed() && mc.currentScreen != null && mouseValid)
				{ //press left mouse button
					//if (Display.isActive())
					//	KeyboardSimulator.robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
					//else
					//	mc.currentScreen.mouseClicked(d0, d1, 0);
					InputSimulator.pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
					lastPressedLeftClick = true;
				}	

		if (!keyLeftClick.isKeyDown() && lastPressedLeftClick) {
					//release left mouse button
					//if (Display.isActive()) KeyboardSimulator.robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					//else 
					//	mc.currentScreen.mouseReleased(d0, d1, 0);
					InputSimulator.releaseMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
					lastPressedLeftClick = false;
				}
				//end LMB

				//RMB
		if (keyRightClick.isPressed() && mc.currentScreen != null && mouseValid) {
					//press right mouse button
					//if (Display.isActive()) KeyboardSimulator.robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
					//else 
					//	mc.currentScreen.mouseClicked(d0, d1, 1);
					InputSimulator.pressMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
					lastPressedRightClick = true;
				}	

				//if (keyRightClick.isKeyDown() && mc.currentScreen != null)
				//	mc.currentScreen.mouseDragged(d0, d1, 0, deltaX, deltaY);//Signals mouse move

		if (!keyRightClick.isKeyDown() && lastPressedRightClick) {
					//release right mouse button
					//if (Display.isActive())
					//	KeyboardSimulator.robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
					//else
					//	mc.currentScreen.mouseReleased(d0, d1, 1);
					InputSimulator.releaseMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
					lastPressedRightClick = false;
				}	
				//end RMB	

				//MMB
		if (keyMiddleClick.isPressed() && mc.currentScreen != null && mouseValid) {
					//press middle mouse button
					//if (Display.isActive())
					//	KeyboardSimulator.robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
					//else
					//	mc.currentScreen.mouseClicked(d0, d1, 2);
					InputSimulator.pressMouse(GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
					lastPressedMiddleClick = true;
				}	

				//if (keyMiddleClick.isKeyDown() && mc.currentScreen != null)
				//	mc.currentScreen.mouseDragged(d0, d1, 0, deltaX, deltaY);//Signals mouse move

		if (!keyMiddleClick.isKeyDown() && lastPressedMiddleClick) {
					//release middle mouse button
					//if (Display.isActive())
					//	KeyboardSimulator.robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
					//else 
					//	mc.currentScreen.mouseReleased(d0, d1, 2);
					InputSimulator.releaseMouse(GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
					lastPressedMiddleClick = false;
				}	
				//end MMB

		//lastMouseX = controllerMouseX;
		//lastMouseY = controllerMouseY;

			//Shift
		if (keyShift.isPressed() && mc.currentScreen != null)
			{
				//press Shift
				//if (mc.currentScreen != null) mc.currentScreen.pressShiftFake = true;
				//if (Display.isActive()) KeyboardSimulator.robot.keyPress(KeyEvent.VK_SHIFT);
				InputSimulator.pressKey(GLFW.GLFW_KEY_LEFT_SHIFT);
				lastPressedShift = true;
			}


			if (!keyShift.isKeyDown() && lastPressedShift)
			{
				//release Shift
				//if (mc.currentScreen != null) mc.currentScreen.pressShiftFake = false;
				//if (Display.isActive()) KeyboardSimulator.robot.keyRelease(KeyEvent.VK_SHIFT);
				InputSimulator.releaseKey(GLFW.GLFW_KEY_LEFT_SHIFT);
				lastPressedShift = false;
			}	
			//end Shift

			//Ctrl
		if (keyCtrl.isPressed() && mc.currentScreen != null)
			{
				//press Ctrl
				//if (Display.isActive()) KeyboardSimulator.robot.keyPress(KeyEvent.VK_CONTROL);
				InputSimulator.pressKey(GLFW.GLFW_KEY_LEFT_CONTROL);
				lastPressedCtrl = true;
			}


			if (!keyCtrl.isKeyDown() && lastPressedCtrl)
			{
				//release Ctrl
				//if (Display.isActive()) KeyboardSimulator.robot.keyRelease(KeyEvent.VK_CONTROL);
				InputSimulator.releaseKey(GLFW.GLFW_KEY_LEFT_CONTROL);
				lastPressedCtrl = false;
			}	
			//end Ctrl

			//Alt
		if (keyAlt.isPressed() && mc.currentScreen != null)
			{
				//press Alt
				//if (Display.isActive()) KeyboardSimulator.robot.keyPress(KeyEvent.VK_ALT);
				InputSimulator.pressKey(GLFW.GLFW_KEY_LEFT_ALT);
				lastPressedAlt = true;
			}


			if (!keyAlt.isKeyDown() && lastPressedAlt)
			{
				//release Alt
				//if (Display.isActive()) KeyboardSimulator.robot.keyRelease(KeyEvent.VK_ALT);
				InputSimulator.releaseKey(GLFW.GLFW_KEY_LEFT_ALT);
				lastPressedAlt = false;
			}	
			//end Alt

		if (keyScrollUp.isPressed() && mc.currentScreen != null) {
			InputSimulator.scrollMouse(0, 4);
		}

		if (keyScrollDown.isPressed() && mc.currentScreen != null) {
			InputSimulator.scrollMouse(0, -4);
		}
	}

	
	
	public static void onScreenChanged(Screen previousGuiScreen, Screen newScreen, boolean unpressKeys)
	{
		/*if(unpressKeys){
			for (VRButtonMapping mapping : mc.vrSettings.buttonMappings.values()) {
				if(newScreen!=null) {
					if(mapping.isGUIBinding() && mapping.keyBinding != mc.gameSettings.keyBindInventory)
						mapping.actuallyUnpress();
				} else
					mapping.actuallyUnpress();
			}
		}*/
		if (unpressKeys)
			MCOpenVR.ignorePressesNextFrame = true;
						
		if(newScreen == null) {
			//just insurance
			guiPos_room = null;
			guiRotation_room = null;
			guiScale=1;
			if(KeyboardHandler.keyboardForGui)
				KeyboardHandler.setOverlayShowing(false);
		} else {
			RadialHandler.setOverlayShowing(false, null);
		}

		if (mc.world == null || newScreen instanceof WinGameScreen) {
			mc.vrSettings.vrWorldRotationCached = mc.vrSettings.vrWorldRotation;
			mc.vrSettings.vrWorldRotation = 0;
		} else { //these dont update when screen open.
			if (mc.vrSettings.vrWorldRotationCached != 0) {
				mc.vrSettings.vrWorldRotation = mc.vrSettings.vrWorldRotationCached;
				mc.vrSettings.vrWorldRotationCached = 0;
			}
		}

		boolean staticScreen = mc.gameRenderer == null || mc.gameRenderer.isInMenuRoom();
		staticScreen &= !mc.vrSettings.seated && !mc.vrSettings.menuAlwaysFollowFace;
		//staticScreen |= mc.load

		if (staticScreen) {
			//TODO reset scale things
			guiScale = 2.0f;
			float[] playArea = MCOpenVR.getPlayAreaSize();
			guiPos_room = new Vec3d(
					(float) (0),
					(float) (1.3f),
					(float) (playArea != null ? -playArea[1] / 2f : -1.5f));			

			guiRotation_room = new Matrix4f();
			guiRotation_room.M[0][0] = guiRotation_room.M[1][1] = guiRotation_room.M[2][2] = guiRotation_room.M[3][3] = 1.0F;
			guiRotation_room.M[0][1] = guiRotation_room.M[1][0] = guiRotation_room.M[2][3] = guiRotation_room.M[3][1] = 0.0F;
			guiRotation_room.M[0][2] = guiRotation_room.M[1][2] = guiRotation_room.M[2][0] = guiRotation_room.M[3][2] = 0.0F;
			guiRotation_room.M[0][3] = guiRotation_room.M[1][3] = guiRotation_room.M[2][1] = guiRotation_room.M[3][0] = 0.0F;

			return;
		}

		if((previousGuiScreen==null && newScreen != null) || (newScreen instanceof ChatScreen || newScreen instanceof EditBookScreen || newScreen instanceof EditSignScreen))		
		{
			Quaternion controllerOrientationQuat;
			boolean appearOverBlock = (newScreen instanceof CraftingScreen)
					|| (newScreen instanceof ChestScreen)
					|| (newScreen instanceof ShulkerBoxScreen)
					|| (newScreen instanceof HopperScreen)
					|| (newScreen instanceof FurnaceScreen)
					|| (newScreen instanceof BrewingStandScreen)
					|| (newScreen instanceof BeaconScreen)
					|| (newScreen instanceof DispenserScreen)
					|| (newScreen instanceof EnchantmentScreen)
					|| (newScreen instanceof AnvilScreen)
					;

			if(appearOverBlock && mc.objectMouseOver != null && mc.objectMouseOver.getType() == RayTraceResult.Type.BLOCK){	
				//appear over block.
				BlockRayTraceResult hit = (BlockRayTraceResult) mc.objectMouseOver;
				Vec3d temp =new Vec3d(hit.getPos().getX() + 0.5f,
						hit.getPos().getY(),
						hit.getPos().getZ() + 0.5f);

				Vec3d temp_room = mc.vrPlayer.world_to_room_pos(temp, mc.vrPlayer.vrdata_world_pre);			
				Vec3d pos = mc.vrPlayer.vrdata_room_pre.hmd.getPosition();

				double dist = temp_room.subtract(pos).length();
				guiScale = (float) Math.sqrt(dist);

				//idk it works.
				Vec3d guiPosWorld = new Vec3d(temp.x, hit.getPos().getY() + 1.1 + (0.5f * guiScale/2), temp.z);

				guiPos_room = mc.vrPlayer.world_to_room_pos(guiPosWorld, mc.vrPlayer.vrdata_world_pre);	

				Vector3 look = new Vector3();
				look.setX((float) (guiPos_room.x - pos.x));
				look.setY((float) (guiPos_room.y - pos.y));
				look.setZ((float) (guiPos_room.z - pos.z));

				float pitch = (float) Math.asin(look.getY()/look.length());
				float yaw = (float) ((float) Math.PI + Math.atan2(look.getX(), look.getZ()));    
				guiRotation_room = Matrix4f.rotationY((float) yaw);
				Matrix4f tilt = OpenVRUtil.rotationXMatrix(pitch);	
				guiRotation_room = Matrix4f.multiply(guiRotation_room,tilt);		

			}				
			else{
				//static screens like menu, inventory, and dead.
				Vec3d adj = new Vec3d(0,0,-2);
				if (newScreen instanceof ChatScreen){
					adj = new Vec3d(0,0.5,-2);
				} else if (newScreen instanceof EditBookScreen || newScreen instanceof EditSignScreen) {
					adj = new Vec3d(0,0.25,-2);
				}

				Vec3d v = mc.vrPlayer.vrdata_room_pre.hmd.getPosition();
				Vec3d e = mc.vrPlayer.vrdata_room_pre.hmd.getCustomVector(adj);
				guiPos_room = new Vec3d(
						(e.x  / 2 + v.x),
						(e.y / 2 + v.y),
						(e.z / 2 + v.z));

				Vec3d pos = mc.vrPlayer.vrdata_room_pre.hmd.getPosition();
				Vector3 look = new Vector3();
				look.setX((float) (guiPos_room.x - pos.x));
				look.setY((float) (guiPos_room.y - pos.y));
				look.setZ((float) (guiPos_room.z - pos.z));

				float pitch = (float) Math.asin(look.getY()/look.length());
				float yaw = (float) ((float) Math.PI + Math.atan2(look.getX(), look.getZ()));    
				guiRotation_room = Matrix4f.rotationY((float) yaw);
				Matrix4f tilt = OpenVRUtil.rotationXMatrix(pitch);	
				guiRotation_room = Matrix4f.multiply(guiRotation_room,tilt);		

			}
		}

		KeyboardHandler.orientOverlay(newScreen!=null);

	}

  	public static Vec3d applyGUIModelView(RenderPass currentPass)
  	{
  		mc.getProfiler().startSection("applyGUIModelView");

  		Vec3d eye =mc.vrPlayer.vrdata_world_render.getEye(currentPass).getPosition();

  		if(mc.currentScreen != null && GuiHandler.guiPos_room == null){
  			//naughty mods!
  			GuiHandler.onScreenChanged(null, mc.currentScreen, false);			
  		}

  		Vec3d guipos = GuiHandler.guiPos_room;
  		Matrix4f guirot = GuiHandler.guiRotation_room;
  		Vec3d guiLocal = new Vec3d(0, 0, 0);		
  		float scale = GuiHandler.guiScale;

  		if(guipos == null){
  			guirot = null;
  			scale = 1;
  			if (mc.world!=null && (mc.currentScreen==null || mc.vrSettings.floatInventory == false))
  			{  // HUD view - attach to head or controller
  				int i = 1;
  				if (mc.vrSettings.vrReverseHands) i = -1;

  				//					if(currentPass != renderPass.Third)
  				//						eye = mc.vrPlayer.getEyePos_World(currentPass); //dont need interpolation.
  				//					else {
  				//						mc.getMRTransform(false);
  				//						eye = mc.vrPlayer.vrdata_world_render.getEye(mc.currentPass).getPosition();
  				//					}
  				if (mc.vrSettings.seated || mc.vrSettings.vrHudLockMode == VRSettings.HUD_LOCK_HEAD)
  				{
  					Matrix4f rot = Matrix4f.rotationY((float)mc.vrPlayer.vrdata_world_render.rotation_radians);
  					Matrix4f max = Matrix4f.multiply(rot, MCOpenVR.hmdRotation);

  					Vec3d v = mc.vrPlayer.vrdata_world_render.hmd.getPosition();
  					Vec3d d = mc.vrPlayer.vrdata_world_render.hmd.getDirection();

  					if(mc.vrSettings.seated && mc.vrSettings.seatedHudAltMode){
  						d = mc.vrPlayer.vrdata_world_render.getController(0).getDirection();
  						max = Matrix4f.multiply(rot, MCOpenVR.getAimRotation(0));
  					}

  					guipos = new Vec3d((v.x + d.x*mc.vrPlayer.vrdata_world_render.worldScale*mc.vrSettings.hudDistance),
  							(v.y + d.y*mc.vrPlayer.vrdata_world_render.worldScale*mc.vrSettings.hudDistance),
  							(v.z + d.z*mc.vrPlayer.vrdata_world_render.worldScale*mc.vrSettings.hudDistance));


  					Quaternion orientationQuat = OpenVRUtil.convertMatrix4ftoRotationQuat(max);

  					guirot = new Matrix4f(orientationQuat);

  					//float pitchOffset = (float) Math.toRadians( -mc.vrSettings.hudPitchOffset );
  					//float yawOffset = (float) Math.toRadians( -mc.vrSettings.hudYawOffset );
  					//guiRotationPose = Matrix4f.multiply(guiRotationPose, OpenVRUtil.rotationXMatrix(yawOffset));
  					//guiRotationPose = Matrix4f.multiply(guiRotationPose, Matrix4f.rotationY(pitchOffset));
  					//guirot.M[3][3] = 1.0f;

  					scale = mc.vrSettings.hudScale;

  				}else if (mc.vrSettings.vrHudLockMode == VRSettings.HUD_LOCK_HAND)//hud on hand
  				{
  					Matrix4f out = MCOpenVR.getAimRotation(1);
  					Matrix4f rot = Matrix4f.rotationY((float) mc.vrPlayer.vrdata_world_render.rotation_radians);
  					Matrix4f MguiRotationPose =  Matrix4f.multiply(rot,out);
  					//	MguiRotationPose.M[1][3] = 0.5f;
  					//guiRotationPose = mc.vrPlayer.getControllerMatrix_World(1);
  					guirot = Matrix4f.multiply(MguiRotationPose, OpenVRUtil.rotationXMatrix((float) Math.PI * -0.2F));
  					guirot = Matrix4f.multiply(guirot, Matrix4f.rotationY((float) Math.PI * 0.1F * i));
  					scale = 1/1.7f;
  					//guirot.M[3][3] = 1.7f;

  					guiLocal = new Vec3d(guiLocal.x, 0.32*mc.vrPlayer.vrdata_world_render.worldScale,guiLocal.z);

  					guipos = mc.gameRenderer.getControllerRenderPos(1);

  					MCOpenVR.hudPopup = true;

  				}
  				else if (mc.vrSettings.vrHudLockMode == VRSettings.HUD_LOCK_WRIST)//hud on wrist
  				{

  					Matrix4f out = MCOpenVR.getAimRotation(1);
  					Matrix4f rot = Matrix4f.rotationY((float) mc.vrPlayer.vrdata_world_render.rotation_radians);
  					guirot =  Matrix4f.multiply(rot,out);

                    guirot = Matrix4f.multiply(guirot, OpenVRUtil.rotationZMatrix((float)Math.PI * 0.5f * i));
  					guirot = Matrix4f.multiply(guirot, Matrix4f.rotationY((float) Math.PI * 0.3f *i));

                    guipos = mc.gameRenderer.getControllerRenderPos(1);

  					/*Vector3 forward = new Vector3(0,0,1);
  					Vector3 guiNormal = guirot.transform(forward);

  					Vec3d facev = mc.vrPlayer.vrdata_world_render.hmd.getDirection();
  					Vector3 face = new Vector3((float)facev.x, (float)facev.y, (float)facev.z);

                    float dot = face.dot(guiNormal);

  					Vec3d head = mc.vrPlayer.vrdata_world_render.hmd.getPosition();

  					Vector3 headv = new Vector3((float)guipos.x, (float)guipos.y, (float)guipos.z).subtract(new Vector3((float)head.x, (float)head.y, (float)head.z)).normalised();
  					if(headv == null) return guipos;
  					float dot2 = (float) headv.dot(guiNormal);

  					if(MCOpenVR.hudPopup){
  						MCOpenVR.hudPopup = Math.abs(dot2) > 0.7 &&  dot < -0.8;
  					}else {
  						MCOpenVR.hudPopup = Math.abs(dot2) > 0.9 &&  dot < -0.97;
  					}*/

                    MCOpenVR.hudPopup = true;

                    boolean slim = mc.player.getSkinType().equals("slim");

  					/*if(MCOpenVR.hudPopup){
  						scale = .5f;
  						guiLocal = new Vec3d(
  								-0.005*mc.vrPlayer.vrdata_world_render.worldScale,
  								0.16*mc.vrPlayer.vrdata_world_render.worldScale,
  								0.19*mc.vrPlayer.vrdata_world_render.worldScale);
  					}else {*/
  						scale = 0.4f;
  						guiLocal = new Vec3d(
  								i*-0.136f*mc.vrPlayer.vrdata_world_render.worldScale,
								(slim ? 0.135 : 0.125)*mc.vrPlayer.vrdata_world_render.worldScale,
                                0.06*mc.vrPlayer.vrdata_world_render.worldScale);
  						guirot = Matrix4f.multiply(guirot, Matrix4f.rotationY((float) Math.PI * 0.2f*i ));
  					//}
  				}
  			} 
  		} else {
  			//convert previously calculated coords to world coords
  			guipos = mc.vrPlayer.room_to_world_pos(guipos, mc.vrPlayer.vrdata_world_render);
  			Matrix4f rot = Matrix4f.rotationY(mc.vrPlayer.vrdata_world_render.rotation_radians);
  			guirot = Matrix4f.multiply(rot, guirot);
  		}


  		// otherwise, looking at inventory screen. use pose calculated when screen was opened
  		//where is this set up... should be here....

  		if ((mc.vrSettings.seated || mc.vrSettings.menuAlwaysFollowFace) && mc.gameRenderer.isInMenuRoom()){ //|| mc.vrSettings.vrHudLockMode == VRSettings.HUD_LOCK_BODY) {

  			//main menu slow yaw tracking thing
  			scale = 2;

  			Vec3d posAvg = new Vec3d(0, 0, 0);
  			for (Vec3d vec : mc.hmdPosSamples) {
  				posAvg = new Vec3d(posAvg.x + vec.x, posAvg.y + vec.y, posAvg.z + vec.z);
  			}
  			posAvg = new Vec3d(posAvg.x / mc.hmdPosSamples.size(), posAvg.y / mc.hmdPosSamples.size(), posAvg.z / mc.hmdPosSamples.size());

  			float yawAvg = 0;
  			for (float f : mc.hmdYawSamples) {
  				yawAvg += f;
  			}
  			yawAvg /= mc.hmdYawSamples.size();
  			yawAvg = (float)Math.toRadians(yawAvg);

  			Vec3d dir = new Vec3d(-Math.sin(yawAvg), 0, Math.cos(yawAvg));
  			float dist = mc.gameRenderer.isInMenuRoom() ? 2.5F*mc.vrPlayer.vrdata_world_render.worldScale: mc.vrSettings.hudDistance;
  			Vec3d pos = posAvg.add(new Vec3d(dir.x * dist, dir.y * dist, dir.z * dist));
  			Vec3d gpr = new Vec3d(pos.x, pos.y, pos.z);

  			Matrix4f gr = Matrix4f.rotationY(135-yawAvg); // don't ask

  			guirot = Matrix4f.multiply(gr, Matrix4f.rotationY(mc.vrPlayer.vrdata_world_render.rotation_radians));
  			guipos = mc.vrPlayer.room_to_world_pos(gpr, mc.vrPlayer.vrdata_world_render);

  			//for mouse control
  			GuiHandler.guiRotation_room = gr;
  			GuiHandler.guiScale=2;
  			GuiHandler.guiPos_room = gpr;
  			//
  		}

  		// counter head rotation
  		if (currentPass != RenderPass.THIRD) {
  			GL11.glMultMatrixf(mc.vrPlayer.vrdata_world_render.getEye(currentPass).getMatrix().toFloatBuffer());
  		} else {
  			mc.gameRenderer.applyMRCameraRotation(false);			
  		}


  		GL11.glTranslatef((float) (guipos.x - eye.x), (float)(guipos.y - eye.y), (float)(guipos.z - eye.z));
  		//  		
  		//  			// offset from eye to gui pos
  		GL11.glMultMatrixf(guirot.transposed().toFloatBuffer());
  		GL11.glTranslatef((float)guiLocal.x, (float) guiLocal.y, (float)guiLocal.z);

  		float thescale = scale * mc.vrPlayer.vrdata_world_render.worldScale; // * this.mc.vroptions.hudscale
  		GlStateManager.scalef(thescale, thescale, thescale);
  		GuiHandler.guiScaleApplied = thescale;
  		//double timeOpen = getCurrentTimeSecs() - startedOpeningInventory;


  		//		if (timeOpen < 1.5) {
  		//			scale = (float)(Math.sin(Math.PI*0.5*timeOpen/1.5));
  		//		}

  		mc.getProfiler().endSection();

  		return guipos;
  	}

}
