package org.vivecraft.gui.physical;

import net.minecraft.item.ItemGroup;
import org.vivecraft.api.VRData;
import org.vivecraft.utils.Convert;
import org.vivecraft.utils.Quaternion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.model.ModelResourceLocation;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.HandSide;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.vivecraft.gui.physical.interactables.*;
import org.vivecraft.utils.Utils;

import java.util.*;
import java.util.logging.Logger;

public class PhysicalInventory extends PhysicalItemSlotGui {
	public Hotbar hotbar;
	ClientPlayerEntity player;
	int layer;
	ModelResourceLocation bagLoc;
	ModelResourceLocation bagLocGold;
	ModelResourceLocation bagLocClosed;
	ModelResourceLocation bagLocGoldClosed;
	double slotOffset;
	boolean fullyClosed = true;
	
	/**
	 * The slotId this item was picked up from
	 */
	int heldItemSlot = -1;
	ArrayList<Interactable> miscInteractables = new ArrayList<>();
	ItemGroup selectedTab = ItemGroup.INVENTORY;
	ArrayList<CreativeTabButton> creativeTabButtons = new ArrayList<>();
	PhysicalItemSlot offhand = null;
	MiniCrafting miniCrafting;
	ArmorDisplay armorDisplay;
	
	
	HashMap<ItemGroup, List<PhysicalItemSlot>> tabs = new HashMap<>();
	
	public PhysicalInventory(ClientPlayerEntity player) {
		super(player);
		this.player = player;
		bagLoc = new ModelResourceLocation("vivecraft:bag", "type=normal");
		bagLocGold = new ModelResourceLocation("vivecraft:bag", "type=gold");
		bagLocClosed = new ModelResourceLocation("vivecraft:bag", "type=closed");
		bagLocGoldClosed = new ModelResourceLocation("vivecraft:bag", "type=gold_closed");
		
		
		hotbar = new Hotbar(this);
		miniCrafting = new MiniCrafting(this);
		armorDisplay = new ArmorDisplay(this);
		container = player.container;
		loadSlots();
		hotbar.open(null);
	}
	
	@Override
	public boolean isFullyClosed() {
		return fullyClosed;
	}
	
	@Override
	public void open(Object payload) {
		if (!player.isCreative())
			selectedTab = ItemGroup.INVENTORY;
		isOpen = true;
		fullyClosed = false;
		loadSlots();
		hotbar.open(null);
		armorDisplay.open(null);
		miniCrafting.setExtended(false);
		mc.physicalGuiManager.onGuiOpened();
	}
	
	@Override
	public void close() {
		isOpen = false;
		armorDisplay.close();
		mc.physicalGuiManager.onGuiClosed();
	}
	
	/**
	 * Make the bag disappear
	 */
	public void hideBag() {
		if (isOpen)
			close();
		fullyClosed = true;
	}
	
