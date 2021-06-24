package org.vivecraft.provider;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.glfw.GLFW;
import org.vivecraft.api.VRData;
import org.vivecraft.api.Vec3History;
import org.vivecraft.gameplay.screenhandlers.GuiHandler;
import org.vivecraft.gameplay.screenhandlers.KeyboardHandler;
import org.vivecraft.gameplay.screenhandlers.RadialHandler;
import org.vivecraft.menuworlds.MenuWorldExporter;
import org.vivecraft.provider.openvr_jna.VRInputAction;
import org.vivecraft.provider.openvr_jna.control.VRInputActionSet;
import org.vivecraft.provider.openvr_jna.control.VivecraftMovementInput;
import org.vivecraft.reflection.MCReflection;
import org.vivecraft.render.RenderPass;
import org.vivecraft.settings.VRHotkeys;
import org.vivecraft.settings.VRSettings;
import org.vivecraft.utils.LangHelper;
import org.vivecraft.utils.Utils;
import org.vivecraft.utils.math.Matrix4f;
import org.vivecraft.utils.math.Quaternion;
import org.vivecraft.utils.math.Vector3;

import net.minecraft.block.TorchBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.WinGameScreen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.main.Main;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.optifine.Lang;
import net.optifine.reflect.Reflector;

public abstract class MCVR {

	public MCVR(Minecraft mc) {
		super();
		this.mc = mc;
		me = this;
		
		for (int c=0;c<3;c++)
		{
			aimSource[c] = new Vector3d(0.0D, 0.0D, 0.0D);
			controllerPose[c] = new Matrix4f();
			controllerRotation[c] = new Matrix4f();
			handRotation[c] = new Matrix4f();
		}
	}
	public static MCVR get() {
		return me;
	}
	protected Minecraft mc;
	protected static MCVR me;
	
	protected Matrix4f hmdPose = new Matrix4f();
	public Matrix4f hmdRotation = new Matrix4f();
	public HardwareType detectedHardware = HardwareType.VIVE;

	protected Matrix4f hmdPoseLeftEye = new Matrix4f();
	protected Matrix4f hmdPoseRightEye = new Matrix4f();
	public Vec3History hmdHistory = new Vec3History();
	public Vec3History hmdPivotHistory = new Vec3History();
	protected boolean headIsTracking;

	protected Matrix4f[] controllerPose = new Matrix4f[3];
	protected Matrix4f[] controllerRotation = new Matrix4f[3];
	protected boolean[] controllerTracking = new boolean[3];
	protected Matrix4f[] handRotation = new Matrix4f[3];

	public Vec3History[] controllerHistory = new Vec3History[] { new Vec3History(), new Vec3History()};
	public Vec3History[] controllerForwardHistory = new Vec3History[] { new Vec3History(), new Vec3History()};
	public Vec3History[] controllerUpHistory = new Vec3History[] { new Vec3History(), new Vec3History()};

	protected double gunAngle = 0;
	protected boolean gunStyle;

	public boolean initialized;
	public String initStatus;
	public boolean initSuccess;

	protected Matrix4f[] poseMatrices;
	protected Vector3d[] deviceVelocity;
	protected Vector3d[] aimSource = new Vector3d[3];
	public Vector3 offset=new Vector3(0,0,0);
	public Vector3 forward = new Vector3(0,0,-1);
	public Vector3 up = new Vector3(0,1,0);
	//hmd sampling
	public int hmdAvgLength = 90;
	public LinkedList<Vector3d> hmdPosSamples = new LinkedList<Vector3d>();
	public LinkedList<Float> hmdYawSamples = new LinkedList<Float>();
	protected float hmdYawTotal;
	protected float hmdYawLast;
	protected boolean trigger;

	public boolean mrMovingCamActive;
	public Vector3d mrControllerPos = Vector3d.ZERO;
	public float mrControllerPitch;
	public float mrControllerYaw;
	public float mrControllerRoll;
	protected HapticScheduler hapticScheduler;

	//seated
	public float seatedRot;
	public float aimPitch = 0; 
	//
	
	//Covid-19 Quarantine Helper Code
	protected final Matrix4f Neutral_HMD = new Matrix4f(1,0,0,0f,			0,1,0,1.62f,			0,0,1,0f,			0,0,0,1);
	protected final Matrix4f TPose_Left = new Matrix4f(1,0,0,.25f,			0,1,0,1.62f,			0,0,1,.25f,			0,0,0,1);
	protected final Matrix4f TPose_Right = new Matrix4f(1,0,0,.75f,			0,1,0,1.62f,			0,0,1,.75f,			0,0,0,1);
	protected boolean TPose = false;
	//
	public boolean hudPopup = true;
	protected int moveModeSwitchCount = 0;
	public boolean isWalkingAbout;
	protected boolean isFreeRotate;
	protected ControllerType walkaboutController;
	protected ControllerType freeRotateController;
	protected float walkaboutYawStart;
	protected float hmdForwardYaw;
	public boolean ignorePressesNextFrame = false;
	protected int quickTorchPreviousSlot;
	
	protected Map<String, VRInputAction> inputActions = new HashMap<>();
	protected Map<String, VRInputAction> inputActionsByKeyBinding = new HashMap<>();
	
