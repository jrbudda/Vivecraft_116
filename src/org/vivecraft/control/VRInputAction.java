package org.vivecraft.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.utils.InputSimulator;
import org.vivecraft.utils.MCReflection;
import org.vivecraft.utils.Vector2;
import org.vivecraft.utils.Vector3;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.LongByReference;
import jopenvr.InputAnalogActionData_t;
import jopenvr.InputDigitalActionData_t;
import jopenvr.JOpenVRLibrary;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.optifine.reflect.Reflector;
import org.lwjgl.glfw.GLFW;

public class VRInputAction {
	public final KeyBinding keyBinding;
	public final String name;
	public final String requirement;
	public final String type;
	public final VRInputActionSet actionSet;

	private int priority = 0;
	private boolean[] enabled = new boolean[ControllerType.values().length];
	private List<KeyListener> listeners = new ArrayList<>();
	private ControllerType currentHand = ControllerType.RIGHT;

	private long handle;
	private boolean[] pressed = new boolean[ControllerType.values().length];
	protected int[] unpressInTicks = new int[ControllerType.values().length];

	private InputDigitalActionData_t.ByReference[] digitalData = new InputDigitalActionData_t.ByReference[ControllerType.values().length];
	private InputAnalogActionData_t.ByReference[] analogData = new InputAnalogActionData_t.ByReference[ControllerType.values().length];

	public VRInputAction(KeyBinding keyBinding, String requirement, String type, VRInputActionSet actionSetOverride) {
		this.keyBinding = keyBinding;
		this.requirement = requirement;
		this.type = type;
		this.actionSet = actionSetOverride != null ? actionSetOverride : VRInputActionSet.fromKeyBinding(keyBinding);
		this.name = this.actionSet.name + "/in/" + keyBinding.getKeyDescription().replace('/', '_');

		for (int i = 0; i < ControllerType.values().length; i++) {
			enabled[i] = true;
			digitalData[i] = new InputDigitalActionData_t.ByReference();
			digitalData[i].setAutoRead(false);
			digitalData[i].setAutoWrite(false);
			digitalData[i].setAutoSynch(false);
			analogData[i] = new InputAnalogActionData_t.ByReference();
			analogData[i].setAutoRead(false);
			analogData[i].setAutoWrite(false);
			analogData[i].setAutoSynch(false);
		}
	}

	public boolean isButtonPressed() {
		if (type.equals("boolean")) {
			return digitalData().bState != 0;
		} else {
			Vector3 axis = getAxis3D(false);
			return Math.abs(axis.getX()) > 0.5f || Math.abs(axis.getY()) > 0.5f || Math.abs(axis.getZ()) > 0.5f;
		}
	}

	public boolean isButtonChanged() {
		if (type.equals("boolean")) {
			return digitalData().bChanged != 0;
		} else {
			Vector3 axis = getAxis3D(false);
			Vector3 delta = getAxis3D(true);
			return (Math.abs(axis.getX() - delta.getX()) > 0.5f) != (Math.abs(axis.getX()) > 0.5f) ||
					(Math.abs(axis.getY() - delta.getY()) > 0.5f) != (Math.abs(axis.getY()) > 0.5f) ||
					(Math.abs(axis.getZ() - delta.getZ()) > 0.5f) != (Math.abs(axis.getZ()) > 0.5f);
		}
	}

	public float getAxis1D(boolean delta) {
		switch (type) {
			case "boolean":
				return digitalToAnalog(delta);
			case "vector1":
			case "vector2":
			case "vector3":
				return delta ? analogData().deltaX : analogData().x;
			default:
				return 0.0f;
		}
	}

	public Vector2 getAxis2D(boolean delta) {
		switch (type) {
			case "boolean":
				return new Vector2(digitalToAnalog(delta), 0.0f);
			case "vector1":
				return delta ? new Vector2(analogData().deltaX, 0.0f) : new Vector2(analogData().x, 0.0f);
			case "vector2":
			case "vector3":
				return delta ? new Vector2(analogData().deltaX, analogData().deltaY) : new Vector2(analogData().x, analogData().y);
			default:
				return new Vector2();
		}
	}

