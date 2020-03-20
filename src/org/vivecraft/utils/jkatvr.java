package org.vivecraft.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;

public class jkatvr implements Library {
	public static final String KATVR_LIBRARY_NAME = "WalkerBase.dll";

	public static final NativeLibrary KATVR_NATIVE_LIB = NativeLibrary.getInstance(KATVR_LIBRARY_NAME);
	
	static {
		Native.register(jkatvr.class, KATVR_NATIVE_LIB);
	}
	
	public static native void Init(int count);
	public static native int Launch();
	public static native boolean CheckForLaunch();
	public static native void Halt();

	//index = 1
	//bodyYaw 0 to 1024 
	//walkPower: ??
	//moveDirection 1= forward, 0 = stop, -1 = backwards
	//isMoving: 0 for not, 1 for moving
	//Distancer: total distance walked.
	public static native boolean GetWalkerData(int index, 
			IntByReference bodyYaw, 
			DoubleByReference walkPower, 
			IntByReference moveDirection, 
			IntByReference isMoving, 
			FloatByReference Distancer);
	
	static float yaw, yawOffset;
	static double power;
	static int direction;
	static int ismoving;

	static IntByReference y = new IntByReference();
	static IntByReference m = new IntByReference();
	static IntByReference is = new IntByReference();
	static DoubleByReference pow = new DoubleByReference();
	static FloatByReference fl = new FloatByReference();

	static float mag = 0.15f;
	static float bmag = 0.10f;
	static float maxpower = 3000;
	
	public static void query(){
		try {
			boolean b = GetWalkerData(0, y, pow, m, is, fl);
			yaw = y.getValue();
			power = pow.getValue();
			direction = -m.getValue();
			ismoving = is.getValue();
			yaw = yaw / 1024f * 360f;
		} catch (Exception e) {
			System.out.println("KATVR Error: " + e.getMessage());		
		}
	}
	
	public static float getYaw(){
		return yaw - yawOffset;
	}
	
	public static boolean isMoving(){
		return ismoving == 1;
	}
	
	public static void resetYaw(float offsetDegrees){
		yawOffset = offsetDegrees + yaw;
	}
	
	
	public static float walkDirection(){
		return direction;
	}
	
	public static float getSpeed(){
		return (float) (power/maxpower * (walkDirection() == 1 ? mag : bmag));
	}
	
	
}
