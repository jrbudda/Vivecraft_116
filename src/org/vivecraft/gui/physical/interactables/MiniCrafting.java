package org.vivecraft.gui.physical.interactables;

import org.vivecraft.gui.physical.PhysicalInventory;
import org.vivecraft.gui.physical.WindowCoordinator;
import org.vivecraft.utils.Quaternion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.ModelResourceLocation;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class MiniCrafting implements Interactable {
	Minecraft mc;
	ModelResourceLocation craftingLoc;
	boolean extended;
	PhysicalInventory inventory;
	ArrayList<PhysicalItemSlot> craftingSlots=new ArrayList<>();

	public MiniCrafting(PhysicalInventory inventory){
		this.inventory=inventory;
		craftingLoc=new ModelResourceLocation("vivecraft:mini_crafting");
		mc=Minecraft.getInstance();
	}
	@Override
	public void render(double partialTicks, int renderLayer) {
		if(renderLayer==0) {
			GlStateManager.pushMatrix();
			GlStateManager.translated(-0.22 - 0.22, 0, -0.22);
			mc.worldRenderer.renderCustomModel(craftingLoc);
			GlStateManager.popMatrix();
		}
	}

	public void loadSlots(){
		craftingSlots.clear();
		if (!inventory.metaData.hasExtra)
			return;
		for (int x = 0; x < 2; x++) {
			for (int y = 0; y < 2; y++) {
				int slotId=y*2+x+inventory.metaData.craftingOffset +1;
				PhysicalItemSlot slot=new PhysicalItemSlot(inventory,slotId){
					@Override
					public Vec3d getAnchorPos(double partialTicks) {
						Vec3d pos= MiniCrafting.this.getAnchorPos(partialTicks);
						pos=pos.add(MiniCrafting.this.getAnchorRotation(partialTicks).multiply(
								MiniCrafting.this.getPosition(partialTicks)
						));
						return pos;
					}

					@Override
					public Quaternion getAnchorRotation(double partialTicks) {
						return MiniCrafting.this.getAnchorRotation(partialTicks).multiply(
								MiniCrafting.this.getRotation(partialTicks));
					}

					@Override
					public boolean isTouchable() {
						return (!mc.physicalGuiManager.getVirtualHeldItem().isEmpty()
						|| !slot.getStack().isEmpty());
					}
				};
				slot.slot=inventory.container.inventorySlots.get(slotId);
				slot.enabled=false;
				Vec3d anchor=new Vec3d(-0.15,0.12,0.07);
				double spacing=0.15;
				slot.position=anchor.add(new Vec3d(-x*spacing,0,-y*spacing));
				slot.rotation=new Quaternion(90,0,0);
				slot.scale=0.15;
				craftingSlots.add(slot);
			}
		}

		PhysicalItemSlot result=new PhysicalItemSlot(inventory,inventory.metaData.craftingOffset){
			@Override
			public Vec3d getAnchorPos(double partialTicks) {
				Vec3d pos= MiniCrafting.this.getAnchorPos(partialTicks);
				pos=pos.add(MiniCrafting.this.getAnchorRotation(partialTicks).multiply(
						MiniCrafting.this.getPosition(partialTicks)
				));
				return pos;
			}

			@Override
			public Quaternion getAnchorRotation(double partialTicks) {
				return MiniCrafting.this.getAnchorRotation(partialTicks).multiply(
						MiniCrafting.this.getRotation(partialTicks));
			}

			@Override
			public boolean isTouchable() {
				return (!slot.getStack().isEmpty());
			}
		};
		result.slot=inventory.container.inventorySlots.get(result.slotId);
		result.enabled=false;
		result.position=new Vec3d(-0.2,0.4,0);
		result.fullBlockRotation = new Quaternion();
		result.scale=0.15;
		result.preview=false;
		craftingSlots.add(result);
	}

	public Vec3d position=Vec3d.ZERO;
	@Override
	public Vec3d getPosition(double partialTicks) {
		return position;
	}

	public Quaternion rotation=new Quaternion();
	@Override
	public Quaternion getRotation(double partialTicks) {
		if(extended)
			return rotation;
		else{
			return rotation.multiply(new Quaternion(0,0,90));
		}
	}

	@Override
	public Vec3d getAnchorPos(double partialTicks) {
		return inventory.getAnchorPos(partialTicks);
	}

	@Override
	public Quaternion getAnchorRotation(double partialTicks) {
		return inventory.getAnchorRotation(partialTicks);
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void touch() {
	}

	@Override
	public void untouch() {

	}

	public ArrayList<PhysicalItemSlot> getCraftingSlots() {
		return craftingSlots;
	}

	@Override
	public void click(int button) {
		setExtended(!extended);
	}

	public void setExtended(boolean extended){
		if(extended){
			inventory.requestFatInventory();
		}else{
			returnItems();
		}
		this.extended=extended;
		for (PhysicalItemSlot slot : craftingSlots) {
			slot.enabled=extended;
		}
	}

	void returnItems(){
		ArrayList<PhysicalItemSlot> nonEmpty=new ArrayList<>();
		for (PhysicalItemSlot slot : craftingSlots) {
			if (!slot.slot.getStack().isEmpty()){
				nonEmpty.add(slot);
			}
		}

		int[] freeSlots= WindowCoordinator.getFreeSlotsInInventory(nonEmpty.size());
		for (int i = 0; i < freeSlots.length; i++) {
			if(freeSlots[i]==-1){
				//drop the item
				freeSlots[i]=-999;
			}
			mc.physicalGuiManager.windowCoordinator.enqueueOperation(new WindowCoordinator.ClickOperation(
					mc.physicalGuiManager,nonEmpty.get(i).slotId,0
			));
			mc.physicalGuiManager.windowCoordinator.enqueueOperation(new WindowCoordinator.ClickOperation(
					mc.physicalGuiManager,freeSlots[i],0
			));
		}
	}

	@Override
	public boolean isTouchable() {
		if(extended) {
			if(!mc.physicalGuiManager.getVirtualHeldItem().isEmpty())
				return false;
			for (PhysicalItemSlot slot : craftingSlots) {
				if (!slot.slot.getStack().isEmpty())
					return false;
			}
		}
		return isEnabled();
	}

	@Override
	public void unclick(int button) {

	}

	@Override
	public AxisAlignedBB getBoundingBox() {
		return new AxisAlignedBB(-0.22 -0.22,0,-0.22,0.22 -0.22,0.12,0.22).grow(0.05);
	}
}
