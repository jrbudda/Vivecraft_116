package org.vivecraft.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

public enum HardwareType {
	VIVE(true, false, "HTC"),
	OCULUS(false, true, "Oculus"),
	WINDOWSMR(true, true, "WindowsMR");

	public final List<String> manufacturers;
	public final boolean hasTouchpad;
	public final boolean hasStick;
	private static Map<String, HardwareType> map = new HashMap<>();
	
	private HardwareType(boolean hasTouchpad, boolean hasStick, String... manufacturers) {
		this.hasTouchpad = hasTouchpad;
		this.hasStick = hasStick;
		this.manufacturers = ImmutableList.copyOf(manufacturers);
	}
	
	public static HardwareType fromManufacturer(String name) {
		return map.containsKey(name) ? map.get(name) : VIVE;
	}
	
	static {
		for (HardwareType hw : values()) {
			for (String str : hw.manufacturers) {
				assert !map.containsKey(str) : "Duplicate manufacturer: " + str;
				map.put(str, hw);
			}
		}
	}
}
