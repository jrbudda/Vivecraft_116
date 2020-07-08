package org.vivecraft.gui.physical.interactables;

import org.vivecraft.gui.physical.PhysicalInventory;
import org.vivecraft.utils.math.Quaternion;

import net.minecraft.item.ItemGroup;
import net.minecraft.util.math.vector.Vec3d;

public class CreativeTabButton extends Button {
	public PhysicalInventory inventory;
	public ItemGroup tab;
	public CreativeTabButton(PhysicalInventory inventory, ItemGroup tab){
		super(tab.getIcon());
		this.inventory=inventory;
		this.tab=tab;
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
	public void click(int button) {
		super.click(button);
		inventory.setSelectedTab(tab);
		inventory.refreshButtonStates();
	}
}