	public Vector3 getAxis3D(boolean delta) {
		switch (type) {
			case "boolean":
				return new Vector3(digitalToAnalog(delta), 0.0f, 0.0f);
			case "vector1":
				return delta ? new Vector3(analogData().deltaX, 0.0f, 0.0f) : new Vector3(analogData().x, 0.0f, 0.0f);
			case "vector2":
				return delta ? new Vector3(analogData().deltaX, analogData().deltaY, 0.0f) : new Vector3(analogData().x, analogData().y, 0.0f);
			case "vector3":
				return delta ? new Vector3(analogData().deltaX, analogData().deltaY, analogData().deltaZ) : new Vector3(analogData().x, analogData().y, analogData().z);
			default:
				return new Vector3();
		}
	}

	private float digitalToAnalog(boolean delta) {
		if (delta) {
			if (digitalData().bChanged != 0)
				return digitalData().bState != 0 ? 1.0f : -1.0f;
			else
				return 0.0f;
		} else {
			return digitalData().bState != 0 ? 1.0f : 0.0f;
		}
	}

	public long getLastOrigin() {
		switch (type) {
			case "boolean":
				return digitalData().activeOrigin;
			case "vector1":
			case "vector2":
			case "vector3":
				return analogData().activeOrigin;
			default:
				return JOpenVRLibrary.k_ulInvalidInputValueHandle;
		}
	}

	public ControllerType getCurrentHand() {
		return currentHand;
	}

	public void setCurrentHand(ControllerType currentHand) {
		this.currentHand = currentHand;
	}

	public void readNewData() {
		switch (type) {
			case "boolean":
				if (isHanded())
					Arrays.stream(ControllerType.values()).forEach(this::readDigitalData);
				else
					readDigitalData(null);
				break;
			case "vector1":
			case "vector2":
			case "vector3":
				if (isHanded())
					Arrays.stream(ControllerType.values()).forEach(this::readAnalogData);
				else
					readAnalogData(null);
				break;
		}
	}

	private void readDigitalData(ControllerType hand) {
		int index = 0;
		if (hand != null)
			index = hand.ordinal();

		int error = MCOpenVR.vrInput.GetDigitalActionData.apply(handle, digitalData[index], digitalData[index].size(), hand != null ? MCOpenVR.getControllerHandle(hand) : JOpenVRLibrary.k_ulInvalidInputValueHandle);
		if (error != 0)
			throw new RuntimeException("Error reading digital data for '" + this.name + "': " + MCOpenVR.getInputError(error));
		digitalData[index].read();
	}

	private void readAnalogData(ControllerType hand) {
		int index = 0;
		if (hand != null)
			index = hand.ordinal();

		int error = MCOpenVR.vrInput.GetAnalogActionData.apply(handle, analogData[index], analogData[index].size(), hand != null ? MCOpenVR.getControllerHandle(hand) : JOpenVRLibrary.k_ulInvalidInputValueHandle);
		if (error != 0)
			throw new RuntimeException("Error reading analog data for '" + this.name + "': " + MCOpenVR.getInputError(error));
		analogData[index].read();
	}

	private InputDigitalActionData_t digitalData() {
		if (isHanded()) {
			return digitalData[currentHand.ordinal()];
		} else {
			return digitalData[0];
		}
	}

	private InputAnalogActionData_t analogData() {
		if (isHanded()) {
			return analogData[currentHand.ordinal()];
		} else {
			return analogData[0];
		}
	}

	public List<Long> getOrigins() {
		Pointer p = new Memory(JOpenVRLibrary.k_unMaxActionOriginCount * 8);
		LongByReference longRef = new LongByReference();
		longRef.setPointer(p);
		int error = MCOpenVR.vrInput.GetActionOrigins.apply(MCOpenVR.getActionSetHandle(actionSet), handle, longRef, JOpenVRLibrary.k_unMaxActionOriginCount);
		if (error != 0)
			throw new RuntimeException("Error getting action origins for '" + this.name + "': " + MCOpenVR.getInputError(error));

		List<Long> list = new ArrayList<>();
		for (long handle : p.getLongArray(0, JOpenVRLibrary.k_unMaxActionOriginCount)) {
			if (handle != JOpenVRLibrary.k_ulInvalidInputValueHandle)
				list.add(handle);
		}

		return list;
	}

