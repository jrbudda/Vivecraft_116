package org.vivecraft.gui.physical;

import org.vivecraft.utils.Semaphore;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CCloseWindowPacket;
import net.minecraft.network.play.client.CCreativeInventoryActionPacket;

import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;

/**
 * Coordinator class that makes sure sequential Window Operations such as clicking are not affected by server lag
 * and are guaranteed to run in the correct order.
 * */
public class WindowCoordinator extends Thread implements PhysicalGuiManager.WindowReceivedListener{
	PhysicalGuiManager guiManager;
	Minecraft mc;

	public WindowCoordinator(PhysicalGuiManager mgr){
		super();
		this.guiManager=mgr;
		this.mc=mgr.mc;
	}

	Queue<WindowOperation> operationQueue=new LinkedTransferQueue<>();

	@Override
	public void run() {
		while (true){
			try {
				waitForQueuePopulated();
				WindowOperation operation = operationQueue.peek();
				operation.execute();
				operationQueue.poll();
				synchronized (emptySemaphore){
					if (operationQueue.isEmpty())
						emptySemaphore.notifyAll();
				}
			}catch (Exception e){
				e.printStackTrace();
			}
		}
	}

	public void enqueueOperation(WindowOperation operation){
		operationQueue.add(operation);
		synchronized (populatedSemaphore){
			populatedSemaphore.notifyAll();
		}
	}

