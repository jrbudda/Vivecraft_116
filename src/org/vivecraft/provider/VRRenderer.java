package org.vivecraft.provider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.vivecraft.gameplay.screenhandlers.GuiHandler;
import org.vivecraft.gameplay.screenhandlers.KeyboardHandler;
import org.vivecraft.gameplay.screenhandlers.RadialHandler;
import org.vivecraft.gameplay.trackers.TelescopeTracker;
import org.vivecraft.render.RenderConfigException;
import org.vivecraft.render.RenderPass;
import org.vivecraft.render.ShaderHelper;
import org.vivecraft.render.VRShaders;
import org.vivecraft.settings.VRSettings;
import org.vivecraft.utils.LangHelper;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.world.DimensionType;
import net.optifine.Config;
import net.optifine.shaders.Shaders;

public abstract class VRRenderer {
	
	public static final String RENDER_SETUP_FAILURE_MESSAGE = "Failed to initialise stereo rendering plugin: ";

	public Map<String,ShaderGroup> alphaShaders = new HashMap<String, ShaderGroup>();
	public Framebuffer cameraFramebuffer;
	public Framebuffer cameraRenderFramebuffer;
	protected int dispLastWidth, dispLastHeight;
	public Map<String,ShaderGroup> entityShaders = new HashMap<String, ShaderGroup>();
	public Matrix4f[] eyeproj = new Matrix4f[2];
	//public net.minecraft.client.renderer.Matrix4f[] cloudeyeproj = new net.minecraft.client.renderer.Matrix4f[2];
	//output fbs
	public Framebuffer framebufferEye0;

	public Framebuffer framebufferEye1;
	public Framebuffer framebufferMR;

	public Framebuffer framebufferUndistorted;
	//Render target fbs
	public Framebuffer framebufferVrRender;

	//intermediate fbs
	public Framebuffer fsaaFirstPassResultFBO;

	public Framebuffer fsaaLastPassResultFBO;
	
	protected float[][] hiddenMesheVertecies = new float[2][];
	public RegistryKey<DimensionType> lastDimensionId = DimensionType.OVERWORLD;  

	public int lastDisplayFBHeight = 0;
	public int lastDisplayFBWidth = 0;
	public boolean lastEnableVsync = true;
	public boolean lastFogFancy = true;
	public boolean lastFogFast = false;
	public int lastGuiScale = 0;
	protected int lastMirror;
	public int lastRenderDistanceChunks = -1;
	public long lastWindow = 0;
	public float lastWorldScale = 0f;
	// TextureIDs of framebuffers for each eye
	protected int LeftEyeTextureId =-1, RightEyeTextureId=-1;
	public int mirrorFBHeight;

	public int mirrorFBWidth;     /* Actual width of the display buffer */
	protected boolean reinitFramebuffers = true;

	public boolean reinitShadersFlag = false;

	public float renderScale;
	protected Tuple<Integer, Integer> resolution;
	public float ss = -1;

	public Framebuffer telescopeFramebufferL;
	public Framebuffer telescopeFramebufferR;

	protected MCVR vr;
	public VRRenderer(MCVR vr) {
		super();
		this.vr = vr;
	}
	protected void checkGLError(String message)
	{
		Config.checkGlError(message);
	}
	public boolean clipPlanesChanged()
	{	
		return false;
		//TODO: Update

		//			boolean changed = false;
		//
		//			if (this.world != null && this.world.provider != null)
		//			{
		//				if (this.world.provider.getDimensionType() != this.lastDimensionId)
		//				{
		//					changed = true;
		//				}
		//			}
		//
		//			if( this.gameSettings.renderDistanceChunks != this.lastRenderDistanceChunks ||
		//					Config.isFogFancy() != this.lastFogFancy                                ||
		//					Config.isFogFast() != this.lastFogFast)
		//			{
		//				changed = true;
		//			}
		//
		//			
		//			lastRenderDistanceChunks = mc.gameSettings.renderDistanceChunks;
		//			lastFogFancy = Config.isFogFancy();
		//			lastFogFast = Config.isFogFast();
		//			if (this.world != null && this.world.provider != null)
		//				lastDimensionId = this.world.provider.getDimensionType();
		//
		//			return changed;
	}
	
	public abstract void createRenderTexture(int lwidth, int lheight);
	public abstract Matrix4f getProjectionMatrix(int eyeType,float nearClip,float farClip);
	public abstract void endFrame() throws RenderConfigException;
	public abstract boolean providesStencilMask();
	