	// Vivecraft bindings included
	protected Set<KeyBinding> vanillaBindingSet;
	Set<KeyBinding> keyBindingSet;
	public final HandedKeyBinding keyClimbeyGrab = new HandedKeyBinding("vivecraft.key.climbeyGrab", GLFW.GLFW_KEY_UNKNOWN,"vivecraft.key.category.climbey");
	public final HandedKeyBinding keyClimbeyJump = new HandedKeyBinding("vivecraft.key.climbeyJump", GLFW.GLFW_KEY_UNKNOWN,"vivecraft.key.category.climbey");
	public final KeyBinding keyExportWorld = new KeyBinding("vivecraft.key.exportWorld", GLFW.GLFW_KEY_UNKNOWN, "key.categories.misc");
	public final KeyBinding keyFreeMoveRotate = new KeyBinding("vivecraft.key.freeMoveRotate", GLFW.GLFW_KEY_UNKNOWN, "key.categories.movement"); // dummy binding
	public final KeyBinding keyFreeMoveStrafe = new KeyBinding("vivecraft.key.freeMoveStrafe", GLFW.GLFW_KEY_UNKNOWN, "key.categories.movement"); // dummy binding
	public final KeyBinding keyHotbarNext = new KeyBinding("vivecraft.key.hotbarNext", GLFW.GLFW_KEY_PAGE_UP, "key.categories.inventory");
	public final KeyBinding keyHotbarPrev = new KeyBinding("vivecraft.key.hotbarPrev", GLFW.GLFW_KEY_PAGE_DOWN, "key.categories.inventory");
	public final KeyBinding keyHotbarScroll = new KeyBinding("vivecraft.key.hotbarScroll", GLFW.GLFW_KEY_UNKNOWN, "key.categories.inventory"); // dummy binding
	public final KeyBinding keyHotbarSwipeX = new KeyBinding("vivecraft.key.hotbarSwipeX", GLFW.GLFW_KEY_UNKNOWN, "key.categories.inventory"); // dummy binding
	public final KeyBinding keyHotbarSwipeY = new KeyBinding("vivecraft.key.hotbarSwipeY", GLFW.GLFW_KEY_UNKNOWN, "key.categories.inventory"); // dummy binding
	public final KeyBinding keyMenuButton = new KeyBinding("vivecraft.key.ingameMenuButton", GLFW.GLFW_KEY_UNKNOWN, "key.categories.ui");
	public final KeyBinding keyMoveThirdPersonCam = new KeyBinding("vivecraft.key.moveThirdPersonCam", GLFW.GLFW_KEY_UNKNOWN, "key.categories.misc");
	public final KeyBinding keyQuickHandheldCam = new KeyBinding("vivecraft.key.quickHandheldCam", GLFW.GLFW_KEY_UNKNOWN, "key.categories.misc");
	public final KeyBinding keyQuickTorch = new KeyBinding("vivecraft.key.quickTorch", GLFW.GLFW_KEY_INSERT, "key.categories.gameplay");
	public final KeyBinding keyRadialMenu = new KeyBinding("vivecraft.key.radialMenu", GLFW.GLFW_KEY_UNKNOWN, "key.categories.ui");
	public final KeyBinding keyRotateAxis = new KeyBinding("vivecraft.key.rotateAxis", GLFW.GLFW_KEY_UNKNOWN, "key.categories.movement"); // dummy binding
	public final KeyBinding keyRotateFree = new KeyBinding("vivecraft.key.rotateFree", GLFW.GLFW_KEY_HOME, "key.categories.movement");
	public final KeyBinding keyRotateLeft = new KeyBinding("vivecraft.key.rotateLeft", GLFW.GLFW_KEY_LEFT, "key.categories.movement");
	public final KeyBinding keyRotateRight = new KeyBinding("vivecraft.key.rotateRight", GLFW.GLFW_KEY_RIGHT, "key.categories.movement");
	public final KeyBinding keySwapMirrorView = new KeyBinding("vivecraft.key.swapMirrorView", GLFW.GLFW_KEY_UNKNOWN, "key.categories.misc");
	public final KeyBinding keyTeleport = new KeyBinding("vivecraft.key.teleport", GLFW.GLFW_KEY_UNKNOWN, "key.categories.movement");
	public final KeyBinding keyTeleportFallback = new KeyBinding("vivecraft.key.teleportFallback", GLFW.GLFW_KEY_UNKNOWN, "key.categories.movement");
	public final KeyBinding keyToggleHandheldCam = new KeyBinding("vivecraft.key.toggleHandheldCam", GLFW.GLFW_KEY_UNKNOWN, "key.categories.misc");
	public final KeyBinding keyToggleKeyboard = new KeyBinding("vivecraft.key.toggleKeyboard", GLFW.GLFW_KEY_UNKNOWN, "key.categories.ui");
	public final KeyBinding keyToggleMovement = new KeyBinding("vivecraft.key.toggleMovement", GLFW.GLFW_KEY_UNKNOWN, "key.categories.movement");
	public final KeyBinding keyTogglePlayerList = new KeyBinding("vivecraft.key.togglePlayerList", GLFW.GLFW_KEY_UNKNOWN, "key.categories.multiplayer");
	public final HandedKeyBinding keyTrackpadTouch = new HandedKeyBinding("vivecraft.key.trackpadTouch", GLFW.GLFW_KEY_UNKNOWN, "key.categories.misc"); // used for swipe sampler
	public final HandedKeyBinding keyVRInteract = new HandedKeyBinding("vivecraft.key.vrInteract", GLFW.GLFW_KEY_UNKNOWN,"key.categories.gameplay");
	public final KeyBinding keyWalkabout = new KeyBinding("vivecraft.key.walkabout", GLFW.GLFW_KEY_END, "key.categories.movement");

	public abstract String getName();
	public abstract String getID();
	public abstract void processInputs();
	public abstract void destroy();

	public double getGunAngle() {
		//return 40;
		return gunAngle;
	}

	public Set<KeyBinding> getKeyBindings() {
		if (keyBindingSet == null) {
			keyBindingSet = new LinkedHashSet<>();
			keyBindingSet.add(keyRotateLeft);
			keyBindingSet.add(keyRotateRight);
			keyBindingSet.add(keyRotateAxis);
			keyBindingSet.add(keyRotateFree);
			keyBindingSet.add(keyWalkabout);
			keyBindingSet.add(keyTeleport);
			keyBindingSet.add(keyTeleportFallback);
			keyBindingSet.add(keyFreeMoveRotate);
			keyBindingSet.add(keyFreeMoveStrafe);
			keyBindingSet.add(keyToggleMovement);
			keyBindingSet.add(keyQuickTorch);
			keyBindingSet.add(keyHotbarNext);
			keyBindingSet.add(keyHotbarPrev);
			keyBindingSet.add(keyHotbarScroll);
			keyBindingSet.add(keyHotbarSwipeX);
			keyBindingSet.add(keyHotbarSwipeY);
			keyBindingSet.add(keyMenuButton);
			keyBindingSet.add(keyRadialMenu);
			keyBindingSet.add(keyVRInteract);
			keyBindingSet.add(keySwapMirrorView);
			keyBindingSet.add(keyExportWorld);
			keyBindingSet.add(keyToggleKeyboard);
			keyBindingSet.add(keyMoveThirdPersonCam);
			keyBindingSet.add(keyTogglePlayerList);
			keyBindingSet.add(keyToggleHandheldCam);
			keyBindingSet.add(keyQuickHandheldCam);
			keyBindingSet.add(keyTrackpadTouch);
			keyBindingSet.add(GuiHandler.keyLeftClick);
			keyBindingSet.add(GuiHandler.keyRightClick);
			keyBindingSet.add(GuiHandler.keyMiddleClick);
			keyBindingSet.add(GuiHandler.keyShift);
			keyBindingSet.add(GuiHandler.keyCtrl);
			keyBindingSet.add(GuiHandler.keyAlt);
			keyBindingSet.add(GuiHandler.keyScrollUp);
			keyBindingSet.add(GuiHandler.keyScrollDown);
			keyBindingSet.add(GuiHandler.keyScrollAxis);
			keyBindingSet.add(GuiHandler.keyKeyboardClick);
			keyBindingSet.add(GuiHandler.keyKeyboardShift);
			keyBindingSet.add(keyClimbeyGrab);
			keyBindingSet.add(keyClimbeyJump);
		}

		return keyBindingSet;
	}

	public Matrix4f getAimRotation( int controller ) {
		return controllerRotation[controller];
	}

	public Vector3d getAimSource( int controller ) {
		Vector3d out = new Vector3d(aimSource[controller].x, aimSource[controller].y, aimSource[controller].z);
		if(!mc.vrSettings.seated && mc.vrSettings.allowStandingOriginOffset)
			out = out.add(offset.getX(), offset.getY(), offset.getZ());
		return out;
	}

	public Vector3d getAimVector( int controller ) {
		Vector3 v = controllerRotation[controller].transform(forward);
		return v.toVector3d();
	}

	public void triggerHapticPulse(ControllerType controller, float durationSeconds, float frequency, float amplitude) {
		triggerHapticPulse(controller, durationSeconds, frequency, amplitude, 0);
	}

	public void triggerHapticPulse(ControllerType controller, float durationSeconds, float frequency, float amplitude, float delaySeconds) {
		if (mc.vrSettings.seated) return;
		if (mc.vrSettings.vrReverseHands) {
			if (controller == ControllerType.RIGHT)
				controller = ControllerType.LEFT;
			else
				controller = ControllerType.RIGHT;
		}
		hapticScheduler.queueHapticPulse(controller, durationSeconds, frequency, amplitude, delaySeconds);
	}

	@Deprecated
	public void triggerHapticPulse(ControllerType controller, int strength) {
		if (strength < 1) return;
		// Through careful analysis of the haptics in the legacy API (read: I put the controller to
		// my ear, listened to the vibration, and reproduced the frequency in Audacity), I have determined
		// that the old haptics used 160Hz. So, these parameters will match the "feel" of the old haptics.
		triggerHapticPulse(controller, strength / 1000000f, 160, 1);
	}

