package org.vivecraft.provider.ovr_lwjgl;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox.KeySelectionManager;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.ovr.OVR;
import org.lwjgl.ovr.OVREyeRenderDesc;
import org.lwjgl.ovr.OVRGraphicsLuid;
import org.lwjgl.ovr.OVRHmdDesc;
import org.lwjgl.ovr.OVRInputState;
import org.lwjgl.ovr.OVRLayerEyeFov;
import org.lwjgl.ovr.OVRPoseStatef;
import org.lwjgl.ovr.OVRPosef;
import org.lwjgl.ovr.OVRQuatf;
import org.lwjgl.ovr.OVRTrackingState;
import org.lwjgl.ovr.OVRUtil;
import org.lwjgl.ovr.OVRVector3f;
import org.vivecraft.gameplay.screenhandlers.GuiHandler;
import org.vivecraft.gameplay.trackers.SneakTracker;
import org.vivecraft.provider.ActionParams;
import org.vivecraft.provider.ControllerType;
import org.vivecraft.provider.MCVR;
import org.vivecraft.provider.openvr_jna.MCOpenVR;
import org.vivecraft.provider.openvr_jna.OpenVRHapticScheduler;
import org.vivecraft.provider.openvr_jna.VRInputAction;
import org.vivecraft.utils.math.Matrix4f;

import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.Main;
import net.minecraft.client.settings.KeyBinding;

public class MC_OVR extends MCVR {
	PointerBuffer session;
	OVRGraphicsLuid luid;
	OVREyeRenderDesc eyeRenderDesc0;
	OVREyeRenderDesc eyeRenderDesc1;	
	org.lwjgl.ovr.OVRPosef.Buffer  hmdToEyeViewPose;
	OVRHmdDesc hmdDesc;
	OVRLayerEyeFov layer;
	PointerBuffer textureSwapChainL;
	PointerBuffer textureSwapChainR;
	OVRTrackingState trackingState;
	protected static MC_OVR ome;
	OVRVector3f guardian;
	OVRInputState inputs;
	
	public MC_OVR(Minecraft mc) {
		super(mc);
		ome = this;
		hapticScheduler = new OVR_HapticScheduler();
		trackingState = OVRTrackingState.malloc();
		eyeRenderDesc0 = OVREyeRenderDesc.malloc();
		eyeRenderDesc1 = OVREyeRenderDesc.malloc();	
		hmdToEyeViewPose = OVRPosef.create(2);
		hmdDesc = OVRHmdDesc.malloc();
		inputs = OVRInputState.malloc();
		guardian = OVRVector3f.malloc();
	}
	public static MC_OVR get() {
		return ome;
	}
	@Override
	public String getName() {
		return "Oculus_LWJGL";
	}

	@Override
	public String getID() {
		return "oculus_lwjgl";
	}

	@Override
	public void processInputs() {
		if (mc.vrSettings.seated || Main.viewonly) return;	
		OVR.ovr_GetInputState(session.get(0), OVR.ovrControllerType_Touch, inputs);	
		processInputAction(getInputAction(mc.gameSettings.keyBindAttack), inputs.IndexTrigger(0) > 0.5f);
		processInputAction(getInputAction(mc.gameSettings.keyBindUseItem), (inputs.Buttons() & OVR.ovrButton_A) == OVR.ovrButton_A);
		processInputAction(getInputAction(keyRadialMenu), (inputs.Buttons() & OVR.ovrButton_B) == OVR.ovrButton_B);
		processInputAction(getInputAction(keyVRInteract), inputs.IndexTrigger(0) > 0.5f || inputs.IndexTrigger(0) > 0.5f || inputs.IndexTrigger(1) > 0.5f ||inputs.HandTrigger(1) > 0.5f);
		processInputAction(getInputAction(mc.gameSettings.keyBindInventory), (inputs.Buttons() & OVR.ovrButton_X) == OVR.ovrButton_X);
		processInputAction(getInputAction(keyMenuButton), (inputs.Buttons() & OVR.ovrButton_Y) == OVR.ovrButton_Y);
		processInputAction(getInputAction(mc.gameSettings.keyBindSneak), (inputs.Buttons() & OVR.ovrButton_RThumb) == OVR.ovrButton_RThumb);
		processInputAction(getInputAction(mc.gameSettings.keyBindJump), (inputs.Buttons() & OVR.ovrButton_LThumb) == OVR.ovrButton_LThumb);	
		processInputAction(getInputAction(keyHotbarNext), inputs.HandTrigger(0) > 0.5f);
		processInputAction(getInputAction(keyHotbarPrev), inputs.HandTrigger(1) > 0.5f);
		processInputAction(getInputAction(keyTeleport),  inputs.IndexTrigger(1) > 0.5f);
		processInputAction(getInputAction(keyFreeMoveStrafe),  inputs.Thumbstick(1).y() > 0.5f);
		
		getInputAction(keyRotateAxis).analogData[0].x = inputs.Thumbstick(0).x();	
		getInputAction(keyFreeMoveStrafe).analogData[0].x = inputs.Thumbstick(1).x();
		getInputAction(keyFreeMoveStrafe).analogData[0].y = inputs.Thumbstick(1).y();

		ignorePressesNextFrame = false;
	}
	
