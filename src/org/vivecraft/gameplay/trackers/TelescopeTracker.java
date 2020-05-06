package org.vivecraft.gameplay.trackers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class TelescopeTracker extends Tracker {

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
	
	public static boolean isViewing(ClientPlayerEntity p, int controller){
		//TODO: dont do extra render passes when not looking thru the thing.
		return true;
	}
}