	public void setHandle(long handle) {
		if (this.handle != 0)
			throw new IllegalStateException("Handle already assigned!");
		this.handle = handle;
	}

	public int getPriority() {
		return priority;
	}

	public VRInputAction setPriority(int priority) {
		this.priority = priority;
		return this;
	}

	/**
	 * If this is a handed binding, applies to the hand from {@link VRInputAction#setCurrentHand(ControllerType)}
	 */
	public boolean isEnabled() {
		if (!isEnabledRaw(currentHand)) return false;

		long lastOrigin = this.getLastOrigin();
		ControllerType hand = MCOpenVR.getOriginControllerType(lastOrigin);
		if (hand == null)
			return false;

		for (VRInputAction action : MCOpenVR.getInputActions()) {
			if (action != this && action.isEnabledRaw(hand) && action.isActive() && action.getPriority() > this.getPriority() && action.getOrigins().contains(lastOrigin)) {
				if (action.isHanded())
					return !((HandedKeyBinding)action.keyBinding).isPriorityOnController(hand);
				return false;
			}
		}

		return true;
	}

	public boolean isEnabledRaw(ControllerType hand) {
		if (isHanded())
			return enabled[hand.ordinal()];
		else
			return enabled[0];
	}

	public boolean isEnabledRaw() {
		return Arrays.stream(ControllerType.values()).anyMatch(this::isEnabledRaw);
	}

	public VRInputAction setEnabled(ControllerType hand, boolean enabled) {
		if (!isHanded())
			throw new IllegalStateException("Not a handed key binding!");
		this.enabled[hand.ordinal()] = enabled;
		return this;
	}

	public VRInputAction setEnabled(boolean enabled) {
		if (isHanded()) {
			for (ControllerType hand : ControllerType.values())
				this.enabled[hand.ordinal()] = enabled;
		} else {
			this.enabled[0] = enabled;
		}
		return this;
	}

	public boolean isActive() {
		switch (type) {
			case "boolean":
				return digitalData().bActive != 0;
			case "vector1":
			case "vector2":
			case "vector3":
				return analogData().bActive != 0;
			default:
				return false;
		}
	}

	public boolean isHanded() {
		return keyBinding instanceof HandedKeyBinding;
	}

	public void registerListener(KeyListener listener) {
		listeners.add(listener);
		listeners.sort(Comparator.comparingInt(KeyListener::getPriority).reversed());
	}

	public void unregisterListener(KeyListener listener) {
		listeners.remove(listener);
		// don't need to sort on remove
	}

	public boolean notifyListeners(boolean pressed, ControllerType hand) {
		for (KeyListener listener : listeners) {
			if (pressed) {
				if (listener.onPressed(hand))
					return true;
			}
			else {
				if (listener.onUnpressed(hand))
					return true;
			}
		}
		return false;
	}

	public void tick() {
		if (isHanded()) {
			for (int i = 0; i < ControllerType.values().length; i++) {
				if (this.unpressInTicks[i] > 0) {
					if (--this.unpressInTicks[i] == 0)
						unpressBindingImmediately(ControllerType.values()[i]);
				}
			}
		} else {
			if (this.unpressInTicks[0] > 0) {
				if (--this.unpressInTicks[0] == 0)
					unpressBindingImmediately(null);
			}
		}
	}

	private void pressBinding(ControllerType hand) {
		if (isHanded()) {
			if (hand == null || this.pressed[hand.ordinal()]) return;
			this.pressed[hand.ordinal()] = true;
			if (notifyListeners(true, hand)) return;
			((HandedKeyBinding)keyBinding).pressKey(hand);
		} else {
			if (this.pressed[0]) return;
			this.pressed[0] = true;
			if (notifyListeners(true, null)) return;
			pressKey();
		}
	}

	public void pressBinding() {
		pressBinding(this.currentHand);
	}

	public void unpressBinding(int unpressInTicks, ControllerType hand) {
		if (isHanded()) {
			if (hand == null || !this.pressed[hand.ordinal()]) return;
			this.unpressInTicks[hand.ordinal()] = unpressInTicks;
		} else {
			if (!this.pressed[0]) return;
			this.unpressInTicks[0] = unpressInTicks;
		}
	}

