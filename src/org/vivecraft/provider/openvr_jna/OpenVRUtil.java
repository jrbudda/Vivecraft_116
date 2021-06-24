/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.vivecraft.provider.openvr_jna;

import java.util.concurrent.TimeUnit;

import org.vivecraft.utils.Utils;
import org.vivecraft.utils.math.Matrix4f;
import org.vivecraft.utils.math.Quaternion;
import org.vivecraft.utils.math.Vector3;

import jopenvr.HmdMatrix34_t;
import jopenvr.HmdMatrix44_t;


public class OpenVRUtil {
        
    public static Matrix4f convertSteamVRMatrix3ToMatrix4f(HmdMatrix34_t hmdMatrix, Matrix4f mat){
        Utils.Matrix4fSet(mat,
                hmdMatrix.m[0], hmdMatrix.m[1], hmdMatrix.m[2], hmdMatrix.m[3],
                hmdMatrix.m[4], hmdMatrix.m[5], hmdMatrix.m[6], hmdMatrix.m[7],
                hmdMatrix.m[8], hmdMatrix.m[9], hmdMatrix.m[10], hmdMatrix.m[11],
                0f, 0f, 0f, 1f
        );
        return mat;
    }
    
    public static Matrix4f convertSteamVRMatrix4ToMatrix4f(HmdMatrix44_t hmdMatrix, Matrix4f mat)
    {
        Utils.Matrix4fSet(mat, hmdMatrix.m[0], hmdMatrix.m[1], hmdMatrix.m[2], hmdMatrix.m[3],
                hmdMatrix.m[4], hmdMatrix.m[5], hmdMatrix.m[6], hmdMatrix.m[7],
                hmdMatrix.m[8], hmdMatrix.m[9], hmdMatrix.m[10], hmdMatrix.m[11],
                hmdMatrix.m[12], hmdMatrix.m[13], hmdMatrix.m[14], hmdMatrix.m[15]);
        return mat;
    }
   
    public static Quaternion convertMatrix4ftoRotationQuat(Matrix4f mat) {
        return Utils.convertMatrix4ftoRotationQuat(
                mat.M[0][0],mat.M[0][1],mat.M[0][2],
                mat.M[1][0],mat.M[1][1],mat.M[1][2],
                mat.M[2][0],mat.M[2][1],mat.M[2][2]);
    }

	public static HmdMatrix34_t convertToMatrix34(net.minecraft.util.math.vector.Matrix4f matrix) {
		HmdMatrix34_t mat = new HmdMatrix34_t();
		mat.m[0 + 0 * 4] = matrix.m00;
		mat.m[1 + 0 * 4] = matrix.m10;
		mat.m[2 + 0 * 4] = matrix.m20;
		mat.m[3 + 0 * 4] = matrix.m30;
		mat.m[0 + 1 * 4] = matrix.m01;
		mat.m[1 + 1 * 4] = matrix.m11;
		mat.m[2 + 1 * 4] = matrix.m21;
		mat.m[3 + 1 * 4] = matrix.m31;
		mat.m[0 + 2 * 4] = matrix.m02;
		mat.m[1 + 2 * 4] = matrix.m12;
		mat.m[2 + 2 * 4] = matrix.m22;
		mat.m[3 + 2 * 4] = matrix.m32;
		return mat;
	}
    
    // VIVE END

}
