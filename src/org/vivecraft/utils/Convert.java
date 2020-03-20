package org.vivecraft.utils;

//import org.vivecraft.utils.Matrix4f;
//import javax.vecmath.Matrix4f;
//import net.minecraft.client.renderer.Matrix4f;

import org.vivecraft.utils.lwjgl.Quaternion;

import java.nio.FloatBuffer;

public class Convert {
	public static Convert.Matrix matrix(float[] floatArray){
		return new Convert.Matrix(floatArray);
	}
	
	public static Convert.Matrix matrix(float[][] float2Array){
		float[] floatArray=new float[float2Array.length*float2Array[0].length];
		for (int i = 0; i < float2Array.length; i++) {
			for (int j = 0; j < float2Array[0].length; j++) {
				floatArray[i*float2Array[0].length+j] = float2Array[i][j];
			}
		}
		return matrix(floatArray);
	}
	
	public static Convert.Matrix matrix(FloatBuffer floatBuffer){
		float[] array=new float[floatBuffer.capacity()];
		floatBuffer.position(0);
		floatBuffer.get(array);
		floatBuffer.position(0);
		return matrix(array);
	}
	
	public static Convert.Matrix matrix(org.vivecraft.utils.lwjgl.Matrix4f matrix4f){
		FloatBuffer floatBuffer=FloatBuffer.allocate(4*4);
		matrix4f.store(floatBuffer);
		return matrix(floatBuffer);
	}
	
	public static Convert.Matrix matrix(org.vivecraft.utils.Quaternion quaternion){
		return matrix(quaternion.getMatrix());
	}
	
	
	public static class Matrix {
		int dimension;
		
		float[] floatArray;
		double[] doubleArray;
		int[] intArray;
		
		boolean floatFilled = false;
		boolean doubleFilled = false;
		boolean intFilled = false;
		
		public Matrix(float[] floatArray) {
			dimension = (int) Math.sqrt(floatArray.length);
			if (dimension * dimension != floatArray.length) {
				throw new IllegalArgumentException("Input array has invalid length");
			}
			
			this.floatArray = floatArray;
			floatFilled = true;
		}
		
		private void needFloats() {
			if (!floatFilled) {
				for (int i = 0; i < floatArray.length; i++) {
					if (doubleFilled) {
						floatArray[i] = (float) doubleArray[i];
						continue;
					}
					if (intFilled) {
						floatArray[i] = (float) intArray[i];
						continue;
					}
				}
				floatFilled = true;
			}
		}
		
		private void needDoubles() {
			if (!doubleFilled) {
				for (int i = 0; i < doubleArray.length; i++) {
					if (floatFilled) {
						doubleArray[i] = (double) floatArray[i];
						continue;
					}
					if (intFilled) {
						doubleArray[i] = (double) intArray[i];
						continue;
					}
				}
				doubleFilled = true;
			}
		}
		
		private void needInts() {
			if (!intFilled) {
				for (int i = 0; i < intArray.length; i++) {
					if (doubleFilled) {
						intArray[i] = (int) doubleArray[i];
						continue;
					}
					if (floatFilled) {
						intArray[i] = (int) floatArray[i];
						continue;
					}
				}
				intFilled = true;
			}
		}
		
		
		public org.vivecraft.utils.Matrix4f toOVRMatrix4f() {
			needFloats();
			if (dimension == 3) {
				return new org.vivecraft.utils.Matrix4f(
						floatArray[0], floatArray[1], floatArray[2],
						floatArray[3], floatArray[4], floatArray[5],
						floatArray[6], floatArray[7], floatArray[8]);
				
			} else if (dimension == 4) {
				return new org.vivecraft.utils.Matrix4f(
						floatArray[0], floatArray[1], floatArray[2], floatArray[3],
						floatArray[4], floatArray[5], floatArray[6], floatArray[7],
						floatArray[8], floatArray[9], floatArray[10], floatArray[11],
						floatArray[12], floatArray[13], floatArray[14], floatArray[15]
				);
			} else {
				throw new IllegalArgumentException("Wrong dimension! Can't convert Matrix" + dimension + " to Matrix4f");
			}
		}
		
		public net.minecraft.client.renderer.Matrix4f toMCMatrix4f() {
			needFloats();
			if (dimension == 4) {
				net.minecraft.client.renderer.Matrix4f mat=new net.minecraft.client.renderer.Matrix4f();
				for (int row = 0; row < 4; row++) {
					for (int col = 0; col < 4; col++) {
						mat.set(col,row,floatArray[row*4+col]);
					}
				}
				return mat;
			} else {
				throw new IllegalArgumentException("Wrong dimension! Can't convert Matrix" + dimension + " to Matrix4f");
			}
		}
		
		
		
		public FloatBuffer toFloatBuffer(){
			return FloatBuffer.wrap(floatArray);
		}
		
	}
}
