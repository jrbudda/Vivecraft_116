package org.vivecraft.provider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.vivecraft.api.Vec3History;
import org.vivecraft.control.ControllerType;
import org.vivecraft.control.HandedKeyBinding;
import org.vivecraft.control.HapticScheduler;
import org.vivecraft.control.TrackedController;
import org.vivecraft.control.TrackpadSwipeSampler;
import org.vivecraft.control.VRInputAction;
import org.vivecraft.control.VRInputActionSet;
import org.vivecraft.gameplay.screenhandlers.GuiHandler;
import org.vivecraft.gameplay.screenhandlers.KeyboardHandler;
import org.vivecraft.gameplay.screenhandlers.RadialHandler;
import org.vivecraft.render.RenderPass;
import org.vivecraft.settings.VRHotkeys;
import org.vivecraft.settings.VRSettings;
import org.vivecraft.utils.Angle;
import org.vivecraft.utils.HardwareType;
import org.vivecraft.utils.InputSimulator;
import org.vivecraft.utils.MCReflection;
import org.vivecraft.utils.Matrix4f;
import org.vivecraft.utils.MenuWorldExporter;
import org.vivecraft.utils.OpenVRUtil;
import org.vivecraft.utils.Quaternion;
import org.vivecraft.utils.Utils;
import org.vivecraft.utils.Vector2;
import org.vivecraft.utils.Vector3;
import org.vivecraft.utils.jinfinadeck;
import org.vivecraft.utils.jkatvr;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jna.Memory;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import jopenvr.HmdMatrix34_t;
import jopenvr.HmdVector2_t;
import jopenvr.InputOriginInfo_t;
import jopenvr.InputPoseActionData_t;
import jopenvr.JOpenVRLibrary;
import jopenvr.JOpenVRLibrary.EVREventType;
import jopenvr.JOpenVRLibrary.EVRInputError;
import jopenvr.RenderModel_ComponentState_t;
import jopenvr.RenderModel_ControllerMode_State_t;
import jopenvr.Texture_t;
import jopenvr.TrackedDevicePose_t;
import jopenvr.VRActiveActionSet_t;
import jopenvr.VREvent_t;
import jopenvr.VRTextureBounds_t;
import jopenvr.VRTextureDepthInfo_t;
import jopenvr.VRTextureWithDepth_t;
import jopenvr.VR_IVRApplications_FnTable;
import jopenvr.VR_IVRChaperone_FnTable;
import jopenvr.VR_IVRCompositor_FnTable;
import jopenvr.VR_IVRInput_FnTable;
import jopenvr.VR_IVROCSystem_FnTable;
import jopenvr.VR_IVROverlay_FnTable;
import jopenvr.VR_IVRRenderModels_FnTable;
import jopenvr.VR_IVRSettings_FnTable;
import jopenvr.VR_IVRSystem_FnTable;
import net.minecraft.block.TorchBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.WinGameScreen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.main.Main;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.glfw.GLFW;

public class MCOpenVR 
{
	static String initStatus;
	private static boolean initialized;
	private static boolean inputInitialized;

	static Minecraft mc;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	public static VR_IVRSystem_FnTable vrsystem;
	static VR_IVRCompositor_FnTable vrCompositor;
	static VR_IVROverlay_FnTable vrOverlay;
	static VR_IVRSettings_FnTable vrSettings;
	static VR_IVRRenderModels_FnTable vrRenderModels;
	static VR_IVRChaperone_FnTable vrChaperone;
	public static VR_IVROCSystem_FnTable vrOpenComposite;
	static VR_IVRApplications_FnTable vrApplications;
	public static VR_IVRInput_FnTable vrInput;

	private static IntByReference hmdErrorStore = new IntByReference();
	private static IntBuffer hmdErrorStoreBuf;

	private static TrackedDevicePose_t.ByReference hmdTrackedDevicePoseReference;
	private static TrackedDevicePose_t[] hmdTrackedDevicePoses;

	private static Matrix4f[] poseMatrices;
	private static Vec3d[] deviceVelocity;

	private LongByReference oHandle = new LongByReference();

	// position/orientation of headset and eye offsets
	private static final Matrix4f hmdPose = new Matrix4f();
	public static final Matrix4f hmdRotation = new Matrix4f();
	public static Matrix4f hmdProjectionLeftEye;
	public static Matrix4f hmdProjectionRightEye;
	
	static Matrix4f hmdPoseLeftEye = new Matrix4f();
	static Matrix4f hmdPoseRightEye = new Matrix4f();
	static boolean initSuccess = false, flipEyes = false;

	private static IntBuffer hmdDisplayFrequency;

	private static float vsyncToPhotons;
	private static double timePerFrame, frameCountRun;
	private static long frameCount;

	public static Vec3History hmdHistory = new Vec3History();
	public static Vec3History hmdPivotHistory = new Vec3History();
	public static Vec3History[] controllerHistory = new Vec3History[] { new Vec3History(), new Vec3History()};

	/**
	 * Do not make this public and reference it! Call the {@link #getHardwareType()} method instead!
	 */
	private static HardwareType detectedHardware = HardwareType.VIVE;

	// TextureIDs of framebuffers for each eye
	private int LeftEyeTextureId;

	final static VRTextureBounds_t texBounds = new VRTextureBounds_t();
	final static Texture_t texType0 = new Texture_t();
	final static Texture_t texType1 = new Texture_t();
	// aiming

	static Vec3d[] aimSource = new Vec3d[3];

	public static Vector3 offset=new Vector3(0,0,0);

	static boolean[] controllerTracking = new boolean[3];
	public static TrackedController[] controllers = new TrackedController[2];

	// Controllers
	public static final int RIGHT_CONTROLLER = 0;
	public static final int LEFT_CONTROLLER = 1;
	public static final int THIRD_CONTROLLER = 2;
	private static Matrix4f[] controllerPose = new Matrix4f[3];
	private static Matrix4f[] controllerRotation = new Matrix4f[3];
	private static Matrix4f[] handRotation = new Matrix4f[3];
	public static int[] controllerDeviceIndex = new int[3];

	private static Queue<VREvent_t> vrEvents = new LinkedList<>();

	public static boolean hudPopup = true;

	static boolean headIsTracking;

	private static int moveModeSwitchCount = 0;

	public static boolean isWalkingAbout;
	private static boolean isFreeRotate;
	private static ControllerType walkaboutController;
	private static ControllerType freeRotateController;
	private static float walkaboutYawStart;
	private static float hmdForwardYaw;
	public static boolean ignorePressesNextFrame = false;
	
	private static Map<String, VRInputAction> inputActions = new HashMap<>();
	private static Map<String, VRInputAction> inputActionsByKeyBinding = new HashMap<>();
	private static Map<VRInputActionSet, Long> actionSetHandles = new EnumMap<>(VRInputActionSet.class);

	private static long leftPoseHandle;
	private static long rightPoseHandle;
	private static long leftHapticHandle;
	private static long rightHapticHandle;
	private static long externalCameraPoseHandle;

	private static long leftControllerHandle;
	private static long rightControllerHandle;

	private static Map<String, TrackpadSwipeSampler> trackpadSwipeSamplers = new HashMap<>();
	private static Map<String, Boolean> axisUseTracker = new HashMap<>();

	private static InputPoseActionData_t.ByReference poseData;
	private static InputOriginInfo_t.ByReference originInfo;
	private static VRActiveActionSet_t.ByReference activeActionSetsReference;

	private static HapticScheduler hapticScheduler;

	public static boolean mrMovingCamActive;
	public static Vec3d mrControllerPos = Vec3d.ZERO;
	public static float mrControllerPitch;
	public static float mrControllerYaw;
	public static float mrControllerRoll;

	private static Set<KeyBinding> keyBindingSet;

	public String getName() {
		return "OpenVR";
	}

	public String getID() {
		return "openvr";
	}

	public static final KeyBinding keyHotbarNext = new KeyBinding("vivecraft.key.hotbarNext", GLFW.GLFW_KEY_PAGE_UP, "key.categories.inventory");
	public static final KeyBinding keyHotbarPrev = new KeyBinding("vivecraft.key.hotbarPrev", GLFW.GLFW_KEY_PAGE_DOWN, "key.categories.inventory");
	public static final KeyBinding keyHotbarScroll = new KeyBinding("vivecraft.key.hotbarScroll", GLFW.GLFW_KEY_UNKNOWN, "key.categories.inventory"); // dummy binding
	public static final KeyBinding keyHotbarSwipeX = new KeyBinding("vivecraft.key.hotbarSwipeX", GLFW.GLFW_KEY_UNKNOWN, "key.categories.inventory"); // dummy binding
	public static final KeyBinding keyHotbarSwipeY = new KeyBinding("vivecraft.key.hotbarSwipeY", GLFW.GLFW_KEY_UNKNOWN, "key.categories.inventory"); // dummy binding
	public static final KeyBinding keyRotateLeft = new KeyBinding("vivecraft.key.rotateLeft", GLFW.GLFW_KEY_LEFT, "key.categories.movement");
	public static final KeyBinding keyRotateRight = new KeyBinding("vivecraft.key.rotateRight", GLFW.GLFW_KEY_RIGHT, "key.categories.movement");
	public static final KeyBinding keyRotateAxis = new KeyBinding("vivecraft.key.rotateAxis", GLFW.GLFW_KEY_UNKNOWN, "key.categories.movement"); // dummy binding
	public static final KeyBinding keyWalkabout = new KeyBinding("vivecraft.key.walkabout", GLFW.GLFW_KEY_END, "key.categories.movement");
	public static final KeyBinding keyRotateFree = new KeyBinding("vivecraft.key.rotateFree", GLFW.GLFW_KEY_HOME, "key.categories.movement");
	public static final KeyBinding keyTeleport = new KeyBinding("vivecraft.key.teleport", GLFW.GLFW_KEY_UNKNOWN, "key.categories.movement");
	public static final KeyBinding keyTeleportFallback = new KeyBinding("vivecraft.key.teleportFallback", GLFW.GLFW_KEY_UNKNOWN, "key.categories.movement");
	public static final KeyBinding keyFreeMoveRotate = new KeyBinding("vivecraft.key.freeMoveRotate", GLFW.GLFW_KEY_UNKNOWN, "key.categories.movement"); // dummy binding
	public static final KeyBinding keyFreeMoveStrafe = new KeyBinding("vivecraft.key.freeMoveStrafe", GLFW.GLFW_KEY_UNKNOWN, "key.categories.movement"); // dummy binding
	public static final KeyBinding keyToggleMovement = new KeyBinding("vivecraft.key.toggleMovement", GLFW.GLFW_KEY_UNKNOWN, "key.categories.movement");
	public static final KeyBinding keyQuickTorch = new KeyBinding("vivecraft.key.quickTorch", GLFW.GLFW_KEY_INSERT, "key.categories.gameplay");
	public static final KeyBinding keyMenuButton = new KeyBinding("vivecraft.key.ingameMenuButton", GLFW.GLFW_KEY_UNKNOWN, "key.categories.ui");
	public static final KeyBinding keyExportWorld = new KeyBinding("vivecraft.key.exportWorld", GLFW.GLFW_KEY_UNKNOWN, "key.categories.misc");
	public static final KeyBinding keyRadialMenu = new KeyBinding("vivecraft.key.radialMenu", GLFW.GLFW_KEY_UNKNOWN, "key.categories.ui");
	public static final KeyBinding keySwapMirrorView = new KeyBinding("vivecraft.key.swapMirrorView", GLFW.GLFW_KEY_UNKNOWN, "key.categories.misc");
	public static final KeyBinding keyToggleKeyboard = new KeyBinding("vivecraft.key.toggleKeyboard", GLFW.GLFW_KEY_UNKNOWN, "key.categories.ui");
	public static final KeyBinding keyMoveThirdPersonCam = new KeyBinding("vivecraft.key.moveThirdPersonCam", GLFW.GLFW_KEY_UNKNOWN, "key.categories.misc");
	public static final KeyBinding keyTogglePlayerList = new KeyBinding("vivecraft.key.togglePlayerList", GLFW.GLFW_KEY_UNKNOWN, "key.categories.multiplayer");
	public static final HandedKeyBinding keyTrackpadTouch = new HandedKeyBinding("vivecraft.key.trackpadTouch", GLFW.GLFW_KEY_UNKNOWN, "key.categories.misc"); // used for swipe sampler
	public static final HandedKeyBinding keyVRInteract = new HandedKeyBinding("vivecraft.key.vrInteract", GLFW.GLFW_KEY_UNKNOWN,"key.categories.gameplay");
	public static final HandedKeyBinding keyClimbeyGrab = new HandedKeyBinding("vivecraft.key.climbeyGrab", GLFW.GLFW_KEY_UNKNOWN,"vivecraft.key.category.climbey");
	public static final HandedKeyBinding keyClimbeyJump = new HandedKeyBinding("vivecraft.key.climbeyJump", GLFW.GLFW_KEY_UNKNOWN,"vivecraft.key.category.climbey");


