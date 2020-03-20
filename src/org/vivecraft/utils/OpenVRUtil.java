/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.vivecraft.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import jopenvr.HmdMatrix34_t;
import jopenvr.HmdMatrix44_t;


/**
 *
 * @author reden
 */
public class OpenVRUtil {

    private static final long SLEEP_PRECISION = TimeUnit.MILLISECONDS.toNanos(4);
    private static final long SPIN_YIELD_PRECISION = TimeUnit.MILLISECONDS.toNanos(2);
    

    // VIVE START
    public static void Matrix4fSet(Matrix4f mat, float m11, float m12, float m13, float m14, float m21, float m22, float m23, float m24, float m31, float m32, float m33, float m34, float m41, float m42, float m43, float m44)
    {
        mat.M[0][0] = m11;
        mat.M[0][1] = m12;
        mat.M[0][2] = m13;
        mat.M[0][3] = m14;
        mat.M[1][0] = m21;
        mat.M[1][1] = m22;
        mat.M[1][2] = m23;
        mat.M[1][3] = m24;
        mat.M[2][0] = m31;
        mat.M[2][1] = m32;
        mat.M[2][2] = m33;
        mat.M[2][3] = m34;
        mat.M[3][0] = m41;
        mat.M[3][1] = m42;
        mat.M[3][2] = m43;
        mat.M[3][3] = m44;
    }

    public static void Matrix4fCopy(Matrix4f source, Matrix4f dest)
    {
        dest.M[0][0] = source.M[0][0];
        dest.M[0][1] = source.M[0][1];
        dest.M[0][2] = source.M[0][2];
        dest.M[0][3] = source.M[0][3];
        dest.M[1][0] = source.M[1][0];
        dest.M[1][1] = source.M[1][1];
        dest.M[1][2] = source.M[1][2];
        dest.M[1][3] = source.M[1][3];
        dest.M[2][0] = source.M[2][0];
        dest.M[2][1] = source.M[2][1];
        dest.M[2][2] = source.M[2][2];
        dest.M[2][3] = source.M[2][3];
        dest.M[3][0] = source.M[3][0];
        dest.M[3][1] = source.M[3][1];
        dest.M[3][2] = source.M[3][2];
        dest.M[3][3] = source.M[3][3];
    }

    public static Matrix4f Matrix4fSetIdentity(Matrix4f mat)
    {
        mat.M[0][0] = mat.M[1][1] = mat.M[2][2] = mat.M[3][3] = 1.0F;
        mat.M[0][1] = mat.M[1][0] = mat.M[2][3] = mat.M[3][1] = 0.0F;
        mat.M[0][2] = mat.M[1][2] = mat.M[2][0] = mat.M[3][2] = 0.0F;
        mat.M[0][3] = mat.M[1][3] = mat.M[2][1] = mat.M[3][0] = 0.0F;
        return mat;
    }
        
    public static Matrix4f convertSteamVRMatrix3ToMatrix4f(HmdMatrix34_t hmdMatrix, Matrix4f mat){
        Matrix4fSet(mat,
                hmdMatrix.m[0], hmdMatrix.m[1], hmdMatrix.m[2], hmdMatrix.m[3],
                hmdMatrix.m[4], hmdMatrix.m[5], hmdMatrix.m[6], hmdMatrix.m[7],
                hmdMatrix.m[8], hmdMatrix.m[9], hmdMatrix.m[10], hmdMatrix.m[11],
                0f, 0f, 0f, 1f
        );
        return mat;
    }
    
    public static Matrix4f convertSteamVRMatrix4ToMatrix4f(HmdMatrix44_t hmdMatrix, Matrix4f mat)
    {
        Matrix4fSet(mat, hmdMatrix.m[0], hmdMatrix.m[1], hmdMatrix.m[2], hmdMatrix.m[3],
                hmdMatrix.m[4], hmdMatrix.m[5], hmdMatrix.m[6], hmdMatrix.m[7],
                hmdMatrix.m[8], hmdMatrix.m[9], hmdMatrix.m[10], hmdMatrix.m[11],
                hmdMatrix.m[12], hmdMatrix.m[13], hmdMatrix.m[14], hmdMatrix.m[15]);
        return mat;
    }

    public static Vector3 convertMatrix4ftoTranslationVector(Matrix4f mat) {
        return new Vector3(mat.M[0][3], mat.M[1][3], mat.M[2][3]);
    }

    public static Quaternion convertMatrix4ftoRotationQuat(Matrix4f mat) {
        return convertMatrix4ftoRotationQuat(
                mat.M[0][0],mat.M[0][1],mat.M[0][2],
                mat.M[1][0],mat.M[1][1],mat.M[1][2],
                mat.M[2][0],mat.M[2][1],mat.M[2][2]);
    }

