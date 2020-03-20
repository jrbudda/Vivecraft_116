package org.vivecraft.gui.physical.interactables;

import org.vivecraft.utils.Quaternion;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public interface Interactable{
	void render(double partialTicks, int renderLayer);
	Vec3d getPosition(double partialTicks);
	Quaternion getRotation(double partialTicks);

	Vec3d getAnchorPos(double partialTicks);
	Quaternion getAnchorRotation(double partialTicks);

	boolean isEnabled();
	default boolean isTouchable(){ return isEnabled(); }
	void touch();
	void untouch();
	void click(int button);
	void unclick(int button);
	default void update(){};
	default void onDragDrop(Interactable source){}

	AxisAlignedBB getBoundingBox();
}