	/**
	 * Show the bag, but in a closed state
	 */
	public void showBag() {
		fullyClosed = false;
	}
	
	
	@Override
	void loadSlots() {
		if (container == null)
			return;
		interactables.clear();
		miscInteractables.clear();
		creativeTabButtons.clear();
		
		metaData = analyseInventory(container);
		populateTabs();
		
		if (metaData == null || metaData.inventoryOffset == -1)
			return;
		
		Vec3d anchor = new Vec3d(-0.25, -0.65, 0.2);
		double spanX = 0.5;
		double spanY = 0.4;
		double spanZ = 0.5;
		
		for (Map.Entry<ItemGroup, List<PhysicalItemSlot>> entry : tabs.entrySet()) {
			for (int i = 0; i < entry.getValue().size(); i++) {
				PhysicalItemSlot slot = entry.getValue().get(i);
				
				double spacingX = spanX / 3;
				double spacingY = spanY / 3;
				double spacingZ = spanZ / 3;
				
				int slotIndex;
				if (slot instanceof CreativeItemSlot) {
					slotIndex = i;
				} else {
					slotIndex = slot.slotId - metaData.inventoryOffset;
					slot.slot = container.inventorySlots.get(slotIndex + metaData.inventoryOffset);
				}
				
				int row = slotIndex / 9;
				int y = (slotIndex % 9) / 3;
				int x = (slotIndex % 9) % 3;
				
				slot.position = anchor.add(new Vec3d(-spacingX * x, spacingY * row, -spacingZ * y));
				slot.rotation = new Quaternion(90, 0, 0);
				slot.fullBlockScaleMult = 1.8;
				slot.scale = 0.15;
				slot.opacity = 1;
				if (selectedTab.equals(entry.getKey())) {
					interactables.add(slot);
				}
			}
		}
		
		if (player.isCreative()) {
			Slider slider = new Slider() {
				@Override
				public Quaternion getAnchorRotation(double partialTicks) {
					return PhysicalInventory.this.getAnchorRotation(partialTicks);
				}
				
				@Override
				public Vec3d getAnchorPos(double partialTicks) {
					return PhysicalInventory.this.getAnchorPos(partialTicks);
				}
			};
			slider.position = new Vec3d(-0.712, 0.02, 0);
			slider.registerScrollListener(new Slider.ScrollListener() {
				@Override
				public void onScroll(double perc) {
					scrollTo(perc);
				}
			});
			
			miscInteractables.add(slider);
			
			Trashbin bin = new Trashbin(this);
			bin.position = new Vec3d(-0.1, 0.03, 0.3);
			bin.rotation = new Quaternion(90, 0, 0);
			bin.scale = 0.1;
			
			miscInteractables.add(bin);
		}
		
		Button armorButton = new Button(new ItemStack(Items.CHAINMAIL_CHESTPLATE)) {
			@Override
			public Vec3d getAnchorPos(double partialTicks) {
				return PhysicalInventory.this.getAnchorPos(partialTicks);
			}
			
			@Override
			public Quaternion getAnchorRotation(double partialTicks) {
				return PhysicalInventory.this.getAnchorRotation(partialTicks);
			}
			
			@Override
			public void click(int button) {
				super.click(button);
				if (isDown) {
					requestFatInventory();
					armorDisplay.setArmorMode(true);
				} else {
					armorDisplay.setArmorMode(false);
				}
			}
		};
		armorButton.toggle = true;
		armorButton.rotation = new Quaternion(-90, -90, 0);
		armorButton.position = new Vec3d(-0.07, -0.5, 0);
		
		miscInteractables.add(armorButton);
		
		loadTabButtons();
		
		loadFatInvSlots();
		
		interactables.addAll(miscInteractables);
		scrollTo(0);
	}
	
	/**
	 * Load the slots that are only available if no gui is open (offhand, armor, 2x2 crafting)
	 */
	void loadFatInvSlots() {
		if (metaData.hasExtra) {
			offhand = new PhysicalItemSlot(this, metaData.hotbarOffset + 9) {
				@Override
				public void click(int button) {
					requestFatInventory();
					super.click(button);
				}
				
				@Override
				public AxisAlignedBB getBoundingBox() {
					return super.getBoundingBox().shrink(0.1);
				}
				
				@Override
				public void render(double partialTicks, int renderLayer) {
					//invisible, we render via held item override
				}
			};
			offhand.slot = container.getSlot(offhand.slotId);
			offhand.position = new Vec3d(0, 0, 0.1);
			interactables.add(offhand);
			
		}
		miniCrafting.position = new Vec3d(-0.75, -0.1, 0);
		miscInteractables.add(miniCrafting);
		
		miniCrafting.loadSlots();
		miscInteractables.addAll(miniCrafting.getCraftingSlots());
	}
	
	/**
	 * Close all open guis so we can use 2x2 crafting and armor
	 */
	public void requestFatInventory() {
		PhysicalGui activeGui = mc.physicalGuiManager.activeGui;
		if (activeGui != null)
			activeGui.close();
	}
	
	void loadTabButtons() {
		if (!player.isCreative())
			return;
		ArrayList<ItemGroup> cTabs = new ArrayList<>(Arrays.asList(ItemGroup.GROUPS));
		cTabs.removeIf(it -> !tabs.containsKey(it));
		
		int tabcount = cTabs.size();
		
		double spanX = 0.5;
		double spanY = 0.5;
		int rows = (int) Math.ceil(Math.sqrt(tabcount));
		int columnsNorm = (int) (tabcount / Math.sqrt(tabcount));
		int columnsRest = tabcount % columnsNorm;
		
		Vec3d base = new Vec3d(-0.4, -0.9, -0.3);
		
		for (int i = 0; i < cTabs.size(); i++) {
			ItemGroup tab = cTabs.get(i);
			CreativeTabButton button = new CreativeTabButton(PhysicalInventory.this, tab);
			int row = i / columnsNorm;
			int column = i % columnsNorm;
			double offsetY = ((double) (row + 0.5) / rows) * spanY + spanY / 2;
			
			int cColumns;
			if (row == rows - 1 && columnsRest != 0) {
				cColumns = columnsRest;
			} else {
				cColumns = columnsNorm;
			}
			double cSpanX = ((double) cColumns / columnsNorm) * spanX;
			double offsetX = ((double) (column + 0.5) / cColumns) * cSpanX - cSpanX / 2;
			
			
			button.position = base.add(new Vec3d(offsetX, offsetY, 0));
			button.rotation = new Quaternion(-90, 0, 0);
			button.sticky = true;
			creativeTabButtons.add(button);
			refreshButtonStates();
		}
		miscInteractables.addAll(creativeTabButtons);
	}
	
