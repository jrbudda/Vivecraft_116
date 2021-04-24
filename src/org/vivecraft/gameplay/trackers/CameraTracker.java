package org.vivecraft.gameplay.trackers;

import org.vivecraft.api.VRData;
import org.vivecraft.render.RenderPass;
import org.vivecraft.utils.Utils;
import org.vivecraft.utils.math.Axis;
import org.vivecraft.utils.math.Matrix4f;
import org.vivecraft.utils.math.Quaternion;
import org.vivecraft.utils.math.Vector3;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.util.math.vector.Vector3d;

public class CameraTracker extends Tracker {
	public static final ModelResourceLocation cameraModel = new ModelResourceLocation("vivecraft:camera");
	public static final ModelResourceLocation cameraDisplayModel = new ModelResourceLocation("vivecraft:camera_display");

	private boolean visible = false;
	private Vector3d position = new Vector3d(0, 0, 0);
	private Quaternion rotation = new Quaternion();

	private int startController;
	private VRData.VRDevicePose startControllerPose;
	private Vector3d startPosition;
	private Quaternion startRotation;
	private boolean quickMode;

	public CameraTracker(Minecraft mc) {
		super(mc);
	}

	@Override
	public boolean isActive(ClientPlayerEntity player) {
		if (mc.playerController == null)
			return false;
		if (mc.vrSettings.seated)
			return false;
		if (!isVisible())
			return false;
		return true;
	}

	@Override
	public void doProcess(ClientPlayerEntity player) {
		if (startControllerPose != null) {
			VRData.VRDevicePose controllerPose = mc.vrPlayer.vrdata_world_render.getController(startController);
			Vector3d startPos = startControllerPose.getPosition();
			Vector3d deltaPos = controllerPose.getPosition().subtract(startPos);

			Matrix4f deltaMatrix = Matrix4f.multiply(controllerPose.getMatrix(), startControllerPose.getMatrix().inverted());
			Vector3 offset = new Vector3((float)startPosition.x - (float)startPos.x, (float)startPosition.y - (float)startPos.y, (float)startPosition.z - (float)startPos.z);
			Vector3 offsetRotated = deltaMatrix.transform(offset);

			position = new Vector3d(startPosition.x + (float)deltaPos.x + (offsetRotated.getX() - offset.getX()), startPosition.y + (float)deltaPos.y + (offsetRotated.getY() - offset.getY()), startPosition.z + (float)deltaPos.z + (offsetRotated.getZ() - offset.getZ()));
			rotation = startRotation.multiply(new Quaternion(Utils.convertOVRMatrix(deltaMatrix)));
		}

		if (quickMode && !isMoving() && !mc.grabScreenShot)
			visible = false;

		// chunk renderer gets angry if we're really far away, force hide when >3/4 render distance
		if (mc.vrPlayer.vrdata_world_render.getEye(RenderPass.CENTER).getPosition().distanceTo(position) > mc.gameSettings.renderDistanceChunks * 12)
			visible = false;
	}

	@Override
	public void reset(ClientPlayerEntity player) {
		visible = false;
		quickMode = false;
		stopMoving();
	}

	@Override
	public EntryPoint getEntryPoint() {
		return EntryPoint.SPECIAL_ITEMS; // smoother camera movement
	}

	public boolean isVisible() {
		return visible;
	}

	public void toggleVisibility() {
		visible = !visible;
	}

	public Vector3d getPosition() {
		return position;
	}

	public void setPosition(Vector3d position) {
		this.position = position;
	}

	public Quaternion getRotation() {
		return rotation;
	}

	public void setRotation(Quaternion rotation) {
		this.rotation = rotation;
	}

	public boolean isMoving() {
		return startControllerPose != null;
	}

	public int getMovingController() {
		return startController;
	}

	public boolean isQuickMode() {
		return quickMode;
	}

	public void startMoving(int controller, boolean quickMode) {
		startController = controller;
		startControllerPose = mc.vrPlayer.vrdata_world_pre.getController(controller);
		startPosition = position;
		startRotation = rotation.copy();
		this.quickMode = quickMode;
	}

	public void startMoving(int controller) {
		startMoving(controller, false);
	}

	public void stopMoving() {
		startControllerPose = null;
	}
}