	protected ShaderGroup createShaderGroup(ResourceLocation resource, Framebuffer fb) throws JsonSyntaxException, IOException
	{
		Minecraft mc = Minecraft.getInstance();
		ShaderGroup shadergroup = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), fb, resource);
		shadergroup.createBindFramebuffers(fb.framebufferWidth, fb.framebufferHeight);
		return shadergroup;
	}

	public void deleteRenderTextures() {
		if (LeftEyeTextureId > 0)	GL11.glDeleteTextures(LeftEyeTextureId);
		if (RightEyeTextureId > 0)	GL11.glDeleteTextures(RightEyeTextureId);
		LeftEyeTextureId = RightEyeTextureId = -1;
	}

	public void doCircleStencil(Framebuffer fb) {
		Minecraft mc = Minecraft.getInstance();

		GL11.glEnable(GL11.GL_STENCIL_TEST);

		GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
		GL11.glStencilMask(0xFF); // Write to stencil buffer
		GL11.glClearStencil(0xFF);
		GlStateManager.clear(GL11.GL_STENCIL_BUFFER_BIT); // Clear stencil buffer (0 by default)
		GL11.glClearStencil(0);
		GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF); // Set any stencil to 1
		RenderSystem.colorMask(false, false, false, true); //do write to alpha.
		RenderSystem.depthMask(false); // Don't write to depth buffer

		RenderSystem.disableAlphaTest();
		RenderSystem.disableDepthTest();
		RenderSystem.disableTexture();
		RenderSystem.disableCull();

		RenderSystem.color4f(0, 0, 0, 1);

		RenderSystem.matrixMode(GL11.GL_PROJECTION);
		RenderSystem.pushMatrix();
		RenderSystem.loadIdentity();
		RenderSystem.matrixMode(GL11.GL_MODELVIEW);
		RenderSystem.pushMatrix();
		RenderSystem.loadIdentity();
		RenderSystem.ortho(0.0D, fb.framebufferWidth, 0.0D, fb.framebufferHeight, -10, 20.0D);
		RenderSystem.viewport(0, 0, fb.framebufferWidth, fb.framebufferHeight);

		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		int edges = 32;
		float radius = fb.framebufferWidth/2;
		GL11.glVertex2f(fb.framebufferWidth/2,fb.framebufferWidth/2);
		for (int i=0;i<edges + 1;i++)
		{
			float startAngle;
			startAngle = ( (float) (i) / (float) edges ) * (float) Math.PI * 2.0f;
			float x =  (float) (fb.framebufferWidth/2 + Math.cos(startAngle) * radius);
			float z =  (float) (fb.framebufferWidth/2 + Math.sin(startAngle) * radius);
			GL11.glVertex2f(x,z);
		}	
		GL11.glEnd();
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		RenderSystem.popMatrix();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		RenderSystem.popMatrix();

		RenderSystem.depthMask(true); // Do write to depth buffer
		RenderSystem.colorMask(true, true, true, true);

		RenderSystem.enableDepthTest();
		RenderSystem.enableAlphaTest();
		RenderSystem.enableTexture();
		RenderSystem.enableCull();

		GL11.glStencilFunc(GL11.GL_NOTEQUAL, 0xFF, 1);
		GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
		GL11.glStencilMask(0x0); // Dont Write to stencil buffer

		/// END STENCIL TESTING
	}

	public void doFSAA(boolean hasShaders) {
		if (this.fsaaFirstPassResultFBO == null){
			this.reinitFrameBuffers("FSAA Setting Changed");
			return;
		} else {

			GlStateManager.disableAlphaTest();
			GlStateManager.disableBlend();

			// Setup ortho projection
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GlStateManager.pushMatrix();
			GlStateManager.loadIdentity();
			GlStateManager.matrixMode(GL11.GL_MODELVIEW);
			GlStateManager.pushMatrix();
			GlStateManager.loadIdentity();

			GL11.glTranslatef(0.0f, 0.0f, -.7f);
			// Pass 1 - horizontal
			// Now switch to 1st pass FSAA result target framebuffer
			this.fsaaFirstPassResultFBO.bindFramebuffer(true);

			// bind color and depth textures
			GlStateManager.activeTexture(GL13.GL_TEXTURE1);
			framebufferVrRender.bindFramebufferTexture();
			GlStateManager.activeTexture(GL13.GL_TEXTURE2);		

			if (hasShaders && Shaders.dfb != null) 
				GlStateManager.bindTexture(Shaders.dfb.depthTextures.get(0)); // shadersmod has its own depth buffer
			else 
				GlStateManager.bindTexture(framebufferVrRender.depthBuffer);

			GlStateManager.activeTexture(GL13.GL_TEXTURE0);

			GlStateManager.clearColor(1, 1, 1, 1.0f);
			GlStateManager.clearDepth(1.0D);
			GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);            // Clear Screen And Depth Buffer on the framebuffer

			// Render onto the entire screen framebuffer
			GlStateManager.viewport(0, 0, fsaaFirstPassResultFBO.framebufferWidth, fsaaFirstPassResultFBO.framebufferHeight);

			// Set the downsampling shader as in use
			ARBShaderObjects.glUseProgramObjectARB(VRShaders._Lanczos_shaderProgramId);

			// Set up the fragment shader uniforms
			ARBShaderObjects.glUniform1fARB(VRShaders._Lanczos_texelWidthOffsetUniform, 1.0f / (3.0f * (float) fsaaFirstPassResultFBO.framebufferWidth));
			ARBShaderObjects.glUniform1fARB(VRShaders._Lanczos_texelHeightOffsetUniform, 0.0f);
			ARBShaderObjects.glUniform1iARB(VRShaders._Lanczos_inputImageTextureUniform, 1);
			ARBShaderObjects.glUniform1iARB(VRShaders._Lanczos_inputDepthTextureUniform, 2);

			GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);


			drawQuad();

			// checkGLError("After Lanczos Pass1");

			// Pass 2 - Vertial
			// Now switch to 2nd pass screen framebuffer

			fsaaLastPassResultFBO.bindFramebuffer(true);				

			// bind color and depth textures
			GlStateManager.activeTexture(GL13.GL_TEXTURE1);
			fsaaFirstPassResultFBO.bindFramebufferTexture();
			GlStateManager.activeTexture(GL13.GL_TEXTURE2);			
			GlStateManager.bindTexture(fsaaFirstPassResultFBO.depthBuffer);
			GlStateManager.activeTexture(GL13.GL_TEXTURE0);
			//					

			checkGLError("posttex");

			GlStateManager.viewport(0, 0, fsaaLastPassResultFBO.framebufferWidth, fsaaLastPassResultFBO.framebufferHeight);

			GlStateManager.clearColor(1, 1, 1, 1.0f);
			GlStateManager.clearDepth(1.0D);
			GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
			checkGLError("postclear");
			// Bind the texture
			GlStateManager.activeTexture(GL13.GL_TEXTURE0);
			checkGLError("postact");
			// Set up the fragment shader uniforms for pass 2
			ARBShaderObjects.glUniform1fARB(VRShaders._Lanczos_texelWidthOffsetUniform, 0.0f);
			ARBShaderObjects.glUniform1fARB(VRShaders._Lanczos_texelHeightOffsetUniform, 1.0f / (3.0f * (float) framebufferEye0.framebufferHeight));
			ARBShaderObjects.glUniform1iARB(VRShaders._Lanczos_inputImageTextureUniform, 1);
			ARBShaderObjects.glUniform1iARB(VRShaders._Lanczos_inputDepthTextureUniform, 2);

			drawQuad();

			checkGLError("postdraw");

			// Stop shader use
			ARBShaderObjects.glUseProgramObjectARB(0);
			// checkGLError("After Lanczos Pass2");

			GlStateManager.enableAlphaTest();
			GlStateManager.enableBlend();

			GlStateManager.matrixMode(GL11.GL_PROJECTION);
			GlStateManager.popMatrix();		
			GlStateManager.matrixMode(GL11.GL_MODELVIEW);
			GlStateManager.popMatrix();
		}
	}

	public void doStencilForEye(int i) {
		Minecraft mc = Minecraft.getInstance();
		float[] verts = getStencilMask(mc.currentPass);

		//START STENCIL TESTING - Yes I know there's about 15 better ways to do this.
		GL11.glEnable(GL11.GL_STENCIL_TEST);

		GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
		GL11.glStencilMask(0xFF); // Write to stencil buffer
		GlStateManager.clear(GL11.GL_STENCIL_BUFFER_BIT); // Clear stencil buffer (0 by default)
		GL11.glStencilFunc(GL11.GL_ALWAYS, 0xFF, 0xFF); // Set any stencil to 1

		if (verts != null) {
			RenderSystem.disableAlphaTest();
			RenderSystem.disableDepthTest();
			RenderSystem.disableTexture();
			RenderSystem.disableCull();

			RenderSystem.color3f(0, 0, 0);
			RenderSystem.depthMask(false); // Don't write to depth buffer
			RenderSystem.matrixMode(GL11.GL_PROJECTION);
			RenderSystem.pushMatrix();
			RenderSystem.loadIdentity();
			RenderSystem.matrixMode(GL11.GL_MODELVIEW);
			RenderSystem.pushMatrix();
			RenderSystem.loadIdentity();
			RenderSystem.ortho(0.0D, framebufferVrRender.framebufferWidth, 0.0D, framebufferVrRender.framebufferHeight, -10, 20.0D);
			RenderSystem.viewport(0, 0, framebufferVrRender.framebufferWidth, framebufferVrRender.framebufferHeight);
			//this viewport might be wrong for some shaders.
			GL11.glBegin(GL11.GL_TRIANGLES);

			for (int ix = 0; ix < verts.length; ix += 2) {
				GL11.glVertex2f(verts[ix] * mc.vrRenderer.renderScale, verts[ix + 1] * mc.vrRenderer.renderScale);
			}
			GL11.glEnd();

			GL11.glMatrixMode(GL11.GL_PROJECTION);
			RenderSystem.popMatrix();
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			RenderSystem.popMatrix();

			RenderSystem.depthMask(true); // Do write to depth buffer

			RenderSystem.enableDepthTest();
			RenderSystem.enableAlphaTest();
			RenderSystem.enableTexture();
			RenderSystem.enableCull();
		}

		GL11.glStencilFunc(GL11.GL_NOTEQUAL, 0xFF, 1);
		GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
		GL11.glStencilMask(0x0); // Dont Write to stencil buffer
		/// END STENCIL TESTING
	}

	public void drawQuad()
	{
		// this func just draws a perfectly normal box with some texture coordinates
		GL11.glBegin(GL11.GL_QUADS);

		// Front Face
		GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex3f(-1.0f, -1.0f,  0.0f);  // Bottom Left Of The Texture and Quad
		GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex3f( 1.0f, -1.0f,  0.0f);  // Bottom Right Of The Texture and Quad
		GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex3f( 1.0f,  1.0f,  0.0f);  // Top Right Of The Texture and Quad
		GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex3f(-1.0f,  1.0f,  0.0f);  // Top Left Of The Texture and Quad

		GL11.glEnd();
	}

	public double getCurrentTimeSecs()
	{
		return System.nanoTime() / 1000000000d;
	}

	public double getFrameTiming() {
		return getCurrentTimeSecs();
	}

	public String getinitError() {
		return vr.initStatus;
	}

	public String getLastError() { return ""; }

	public String getName() {
		return "OpenVR";
	}

	public List<RenderPass> getRenderPasses() {
		Minecraft mc = Minecraft.getInstance();
		List<RenderPass> passes = new ArrayList<>();

		// Always do these for obvious reasons
		passes.add(RenderPass.LEFT);
		passes.add(RenderPass.RIGHT);

		if (mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_FIRST_PERSON) {
			passes.add(RenderPass.CENTER);
		} else if (mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_MIXED_REALITY) {
			if (mc.vrSettings.mixedRealityMRPlusUndistorted && mc.vrSettings.mixedRealityUnityLike)
				passes.add(RenderPass.CENTER);
			passes.add(RenderPass.THIRD);
		} else if (mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_THIRD_PERSON) {
			passes.add(RenderPass.THIRD);
		}

		if(mc.player != null) {
			if (TelescopeTracker.isTelescope(mc.player.getHeldItemMainhand())) {
				if(TelescopeTracker.isViewing(0))
					passes.add(RenderPass.SCOPER);
			}		
			if (TelescopeTracker.isTelescope(mc.player.getHeldItemOffhand())) {
				if(TelescopeTracker.isViewing(1))
					passes.add(RenderPass.SCOPEL);
			}
			if (mc.cameraTracker.isVisible())
				passes.add(RenderPass.CAMERA);
		}
		return passes;
	}

	public abstract Tuple<Integer, Integer> getRenderTextureSizes();

	public float[] getStencilMask(RenderPass eye) {
		if(hiddenMesheVertecies == null || (eye != RenderPass.LEFT && eye != RenderPass.RIGHT)) return null;
		return eye == RenderPass.LEFT ? hiddenMesheVertecies[0] : hiddenMesheVertecies[1];
	}

	public boolean isInitialized() {
		return vr.initSuccess;
	}

	public void reinitFrameBuffers(String cause) {
		this.reinitFramebuffers  =true;
		System.out.println("Reinit Render: " + cause );
	}

	public void setupRenderConfiguration() throws Exception 
	{
		Minecraft mc = Minecraft.getInstance();
		boolean changeNonDestructiveRenderConfig = false;

		if (clipPlanesChanged()) {
			reinitFrameBuffers("Clip Planes Changed");
		}

		//if (lastGuiScale != mc.gameSettings.guiScale)
		//{
		//	lastGuiScale = mc.gameSettings.guiScale;
		//	reinitFrameBuffers("GUI Scale Changed");
		//}

		// Check for changes in window handle
		if (mc.getMainWindow().getHandle() != lastWindow) {
			lastWindow = mc.getMainWindow().getHandle();
			reinitFrameBuffers("Window Handle Changed");
		}

		if (lastEnableVsync != mc.gameSettings.vsync) {
			reinitFrameBuffers("VSync Changed");
			lastEnableVsync = mc.gameSettings.vsync;
		}

		if (lastMirror != mc.vrSettings.displayMirrorMode) {
			reinitFrameBuffers("Mirror Changed");
			lastMirror = mc.vrSettings.displayMirrorMode;
		}

		if (reinitFramebuffers) {
			//visible = true;
			this.reinitShadersFlag = true;
			checkGLError("Start Init");

			int displayFBWidth = (mc.getMainWindow().getWidth() < 1) ? 1 : mc.getMainWindow().getWidth();
			int displayFBHeight = (mc.getMainWindow().getHeight() < 1) ? 1 : mc.getMainWindow().getHeight();

			int eyew, eyeh;

			eyew = displayFBWidth;
			eyeh = displayFBHeight;

			if (Config.openGlRenderer.toLowerCase().contains("intel")) {
				throw new RenderConfigException("Incompatible", LangHelper.get("vivecraft.messages.intelgraphics", Config.openGlRenderer));
			}

			if (!isInitialized()) {
				throw new RenderConfigException(RENDER_SETUP_FAILURE_MESSAGE + getName(), LangHelper.get(getinitError()));
			}

			Tuple<Integer, Integer> renderTextureInfo = getRenderTextureSizes();

			eyew = renderTextureInfo.getA();
			eyeh = renderTextureInfo.getB();

			if (framebufferVrRender != null) {
				framebufferVrRender.deleteFramebuffer();
				framebufferVrRender = null;
			}

			if (framebufferMR != null) {
				framebufferMR.deleteFramebuffer();
				framebufferMR = null;
			}

			if (framebufferUndistorted != null) {
				framebufferUndistorted.deleteFramebuffer();
				framebufferUndistorted = null;
			}

			//if (framebufferEye0 != null) {
			//	framebufferEye0.deleteFramebuffer();
			//	framebufferEye0 = null;
			//}

			//if (framebufferEye1 != null) {
			//	framebufferEye1.deleteFramebuffer();
			//	framebufferEye1 = null;
			//}

			// SteamVR on Linux breaks if we delete the eye textures
			// https://github.com/ValveSoftware/SteamVR-for-Linux/issues/378
			//deleteRenderTextures();

			if (GuiHandler.guiFramebuffer != null) {
				GuiHandler.guiFramebuffer.deleteFramebuffer();
				GuiHandler.guiFramebuffer = null;
			}

			if (KeyboardHandler.Framebuffer != null) {
				KeyboardHandler.Framebuffer.deleteFramebuffer();
				KeyboardHandler.Framebuffer = null;
			}
			if (RadialHandler.Framebuffer != null) {
				RadialHandler.Framebuffer.deleteFramebuffer();
				RadialHandler.Framebuffer = null;
			}
			if (telescopeFramebufferL != null) {
				telescopeFramebufferL.deleteFramebuffer();
				telescopeFramebufferL = null;
			}
			if (telescopeFramebufferR != null) {
				telescopeFramebufferR.deleteFramebuffer();
				telescopeFramebufferR = null;
			}
			if (cameraFramebuffer != null) {
				cameraFramebuffer.deleteFramebuffer();
				cameraFramebuffer = null;
			}
			if (cameraRenderFramebuffer != null) {
				cameraRenderFramebuffer.deleteFramebuffer();
				cameraRenderFramebuffer = null;
			}
			//if (loadingScreen != null) {
			//	loadingScreen.deleteFramebuffer();
			//}

			if (fsaaFirstPassResultFBO != null) {
				fsaaFirstPassResultFBO.deleteFramebuffer();
				fsaaFirstPassResultFBO = null;
			}

			if (fsaaLastPassResultFBO != null) {
				fsaaLastPassResultFBO.deleteFramebuffer();
				fsaaLastPassResultFBO = null;
			}

			int multiSampleCount = 0;
			boolean multiSample = multiSampleCount > 0;

			if (LeftEyeTextureId == -1) {
				createRenderTexture(eyew, eyeh);
				if (LeftEyeTextureId == -1)
					throw new RenderConfigException(RENDER_SETUP_FAILURE_MESSAGE + getName(), getLastError());

				mc.print("Provider supplied render texture IDs: " + LeftEyeTextureId + " " + RightEyeTextureId);
				mc.print("Provider supplied texture resolution: " + eyew + " x " + eyeh);
			}

			checkGLError("Render Texture setup");

			if (framebufferEye0 == null) {
				framebufferEye0 = new Framebuffer("L Eye", eyew, eyeh, false, false, LeftEyeTextureId, false, true);
				mc.print(framebufferEye0.toString());
				checkGLError("Left Eye framebuffer setup");
			}

			if (framebufferEye1 == null) {
				framebufferEye1 = new Framebuffer("R Eye", eyew, eyeh, false, false, RightEyeTextureId, false, true);
				mc.print(framebufferEye1.toString());
				checkGLError("Right Eye framebuffer setup");
			}

			//vr.texType0.depth.handle = Pointer.createConstant(framebufferEye0.depthBuffer);
			//vr.texType1.depth.handle = Pointer.createConstant(framebufferEye1.depthBuffer);
			this.renderScale = (float)Math.sqrt((mc.vrSettings.renderScaleFactor));
			displayFBWidth = (int)Math.ceil(eyew * renderScale);
			displayFBHeight = (int)Math.ceil(eyeh * renderScale);

			framebufferVrRender = new Framebuffer("3D Render", displayFBWidth, displayFBHeight, true, false, Framebuffer.NO_TEXTURE_ID, true, true);
			mc.print(framebufferVrRender.toString());
			checkGLError("3D framebuffer setup");

			mirrorFBWidth = mc.getMainWindow().getWidth();
			mirrorFBHeight = mc.getMainWindow().getHeight();

			if (mc.vrSettings.displayMirrorMode == VRSettings.MIRROR_MIXED_REALITY) {
				mirrorFBWidth = mc.getMainWindow().getWidth() / 2;
				if (mc.vrSettings.mixedRealityUnityLike)
					mirrorFBHeight = mc.getMainWindow().getHeight() / 2;
			}

			if (Config.isShaders()) {
				mirrorFBWidth = displayFBWidth;
				mirrorFBHeight = displayFBHeight;
			}

			List<RenderPass> renderPasses = getRenderPasses();

			//debug
			for (RenderPass renderPass : renderPasses) {
				System.out.println("Passes: " + renderPass.toString());
			}

			if (renderPasses.contains(RenderPass.THIRD)) {
				framebufferMR = new Framebuffer("Mixed Reality Render", mirrorFBWidth, mirrorFBHeight, true, false, Framebuffer.NO_TEXTURE_ID, true, false);
				mc.print(framebufferMR.toString());
				checkGLError("Mixed reality framebuffer setup");
			}

			if (renderPasses.contains(RenderPass.CENTER)) {
				framebufferUndistorted = new Framebuffer("Undistorted View Render", mirrorFBWidth, mirrorFBHeight, true, false, Framebuffer.NO_TEXTURE_ID, false, false);
				mc.print(framebufferUndistorted.toString());
				checkGLError("Undistorted view framebuffer setup");
			}

			GuiHandler.guiFramebuffer = new Framebuffer("GUI", mc.getMainWindow().getWidth(), mc.getMainWindow().getHeight(), true, false, Framebuffer.NO_TEXTURE_ID, false, true);
			mc.print(GuiHandler.guiFramebuffer.toString());
			checkGLError("GUI framebuffer setup");

			KeyboardHandler.Framebuffer = new Framebuffer("Keyboard", mc.getMainWindow().getWidth(), mc.getMainWindow().getHeight(), true, false, Framebuffer.NO_TEXTURE_ID, false, true);
			mc.print(KeyboardHandler.Framebuffer.toString());
			checkGLError("Keyboard framebuffer setup");

			RadialHandler.Framebuffer = new Framebuffer("Radial Menu", mc.getMainWindow().getWidth(), mc.getMainWindow().getHeight(), true, false, Framebuffer.NO_TEXTURE_ID, false, true);
			mc.print(RadialHandler.Framebuffer.toString());
			checkGLError("Radial framebuffer setup");

			int scopeW = 720;
			int scopeH = 720;

			if (Config.isShaders()) { //ugh.
				scopeW = displayFBWidth;
				scopeH = displayFBHeight;
			}

			checkGLError("Mirror framebuffer setup");

			telescopeFramebufferR = new Framebuffer("TelescopeR", scopeW, scopeH, true, false, Framebuffer.NO_TEXTURE_ID, true, false);
			mc.print(telescopeFramebufferR.toString());
			checkGLError("TelescopeR framebuffer setup");

			telescopeFramebufferL = new Framebuffer("TelescopeL", scopeW, scopeH, true, false, Framebuffer.NO_TEXTURE_ID, true, false);
			mc.print(telescopeFramebufferL.toString());
			checkGLError("TelescopeL framebuffer setup");

			int cameraW = Math.round(1920 * mc.vrSettings.handCameraResScale);
			int cameraH = Math.round(1080 * mc.vrSettings.handCameraResScale);
			int cameraRenderW = cameraW;
			int cameraRenderH = cameraH;

			if (Config.isShaders()) { //double ugh.
				float aspect = (float)cameraW / (float)cameraH;
				if (aspect > (displayFBWidth / displayFBHeight)) {
					cameraW = displayFBWidth;
					cameraH = Math.round(displayFBWidth / aspect);
				} else {
					cameraW = Math.round(displayFBHeight * aspect);
					cameraH = displayFBHeight;
				}

				cameraRenderW = displayFBWidth;
				cameraRenderH = displayFBHeight;
			}

			cameraFramebuffer = new Framebuffer("Handheld Camera", cameraW, cameraH, true, false, Framebuffer.NO_TEXTURE_ID, true, false);
			mc.print(cameraFramebuffer.toString());
			checkGLError("Camera framebuffer setup");

			cameraRenderFramebuffer = new Framebuffer("Handheld Camera Render", cameraRenderW, cameraRenderH, true, false, Framebuffer.NO_TEXTURE_ID, true, true);
			mc.print(cameraRenderFramebuffer.toString());
			checkGLError("Camera render framebuffer setup");

			mc.gameRenderer.setupClipPlanes();

			eyeproj[0] = getProjectionMatrix(0, mc.gameRenderer.minClipDistance, mc.gameRenderer.clipDistance * 4);
			eyeproj[1] = getProjectionMatrix(1, mc.gameRenderer.minClipDistance, mc.gameRenderer.clipDistance * 4);

			if (mc.vrSettings.useFsaa) {
				try //setup fsaa
				{
					checkGLError("pre FSAA FBO creation");
					// Lanczos downsample FBOs
					fsaaFirstPassResultFBO = new Framebuffer("FSAA Pass1 FBO", eyew, displayFBHeight, false, false, Framebuffer.NO_TEXTURE_ID, false, false);
					//TODO: ugh, support multiple color attachments in Framebuffer....
					fsaaLastPassResultFBO = new Framebuffer("FSAA Pass2 FBO", eyew, eyeh, false, false, Framebuffer.NO_TEXTURE_ID, false, false);

					mc.print(fsaaFirstPassResultFBO.toString());
					mc.print(fsaaLastPassResultFBO.toString());

					checkGLError("FSAA FBO creation");

					VRShaders.setupFSAA();

					ShaderHelper.checkGLError("FBO init fsaa shader");
				} catch (Exception ex) {
					// We had an issue. Set the usual suspects to defaults...
					mc.vrSettings.useFsaa = false;
					mc.vrSettings.saveOptions();
					System.out.println(ex.getMessage());
					reinitFramebuffers = true;
					return;
				}
			}

			try { //setup other shaders
				mc.framebuffer = this.framebufferVrRender;
				VRShaders.setupDepthMask();
				ShaderHelper.checkGLError("init depth shader");
				VRShaders.setupFOVReduction();
				ShaderHelper.checkGLError("init FOV shader");

				List<ShaderGroup> old = new ArrayList<>();
				old.addAll(entityShaders.values());

				//vanilla entity outline shader

				entityShaders.clear();
				ResourceLocation outline = new ResourceLocation("shaders/post/entity_outline.json");
				entityShaders.put(framebufferVrRender.name, createShaderGroup(outline, framebufferVrRender));
				if (renderPasses.contains(RenderPass.THIRD))
					entityShaders.put(framebufferMR.name, createShaderGroup(outline, framebufferMR));
				if (renderPasses.contains(RenderPass.CENTER))
					entityShaders.put(framebufferUndistorted.name, createShaderGroup(outline, framebufferUndistorted));
				entityShaders.put(telescopeFramebufferL.name, createShaderGroup(outline, telescopeFramebufferL));
				entityShaders.put(telescopeFramebufferR.name, createShaderGroup(outline, telescopeFramebufferR));
				entityShaders.put(cameraRenderFramebuffer.name, createShaderGroup(outline, cameraRenderFramebuffer));

				for (ShaderGroup s : old) {
					s.close();
				}
				old.clear();
				//

				old.addAll(alphaShaders.values());
				//Vanilla alpha sort shader

				alphaShaders.clear();
				if (Minecraft.isFabulousGraphicsEnabled()) { //Fabulous
					ResourceLocation resourcelocation = new ResourceLocation("shaders/post/vrtransparency.json");
					alphaShaders.put(framebufferVrRender.name, createShaderGroup(resourcelocation, framebufferVrRender));
					if (renderPasses.contains(RenderPass.THIRD))
						alphaShaders.put(framebufferMR.name, createShaderGroup(resourcelocation, framebufferMR));
					if (renderPasses.contains(RenderPass.CENTER))
						alphaShaders.put(framebufferUndistorted.name, createShaderGroup(resourcelocation, framebufferUndistorted));
					alphaShaders.put(telescopeFramebufferL.name, createShaderGroup(resourcelocation, telescopeFramebufferL));
					alphaShaders.put(telescopeFramebufferR.name, createShaderGroup(resourcelocation, telescopeFramebufferR));
					alphaShaders.put(cameraRenderFramebuffer.name, createShaderGroup(resourcelocation, cameraRenderFramebuffer));
				} else {//not fabulous!

				}

				for (ShaderGroup s : old) {
					s.close();
				}			
				//

				//Vanilla mob spectator shader
				mc.gameRenderer.loadEntityShader(mc.getRenderViewEntity());
				//
			} catch (Exception e) {
				System.out.println(e.getMessage());
				System.exit(-1);
			}

			// Init screen size
			if (mc.currentScreen != null) {
				int k = mc.getMainWindow().getScaledWidth();
				int l = mc.getMainWindow().getScaledHeight();
				mc.currentScreen.init(mc, k, l);
			}

			long mainWindowPixels = mc.getMainWindow().getWidth() * mc.getMainWindow().getHeight();
			long pixelsPerFrame = displayFBWidth * displayFBHeight * 2;
			if (renderPasses.contains(RenderPass.CENTER))
				pixelsPerFrame += mainWindowPixels;
			if (renderPasses.contains(RenderPass.THIRD))
				pixelsPerFrame += mainWindowPixels;
			System.out.println("[Minecrift] New render config:" +
					"\nOpenVR target width: " + eyew + ", height: " + eyeh + " [" + String.format("%.1f", (eyew * eyeh) / 1000000F) + " MP]" +
					"\nRender target width: " + displayFBWidth + ", height: " + displayFBHeight + " [Render scale: " + Math.round(mc.vrSettings.renderScaleFactor * 100) + "%, " + String.format("%.1f", (displayFBWidth * displayFBHeight) / 1000000F) + " MP]" +
					"\nMain window width: " + mc.getMainWindow().getWidth() + ", height: " + mc.getMainWindow().getHeight() + " [" + String.format("%.1f", mainWindowPixels / 1000000F) + " MP]" +
					"\nTotal shaded pixels per frame: " + String.format("%.1f", pixelsPerFrame / 1000000F) + " MP (eye stencil not accounted for)"
					);

			lastDisplayFBWidth = displayFBWidth;
			lastDisplayFBHeight = displayFBHeight;
			reinitFramebuffers = false;
		}
	}

	public boolean wasDisplayResized()
	{
		Minecraft mc = Minecraft.getInstance();

		int h = mc.getMainWindow().getHeight();
		int w = mc.getMainWindow().getWidth();

		boolean was = dispLastHeight != h || dispLastWidth != w;
		dispLastHeight = h;
		dispLastWidth = w;
		return was;
	}

}