	/**
	 * Makes sure the correct creativetab button is pressed
	 */
	public void refreshButtonStates() {
		for (CreativeTabButton b : creativeTabButtons) {
			b.isDown = (b.tab.equals(selectedTab));
		}
	}
	
	double scrollPos = 0;
	
	public void scrollTo(double perc) {
		scrollPos = perc;
		List<PhysicalItemSlot> slots = tabs.get(selectedTab);
		int totalRows = (int) Math.ceil((double) slots.size() / 9);
		slotOffset = -perc * (totalRows - 3) * (0.4 / 3);
		int activeRow = (int) ((totalRows - 3) * perc);
		
		for (int i = 0; i < slots.size(); i++) {
			int row = i / 9;
			boolean active = (row >= activeRow && row <= activeRow + 2);
			slots.get(i).enabled = active;
		}
	}
	
	void populateTabs() {
		tabs.clear();
		for (ItemGroup ctab : ItemGroup.GROUPS) {
			if (ctab.equals(ItemGroup.HOTBAR))
				continue;
			ArrayList<PhysicalItemSlot> itemSlots = new ArrayList<>();
			if (ctab.equals(ItemGroup.INVENTORY)) {
				for (int i = metaData.inventoryOffset; i < metaData.inventoryOffset + 27; i++) {
					itemSlots.add(new PhysicalItemSlot(this, i) {
						@Override
						public Vec3d getPosition(double partialTicks) {
							return super.getPosition(partialTicks).add(new Vec3d(0, slotOffset, 0));
						}
						
						@Override
						public void click(int button) {
							super.click(button);
							setTabOverride(false);
						}
					});
				}
			} else {
				if (!player.isCreative())
					continue;
				
				NonNullList<ItemStack> items = NonNullList.create();
				ctab.fill(items);
				
				for (int i = 0; i < items.size(); i++) {
					CreativeItemSlot slot = new CreativeItemSlot(this, items.get(i), i + metaData.inventoryOffset) {
						@Override
						public Vec3d getPosition(double partialTicks) {
							return super.getPosition(partialTicks).add(new Vec3d(0, slotOffset, 0));
						}
					};
					itemSlots.add(slot);
				}
			}
			
			tabs.put(ctab, itemSlots);
		}
	}
	
	public void setSelectedTab(ItemGroup selectedTab) {
		setSelectedTab(selectedTab, 0);
	}
	
	public void setSelectedTab(ItemGroup selectedTab, double scroll) {
		this.selectedTab = selectedTab;
		interactables.clear();
		interactables.addAll(miscInteractables);
		interactables.addAll(tabs.get(selectedTab));
		scrollTo(scroll);
		overridingTab = false;
	}
	
	ItemGroup bkgTab;
	double bkgScroll;
	boolean overridingTab = false;
	
	/**
	 * Temporarily overrides the current tab with the inventory tab
	 */
	public void setTabOverride(boolean doOverride) {
		if (!overridingTab && doOverride) {
			bkgTab = selectedTab;
			bkgScroll = scrollPos;
			setSelectedTab(ItemGroup.INVENTORY);
			overridingTab = true;
			
			
		} else if (overridingTab && !doOverride) {
			setSelectedTab(bkgTab, bkgScroll);
		}
		
	}
	
	@Override
	public void tryOpenWindow() {
		//Not necessary
	}
	