	private final Object populatedSemaphore=new Object();
	public void waitForQueuePopulated(){
		if(!operationQueue.isEmpty())
			return;
		synchronized (populatedSemaphore){
			try {
				populatedSemaphore.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private final Object emptySemaphore=new Object();
	public void waitForQueueEmpty(){
		if(operationQueue.isEmpty())
			return;
		synchronized (emptySemaphore){
			try {
				emptySemaphore.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onWindowReceived() {
		WindowOperation current=operationQueue.peek();
		if(current!=null && current instanceof PhysicalGuiManager.WindowReceivedListener){
			((PhysicalGuiManager.WindowReceivedListener)current).onWindowReceived();
		}
	}

	public abstract static class WindowOperation{
		Minecraft mc=Minecraft.getInstance();
		public abstract void execute();
	}

	public static class ClickOperation extends WindowOperation{
		int slotId, mouseButton;
		PhysicalGuiManager guiManager;
		ClickType type=ClickType.PICKUP;
		boolean raw = false;

		public ClickOperation(PhysicalGuiManager guiManager,int slotId, ClickType type, boolean raw, int mouseButton){
			this(guiManager,slotId,mouseButton);
			this.type=type;
			this.raw=raw;
		}
		public ClickOperation(PhysicalGuiManager guiManager,int slotId, int mouseButton){
			this.slotId=slotId;
			this.mouseButton=mouseButton;
			this.guiManager=guiManager;
		}
		
		@Override
		public String toString() {
			return "ClickOperation{" +
					"slotId=" + slotId +
					", mouseButton=" + mouseButton +
					", guiManager=" + guiManager +
					", type=" + type +
					", raw=" + raw +
					'}';
		}
		
		@Override
		public void execute() {
			Container container = mc.player.openContainer;

			int hotbarslot = mc.player.inventory.currentItem;
			PhysicalGui.InventoryMetaData metaData = PhysicalGui.analyseInventory(container);
			int offset = metaData.hotbarOffset;

			if (!raw && guiManager.isHoldingHotbarSlot && !mc.player.inventory.getCurrentItem().isEmpty()) {
				if (slotId == offset + hotbarslot) {
					guiManager.isHoldingHotbarSlot=false;
					return;
				}

				mc.player.windowClickSynced(container.windowId, hotbarslot + offset, 0, ClickType.PICKUP, 1000);
				mc.player.windowClickSynced(container.windowId, slotId, mouseButton, type, 1000);
				guiManager.isHoldingHotbarSlot = false;
			} else {
				mc.player.windowClickSynced(container.windowId, slotId, mouseButton, type, 1000);
			}
		}
	}

	/**
	 * Creates an item in Creative Mode
	 * */
	public static class FabricateItemOperation extends WindowOperation{
		ItemStack item;
		public FabricateItemOperation(ItemStack item){
			this.item=item;
		}
		@Override
		public void execute() {
			PhysicalGui.InventoryMetaData metaData=PhysicalGui.analyseInventory(mc.player.openContainer);
			if(mc.physicalGuiManager.isHoldingHotbarSlot && !mc.player.inventory.getCurrentItem().isEmpty()){
				//we are fake-holding an item. Delete it.
				mc.physicalGuiManager.isHoldingHotbarSlot=false;
				mc.player.connection.sendPacket(
						new CCreativeInventoryActionPacket(36+mc.player.inventory.currentItem,ItemStack.EMPTY));
				return;
			}
			ItemStack itemToMake;
			if(!mc.player.inventory.getItemStack().isEmpty())
				itemToMake=ItemStack.EMPTY;
			else
				itemToMake=item;

			//Create the item in inventory, then pick it up
			mc.physicalGuiManager.isHoldingHotbarSlot=false;
			ItemStack tmp=mc.player.openContainer.getSlot(metaData.inventoryOffset).getStack().copy();
			mc.player.connection.sendPacket(
					new CCreativeInventoryActionPacket(9,itemToMake));
			mc.player.windowClickSynced(mc.player.openContainer.windowId, metaData.inventoryOffset, 0, ClickType.PICKUP, 1000);
			mc.player.connection.sendPacket(
					new CCreativeInventoryActionPacket(9,tmp));

		}
	}

	public static class CloseWindowOperation extends WindowOperation{
		@Override
		public void execute() {
			mc.player.connection.sendPacket(new CCloseWindowPacket(mc.player.openContainer.windowId));
			mc.player.inventory.setItemStack(ItemStack.EMPTY);
			mc.player.openContainer=mc.player.container;
			try {
				Thread.sleep(10); //Shouldn't be necessary, but doesn't hurt to wait a little
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static class OpenWindowOperation extends WindowOperation implements PhysicalGuiManager.WindowReceivedListener{
		PhysicalGui gui;
		PhysicalGuiManager manager;
		public OpenWindowOperation(PhysicalGuiManager manager,PhysicalGui gui){
			this.gui=gui;
			this.manager=manager;
		}

		final Semaphore windowRecievedSemaphore=new Semaphore(5000);
		@Override
		public void onWindowReceived() {
			windowRecievedSemaphore.wakeUp();
		}

		@Override
		public void execute() {
			windowRecievedSemaphore.reactivate();
			boolean success=gui.requestOpen();
			if (!success)
				return;
			manager.activeGui = gui;
			manager.requestWindowIntercept();
			windowRecievedSemaphore.waitFor();

		}
	}

	public static class ClearInventoryOperation extends WindowOperation{
		@Override
		public void execute() {
			for (int i = 1; i <= 45; i++) {
				mc.player.connection.sendPacket(
						new CCreativeInventoryActionPacket(i,ItemStack.EMPTY));
			}

		}
	}

	public static class FakeHoldOperation extends WindowOperation{
		boolean holding;
		public FakeHoldOperation(boolean holding){
			this.holding=holding;
		}
		@Override
		public void execute() {
			mc.physicalGuiManager.isHoldingHotbarSlot=holding;
		}
	}

	/**
	 * Finds (count) free slots in the player inventory and returns their absolute slot ids as an array
	 * If a free slot is not found that array entry will be -1
	 * */
	public static int[] getFreeSlotsInInventory(int count){
		if (count==0)
			return new int[0];
		Container container=Minecraft.getInstance().player.openContainer;
		PhysicalGui.InventoryMetaData metaData= PhysicalGui.analyseInventory(container);
		int offset=metaData.inventoryOffset;

		int[] targets=new int[count];
		//init array
		for (int i = 0; i < targets.length; i++) {
			targets[i]=-1;
		}
		int index=0;

		for (int i = 0; i < 36; i++) {
			ItemStack item=container.inventorySlots.get(i+offset).getStack();
			if(item.isEmpty()){
				if(index<count){
					targets[index]=i+offset;
					index++;
				}else{
					break;
				}
			}
		}
		return targets;
	}

}
