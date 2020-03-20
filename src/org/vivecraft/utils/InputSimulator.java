package org.vivecraft.utils;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;

import static org.lwjgl.glfw.GLFW.*;

public class InputSimulator {
	private static Set<Integer> pressedKeys = new HashSet<>();

	public static boolean isKeyDown(int key) {
		return pressedKeys.contains(key);
	}

	public static void pressKey(int key, int modifiers) {
		Minecraft.getInstance().keyboardListener.onKeyEvent(Minecraft.getInstance().mainWindow.getHandle(), key, 0, GLFW_PRESS, modifiers);
		pressedKeys.add(key);
	}

	public static void pressKey(int key) {
		pressKey(key, 0);
	}
    
	public static void releaseKey(int key, int modifiers) {
		Minecraft.getInstance().keyboardListener.onKeyEvent(Minecraft.getInstance().mainWindow.getHandle(), key, 0, GLFW_RELEASE, modifiers);
		pressedKeys.remove(key);
	}

	public static void releaseKey(int key) {
		releaseKey(key, 0);
	}
    
	public static void typeChar(char character, int modifiers) {
		Minecraft.getInstance().keyboardListener.onCharEvent(Minecraft.getInstance().mainWindow.getHandle(), character, modifiers);
	}

	public static void typeChar(char character) {
    	typeChar(character, 0);
	}

	public static void pressMouse(int button, int modifiers) {
    	Minecraft.getInstance().mouseHelper.mouseButtonCallback(Minecraft.getInstance().mainWindow.getHandle(), button, GLFW_PRESS, modifiers);
	}

	public static void pressMouse(int button) {
		pressMouse(button, 0);
	}

	public static void releaseMouse(int button, int modifiers) {
		Minecraft.getInstance().mouseHelper.mouseButtonCallback(Minecraft.getInstance().mainWindow.getHandle(), button, GLFW_RELEASE, modifiers);
	}

	public static void releaseMouse(int button) {
		releaseMouse(button, 0);
	}

	public static void setMousePos(double x, double y) {
		Minecraft.getInstance().mouseHelper.cursorPosCallback(Minecraft.getInstance().mainWindow.getHandle(), x, y);
	}

	public static void scrollMouse(double xOffset, double yOffset) {
		Minecraft.getInstance().mouseHelper.scrollCallback(Minecraft.getInstance().mainWindow.getHandle(), xOffset, yOffset);
	}

	public static void typeChars(CharSequence characters) {
		int length = characters.length();
		for (int i = 0; i < length; i++) {
			char character = characters.charAt(i);
			typeChar(character);
		}
	}
}