	void switchLayer(int layer) {
		this.layer = layer;
		for (Interactable inter : interactables) {
			if (inter instanceof PhysicalItemSlot) {
				PhysicalItemSlot slot = (PhysicalItemSlot) inter;
				int slotIndex = slot.slotId - metaData.inventoryOffset;
				int j = slotIndex / 9;
				if (layer != -1 && j > layer) {
					slot.opacity = 0.1;
				} else {
					slot.opacity = 1;
				}
			}
		}
		/*for(Interactable inter: hotbar.interactables){
			if(inter instanceof PhysicalItemSlot){
				PhysicalItemSlot slot = (PhysicalItemSlot) inter;
				slot.opacity=1;
			}
		}*/
	}
	
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		//reloadSlots();
		//if (mc.player.inventoryContainer!=null && container==null){
		//	container=mc.player.inventoryContainer;
		//	hotbar.open(null);
		//}
		
		
		hotbar.onUpdate();
		armorDisplay.onUpdate();
		for (Interactable inter : interactables) {
			inter.update();
		}
		
		int offhand = mc.gameSettings.mainHand == HandSide.RIGHT ? 1 : 0;
		Vec3d up = mc.vrPlayer.vrdata_world_pre.getController(offhand).getCustomVector(new Vec3d(0, 1, 0));
		if (up.y < 0.2 && isOpen) {
			close();
		}
		if (up.y > 0.3 && !isOpen && !fullyClosed) {
			open(null);
		}
		
		if (transitionTimeStamp != -1 && Utils.milliTime() - transitionTimeStamp > 5000) {
			//Timeout
			mc.physicalGuiManager.guiTransitionOverride = null;
			transitionTimeStamp = -1;
		}
		