	@Deprecated
	public void triggerHapticPulse(int controller, int strength) {
		if (controller < 0 || controller >= ControllerType.values().length) return;
		triggerHapticPulse(ControllerType.values()[controller], strength);
	}

	/**
	 * @return The coordinate of the left or right eye position relative to the head yaw plane
	 */

	public Matrix4f getHandRotation( int controller ) {
		return handRotation[controller];
	}

	public Vector3d getHandVector( int controller ) {
		Vector3 forward = new Vector3(0,0,-1);
		Matrix4f aimRotation = handRotation[controller];
		Vector3 controllerDirection = aimRotation.transform(forward);
		return controllerDirection.toVector3d();
	}

	public Vector3d getCenterEyePosition() {
		Vector3 pos = Utils.convertMatrix4ftoTranslationVector(hmdPose);
		if (mc.vrSettings.seated || mc.vrSettings.allowStandingOriginOffset)
			pos=pos.add(offset);
		return pos.toVector3d();
	}

	public Vector3d getEyePosition(RenderPass eye)
	{
		Matrix4f hmdToEye = hmdPoseRightEye;
		if (eye == RenderPass.LEFT)
		{
			hmdToEye = hmdPoseLeftEye;
		} else if (eye == RenderPass.RIGHT)
		{
			hmdToEye = hmdPoseRightEye;
		} else {
			hmdToEye = null;
		}

		if(hmdToEye == null){
			Matrix4f pose = hmdPose;
			Vector3 pos = Utils.convertMatrix4ftoTranslationVector(pose);
			if (mc.vrSettings.seated || mc.vrSettings.allowStandingOriginOffset)
				pos=pos.add(offset);
			return pos.toVector3d();
		} else {
			Matrix4f pose = Matrix4f.multiply( hmdPose, hmdToEye );
			Vector3 pos = Utils.convertMatrix4ftoTranslationVector(pose);
			if (mc.vrSettings.seated || mc.vrSettings.allowStandingOriginOffset)
				pos=pos.add(offset);
			return pos.toVector3d();
		}
	}

	public HardwareType getHardwareType() {
		return mc.vrSettings.forceHardwareDetection > 0 ? HardwareType.values()[mc.vrSettings.forceHardwareDetection - 1] : detectedHardware;
	}

	public Vector3d getHmdVector() {
		Vector3 v = hmdRotation.transform(forward);
		return v.toVector3d();
	}

	public Matrix4f getEyeRotation(RenderPass eye)
	{
		Matrix4f hmdToEye;
		if ( eye == RenderPass.LEFT) {
			hmdToEye = hmdPoseLeftEye;
		} else if ( eye == RenderPass.RIGHT) {
			hmdToEye = hmdPoseRightEye;
		} else {
			hmdToEye = null;
		}

		if (hmdToEye != null) {
			Matrix4f eyeRot = new Matrix4f();
			eyeRot.M[0][0] = hmdToEye.M[0][0];
			eyeRot.M[0][1] = hmdToEye.M[0][1];
			eyeRot.M[0][2] = hmdToEye.M[0][2];
			eyeRot.M[0][3] = 0.0F;
			eyeRot.M[1][0] = hmdToEye.M[1][0];
			eyeRot.M[1][1] = hmdToEye.M[1][1];
			eyeRot.M[1][2] = hmdToEye.M[1][2];
			eyeRot.M[1][3] = 0.0F;
			eyeRot.M[2][0] = hmdToEye.M[2][0];
			eyeRot.M[2][1] = hmdToEye.M[2][1];
			eyeRot.M[2][2] = hmdToEye.M[2][2];
			eyeRot.M[2][3] = 0.0F;
			eyeRot.M[3][0] = 0.0F;
			eyeRot.M[3][1] = 0.0F;
			eyeRot.M[3][2] = 0.0F;
			eyeRot.M[3][3] = 1.0F;

			return Matrix4f.multiply(hmdRotation, eyeRot);
		} else {
			return hmdRotation;
		}
	}
	
	public boolean isSafeBinding(KeyBinding kb) {
		// Stupid hard-coded junk
		return getKeyBindings().contains(kb) || kb == mc.gameSettings.keyBindChat || kb == mc.gameSettings.keyBindInventory;
	}
	
	public boolean isModBinding(KeyBinding kb) {
		return !vanillaBindingSet.contains(kb);
	}
	
	public VRInputAction getInputAction(String keyBindingDesc) {
		return inputActionsByKeyBinding.get(keyBindingDesc);
	}

	public VRInputAction getInputActionByName(String name) {
		return inputActions.get(name);
	}

	public Collection<VRInputAction> getInputActions() {
		return Collections.unmodifiableCollection(inputActions.values());
	}
	
	public VRInputAction getInputAction(KeyBinding keyBinding) {
		return getInputAction(keyBinding.getKeyDescription());
	}
	
	public Collection<VRInputAction> getInputActionsInSet(VRInputActionSet set) {
		return Collections.unmodifiableCollection(inputActions.values().stream().filter(action -> action.actionSet == set).collect(Collectors.toList()));
	}
	
	@SuppressWarnings("unchecked")
	public KeyBinding[] initializeBindings(KeyBinding[] keyBindings) {
		for (KeyBinding keyBinding : getKeyBindings())
			keyBindings = ArrayUtils.add(keyBindings, keyBinding);

		// Copy the bindings array here so we know which ones are from mods
		setVanillaBindings(keyBindings);

		Map<String, Integer> co = (Map<String, Integer>)MCReflection.KeyBinding_CATEGORY_ORDER.get(null);
		co.put("vivecraft.key.category.gui", 8);
		co.put("vivecraft.key.category.climbey", 9);
		co.put("vivecraft.key.category.keyboard", 10);

		return keyBindings;
	}
	
//	public boolean isBoundInActiveActionSets(KeyBinding binding) {
//		List<Long> origins = getInputAction(binding).getOrigins();
//		return !origins.isEmpty();
//	}

	public boolean isControllerTracking(ControllerType controller) {
		return isControllerTracking(controller.ordinal());
	}

	public boolean isControllerTracking(int controller) {
		return controllerTracking[controller];
	}
	
	public void resetPosition() {
		Vector3d pos= getCenterEyePosition().scale(-1).add(offset.getX(),offset.getY(),offset.getZ());
		offset=new Vector3((float) pos.x,(float)pos.y+1.62f,(float)pos.z);
	}
	
	public void clearOffset() {
		offset=new Vector3(0,0,0);
	}
	
	public void setVanillaBindings(KeyBinding[] bindings) {
		vanillaBindingSet = new HashSet<>(Arrays.asList(bindings));
	}

	public boolean isHMDTracking() {
		return headIsTracking;
	}
	
