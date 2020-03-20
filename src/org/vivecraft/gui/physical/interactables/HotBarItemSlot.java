package org.vivecraft.gui.physical.interactables;

import org.vivecraft.gui.physical.PhysicalInventory;
import org.vivecraft.gui.physical.WindowCoordinator;
import org.vivecraft.utils.Quaternion;

import net.minecraft.inventory.container.ClickType;
import net.minecraft.util.math.Vec3d;

public class HotBarItemSlot extends PhysicalItemSlot {
	PhysicalInventory.Hotbar gui;
	public HotBarItemSlot(PhysicalInventory.Hotbar gui, int slotId) {
		super(gui,slotId);
		this.gui=gui;
	}

	@Override
	public void touch() {
		if(gui.parent.isOpen()) {
			super.touch();
		}else{
			//mc.physicalGuiManager.setHideItemTouchingSlotOverride(slot.getStack());
			popOut=true;
		}
	}

	@Override
	public Vec3d getAnchorPos(double partialTicks) {
		return gui.getAnchorPos(partialTicks);
	}

	@Override
	public Quaternion getAnchorRotation(double partialTicks) {
		return gui.getAnchorRotation(partialTicks);
	}

	@Override
	public void click(int button) {
		int offset=gui.parent.metaData.hotbarOffset;

		if(gui.parent.isOpen()){
			super.click(button);
		}else{
			for (Interactable inter: gui.interactables){
				if(inter instanceof PhysicalItemSlot) {
					PhysicalItemSlot slot=(PhysicalItemSlot)inter;
					slot.opacity = 1;
				}
			}

			if (mc.player.inventory.currentItem==slotId-offset){
				if(mc.physicalGuiManager.isHoldingHotbarSlot){
					mc.physicalGuiManager.isHoldingHotbarSlot=false;
				}else{
					mc.physicalGuiManager.isHoldingHotbarSlot=true;
					opacity=0.1;
				}
			}else{
				mc.physicalGuiManager.isHoldingHotbarSlot=true;
				opacity=0.1;
			}


			mc.player.inventory.currentItem=slotId-offset;

		}
	}

	@Override
	public void onDragDrop(Interactable source) {
		if(source instanceof HotBarItemSlot){
			HotBarItemSlot sourceSlot=(HotBarItemSlot) source;
			int target=slotId-gui.metaData.hotbarOffset;

			mc.physicalGuiManager.windowCoordinator.enqueueOperation(
					new WindowCoordinator.ClickOperation(mc.physicalGuiManager,sourceSlot.slotId,ClickType.SWAP,true,target));
		}
	}
}