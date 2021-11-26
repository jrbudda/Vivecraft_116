package org.vivecraft.provider.ovr_lwjgl;

import java.nio.IntBuffer;

import javax.annotation.CheckReturnValue;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.ovr.OVR;
import org.lwjgl.ovr.OVRErrorInfo;
import org.lwjgl.ovr.OVREyeRenderDesc;
import org.lwjgl.ovr.OVRFovPort;
import org.lwjgl.ovr.OVRGL;
import org.lwjgl.ovr.OVRGraphicsLuid;
import org.lwjgl.ovr.OVRHmdDesc;
import org.lwjgl.ovr.OVRLayerEyeFov;
import org.lwjgl.ovr.OVRMatrix4f;
import org.lwjgl.ovr.OVRPosef;
import org.lwjgl.ovr.OVRRecti;
import org.lwjgl.ovr.OVRSizei;
import org.lwjgl.ovr.OVRTextureSwapChainDesc;
import org.lwjgl.ovr.OVRUtil;
import org.lwjgl.ovr.OVRVector2i;
import org.vivecraft.provider.MCVR;
import org.vivecraft.provider.VRRenderer;
import org.vivecraft.render.RenderConfigException;

import net.minecraft.util.Tuple;
import net.minecraft.util.math.vector.Matrix4f;

public class OVR_StereoRenderer extends VRRenderer {
	
	private OVRSizei bufferSize;
	private OVRMatrix4f projL;
	private OVRMatrix4f projR;
	
	private MC_OVR mcovr;
	
	public OVR_StereoRenderer(MCVR vr) {
		super(vr);
		this.mcovr = (MC_OVR) vr;
		projL = OVRMatrix4f.malloc();
		projR = OVRMatrix4f.malloc();
		bufferSize = OVRSizei.malloc();
	}
	private void checkret(int ret)  {
		checkret (ret, "unspecified");
	}
	
	private void checkret(int ret, String desc)  {
		if(ret == 0) return;
		OVRErrorInfo error = OVRErrorInfo.malloc();
		OVR.ovr_GetLastErrorInfo(error);
		System.out.println("Oculus error in " + desc + " " + error.ErrorStringString());
	}
	
	int ret;
	
	@Override
	public void createRenderTexture(int lwidth, int lheight) {
		mcovr.textureSwapChainL = PointerBuffer.allocateDirect(1);
		mcovr.textureSwapChainR = PointerBuffer.allocateDirect(1);
	
		OVRTextureSwapChainDesc desc = OVRTextureSwapChainDesc.calloc();
		desc.set(OVR.ovrTexture_2D, OVR.OVR_FORMAT_R8G8B8A8_UNORM_SRGB, 1, bufferSize.w(), bufferSize.h(), 1, 1, false, desc.MiscFlags(), desc.BindFlags());
		
		checkret(OVRGL.ovr_CreateTextureSwapChainGL(mcovr.session.get(0), desc, mcovr.textureSwapChainL), "create l eye");	
		checkret(OVRGL.ovr_CreateTextureSwapChainGL(mcovr.session.get(0), desc, mcovr.textureSwapChainR), "create r eye");
		
		IntBuffer chainTexId = BufferUtils.createIntBuffer(1); 
		
		checkret(OVRGL.ovr_GetTextureSwapChainBufferGL(mcovr.session.get(0), mcovr.textureSwapChainL.get(0), 0, chainTexId), "create l chain");
		LeftEyeTextureId = chainTexId.get();
		chainTexId.rewind();
		checkret(OVRGL.ovr_GetTextureSwapChainBufferGL(mcovr.session.get(0), mcovr.textureSwapChainR.get(0), 0, chainTexId), "create r chain");
		RightEyeTextureId = chainTexId.get();
			
		mcovr.layer.Header().Type(OVR.ovrLayerType_EyeFov);
		mcovr.layer.Header().Flags(0);
		mcovr.layer.ColorTexture(0, mcovr.textureSwapChainL.get(0));
		mcovr.layer.ColorTexture(1, mcovr.textureSwapChainR.get(0));
		mcovr.layer.Fov(0, mcovr.eyeRenderDesc0.Fov());
		mcovr.layer.Fov(1, mcovr.eyeRenderDesc1.Fov());	
		mcovr.layer.Viewport(0, createRecti(0, 0, bufferSize.w(), bufferSize.h()));
		mcovr.layer.Viewport(1, createRecti(0, 0, bufferSize.w(), bufferSize.h()));
		mcovr.layer.RenderPose(0, mcovr.hmdToEyeViewPose.get(0));		
		mcovr.layer.RenderPose(1, mcovr.hmdToEyeViewPose.get(1));	
	}
	
	private static OVRRecti createRecti(int x, int y, int w, int h) {
		OVRVector2i pos = OVRVector2i.malloc();
		pos.set(x, y);
		OVRSizei size = OVRSizei.malloc();
		size.set(w, h);
		
		OVRRecti recti = OVRRecti.malloc();
		recti.set(pos, size);
		return recti;
	}
	
	@Override
	public Matrix4f getProjectionMatrix(int eyeType, float nearClip, float farClip) {
		if (eyeType == 0) {
			OVRUtil.ovrMatrix4f_Projection(mcovr.hmdDesc.DefaultEyeFov(0), nearClip, farClip, 0, projL);
			return OVRUtils.ovrMatrix4ToMatrix4f(projL).toMCMatrix();
		}	
		else {
			OVRUtil.ovrMatrix4f_Projection(mcovr.hmdDesc.DefaultEyeFov(1), nearClip, farClip, 0, projR);
			return OVRUtils.ovrMatrix4ToMatrix4f(projR).toMCMatrix();
		}
	}

	@Override
	public void endFrame() throws RenderConfigException {
		OVR.ovr_CommitTextureSwapChain(mcovr.session.get(0), mcovr.textureSwapChainL.get(0));
		OVR.ovr_CommitTextureSwapChain(mcovr.session.get(0), mcovr.textureSwapChainR.get(0));

		PointerBuffer layerPtrList = BufferUtils.createPointerBuffer(1);
		layerPtrList.put(mcovr.layer.address());
		layerPtrList.flip();
		
		OVR.ovr_EndFrame(mcovr.session.get(0), 0, null, layerPtrList);
	}

	@Override
	public boolean providesStencilMask() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Tuple<Integer, Integer> getRenderTextureSizes() {
		OVRSizei recommenedTex0Size = OVRSizei.malloc();
		OVR.ovr_GetFovTextureSize(mcovr.session.get(0), OVR.ovrEye_Left, mcovr.hmdDesc.DefaultEyeFov(OVR.ovrEye_Left), 1.0f, recommenedTex0Size);
		
		OVRSizei recommenedTex1Size = OVRSizei.malloc();
		OVR.ovr_GetFovTextureSize(mcovr.session.get(0), OVR.ovrEye_Right,mcovr.hmdDesc.DefaultEyeFov(OVR.ovrEye_Right), 1.0f, recommenedTex1Size);
		
		int bufferSizeW = Math.max(recommenedTex0Size.w(), recommenedTex1Size.w());
		int bufferSizeH = Math.max(recommenedTex0Size.h(), recommenedTex1Size.h());
		bufferSize.w(bufferSizeW);
		bufferSize.h(bufferSizeH);
		return new Tuple<Integer, Integer>(bufferSizeW, bufferSizeH);
	}

}