	public MCOpenVR()
	{
		super();

		for (int c=0;c<3;c++)
		{
			aimSource[c] = new Vec3d(0.0D, 0.0D, 0.0D);
			controllerPose[c] = new Matrix4f();
			controllerRotation[c] = new Matrix4f();
			handRotation[c] = new Matrix4f();
			controllerDeviceIndex[c] = -1;

			poseData = new InputPoseActionData_t.ByReference();
			poseData.setAutoRead(false);
			poseData.setAutoWrite(false);
			poseData.setAutoSynch(false);

			originInfo = new InputOriginInfo_t.ByReference();
			originInfo.setAutoRead(false);
			originInfo.setAutoWrite(false);
			originInfo.setAutoSynch(false);
		}
	}

	private static boolean tried;

	public static boolean init()  throws Exception
	{
		if ( initialized )
			return true;

		if ( tried )
			return initialized;

		tried = true;

		mc = Minecraft.getInstance();

		String osname = System.getProperty("os.name").toLowerCase();
		String osarch= System.getProperty("os.arch").toLowerCase();

		String osFolder = "win";

		if( osname.contains("linux")){
			osFolder = "linux";
		}
		else if( osname.contains("mac")){
			osFolder = "osx";
		}

		if (osarch.contains("64"))
		{
			osFolder += "64";
		} else {
			osFolder += "32";
		}

		Utils.unpackNatives(osFolder);

		String openVRPath = new File("openvr/" + osFolder).getAbsolutePath();
		System.out.println("Adding OpenVR search path: " + openVRPath);
		NativeLibrary.addSearchPath("openvr_api", openVRPath);

		if(jopenvr.JOpenVRLibrary.VR_IsHmdPresent() == 0){
			initStatus =  "VR Headset not detected.";
			return false;
		}

		try {
			initializeJOpenVR();
			initOpenVRCompositor() ;
			initOpenVRSettings();
			initOpenVRRenderModels();
			initOpenVRChaperone();
			initOpenVRApplications();
			initOpenVRInput();
			initOpenComposite();
		} catch (Exception e) {
			e.printStackTrace();
			initSuccess = false;
			initStatus = e.getLocalizedMessage();
			return false;
		}
		
		if (vrInput == null) {
			System.out.println("Controller input not available. Forcing seated mode.");
			mc.vrSettings.seated = true;
		}
		
		System.out.println( "OpenVR initialized & VR connected." );

		controllers[RIGHT_CONTROLLER] = new TrackedController(ControllerType.RIGHT);
		controllers[LEFT_CONTROLLER] = new TrackedController(ControllerType.LEFT);

		deviceVelocity = new Vec3d[JOpenVRLibrary.k_unMaxTrackedDeviceCount];

		for(int i=0;i<poseMatrices.length;i++)
		{
			poseMatrices[i] = new Matrix4f();
			deviceVelocity[i] = new Vec3d(0,0,0);
		}

		hapticScheduler = new HapticScheduler();

		initialized = true;

		if(Main.katvr){
			try {
				System.out.println( "Waiting for KATVR...." );
				Utils.unpackNatives("katvr");
				NativeLibrary.addSearchPath(jkatvr.KATVR_LIBRARY_NAME, new File("openvr/katvr").getAbsolutePath());
				jkatvr.Init(1);
				jkatvr.Launch();
				if(jkatvr.CheckForLaunch()){
					System.out.println( "KATVR Loaded" );
				}else {
					System.out.println( "KATVR Failed to load" );
				}

			} catch (Exception e) {
				System.out.println( "KATVR crashed: " + e.getMessage() );
			}
		}

		if(Main.infinadeck){
			try {
				System.out.println( "Waiting for Infinadeck...." );
				Utils.unpackNatives("infinadeck");
				NativeLibrary.addSearchPath(jinfinadeck.INFINADECK_LIBRARY_NAME, new File("openvr/infinadeck").getAbsolutePath());

				if(jinfinadeck.InitConnection()){
					jinfinadeck.CheckConnection();
					System.out.println( "Infinadeck Loaded" );
				}else {
					System.out.println( "Infinadeck Failed to load" );
				}
				
			} catch (Exception e) {
				System.out.println( "Infinadeck crashed: " + e.getMessage() );
			}
		}
		return true;
	}

	public static boolean isError(){
		return hmdErrorStore.getValue() != 0 || hmdErrorStoreBuf.get(0) != 0;
	}

	public static int getError(){
		return hmdErrorStore.getValue() != 0 ? hmdErrorStore.getValue() : hmdErrorStoreBuf.get(0);
	}

