package org.vivecraft.api;

import java.util.LinkedList;
import java.util.ListIterator;

import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;

public class Vec3History {

	private class entry{
		public long ts;
		public Vec3d data;
		public entry(Vec3d in){
			this.ts = Util.milliTime();
			this.data = in;
		}
	}
	
	private int _capacity = 90*5;
	private LinkedList<entry> _data = new LinkedList<entry>();
	
	
	public void add(Vec3d in){
		_data.add(new entry(in));
		if (_data.size() > _capacity) _data.removeFirst();
	}
	
	public void clear(){
		_data.clear();
	}
	
	public Vec3d latest(){
		return _data.getLast().data;
	}
	

	/**
	 * Get the total integrated device translation for the specified time period. Return value is in meters.
	 */
	public double totalMovement(double seconds){
		long now = Util.milliTime();
		ListIterator<entry> it = _data.listIterator(_data.size());
		entry last = null;
		double sum = 0;
		int count = 0;
		while (it.hasPrevious()){
			entry i = it.previous();
			count++;
			if(now - i.ts > seconds *1000)
				break;
			if (last == null){
				last = i;
				continue;
			}
			sum += (last.data.distanceTo(i.data));
		}
		return sum;
	}
	
	/**
	 * Get the vector representing the difference in position from now to @seconds ago.
	 */
	public Vec3d netMovement(double seconds){
		long now = Util.milliTime();
		ListIterator<entry> it = _data.listIterator(_data.size());
		entry last = null;
		entry thing = null;
		double sum = 0;
		
		while (it.hasPrevious()){
			entry i = it.previous();
			if(now - i.ts > seconds *1000) break;
			if (last == null){
				last = i;
				continue;
			}
			thing = i;
		}
		if(last == null || thing == null) return new Vec3d(0, 0, 0);
		return last.data.subtract(thing.data);	
	}
	
	/**
	 * Get the average scalar speed of the device over the specified length of time. Returns m/s.
	 */
	public double averageSpeed(double seconds){
		long now = Util.milliTime();
		ListIterator<entry> it = _data.listIterator(_data.size());
		double out = 0;
		entry last = null;
		int j = 0;
		while (it.hasPrevious()){
			entry i = it.previous();
			if(now - i.ts > seconds *1000) break;
			if (last == null){
				last = i;
				continue;
			}
			j++;
			double tdelta = (.001*(last.ts - i.ts));
			double ddelta = (last.data.subtract(i.data).length());
			out = out + ddelta/tdelta;
		}
		if(j == 0) return out;
		
		return out/j;
	}

	/**
	 * Get the average room position for the last @seconds.
	 */
	public Vec3d averagePosition(double seconds){
		long now = Util.milliTime();
		ListIterator<entry> it = _data.listIterator(_data.size());
		Vec3d out = new Vec3d(0,0,0);
		int j = 0;
		while (it.hasPrevious()){
			entry i = it.previous();
			if(now - i.ts > seconds *1000) break;
			j++;
			out=out.add(i.data);
		}
		if(j==0) return out;
		return out.scale(1.0/j);
	}
	
	
}
