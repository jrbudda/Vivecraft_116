package org.vivecraft.render;

import java.util.Random;
import java.util.function.Function;

import org.vivecraft.gameplay.trackers.CameraTracker;
import org.vivecraft.settings.VRHotkeys;
import org.vivecraft.settings.VRSettings;
import org.vivecraft.utils.Utils;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.optifine.model.QuadBounds;
import org.lwjgl.opengl.GL11;

public class VRWidgetHelper {
	private static final Random random = new Random();
	public static boolean debug = false;

	public static void renderVRThirdPersonCamWidget() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.currentPass == RenderPass.LEFT || mc.currentPass == RenderPass.RIGHT) {
			if (mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_MIXED_REALITY || mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_THIRD_PERSON) {
				float scale = 0.35f;
				if (mc.interactTracker.isInCamera() && !VRHotkeys.isMovingThirdPersonCam())
					scale *= 1.03f;

				renderVRCameraWidget(-0.748f, -0.438f, -0.06f, scale, RenderPass.THIRD, GameRenderer.thirdPersonCameraModel, GameRenderer.thirdPersonCameraDisplayModel, () -> {
					mc.stereoProvider.framebufferMR.bindFramebufferTexture();
				}, face -> {
					if (face == Direction.NORTH)
						return DisplayFace.MIRROR;
					if (face == Direction.SOUTH)
						return DisplayFace.NORMAL;
					return DisplayFace.NONE;
				});
			}
		}
	}

	public static void renderVRHandheldCameraWidget() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.currentPass != RenderPass.CAMERA && mc.cameraTracker.isVisible()) {
			float scale = 0.25f;
			if (mc.interactTracker.isInHandheldCamera() && !mc.cameraTracker.isMoving())
				scale *= 1.03f;

			renderVRCameraWidget(-0.5f, -0.25f, -0.22f, scale, RenderPass.CAMERA, CameraTracker.cameraModel, CameraTracker.cameraDisplayModel, () -> {
				if (mc.getFirstPersonRenderer().getNearOpaqueBlock(mc.vrPlayer.vrdata_world_render.getEye(RenderPass.CAMERA).getPosition(), mc.gameRenderer.minClipDistance) == null)
					mc.stereoProvider.cameraFramebuffer.bindFramebufferTexture();
				else
					mc.getTextureManager().bindTexture(new ResourceLocation("vivecraft:textures/black.png"));
			}, face -> {
				if (face == Direction.SOUTH)
					return DisplayFace.NORMAL;
				return DisplayFace.NONE;
			});
		}
	}

	public static void renderVRCameraWidget(float offsetX, float offsetY, float offsetZ, float scale, RenderPass renderPass, ModelResourceLocation model, ModelResourceLocation displayModel, Runnable displayBindFunc, Function<Direction, DisplayFace> displayFaceFunc) {
		Minecraft mc = Minecraft.getInstance();
		RenderSystem.pushMatrix();

		mc.gameRenderer.applyVRModelViewLegacy(mc.currentPass);

		Vector3d cam = mc.vrPlayer.vrdata_world_render.getEye(renderPass).getPosition();
		Vector3d o = mc.vrPlayer.vrdata_world_render.getEye(mc.currentPass).getPosition();
		Vector3d pos = cam.subtract(o);

		RenderSystem.enableDepthTest();
		RenderSystem.defaultBlendFunc();
		//RenderSystem.depthFunc(GL11.GL_ALWAYS);

		RenderSystem.translated(pos.x, pos.y, pos.z);
		RenderSystem.multMatrix(mc.vrPlayer.vrdata_world_render.getEye(renderPass).getMatrix().toMCMatrix());

		scale = scale * mc.vrPlayer.vrdata_world_render.worldScale;
		RenderSystem.scalef(scale, scale, scale);

		// Position testing
		if (debug) {
			RenderSystem.rotatef(180, 0, 1, 0);
			mc.gameRenderer.renderDebugAxes(0, 0, 0, 0.08f);
			RenderSystem.rotatef(180, 0, 1, 0);
		}

		// Probably magic transform
		RenderSystem.translatef(offsetX, offsetY, offsetZ);

		mc.getTextureManager().bindTexture(PlayerContainer.LOCATION_BLOCKS_TEXTURE);
		BlockPos lightPos = new BlockPos(mc.vrPlayer.vrdata_world_render.getEye(renderPass).getPosition());
		int combinedLight = Utils.getCombinedLightWithMin(mc.world, lightPos, 0);

		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buffer = tess.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
		mc.getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightnessColor(new MatrixStack().getLast(), buffer, null, mc.getModelManager().getModel(model), 1.0F, 1.0F, 1.0F, combinedLight, OverlayTexture.NO_OVERLAY);
		tess.draw();

		RenderSystem.disableBlend();
		RenderSystem.alphaFunc(GL11.GL_ALWAYS, 0);
		displayBindFunc.run();

		BufferBuilder b = tess.getBuffer();
		b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_LMAP_COLOR_NORMAL);

		// This is very silly but it works
		for (BakedQuad quad : mc.getModelManager().getModel(displayModel).getQuads(null, null, random)) {
			if (displayFaceFunc.apply(quad.getFace()) != DisplayFace.NONE && quad.getSprite().getName().equals(new ResourceLocation("vivecraft:transparent"))) {
				QuadBounds bounds = quad.getQuadBounds();
				boolean mirror = displayFaceFunc.apply(quad.getFace()) == DisplayFace.MIRROR;
				int light = LightTexture.packLight(15, 15);

				b.pos(mirror ? bounds.getMaxX() : bounds.getMinX(), bounds.getMinY(), bounds.getMinZ()).tex(mirror ? 1.0f : 0.0f, 0.0f).lightmap(light).color(1.0f, 1.0f, 1.0f, 1.0f).normal(0, 0, mirror ? -1 : 1).endVertex();
				b.pos(mirror ? bounds.getMinX() : bounds.getMaxX(), bounds.getMinY(), bounds.getMinZ()).tex(mirror ? 0.0f : 1.0f, 0.0f).lightmap(light).color(1.0f, 1.0f, 1.0f, 1.0f).normal(0, 0, mirror ? -1 : 1).endVertex();
				b.pos(mirror ? bounds.getMinX() : bounds.getMaxX(), bounds.getMaxY(), bounds.getMinZ()).tex(mirror ? 0.0f : 1.0f, 1.0f).lightmap(light).color(1.0f, 1.0f, 1.0f, 1.0f).normal(0, 0, mirror ? -1 : 1).endVertex();
				b.pos(mirror ? bounds.getMaxX() : bounds.getMinX(), bounds.getMaxY(), bounds.getMinZ()).tex(mirror ? 1.0f : 0.0f, 1.0f).lightmap(light).color(1.0f, 1.0f, 1.0f, 1.0f).normal(0, 0, mirror ? -1 : 1).endVertex();
			}
		}

		tess.draw();

		//RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.enableBlend();
		RenderSystem.defaultAlphaFunc();
		RenderSystem.popMatrix();
	}

	public enum DisplayFace {
		NONE,
		NORMAL,
		MIRROR
	}
}
