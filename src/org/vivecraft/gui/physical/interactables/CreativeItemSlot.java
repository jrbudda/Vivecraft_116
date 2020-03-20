package org.vivecraft.gui.physical.interactables;

import net.minecraft.item.ItemGroup;
import org.vivecraft.gui.physical.PhysicalItemSlotGui;
import org.vivecraft.gui.physical.WindowCoordinator;
import net.minecraft.item.ItemStack;

public class CreativeItemSlot extends PhysicalItemSlot{
	ItemStack prefabItem;

	public CreativeItemSlot(PhysicalItemSlotGui gui, ItemStack item, int fakeSlotnum){
		super(gui, fakeSlotnum);
		this.prefabItem =item;
	}

	@Override
	public ItemStack getDisplayedItem() {
		return prefabItem;
	}

	@Override
	public void click(int button) {
		ItemStack item=prefabItem.copy();
		item.setCount(button==0 ? item.getMaxStackSize() : 1);
		if(mc.physicalGuiManager.getVirtualHeldItem().isEmpty()){
			//mc.physicalGuiManager.playerInventory.setTabOverride(true);
			mc.physicalGuiManager.playerInventory.setSelectedTab(ItemGroup.INVENTORY);
			mc.physicalGuiManager.playerInventory.refreshButtonStates();
		}
		mc.physicalGuiManager.windowCoordinator.enqueueOperation(new WindowCoordinator.FabricateItemOperation(item));
	}
}
