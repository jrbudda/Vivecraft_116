package org.vivecraft.gui.physical;

import org.vivecraft.gui.physical.interactables.Interactable;
import org.vivecraft.gui.physical.interactables.PhysicalItemSlot;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.utils.Quaternion;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.*;
import net.minecraft.inventory.container.Slot;
import net.minecraft.inventory.container.WorkbenchContainer;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.vivecraft.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;


public class PhysicalItemSlotGui extends PhysicalGui {
	protected boolean isOpen = false;

	boolean isBlock;
	public Entity entity;
	public BlockPos blockPos;
	public BlockState blockState;

	public InventoryMetaData metaData=null;

	public ArrayList<Interactable> interactables = new ArrayList<>();

	public PhysicalItemSlotGui(BlockPos pos) {
		super();
		isBlock = true;
		this.blockPos = pos;
		blockState = mc.world.getBlockState(pos);
		init();
	}

	public PhysicalItemSlotGui(Entity entity) {
		super();
		isBlock = false;
		this.entity = entity;
		init();
	}

	void init() {
		loadSlots();
		
		/*VRButtonMapping vrPrime=mc.vrSettings.buttonMappings.get(MCOpenVR.keyInteractVRprimary.getKeyDescription());
		vrPrime.registerListener(new VRButtonMapping.KeyListener() {
			@Override
			public boolean onPressed() {
				if(!mc.vrSettings.physicalGuiEnabled)
					return false;
				if (!isOpen)
					return false;
				if (touching != null) {
					clicked=touching;
					touching.click(0);
					return true;
				}

				if (isInRange()) {
					//consume near misses
					return true;
				}
				return false;
			}

			@Override
			public void onUnpressed() {
				if (!isOpen)
					return;
				if (clicked != null) {
					clicked.unclick(0);
					if(touching!=null && !clicked.equals(touching)){
						touching.onDragDrop(clicked);
					}
					clicked=null;
				}
			}
		});
		VRButtonMapping vrSecond=mc.vrSettings.buttonMappings.get(MCOpenVR.keyInteractVRsecondary.getKeyDescription());
		vrSecond.registerListener(new VRButtonMapping.KeyListener() {
			@Override
			public boolean onPressed() {
				if(!mc.vrSettings.physicalGuiEnabled)
					return false;
				if (!isOpen)
					return false;
				if (touching != null) {
					touching.click(1);
					return true;
				}

				if (isInRange()) {
					//consume near misses
					return true;
				}
				return false;
			}

			@Override
			public void onUnpressed() {
			}
		});*/
		
	}

	boolean isInRange(){
		return shortestDist != -1 && shortestDist < 0.5;
	}

	@Override
	public void render(double partialTicks) {


		if (isFullyClosed())
			return;


		PlayerEntity player = Minecraft.getInstance().player;
		Vec3d playerPos = new Vec3d(
				player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks,
				player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks,
				player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks
		);
		
		int depthFun = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);

