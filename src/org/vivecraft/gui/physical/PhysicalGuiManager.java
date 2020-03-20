package org.vivecraft.gui.physical;

import org.vivecraft.gameplay.trackers.Tracker;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.inventory.container.ClickType;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.vivecraft.utils.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;
/**
 * Misc definitions:<br>
 * Held item: item after clicking it in the gui to pick it up.
 * 	Item is in a 'buffer state'. Meaning it is not in any slot after being picked up.<br>
 * Equipped item:
 * 	Item that is selected in the hotbar with the inventory gui closed.
 * */
public class PhysicalGuiManager extends Tracker {
	public ArrayList<PhysicalGui> guisInRange = new ArrayList<>();
	public PhysicalGui activeGui = null;
	public PhysicalInventory playerInventory = null;
	public WindowCoordinator windowCoordinator;

	boolean isGuiOpen;

	ItemStack guiTransitionOverride=null;

	/**
	 * Whether the selected hotbar slot should act like a held item
	 */
	public boolean isHoldingHotbarSlot = true;

	public void setHoldingHotbarSlotSafe(boolean holdingHotbarSlot) {
		windowCoordinator.enqueueOperation(new WindowCoordinator.FakeHoldOperation(holdingHotbarSlot));
	}

	/**
	 * Returns the currently held item, or if there is none, the equipped item
	 * @see PhysicalGuiManager
	 */
	public ItemStack getVirtualHeldItem() {
		ItemStack heldItem=mc.player.inventory.getItemStack();
		if (!heldItem.isEmpty()) {
			return heldItem;
		}
		if (isHoldingHotbarSlot) {
			int hotbarslot = mc.player.inventory.currentItem;
			PhysicalGui.InventoryMetaData metaData = PhysicalGui.analyseInventory(mc.player.openContainer);
			int offset = metaData.hotbarOffset;

			return mc.player.openContainer.inventorySlots.get(offset + hotbarslot).getStack();
		} else {
			return ItemStack.EMPTY;
		}
	}

	/**
	 * Returns the currently held item.
	 * @see PhysicalGuiManager
	 * */
	public ItemStack getRawHeldItem(){
		return mc.player.inventory.getItemStack();
	}


	public PhysicalGuiManager(Minecraft mc) {
		super(mc);
		windowCoordinator=new WindowCoordinator(this);
		registerWindowReceivedListener(windowCoordinator);
		windowCoordinator.start();
	}

	@Override
	public boolean isActive(ClientPlayerEntity player) {
		return false;//mc.vrSettings.physicalGuiEnabled;
	}

	public void init(ClientPlayerEntity player){
		if (playerInventory != null) {
			reset(player);
		}
		playerInventory = new PhysicalInventory(player);
	}

	@Override
	public void reset(ClientPlayerEntity player) {
		guisInRange.clear();
		if(playerInventory!=null)
			playerInventory.close();
		playerInventory=null;
	}

	/**
	 * Called before a click with the held items is performed, so we can put it down in time
	 * */
	public void preClickAction(){
		if(!mc.vrSettings.physicalGuiEnabled)
			return;
		if(activeGui==null && !playerInventory.isOpen())
			return;
		equipHeldItem();
	}