	public static Set<KeyBinding> getKeyBindings() {
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

	@SuppressWarnings("unchecked")
	public static KeyBinding[] initializeBindings(KeyBinding[] keyBindings) {
		for (KeyBinding keyBinding : getKeyBindings())
			keyBindings = ArrayUtils.add(keyBindings, keyBinding);

		Map<String, Integer> co = (Map<String, Integer>)MCReflection.KeyBinding_CATEGORY_ORDER.get(null);
		co.put("vivecraft.key.category.gui", 8);
		co.put("vivecraft.key.category.climbey", 9);
		co.put("vivecraft.key.category.keyboard", 10);

		return keyBindings;
	}

	private static void installApplicationManifest(boolean force) {
		File file = new File("openvr/vivecraft.vrmanifest");
		Utils.loadAssetToFile("vivecraft.vrmanifest", file, true);

		File customFile = new File("openvr/custom.vrmanifest");
		if (customFile.exists())
			file = customFile;

		if (vrApplications != null) {
			String appKey;
			try {
				Map map = new Gson().fromJson(new FileReader(file), Map.class);
				appKey = ((Map)((List)map.get("applications")).get(0)).get("app_key").toString();
			} catch (Exception e) {
				System.out.println("Error reading appkey from manifest");
				e.printStackTrace();
				return;
			}

			System.out.println("Appkey: " + appKey);

			if (!force && vrApplications.IsApplicationInstalled.apply(ptrFomrString(appKey)) != 0) {
				System.out.println("Application manifest already installed");
			} else {
				// 0 = Permanent manifest which will show up in the library
				// 1 = Temporary manifest which will only show up in bindings until SteamVR is restarted
				int error = vrApplications.AddApplicationManifest.apply(ptrFomrString(file.getAbsolutePath()), (byte)1);
				if (error != 0) {
					System.out.println("Failed to install application manifest: " + vrApplications.GetApplicationsErrorNameFromEnum.apply(error).getString(0));
					return;
				} else {
					System.out.println("Application manifest installed successfully");
				}
			}

			// OpenVR doc says pid = 0 will use the calling process, but it actually doesn't, so we
			// have to use this dumb hack that *probably* works on all relevant platforms.
			// TODO: When Minecraft one day requires Java 9+, we can use ProcessHandle.current().pid()
			int pid;
			try {
				String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
				pid = Integer.parseInt(runtimeName.split("@")[0]);
			} catch (Exception e) {
				System.out.println("Error getting process id");
				e.printStackTrace();
				return;
			}

			int error = vrApplications.IdentifyApplication.apply(pid, ptrFomrString(appKey));
			if (error != 0) {
				System.out.println("Failed to identify application: " + vrApplications.GetApplicationsErrorNameFromEnum.apply(error).getString(0));
			} else {
				System.out.println("Application identified successfully");
			}
		}
	}

	private static class ActionParams {
		final String requirement;
		final String type;
		final VRInputActionSet actionSetOverride;

		ActionParams(String requirement, String type, VRInputActionSet actionSetOverride) {
			this.requirement = requirement;
			this.type = type;
			this.actionSetOverride = actionSetOverride;
		}
	}

	public static void initInputAndApplication() {
		populateInputActions();
		if (vrInput == null) return;
		generateActionManifest();
		loadActionManifest();
		loadActionHandles();
		installApplicationManifest(false);
		inputInitialized = true;
	}

	private static void populateInputActions() {
		Map<String, ActionParams> actionParams = getSpecialActionParams();
		for (final KeyBinding keyBinding : mc.gameSettings.keyBindings) {
			ActionParams params = actionParams.getOrDefault(keyBinding.getKeyDescription(), new ActionParams("optional", "boolean", null));
			VRInputAction action = new VRInputAction(keyBinding, params.requirement, params.type, params.actionSetOverride);
			inputActions.put(action.name, action);
		}
		for (VRInputAction action : inputActions.values()) {
			inputActionsByKeyBinding.put(action.keyBinding.getKeyDescription(), action);
		}

		getInputAction(MCOpenVR.keyVRInteract).setPriority(5).setEnabled(false);
		getInputAction(MCOpenVR.keyClimbeyGrab).setPriority(10).setEnabled(false);
		//getInputAction(MCOpenVR.keyClimbeyJump).setPriority(10).setEnabled(false);
		getInputAction(MCOpenVR.keyClimbeyJump).setEnabled(false);
		getInputAction(GuiHandler.keyKeyboardClick).setPriority(50);
		getInputAction(GuiHandler.keyKeyboardShift).setPriority(50);
	}

	// This is for bindings with specific requirement/type params, anything not listed will default to optional and boolean
	// See OpenVR docs for valid values: https://github.com/ValveSoftware/openvr/wiki/Action-manifest#actions
	private static Map<String, ActionParams> getSpecialActionParams() {
		Map<String, ActionParams> map = new HashMap<>();

		addActionParams(map, mc.gameSettings.keyBindForward, "optional", "vector1", null);
		addActionParams(map, mc.gameSettings.keyBindBack, "optional", "vector1", null);
		addActionParams(map, mc.gameSettings.keyBindLeft, "optional", "vector1", null);
		addActionParams(map, mc.gameSettings.keyBindRight, "optional", "vector1", null);
		addActionParams(map, mc.gameSettings.keyBindInventory, "mandatory", "boolean", VRInputActionSet.GLOBAL);
		addActionParams(map, mc.gameSettings.keyBindAttack, "mandatory", "boolean", null);
		addActionParams(map, mc.gameSettings.keyBindUseItem, "mandatory", "boolean", null);
		addActionParams(map, mc.gameSettings.keyBindChat, "optional", "boolean", VRInputActionSet.GLOBAL);
		addActionParams(map, MCOpenVR.keyHotbarScroll, "optional", "vector2", null);
		addActionParams(map, MCOpenVR.keyHotbarSwipeX, "optional", "vector2", null);
		addActionParams(map, MCOpenVR.keyHotbarSwipeY, "optional", "vector2", null);
		addActionParams(map, MCOpenVR.keyMenuButton, "mandatory", "boolean", VRInputActionSet.GLOBAL);
		addActionParams(map, MCOpenVR.keyTeleportFallback, "suggested", "vector1", null);
		addActionParams(map, MCOpenVR.keyFreeMoveRotate, "optional", "vector2", null);
		addActionParams(map, MCOpenVR.keyFreeMoveStrafe, "optional", "vector2", null);
		addActionParams(map, MCOpenVR.keyRotateLeft, "optional", "vector1", null);
		addActionParams(map, MCOpenVR.keyRotateRight, "optional", "vector1", null);
		addActionParams(map, MCOpenVR.keyRotateAxis, "optional", "vector2", null);
		addActionParams(map, MCOpenVR.keyRadialMenu, "suggested", "boolean", null);
		addActionParams(map, MCOpenVR.keySwapMirrorView, "optional", "boolean", VRInputActionSet.GLOBAL);
		addActionParams(map, MCOpenVR.keyToggleKeyboard, "optional", "boolean", VRInputActionSet.GLOBAL);
		addActionParams(map, MCOpenVR.keyMoveThirdPersonCam, "optional", "boolean", VRInputActionSet.GLOBAL);
		addActionParams(map, MCOpenVR.keyTrackpadTouch, "optional", "boolean", VRInputActionSet.TECHNICAL);
		addActionParams(map, MCOpenVR.keyVRInteract, "suggested", "boolean", VRInputActionSet.CONTEXTUAL);
		addActionParams(map, MCOpenVR.keyClimbeyGrab, "suggested", "boolean", null);
		addActionParams(map, MCOpenVR.keyClimbeyJump, "suggested", "boolean", null);
		addActionParams(map, GuiHandler.keyLeftClick, "mandatory", "boolean", null);
		addActionParams(map, GuiHandler.keyScrollAxis, "optional", "vector2", null);
		addActionParams(map, GuiHandler.keyRightClick, "suggested", "boolean", null);
		addActionParams(map, GuiHandler.keyShift, "suggested", "boolean", null);
		addActionParams(map, GuiHandler.keyKeyboardClick, "mandatory", "boolean", null);
		addActionParams(map, GuiHandler.keyKeyboardShift, "suggested", "boolean", null);

		return map;
	}

	private static void addActionParams(Map<String, ActionParams> map, KeyBinding keyBinding, String requirement, String type, VRInputActionSet actionSetOverride) {
		ActionParams params = new ActionParams(requirement, type, actionSetOverride);
		map.put(keyBinding.getKeyDescription(), params);
	}

	public static final String ACTION_LEFT_HAND = "/actions/global/in/lefthand";
	public static final String ACTION_RIGHT_HAND = "/actions/global/in/righthand";
	public static final String ACTION_LEFT_HAPTIC = "/actions/global/out/lefthaptic";
	public static final String ACTION_RIGHT_HAPTIC = "/actions/global/out/righthaptic";
	public static final String ACTION_EXTERNAL_CAMERA = "/actions/mixedreality/in/externalcamera";

	private static void generateActionManifest() {
		Map<String, Object> jsonMap = new HashMap<>();

		List<Map<String, Object>> actionSets = new ArrayList<>();
		for (VRInputActionSet actionSet : VRInputActionSet.values()) {
			String usage = actionSet.usage;
			if (actionSet.advanced && !mc.vrSettings.allowAdvancedBindings)
				usage = "hidden";
			actionSets.add(ImmutableMap.<String, Object>builder().put("name", actionSet.name).put("usage", usage).build());
		}
		jsonMap.put("action_sets", actionSets);

		// Sort the bindings so they're easy to look through in SteamVR
		List<VRInputAction> sortedActions = new ArrayList<>(inputActions.values());
		sortedActions.sort(Comparator.comparing(action -> action.keyBinding));

		List<Map<String, Object>> actions = new ArrayList<>();
		for (VRInputAction action : sortedActions) {
			actions.add(ImmutableMap.<String, Object>builder().put("name", action.name).put("requirement", action.requirement).put("type", action.type).build());
		}
		// Bunch of hard-coded bullshit
		actions.add(ImmutableMap.<String, Object>builder().put("name", ACTION_LEFT_HAND).put("requirement", "suggested").put("type", "pose").build());
		actions.add(ImmutableMap.<String, Object>builder().put("name", ACTION_RIGHT_HAND).put("requirement", "suggested").put("type", "pose").build());
		actions.add(ImmutableMap.<String, Object>builder().put("name", ACTION_EXTERNAL_CAMERA).put("requirement", "optional").put("type", "pose").build());
		actions.add(ImmutableMap.<String, Object>builder().put("name", ACTION_LEFT_HAPTIC).put("requirement", "suggested").put("type", "vibration").build());
		actions.add(ImmutableMap.<String, Object>builder().put("name", ACTION_RIGHT_HAPTIC).put("requirement", "suggested").put("type", "vibration").build());
		jsonMap.put("actions", actions);

		Map<String, Object> localization = new HashMap<>();
		for (VRInputAction action : sortedActions) {
			localization.put(action.name, I18n.format(action.keyBinding.getKeyCategory()) + " - " + I18n.format(action.keyBinding.getKeyDescription()));
		}
		for (VRInputActionSet actionSet : VRInputActionSet.values()) {
			localization.put(actionSet.name, I18n.format(actionSet.localizedName));
		}
		// More hard-coded bullshit
		localization.put(ACTION_LEFT_HAND, "Left Hand Pose");
		localization.put(ACTION_RIGHT_HAND, "Right Hand Pose");
		localization.put(ACTION_EXTERNAL_CAMERA, "External Camera");
		localization.put(ACTION_LEFT_HAPTIC, "Left Hand Haptic");
		localization.put(ACTION_RIGHT_HAPTIC, "Right Hand Haptic");
		localization.put("language_tag", "en_US");
		jsonMap.put("localization", ImmutableList.<Map<String, Object>>builder().add(localization).build());

		List<Map<String, Object>> defaultBindings = new ArrayList<>();
		defaultBindings.add(ImmutableMap.<String, Object>builder().put("controller_type", "vive_controller").put("binding_url", "vive_defaults.json").build());
		defaultBindings.add(ImmutableMap.<String, Object>builder().put("controller_type", "oculus_touch").put("binding_url", "oculus_defaults.json").build());
		defaultBindings.add(ImmutableMap.<String, Object>builder().put("controller_type", "holographic_controller").put("binding_url", "wmr_defaults.json").build());
		defaultBindings.add(ImmutableMap.<String, Object>builder().put("controller_type", "knuckles").put("binding_url", "knuckles_defaults.json").build());
		defaultBindings.add(ImmutableMap.<String, Object>builder().put("controller_type", "vive_cosmos_controller").put("binding_url", "cosmos_defaults.json").build());
		defaultBindings.add(ImmutableMap.<String, Object>builder().put("controller_type", "vive_tracker_camera").put("binding_url", "tracker_defaults.json").build());
		jsonMap.put("default_bindings", defaultBindings);

		try {			
			new File("openvr/input").mkdirs();
			try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("openvr/input/action_manifest.json"), StandardCharsets.UTF_8)) {
				GSON.toJson(jsonMap, writer);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to write action manifest", e);
		}

		String rev = mc.vrSettings.vrReverseHands ? "_reversed" : "";
		Utils.loadAssetToFile("input/vive_defaults" + rev + ".json", new File("openvr/input/vive_defaults.json"), false);
		Utils.loadAssetToFile("input/oculus_defaults" + rev + ".json", new File("openvr/input/oculus_defaults.json"), false);
		Utils.loadAssetToFile("input/wmr_defaults" + rev + ".json", new File("openvr/input/wmr_defaults.json"), false);
		Utils.loadAssetToFile("input/knuckles_defaults" + rev + ".json", new File("openvr/input/knuckles_defaults.json"), false);
		Utils.loadAssetToFile("input/cosmos_defaults" + rev + ".json", new File("openvr/input/cosmos_defaults.json"), false);
		Utils.loadAssetToFile("input/tracker_defaults.json", new File("openvr/input/tracker_defaults.json"), false);
	}

	private static void loadActionManifest() {
		int error = vrInput.SetActionManifestPath.apply(ptrFomrString(new File("openvr/input/action_manifest.json").getAbsolutePath()));
		if (error != 0) {
			throw new RuntimeException("Failed to load action manifest: " + getInputError(error));
		}
	}

	private static void loadActionHandles() {
		LongByReference longRef = new LongByReference();

		for (VRInputAction action : inputActions.values()) {
			int error = vrInput.GetActionHandle.apply(ptrFomrString(action.name), longRef);
			if (error != 0)
				throw new RuntimeException("Error getting action handle for '" + action.name + "': " + getInputError(error));
			action.setHandle(longRef.getValue());
		}

		leftPoseHandle = getActionHandle(ACTION_LEFT_HAND);
		rightPoseHandle = getActionHandle(ACTION_RIGHT_HAND);
		leftHapticHandle = getActionHandle(ACTION_LEFT_HAPTIC);
		rightHapticHandle = getActionHandle(ACTION_RIGHT_HAPTIC);
		externalCameraPoseHandle = getActionHandle(ACTION_EXTERNAL_CAMERA);

		for (VRInputActionSet actionSet : VRInputActionSet.values()) {
			int error = vrInput.GetActionSetHandle.apply(ptrFomrString(actionSet.name), longRef);
			if (error != 0)
				throw new RuntimeException("Error getting action set handle for '" + actionSet.name + "': " + getInputError(error));
			actionSetHandles.put(actionSet, longRef.getValue());
		}

		leftControllerHandle = getInputSourceHandle("/user/hand/left");
		rightControllerHandle = getInputSourceHandle("/user/hand/right");
	}

	private static long getActionHandle(String name) {
		LongByReference longRef = new LongByReference();
		int error = vrInput.GetActionHandle.apply(ptrFomrString(name), longRef);
		if (error != 0)
			throw new RuntimeException("Error getting action handle for '" + name + "': " + getInputError(error));
		return longRef.getValue();
	}

	private static VRActiveActionSet_t[] getActiveActionSets() {
		ArrayList<VRInputActionSet> list = new ArrayList<>();
		list.add(VRInputActionSet.GLOBAL);
		list.add(VRInputActionSet.MIXED_REALITY);
		list.add(VRInputActionSet.TECHNICAL);
		if (mc.currentScreen == null) {
			list.add(VRInputActionSet.INGAME);
		//	if (getInputActionsInSet(VRInputActionSet.CONTEXTUAL).stream().anyMatch(VRInputAction::isEnabledRaw))
			list.add(VRInputActionSet.CONTEXTUAL);
		} else {
			list.add(VRInputActionSet.GUI);
		}
		if (KeyboardHandler.Showing || RadialHandler.isShowing())
			list.add(VRInputActionSet.KEYBOARD);

		activeActionSetsReference = new VRActiveActionSet_t.ByReference();
		activeActionSetsReference.setAutoRead(false);
		activeActionSetsReference.setAutoWrite(false);
		activeActionSetsReference.setAutoSynch(false);

		VRActiveActionSet_t[] activeActionSets = (VRActiveActionSet_t[])activeActionSetsReference.toArray(list.size());

		for (int i = 0; i < list.size(); i++) {
			VRInputActionSet actionSet = list.get(i);
			activeActionSets[i].ulActionSet = getActionSetHandle(actionSet);
			activeActionSets[i].ulRestrictedToDevice = JOpenVRLibrary.k_ulInvalidInputValueHandle;
			activeActionSets[i].nPriority = 0;

			activeActionSets[i].setAutoRead(false);
			activeActionSets[i].setAutoWrite(false);
			activeActionSets[i].setAutoSynch(false);
			activeActionSets[i].write();
		}

		activeActionSetsReference.write();
		return activeActionSets;
	}