		for (int renderLayer = 0; renderLayer <= 1; renderLayer++) {
			for (Interactable slot : interactables) {
				if (!slot.isEnabled())
					continue;


				//GlStateManager.popMatrix();
				GlStateManager.pushMatrix();
				//RenderHelper.enableStandardItemLighting();
				GlStateManager.matrixMode(GL11.GL_MODELVIEW);
				//make sure we have the original depth function
				GlStateManager.depthFunc(depthFun);

				Vec3d origin = slot.getAnchorPos(partialTicks);
								
				Quaternion rotation=slot.getAnchorRotation(partialTicks);
				origin = origin.subtract(playerPos);

				GlStateManager.translated(origin.x, origin.y, origin.z);
				Utils.glRotate(rotation);

				Vec3d slotpos = slot.getPosition(partialTicks);
				GlStateManager.translated(slotpos.x, slotpos.y, slotpos.z);
				Utils.glRotate(slot.getRotation(partialTicks));

				slot.render(partialTicks,renderLayer);
				
				GlStateManager.popMatrix();
			}
		}
	}


	@Override
	public void close() {
		if (!isOpen)
			return;
		isOpen = false;
		touching = null;
		shortestDist = -1;
		
		if (mc.physicalGuiManager.activeGui != null && mc.physicalGuiManager.activeGui.equals(this))
			mc.physicalGuiManager.activeGui = null;
		
		mc.physicalGuiManager.requestGuiSwitch(null);
		
		mc.physicalGuiManager.onGuiClosed();
	}

	@Override
	public boolean isFullyClosed() {
		return !isOpen;
	}

	@Override
	public void open(Object payload) {
//		if (payload instanceof IInteractionObject && container==null) {
//			container = new WorkbenchContainer(mc.player.inventory, mc.world, blockPos);
//		}

		mc.player.openContainer = container;
		loadSlots();
		metaData=analyseInventory(container);
		isOpen = true;
		mc.physicalGuiManager.onGuiOpened();
		//mc.physicalGuiManager.playerInventory.postGuiChange(this);
	}


	@Override
	public Vec3d getAnchorPos(double partialTicks) {
		if (isBlock) {
			return new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
		} else {
			Vec3d prev=new Vec3d(entity.prevPosX,entity.prevPosY,entity.prevPosZ);
			return prev.add((entity.getPositionVector().subtract(prev)).scale(partialTicks));
		}
	}

	@Override
	public Quaternion getAnchorRotation(double partialTicks) {
		if (isBlock) {
			return getBlockOrientation(blockPos);
		} else {
			return new Quaternion(0, (float)-(entity.prevRotationYaw+ partialTicks*( entity.rotationYaw-entity.prevRotationYaw)), 0);
		}
	}

	@Override
	public boolean requestOpen() {
		boolean success;
		if (isBlock) {
			success = (mc.playerController.processRightClickBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockRayTraceResult(Vec3d.ZERO, Direction.UP, blockPos, false))
					== ActionResultType.SUCCESS);
		} else {
			success = mc.playerController.interactWithEntity(mc.player, entity, Hand.MAIN_HAND) == ActionResultType.SUCCESS;
		}
		return success;
	}

	public void tryOpenWindow() {
		if (mc.player.isSneaking())
			return;
		mc.physicalGuiManager.requestGuiSwitch(this);

	}

	void loadSlots() {
		if(blockState==null)
			return;
		String blockid = getBlockId(blockState.getBlock());
		interactables.clear();

		if (blockid.equals("minecraft:crafting_table")) {
			for (int i = 0; i < 3; i++) {
				for (int j = 1; j <= 3; j++) {
					int slotnum = i * 3 + j;

					PhysicalItemSlot craft = new PhysicalItemSlot(this,slotnum);
					craft.position = new Vec3d((2 - j) * 0.2, 0.5, (1 - i) * 0.2);
					craft.rotation = new Quaternion(90, 0, 0);
					if (container != null)
						craft.slot = container.inventorySlots.get(slotnum);
					interactables.add(craft);
				}
			}
			PhysicalItemSlot output = new PhysicalItemSlot(this,0);
			output.position = new Vec3d(0, 1, 0);
			output.fullBlockRotation = new Quaternion();
			output.preview=false;
			if (container != null)
				output.slot = container.inventorySlots.get(0);
			interactables.add(output);
		}
	}

	void reloadSlots() {
		HashMap<Integer, Slot> mcslots = new HashMap<>();
		for (Interactable inter : interactables) {
			if(inter instanceof PhysicalItemSlot) {
				PhysicalItemSlot slot=(PhysicalItemSlot) inter;
				mcslots.put(slot.slotId, slot.slot);
			}
		}
		loadSlots();
		for (Interactable inter : interactables) {
			if(inter instanceof PhysicalItemSlot) {
				PhysicalItemSlot slot = (PhysicalItemSlot) inter;
				Slot s = mcslots.get(slot.slotId);
				if (s != null)
					slot.slot = s;
			}
		}
	}

	/**
	 * Called when the inventory may close, meaning there are no slots with items and it is not touched
	 * */
	public void onMayClose(){
		if (blockState!=null) {
			String blockid = getBlockId(blockState.getBlock());
			if (blockid.equals("minecraft:crafting_table")) {
				close();
			}
		}
	}

	@Override
	public boolean isOpen() {
		return isOpen;
	}

	@Override
	public boolean isAlive() {
		if (isBlock) {
			return mc.world.getBlockState(blockPos).getBlock().equals(blockState.getBlock());
		} else {
			return entity.isAlive();
		}
	}

	double shortestDist = -1;
	public double touchDistance = 0.25;
	double openDistance = 0.4;

	@Override
	public void onUpdate() {

		//if (isOpen && !mc.player.openContainer.equals(container)) {
		//	close();
		//	return;
		//}


		int mainhand = (mc.gameSettings.mainHand == HandSide.RIGHT) ? 0 : 1;
		Vec3d handPos = mc.vrPlayer.vrdata_world_pre.getController(mainhand).getPosition();
		handPos = handPos.add(mc.vrPlayer.vrdata_world_pre.getController(mainhand).getDirection().scale(0.1));


		if (touching != null && !interactables.contains(touching)) {
			touching.untouch();
			touching =null;
		}

		ArrayList<Interactable> touchingSlots=new ArrayList<>();
		for (Interactable slot : interactables) {
			if (!slot.isTouchable())
				continue;
			Vec3d relHand=slot.getAnchorRotation(0).inverse().multiply(handPos.subtract(slot.getAnchorPos(0)));
			relHand=slot.getRotation(0).inverse().multiply(relHand.subtract(slot.getPosition(0)));
			if(slot.getBoundingBox().contains(relHand))
				touchingSlots.add(slot);
		}

		shortestDist = -1;
		Interactable closestSlot=null;
		double currentSlotDistance=-1;

		for (Interactable slot : touchingSlots) {
			Vec3d basePos = slot.getAnchorPos(0);
			Quaternion rot=slot.getAnchorRotation(0);
			Vec3d absSlotPos=basePos.add(rot.multiply(slot.getPosition(0)));

			double dist = absSlotPos.subtract(handPos).length();
			if (shortestDist == -1 || shortestDist > dist) {
				shortestDist = dist;
				closestSlot=slot;
			}
			if(slot.equals(touching)){
				currentSlotDistance=dist;
			}

		}

			if (isOpen() && touching !=null && (currentSlotDistance == -1 || currentSlotDistance-shortestDist > 0.01)) {
				touching.untouch();
				touching =null;
			}
			if (!isOpen && !mc.physicalGuiManager.isIntercepting() && closestSlot!=null && shortestDist < openDistance) {
				tryOpenWindow();
			}else if(isOpen && (closestSlot==null || shortestDist > openDistance+0.05)){
				boolean isEmpty=true;
				for (Interactable in: interactables){
					if(in instanceof PhysicalItemSlot){
						PhysicalItemSlot slot=(PhysicalItemSlot) in;
						if (!slot.getDisplayedItem().isEmpty()){
							isEmpty=false;
						}
					}
				}
				if (isEmpty){
					onMayClose();
				}
			}

			if (isOpen() && closestSlot!=null && shortestDist != -1 && touching == null) {
				touching =closestSlot;
				closestSlot.touch();
			}

	}




}
