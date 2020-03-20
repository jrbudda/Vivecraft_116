package org.vivecraft.gui.physical.interactables;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.inventory.container.Slot;

import org.vivecraft.gui.physical.PhysicalItemSlotGui;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.utils.Convert;
import org.vivecraft.utils.Quaternion;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import org.vivecraft.utils.Utils;

public class PhysicalItemSlot implements Interactable {
	public PhysicalItemSlotGui gui;
	public Minecraft mc;
	public boolean enabled = true;
	public boolean preview=true;
	public PhysicalItemSlot(PhysicalItemSlotGui gui, int slotId) {
		this.slotId = slotId;
		this.gui=gui;
		this.mc=Minecraft.getInstance();
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Render this slot at the set position and rotation
	 * */
	public void render(double partialTicks, int renderLayer){
		if(this.getOpacity()==0)
			return;

		ItemRenderer renderItem = Minecraft.getInstance().getItemRenderer();
		ItemStack item = getDisplayedItem();
		if (item==null)
			return;
		float scale = (float) this.scale;
		if (renderItem.shouldRenderItemIn3D(item)) {
			scale *= this.fullBlockScaleMult;
		}

		if(popOut){
			scale*=popOutScaleMult;
		}

		boolean is3d = renderItem.shouldRenderItemIn3D(item);
		double itemSize = is3d ? 1 / 2f : 1 / 16f;

		if (is3d) {
			Utils.glRotate(this.fullBlockRotation);
			GlStateManager.translated(0, scale * itemSize * 0.5, 0);
		} else {
			GlStateManager.translated(0, 0, -scale * itemSize * 0.5);
		}

		GlStateManager.scalef(scale, scale, scale);

		GlStateManager.pushMatrix();

		if(renderLayer==0) {
			renderItem.setFade((float) this.getOpacity());
			renderItem.renderItem(item, mc.player, TransformType.FIXED, false);
		}
		GlStateManager.popMatrix();

		GlStateManager.pushMatrix();
		if (item.getCount() > 1 && renderLayer==1) {
			double itemHeight = itemSize;
			double itemWidth = 1 / 2f;
			if (is3d) {
				Utils.glRotate(this.fullBlockRotation.inverse());
				//if(renderLayer==0)
					GlStateManager.scaled((float) 1 / this.fullBlockScaleMult, (float) 1 / this.fullBlockScaleMult, (float) 1 / this.fullBlockScaleMult);
				itemHeight *= this.fullBlockScaleMult;
				itemWidth *= this.fullBlockScaleMult;
			}
	//		GlStateManager.multMatrixf(Convert.matrix(this.counterRot).toMCMatrix4f());

			Vec3d counterpos = new Vec3d(-itemWidth * 0.5, -itemWidth * 0.5, -itemHeight * 0.5 - 0.05);
			counterpos = counterpos.add(this.counterPos);

			GlStateManager.translated(counterpos.x,counterpos.y,counterpos.z);

			GlStateManager.scaled(counterScale/this.scale, counterScale/this.scale, counterScale/this.scale);

			if (this.getOpacity() == 1) {
				GameRenderer.drawText(mc.fontRenderer, "" + item.getCount(),
						0,0,0, 0, 0, true);
			}
		}

		renderItem.setFade(1);
		GlStateManager.popMatrix();
	}


	/**
	 * The corresponding vanilla slot number
	 *
	 * @see <a href="http://wiki.vg/Inventory">wiki.vg/Inventory</a> for reference
	 */
	public int slotId;

	/**
	 * Position in meters relative to the position of the entity or block<br>
	 * If block, the middle of the block (0.5,0.5,0.5) is considered the origin<br>
	 * If relative to connected blocks (e.g. double Chest) the leftmost (w/a frontmost) when looking from the front is the origin<br>
	 * The coordinate systems forward vector is the entity or blocks backwards vector<br>
	 * (the direction you're looking you face each other) using minecrafts coordinate system,<br>
	 * meaning +X is left, +Y is up, +Z is forward, +pitch is rotation down, +yaw rotation right and +roll rotation clockwise<br>
	 * <br>
	 * The item is rendered with its *back-face* at this position if it is a flat item<br>
	 * or with its bottom face at this position if it is 3d<br>
	 * <br>
	 * Example: (0.5,0.5,-0.5) is on the front face, in the top left corner
	 */
	public Vec3d position = new Vec3d(0, 0, -0.5);

	public Vec3d getPosition(double partialTicks) {
		return position;
	}

	public Quaternion getRotation(double partialTicks) {
		return rotation;
	}

	public Quaternion getAnchorRotation(double partialTicks){
		return gui.getAnchorRotation(partialTicks);
	}

	public Vec3d getAnchorPos(double partialTicks){
		return gui.getAnchorPos(partialTicks);
	}

	public double popOutScaleMult=1.2;
	public boolean popOut=false;

	/**
	 * The rotation of the slot, relative to the entity or blocks backwards vector (see {@link PhysicalItemSlot#position})<br>
	 * Defines which way the *back-face* of the itemslot is looking, meaning lying flat is a 90° pitch down (so +90° in minecraft terms)
	 */
	public Quaternion rotation = new Quaternion();

	public Quaternion fullBlockRotation = new Quaternion(-90, 0, 0);

	/**
	 * The position of the counter relative to {@link PhysicalItemSlot#position} and aligned with {@link PhysicalItemSlot#rotation}
	 * originated at the lower right corner of the items top face. In short: this is an offset from the counters normal position
	 */
	public Vec3d counterPos = new Vec3d(0, 0, 0);

	/**
	 * The rotation of the counter relative to {@link PhysicalItemSlot#rotation}
	 */
	public Quaternion counterRot = new Quaternion();

	public Slot slot;

	public ItemStack getDisplayedItem(){
		if(preview && gui.touching ==this){
			ItemStack fakeItem=mc.physicalGuiManager.getVirtualHeldItem();
			if(!fakeItem.isEmpty())
				return fakeItem;
		}

		if(slot!=null)
			return slot.getStack();
		else return null;
	}

	/**
	 * The size of the rendered item in meters (width and height)
	 */
	public double scale = 0.2;

	public double opacity = 1;

	public double getOpacity(){
		if(gui.touching ==this && preview && !mc.physicalGuiManager.getVirtualHeldItem().isEmpty())
			return 0.5;
		return opacity;
	}

	/**
	 * Scales the item if it is a 3d block. 2 matches the size of a held item
	 */
	public double fullBlockScaleMult = 1.9;

	/**
	 * Scale of the counter
	 *
	 * @see PhysicalItemSlot#scale
	 */
	public double counterScale = 0.1;

	@Override
	public void touch() {
		int mainhand = (mc.gameSettings.mainHand == HandSide.RIGHT) ? 0 : 1;

		popOut=true;

		if(getDisplayedItem()!=null && !getDisplayedItem().isEmpty()) {
			MCOpenVR.triggerHapticPulse(mainhand, 500);
		}

		if(preview && !mc.physicalGuiManager.getVirtualHeldItem().isEmpty()){
			mc.physicalGuiManager.setHideItemTouchingSlotOverride(ItemStack.EMPTY);
			//opacity=0.5;
		}else{
			mc.physicalGuiManager.setHideItemTouchingSlotOverride(null);
		}
	}

	@Override
	public void untouch() {
		popOut=false;
		mc.physicalGuiManager.setHideItemTouchingSlotOverride(null);
	}

	@Override
	public void click(int button) {
		if (!gui.isOpen())
			return;
		opacity=1;
		mc.physicalGuiManager.clickSlot(slotId,button);
	}

	@Override
	public void unclick(int button) {
	}

	@Override
	public AxisAlignedBB getBoundingBox() {
		return new AxisAlignedBB(-gui.touchDistance,-gui.touchDistance,-gui.touchDistance,gui.touchDistance,gui.touchDistance,gui.touchDistance);
	}
}