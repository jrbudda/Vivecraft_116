package org.vivecraft.gameplay.trackers;

import org.vivecraft.api.VRData;
import org.vivecraft.api.VRData.VRDevicePose;
import org.vivecraft.render.RenderPass;
import org.vivecraft.utils.math.Vector3;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vec3d;

public class TelescopeTracker extends Tracker {
	
	public static final ResourceLocation scopeResource = new ResourceLocation("vivecraft:trashbin");
	public static final ModelResourceLocation scopeModel = new ModelResourceLocation("vivecraft:trashbin");
	
	public TelescopeTracker(Minecraft mc) {
		super(mc);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean isActive(ClientPlayerEntity player) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void doProcess(ClientPlayerEntity player) {
		// TODO Auto-generated method stub

	}
	
	public static boolean isTelescope(ItemStack i){
		if(i.isEmpty())return false;
		if(!i.hasDisplayName()) return false;
		if(i.getItem() != Items.ENDER_EYE) return false;
		if(!(i.getTag().getBoolean("Unbreakable"))) return false;
		return i.getDisplayName().getString().equals("Eye of the Farseer");
	}
	
	public static Vec3d getLensOrigin( int controller){
		VRDevicePose con = Minecraft.getInstance().vrPlayer.vrdata_world_pre.getController(controller);
		return con.getPosition().add((getViewVector(controller).scale(-0.2)).add(con.getDirection().scale(0.05f)));
	}
	
	public static Vec3d getViewVector(int controller){
		return Minecraft.getInstance().vrPlayer.vrdata_world_pre.getController(controller).getCustomVector(new Vec3d(0,-1,0));
	}
	
	private final static double lensDistMax = 0.05; // :shrug:
	private final static double lensDistMin = 0.085; // :shrug:

	private final static double lensDotMax = 0.9; // :shrug:
	private final static double lensDotMin = 0.75; // :shrug:
	
	public static boolean isViewing(int controller){	
		return viewPercent(controller) > 0;
	}
	
	public static float viewPercent(int controller){	
		float out = 0;
		for (int e = 0; e < 2; e++) {
			float tmp = viewPercent(controller, e);
			if (tmp > out) out = tmp;
		}
		return out;
	}
	
	private static float viewPercent(int controller, int e){	
		float out = 0;
		if(e == -1) return 0;
		VRDevicePose eye = Minecraft.getInstance().vrPlayer.vrdata_world_pre.getEye(RenderPass.values()[e]);	
		double dist = eye.getPosition().subtract(getLensOrigin(controller)).length();	
		Vec3d look = eye.getDirection();
		double dot = Math.abs(look.dotProduct(getViewVector(controller)));

		double dfact = 0;
		double distfact = 0;
		
		if(dot > lensDotMin) {
			if (dot > lensDotMax)
				dfact = 1;
			else
				dfact = (dot - lensDotMin) / (lensDotMax - lensDotMin);
		}
				
		if (dist < lensDistMin) {
			if(dist < lensDistMax)
				distfact = 1;
			else
				distfact = 1 - (dist - lensDistMax) / (lensDistMin - lensDistMax);
		}
		
		return (float) Math.min(dfact, distfact);
				
	}
}
