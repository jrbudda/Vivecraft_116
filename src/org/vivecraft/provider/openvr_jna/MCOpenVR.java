package org.vivecraft.provider.openvr_jna;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import org.lwjgl.glfw.GLFW;
import org.vivecraft.api.VRData;
import org.vivecraft.gameplay.screenhandlers.GuiHandler;
import org.vivecraft.gameplay.screenhandlers.KeyboardHandler;
import org.vivecraft.gameplay.screenhandlers.RadialHandler;
import org.vivecraft.menuworlds.MenuWorldExporter;
import org.vivecraft.provider.ActionParams;
import org.vivecraft.provider.ControllerType;
import org.vivecraft.provider.HardwareType;
import org.vivecraft.provider.InputSimulator;
import org.vivecraft.provider.MCVR;
import org.vivecraft.provider.openvr_jna.control.TrackpadSwipeSampler;
import org.vivecraft.provider.openvr_jna.control.VRInputActionSet;
import org.vivecraft.provider.openvr_jna.control.VivecraftMovementInput;
import org.vivecraft.settings.VRHotkeys;
import org.vivecraft.settings.VRSettings;
import org.vivecraft.utils.LangHelper;
import org.vivecraft.utils.Utils;
import org.vivecraft.utils.external.jinfinadeck;
import org.vivecraft.utils.external.jkatvr;
import org.vivecraft.utils.math.Matrix4f;
import org.vivecraft.utils.math.Quaternion;
import org.vivecraft.utils.math.Vector2;
import org.vivecraft.utils.math.Vector3;

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
import jopenvr.InputAnalogActionData_t;
import jopenvr.InputDigitalActionData_t;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.optifine.Lang;
import net.optifine.reflect.Reflector;

public class MCOpenVR extends MCVR
{
	public final static int LEFT_CONTROLLER = 1;
	public final static int RIGHT_CONTROLLER = 0;
	public final static int THIRD_CONTROLLER = 2;
	
	protected static MCOpenVR ome;
		
	private final String ACTION_EXTERNAL_CAMERA = "/actions/mixedreality/in/externalcamera";
	private final String ACTION_LEFT_HAND = "/actions/global/in/lefthand";
	private final String ACTION_LEFT_HAPTIC = "/actions/global/out/lefthaptic";
	private final String ACTION_RIGHT_HAND = "/actions/global/in/righthand";
	private final String ACTION_RIGHT_HAPTIC = "/actions/global/out/righthaptic";

	private Map<VRInputActionSet, Long> actionSetHandles = new EnumMap<>(VRInputActionSet.class);

	private VRActiveActionSet_t.ByReference activeActionSetsReference;

	private Map<Long, String> controllerComponentNames;
	
	private Map<String, Matrix4f[]> controllerComponentTransforms;
	private boolean dbg = true;
	
	private long externalCameraPoseHandle;
	private int[] controllerDeviceIndex = new int[3];
	private boolean getXforms = true;
	private final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private IntByReference hmdErrorStore = new IntByReference();
	private IntBuffer hmdErrorStoreBuf;

	private TrackedDevicePose_t.ByReference hmdTrackedDevicePoseReference;
	private TrackedDevicePose_t[] hmdTrackedDevicePoses;
	private boolean inputInitialized;

	private long leftControllerHandle;
	private long leftHapticHandle;
	private long leftPoseHandle;

	private InputOriginInfo_t.ByReference originInfo;
	private boolean paused =false;
	private InputPoseActionData_t.ByReference poseData;

	private long rightControllerHandle;
	private long rightHapticHandle;
	private long rightPoseHandle;

	private final VRTextureBounds_t texBounds = new VRTextureBounds_t();
	private Map<String, TrackpadSwipeSampler> trackpadSwipeSamplers = new HashMap<>();
	
	private boolean tried;
	private Queue<VREvent_t> vrEvents = new LinkedList<>();
	
	private VR_IVRApplications_FnTable vrApplications;
	private VR_IVRChaperone_FnTable vrChaperone;
	private VR_IVROCSystem_FnTable vrOpenComposite;
	private VR_IVROverlay_FnTable vrOverlay;
	private VR_IVRRenderModels_FnTable vrRenderModels;
	private VR_IVRSettings_FnTable vrSettings;
	
	VR_IVRCompositor_FnTable vrCompositor;
	VR_IVRInput_FnTable vrInput;
	VR_IVRSystem_FnTable vrsystem;
	