    public static Matrix4f rotationXMatrix(float angle) {
        float sina = (float) Math.sin((double)angle);
        float cosa = (float) Math.cos((double)angle);
        return new Matrix4f(1.0F, 0.0F, 0.0F,
                            0.0F, cosa, -sina,
                            0.0F, sina, cosa);
    }

    public static Matrix4f rotationZMatrix(float angle) {
        float sina = (float) Math.sin((double)angle);
        float cosa = (float) Math.cos((double)angle);
        return new Matrix4f(cosa, -sina, 0.0F,
                sina, cosa, 0.0f,
                0.0F, 0.0f, 1.0f);
    }

    
    public static Quaternion convertMatrix4ftoRotationQuat(float m00, float m01, float m02,
    		float m10, float m11, float m12, float m20, float m21, float m22) {
    	// first normalize the forward (F), up (U) and side (S) vectors of the rotation matrix
    	// so that the scale does not affect the rotation
    	double lengthSquared = m00 * m00 + m10 * m10 + m20 * m20;
    	if (lengthSquared != 1f && lengthSquared != 0f) {
    		lengthSquared = 1.0 / Math.sqrt(lengthSquared);
    		m00 *= lengthSquared;
    		m10 *= lengthSquared;
    		m20 *= lengthSquared;
    	}
    	lengthSquared = m01 * m01 + m11 * m11 + m21 * m21;
    	if (lengthSquared != 1 && lengthSquared != 0f) {
    		lengthSquared = 1.0 / Math.sqrt(lengthSquared);
    		m01 *= lengthSquared;
    		m11 *= lengthSquared;
    		m21 *= lengthSquared;
    	}
    	lengthSquared = m02 * m02 + m12 * m12 + m22 * m22;
    	if (lengthSquared != 1f && lengthSquared != 0f) {
    		lengthSquared = 1.0 / Math.sqrt(lengthSquared);
    		m02 *= lengthSquared;
    		m12 *= lengthSquared;
    		m22 *= lengthSquared;
    	}

    	// Use the Graphics Gems code, from
    	// ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z
    	// *NOT* the "Matrix and Quaternions FAQ", which has errors!

    	// the trace is the sum of the diagonal elements; see
    	// http://mathworld.wolfram.com/MatrixTrace.html
    	float t = m00 + m11 + m22;

    	// we protect the division by s by ensuring that s>=1
    	Quaternion quat = new Quaternion();
    	if (t >= 0) { // |w| >= .5
    		double s = Math.sqrt(t + 1); // |s|>=1 ...
    		quat.w = (float)(0.5f * s);
    		s = 0.5f / s;                 // so this division isn't bad
    		quat.x = (float)((m21 - m12) * s);
    		quat.y = (float)((m02 - m20) * s);
    		quat.z = (float)((m10 - m01) * s);
    	} else if (m00 > m11 && m00 > m22) {
    		double s = Math.sqrt(1.0 + m00 - m11 - m22); // |s|>=1
    		quat.x = (float)(s * 0.5f); // |x| >= .5
    		s = 0.5f / s;
    		quat.y = (float)((m10 + m01) * s);
    		quat.z = (float)((m02 + m20) * s);
    		quat.w = (float)((m21 - m12) * s);
    	} else if (m11 > m22) {
    		double s = Math.sqrt(1.0 + m11 - m00 - m22); // |s|>=1
    		quat.y = (float)(s * 0.5f); // |y| >= .5
    		s = 0.5f / s;
    		quat.x = (float)((m10 + m01) * s);
    		quat.z = (float)((m21 + m12) * s);
    		quat.w = (float)((m02 - m20) * s);
    	} else {
    		double s = Math.sqrt(1.0 + m22 - m00 - m11); // |s|>=1
    		quat.z = (float)(s * 0.5f); // |z| >= .5
    		s = 0.5f / s;
    		quat.x = (float)((m02 + m20) * s);
    		quat.y = (float)((m21 + m12) * s);
    		quat.w = (float)((m10 - m01) * s);
    	}

    	return quat;
}
    // VIVE END

/*    public static long getNativeWindow() {
        long window = -1;
        try {
            Object displayImpl = null;
            Method[] displayMethods = Display.class.getDeclaredMethods();
            for (Method m : displayMethods) {
                if (m.getName().equals("getImplementation")) {
                    m.setAccessible(true);
                    displayImpl = m.invoke(null, (Object[]) null);
                    break;
                }
            }            
            String fieldName = null;
            switch (LWJGLUtil.getPlatform()) {
                case LWJGLUtil.PLATFORM_LINUX:
                    fieldName = "current_window";
                    break;
                case LWJGLUtil.PLATFORM_WINDOWS:
                    fieldName = "hwnd";
                    break;
            }
            if (null != fieldName) {
                Field[] windowsDisplayFields = displayImpl.getClass().getDeclaredFields();
                for (Field f : windowsDisplayFields) {
                    if (f.getName().equals(fieldName)) {
                        f.setAccessible(true);
                        window = (Long) f.get(displayImpl);
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return window;
    }*/
}
