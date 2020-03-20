package org.vivecraft.control;

import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.utils.Vector2;
import org.vivecraft.utils.lwjgl.Vector2f;

public class TrackpadSwipeSampler {
	private static final int UP = 0;
	private static final int RIGHT = 1;
	private static final int DOWN = 2;
	private static final int LEFT = 3;

	private Vector2f[] buffer = new Vector2f[5];
	private int index;
	private long count;
	private Vector2f accumulator = new Vector2f();
	private int[] swiped = new int[4];

	public float threshold = 0.5f;

	public TrackpadSwipeSampler() {
		for (int i = 0; i < buffer.length; i++)
			buffer[i] = new Vector2f();
	}

	public void update(ControllerType hand, Vector2 position) {
		MCOpenVR.getInputAction(MCOpenVR.keyTrackpadTouch).setCurrentHand(hand);
		if (MCOpenVR.getInputAction(MCOpenVR.keyTrackpadTouch).isButtonPressed()) {
			buffer[index].set(position.getX(), position.getY());
			if (++index >= buffer.length)
				index = 0;
			++count;
		} else {
			for (Vector2f vec : buffer)
				vec.set(0, 0);
			count = 0;
		}

		if (count >= buffer.length) {
			int nextIndex = (index + 1) % buffer.length;
			accumulator.x += buffer[nextIndex].x - buffer[index].x;
			accumulator.y += buffer[nextIndex].y - buffer[index].y;

			if (accumulator.x >= threshold) {
				accumulator.x -= threshold;
				++swiped[RIGHT];
			}
			if (accumulator.x <= -threshold) {
				accumulator.x += threshold;
				++swiped[LEFT];
			}
			if (accumulator.y >= threshold) {
				accumulator.y -= threshold;
				++swiped[UP];
			}
			if (accumulator.y <= -threshold) {
				accumulator.y += threshold;
				++swiped[DOWN];
			}
		} else {
			accumulator.set(0, 0);
		}
	}

	public boolean isSwipedLeft() {
		return isSwiped(LEFT);
	}

	public boolean isSwipedRight() {
		return isSwiped(RIGHT);
	}

	public boolean isSwipedUp() {
		return isSwiped(UP);
	}

	public boolean isSwipedDown() {
		return isSwiped(DOWN);
	}

	private boolean isSwiped(int direction) {
		if (swiped[direction] > 0) {
			--swiped[direction];
			return true;
		} else {
			return false;
		}
	}
}
