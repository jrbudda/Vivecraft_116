package org.vivecraft.gameplay.trackers;

import org.vivecraft.gameplay.OpenVRPlayer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;

/**
 * register in {@link OpenVRPlayer}
 * */
public abstract class Tracker {
	public Minecraft mc;
	public Tracker(Minecraft mc){
		this.mc=mc;
	}

	public abstract boolean isActive(ClientPlayerEntity player);
	public abstract void doProcess(ClientPlayerEntity player);
	public void reset(ClientPlayerEntity player){}
	public void idleTick(ClientPlayerEntity player){}

	public EntryPoint getEntryPoint(){return EntryPoint.LIVING_UPDATE;}

	public enum EntryPoint{
		LIVING_UPDATE, SPECIAL_ITEMS
	}
}
