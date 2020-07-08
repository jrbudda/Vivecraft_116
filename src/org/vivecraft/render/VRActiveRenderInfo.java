package org.vivecraft.render;

import org.vivecraft.api.VRData.VRDevicePose;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.vector.Vec3f;
import net.minecraft.client.shader.Shader;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vec3d;
import net.minecraft.world.IBlockReader;
import net.optifine.shaders.Shaders;

public class VRActiveRenderInfo extends ActiveRenderInfo {

	@Override
	public void update(IBlockReader worldIn, Entity renderViewEntity, boolean thirdPersonIn,
			boolean thirdPersonReverseIn, float partialTicks) {
		this.valid = true;
		this.world = worldIn;
		this.renderViewEntity = renderViewEntity;
		Minecraft mc = Minecraft.getInstance();
		
		RenderPass p = mc.currentPass;
		
		if (Shaders.isShadowPass && p != RenderPass.THIRD)
			p = RenderPass.CENTER;
		
		VRDevicePose src = mc.vrPlayer.vrdata_world_render.getEye(p);	
		this.setPosition(src.getPosition());
			
		// this.setDirection(mc.vrPlayer.vrdata_world_render.hmd.getYaw(),mc.vrPlayer.vrdata_world_render.hmd.getPitch());
		this.pitch = -src.getPitch(); //No, I do not know why this is negative.
		this.yaw = src.getYaw();
	
		//These are used for the soundsystem.
		this.look.set((float)src.getDirection().x,(float)src.getDirection().y, (float)src.getDirection().z);
		Vec3d up = src.getCustomVector(new Vec3d(0, 1, 0));
		this.up.set((float)up.x,(float) up.y,(float) up.z);
		//
		
		//what even are you
		Vec3d left = src.getCustomVector(new Vec3d(1, 0, 0));
		this.left.set((float)up.x,(float) up.y,(float) up.z);

		//This is used for rendering sprites normal to the camera dir, which is terrible and needs to change.
        this.rotation.set(0.0F, 0.0F, 0.0F, 1.0F);
        this.rotation.multiply(Vec3f.YP.rotationDegrees(-yaw));
        this.rotation.multiply(Vec3f.XP.rotationDegrees(pitch));
	}

	@Override
	public void interpolateHeight() {
		// noop
	}

	@Override
	public boolean isThirdPerson() {
		return false;
		// ehhhh  return Minecraft.getInstance().currentPass == RenderPass.THIRD;
	}

}
