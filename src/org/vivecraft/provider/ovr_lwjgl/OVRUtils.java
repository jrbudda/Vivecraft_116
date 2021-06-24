package org.vivecraft.provider.ovr_lwjgl;

import org.lwjgl.ovr.OVRMatrix4f;
import org.lwjgl.ovr.OVRPosef;
import org.vivecraft.utils.Utils;
import org.vivecraft.utils.math.Matrix4f;
import org.vivecraft.utils.math.Quaternion;

import jopenvr.HmdMatrix44_t;

public class OVRUtils {

	public static Matrix4f ovrPoseToMatrix(OVRPosef pose) {
		Quaternion quat = new Quaternion();
		quat.x = pose.Orientation().x();
		quat.y = pose.Orientation().y();
		quat.z = pose.Orientation().z();
		quat.w = pose.Orientation().w();
		
		Matrix4f out = new Matrix4f(quat);
		out.M[0][3] = pose.Position().x();
		out.M[1][3] = pose.Position().y();
		out.M[2][3] = pose.Position().z();
		
		return out;
	}
	
    public static Matrix4f ovrMatrix4ToMatrix4f(OVRMatrix4f hmdMatrix)
    {
    	Matrix4f out = new Matrix4f();
        Utils.Matrix4fSet(out, hmdMatrix.M(0), hmdMatrix.M(1), hmdMatrix.M(2), hmdMatrix.M(3),
                hmdMatrix.M(4), hmdMatrix.M(5), hmdMatrix.M(6), hmdMatrix.M(7),
                hmdMatrix.M(8), hmdMatrix.M(9), hmdMatrix.M(10), hmdMatrix.M(11),
                hmdMatrix.M(12), hmdMatrix.M(13), hmdMatrix.M(14), hmdMatrix.M(15));
        return out;
    }
}
