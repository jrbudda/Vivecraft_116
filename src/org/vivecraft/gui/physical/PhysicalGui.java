package org.vivecraft.gui.physical;

import net.minecraft.state.IProperty;
import org.vivecraft.gui.physical.interactables.Interactable;
import org.vivecraft.utils.Debug;
import org.vivecraft.utils.Quaternion;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.*;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.CraftingResultSlot;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;

import org.vivecraft.utils.Vector3;

public abstract class PhysicalGui{
	public Container container = null;
	public Minecraft mc;
	public Interactable touching;
	public Interactable clicked;

	public PhysicalGui(){
		this.mc=Minecraft.getInstance();
	}

	public abstract void render(double partialTicks);


	/**
	 * Performs the necessary cleanup and closes the inventory
	 * */
	public abstract void close();

	/**
	 * Performs initialization and opens the gui
	 * payload may be an instance of IInventory, IInteractionObject, IMerchant, ...
	 * */
	public abstract void open(Object payload);

	/**
	 * Sends a request to the server to open a window for this gui
	 * @return whether the request was send successfully
	 * */
	public abstract boolean requestOpen();

	/**
	 * Returns the base position of this gui
	 * */
	public abstract Vec3d getAnchorPos(double partialTicks);

	public abstract Quaternion getAnchorRotation(double partialTicks);

	public final Vec3d getAnchorPos(){
		return getAnchorPos(Minecraft.getInstance().getRenderPartialTicks());
	}

	public final Quaternion getAnchorRotation(){
		return getAnchorRotation(Minecraft.getInstance().getRenderPartialTicks());
	}

	/**
	 * Whether this gui is open and in use
	 * */
	public abstract boolean isOpen();


	/**
	 * Whether this gui is completely closed and should no longer receive rendering calls
	 * */
	public abstract boolean isFullyClosed();

	/**
	 * Whether the object this anchor is attached to is still existing
	 * */
	public abstract boolean isAlive();

	public abstract void onUpdate();

	/**
	 * Returns whether we have a setup available for this blocks
	 * */
	static boolean isImplemented(Block block){
		if(block instanceof ChestBlock || block instanceof EnderChestBlock)
			return true;
		if (block instanceof CraftingTableBlock)
			return true;
		return false;
	}


	static String getBlockId(Block block){
		return Registry.BLOCK.getKey(block).toString();
	}

	static PhysicalGui createFromBlock(PlayerEntity player, Block block, BlockPos pos){
		if(block instanceof CraftingTableBlock){
				return new PhysicalItemSlotGui(pos);
		}else if(block instanceof ChestBlock || block instanceof EnderChestBlock){
				return new PhysicalChest(pos);
		}

		return null;
	}

	static boolean isMainPart(PlayerEntity player, BlockState blockState, BlockPos pos){

		if(blockState.getBlock() instanceof ChestBlock){
			Vec3d posVec=new Vec3d(pos).add(new Vec3d(0.5,0.5,0.5));
			Vec3d left=new Vec3d(1,0,0);
			Quaternion rot=getBlockOrientation(pos);
			BlockPos neighborPos=new BlockPos(posVec.add(rot.multiply(left)));


			BlockState neighborBlock=player.world.getBlockState(neighborPos);
			if((neighborBlock.getBlock().equals(Blocks.CHEST) && blockState.getBlock().equals(Blocks.CHEST)) ||
					(neighborBlock.getBlock().equals(Blocks.TRAPPED_CHEST) && blockState.getBlock().equals(Blocks.TRAPPED_CHEST)) ){
				return false;
			}
		}

		return true;
	}


	/**
	 * Returns the direction of the block based on the way you are looking when facing it
	 */
	public static Quaternion getBlockOrientation(BlockPos pos) {
		BlockState blockState = Minecraft.getInstance().world.getBlockState(pos);
	//	Debug d=new Debug(new Vec3d(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5));
		Vec3d dir;
		if (blockState.getProperties().contains(HorizontalBlock.HORIZONTAL_FACING)) {
			Direction facing = blockState.get(HorizontalBlock.HORIZONTAL_FACING);

			dir = new Vec3d(facing.getDirectionVec()).scale(-1);
			
		} else {
			dir = Minecraft.getInstance().player.getLookVec();

			if (Math.abs(dir.x) > Math.abs(dir.z)) {
				dir = new Vec3d(Math.signum(dir.x), 0, 0);
			} else {
				dir = new Vec3d(0, 0, Math.signum(dir.z));
			}
		}
		
		if (dir.x==0 && dir.y == 0 && dir.z == -1) {
			return new Quaternion(0, 180, 0); //parallel vectors need special case		
		}
		
	//	d.drawVector(Vec3d.ZERO,dir, Color.green);
	//	d.drawVector(Vec3d.ZERO,new Vec3d(0,0,1),Color.red);
		
		return Quaternion.createFromToVector(new Vector3(0, 0, 1), new Vector3(dir));
	}


	public static InventoryMetaData analyseInventory(Container container){
		if (container==null)
			return null;
		int offsetArmor=-1;
		int offsetMain=-1;
		int offsetCrafting=-1;

		int playerInvSlots=0;

		for (int i=0; i<container.inventorySlots.size(); i++){
			Slot slot=container.inventorySlots.get(i);
			if(slot instanceof CraftingResultSlot ){
				if(offsetCrafting==-1)
					offsetCrafting=i;
			}

			if(slot.inventory instanceof PlayerInventory){
				if(offsetMain==-1)
					offsetMain=i;
				playerInvSlots++;
			}
		}

		if(playerInvSlots==41){
			//this is the player inventory screen
			offsetArmor=offsetMain;
			offsetMain+=4;
		}else if(playerInvSlots != 36){
			//I don't know what the fuck this is
			offsetMain=-1;
		}

		InventoryMetaData meta =new InventoryMetaData();
		meta.armorOffset=offsetArmor;
		meta.inventoryOffset=offsetMain;
		meta.craftingOffset=offsetCrafting;
		meta.hotbarOffset=offsetMain+27;
		meta.hasExtra=offsetArmor!=-1;
		return meta;
	}
	public static class InventoryMetaData{
		public int inventoryOffset;
		public int armorOffset;
		public int hotbarOffset;
		public int craftingOffset;
		/** Whether we have armor and crafting */
		public boolean hasExtra;
	}

}