	final Texture_t texType0 = new Texture_t();
	final Texture_t texType1 = new Texture_t();
			
	public MCOpenVR(Minecraft mc)
	{
		super(mc);
		ome = this;
		hapticScheduler = new OpenVRHapticScheduler();

		for (int c=0;c<3;c++)
		{
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
		digital.setAutoRead(false);
		digital.setAutoWrite(false);
		digital.setAutoSynch(false);
		analog.setAutoRead(false);
		analog.setAutoWrite(false);
		analog.setAutoSynch(false);
	}
	
	public static MCOpenVR get() {
		return ome;
	}

	static String getInputErrorName(int code){
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

	public void destroy()
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

	public String getID() {
		return "openvr_jna";
	}

	public String getName() {
		return "OpenVR_JNA";
	}

	public float[] getPlayAreaSize() {
		if (vrChaperone == null || vrChaperone.GetPlayAreaSize == null) return null;
		FloatByReference bufz = new FloatByReference();
		FloatByReference bufx = new FloatByReference();
		byte valid = vrChaperone.GetPlayAreaSize.apply(bufx, bufz);
		if (valid == 1) return new float[]{bufx.getValue()*mc.vrSettings.walkMultiplier, bufz.getValue()*mc.vrSettings.walkMultiplier};
		return null;
	}

	public boolean init()
	{
		if ( initialized )
			return true;

		if ( tried )
			return initialized;

		tried = true;

		mc = Minecraft.getInstance();

		unpackPlatformNatives();

		if(jopenvr.JOpenVRLibrary.VR_IsHmdPresent() == 0){
			initStatus =  "vivecraft.messages.nosteamvr";
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

//		controllers[RIGHT_CONTROLLER] = new TrackedController_OpenVR(ControllerType.RIGHT);
//		controllers[LEFT_CONTROLLER] = new TrackedController_OpenVR(ControllerType.LEFT);

		deviceVelocity = new Vector3d[JOpenVRLibrary.k_unMaxTrackedDeviceCount];

		for(int i=0;i<poseMatrices.length;i++)
		{
			poseMatrices[i] = new Matrix4f();
			deviceVelocity[i] = new Vector3d(0,0,0);
		}

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

	public void poll(long frameIndex)
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

		mc.getProfiler().endStartSection("updatePose/Vsync");
		updatePose();

		mc.getProfiler().endStartSection("processInputs");
		processInputs();

		mc.getProfiler().endStartSection("hmdSampling");
		hmdSampling();

		mc.getProfiler().endSection();
	}


	public void processInputs() {
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

	@Deprecated
	protected void triggerBindingHapticPulse(KeyBinding binding, int duration) {
		ControllerType controller = findActiveBindingControllerType(binding);
		if (controller != null) triggerHapticPulse(controller, duration);
	}
	
	private boolean isError(){
		return hmdErrorStore.getValue() != 0 || hmdErrorStoreBuf.get(0) != 0;
	}
	

	private void debugOut(int deviceindex){
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

	protected ControllerType findActiveBindingControllerType(KeyBinding binding) {
		if (!inputInitialized) return null;
		long origin = getInputAction(binding).getLastOrigin();
		if (origin != JOpenVRLibrary.k_ulInvalidInputValueHandle) {
			return getOriginControllerType(origin);
		}
		return null;
	}

	private String findEvent(int eventcode) {
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


	private void generateActionManifest() {
		Map<String, Object> jsonMap = new HashMap<>();

		List<Map<String, Object>> actionSets = new ArrayList<>();
		for (VRInputActionSet actionSet : VRInputActionSet.values()) {
			if (actionSet == VRInputActionSet.MOD && !Reflector.ClientModLoader.exists())
				continue;
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

	private long getActionHandle(String name) {
		LongByReference longRef = new LongByReference();
		int error = vrInput.GetActionHandle.apply(ptrFomrString(name), longRef);
		if (error != 0)
			throw new RuntimeException("Error getting action handle for '" + name + "': " + getInputErrorName(error));
		return longRef.getValue();
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


	private VRActiveActionSet_t[] getActiveActionSets() {
		ArrayList<VRInputActionSet> list = new ArrayList<>();
		list.add(VRInputActionSet.GLOBAL);
		if (Reflector.ClientModLoader.exists())
			list.add(VRInputActionSet.MOD);
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

	public Matrix4f getControllerComponentTransform(int controllerIndex, String componenetName){
		if(controllerComponentTransforms == null || !controllerComponentTransforms.containsKey(componenetName)  || controllerComponentTransforms.get(componenetName)[controllerIndex] == null)
			return Utils.Matrix4fSetIdentity(new Matrix4f());
		return controllerComponentTransforms.get(componenetName)[controllerIndex];
	}

	private Matrix4f getControllerComponentTransformFromButton(int controllerIndex, long button){
		if (controllerComponentNames == null || !controllerComponentNames.containsKey(button))
			return new Matrix4f();

		return getControllerComponentTransform(controllerIndex, controllerComponentNames.get(button));
	}

	private int getError(){
		return hmdErrorStore.getValue() != 0 ? hmdErrorStore.getValue() : hmdErrorStoreBuf.get(0);
	}

	long getHapticHandle(ControllerType hand) {
		if (hand == ControllerType.RIGHT)
			return rightHapticHandle;
		else
			return leftHapticHandle;
	}

	private InputOriginInfo_t getOriginInfo(long inputValueHandle) {
		InputOriginInfo_t originInfoT = new InputOriginInfo_t();
		readOriginInfo(inputValueHandle);
		originInfoT.devicePath = originInfo.devicePath;
		originInfoT.trackedDeviceIndex = originInfo.trackedDeviceIndex;
		originInfoT.rchRenderModelComponentName = originInfo.rchRenderModelComponentName;
		return originInfoT;
	}

	private String getOriginName(long handle) {
		Pointer p = new Memory(JOpenVRLibrary.k_unMaxPropertyStringSize + 1);
		int error = vrInput.GetOriginLocalizedName.apply(handle, p, JOpenVRLibrary.k_unMaxPropertyStringSize, JOpenVRLibrary.EVRInputStringBits.EVRInputStringBits_VRInputString_All);
		if (error != 0)
			throw new RuntimeException("Error getting origin name: " + getInputErrorName(error));
		return p.getString(0);
	}


	float getSuperSampling(){
		if (vrSettings == null)
			return -1;
		return 
				vrSettings.GetFloat.apply(ptrFomrString("steamvr"),ptrFomrString("supersampleScale"), hmdErrorStore);
	}

	private void getTransforms(){
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
		//wmr doesnt define these...
		//componentNames.add("base"); 
		//componentNames.add("status");
		//
		componentNames.add("handgrip");
		boolean failed = false;

		for (String comp : componentNames) {
			controllerComponentTransforms.put(comp, new Matrix4f[2]); 			

			for (int i = 0; i < 2; i++) {

				if (controllerDeviceIndex[i] == JOpenVRLibrary.k_unTrackedDeviceIndexInvalid) {
					failed = true;
					continue;
				}
				vrsystem.GetStringTrackedDeviceProperty.apply(controllerDeviceIndex[i], JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_RenderModelName_String, pointer, JOpenVRLibrary.k_unMaxPropertyStringSize - 1, hmdErrorStore);
				String renderModel = pointer.getString(0);
				Pointer p = ptrFomrString(comp);

				Pointer test = new Memory(JOpenVRLibrary.k_unMaxPropertyStringSize);
				vrsystem.GetStringTrackedDeviceProperty.apply(controllerDeviceIndex[i], JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_InputProfilePath_String, test, JOpenVRLibrary.k_unMaxPropertyStringSize - 1, hmdErrorStore);
				String path = test.getString(0);
				boolean isWMR = path.contains("holographic");
				boolean isRiftS = path.contains("rifts");

				if(isWMR && comp.equals("handgrip")) {// i have no idea, Microsoft, none.
					//	System.out.println("Apply WMR override " + i);
					p = ptrFomrString("body");
				}	

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
					//	System.out.println("Failed getting transform: " + comp + " controller " + i);
					failed = true; // Oculus does not seem to raise ANY trackedDevice events. So just keep trying...
					continue;
				}
				Matrix4f xform = new Matrix4f();
				OpenVRUtil.convertSteamVRMatrix3ToMatrix4f(componentState.mTrackingToComponentLocal, xform);
				controllerComponentTransforms.get(comp)[i] = xform;

				if(i == 1 && isRiftS && comp.equals("handgrip")) {// i have no idea, Valve, none.
					controllerComponentTransforms.get(comp)[1] = controllerComponentTransforms.get(comp)[0];
				}

				//	System.out.println("Transform: " + comp + " controller: " + i + "model " + renderModel + " button: " + button + "\r" + Utils.convertOVRMatrix(xform).toString());

				if (!failed && i == 0) {
					try {

						Matrix4f tip = getControllerComponentTransform(0,"tip");
						Matrix4f hand = getControllerComponentTransform(0,"handgrip");

						Vector3 tipvec = tip.transform(forward);
						Vector3 handvec = hand.transform(forward);

						double dot = Math.abs(tipvec.normalized().dot(handvec.normalized()));

						double anglerad = Math.acos(dot);
						double angledeg = Math.toDegrees(anglerad);

						double angletestrad = Math.acos(tipvec.normalized().dot(forward.normalized()));
						double angletestdeg = Math.toDegrees(angletestrad);

						//		System.out.println("gun angle: " + anglerad + " : " + angledeg + " deg");

						gunStyle = angledeg > 10;
						gunAngle = angledeg;
					} catch (Exception e) {
						failed = true;
					}
				}
			}
		}

		getXforms = failed;
	}

	private boolean hasOpenComposite() {
		return vrOpenComposite != null;
	}

	private void initializeJOpenVR() {
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

			hmdTrackedDevicePoseReference = new TrackedDevicePose_t.ByReference();
			hmdTrackedDevicePoses = (TrackedDevicePose_t[])hmdTrackedDevicePoseReference.toArray(JOpenVRLibrary.k_unMaxTrackedDeviceCount);
			poseMatrices = new Matrix4f[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
			for(int i=0;i<poseMatrices.length;i++) poseMatrices[i] = new Matrix4f();

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
	
	public boolean postinit() {
		//y is this called later, i forget.
		initInputAndApplication();
		return inputInitialized;
	}
	
	private void initInputAndApplication() {
		populateInputActions();
		if (vrInput == null) return;
		generateActionManifest();
		loadActionManifest();
		loadActionHandles();
		installApplicationManifest(false);
		inputInitialized = true;
	}
	private void initOpenComposite() {
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
	private void initOpenVRApplications() {
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

	private void initOpenVRChaperone() {
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

	private void initOpenVRCompositor() throws Exception
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

	private boolean initOpenVRControlPanel()
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

	private void initOpenVRInput() {
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

	private void initOpenVRRenderModels()
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

	private void initOpenVRSettings()
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

	private void installApplicationManifest(boolean force) {
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

	private void loadActionHandles() {
		LongByReference longRef = new LongByReference();

		for (VRInputAction action : inputActions.values()) {
			int error = vrInput.GetActionHandle.apply(ptrFomrString(action.name), longRef);
			if (error != 0)
				throw new RuntimeException("Error getting action handle for '" + action.name + "': " + getInputErrorName(error));
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
				throw new RuntimeException("Error getting action set handle for '" + actionSet.name + "': " + getInputErrorName(error));
			actionSetHandles.put(actionSet, longRef.getValue());
		}

		leftControllerHandle = getInputSourceHandle("/user/hand/left");
		rightControllerHandle = getInputSourceHandle("/user/hand/right");
	}

	private void loadActionManifest() {
		int error = vrInput.SetActionManifestPath.apply(ptrFomrString(new File("openvr/input/action_manifest.json").getAbsolutePath()));
		if (error != 0) {
			throw new RuntimeException("Failed to load action manifest: " + getInputErrorName(error));
		}
	}

	// Valve why do we have to poll events before we can get updated controller state?
	private void pollVREvents()
	{
		if (vrsystem == null) return;
		for (VREvent_t event = new VREvent_t(); vrsystem.PollNextEvent.apply(event, event.size()) > 0; event = new VREvent_t()) {
			vrEvents.add(event);
		}
	}


	private void processInputAction(VRInputAction action) {
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

	private void processScrollInput(KeyBinding keyBinding, Runnable upCallback, Runnable downCallback) {
		VRInputAction action = getInputAction(keyBinding);
		if (action.isEnabled() && action.getLastOrigin() != JOpenVRLibrary.k_ulInvalidInputValueHandle && action.getAxis2D(true).getY() != 0) {
			float value = action.getAxis2D(false).getY();
			if (value > 0)
				upCallback.run();
			else if (value < 0)
				downCallback.run();
		}
	}

	private void processSwipeInput(KeyBinding keyBinding, Runnable leftCallback, Runnable rightCallback, Runnable upCallback, Runnable downCallback) {
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


	private void processVREvents() {
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

	private Pointer ptrFomrString(String in){
		Pointer p = new Memory(in.getBytes(StandardCharsets.UTF_8).length + 1);
		p.setString(0, in, StandardCharsets.UTF_8.name());
		return p;
	}

	private void readOriginInfo(long inputValueHandle) {
		int error = vrInput.GetOriginTrackedDeviceInfo.apply(inputValueHandle, originInfo, originInfo.size());
		if (error != 0)
			throw new RuntimeException("Error reading origin info: " + getInputErrorName(error));
		originInfo.read();
	}

	private void readPoseData(long actionHandle) {
		int error = vrInput.GetPoseActionDataForNextFrame.apply(actionHandle, JOpenVRLibrary.ETrackingUniverseOrigin.ETrackingUniverseOrigin_TrackingUniverseStanding, poseData, poseData.size(), JOpenVRLibrary.k_ulInvalidInputValueHandle);
		if (error != 0)
			throw new RuntimeException("Error reading pose data: " + getInputErrorName(error));
		poseData.read();
	}

	private void unpackPlatformNatives() {
		String osname = System.getProperty("os.name").toLowerCase();
		String osarch = System.getProperty("os.arch").toLowerCase();

		String osFolder = "win";

		if (osname.contains("linux")) {
			osFolder = "linux";
		} else if (osname.contains("mac")) {
			osFolder = "osx";
		}

		if (!osname.contains("mac")) {
			if (osarch.contains("64")) {
				osFolder += "64";
			} else {
				osFolder += "32";
			}
		}

		try {
			Utils.unpackNatives(osFolder);
		} catch (Exception e) {
			System.out.println("Native path not found");
			return;
		}

		String openVRPath = new File("openvr/" + osFolder).getAbsolutePath();
		System.out.println("Adding OpenVR search path: " + openVRPath);
		NativeLibrary.addSearchPath("openvr_api", openVRPath);
	}



	private void updateControllerPose(int controller, long actionHandle) {

		if(TPose) {
			if(controller == 0) {
				Utils.Matrix4fCopy(TPose_Right, controllerPose[controller]);
			}
			else if(controller == 1) {
				Utils.Matrix4fCopy(TPose_Left, controllerPose[controller]);		
			}
			controllerTracking[controller] = true;
			return;
		}

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
					deviceVelocity[deviceIndex] = new Vector3d(pose.vVelocity.v[0], pose.vVelocity.v[1], pose.vVelocity.v[2]);
					Utils.Matrix4fCopy(poseMatrices[deviceIndex], controllerPose[controller]);

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

	private void updatePose()
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
			//dbg = true;
			getTransforms(); //do we want the dynamic info? I don't think so...
			//findControllerDevices();
		} else {
			if (dbg) {
				dbg = false;
				debugOut(0);
				debugOut(controllerDeviceIndex[0]);
				debugOut(controllerDeviceIndex[1]);
			}
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
				deviceVelocity[nDevice] = new Vector3d(hmdTrackedDevicePoses[nDevice].vVelocity.v[0],hmdTrackedDevicePoses[nDevice].vVelocity.v[1],hmdTrackedDevicePoses[nDevice].vVelocity.v[2]);
			}		
		}

		if (hmdTrackedDevicePoses[JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd].bPoseIsValid != 0 )
		{
			Utils.Matrix4fCopy(poseMatrices[JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd], hmdPose);
			headIsTracking = true;
		}
		else
		{
			headIsTracking = false;
			Utils.Matrix4fSetIdentity(hmdPose);
			hmdPose.M[1][3] = 1.62f;
		}
		TPose = false;
		if(TPose) {
			TPose_Right.M[0][3] = 0f;
			TPose_Right.M[1][3] = 0f;
			TPose_Right.M[2][3] = 0f;
			Utils.Matrix4fCopy(TPose_Right.rotationY(-120), TPose_Right);
			TPose_Right.M[0][3] = 0.5f;
			TPose_Right.M[1][3] = 1.0f;
			TPose_Right.M[2][3] = -.5f;

			TPose_Left.M[0][3] = 0f;
			TPose_Left.M[1][3] = 0f;
			TPose_Left.M[2][3] = 0f;
			Utils.Matrix4fCopy(TPose_Left.rotationY(120), TPose_Left);
			TPose_Left.M[0][3] = -.5f;
			TPose_Left.M[1][3] = 1.0f;
			TPose_Left.M[2][3] = -.5f;

			Neutral_HMD.M[0][3] = 0f;
			Neutral_HMD.M[1][3] = 1.8f;

			Utils.Matrix4fCopy(Neutral_HMD, hmdPose);
			headIsTracking = true;
		}

		// Gotta do this here so we can get the poses
		if(inputInitialized) {

			mc.getProfiler().startSection("updateActionState");

			VRActiveActionSet_t[] activeActionSets = getActiveActionSets();
			if (activeActionSets.length > 0) {
				int error = vrInput.UpdateActionState.apply(activeActionSetsReference, activeActionSets[0].size(), activeActionSets.length);
				if (error != 0)
					throw new RuntimeException("Error updating action state: code " + getInputErrorName(error));
			}
			
			inputActions.values().forEach(this::readNewData);

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

		updateAim();

	}

	long getActionSetHandle(VRInputActionSet actionSet) {
		return actionSetHandles.get(actionSet);
	}


	long getControllerHandle(ControllerType hand) {
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

	long getInputSourceHandle(String path) {
		LongByReference longRef = new LongByReference();
		int error = vrInput.GetInputSourceHandle.apply(ptrFomrString(path), longRef);
		if (error != 0)
			throw new RuntimeException("Error getting input source handle for '" + path + "': " + getInputErrorName(error));
		return longRef.getValue();
	}

	ControllerType getOriginControllerType(long inputValueHandle) {
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
	
	public void readNewData(VRInputAction action) {
		switch (action.type) {
			case "boolean":
				if (action.isHanded())
					for (ControllerType cont : ControllerType.values()) {
						readDigitalData(action, cont);
					}
				else
					readDigitalData(action, null);
				break;
			case "vector1":
			case "vector2":
			case "vector3":
				if (action.isHanded())
					for (ControllerType cont : ControllerType.values()) {
						readAnalogData(action, cont);
					}
				else
					readAnalogData(action, null);
				break;
		}
	}
	
	InputDigitalActionData_t digital = new InputDigitalActionData_t.ByReference();
	InputAnalogActionData_t analog = new InputAnalogActionData_t.ByReference();

	private void readDigitalData(VRInputAction action, ControllerType hand) {
		int index = 0;
		if (hand != null)
			index = hand.ordinal();
		int error = vrInput.GetDigitalActionData.apply(action.handle, digital, digital.size(), hand != null ? getControllerHandle(hand) : JOpenVRLibrary.k_ulInvalidInputValueHandle);
		if (error != 0)
			throw new RuntimeException("Error reading digital data for '" + action.name + "': " + getInputErrorName(error));
		digital.read();

		action.digitalData[index].activeOrigin = digital.activeOrigin;
		action.digitalData[index].isActive = digital.bActive != 0;
		action.digitalData[index].state = digital.bState != 0;
		action.digitalData[index].isChanged = digital.bChanged != 0;
	}

	private void readAnalogData(VRInputAction action, ControllerType hand) {
		int index = 0;
		if (hand != null)
			index = hand.ordinal();

		int error = vrInput.GetAnalogActionData.apply(action.handle, analog, analog.size(), hand != null ? getControllerHandle(hand) : JOpenVRLibrary.k_ulInvalidInputValueHandle);
		if (error != 0)
			throw new RuntimeException("Error reading analog data for '" + action.name + "': " + getInputErrorName(error));
		analog.read();
		
		action.analogData[index].x = analog.x;
		action.analogData[index].y = analog.y;
		action.analogData[index].z = analog.z;
		
		action.analogData[index].deltaX = analog.deltaX;
		action.analogData[index].deltaY = analog.deltaY;
		action.analogData[index].deltaZ = analog.deltaZ;
		
		action.analogData[index].activeOrigin = analog.activeOrigin;
		action.analogData[index].isActive = analog.bActive != 0;
	}
	
	@Override
	public boolean hasThirdController() {
		return controllerDeviceIndex[2] != -1;
	}
	
	public List<Long> getOrigins(VRInputAction action) {
		Pointer p = new Memory(JOpenVRLibrary.k_unMaxActionOriginCount * 8);
		LongByReference longRef = new LongByReference();
		longRef.setPointer(p);
		int error = MCOpenVR.get().vrInput.GetActionOrigins.apply(getActionSetHandle(action.actionSet), action.handle, longRef, JOpenVRLibrary.k_unMaxActionOriginCount);
		if (error != 0)
			throw new RuntimeException("Error getting action origins for '" + action.name + "': " + MCOpenVR.getInputErrorName(error));

		List<Long> list = new ArrayList<>();
		for (long handle : p.getLongArray(0, JOpenVRLibrary.k_unMaxActionOriginCount)) {
			if (handle != JOpenVRLibrary.k_ulInvalidInputValueHandle)
				list.add(handle);
		}

		return list;
	}
}