	private static void initializeJOpenVR() {
		hmdErrorStoreBuf = IntBuffer.allocate(1);
		vrsystem = null;
		JOpenVRLibrary.VR_InitInternal(hmdErrorStoreBuf, JOpenVRLibrary.EVRApplicationType.EVRApplicationType_VRApplication_Scene);

		if(!isError()) {
			// ok, try and get the vrsystem pointer..
			vrsystem = new VR_IVRSystem_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRSystem_Version, hmdErrorStoreBuf));
		}

		if( vrsystem == null || isError()) {
			throw new RuntimeException(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(getError()).getString(0));
		} else {

			vrsystem.setAutoSynch(false);
			vrsystem.read();

			System.out.println("OpenVR System Initialized OK.");

			hmdDisplayFrequency = IntBuffer.allocate(1);
			hmdDisplayFrequency.put( (int) JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayFrequency_Float);
			hmdTrackedDevicePoseReference = new TrackedDevicePose_t.ByReference();
			hmdTrackedDevicePoses = (TrackedDevicePose_t[])hmdTrackedDevicePoseReference.toArray(JOpenVRLibrary.k_unMaxTrackedDeviceCount);
			poseMatrices = new Matrix4f[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
			for(int i=0;i<poseMatrices.length;i++) poseMatrices[i] = new Matrix4f();

			timePerFrame = 1.0 / hmdDisplayFrequency.get(0);

			// disable all this stuff which kills performance
			hmdTrackedDevicePoseReference.setAutoRead(false);
			hmdTrackedDevicePoseReference.setAutoWrite(false);
			hmdTrackedDevicePoseReference.setAutoSynch(false);
			
			for(int i=0;i<JOpenVRLibrary.k_unMaxTrackedDeviceCount;i++) {
				hmdTrackedDevicePoses[i].setAutoRead(false);
				hmdTrackedDevicePoses[i].setAutoWrite(false);
				hmdTrackedDevicePoses[i].setAutoSynch(false);
			}

			initSuccess = true;
		}
	}

	private static Pointer ptrFomrString(String in){
		Pointer p = new Memory(in.getBytes(StandardCharsets.UTF_8).length + 1);
		p.setString(0, in, StandardCharsets.UTF_8.name());
		return p;
	}
	
	static float getSuperSampling(){
		if (vrSettings == null)
			return -1;
		return 
				MCOpenVR.vrSettings.GetFloat.apply(MCOpenVR.ptrFomrString("steamvr"),MCOpenVR.ptrFomrString("supersampleScale"), hmdErrorStore);
	}


	static void debugOut(int deviceindex){
		System.out.println("******************* VR DEVICE: " + deviceindex + " *************************");
		for(Field i :JOpenVRLibrary.ETrackedDeviceProperty.class.getDeclaredFields()){
			try {
				String[] ts = i.getName().split("_");
				String Type = ts[ts.length - 1];
				String out = "";
				if (Type.equals("Float")) {
					out += i.getName() + " " + vrsystem.GetFloatTrackedDeviceProperty.apply(deviceindex, i.getInt(null), hmdErrorStore);
				}				else if (Type.equals("String")) {
					Pointer pointer = new Memory(JOpenVRLibrary.k_unMaxPropertyStringSize);
					int len = vrsystem.GetStringTrackedDeviceProperty.apply(deviceindex, i.getInt(null), pointer, JOpenVRLibrary.k_unMaxPropertyStringSize - 1, hmdErrorStore);
					out += i.getName() + " " + pointer.getString(0);
				} else if (Type.equals("Bool")) {
					out += i.getName() + " " + vrsystem.GetBoolTrackedDeviceProperty.apply(deviceindex, i.getInt(null), hmdErrorStore);
				} else if (Type.equals("Int32")) {
					out += i.getName() + " " + vrsystem.GetInt32TrackedDeviceProperty.apply(deviceindex, i.getInt(null), hmdErrorStore);
				} else if (Type.equals("Uint64")) {
					out += i.getName() + " " + vrsystem.GetUint64TrackedDeviceProperty.apply(deviceindex, i.getInt(null), hmdErrorStore);
				}else {
					out += i.getName() + " (skipped)" ; 
				}
				System.out.println(out.replace("ETrackedDeviceProperty_Prop_", ""));
			}catch (IllegalAccessException e){
				e.printStackTrace();
			}
		}
		System.out.println("******************* END VR DEVICE: " + deviceindex + " *************************");

	}

	static void initOpenVRApplications() {
		vrApplications = new VR_IVRApplications_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRApplications_Version, hmdErrorStoreBuf));
		if (!isError()) {
			vrApplications.setAutoSynch(false);
			vrApplications.read();
			System.out.println("OpenVR Applications initialized OK");
		} else {
			System.out.println("VRApplications init failed: " + jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(getError()).getString(0));
			vrApplications = null;
		}
	}

	public static void initOpenVRSettings()
	{
		vrSettings = new VR_IVRSettings_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRSettings_Version, hmdErrorStoreBuf));
		if (!isError()) {
			vrSettings.setAutoSynch(false);
			vrSettings.read();					
			System.out.println("OpenVR Settings initialized OK");
		} else {
				System.out.println("VRSettings init failed: " + jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(getError()).getString(0));
				vrSettings = null;
		}
	}


	public static void initOpenVRRenderModels()
	{
		vrRenderModels = new VR_IVRRenderModels_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRRenderModels_Version, hmdErrorStoreBuf));
		if (!isError()) {
			vrRenderModels.setAutoSynch(false);
			vrRenderModels.read();			
			System.out.println("OpenVR RenderModels initialized OK");
		} else {
				System.out.println("VRRenderModels init failed: " + jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(getError()).getString(0));
				vrRenderModels = null;
		}
	}

	private static void initOpenVRChaperone() {
		vrChaperone = new VR_IVRChaperone_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRChaperone_Version, hmdErrorStoreBuf));
		if (!isError()) {
			vrChaperone.setAutoSynch(false);
			vrChaperone.read();
			System.out.println("OpenVR chaperone initialized.");
		} else {
				System.out.println("VRChaperone init failed: " + jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(getError()).getString(0));
				vrChaperone = null;
		}
	}

	private static void initOpenVRInput() {
		vrInput = new VR_IVRInput_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRInput_Version, hmdErrorStoreBuf));
		if (!isError()) {
			vrInput.setAutoSynch(false);
			vrInput.read();
			System.out.println("OpenVR Input initialized OK");
		} else {
			System.out.println("VRInput init failed: " + jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(getError()).getString(0));
			vrInput = null;
		}
	}

	private static void initOpenComposite() {
		vrOpenComposite = new VR_IVROCSystem_FnTable(JOpenVRLibrary.VR_GetGenericInterface(VR_IVROCSystem_FnTable.Version, hmdErrorStoreBuf));
		if (!isError()) {
			vrOpenComposite.setAutoSynch(false);
			vrOpenComposite.read();
			System.out.println("OpenComposite initialized.");
		} else {
			System.out.println("OpenComposite not found: " + jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(getError()).getString(0));
			vrOpenComposite = null;
		}
	}

	private static boolean getXforms = true;

	private static Map<String, Matrix4f[]> controllerComponentTransforms;
	private static Map<Long, String> controllerComponentNames;

	private static void getTransforms(){
		if (vrRenderModels == null) return;

		if(getXforms == true) {
			controllerComponentTransforms = new HashMap<String, Matrix4f[]>();
		}

		if(controllerComponentNames == null) {
			controllerComponentNames = new HashMap<Long, String>();
		}

		int count = vrRenderModels.GetRenderModelCount.apply();
		Pointer pointer = new Memory(JOpenVRLibrary.k_unMaxPropertyStringSize);

		List<String> componentNames = new ArrayList<String>(); //TODO get the controller-specific list

		componentNames.add("tip");
		componentNames.add("base");
		componentNames.add("handgrip");
		componentNames.add("status");
		boolean failed = false;
		
		for (String comp : componentNames) {
			controllerComponentTransforms.put(comp, new Matrix4f[2]); 			
			Pointer p = ptrFomrString(comp);

			for (int i = 0; i < 2; i++) {

				//	debugOut(controllerDeviceIndex[i]);

				if (controllerDeviceIndex[i] == JOpenVRLibrary.k_unTrackedDeviceIndexInvalid) {
					failed = true;
					continue;
				}
				vrsystem.GetStringTrackedDeviceProperty.apply(controllerDeviceIndex[i], JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_RenderModelName_String, pointer, JOpenVRLibrary.k_unMaxPropertyStringSize - 1, hmdErrorStore);

				//doing this next bit for each controller because pointer
				long button = vrRenderModels.GetComponentButtonMask.apply(pointer, p);   		
				if(button > 0){ //see now... wtf openvr, '0' is the system button, it cant also be the error value! (hint: it's a mask, not an index)
					controllerComponentNames.put(button, comp); //u get 1 button per component, nothing more
				}
				//
				long sourceHandle = i == RIGHT_CONTROLLER ? rightControllerHandle : leftControllerHandle;
				if (sourceHandle == JOpenVRLibrary.k_ulInvalidInputValueHandle) {
					failed = true;
					continue;
				}
				//
				RenderModel_ControllerMode_State_t.ByReference modeState = new RenderModel_ControllerMode_State_t.ByReference();
				RenderModel_ComponentState_t.ByReference componentState = new RenderModel_ComponentState_t.ByReference();
				byte ret = vrRenderModels.GetComponentStateForDevicePath.apply(pointer, p, sourceHandle, modeState, componentState);
				if(ret == 0) {
					//System.out.println("Failed getting transform: " + comp + " controller " + i);
					failed = true; // Oculus does not seem to raise ANY trackedDevice events. So just keep trying...
					continue;
				}
				Matrix4f xform = new Matrix4f();
				OpenVRUtil.convertSteamVRMatrix3ToMatrix4f(componentState.mTrackingToComponentLocal, xform);
				controllerComponentTransforms.get(comp)[i] = xform;
			//	System.out.println("Transform: " + comp + " controller: " + i +" button: " + button + "\r" + Utils.convertOVRMatrix(xform).toString());

				if (!failed && i == 0) {
					try {

						Matrix4f tip = getControllerComponentTransform(0,"tip");
						Matrix4f hand = getControllerComponentTransform(0,"base");

						Vector3 tipvec = tip.transform(forward);
						Vector3 handvec = hand.transform(forward);

						double dot = Math.abs(tipvec.normalized().dot(handvec.normalized()));
						
						double anglerad = Math.acos(dot);
						double angledeg = Math.toDegrees(anglerad);

						double angletestrad = Math.acos(tipvec.normalized().dot(forward.normalized()));
						double angletestdeg = Math.toDegrees(angletestrad);

					//	System.out.println("gun angle " + angledeg + " default angle " + angletestdeg);
						
						gunStyle = angledeg > 10;

					} catch (Exception e) {
						failed = true;
					}
				}
			}
		}
		
		getXforms = failed;
	}

	public static Matrix4f getControllerComponentTransform(int controllerIndex, String componenetName){
		if(controllerComponentTransforms == null || !controllerComponentTransforms.containsKey(componenetName)  || controllerComponentTransforms.get(componenetName)[controllerIndex] == null)
			return OpenVRUtil.Matrix4fSetIdentity(new Matrix4f());
		return controllerComponentTransforms.get(componenetName)[controllerIndex];
	}

	public static Matrix4f getControllerComponentTransformFromButton(int controllerIndex, long button){
		if (controllerComponentNames == null || !controllerComponentNames.containsKey(button))
			return getControllerComponentTransform(controllerIndex, "status");

		return getControllerComponentTransform(controllerIndex, controllerComponentNames.get(button));
	}

	public static boolean hasOpenComposite() {
		return vrOpenComposite != null;
	}

	public static void initOpenVRCompositor() throws Exception
	{
		if(vrsystem != null ) {
			vrCompositor = new VR_IVRCompositor_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRCompositor_Version, hmdErrorStoreBuf));
			if(vrCompositor != null && !isError()){                
				System.out.println("OpenVR Compositor initialized OK.");
				vrCompositor.setAutoSynch(false);
				vrCompositor.read();
				vrCompositor.SetTrackingSpace.apply(JOpenVRLibrary.ETrackingUniverseOrigin.ETrackingUniverseOrigin_TrackingUniverseStanding);

				int buffsize=20;
				Pointer s=new Memory(buffsize);

				System.out.println("TrackingSpace: "+vrCompositor.GetTrackingSpace.apply());

				vrsystem.GetStringTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd,JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_ManufacturerName_String,s,buffsize,hmdErrorStore);
				String id=s.getString(0);
				System.out.println("Device manufacturer is: "+id);

				detectedHardware = HardwareType.fromManufacturer(id);
				mc.vrSettings.loadOptions();
				VRHotkeys.loadExternalCameraConfig();

			} else {
				throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(getError()).getString(0));			 
			}
		}
		if( vrCompositor == null ) {
			System.out.println("Skipping VR Compositor...");
			if( vrsystem != null ) {
				vsyncToPhotons = vrsystem.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_SecondsFromVsyncToPhotons_Float, hmdErrorStore);
			} else {
				vsyncToPhotons = 0f;
			}
		}

		// left eye
		texBounds.uMax = 1f;
		texBounds.uMin = 0f;
		texBounds.vMax = 1f;
		texBounds.vMin = 0f;
		texBounds.setAutoSynch(false);
		texBounds.setAutoRead(false);
		texBounds.setAutoWrite(false);
		texBounds.write();


		// texture type
		texType0.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
		texType0.eType = JOpenVRLibrary.ETextureType.ETextureType_TextureType_OpenGL;
		texType0.handle = Pointer.createConstant(-1);
	//	VRTextureDepthInfo_t info = new VRTextureDepthInfo_t();
	//	info.vRange = new HmdVector2_t(new float[]{0,1});
	//	texType0.depth = info;
		texType0.setAutoSynch(false);
		texType0.setAutoRead(false);
		texType0.setAutoWrite(false);
		texType0.write();


		// texture type
		texType1.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
		texType1.eType = JOpenVRLibrary.ETextureType.ETextureType_TextureType_OpenGL;
		texType1.handle = Pointer.createConstant(-1);
	//	VRTextureDepthInfo_t info2 = new VRTextureDepthInfo_t();
		//info2.vRange = new HmdVector2_t(new float[]{0,1});
	//	texType1.depth = info2;
		texType1.setAutoSynch(false);
		texType1.setAutoRead(false);
		texType1.setAutoWrite(false);
		texType1.write();

		System.out.println("OpenVR Compositor initialized OK.");

	}

	public boolean initOpenVRControlPanel()
	{
		return true;
		//		vrControlPanel = new VR_IVRSettings_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRControlPanel_Version, hmdErrorStore));
		//		if(vrControlPanel != null && hmdErrorStore.getValue() == 0){
		//			System.out.println("OpenVR Control Panel initialized OK.");
		//			return true;
		//		} else {
		//			initStatus = "OpenVR Control Panel error: " + JOpenVRLibrary.VR_GetStringForHmdError(hmdErrorStore.getValue()).getString(0);
		//			return false;
		//		}
	}

	private String lasttyped = "";

	public static boolean paused =false; 

	public static void poll(long frameIndex)
	{
		if (!initialized) return;
	
		paused = vrsystem.ShouldApplicationPause.apply() != 0;

		mc.getProfiler().startSection("events");
		pollVREvents();

		if(!mc.vrSettings.seated){
			mc.getProfiler().endStartSection("controllers");

			// GUI controls

			mc.getProfiler().startSection("gui");

			if(mc.currentScreen == null && mc.vrSettings.vrTouchHotbar && mc.vrSettings.vrHudLockMode != mc.vrSettings.HUD_LOCK_HEAD && hudPopup){
				processHotbar();
			}

			mc.getProfiler().endSection();
		}

		mc.getProfiler().endStartSection("processEvents");
		processVREvents();

		mc.getProfiler().endStartSection("updatePose");
		updatePose();

		mc.getProfiler().endStartSection("processInputs");
		processInputs();

		mc.getProfiler().endSection();
	}

	private static int quickTorchPreviousSlot;

	private static void processHotbar() {

		if(mc.player == null) return;
		if(mc.player.inventory == null) return;
		
		if(mc.climbTracker.isGrabbingLadder() && 
				mc.climbTracker.isClaws(mc.player.getHeldItemMainhand())) return;

		Vec3d main = getAimSource(0);
		Vec3d off = getAimSource(1);

		Vec3d barStartos = null,barEndos = null;

		int i = 1;
		if(mc.vrSettings.vrReverseHands) i = -1;

		if (mc.vrSettings.vrHudLockMode == VRSettings.HUD_LOCK_WRIST){
			barStartos =  getAimRotation(1).transform(new Vector3(i*0.02f,0.05f,0.26f)).toVec3d();
			barEndos =  getAimRotation(1).transform(new Vector3(i*0.02f,0.05f,0.01f)).toVec3d();
		} else if (mc.vrSettings.vrHudLockMode == VRSettings.HUD_LOCK_HAND){
			barStartos =  getAimRotation(1).transform(new Vector3(i*-.18f,0.08f,-0.01f)).toVec3d();
			barEndos =  getAimRotation(1).transform(new Vector3(i*0.19f,0.04f,-0.08f)).toVec3d();
		} else return; //how did u get here


		Vec3d barStart = off.add(barStartos.x, barStartos.y, barStartos.z);	
		Vec3d barEnd = off.add(barEndos.x, barEndos.y, barEndos.z);

		Vec3d u = barStart.subtract(barEnd);
		Vec3d pq = barStart.subtract(main);
		float dist = (float) (pq.crossProduct(u).length() / u.length());

		if(dist > 0.06) return;

		float fact = (float) (pq.dotProduct(u) / (u.x*u.x + u.y*u.y + u.z*u.z));

		if(fact < 0) return;

		Vec3d w2 = u.scale(fact).subtract(pq);

		Vec3d point = main.subtract(w2);
		float linelen = (float) barStart.subtract(barEnd).length();
		float ilen = (float) barStart.subtract(point).length();

		float pos = ilen / linelen * 9; 

		if(mc.vrSettings.vrReverseHands) pos = 9 - pos;

		int box = (int) Math.floor(pos);
		if(pos - Math.floor(pos) < 0.1) return;

		if(box > 8) return;
		if(box < 0) return;
		//all that maths for this.
		if(box != mc.player.inventory.currentItem){
			mc.player.inventory.currentItem = box;	
			triggerHapticPulse(0, 750);
		}
	}

	

	public static void destroy()
	{
		if (initialized)
		{
			try {
				JOpenVRLibrary.VR_ShutdownInternal();
				initialized = false;
				if(Main.katvr)
					jkatvr.Halt();
				if(Main.infinadeck)
					jinfinadeck.Destroy();
			} catch (Throwable e) { // wtf valve
				e.printStackTrace();
			}

		}
	}

	//	public HmdParameters getHMDInfo()
	//	{
	//		HmdParameters hmd = new HmdParameters();
	//		if ( isInitialized() )
	//		{
	//			IntBuffer rtx = IntBuffer.allocate(1);
	//			IntBuffer rty = IntBuffer.allocate(1);
	//			vrsystem.GetRecommendedRenderTargetSize.apply(rtx, rty);
	//
	//			hmd.Type = HmdType.ovrHmd_Other;
	//			hmd.ProductName = "OpenVR";
	//			hmd.Manufacturer = "Unknown";
	//			hmd.AvailableHmdCaps = 0;
	//			hmd.DefaultHmdCaps = 0;
	//			hmd.AvailableTrackingCaps = HmdParameters.ovrTrackingCap_Orientation | HmdParameters.ovrTrackingCap_Position;
	//			hmd.DefaultTrackingCaps = HmdParameters.ovrTrackingCap_Orientation | HmdParameters.ovrTrackingCap_Position;
	//			hmd.Resolution = new Sizei( rtx.get(0) * 2, rty.get(0) );
	//
	//			float topFOV = vrsystem.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewTopDegrees_Float, hmdErrorStore);
	//			float bottomFOV = vrsystem.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewBottomDegrees_Float, hmdErrorStore);
	//			float leftFOV = vrsystem.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewLeftDegrees_Float, hmdErrorStore);
	//			float rightFOV = vrsystem.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_FieldOfViewRightDegrees_Float, hmdErrorStore);
	//
	//			hmd.DefaultEyeFov[0] = new FovPort((float)Math.tan(topFOV),(float)Math.tan(bottomFOV),(float)Math.tan(leftFOV),(float)Math.tan(rightFOV));
	//			hmd.DefaultEyeFov[1] = new FovPort((float)Math.tan(topFOV),(float)Math.tan(bottomFOV),(float)Math.tan(leftFOV),(float)Math.tan(rightFOV));
	//			hmd.MaxEyeFov[0] = new FovPort((float)Math.tan(topFOV),(float)Math.tan(bottomFOV),(float)Math.tan(leftFOV),(float)Math.tan(rightFOV));
	//			hmd.MaxEyeFov[1] = new FovPort((float)Math.tan(topFOV),(float)Math.tan(bottomFOV),(float)Math.tan(leftFOV),(float)Math.tan(rightFOV));
	//			hmd.DisplayRefreshRate = 90.0f;
	//		}
	//
	//		return hmd;
	//	}



	private static void processScrollInput(KeyBinding keyBinding, Runnable upCallback, Runnable downCallback) {
		VRInputAction action = getInputAction(keyBinding);
		if (action.isEnabled() && action.getLastOrigin() != JOpenVRLibrary.k_ulInvalidInputValueHandle && action.getAxis2D(true).getY() != 0) {
			float value = action.getAxis2D(false).getY();
			if (value > 0)
				upCallback.run();
			else if (value < 0)
				downCallback.run();
		}
	}

	private static void processSwipeInput(KeyBinding keyBinding, Runnable leftCallback, Runnable rightCallback, Runnable upCallback, Runnable downCallback) {
		VRInputAction action = getInputAction(keyBinding);
		if (action.isEnabled() && action.getLastOrigin() != JOpenVRLibrary.k_ulInvalidInputValueHandle) {
			ControllerType controller = findActiveBindingControllerType(keyBinding);
			if (controller != null) {
				if (!trackpadSwipeSamplers.containsKey(keyBinding.getKeyDescription()))
					trackpadSwipeSamplers.put(keyBinding.getKeyDescription(), new TrackpadSwipeSampler());
				TrackpadSwipeSampler sampler = trackpadSwipeSamplers.get(keyBinding.getKeyDescription());
				sampler.update(controller, action.getAxis2D(false));

				if (sampler.isSwipedUp() && upCallback != null) {
					triggerHapticPulse(controller, 0.001f, 400, 0.5f);
					upCallback.run();
				}
				if (sampler.isSwipedDown() && downCallback != null) {
					triggerHapticPulse(controller, 0.001f, 400, 0.5f);
					downCallback.run();
				}
				if (sampler.isSwipedLeft() && leftCallback != null) {
					triggerHapticPulse(controller, 0.001f, 400, 0.5f);
					leftCallback.run();
				}
				if (sampler.isSwipedRight() && rightCallback != null) {
					triggerHapticPulse(controller, 0.001f, 400, 0.5f);
					rightCallback.run();
				}
			}
		}
	}

	private static void processInputAction(VRInputAction action) {
		if (!action.isActive() || !action.isEnabledRaw()) {
			action.unpressBinding();
		} else {
			if (action.isButtonChanged()) {
				if (action.isButtonPressed() && action.isEnabled()) {
					// We do this so shit like closing a GUI by clicking a button won't
					// also click in the world immediately after.
					if (!ignorePressesNextFrame)
						action.pressBinding();
				} else {
					action.unpressBinding();
				}
			}
		}
	}

	public static void processInputs() {
		if (mc.vrSettings.seated || Main.viewonly || !inputInitialized) return;

		for (VRInputAction action : inputActions.values()) {
			if (action.isHanded()) {
				for (ControllerType hand : ControllerType.values()) {
					action.setCurrentHand(hand);
					processInputAction(action);
				}
			} else {
				processInputAction(action);
			}
		}

		processScrollInput(GuiHandler.keyScrollAxis, () -> InputSimulator.scrollMouse(0, 1), () -> InputSimulator.scrollMouse(0, -1));
		processScrollInput(keyHotbarScroll, () -> changeHotbar(-1), () -> changeHotbar(1));
		processSwipeInput(keyHotbarSwipeX, () -> changeHotbar(1), () -> changeHotbar(-1), null, null);
		processSwipeInput(keyHotbarSwipeY, null, null, () -> changeHotbar(-1), () -> changeHotbar(1));

		// Reset this flag
		ignorePressesNextFrame = false;
	}

	public static void processBindings() {
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
					mc.printChatMessage("Movement mode switched to: " + (mc.vrSettings.seatedFreeMove ? " Free-move" : "Teleport"));
				} else 
				{
					if (mc.vrPlayer.isTeleportSupported()) {
						mc.vrSettings.forceStandingFreeMove = !mc.vrSettings.forceStandingFreeMove;
						mc.printChatMessage("Movement mode switched to: " + (mc.vrSettings.forceStandingFreeMove ? "Free-move" : "Teleport"));
					} else {
						if (mc.vrPlayer.isTeleportOverridden()) {
							mc.vrPlayer.setTeleportOverride(false);
							mc.printChatMessage("Restricted movement enabled (no teleporting)");
						} else {
							mc.vrPlayer.setTeleportOverride(true);
							mc.printChatMessage("Restricted movement disabled (teleporting allowed)");
						}
					}
				}
			}
		} else {
			moveModeSwitchCount = 0;
		}

		Vec3d main = getAimVector(0);
		Vec3d off = getAimVector(1);

		float myaw = (float) Math.toDegrees(Math.atan2(-main.x, main.z));
		float oyaw= (float) Math.toDegrees(Math.atan2(-off.x, off.z));;

		if(!gui){
			if(keyWalkabout.isKeyDown()){
				float yaw = myaw;

				//oh this is ugly. TODO: cache which hand when binding button.
				TrackedController controller = findActiveBindingController(keyWalkabout);
				if (controller != null && controller.getType() == ControllerType.LEFT) {
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
				TrackedController controller = findActiveBindingController(keyRotateFree);
				if (controller != null && controller.getType() == ControllerType.LEFT) {
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
			MCOpenVR.triggerBindingHapticPulse(keyHotbarNext, 250);
		}

		if(keyHotbarPrev.isPressed()){
			changeHotbar(1);
			MCOpenVR.triggerBindingHapticPulse(keyHotbarPrev, 250);
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
			float ax = getAxis2D(getInputAction(keyRotateAxis)).getX();
			if (ax == 0) ax = getAxis2D(getInputAction(keyFreeMoveRotate)).getX();
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
			float ax = MovementInputFromOptions.getMovementAxisValue(keyRotateLeft);
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
			float ax = MovementInputFromOptions.getMovementAxisValue(keyRotateRight);
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
			TrackedController controller = findActiveBindingController(keyRadialMenu);
			if (controller != null)
				RadialHandler.setOverlayShowing(!RadialHandler.isShowing(), controller.getType());
		}

		if (keySwapMirrorView.isPressed()) {
			if (mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_THIRD_PERSON)
				mc.vrSettings.displayMirrorMode = VRSettings.MIRROR_FIRST_PERSON;
			else if (mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_FIRST_PERSON)
				mc.vrSettings.displayMirrorMode = VRSettings.MIRROR_THIRD_PERSON;
			mc.stereoProvider.reinitFrameBuffers("Mirror Setting Changed");
		}

		if (keyToggleKeyboard.isPressed()) {
			KeyboardHandler.setOverlayShowing(!KeyboardHandler.Showing);
		}

		if (keyMoveThirdPersonCam.isPressed() && !Main.kiosk && !mc.vrSettings.seated && (mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_MIXED_REALITY || mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_THIRD_PERSON)) {
			TrackedController controller = MCOpenVR.findActiveBindingController(keyMoveThirdPersonCam);
			if (controller != null)
				VRHotkeys.startMovingThirdPersonCam(controller.getType().ordinal());
		}
		if (!keyMoveThirdPersonCam.isKeyDown() && VRHotkeys.isMovingThirdPersonCam()) {
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
						final World world = mc.getIntegratedServer().getWorld(mc.player.dimension);
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
					}
					mc.ingameGUI.getChatGUI().printChatMessage(new StringTextComponent("World export complete... area size: " + size));
					mc.ingameGUI.getChatGUI().printChatMessage(new StringTextComponent("Saved to " + file.getAbsolutePath()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if (keyTogglePlayerList.isPressed()) {
			mc.ingameGUI.showPlayerList = !mc.ingameGUI.showPlayerList;
		}

		GuiHandler.processBindingsGui();
		RadialHandler.processBindings();
		KeyboardHandler.processBindings();
		mc.interactTracker.processBindings();
	}

	private static void changeHotbar(int dir){
		if(mc.player == null || (mc.climbTracker.isGrabbingLadder() && 
				mc.climbTracker.isClaws(mc.player.getHeldItemMainhand()))) //never let go, jack.
		{}
		else{
			//if (Reflector.forgeExists() && mc.currentScreen == null && Display.isActive())
			//	KeyboardSimulator.robot.mouseWheel(-dir * 120);
			//else
				mc.player.inventory.changeCurrentItem(dir);
		}
	}

	private static String findEvent(int eventcode) {
		Field[] fields = EVREventType.class.getFields();

		for (Field field : fields) {
			if (field.getType() == Integer.TYPE) {
				String n = field.getName();
				int val;
				try {
					val = field.getInt(null);
					if(val == eventcode) return n;
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return "";
	}

	// Valve why do we have to poll events before we can get updated controller state?
	private static void pollVREvents()
	{
		if (vrsystem == null) return;
		for (VREvent_t event = new VREvent_t(); vrsystem.PollNextEvent.apply(event, event.size()) > 0; event = new VREvent_t()) {
			vrEvents.add(event);
		}
	}

	//jrbuda:: oh hello there you are.
	private static void processVREvents() {
		while (!vrEvents.isEmpty()) {
			VREvent_t event = vrEvents.poll();
			//System.out.println("SteamVR Event: " + findEvent(event.eventType));

			switch (event.eventType) {
				/*case EVREventType.EVREventType_VREvent_KeyboardClosed:
					//'huzzah'
					keyboardShowing = false;
					if (mc.currentScreen instanceof GuiChat && !mc.vrSettings.seated) {
						GuiTextField field = (GuiTextField)MCReflection.getField(MCReflection.GuiChat_inputField, mc.currentScreen);
						if (field != null) {
							String s = field.getText().trim();
							if (!s.isEmpty()) {
								mc.currentScreen.sendChatMessage(s);
							}
						}
						//mc.displayGuiScreen((Screen)null);
					}
					break;
				case EVREventType.EVREventType_VREvent_KeyboardCharInput:
					byte[] inbytes = event.data.getPointer().getByteArray(0, 8);
					int len = 0;
					for (byte b : inbytes) {
						if(b>0)len++;
					}
					String str = new String(inbytes,0,len, StandardCharsets.UTF_8);
					if (mc.currentScreen != null && !mc.vrSettings.alwaysSimulateKeyboard) { // experimental, needs testing
						try {
							for (char ch : str.toCharArray()) {
								int[] codes = KeyboardSimulator.getLWJGLCodes(ch);
								int code = codes.length > 0 ? codes[codes.length - 1] : 0;
								if (InputInjector.isSupported()) InputInjector.typeKey(code, ch);
								else mc.currentScreen.keyTypedPublic(ch, code);
								break;
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						KeyboardSimulator.type(str); //holy shit it works.
					}
					break;*/
				case EVREventType.EVREventType_VREvent_Quit:
					mc.shutdown();
					break;
				case EVREventType.EVREventType_VREvent_TrackedDeviceActivated:
				case EVREventType.EVREventType_VREvent_TrackedDeviceDeactivated:
				case EVREventType.EVREventType_VREvent_TrackedDeviceRoleChanged:
				case EVREventType.EVREventType_VREvent_TrackedDeviceUpdated:
				case EVREventType.EVREventType_VREvent_ModelSkinSettingsHaveChanged:
					getXforms = true;
					break;
				default:
					break;
			}
		}
	}

	public static boolean isBoundInActiveActionSets(KeyBinding binding) {
		List<Long> origins = getInputAction(binding).getOrigins();
		return !origins.isEmpty();
	}

	public static ControllerType findActiveBindingControllerType(KeyBinding binding) {
		if (!inputInitialized) return null;
		long origin = getInputAction(binding).getLastOrigin();
		if (origin != JOpenVRLibrary.k_ulInvalidInputValueHandle) {
			return getOriginControllerType(origin);
		}
		return null;
	}

	public static TrackedController findActiveBindingController(KeyBinding binding) {
		ControllerType type = findActiveBindingControllerType(binding);
		if (type != null) return type.getController();
		return null;
	}

	public static void triggerBindingHapticPulse(KeyBinding binding, float durationSeconds, float frequency, float amplitude) {
		TrackedController controller = findActiveBindingController(binding);
		if (controller != null) controller.triggerHapticPulse(durationSeconds, frequency, amplitude);
	}

	@Deprecated
	public static void triggerBindingHapticPulse(KeyBinding binding, int duration) {
		TrackedController controller = findActiveBindingController(binding);
		if (controller != null) controller.triggerHapticPulse(duration);
		}

	public static VRInputAction getInputActionByName(String name) {
		return inputActions.get(name);
		}

	public static VRInputAction getInputAction(String keyBindingDesc) {
		return inputActionsByKeyBinding.get(keyBindingDesc);
	}

	public static VRInputAction getInputAction(KeyBinding keyBinding) {
		return getInputAction(keyBinding.getKeyDescription());
	}

	public static Collection<VRInputAction> getInputActions() {
		return Collections.unmodifiableCollection(inputActions.values());
	}

	public static Collection<VRInputAction> getInputActionsInSet(VRInputActionSet set) {
		return Collections.unmodifiableCollection(inputActions.values().stream().filter(action -> action.actionSet == set).collect(Collectors.toList()));
	}

	public static long getActionSetHandle(VRInputActionSet actionSet) {
		return actionSetHandles.get(actionSet);
	}

	public static long getInputSourceHandle(String path) {
		LongByReference longRef = new LongByReference();
		int error = vrInput.GetInputSourceHandle.apply(ptrFomrString(path), longRef);
		if (error != 0)
			throw new RuntimeException("Error getting input source handle for '" + path + "': " + getInputError(error));
		return longRef.getValue();
	}

	public static long getControllerHandle(ControllerType hand) {
		if (mc.vrSettings.vrReverseHands) {
			if (hand == ControllerType.RIGHT)
				return leftControllerHandle;
			else
				return rightControllerHandle;
		} else {
			if (hand == ControllerType.RIGHT)
				return rightControllerHandle;
			else
				return leftControllerHandle;
		}
	}

	public static long getHapticHandle(ControllerType hand) {
		if (hand == ControllerType.RIGHT)
			return rightHapticHandle;
		else
			return leftHapticHandle;
	}
	
	public static String getInputError(int code){
		switch (code){
		case EVRInputError.EVRInputError_VRInputError_BufferTooSmall:
			return "BufferTooSmall";
		case EVRInputError.EVRInputError_VRInputError_InvalidBoneCount:
			return "InvalidBoneCount";
		case EVRInputError.EVRInputError_VRInputError_InvalidBoneIndex:
			return "InvalidBoneIndex";
		case EVRInputError.EVRInputError_VRInputError_InvalidCompressedData:
			return "InvalidCompressedData";
		case EVRInputError.EVRInputError_VRInputError_InvalidDevice:
			return "InvalidDevice";
		case EVRInputError.EVRInputError_VRInputError_InvalidHandle:
			return "InvalidHandle";
		case EVRInputError.EVRInputError_VRInputError_InvalidParam:
			return "InvalidParam";
		case EVRInputError.EVRInputError_VRInputError_InvalidSkeleton:
			return "InvalidSkeleton";
		case EVRInputError.EVRInputError_VRInputError_IPCError:
			return "IPCError";
		case EVRInputError.EVRInputError_VRInputError_MaxCapacityReached:
			return "MaxCapacityReached";
		case EVRInputError.EVRInputError_VRInputError_MismatchedActionManifest:
			return "MismatchedActionManifest";
		case EVRInputError.EVRInputError_VRInputError_MissingSkeletonData:
			return "MissingSkeletonData";
		case EVRInputError.EVRInputError_VRInputError_NameNotFound:
			return "NameNotFound";
		case EVRInputError.EVRInputError_VRInputError_NoActiveActionSet:
			return "NoActiveActionSet";
		case EVRInputError.EVRInputError_VRInputError_NoData:
			return "NoData";
		case EVRInputError.EVRInputError_VRInputError_None:
			return "wat";
		case EVRInputError.EVRInputError_VRInputError_NoSteam:
			return "NoSteam";
		case EVRInputError.EVRInputError_VRInputError_WrongType:
			return "WrongType";
		}
		return "Unknown";
	}
	
	private static void updatePose()
	{
		if ( vrsystem == null || vrCompositor == null )
			return;

		int ret = vrCompositor.WaitGetPoses.apply(hmdTrackedDevicePoseReference, JOpenVRLibrary.k_unMaxTrackedDeviceCount, null, 0);

		if (ret>0)
			System.out.println("Compositor Error: GetPoseError " + OpenVRStereoRenderer.getCompostiorError(ret)); 

		if(ret == 101){ //this is so dumb but it works.
			triggerHapticPulse(0, 500);
			triggerHapticPulse(1, 500);
		}

		if (getXforms == true) { //set null by events.
			getTransforms(); //do we want the dynamic info? I don't think so...
			//findControllerDevices();
		}

		HmdMatrix34_t matL = vrsystem.GetEyeToHeadTransform.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left);
		OpenVRUtil.convertSteamVRMatrix3ToMatrix4f(matL, hmdPoseLeftEye);

		HmdMatrix34_t matR = vrsystem.GetEyeToHeadTransform.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right);
		OpenVRUtil.convertSteamVRMatrix3ToMatrix4f(matR, hmdPoseRightEye);

		for (int nDevice = 0; nDevice < JOpenVRLibrary.k_unMaxTrackedDeviceCount; ++nDevice )
		{
			hmdTrackedDevicePoses[nDevice].read();
			if ( hmdTrackedDevicePoses[nDevice].bPoseIsValid != 0 )
			{
				OpenVRUtil.convertSteamVRMatrix3ToMatrix4f(hmdTrackedDevicePoses[nDevice].mDeviceToAbsoluteTracking, poseMatrices[nDevice]);
				deviceVelocity[nDevice] = new Vec3d(hmdTrackedDevicePoses[nDevice].vVelocity.v[0],hmdTrackedDevicePoses[nDevice].vVelocity.v[1],hmdTrackedDevicePoses[nDevice].vVelocity.v[2]);
			}		
		}

		if (hmdTrackedDevicePoses[JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd].bPoseIsValid != 0 )
		{
			OpenVRUtil.Matrix4fCopy(poseMatrices[JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd], hmdPose);
			headIsTracking = true;
		}
		else
		{
			headIsTracking = false;
			OpenVRUtil.Matrix4fSetIdentity(hmdPose);
			hmdPose.M[1][3] = 1.62f;
		}

		// Gotta do this here so we can get the poses
		if(inputInitialized) {
			
			mc.getProfiler().startSection("updateActionState");

				VRActiveActionSet_t[] activeActionSets = getActiveActionSets();
				if (activeActionSets.length > 0) {
					int error = vrInput.UpdateActionState.apply(activeActionSetsReference, activeActionSets[0].size(), activeActionSets.length);
					if (error != 0)
						throw new RuntimeException("Error updating action state: code " + getInputError(error));
				}
				inputActions.values().forEach(VRInputAction::readNewData);

			mc.getProfiler().endSection();

			if (mc.vrSettings.vrReverseHands) {
				updateControllerPose(RIGHT_CONTROLLER, leftPoseHandle);
				updateControllerPose(LEFT_CONTROLLER, rightPoseHandle);
			} else {
				updateControllerPose(RIGHT_CONTROLLER, rightPoseHandle);
				updateControllerPose(LEFT_CONTROLLER, leftPoseHandle);
			}
			updateControllerPose(THIRD_CONTROLLER, externalCameraPoseHandle);
		}
		/*for (int c=0;c<3;c++)
		{
			if (controllerDeviceIndex[c] != -1)
			{
				controllerTracking[c] = true;
				if (c < 2) controllers[c].tracking = true;
				OpenVRUtil.Matrix4fCopy(poseMatrices[controllerDeviceIndex[c]], controllerPose[c]);
			}
			else
			{
				controllerTracking[c] = false;
				if (c < 2) controllers[c].tracking = false;
				//OpenVRUtil.Matrix4fSetIdentity(controllerPose[c]);
			}
		}*/

		updateAim();
		//VRHotkeys.snapMRCam(mc, 0);

	}

	private static void readPoseData(long actionHandle) {
		int error = vrInput.GetPoseActionDataForNextFrame.apply(actionHandle, JOpenVRLibrary.ETrackingUniverseOrigin.ETrackingUniverseOrigin_TrackingUniverseStanding, poseData, poseData.size(), JOpenVRLibrary.k_ulInvalidInputValueHandle);
		if (error != 0)
			throw new RuntimeException("Error reading pose data: " + getInputError(error));
		poseData.read();
	}

	private static void readOriginInfo(long inputValueHandle) {
		int error = vrInput.GetOriginTrackedDeviceInfo.apply(inputValueHandle, originInfo, originInfo.size());
		if (error != 0)
			throw new RuntimeException("Error reading origin info: " + getInputError(error));
		originInfo.read();
	}

	public static InputOriginInfo_t getOriginInfo(long inputValueHandle) {
		InputOriginInfo_t originInfoT = new InputOriginInfo_t();
		readOriginInfo(inputValueHandle);
		originInfoT.devicePath = originInfo.devicePath;
		originInfoT.trackedDeviceIndex = originInfo.trackedDeviceIndex;
		originInfoT.rchRenderModelComponentName = originInfo.rchRenderModelComponentName;
		return originInfoT;
	}

	public static String getOriginName(long handle) {
		Pointer p = new Memory(JOpenVRLibrary.k_unMaxPropertyStringSize + 1);
		int error = vrInput.GetOriginLocalizedName.apply(handle, p, JOpenVRLibrary.k_unMaxPropertyStringSize, JOpenVRLibrary.EVRInputStringBits.EVRInputStringBits_VRInputString_All);
		if (error != 0)
			throw new RuntimeException("Error getting origin name: " + MCOpenVR.getInputError(error));
		return p.getString(0);
	}

	public static ControllerType getOriginControllerType(long inputValueHandle) {
		if (inputValueHandle == JOpenVRLibrary.k_ulInvalidInputValueHandle)
			return null;
		readOriginInfo(inputValueHandle);
		if (originInfo.trackedDeviceIndex != JOpenVRLibrary.k_unTrackedDeviceIndexInvalid) {
			if (originInfo.trackedDeviceIndex == controllerDeviceIndex[RIGHT_CONTROLLER])
				return ControllerType.RIGHT;
			else if (originInfo.trackedDeviceIndex == controllerDeviceIndex[LEFT_CONTROLLER])
				return ControllerType.LEFT;
		}
		return null;
	}

	private static void updateControllerPose(int controller, long actionHandle) {
		readPoseData(actionHandle);
		if (poseData.activeOrigin != JOpenVRLibrary.k_ulInvalidInputValueHandle) {
			readOriginInfo(poseData.activeOrigin);
			int deviceIndex = originInfo.trackedDeviceIndex;
			if (deviceIndex != controllerDeviceIndex[controller])
				getXforms = true;
			controllerDeviceIndex[controller] = deviceIndex;
			if (deviceIndex != JOpenVRLibrary.k_unTrackedDeviceIndexInvalid) {
				TrackedDevicePose_t pose = poseData.pose;
				if (pose.bPoseIsValid != 0) {
					OpenVRUtil.convertSteamVRMatrix3ToMatrix4f(pose.mDeviceToAbsoluteTracking, poseMatrices[deviceIndex]);
					deviceVelocity[deviceIndex] = new Vec3d(pose.vVelocity.v[0], pose.vVelocity.v[1], pose.vVelocity.v[2]);
					OpenVRUtil.Matrix4fCopy(poseMatrices[deviceIndex], controllerPose[controller]);

					controllerTracking[controller] = true;
					return; // controller is tracking, don't execute the code below
				}
			}
		} else {
			controllerDeviceIndex[controller] = JOpenVRLibrary.k_unTrackedDeviceIndexInvalid;
		}

		//OpenVRUtil.Matrix4fSetIdentity(controllerPose[controller]);
		controllerTracking[controller] = false;
	}

	public static boolean isControllerTracking(int controller) {
		return controllerTracking[controller];
	}

	public static boolean isControllerTracking(ControllerType controller) {
		return isControllerTracking(controller.ordinal());
	}

	// Code duplication to reduce garbage
	public static float getAxis1D(VRInputAction action) {
		if (axisUseTracker.getOrDefault(action.keyBinding.getKeyDescription(), false) || action.isEnabled()) {
			float axis = action.getAxis1D(false);
			boolean used = axis != 0;
			axisUseTracker.put(action.keyBinding.getKeyDescription(), used);
			return axis;
		}
		return 0;
	}

	public static Vector2 getAxis2D(VRInputAction action) {
		if (axisUseTracker.getOrDefault(action.keyBinding.getKeyDescription(), false) || action.isEnabled()) {
			Vector2 axis = action.getAxis2D(false);
			boolean used = axis.getX() != 0 || axis.getY() != 0;
			axisUseTracker.put(action.keyBinding.getKeyDescription(), used);
			return axis;
		}
		return new Vector2();
	}

	public static Vector3 getAxis3D(VRInputAction action) {
		if (axisUseTracker.getOrDefault(action.keyBinding.getKeyDescription(), false) || action.isEnabled()) {
			Vector3 axis = action.getAxis3D(false);
			boolean used = axis.getX() != 0 || axis.getY() != 0 || axis.getZ() != 0;
			axisUseTracker.put(action.keyBinding.getKeyDescription(), used);
			return axis;
		}
		return new Vector3();
	}
	// Weeeeee

	/**
	 * @return The coordinate of the 'center' eye position relative to the head yaw plane
	 */

	public static Vec3d getCenterEyePosition() {
		Vector3 pos = OpenVRUtil.convertMatrix4ftoTranslationVector(hmdPose);
		if (mc.vrSettings.seated || mc.vrSettings.allowStandingOriginOffset)
			pos=pos.add(offset);
		return pos.toVec3d();
	}

	/**
	 * @return The coordinate of the left or right eye position relative to the head yaw plane
	 */

	public static Vec3d getEyePosition(RenderPass eye)
	{
		Matrix4f hmdToEye = hmdPoseRightEye;
		if ( eye == RenderPass.LEFT)
		{
			hmdToEye = hmdPoseLeftEye;
		} else if ( eye == RenderPass.RIGHT)
		{
			hmdToEye = hmdPoseRightEye;
		} else {
			hmdToEye = null;
		}

		if(hmdToEye == null){
			Matrix4f pose = hmdPose;
			Vector3 pos = OpenVRUtil.convertMatrix4ftoTranslationVector(pose);
			if (mc.vrSettings.seated || mc.vrSettings.allowStandingOriginOffset)
				pos=pos.add(offset);
			return pos.toVec3d();
		} else {
			Matrix4f pose = Matrix4f.multiply( hmdPose, hmdToEye );
			Vector3 pos = OpenVRUtil.convertMatrix4ftoTranslationVector(pose);
			if (mc.vrSettings.seated || mc.vrSettings.allowStandingOriginOffset)
				pos=pos.add(offset);
			return pos.toVec3d();
		}
	}

	public static Matrix4f getEyeRotation(RenderPass eye)
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

	/**
	 *
	 * @return Play area size or null if not valid
	 */
	public static float[] getPlayAreaSize() {
		if (vrChaperone == null || vrChaperone.GetPlayAreaSize == null) return null;
		FloatByReference bufz = new FloatByReference();
		FloatByReference bufx = new FloatByReference();
		byte valid = vrChaperone.GetPlayAreaSize.apply(bufx, bufz);
		if (valid == 1) return new float[]{bufx.getValue()*mc.vrSettings.walkMultiplier, bufz.getValue()*mc.vrSettings.walkMultiplier};
		return null;
	}

	/**
	 * Gets the orientation quaternion
	 *
	 * @return quaternion w, x, y & z components
	 */

	static Angle getOrientationEuler()
	{
		Quaternion orient = OpenVRUtil.convertMatrix4ftoRotationQuat(hmdPose);
		return orient.toEuler();
	}

	final String k_pch_SteamVR_Section = "steamvr";
	final String k_pch_SteamVR_RenderTargetMultiplier_Float = "renderTargetMultiplier";



	//-------------------------------------------------------
	// IBodyAimController

	float getBodyPitchDegrees() {
		return 0; //Always return 0 for body pitch
	}

	public static Vec3d getAimVector( int controller ) {
		Vector3 v = controllerRotation[controller].transform(forward);
		return v.toVec3d();

	}

	public static Vec3d getHmdVector() {
		Vector3 v = hmdRotation.transform(forward);
		return v.toVec3d();
	}

	public static Vec3d getHandVector( int controller ) {
		Vector3 forward = new Vector3(0,0,-1);
		Matrix4f aimRotation = handRotation[controller];
		Vector3 controllerDirection = aimRotation.transform(forward);
		return controllerDirection.toVec3d();
	}

	public static Matrix4f getAimRotation( int controller ) {
		return controllerRotation[controller];
	}

	public static Matrix4f getHandRotation( int controller ) {
		return handRotation[controller];
	}


	public boolean initBodyAim() throws Exception
	{
		return init();
	}


	public static Vec3d getAimSource( int controller ) {
		Vec3d out = new Vec3d(aimSource[controller].x, aimSource[controller].y, aimSource[controller].z);
		if(!mc.vrSettings.seated && mc.vrSettings.allowStandingOriginOffset)
			out = out.add(offset.getX(), offset.getY(), offset.getZ());
		return out;
	}

	public static void triggerHapticPulse(ControllerType controller, float durationSeconds, float frequency, float amplitude, float delaySeconds) {
		if (mc.vrSettings.seated || !inputInitialized) return;
		if (mc.vrSettings.vrReverseHands) {
			if (controller == ControllerType.RIGHT)
				controller = ControllerType.LEFT;
			else
				controller = ControllerType.RIGHT;
		}

		hapticScheduler.queueHapticPulse(controller, durationSeconds, frequency, amplitude, delaySeconds);
	}

	public static void triggerHapticPulse(ControllerType controller, float durationSeconds, float frequency, float amplitude) {
		triggerHapticPulse(controller, durationSeconds, frequency, amplitude, 0);
	}
	
	@Deprecated
	public static void triggerHapticPulse(ControllerType controller, int strength) {
		if (strength < 1) return;
		// Through careful analysis of the haptics in the legacy API (read: I put the controller to
		// my ear, listened to the vibration, and reproduced the frequency in Audacity), I have determined
		// that the old haptics used 160Hz. So, these parameters will match the "feel" of the old haptics.
		triggerHapticPulse(controller, strength / 1000000f, 160, 1);
	}

	@Deprecated
	public static void triggerHapticPulse(int controller, int strength) {
		if (controller < 0 || controller >= ControllerType.values().length) return;
		triggerHapticPulse(ControllerType.values()[controller], strength);
	}

	public static float seatedRot;

	public static Vector3 forward = new Vector3(0,0,-1);
	static double aimPitch = 0; //needed for seated mode.


	private static void updateAim() {
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


			Vec3d eye = getCenterEyePosition();
			hmdHistory.add(eye);
			Vector3 v3 = MCOpenVR.hmdRotation.transform(new Vector3(0,-.1f, .1f));
			hmdPivotHistory.add(new Vec3d(v3.getX()+eye.x, v3.getY()+eye.y, v3.getZ()+eye.z));

		}

		Matrix4f[] controllerPoseTip = new Matrix4f[2];
		controllerPoseTip[0] = new Matrix4f();
		controllerPoseTip[1] = new Matrix4f();

		{//right controller
			handRotation[0].M[0][0] = controllerPose[0].M[0][0];
			handRotation[0].M[0][1] = controllerPose[0].M[0][1];
			handRotation[0].M[0][2] = controllerPose[0].M[0][2];
			handRotation[0].M[0][3] = 0.0F;
			handRotation[0].M[1][0] = controllerPose[0].M[1][0];
			handRotation[0].M[1][1] = controllerPose[0].M[1][1];
			handRotation[0].M[1][2] = controllerPose[0].M[1][2];
			handRotation[0].M[1][3] = 0.0F;
			handRotation[0].M[2][0] = controllerPose[0].M[2][0];
			handRotation[0].M[2][1] = controllerPose[0].M[2][1];
			handRotation[0].M[2][2] = controllerPose[0].M[2][2];
			handRotation[0].M[2][3] = 0.0F;
			handRotation[0].M[3][0] = 0.0F;
			handRotation[0].M[3][1] = 0.0F;
			handRotation[0].M[3][2] = 0.0F;
			handRotation[0].M[3][3] = 1.0F;	

			if(mc.vrSettings.seated){
				controllerPose[0] = hmdPose.inverted().inverted();
				controllerPose[1] = hmdPose.inverted().inverted();
				controllerPoseTip[0] = controllerPose[0];
				controllerPoseTip[1] = controllerPose[1];
			} else	
				controllerPoseTip[0] = Matrix4f.multiply(controllerPose[0], getControllerComponentTransform(0,"tip"));

			// grab controller position in tracker space, scaled to minecraft units
			Vector3 controllerPos = OpenVRUtil.convertMatrix4ftoTranslationVector(controllerPoseTip[0]);
			aimSource[0] = controllerPos.toVec3d();

			controllerHistory[0].add(aimSource[0]);

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

			Vec3d hdir = getHmdVector();

			if(mc.vrSettings.seated && mc.currentScreen == null){
				org.vivecraft.utils.lwjgl.Matrix4f temp = new org.vivecraft.utils.lwjgl.Matrix4f();

				float hRange = 110;
				float vRange = 180;
				double h = mc.mouseHelper.getMouseX() / (double) mc.mainWindow.getWidth() * hRange - (hRange / 2);
			
				h = MathHelper.clamp(h, -hRange/2, hRange/2);

				int hei  = mc.mainWindow.getHeight();
				if(hei % 2 != 0)
					hei-=1; //fix drifting vertical mouse.

				double v = -mc.mouseHelper.getMouseY() / (double) hei * vRange + (vRange / 2);		

				double nPitch=-v;
				if(mc.isGameFocused()){
					float rotStart = mc.vrSettings.keyholeX;
					float rotSpeed = 2000 * mc.vrSettings.xSensitivity;
					int leftedge=(int)((-rotStart + (hRange / 2)) *(double) mc.mainWindow.getWidth() / hRange )+1;
					int rightedge=(int)((rotStart + (hRange / 2)) *(double) mc.mainWindow.getWidth() / hRange )-1;
					float rotMul = ((float)Math.abs(h) - rotStart) / ((hRange / 2) - rotStart); // Scaled 0...1 from rotStart to FOV edge
					if(rotMul > 0.15) rotMul = 0.15f;

					double xpos = mc.mouseHelper.getMouseX();
					
					if(h < -rotStart){
						seatedRot += rotSpeed * rotMul * mc.getFrameDelta();
  						seatedRot %= 360; // Prevent stupidly large values
						hmdForwardYaw = (float)Math.toDegrees(Math.atan2(hdir.x, hdir.z));   
						xpos = leftedge;
						h=-rotStart;
					}
					if(h > rotStart){
						seatedRot -= rotSpeed * rotMul * mc.getFrameDelta();
						seatedRot %= 360; // Prevent stupidly large values
						hmdForwardYaw = (float)Math.toDegrees(Math.atan2(hdir.x, hdir.z));    	
						xpos = rightedge;
						h=rotStart;
					}

					double ySpeed=0.5 * mc.vrSettings.ySensitivity;
					nPitch=aimPitch+(v)*ySpeed;
					nPitch=MathHelper.clamp(nPitch,-89.9,89.9);
					
					InputSimulator.setMousePos(xpos, hei/2);
					GLFW.glfwSetCursorPos(mc.mainWindow.getHandle(), xpos, hei/2);

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

			Vec3d dir = getAimVector(0);
			aimPitch = (float)Math.toDegrees(Math.asin(dir.y/dir.length()));
		}

		{//left controller
			handRotation[1].M[0][0] = controllerPose[1].M[0][0];
			handRotation[1].M[0][1] = controllerPose[1].M[0][1];
			handRotation[1].M[0][2] = controllerPose[1].M[0][2];
			handRotation[1].M[0][3] = 0.0F;
			handRotation[1].M[1][0] = controllerPose[1].M[1][0];
			handRotation[1].M[1][1] = controllerPose[1].M[1][1];
			handRotation[1].M[1][2] = controllerPose[1].M[1][2];
			handRotation[1].M[1][3] = 0.0F;
			handRotation[1].M[2][0] = controllerPose[1].M[2][0];
			handRotation[1].M[2][1] = controllerPose[1].M[2][1];
			handRotation[1].M[2][2] = controllerPose[1].M[2][2];
			handRotation[1].M[2][3] = 0.0F;
			handRotation[1].M[3][0] = 0.0F;
			handRotation[1].M[3][1] = 0.0F;
			handRotation[1].M[3][2] = 0.0F;
			handRotation[1].M[3][3] = 1.0F;	

			// update off hand aim
			if(!mc.vrSettings.seated) 
				controllerPoseTip[1] = Matrix4f.multiply(controllerPose[1], getControllerComponentTransform(1,"tip"));

			Vector3 leftControllerPos = OpenVRUtil.convertMatrix4ftoTranslationVector(controllerPoseTip[1]);
			aimSource[1] = leftControllerPos.toVec3d();
			controllerHistory[1].add(aimSource[1]);

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

		if(controllerDeviceIndex[THIRD_CONTROLLER]!=-1 && (mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_MIXED_REALITY || mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_THIRD_PERSON )|| debugThirdController) {
			mrMovingCamActive = true;
			Vector3 thirdControllerPos = OpenVRUtil.convertMatrix4ftoTranslationVector(controllerPose[2]);
			aimSource[2] = thirdControllerPos.toVec3d();
		} else {
			mrMovingCamActive = false;
			aimSource[2] = new Vec3d(
					mc.vrSettings.vrFixedCamposX,
					mc.vrSettings.vrFixedCamposY,
					mc.vrSettings.vrFixedCamposZ);
		}


	}

	public static double getCurrentTimeSecs()
	{
		return System.nanoTime() / 1000000000d;
	}

	public static HardwareType getHardwareType() {
		return mc.vrSettings.forceHardwareDetection > 0 ? HardwareType.values()[mc.vrSettings.forceHardwareDetection - 1] : detectedHardware;
	}

	private static boolean gunStyle = false; 
	
	public static boolean isGunStyle() {
		return gunStyle;
	}

	public static void resetPosition() {
		Vec3d pos= getCenterEyePosition().scale(-1).add(offset.getX(),offset.getY(),offset.getZ());
		offset=new Vector3((float) pos.x,(float)pos.y+1.62f,(float)pos.z);
	}

	public static void clearOffset() {
		offset=new Vector3(0,0,0);
	}

	public static boolean isSafeBinding(KeyBinding kb) {
		// Stupid hard-coded junk
		return getKeyBindings().contains(kb) || kb == mc.gameSettings.keyBindChat || kb == mc.gameSettings.keyBindInventory;
	}

	public static boolean isHMDTracking() {
		return headIsTracking;
	}
}