	private void processInputAction(VRInputAction action, boolean buttonstate) {
		if (!action.isActive() || !action.isEnabledRaw()) {
			action.unpressBinding();
		} else {
			if (buttonstate && action.isEnabled()) {
				// We do this so shit like closing a GUI by clicking a button won't
				// also click in the world immediately after.
				if (!ignorePressesNextFrame)
					action.pressBinding();
			} else {
				action.unpressBinding();
			}
		}
	}
	
	@Override
	public void destroy() {
		OVR.ovr_DestroyTextureSwapChain(session.get(0), textureSwapChainL.get(0));
		OVR.ovr_DestroyTextureSwapChain(session.get(0), textureSwapChainR.get(0));
		OVR.ovr_Destroy(session.get(0));
		OVR.ovr_Shutdown();
	}

	@Override
	public void poll(long frameIndex) {
		if (!initialized) return;
		OVR.ovr_WaitToBeginFrame(session.get(0), 0);
		OVR.ovr_BeginFrame(session.get(0), 0);
		OVR.ovr_GetTrackingState(session.get(0), OVR.novr_GetPredictedDisplayTime(session.get(0), 0), true, trackingState);
		OVRPoseStatef poseState = trackingState.HeadPose();
		OVRPosef pose = poseState.ThePose();
		headIsTracking = (trackingState.StatusFlags() & OVR.ovrStatus_PositionTracked) == OVR.ovrStatus_PositionTracked;
		
        OVRUtil.ovr_CalcEyePoses(pose, hmdToEyeViewPose, layer.RenderPose());      
        hmdPoseLeftEye = OVRUtils.ovrPoseToMatrix(layer.RenderPose(0));
        hmdPoseRightEye = OVRUtils.ovrPoseToMatrix(layer.RenderPose(1));
        
        if(headIsTracking)
        	hmdPose = OVRUtils.ovrPoseToMatrix(pose);      
        else {
        	hmdPose.SetIdentity();
        	hmdPose.M[1][3] = 1.62f;
        }
     
        OVRPoseStatef leftState = trackingState.HandPoses(0);
        OVRPosef leftPose = leftState.ThePose();
        
        OVRPoseStatef rightState = trackingState.HandPoses(1);
        OVRPosef rightPose = rightState.ThePose();
        
        controllerPose[0] = OVRUtils.ovrPoseToMatrix(leftPose);
        controllerPose[1] = OVRUtils.ovrPoseToMatrix(rightPose);
        //TODO: velocities
        
        controllerTracking[0] = (trackingState.HandStatusFlags(0) & OVR.ovrStatus_PositionTracked) == OVR.ovrStatus_PositionTracked;
        controllerTracking[1] = (trackingState.HandStatusFlags(1) & OVR.ovrStatus_PositionTracked) == OVR.ovrStatus_PositionTracked;
        
        updateAim();
        
        processInputs();
	}
	
	@Override
	public float[] getPlayAreaSize() {
		OVR.ovr_GetBoundaryDimensions(session.get(0), OVR.ovrBoundary_PlayArea, guardian);
		return new float[] {guardian.x(), guardian.z()};
	}

	@Override
	public boolean init() {
		if ( initialized )
			return true;
		OVR.ovr_Initialize(null);
		session = BufferUtils.createPointerBuffer(1);
		luid = OVRGraphicsLuid.create();
		
		if(OVR.ovr_Create(session, luid)!=0) {
			initStatus = "Couldn't create OVR!";
			System.err.println(initStatus);
			return false;
		}
		
		OVR.ovr_GetHmdDesc(session.get(0), hmdDesc);
		
		System.out.println("hmd res: " + hmdDesc.Resolution().toString());
		
		OVR.ovr_GetRenderDesc(session.get(0), OVR.ovrEye_Left, hmdDesc.DefaultEyeFov(0), eyeRenderDesc0);
		OVR.ovr_GetRenderDesc(session.get(0), OVR.ovrEye_Right, hmdDesc.DefaultEyeFov(1), eyeRenderDesc1);
		
		hmdToEyeViewPose.put(0, eyeRenderDesc0.HmdToEyePose());
		hmdToEyeViewPose.put(1, eyeRenderDesc1.HmdToEyePose());	
	
		OVR.ovr_SetTrackingOriginType(session.get(0), OVR.ovrTrackingOrigin_FloorLevel);
		initialized = true;
		initSuccess = true;
		return true;
	}

	@Override
	public boolean postinit() {
		populateInputActions();
		return true;
	}
	
	@Override
	public Matrix4f getControllerComponentTransform(int c, String name) {
		//TODO
		return new Matrix4f();
	}

	@Override
	public boolean hasThirdController() {
		return false;
	}
	@Override
	public List<Long> getOrigins(VRInputAction vrInputAction) {
		return new ArrayList<Long>();
	}
	
	@Override
	protected void triggerBindingHapticPulse(KeyBinding key, int i) {
		// TODO Auto-generated method stub
		
	}
	@Override
	protected ControllerType findActiveBindingControllerType(KeyBinding key) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