	public void unpressBinding(int unpressInTicks) {
		unpressBinding(unpressInTicks, this.currentHand);
	}

	public void unpressBinding() {
		unpressBinding(1);
	}

	public void unpressBindingImmediately(ControllerType hand) {
		if (isHanded()) {
			if (hand == null || !this.pressed[hand.ordinal()]) return;
			this.pressed[hand.ordinal()] = false;
			if (notifyListeners(false, hand)) return;
			((HandedKeyBinding)keyBinding).unpressKey(hand);
		} else {
			if (!this.pressed[0]) return;
			this.pressed[0] = false;
			if (notifyListeners(false, null)) return;
			unpressKey();
		}
	}

	public static void setKeyBindState(KeyBinding kb, boolean pressed) {
		if (kb != null) {
			MCReflection.KeyBinding_pressed.set(kb, pressed); //kb.pressed = pressed;
			MCReflection.KeyBinding_pressTime.set(kb, (Integer)MCReflection.KeyBinding_pressTime.get(kb) + 1); //++kb.pressTime;
		}
	}

	public void pressKey() {
		InputMappings.Input input = (InputMappings.Input)MCReflection.KeyBinding_keyCode.get(keyBinding);

		if (input.getKeyCode() != GLFW.GLFW_KEY_UNKNOWN && !MCOpenVR.isSafeBinding(keyBinding) && (!Reflector.forgeExists() || Reflector.call(keyBinding, Reflector.ForgeKeyBinding_getKeyModifier) == Reflector.getFieldValue(Reflector.KeyModifier_NONE))) {
			if (input.getType() == InputMappings.Type.KEYSYM) {
				//System.out.println("InputSimulator pressKey: " + kb.getKeyDescription() + ", input type: " + input.getType().name() + ", key code: " + input.getKeyCode());
				InputSimulator.pressKey(input.getKeyCode());
				return;
			} else if (input.getType() == InputMappings.Type.MOUSE) {
				//System.out.println("InputSimulator pressMouse: " + kb.getKeyDescription() + ", input type: " + input.getType().name() + ", key code: " + input.getKeyCode());
				InputSimulator.pressMouse(input.getKeyCode());
				return;
			}
		}

		// If all else fails, just press the binding directly
		//System.out.println("setKeyBindState true: " + kb.getKeyDescription() + ", input type: " + input.getType().name() + ", key code: " + input.getKeyCode());
		setKeyBindState(keyBinding, true);
	}

	public void unpressKey() {
		InputMappings.Input input = (InputMappings.Input)MCReflection.KeyBinding_keyCode.get(keyBinding);

		if (input.getKeyCode() != GLFW.GLFW_KEY_UNKNOWN && !MCOpenVR.isSafeBinding(keyBinding) && (!Reflector.forgeExists() || Reflector.call(keyBinding, Reflector.ForgeKeyBinding_getKeyModifier) == Reflector.getFieldValue(Reflector.KeyModifier_NONE))) {
			if (input.getType() == InputMappings.Type.KEYSYM) {
				//System.out.println("InputSimulator releaseKey: " + kb.getKeyDescription() + ", input type: " + input.getType().name() + ", key code: " + input.getKeyCode());
				InputSimulator.releaseKey(input.getKeyCode());
				return;
			} else if (input.getType() == InputMappings.Type.MOUSE) {
				//System.out.println("InputSimulator releaseMouse: " + kb.getKeyDescription() + ", input type: " + input.getType().name() + ", key code: " + input.getKeyCode());
				InputSimulator.releaseMouse(input.getKeyCode());
				return;
			}
		}

		// If all else fails, just press the binding directly
		//System.out.println("unpressKey: " + kb.getKeyDescription() + ", input type: " + input.getType().name() + ", key code: " + input.getKeyCode());
		MCReflection.KeyBinding_unpressKey.invoke(keyBinding);
	}

	public interface KeyListener {
		boolean onPressed(@Nullable ControllerType hand);
		boolean onUnpressed(@Nullable ControllerType hand);
		int getPriority();
	}
}