	protected void processHotbar() {
		mc.interactTracker.hotbar = -1;
		if(mc.player == null) return;
		if(mc.player.inventory == null) return;

		if(mc.climbTracker.isGrabbingLadder() && 
				mc.climbTracker.isClaws(mc.player.getHeldItemMainhand())) return;
		if(!mc.interactTracker.isActive(mc.player)) return;
		Vector3d main = getAimSource(0);
		Vector3d off = getAimSource(1);

		Vector3d barStartos = null,barEndos = null;

		int i = 1;
		if(mc.vrSettings.vrReverseHands) i = -1;

		if (mc.vrSettings.vrHudLockMode == VRSettings.HUD_LOCK_WRIST){
			barStartos =  getAimRotation(1).transform(new Vector3(i*0.02f,0.05f,0.26f)).toVector3d();
			barEndos =  getAimRotation(1).transform(new Vector3(i*0.02f,0.05f,0.01f)).toVector3d();
		} else if (mc.vrSettings.vrHudLockMode == VRSettings.HUD_LOCK_HAND){
			barStartos =  getAimRotation(1).transform(new Vector3(i*-.18f,0.08f,-0.01f)).toVector3d();
			barEndos =  getAimRotation(1).transform(new Vector3(i*0.19f,0.04f,-0.08f)).toVector3d();
		} else return; //how did u get here


		Vector3d barStart = off.add(barStartos.x, barStartos.y, barStartos.z);	
		Vector3d barEnd = off.add(barEndos.x, barEndos.y, barEndos.z);

		Vector3d u = barStart.subtract(barEnd);
		Vector3d pq = barStart.subtract(main);
		float dist = (float) (pq.crossProduct(u).length() / u.length());

		if(dist > 0.06) return;

		float fact = (float) (pq.dotProduct(u) / (u.x*u.x + u.y*u.y + u.z*u.z));

		if(fact < -1) return;

		Vector3d w2 = u.scale(fact).subtract(pq);

		Vector3d point = main.subtract(w2);
		float linelen = (float) u.length();
		float ilen = (float) barStart.subtract(point).length();
		if(fact < 0) ilen *= -1;
		float pos = ilen / linelen * 9; 

		if(mc.vrSettings.vrReverseHands) pos = 9 - pos;

		int box = (int) Math.floor(pos);

		if(box > 8) return;
		if(box < 0) {
			if(pos <= -0.5 && pos >= -1.5) //TODO fix reversed hands situation.
				box = 9;
			else
				return;
		}
		//all that maths for this.
		mc.interactTracker.hotbar = box;
		if(box != mc.interactTracker.hotbar){
			triggerHapticPulse(0, 750);
		}
	}
	
	protected KeyBinding findKeyBinding(String name) {
		return Arrays.stream(mc.gameSettings.keyBindings).filter(kb -> name.equals(kb.getKeyDescription())).findFirst().orElse(null);
	}
	
	protected void hmdSampling() {
		if (hmdPosSamples.size() == hmdAvgLength)
			hmdPosSamples.removeFirst();
		if (hmdYawSamples.size() == hmdAvgLength)
			hmdYawSamples.removeFirst();

		float yaw = mc.vrPlayer.vrdata_room_pre.hmd.getYaw();
		if (yaw < 0)
			yaw += 360;
		hmdYawTotal += Utils.angleDiff(yaw, hmdYawLast);
		hmdYawLast = yaw;
		if (Math.abs(Utils.angleNormalize(hmdYawTotal) - hmdYawLast) > 1 || hmdYawTotal > 100000) {
			hmdYawTotal = hmdYawLast;
			System.out.println("HMD yaw desync/overflow corrected");
		}
		hmdPosSamples.add(mc.vrPlayer.vrdata_room_pre.hmd.getPosition());
		float yawAvg = 0;
		if (hmdYawSamples.size() > 0) {
			for (float f : hmdYawSamples) {
				yawAvg += f;
			}
			yawAvg /= hmdYawSamples.size();
		}
		if (Math.abs((hmdYawTotal - yawAvg)) > 20)
			trigger = true;
		if (Math.abs((hmdYawTotal - yawAvg)) < 1)
			trigger = false;
		if (trigger || hmdYawSamples.isEmpty())
			hmdYawSamples.add(hmdYawTotal);
	}
	
