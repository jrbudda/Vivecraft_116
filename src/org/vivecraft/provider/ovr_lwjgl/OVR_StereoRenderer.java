package org.vivecraft.provider.ovr_lwjgl;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.ovr.OVR;
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
	
	private OVRFovPort fovPort;
	private OVRSizei bufferSize;
	private OVRMatrix4f projL;
	private OVRMatrix4f projR;
	
	private MC_OVR vr;
	
	public OVR_StereoRenderer(MCVR vr) {
		super(vr);
		this.vr = (MC_OVR) vr;
		fovPort.malloc();
		projL.malloc();
		projR.malloc();
	}

	@Override
	public void createRenderTexture(int lwidth, int lheight) {
		vr.textureSwapChainL = PointerBuffer.allocateDirect(1);
		vr.textureSwapChainR = PointerBuffer.allocateDirect(1);
		
		OVRTextureSwapChainDesc desc = OVRTextureSwapChainDesc.calloc();
		desc.set(OVR.ovrTexture_2D, OVR.OVR_FORMAT_R8G8B8A8_UNORM_SRGB, 1, bufferSize.w(), bufferSize.h(), 1, 1, false, desc.MiscFlags(), desc.BindFlags());
		OVRGL.ovr_CreateTextureSwapChainGL(vr.session.get(0), desc, vr.textureSwapChainL);
		
		IntBuffer chainTexId = BufferUtils.createIntBuffer(1); 
		
		OVRGL.ovr_GetTextureSwapChainBufferGL(vr.session.get(0), vr.textureSwapChainL.get(0), 0, chainTexId);
		LeftEyeTextureId = chainTexId.get();
		OVRGL.ovr_GetTextureSwapChainBufferGL(vr.session.get(0), vr.textureSwapChainR.get(0), 0, chainTexId);
		RightEyeTextureId = chainTexId.get();
		
		vr.layer = OVRLayerEyeFov.malloc();
		
		vr.layer.Header().Type(OVR.ovrLayerType_EyeFov);
		vr.layer.Header().Flags(0);
		vr.layer.ColorTexture(0, vr.textureSwapChainL.get(0));
		vr.layer.ColorTexture(1, vr.textureSwapChainR.get(0));
		vr.layer.Fov(0, vr.eyeRenderDesc0.Fov());
		vr.layer.Fov(1, vr.eyeRenderDesc1.Fov());	
		vr.layer.Viewport(0, createRecti(0, 0, bufferSize.w(), bufferSize.h()));
		vr.layer.Viewport(1, createRecti(0, 0, bufferSize.w(), bufferSize.h()));
		vr.layer.RenderPose(0, vr.hmdToEyeViewPose.get(0));		
		vr.layer.RenderPose(1, vr.hmdToEyeViewPose.get(1));	
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
			OVRUtil.ovrMatrix4f_Projection(vr.hmdDesc.DefaultEyeFov(0), nearClip, farClip, 0, projL);
			return OVRUtils.ovrMatrix4ToMatrix4f(projL).toMCMatrix();
		}	
		else {
			OVRUtil.ovrMatrix4f_Projection(vr.hmdDesc.DefaultEyeFov(1), nearClip, farClip, 0, projR);
			return OVRUtils.ovrMatrix4ToMatrix4f(projR).toMCMatrix();
		}
	}

	@Override
	public void endFrame() throws RenderConfigException {
		OVR.ovr_CommitTextureSwapChain(vr.session.get(0), vr.textureSwapChainL.get(0));
		OVR.ovr_CommitTextureSwapChain(vr.session.get(0), vr.textureSwapChainR.get(0));

		PointerBuffer layerPtrList = BufferUtils.createPointerBuffer(1);
		layerPtrList.put(vr.layer.address());
		layerPtrList.flip();
		
		OVR.ovr_EndFrame(vr.session.get(0), 0, null, layerPtrList);
	}

	@Override
	public boolean providesStencilMask() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Tuple<Integer, Integer> getRenderTextureSizes() {
		OVRSizei recommenedTex0Size = OVRSizei.malloc();
		OVR.ovr_GetFovTextureSize(vr.session.get(0), OVR.ovrEye_Left, fovPort, 1.0f, recommenedTex0Size);
		
		OVRSizei recommenedTex1Size = OVRSizei.malloc();
		OVR.ovr_GetFovTextureSize(vr.session.get(0), OVR.ovrEye_Right,fovPort, 1.0f, recommenedTex1Size);
		
		bufferSize = OVRSizei.malloc();
		int bufferSizeW = Math.max(recommenedTex0Size.w(), recommenedTex1Size.w());
		int bufferSizeH = Math.max(recommenedTex0Size.h(), recommenedTex1Size.h());
		return new Tuple<Integer, Integer>(bufferSizeW, bufferSizeH);
	}

}