	@Override
	public void doProcess(ClientPlayerEntity player) {
		if(playerInventory == null)
			init(player);

		BlockPos playerPos = player.getPosition();
		Iterator<PhysicalGui> iterator = guisInRange.iterator();

		while (iterator.hasNext()) {
			PhysicalGui gui = iterator.next();
			BlockPos pos = new BlockPos(gui.getAnchorPos());
			boolean inRange = Math.abs(playerPos.getX() - pos.getX()) < 3 &&
					Math.abs(playerPos.getY() - pos.getY()) < 3 &&
					Math.abs(playerPos.getZ() - pos.getZ()) < 3;
			if ((!inRange || !gui.isAlive()) && gui.isOpen()) {
				gui.close();
			}

			if ((!inRange && gui.isFullyClosed()) || !gui.isAlive()) {
				iterator.remove();
			}
		}

		//Scan for nearby physical inventories and activate them
		blockscan:
		for (int x = -2; x < 3; x++) {
			for (int y = -2; y < 3; y++) {
				currentblock:
				for (int z = -2; z < 3; z++) {
					BlockPos blockPos = new BlockPos(x + playerPos.getX(), y + playerPos.getY(), z + playerPos.getZ());
					BlockState blockState = player.world.getBlockState(blockPos);
					Block block = blockState.getBlock();


					if (PhysicalGui.isImplemented(block)) {
						for (PhysicalGui exgui : guisInRange) {
							BlockPos anchor = new BlockPos(exgui.getAnchorPos());

							if (anchor.equals(blockPos)) {
								//gui already exists
								continue currentblock;
							}
						}


						if (!PhysicalGui.isMainPart(player, blockState, blockPos)) {
							continue currentblock;
						}

						PhysicalGui gui = PhysicalGui.createFromBlock(player, block, blockPos);
						if (gui != null) {
							guisInRange.add(gui);
						}
					}

				}
			}
		}


		for (PhysicalGui gui : guisInRange) {
			gui.onUpdate();
		}


		if (playerInventory != null)
			playerInventory.onUpdate();
	}

	long interceptRequestTime = -1;
	int interceptTimeout = 1000;

	public void requestWindowIntercept() {
		interceptRequestTime = Utils.milliTime();
	}

	public boolean isIntercepting() {
		return (Utils.milliTime() - interceptRequestTime < interceptTimeout);
	}


	ArrayList<WindowReceivedListener> windowReceivedListeners=new ArrayList<>();
	public void registerWindowReceivedListener(WindowReceivedListener listener){
		windowReceivedListeners.add(listener);
	}

	public void handleWindow(Object payload) {
		interceptRequestTime = -1;
		if (activeGui != null) {
			activeGui.open(payload);
		}
		for (WindowReceivedListener listener: windowReceivedListeners){
			listener.onWindowReceived();
		}
	}

	public void doRender(double partialTicks) {
		//GlStateManager.disableLighting();
		GlStateManager.enableLighting();
		for (PhysicalGui gui : guisInRange) {
			if (!gui.isFullyClosed()) {
				gui.render(partialTicks);
			}
		}
		if (playerInventory != null) {
			if(!playerInventory.isFullyClosed())
				playerInventory.render(partialTicks);
			playerInventory.hotbar.render(partialTicks);
		}
		//
	}

	boolean inTransition;
	/**
	 * Performs a transition to the given gui, making sure the held item is not dropped in the process.
	 * gui may be null, in which case the active container will be reset to the player inventory
	 * */
	public boolean requestGuiSwitch(PhysicalGui gui){
		if(inTransition|| isIntercepting() || (activeGui!=null && activeGui.equals(gui)))
			return false;
		inTransition=true;

		new Thread(new Runnable() {
			@Override
			public void run() {
				Logger log=Logger.getLogger("inv");
				log.info("Switching from gui "+activeGui+" to "+gui);
				playerInventory.preGuiChange(gui);
				windowCoordinator.waitForQueueEmpty();
				if (activeGui != null) {
					activeGui.close();
					windowCoordinator.waitForQueueEmpty();
				}
				if (gui != null) {
					log.info("Opening new window");
					windowCoordinator.enqueueOperation(new WindowCoordinator.OpenWindowOperation(PhysicalGuiManager.this, gui));
				}else{
					log.info("Closing window");
					mc.physicalGuiManager.windowCoordinator.enqueueOperation(new WindowCoordinator.CloseWindowOperation());
				}
				windowCoordinator.waitForQueueEmpty();
				playerInventory.postGuiChange(gui);
				windowCoordinator.waitForQueueEmpty();
				inTransition=false;
				log.info("Gui switch completed");
			}
		}).start();
		return true;
	}

	/**
	 *
	 * */
	private ItemStack hideItemTouchingSlotOverride;