	protected void updateAim() {
		if (mc==null)
			return;

		{//hmd
			hmdRotation.M[0][0] = hmdPose.M[0][0];
			hmdRotation.M[0][1] = hmdPose.M[0][1];
			hmdRotation.M[0][2] = hmdPose.M[0][2];
			hmdRotation.M[0][3] = 0.0F;
			hmdRotation.M[1][0] = hmdPose.M[1][0];
			hmdRotation.M[1][1] = hmdPose.M[1][1];
			hmdRotation.M[1][2] = hmdPose.M[1][2];
			hmdRotation.M[1][3] = 0.0F;
			hmdRotation.M[2][0] = hmdPose.M[2][0];
			hmdRotation.M[2][1] = hmdPose.M[2][1];
			hmdRotation.M[2][2] = hmdPose.M[2][2];
			hmdRotation.M[2][3] = 0.0F;
			hmdRotation.M[3][0] = 0.0F;
			hmdRotation.M[3][1] = 0.0F;
			hmdRotation.M[3][2] = 0.0F;
			hmdRotation.M[3][3] = 1.0F;


			Vector3d eye = getCenterEyePosition();
			hmdHistory.add(eye);
			Vector3 v3 = hmdRotation.transform(new Vector3(0,-.1f, .1f));
			hmdPivotHistory.add(new Vector3d(v3.getX()+eye.x, v3.getY()+eye.y, v3.getZ()+eye.z));

		}

		if(mc.vrSettings.seated){
			controllerPose[0] = hmdPose.inverted().inverted();
			controllerPose[1] = hmdPose.inverted().inverted();
		}

		Matrix4f[] controllerPoseTip = new Matrix4f[2];
		controllerPoseTip[0] = new Matrix4f();
		controllerPoseTip[1] = new Matrix4f();
		Matrix4f[] controllerPoseHand = new Matrix4f[2];
		controllerPoseHand[0] = new Matrix4f();
		controllerPoseHand[1] = new Matrix4f();

		{//right controller
			if(mc.vrSettings.seated)
				controllerPoseHand[0] = controllerPose[0];
			else	
				controllerPoseHand[0] = Matrix4f.multiply(controllerPose[0], getControllerComponentTransform(0,"handgrip"));

			handRotation[0].M[0][0] = controllerPoseHand[0].M[0][0];
			handRotation[0].M[0][1] = controllerPoseHand[0].M[0][1];
			handRotation[0].M[0][2] = controllerPoseHand[0].M[0][2];
			handRotation[0].M[0][3] = 0.0F;
			handRotation[0].M[1][0] = controllerPoseHand[0].M[1][0];
			handRotation[0].M[1][1] = controllerPoseHand[0].M[1][1];
			handRotation[0].M[1][2] = controllerPoseHand[0].M[1][2];
			handRotation[0].M[1][3] = 0.0F;
			handRotation[0].M[2][0] = controllerPoseHand[0].M[2][0];
			handRotation[0].M[2][1] = controllerPoseHand[0].M[2][1];
			handRotation[0].M[2][2] = controllerPoseHand[0].M[2][2];
			handRotation[0].M[2][3] = 0.0F;
			handRotation[0].M[3][0] = 0.0F;
			handRotation[0].M[3][1] = 0.0F;
			handRotation[0].M[3][2] = 0.0F;
			handRotation[0].M[3][3] = 1.0F;	

			if(mc.vrSettings.seated)
				controllerPoseTip[0] = controllerPose[0];
			else	
				controllerPoseTip[0] = Matrix4f.multiply(controllerPose[0], getControllerComponentTransform(0,"tip"));

			// grab controller position in tracker space, scaled to minecraft units
			Vector3 controllerPos = Utils.convertMatrix4ftoTranslationVector(controllerPoseTip[0]);
			aimSource[0] = controllerPos.toVector3d();

			controllerHistory[0].add(getAimSource(0));

			// build matrix describing controller rotation
			controllerRotation[0].M[0][0] = controllerPoseTip[0].M[0][0];
			controllerRotation[0].M[0][1] = controllerPoseTip[0].M[0][1];
			controllerRotation[0].M[0][2] = controllerPoseTip[0].M[0][2];
			controllerRotation[0].M[0][3] = 0.0F;
			controllerRotation[0].M[1][0] = controllerPoseTip[0].M[1][0];
			controllerRotation[0].M[1][1] = controllerPoseTip[0].M[1][1];
			controllerRotation[0].M[1][2] = controllerPoseTip[0].M[1][2];
			controllerRotation[0].M[1][3] = 0.0F;
			controllerRotation[0].M[2][0] = controllerPoseTip[0].M[2][0];
			controllerRotation[0].M[2][1] = controllerPoseTip[0].M[2][1];
			controllerRotation[0].M[2][2] = controllerPoseTip[0].M[2][2];
			controllerRotation[0].M[2][3] = 0.0F;
			controllerRotation[0].M[3][0] = 0.0F;
			controllerRotation[0].M[3][1] = 0.0F;
			controllerRotation[0].M[3][2] = 0.0F;
			controllerRotation[0].M[3][3] = 1.0F;

			Vector3d hdir = getHmdVector();

			if(mc.vrSettings.seated && mc.currentScreen == null){
				org.vivecraft.utils.lwjgl.Matrix4f temp = new org.vivecraft.utils.lwjgl.Matrix4f();

				float hRange = 110;
				float vRange = 180;
				double h = mc.mouseHelper.getMouseX() / (double) mc.getMainWindow().getWidth() * hRange - (hRange / 2);

				//h = MathHelper.clamp(h, -hRange/2, hRange/2);

				int hei  = mc.getMainWindow().getHeight();
				if(hei % 2 != 0)
					hei-=1; //fix drifting vertical mouse.

				double v = -mc.mouseHelper.getMouseY() / (double) hei * vRange + (vRange / 2);		

				double nPitch=-v;
				if(mc.isGameFocused()){
					float rotStart = mc.vrSettings.keyholeX;
					float rotSpeed = 20 * mc.vrSettings.xSensitivity;
					int leftedge=(int)((-rotStart + (hRange / 2)) *(double) mc.getMainWindow().getWidth() / hRange )+1;
					int rightedge=(int)((rotStart + (hRange / 2)) *(double) mc.getMainWindow().getWidth() / hRange )-1;
					float rotMul = ((float)Math.abs(h) - rotStart) / ((hRange / 2) - rotStart); // Scaled 0...1 from rotStart to FOV edge

					double xpos = mc.mouseHelper.getMouseX();

					if(h < -rotStart){
						seatedRot += rotSpeed * rotMul;
						seatedRot %= 360;
						hmdForwardYaw = (float)Math.toDegrees(Math.atan2(hdir.x, hdir.z));   
						xpos = leftedge;
						h=-rotStart;
					}else if(h > rotStart){
						seatedRot -= rotSpeed * rotMul;
						seatedRot %= 360;
						hmdForwardYaw = (float)Math.toDegrees(Math.atan2(hdir.x, hdir.z));    	
						xpos = rightedge;
						h=rotStart;
					}
					double ySpeed=0.5 * mc.vrSettings.ySensitivity;
					nPitch=aimPitch+(v)*ySpeed;
					nPitch=MathHelper.clamp(nPitch,-89.9,89.9);

					InputSimulator.setMousePos(xpos, hei/2);
					GLFW.glfwSetCursorPos(mc.getMainWindow().getHandle(), xpos, hei/2);

					temp.rotate((float) Math.toRadians(-nPitch), new org.vivecraft.utils.lwjgl.Vector3f(1,0,0));
					temp.rotate((float) Math.toRadians(-180 + h - hmdForwardYaw), new org.vivecraft.utils.lwjgl.Vector3f(0,1,0));
				}


				controllerRotation[0].M[0][0] = temp.m00;
				controllerRotation[0].M[0][1] = temp.m01;
				controllerRotation[0].M[0][2] = temp.m02;

				controllerRotation[0].M[1][0] = temp.m10;
				controllerRotation[0].M[1][1] = temp.m11;
				controllerRotation[0].M[1][2] = temp.m12;

				controllerRotation[0].M[2][0] = temp.m20;
				controllerRotation[0].M[2][1] = temp.m21;
				controllerRotation[0].M[2][2] = temp.m22;
			}	

			Vector3d dir = getAimVector(0);
			aimPitch = (float)Math.toDegrees(Math.asin(dir.y/dir.length()));
			controllerForwardHistory[0].add(dir);
			Vector3d updir = 	controllerRotation[0].transform(up).toVector3d();
			controllerUpHistory[0].add(updir);
		}

		{//left controller

			if(mc.vrSettings.seated)
				controllerPoseHand[1] = controllerPose[1];
			else	
				controllerPoseHand[1] = Matrix4f.multiply(controllerPose[1], getControllerComponentTransform(1,"handgrip"));

			handRotation[1].M[0][0] = controllerPoseHand[1].M[0][0];
			handRotation[1].M[0][1] = controllerPoseHand[1].M[0][1];
			handRotation[1].M[0][2] = controllerPoseHand[1].M[0][2];
			handRotation[1].M[0][3] = 0.0F;
			handRotation[1].M[1][0] = controllerPoseHand[1].M[1][0];
			handRotation[1].M[1][1] = controllerPoseHand[1].M[1][1];
			handRotation[1].M[1][2] = controllerPoseHand[1].M[1][2];
			handRotation[1].M[1][3] = 0.0F;
			handRotation[1].M[2][0] = controllerPoseHand[1].M[2][0];
			handRotation[1].M[2][1] = controllerPoseHand[1].M[2][1];
			handRotation[1].M[2][2] = controllerPoseHand[1].M[2][2];
			handRotation[1].M[2][3] = 0.0F;
			handRotation[1].M[3][0] = 0.0F;
			handRotation[1].M[3][1] = 0.0F;
			handRotation[1].M[3][2] = 0.0F;
			handRotation[1].M[3][3] = 1.0F;	

			// update off hand aim
			if(mc.vrSettings.seated)
				controllerPoseTip[1] = controllerPose[1];
			else
				controllerPoseTip[1] = Matrix4f.multiply(controllerPose[1], getControllerComponentTransform(1,"tip"));

			Vector3 leftControllerPos = Utils.convertMatrix4ftoTranslationVector(controllerPoseTip[1]);
			aimSource[1] = leftControllerPos.toVector3d();
			controllerHistory[1].add(getAimSource(1));

			// build matrix describing controller rotation
			controllerRotation[1].M[0][0] = controllerPoseTip[1].M[0][0];
			controllerRotation[1].M[0][1] = controllerPoseTip[1].M[0][1];
			controllerRotation[1].M[0][2] = controllerPoseTip[1].M[0][2];
			controllerRotation[1].M[0][3] = 0.0F;
			controllerRotation[1].M[1][0] = controllerPoseTip[1].M[1][0];
			controllerRotation[1].M[1][1] = controllerPoseTip[1].M[1][1];
			controllerRotation[1].M[1][2] = controllerPoseTip[1].M[1][2];
			controllerRotation[1].M[1][3] = 0.0F;
			controllerRotation[1].M[2][0] = controllerPoseTip[1].M[2][0];
			controllerRotation[1].M[2][1] = controllerPoseTip[1].M[2][1];
			controllerRotation[1].M[2][2] = controllerPoseTip[1].M[2][2];
			controllerRotation[1].M[2][3] = 0.0F;
			controllerRotation[1].M[3][0] = 0.0F;
			controllerRotation[1].M[3][1] = 0.0F;
			controllerRotation[1].M[3][2] = 0.0F;
			controllerRotation[1].M[3][3] = 1.0F;

			Vector3d dir = getAimVector(1);
			controllerForwardHistory[1].add(dir);
			Vector3d updir = 	controllerRotation[1].transform(up).toVector3d();
			controllerUpHistory[1].add(updir);

			if(mc.vrSettings.seated){
				aimSource[1] = getCenterEyePosition();
				aimSource[0] = getCenterEyePosition();
			}

		}

		boolean debugThirdController = false;
		if(debugThirdController) controllerPose[2] = controllerPose[0];

		// build matrix describing controller rotation
		controllerRotation[2].M[0][0] = controllerPose[2].M[0][0];
		controllerRotation[2].M[0][1] = controllerPose[2].M[0][1];
		controllerRotation[2].M[0][2] = controllerPose[2].M[0][2];
		controllerRotation[2].M[0][3] = 0.0F;
		controllerRotation[2].M[1][0] = controllerPose[2].M[1][0];
		controllerRotation[2].M[1][1] = controllerPose[2].M[1][1];
		controllerRotation[2].M[1][2] = controllerPose[2].M[1][2];
		controllerRotation[2].M[1][3] = 0.0F;
		controllerRotation[2].M[2][0] = controllerPose[2].M[2][0];
		controllerRotation[2].M[2][1] = controllerPose[2].M[2][1];
		controllerRotation[2].M[2][2] = controllerPose[2].M[2][2];
		controllerRotation[2].M[2][3] = 0.0F;
		controllerRotation[2].M[3][0] = 0.0F;
		controllerRotation[2].M[3][1] = 0.0F;
		controllerRotation[2].M[3][2] = 0.0F;
		controllerRotation[2].M[3][3] = 1.0F;

		if(hasThirdController() && (mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_MIXED_REALITY || mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_THIRD_PERSON )|| debugThirdController) {
			mrMovingCamActive = true;
			Vector3 thirdControllerPos = Utils.convertMatrix4ftoTranslationVector(controllerPose[2]);
			aimSource[2] = thirdControllerPos.toVector3d();
		} else {
			mrMovingCamActive = false;
			aimSource[2] = new Vector3d(
					mc.vrSettings.vrFixedCamposX,
					mc.vrSettings.vrFixedCamposY,
					mc.vrSettings.vrFixedCamposZ);
		}
	}
	
