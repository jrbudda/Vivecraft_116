package org.vivecraft.provider.openvr_jna;

import org.lwjgl.opengl.GL11;
import org.vivecraft.provider.MCVR;
import org.vivecraft.provider.VRRenderer;
import org.vivecraft.render.RenderConfigException;
import org.vivecraft.render.RenderPass;
import org.vivecraft.utils.Utils;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import jopenvr.HiddenAreaMesh_t;
import jopenvr.HmdMatrix44_t;
import jopenvr.JOpenVRLibrary;
import jopenvr.JOpenVRLibrary.EVRCompositorError;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.vector.Matrix4f;
/**
 * Created by jrbudda
 */
public class OpenVRStereoRenderer  extends VRRenderer
{
	public OpenVRStereoRenderer(MCVR vr) {
		super(vr);
		this.openvr = (MCOpenVR) vr;
	}

	private HiddenAreaMesh_t[] hiddenMeshes = new HiddenAreaMesh_t[2];
	private MCOpenVR openvr;
	
	public Tuple<Integer, Integer> getRenderTextureSizes()
	{
		if (resolution != null)
			return resolution;

		IntByReference rtx = new IntByReference();
		IntByReference rty = new IntByReference();
		openvr.vrsystem.GetRecommendedRenderTargetSize.apply(rtx, rty);
		resolution = new Tuple<>(rtx.getValue(), rty.getValue());
		System.out.println("OpenVR Render Res " + resolution.getA() + " x " + resolution.getB());
		ss = openvr.getSuperSampling();
		System.out.println("OpenVR Supersampling: " + ss);

		for (int i = 0; i < 2; i++) {
			hiddenMeshes[i] = openvr.vrsystem.GetHiddenAreaMesh.apply(i,0);
			hiddenMeshes[i].read();
			int tc = hiddenMeshes[i].unTriangleCount;
			if(tc >0){
				hiddenMesheVertecies[i] = new float[hiddenMeshes[i].unTriangleCount * 3 * 2];
				Pointer arrptr = new Memory(hiddenMeshes[i].unTriangleCount * 3 * 2);
				hiddenMeshes[i].pVertexData.getPointer().read(0, hiddenMesheVertecies[i], 0, hiddenMesheVertecies[i].length);

				for (int ix = 0;ix < hiddenMesheVertecies[i].length;ix+=2) {
					hiddenMesheVertecies[i][ix] = hiddenMesheVertecies[i][ix] * resolution.getA();
					hiddenMesheVertecies[i][ix + 1] = hiddenMesheVertecies[i][ix +1] * resolution.getB();
				}
				System.out.println("Stencil mesh loaded for eye " + i);
			} else {
				System.out.println("No stencil mesh found for eye " + i);
			}
		}

		return resolution;
	}

	public Matrix4f getProjectionMatrix(int eyeType,float nearClip,float farClip)
	{
		if ( eyeType == 0 )
		{
			HmdMatrix44_t mat = openvr.vrsystem.GetProjectionMatrix.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left, nearClip, farClip);
			//	vr.texType0.depth.mProjection = mat;
			//	vr.texType0.depth.write();
			return Utils.Matrix4fFromOpenVR(mat);
		}else{
			HmdMatrix44_t mat = openvr.vrsystem.GetProjectionMatrix.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right, nearClip, farClip);
			//	vr.texType1.depth.mProjection = mat;
			//	vr.texType1.depth.write();
			return Utils.Matrix4fFromOpenVR(mat);
		}
	}

	public String getLastError() { return ""; }
	
	public void createRenderTexture(int lwidth, int lheight)
	{	
		// generate left eye texture
		LeftEyeTextureId = GL11.glGenTextures();
		int boundTextureId = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, LeftEyeTextureId);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, lwidth, lheight, 0, GL11.GL_RGBA, GL11.GL_INT, (java.nio.ByteBuffer) null);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, boundTextureId);

		openvr.texType0.handle= Pointer.createConstant(LeftEyeTextureId);
		openvr.texType0.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
		openvr.texType0.eType = JOpenVRLibrary.ETextureType.ETextureType_TextureType_OpenGL;
		openvr.texType0.write();
		
		// generate right eye texture
		RightEyeTextureId = GL11.glGenTextures();
		boundTextureId = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, RightEyeTextureId);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, lwidth, lheight, 0, GL11.GL_RGBA, GL11.GL_INT, (java.nio.ByteBuffer) null);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, boundTextureId);

		openvr.texType1.handle=Pointer.createConstant(RightEyeTextureId);
		openvr.texType1.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
		openvr.texType1.eType = JOpenVRLibrary.ETextureType.ETextureType_TextureType_OpenGL;
		openvr.texType1.write();

	}

	public boolean endFrame(RenderPass eye)
	{
		return true;
	}

	public void endFrame() throws RenderConfigException {

		if(openvr.vrCompositor.Submit == null) return;
		
		int lret = openvr.vrCompositor.Submit.apply(
				JOpenVRLibrary.EVREye.EVREye_Eye_Left,
				openvr.texType0, null,
				JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);

		int rret = openvr.vrCompositor.Submit.apply(
				JOpenVRLibrary.EVREye.EVREye_Eye_Right,
				openvr.texType1, null,
				JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);


		openvr.vrCompositor.PostPresentHandoff.apply();
		
		if(lret + rret > 0){
			throw new RenderConfigException("Compositor Error","Texture submission error: Left/Right " + getCompostiorError(lret) + "/" + getCompostiorError(rret));		
		}
	}
	
	public static String getCompostiorError(int code){
		switch (code){
		case EVRCompositorError.EVRCompositorError_VRCompositorError_DoNotHaveFocus:
			return "DoesNotHaveFocus";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_IncompatibleVersion:
			return "IncompatibleVersion";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_IndexOutOfRange:
			return "IndexOutOfRange";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_InvalidTexture:
			return "InvalidTexture";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_IsNotSceneApplication:
			return "IsNotSceneApplication";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_RequestFailed:
			return "RequestFailed";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_SharedTexturesNotSupported:
			return "SharedTexturesNotSupported";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_TextureIsOnWrongDevice:
			return "TextureIsOnWrongDevice";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_TextureUsesUnsupportedFormat:
			return "TextureUsesUnsupportedFormat:";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_None:
			return "None:";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_AlreadySubmitted:
			return "AlreadySubmitted:";
		}
		return "Unknown";
	}

	public boolean providesStencilMask() {
		return true;
	}

	public float[] getStencilMask(RenderPass eye) {
		if(hiddenMesheVertecies == null || (eye != RenderPass.LEFT && eye != RenderPass.RIGHT)) return null;
		return eye == RenderPass.LEFT ? hiddenMesheVertecies[0] : hiddenMesheVertecies[1];
	}

	public String getName() {
		return "OpenVR";
	}

	public boolean isInitialized() {
		return vr.initSuccess;
	}

	public String getinitError() {
		return vr.initStatus;
	}
	
}