		if (isOpen) {
			if (touching != null && touching instanceof PhysicalItemSlot) {
				PhysicalItemSlot slot = (PhysicalItemSlot) touching;
				int slotIndex = slot.slotId - metaData.inventoryOffset;
				int j = slotIndex / 9;
				switchLayer(j);
			} else {
				switchLayer(-1);
			}
		}
	}
	
	long transitionTimeStamp = -1;
	
	public void preGuiChange(PhysicalGui gui) {
		transitionTimeStamp = Utils.milliTime();
		mc.physicalGuiManager.guiTransitionOverride = mc.physicalGuiManager.getVirtualHeldItem();
		metaData = analyseInventory(container);
		putDownHeldItem();
		miniCrafting.setExtended(false);
	}
	
	public void postGuiChange(PhysicalGui gui) {
		transitionTimeStamp = -1;
		if (gui != null && gui.container != null)
			this.container = gui.container;
		else
			this.container=player.container;
		hotbar.open(null);
		loadSlots();
		pickItemBackUp();
		mc.physicalGuiManager.guiTransitionOverride = null;
	}
	
	
	@Override
	public Quaternion getAnchorRotation(double partialTicks) {
		int offhand = mc.gameSettings.mainHand == HandSide.RIGHT ? 1 : 0;
		VRData data = mc.vrPlayer.vrdata_world_render;
		if (data == null)
			data = mc.vrPlayer.vrdata_room_pre;
		Quaternion hand = new Quaternion(data.getController(offhand).getMatrix());
		
		return hand.multiply(new Quaternion(0, 180, 0));
	}
	
	@Override
	public Vec3d getAnchorPos(double partialTicks) {
		int offhand = mc.gameSettings.mainHand == HandSide.RIGHT ? 1 : 0;
		VRData data = mc.vrPlayer.vrdata_world_render;
		if (data == null)
			data = mc.vrPlayer.vrdata_world_pre;
		return data.getController(offhand).getPosition();
	}
	
	/**
	 * The place where we put down the item relative to the inventory offset
	 */
	int putDownItemSlot = -1;
	
	void putDownHeldItem() {
		Logger log= Logger.getLogger("inv");
		putDownItemSlot = -1;
		metaData=analyseInventory(mc.player.openContainer);
		if (mc.physicalGuiManager.getRawHeldItem().isEmpty())
			return;
		
		int offset = metaData.inventoryOffset;
		log.info("Putting down held item: "+mc.physicalGuiManager.getRawHeldItem());
		log.fine("New GUI has inventory offset "+offset);
		for (int i = 0; i < 36; i++) {
			ItemStack item = container.inventorySlots.get(i + offset).getStack();
			if (item.isEmpty()) {
				putDownItemSlot = i;
				log.fine("Found free slot "+putDownItemSlot+". Putting down item.");
				mc.physicalGuiManager.clickSlot(offset + i, 0);
				
				return;
			}
		}
		
		
		//inventory is full. put item back where it came from
		//No warranty if we accidentally drop your diamonds into lava
		putDownItemSlot = Math.min(Math.max(0, heldItemSlot - offset), 35);
		log.warning("Full inventory! Performing item override into slot "+putDownItemSlot );
		
		mc.physicalGuiManager.clickSlot(heldItemSlot, 0);
		mc.physicalGuiManager.clickSlot(-1, 0);
	}
	
	void pickItemBackUp() {
		metaData=analyseInventory(mc.player.openContainer);
		if ( putDownItemSlot == -1)
			return;
		int offset = metaData.inventoryOffset;
		Logger log = Logger.getLogger("inv");
		log.fine("New inventory offset is "+offset);
		if (offset == -1)
			return;
		log.info("Picking item "+mc.player.openContainer.getSlot(offset + putDownItemSlot).getStack().getDisplayName()+" from slot "+putDownItemSlot+" back up.");
		mc.physicalGuiManager.clickSlot(offset + putDownItemSlot, 0);
		putDownItemSlot = -1;
	}
	
	
	@Override
	public void render(double partialTicks) {
		//GlStateManager.popMatrix();
		GlStateManager.pushMatrix();
		
		GlStateManager.enableLighting();
		PlayerEntity player = Minecraft.getInstance().player;
		Vec3d playerPos = new Vec3d(
				player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks,
				player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks,
				player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks
		);
		
		Vec3d origin = getAnchorPos(partialTicks);
		Quaternion rotation = getAnchorRotation(partialTicks);
		
		
		origin = origin.subtract(playerPos);
		
		float scale = 1.0f;
		
		
		ModelResourceLocation loc;
		if (isOpen())
			loc = player.isCreative() ? bagLocGold : bagLoc;
		else
			loc = player.isCreative() ? bagLocGoldClosed : bagLocClosed;
		
		
		
		/*FloatBuffer cMat = ByteBuffer.allocateDirect(16*4).asFloatBuffer();
		FloatBuffer pMat = ByteBuffer.allocateDirect(16*4).asFloatBuffer();
		GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, cMat);
		GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, pMat);
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GlStateManager.loadIdentity();
		if (pass==0) {
			//wsGlStateManager.translate(0, 0, 0.1);
		}
		GlStateManager.multMatrix(pMat);
		GlStateManager.matrixMode(GL11.GL_MODELVIEW);*/
		
		
		GlStateManager.colorMask(false,false,false,false);
		
		//Render inventory in front of everything else
		for (int pass = 0; pass < 2; pass++) {
			
			GlStateManager.pushMatrix();
			
			if(pass == 0){
				//First pass: Render fragments in reverse order to depth buffer
				//GlStateManager.colorMask(true,true,true,true);
				GlStateManager.colorMask(false,false,false,false);
				GlStateManager.depthFunc(GL11.GL_GEQUAL);
			}else{
				//Render normally into hole punched by pass 0
				GlStateManager.colorMask(true,true,true,true);
				GlStateManager.depthFunc(GL11.GL_LEQUAL);
			}
			
			
			
			GlStateManager.translated(origin.x, origin.y, origin.z);
			Utils.glRotate(rotation);
			GlStateManager.translated(-0.9, -0.7, -0.5);
			GlStateManager.scalef(scale, scale, scale);
			
			mc.worldRenderer.renderCustomModel(loc);
			GlStateManager.popMatrix();
			
			GlStateManager.pushMatrix();
			if (isOpen())
				super.render(partialTicks);
			armorDisplay.render(partialTicks);
			GlStateManager.popMatrix();
		}
		
		GlStateManager.depthFunc(GL11.GL_LEQUAL);
		GlStateManager.popMatrix();
	}
	
	public class Hotbar extends PhysicalItemSlotGui {
		//TODO Drag and drop
		public PhysicalInventory parent;
		
		public Hotbar(PhysicalInventory inventory) {
			super(inventory.entity);
			parent = inventory;
		}
		
		@Override
		public void open(Object payload) {
			isOpen = true;
			container = PhysicalInventory.this.container;
			loadSlots();
		}
		
		
		@Override
		public void tryOpenWindow() {
		}
		
		@Override
		void loadSlots() {
			if (container == null)
				return;
			metaData = analyseInventory(container);
			int hotbarOffset = metaData.hotbarOffset;
			
			Vec3d offset = new Vec3d(0, -0.6, -0.1);
			
			interactables.clear();
			
			for (int i = 0; i < 9; i++) {
				PhysicalItemSlot slot;
				
				if (i == 0 || i == 8) {
					//Pocket slots
					int sign = (i == 0) ? 1 : -1; //what side are we on
					slot = new HotBarItemSlot(this, hotbarOffset + i) {
						@Override
						public ItemStack getDisplayedItem() {
							if (PhysicalInventory.this.isOpen())
								return super.getDisplayedItem();
							else
								return slot.getStack();
						}
					};
					
					slot.position = offset.add(new Vec3d(sign * 0.2, 0, 0));
					slot.rotation = new Quaternion(180, -90 * sign, 0);
					slot.scale = 0.4;
					slot.counterRot = new Quaternion(-90, sign * -90, 0);
					slot.fullBlockScaleMult = 0.5;
				} else if (i == 4) {
					//Belt slot
					slot = new HotBarItemSlot(this, hotbarOffset + i) {
						@Override
						public ItemStack getDisplayedItem() {
							if (PhysicalInventory.this.isOpen())
								return super.getDisplayedItem();
							else
								return slot.getStack();
						}
					};
					slot.scale = 0.1;
					slot.rotation = new Quaternion(90, 0, 0);
					slot.position = offset.add(new Vec3d(0, 0, 0.1));
				} else {
					//Wrist slots
					slot = new HotBarItemSlot(this, hotbarOffset + i) {
						@Override
						public Vec3d getAnchorPos(double partialTicks) {
							int offhand = mc.gameSettings.mainHand == HandSide.RIGHT ? 1 : 0;
							VRData data = mc.vrPlayer.vrdata_world_render;
							if (data == null)
								data = mc.vrPlayer.vrdata_world_pre;
							return data.getController(offhand).getPosition();
						}
						
						@Override
						public Quaternion getAnchorRotation(double partialTicks) {
							int offhand = mc.gameSettings.mainHand == HandSide.RIGHT ? 1 : 0;
							VRData data = mc.vrPlayer.vrdata_world_render;
							if (data == null)
								data = mc.vrPlayer.vrdata_world_pre;
							return new Quaternion(data.getController(offhand).getMatrix());
						}
						
						@Override
						public ItemStack getDisplayedItem() {
							if (PhysicalInventory.this.isOpen())
								return super.getDisplayedItem();
							else
								return slot.getStack();
						}
					};
					slot.scale = 0.1;
					slot.rotation = new Quaternion(0, 90, 180);
					int sign = (i < 4) ? 1 : -1;
					int row = (i < 4) ? i : i - 5;
					slot.position = new Vec3d(-0.1, 0.06 * sign, -0.05 + 0.1 * row);
				}
				slot.slot = container.inventorySlots.get(hotbarOffset + i);
				if (mc.physicalGuiManager.isHoldingHotbarSlot && mc.player.inventory.currentItem == i) {
					slot.opacity = 0.1;
				}
				
				//slot.position=offset.add(beltCurve.getPointOnPath(i/8D));
				
				
				interactables.add(slot);
			}
			
			
		}
		
		
		@Override
		boolean isInRange() {
			return touching != null;
		}
		
		@Override
		public Quaternion getAnchorRotation(double partialTicks) {
			VRData data = mc.vrPlayer.vrdata_world_render;
			if (data == null)
				data = mc.vrPlayer.vrdata_world_pre;
			
			return new Quaternion(0,data.getFacingYaw(),0);
		}
		
		@Override
		public Vec3d getAnchorPos(double partialTicks) {
			VRData data = mc.vrPlayer.vrdata_world_render;
			if (data == null)
				data = mc.vrPlayer.vrdata_world_pre;
			//Vec3d prev=new Vec3d(mc.gameRenderer.rveprevX,mc.gameRenderer.rveprevY,mc.gameRenderer.rveprevZ);
			//Vec3d current=new Vec3d(mc.gameRenderer.rveX,mc.gameRenderer.rveY,mc.gameRenderer.rveZ);
			return data.hmd.getPosition();//prev.add((current.subtract(prev)).scale(partialTicks));
		}
		
		@Override
		public void onUpdate() {
			super.onUpdate();
			
			//reloadSlots();
		}
		
		
		@Override
		public void close() {
			isOpen = false;
		}
		
		
	}
	
}