	public void processBindings() {
		//VIVE SPECIFIC FUNCTIONALITY
		//TODO: Find a better home for these. (uh?)
		if (inputActions.isEmpty()) return;
		boolean sleeping = (mc.world !=null && mc.player != null && mc.player.isSleeping());
		boolean gui = mc.currentScreen != null;

		boolean toggleMovementPressed = keyToggleMovement.isPressed();
		if (mc.gameSettings.keyBindPickBlock.isKeyDown() || toggleMovementPressed) {
			if (++moveModeSwitchCount == 20 * 4 || toggleMovementPressed) {
				if (mc.vrSettings.seated) {
					mc.vrSettings.seatedFreeMove = !mc.vrSettings.seatedFreeMove;
					mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("vivecraft.messages.movementmodeswitch", mc.vrSettings.seatedFreeMove ? Lang.get("vivecraft.options.freemove") : Lang.get("vivecraft.options.teleport")));
				} else 
				{
					if (mc.vrPlayer.isTeleportSupported()) {
						mc.vrSettings.forceStandingFreeMove = !mc.vrSettings.forceStandingFreeMove;
						mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("vivecraft.messages.movementmodeswitch", mc.vrSettings.seatedFreeMove ? Lang.get("vivecraft.options.freemove") : Lang.get("vivecraft.options.teleport")));
					} else {
						if (mc.vrPlayer.isTeleportOverridden()) {
							mc.vrPlayer.setTeleportOverride(false);
							mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("vivecraft.messages.teleportdisabled"));
						} else {
							mc.vrPlayer.setTeleportOverride(true);
							mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("vivecraft.messages.teleportenabled"));
						}
					}
				}
			}
		} else {
			moveModeSwitchCount = 0;
		}

		Vector3d main = getAimVector(0);
		Vector3d off = getAimVector(1);

		float myaw = (float) Math.toDegrees(Math.atan2(-main.x, main.z));
		float oyaw= (float) Math.toDegrees(Math.atan2(-off.x, off.z));;

		if(!gui){
			if(keyWalkabout.isKeyDown()){
				float yaw = myaw;

				//oh this is ugly. TODO: cache which hand when binding button.
				ControllerType controller = findActiveBindingControllerType(keyWalkabout);
				if (controller != null && controller == ControllerType.LEFT) {
					yaw = oyaw;
				}

				if (!isWalkingAbout){
					isWalkingAbout = true;
					walkaboutYawStart = mc.vrSettings.vrWorldRotation - yaw;  
				}
				else {
					mc.vrSettings.vrWorldRotation = walkaboutYawStart + yaw;
					mc.vrSettings.vrWorldRotation %= 360; // Prevent stupidly large values (can they even happen here?)
					//	mc.vrPlayer.checkandUpdateRotateScale(true);
				}
			} else {
				isWalkingAbout = false;
			}

			if(keyRotateFree.isKeyDown()){
				float yaw = myaw;

				//oh this is ugly. TODO: cache which hand when binding button.
				ControllerType controller = findActiveBindingControllerType(keyRotateFree);
				if (controller != null && controller == ControllerType.LEFT) {
					yaw = oyaw;
				}

				if (!isFreeRotate){
					isFreeRotate = true;
					walkaboutYawStart = mc.vrSettings.vrWorldRotation + yaw;  
				}
				else {
					mc.vrSettings.vrWorldRotation = walkaboutYawStart - yaw;
					//	mc.vrPlayer.checkandUpdateRotateScale(true,0);
				}
			} else {
				isFreeRotate = false;
			}
		}

		if(keyHotbarNext.isPressed()) {
			changeHotbar(-1);
			triggerBindingHapticPulse(keyHotbarNext, 250);
		}

		if(keyHotbarPrev.isPressed()){
			changeHotbar(1);
			triggerBindingHapticPulse(keyHotbarPrev, 250);
		}

		if(keyQuickTorch.isPressed() && mc.player != null){
			for (int slot=0;slot<9;slot++)
			{  
				ItemStack itemStack = mc.player.inventory.getStackInSlot(slot);
				if (itemStack.getItem() instanceof BlockItem && ((BlockItem)itemStack.getItem()).getBlock() instanceof TorchBlock  && mc.currentScreen == null)
				{
					quickTorchPreviousSlot = mc.player.inventory.currentItem;
					mc.player.inventory.currentItem = slot;
					mc.rightClickMouse();
					// switch back immediately
					mc.player.inventory.currentItem = quickTorchPreviousSlot;
					quickTorchPreviousSlot = -1;
					break;
				}
			}
		}

		// if you start teleporting, close any UI
		if (gui && !sleeping && mc.gameSettings.keyBindForward.isKeyDown() && !(mc.currentScreen instanceof WinGameScreen))
		{
			if(mc.player !=null) mc.player.closeScreen();
		}

		//GuiContainer.java only listens directly to the keyboard to close.
		if (mc.currentScreen instanceof ContainerScreen && mc.gameSettings.keyBindInventory.isPressed()) {
			if (mc.player != null)
				mc.player.closeScreen();
		}

		// allow toggling chat window with chat keybind
		if (mc.currentScreen instanceof ChatScreen && mc.gameSettings.keyBindChat.isPressed()) {
			mc.displayGuiScreen(null);
		}

		if(mc.vrSettings.vrWorldRotationIncrement == 0){
			float ax = getInputAction(keyRotateAxis).getAxis2DUseTracked().getX();
			if (ax == 0) ax = getInputAction(keyFreeMoveRotate).getAxis2DUseTracked().getX();
			if (ax != 0) {
				float analogRotSpeed = 10 * ax;
				mc.vrSettings.vrWorldRotation -= analogRotSpeed;
				mc.vrSettings.vrWorldRotation = mc.vrSettings.vrWorldRotation % 360;
			}
		} else {
			if (keyRotateAxis.isPressed() || keyFreeMoveRotate.isPressed()) {
				float ax = getInputAction(keyRotateAxis).getAxis2D(false).getX();
				if (ax == 0) ax = getInputAction(keyFreeMoveRotate).getAxis2D(false).getX();
				if (Math.abs(ax) > 0.5f) {
					mc.vrSettings.vrWorldRotation -= mc.vrSettings.vrWorldRotationIncrement * Math.signum(ax);
					mc.vrSettings.vrWorldRotation = mc.vrSettings.vrWorldRotation % 360;
				}
			}
		}

		if(mc.vrSettings.vrWorldRotationIncrement == 0){
			float ax = VivecraftMovementInput.getMovementAxisValue(keyRotateLeft);
			if(ax > 0){
				float analogRotSpeed = 5;
				if(ax > 0)	analogRotSpeed= 10 * ax;
				mc.vrSettings.vrWorldRotation+=analogRotSpeed;
				mc.vrSettings.vrWorldRotation = mc.vrSettings.vrWorldRotation % 360;
			}
		}else{
			if(keyRotateLeft.isPressed()){
				mc.vrSettings.vrWorldRotation+=mc.vrSettings.vrWorldRotationIncrement;
				mc.vrSettings.vrWorldRotation = mc.vrSettings.vrWorldRotation % 360;
			}
		}

		if(mc.vrSettings.vrWorldRotationIncrement == 0){
			float ax = VivecraftMovementInput.getMovementAxisValue(keyRotateRight);
			if(ax > 0){
				float analogRotSpeed = 5;
				if(ax > 0)	analogRotSpeed = 10 * ax;
				mc.vrSettings.vrWorldRotation-=analogRotSpeed;
				mc.vrSettings.vrWorldRotation = mc.vrSettings.vrWorldRotation % 360;
			}
		}else{
			if(keyRotateRight.isPressed()){
				mc.vrSettings.vrWorldRotation-=mc.vrSettings.vrWorldRotationIncrement;
				mc.vrSettings.vrWorldRotation = mc.vrSettings.vrWorldRotation % 360;
			}
		}

		seatedRot = mc.vrSettings.vrWorldRotation;

		if(keyRadialMenu.isPressed() && !gui) {
			ControllerType controller = findActiveBindingControllerType(keyRadialMenu);
			if (controller != null)
				RadialHandler.setOverlayShowing(!RadialHandler.isShowing(), controller);
		}

		if (keySwapMirrorView.isPressed()) {
			if (mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_THIRD_PERSON)
				mc.vrSettings.displayMirrorMode = VRSettings.MIRROR_FIRST_PERSON;
			else if (mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_FIRST_PERSON)
				mc.vrSettings.displayMirrorMode = VRSettings.MIRROR_THIRD_PERSON;
			mc.vrRenderer.reinitFrameBuffers("Mirror Setting Changed");
		}

		if (keyToggleKeyboard.isPressed()) {
			KeyboardHandler.setOverlayShowing(!KeyboardHandler.Showing);
		}

		if (keyMoveThirdPersonCam.isPressed() && !Main.kiosk && !mc.vrSettings.seated && (mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_MIXED_REALITY || mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_THIRD_PERSON)) {
			ControllerType controller = findActiveBindingControllerType(keyMoveThirdPersonCam);
			if (controller != null)
				VRHotkeys.startMovingThirdPersonCam(controller.ordinal(), VRHotkeys.Triggerer.BINDING);
		}
		if (!keyMoveThirdPersonCam.isKeyDown() && VRHotkeys.isMovingThirdPersonCam() && VRHotkeys.getMovingThirdPersonCamTriggerer() == VRHotkeys.Triggerer.BINDING) {
			VRHotkeys.stopMovingThirdPersonCam();
			mc.vrSettings.saveOptions();
		}

		if (VRHotkeys.isMovingThirdPersonCam() && VRHotkeys.getMovingThirdPersonCamTriggerer() == VRHotkeys.Triggerer.MENUBUTTON && keyMenuButton.isPressed()) { //super special case.
			VRHotkeys.stopMovingThirdPersonCam();
			mc.vrSettings.saveOptions();
		}

		if(KeyboardHandler.Showing && mc.currentScreen == null && keyMenuButton.isPressed()) { //super special case.
			KeyboardHandler.setOverlayShowing(false);
		}

		if(RadialHandler.isShowing() && keyMenuButton.isPressed()) { //super special case.
			RadialHandler.setOverlayShowing(false, null);
		}

		if(keyMenuButton.isPressed()) { //handle menu directly
			if(!gui) {
				if(!Main.kiosk){
					mc.displayInGameMenu(false);
				}
			} else {
				InputSimulator.pressKey(GLFW.GLFW_KEY_ESCAPE);
				InputSimulator.releaseKey(GLFW.GLFW_KEY_ESCAPE);
			}
			KeyboardHandler.setOverlayShowing(false);
		}

		if (keyExportWorld.isPressed()) {
			if (mc.world != null && mc.player != null) {
				try {
					final BlockPos pos = mc.player.getPosition();
					final int size = 320;
					File dir = new File("menuworlds/custom_114");
					dir.mkdirs();
					File foundFile;
					for (int i = 0;; i++) {
						foundFile = new File(dir, "world" + i + ".mmw");
						if (!foundFile.exists())
							break;
					}
					final File file = foundFile;
					System.out.println("Exporting world... area size: " + size);
					System.out.println("Saving to " + file.getAbsolutePath());
					if (mc.isIntegratedServerRunning()) {
						final World world = mc.getIntegratedServer().getWorld(mc.player.world.getDimensionKey());
						CompletableFuture<Void> task = mc.getIntegratedServer().runAsync(new Runnable() {
							@Override
							public void run() {
								try {
									MenuWorldExporter.saveAreaToFile(world, pos.getX() - size / 2, pos.getZ() - size / 2, size, size, pos.getY(), file);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						});
						while (!task.isDone()) {
							Thread.sleep(10);
						}
					} else {
						MenuWorldExporter.saveAreaToFile(mc.world, pos.getX() - size / 2, pos.getZ() - size / 2, size, size, pos.getY(), file);
						mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("vivecraft.messages.menuworldexportclientwarning"));
					}
					mc.ingameGUI.getChatGUI().printChatMessage(new StringTextComponent(LangHelper.get("vivecraft.messages.menuworldexportcomplete.1", size)));
					mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("vivecraft.messages.menuworldexportcomplete.2", file.getAbsolutePath()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if (keyTogglePlayerList.isPressed()) {
			mc.ingameGUI.showPlayerList = !mc.ingameGUI.showPlayerList;
		}

		if (keyToggleHandheldCam.isPressed() && mc.player != null) {
			mc.cameraTracker.toggleVisibility();
			if (mc.cameraTracker.isVisible()) {
				ControllerType hand = findActiveBindingControllerType(keyToggleHandheldCam);
				if (hand == null)
					hand = ControllerType.RIGHT;
				VRData.VRDevicePose handPose = mc.vrPlayer.vrdata_world_pre.getController(hand.ordinal());
				mc.cameraTracker.setPosition(handPose.getPosition());
				mc.cameraTracker.setRotation(new Quaternion(handPose.getMatrix().transposed()));
			}
		}

		if (keyQuickHandheldCam.isPressed() && mc.player != null) {
			if (!mc.cameraTracker.isVisible())
				mc.cameraTracker.toggleVisibility();
			ControllerType hand = findActiveBindingControllerType(keyQuickHandheldCam);
			if (hand == null)
				hand = ControllerType.RIGHT;
			VRData.VRDevicePose handPose = mc.vrPlayer.vrdata_world_pre.getController(hand.ordinal());
			mc.cameraTracker.setPosition(handPose.getPosition());
			mc.cameraTracker.setRotation(new Quaternion(handPose.getMatrix().transposed()));
			mc.cameraTracker.startMoving(hand.ordinal(), true);
		}
		if (!keyQuickHandheldCam.isKeyDown() && mc.cameraTracker.isMoving() && mc.cameraTracker.isQuickMode() && mc.player != null) {
			mc.cameraTracker.stopMoving();
			mc.grabScreenShot = true;
		}

		GuiHandler.processBindingsGui();
		RadialHandler.processBindings();
		KeyboardHandler.processBindings();
		mc.interactTracker.processBindings();
	}
	
	public void populateInputActions() {
		Map<String, ActionParams> actionParams = getSpecialActionParams();
		
		for (final KeyBinding keyBinding : mc.gameSettings.keyBindings) {
			ActionParams params = actionParams.getOrDefault(keyBinding.getKeyDescription(), new ActionParams("optional", "boolean", null));
			VRInputAction action = new VRInputAction(keyBinding, params.requirement, params.type, params.actionSetOverride);
			inputActions.put(action.name, action);
		}
		
		for (VRInputAction action : inputActions.values()) {
			inputActionsByKeyBinding.put(action.keyBinding.getKeyDescription(), action);
		}

		getInputAction(keyVRInteract).setPriority(5).setEnabled(false);
		getInputAction(keyClimbeyGrab).setPriority(10).setEnabled(false);
		//getInputAction(mc.vr.keyClimbeyJump).setPriority(10).setEnabled(false);
		getInputAction(keyClimbeyJump).setEnabled(false);
		getInputAction(GuiHandler.keyKeyboardClick).setPriority(50);
		getInputAction(GuiHandler.keyKeyboardShift).setPriority(50);
	}
	
	// This is for bindings with specific requirement/type params, anything not listed will default to optional and boolean
	// See OpenVR docs for valid values: https://github.com/ValveSoftware/openvr/wiki/Action-manifest#actions
	public Map<String, ActionParams> getSpecialActionParams() {
		Map<String, ActionParams> map = new HashMap<>();

		addActionParams(map, mc.gameSettings.keyBindForward, "optional", "vector1", null);
		addActionParams(map, mc.gameSettings.keyBindBack, "optional", "vector1", null);
		addActionParams(map, mc.gameSettings.keyBindLeft, "optional", "vector1", null);
		addActionParams(map, mc.gameSettings.keyBindRight, "optional", "vector1", null);
		addActionParams(map, mc.gameSettings.keyBindInventory, "suggested", "boolean", VRInputActionSet.GLOBAL);
		addActionParams(map, mc.gameSettings.keyBindAttack, "suggested", "boolean", null);
		addActionParams(map, mc.gameSettings.keyBindUseItem, "suggested", "boolean", null);
		addActionParams(map, mc.gameSettings.keyBindChat, "optional", "boolean", VRInputActionSet.GLOBAL);
		addActionParams(map, keyHotbarScroll, "optional", "vector2", null);
		addActionParams(map, keyHotbarSwipeX, "optional", "vector2", null);
		addActionParams(map, keyHotbarSwipeY, "optional", "vector2", null);
		addActionParams(map, keyMenuButton, "suggested", "boolean", VRInputActionSet.GLOBAL);
		addActionParams(map, keyTeleportFallback, "suggested", "vector1", null);
		addActionParams(map, keyFreeMoveRotate, "optional", "vector2", null);
		addActionParams(map, keyFreeMoveStrafe, "optional", "vector2", null);
		addActionParams(map, keyRotateLeft, "optional", "vector1", null);
		addActionParams(map, keyRotateRight, "optional", "vector1", null);
		addActionParams(map, keyRotateAxis, "optional", "vector2", null);
		addActionParams(map, keyRadialMenu, "suggested", "boolean", null);
		addActionParams(map, keySwapMirrorView, "optional", "boolean", VRInputActionSet.GLOBAL);
		addActionParams(map, keyToggleKeyboard, "optional", "boolean", VRInputActionSet.GLOBAL);
		addActionParams(map, keyMoveThirdPersonCam, "optional", "boolean", VRInputActionSet.GLOBAL);
		addActionParams(map, keyToggleHandheldCam, "optional", "boolean", VRInputActionSet.GLOBAL);
		addActionParams(map, keyQuickHandheldCam, "optional", "boolean", VRInputActionSet.GLOBAL);
		addActionParams(map, keyTrackpadTouch, "optional", "boolean", VRInputActionSet.TECHNICAL);
		addActionParams(map, keyVRInteract, "suggested", "boolean", VRInputActionSet.CONTEXTUAL);
		addActionParams(map, keyClimbeyGrab, "suggested", "boolean", null);
		addActionParams(map, keyClimbeyJump, "suggested", "boolean", null);
		addActionParams(map, GuiHandler.keyLeftClick, "suggested", "boolean", null);
		addActionParams(map, GuiHandler.keyScrollAxis, "optional", "vector2", null);
		addActionParams(map, GuiHandler.keyRightClick, "suggested", "boolean", null);
		addActionParams(map, GuiHandler.keyShift, "suggested", "boolean", null);
		addActionParams(map, GuiHandler.keyKeyboardClick, "suggested", "boolean", null);
		addActionParams(map, GuiHandler.keyKeyboardShift, "suggested", "boolean", null);

		File file = new File("customactionsets.txt");
		if (file.exists()) {
			System.out.println("Loading custom action set definitions...");
			try (BufferedReader br = new BufferedReader(new FileReader(file))) {
				String line;
				while ((line = br.readLine()) != null) {
					String[] tokens = line.split(":", 2);
					if (tokens.length < 2) {
						System.out.println("Invalid tokens: " + line);
						continue;
					}

					KeyBinding keyBinding = findKeyBinding(tokens[0]);
					if (keyBinding == null) {
						System.out.println("Unknown key binding: " + tokens[0]);
						continue;
					}
					if (getKeyBindings().contains(keyBinding)) {
						System.out.println("NO! Don't touch Vivecraft bindings!");
						continue;
					}

					VRInputActionSet actionSet = null;
					switch (tokens[1].toLowerCase()) {
					case "ingame":
						actionSet = VRInputActionSet.INGAME;
						break;
					case "gui":
						actionSet = VRInputActionSet.GUI;
						break;
					case "global":
						actionSet = VRInputActionSet.GLOBAL;
						break;
					}
					if (actionSet == null) {
						System.out.println("Unknown action set: " + tokens[1]);
						continue;
					}

					addActionParams(map, keyBinding, "optional", "boolean", actionSet);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return map;
	}
	
	protected void changeHotbar(int dir){
		if(mc.player == null || (mc.climbTracker.isGrabbingLadder() && 
				mc.climbTracker.isClaws(mc.player.getHeldItemMainhand()))) //never let go, jack.
		{}
		else{
			if (Reflector.ForgeHooksClient.exists() && mc.currentScreen == null)
				InputSimulator.scrollMouse(0, dir * 4);
			else
				mc.player.inventory.changeCurrentItem(dir);
		}
	}
	
	private void addActionParams(Map<String, ActionParams> map, KeyBinding keyBinding, String requirement, String type, VRInputActionSet actionSetOverride) {
		ActionParams params = new ActionParams(requirement, type, actionSetOverride);
		map.put(keyBinding.getKeyDescription(), params);
	}
	
	
	protected abstract void triggerBindingHapticPulse(KeyBinding keyHotbarNext2, int i);
	protected abstract ControllerType findActiveBindingControllerType(KeyBinding keyQuickHandheldCam2);
	public abstract void poll(long frameIndex);
	public abstract float[] getPlayAreaSize();
	public abstract boolean init();
	public abstract boolean postinit();
	public abstract Matrix4f getControllerComponentTransform(int c, String name);
	public abstract boolean hasThirdController();
	public abstract List<Long> getOrigins(VRInputAction vrInputAction);
}