	public ItemStack getHeldItemOverride() {
		if (hideItemTouchingSlotOverride != null)
			return hideItemTouchingSlotOverride;
		if (guiTransitionOverride!=null)
			return guiTransitionOverride;

		if(isGuiOpen){
			return getVirtualHeldItem();
		}

		if(!isHoldingHotbarSlot)
			return ItemStack.EMPTY;
		return null;
	}

	public ItemStack getOffhandOverride(){
		if(playerInventory==null || !playerInventory.isOpen())
			return null;
		return playerInventory.offhand.getDisplayedItem();
	}

	public void setHideItemTouchingSlotOverride(ItemStack hideItemTouchingSlotOverride) {
		this.hideItemTouchingSlotOverride = hideItemTouchingSlotOverride;
	}


	public void clickSlot(int slotId, int mouseButton, boolean raw, ClickType clickType){
		windowCoordinator.enqueueOperation(new WindowCoordinator.ClickOperation(this,slotId,clickType,raw,mouseButton));
	}
	public void clickSlot(int slotId, int mouseButton) {
		clickSlot(slotId,mouseButton,false,ClickType.PICKUP);
	}

	void equipHeldItem() {
		ItemStack heldItem=getRawHeldItem();
		if (!heldItem.isEmpty()) {
			int miscSlotId=4;
			PhysicalGui.InventoryMetaData metaData = PhysicalGui.analyseInventory(mc.player.openContainer);
			if (metaData == null)
				return;
			ItemStack miscItem=mc.player.openContainer.getSlot(metaData.hotbarOffset+miscSlotId).getStack();

			if(miscItem.isEmpty()){
				//Put into misc itemslot
				mc.physicalGuiManager.clickSlot(metaData.hotbarOffset + miscSlotId, 0, true,ClickType.PICKUP);
				mc.player.inventory.currentItem=miscSlotId;
				setHoldingHotbarSlotSafe(true);
				return;
			}

			for (int i = 0; i < 9; i++) {
				//misc itemslot is full, so lets look for a free hotbar slot
				ItemStack item=mc.player.openContainer.getSlot(metaData.hotbarOffset+i).getStack();
				if (item.isEmpty()){
					mc.physicalGuiManager.clickSlot(metaData.hotbarOffset + i, 0, true,ClickType.PICKUP);
					mc.player.inventory.currentItem=i;
					setHoldingHotbarSlotSafe(true);
					return;
				}
			}

			//hotbar is full, put into misc slot anyways
			mc.physicalGuiManager.clickSlot(metaData.hotbarOffset + miscSlotId, 0, true,ClickType.PICKUP);
			mc.player.inventory.currentItem=miscSlotId;
			setHoldingHotbarSlotSafe(true);

			int[] free= WindowCoordinator.getFreeSlotsInInventory(1);
			if(free[0]!=-1){
				//put item into any free slot
				mc.physicalGuiManager.clickSlot(free[0], 0, true,ClickType.PICKUP);
			}else{
				//completely full, drop item
				mc.physicalGuiManager.clickSlot(-999, 0, true,ClickType.PICKUP);
			}

		}
	}


	void onGuiOpened() {
		int guisOpen = 0;
		for (PhysicalGui gui : guisInRange) {
			if (gui.isOpen())
				guisOpen++;
		}
		if (playerInventory.isOpen())
			guisOpen++;

		if (guisOpen == 1) {
			//first gui opened
			//if(!mc.player.inventory.getCurrentItem().isEmpty())
				//isHoldingHotbarSlot=true;
			isGuiOpen=true;
		}
	}

	void onGuiClosed() {
		int guisOpen = 0;
		for (PhysicalGui gui : guisInRange) {
			if (gui.isOpen())
				guisOpen++;
		}
		if (playerInventory.isOpen())
			guisOpen++;

		if (guisOpen == 0) {
			//last gui closed
			equipHeldItem();
			isGuiOpen=false;
		}
	}

	public void toggleInventoryBag() {
		if(playerInventory.isFullyClosed()){
			playerInventory.showBag();
		}else{
			playerInventory.hideBag();
		}
	}

	public static interface WindowReceivedListener{
		public void onWindowReceived();
	}
}
