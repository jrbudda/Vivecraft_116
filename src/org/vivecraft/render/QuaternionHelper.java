package org.vivecraft.render;

import java.nio.FloatBuffer;

import org.vivecraft.utils.lwjgl.Matrix4f;
import org.vivecraft.utils.lwjgl.Quaternion;

import net.minecraft.util.math.Vec3d;

/**
 * With thanks to 'Exemplos de LWJGL', by fabcam...@hotmail.com
 *  modified from  fcampos/rawengine3D/MathUtil/Quaternion.java
 */
public class QuaternionHelper
{
    public static final Quaternion IDENTITY_QUATERNION = new Quaternion().setIdentity();

//    public static String toEulerDegString(String name, Quaternion q1, Axis rot1, Axis rot2, Axis rot3, HandedSystem hand, RotateDirection rotDir)
//    {
//        Quatf quat = new Quatf(q1.x, q1.y, q1.z, q1.w);
//        return toEulerDegString(name, quat, rot1, rot2, rot3, hand, rotDir);
//    }
//
//    public static String toEulerDegString(String name, Quatf q1, Axis rot1, Axis rot2, Axis rot3, HandedSystem hand, RotateDirection rotDir)
//    {
//        EulerOrient euler = OculusRift.getEulerAnglesDeg(q1, 1.0f, rot1, rot2, rot3, hand, rotDir);
//        return String.format("%s: Yaw: %.3f, Pitch: %.3f, Roll: %.3f", new Object[] {name, Float.valueOf(euler.yaw), Float.valueOf(euler.pitch), Float.valueOf(euler.roll)});
//    }

    public static Quaternion clone(Quaternion q1)
    {
        return new Quaternion(q1.x, q1.y, q1.z, q1.w);
    }

    public static Quaternion pow(Quaternion q1, float power)
    {
        Quaternion input = QuaternionHelper.clone(q1);
        float inputMagnitude = QuaternionHelper.magnitude(input);
        Vec3d nHat = new Vec3d(input.x, input.y, input.z).normalize();
        Quaternion vectorBit = QuaternionHelper.exp(QuaternionHelper.scalarMultiply(new Quaternion((float)nHat.x, (float)nHat.y, (float)nHat.z, 0), (float)(power * Math.acos(input.w / inputMagnitude))));
        return QuaternionHelper.scalarMultiply(vectorBit, (float)Math.pow(inputMagnitude, power));
    }

    public static Quaternion mul(Quaternion left, Quaternion right)
    {
        Quaternion result = IDENTITY_QUATERNION;
        Quaternion.mul(left, right, result);
        return result;
    }

    public static Quaternion exp(Quaternion input)
    {
        float inputA = input.w;
        Vec3d inputV = new Vec3d(input.x, input.y, input.z);
        float outputA = (float)(Math.exp(inputA) * Math.cos(inputV.length()));
        Vec3d outputV = new Vec3d(
        Math.exp(inputA) * (inputV.normalize().x * (float)Math.sin(inputV.length())),
        Math.exp(inputA) * (inputV.normalize().y * (float)Math.sin(inputV.length())),
        Math.exp(inputA) * (inputV.normalize().z * (float)Math.sin(inputV.length())));

        return new Quaternion((float)outputV.x, (float)outputV.y, (float)outputV.z, outputA);
    }

    public static float magnitude(Quaternion input)
    {
        return (float)Math.sqrt(input.x * input.x + input.y * input.y + input.z * input.z + input.w * input.w);
    }

    public static Quaternion scalarMultiply(Quaternion input, float scalar)
    {
        return new Quaternion(input.x * scalar, input.y * scalar, input.z * scalar, input.w * scalar);
    }

    public static Quaternion slerp(Quaternion q1, Quaternion q2, float t)
    {
        Quaternion qInterpolated = new Quaternion();

        if(QuaternionHelper.isEqual(q1, q2))
        {
            return q1;
        }

        // Temporary array to hold second quaternion.

        float cosTheta = q1.x * q2.x + q1.y * q2.y + q1.z * q2.z + q1.w * q2.w;

        if(cosTheta < 0.0f)
        {
            // Flip sign if so.
            q2 = QuaternionHelper.conjugate(q2);
            cosTheta = -cosTheta;
        }

        float beta = 1.0f - t;

        // Set the first and second scale for the interpolation
        float scale0 = 1.0f - t;
        float scale1 = t;

        if(1.0f - cosTheta > 0.1f)
        {
            // We are using spherical interpolation.
            float theta = (float)Math.acos(cosTheta);
            float sinTheta = (float)Math.sin(theta);
            scale0 = (float)Math.sin(theta * beta) / sinTheta;
            scale1 = (float)Math.sin(theta * t) / sinTheta;
        }

        // Interpolation.
        qInterpolated.x = scale0 * q1.x + scale1 * q2.x;
        qInterpolated.y = scale0 * q1.y + scale1 * q2.y;
        qInterpolated.z = scale0 * q1.z + scale1 * q2.z;
        qInterpolated.w = scale0 * q1.w + scale1 * q2.w;

        return qInterpolated;
    }

    public static Quaternion conjugate(Quaternion q1)
    {
        return new Quaternion(-q1.x, -q1.y, -q1.z, q1.w);
    }

    public static boolean isEqual(Quaternion q1, Quaternion q2)
    {
        if(q1.x == q2.x && q1.y == q2.y && q1.z == q2.z && q1.w == q2.w)
        {
            return true;
        }else{
            return false;
        }

    }

    public static Quaternion slerp2(Quaternion a, Quaternion b, float t) {

        Quaternion result = new Quaternion();
        float cosom = a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w;
        float t1 = 1.0f - t;

        // if the two quaternions are close, just use linear interpolation
        if (cosom >= 0.95f) {
            result.x = a.x * t1 + b.x * t;
            result.y = a.y * t1 + b.y * t;
            result.z = a.z * t1 + b.z * t;
            result.w = a.w * t1 + b.w * t;
            return result;
        }

        // the quaternions are nearly opposite, we can pick any axis normal to a,b
        // to do the rotation
        if (cosom <= -0.99f) {
            result.x = 0.5f * (a.x + b.x);
            result.y = 0.5f * (a.y + b.y);
            result.z = 0.5f * (a.z + b.z);
            result.w = 0.5f * (a.w + b.w);
            return result;
        }

        // cosom is now withion range of acos, do a SLERP
        float sinom = (float)Math.sqrt(1.0f - cosom * cosom);
        float omega = (float)Math.acos(cosom);

        float scla = (float)Math.sin(t1 * omega) / sinom;
        float sclb = (float)Math.sin( t * omega) / sinom;

        result.x = a.x * scla + b.x * sclb;
        result.y = a.y * scla + b.y * sclb;
        result.z = a.z * scla + b.z * sclb;
        result.w = a.w * scla + b.w * sclb;
        return result;
    }

    /**
     * <code>slerp</code> sets this quaternion's value as an interpolation
     * between two other quaternions.
     *
     * @param q1
     *            the first quaternion.
     * @param q2
     *            the second quaternion.
     * @param t1
     *            the amount to interpolate between the two quaternions.
     */
    public static Quaternion slerp1(Quaternion q1, Quaternion q2, float t1)
    {
        Quaternion result = new Quaternion();

        // Create a local quaternion to store the interpolated quaternion
        if (q1.x == q2.x && q1.y == q2.y && q1.z == q2.z && q1.w == q2.w) {
            result.set(q1);
            return result;
        }

        float result1 = (q1.x * q2.x) + (q1.y * q2.y) + (q1.z * q2.z)
                + (q1.w * q2.w);

        if (result1 < 0.0f) {
            // Negate the second quaternion and the result of the dot product
            q2.x = -q2.x;
            q2.y = -q2.y;
            q2.z = -q2.z;
            q2.w = -q2.w;
            result1 = -result1;
        }

        // Set the first and second scale for the interpolation
        float scale0 = 1 - t1;
        float scale1 = t1;

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if ((1 - result1) > 0.1f) {// Get the angle between the 2 quaternions,
            // and then store the sin() of that angle
            float theta = (float)Math.acos(result1);
            float invSinTheta = 1f / (float)Math.sin(theta);

            // Calculate the scale for q1 and q2, according to the angle and
            // it's sine value
            scale0 = (float)Math.sin((1 - t1) * theta) * invSinTheta;
            scale1 = (float)Math.sin((t1 * theta)) * invSinTheta;
        }

        // Calculate the x, y, z and w values for the quaternion by using a
        // special
        // form of linear interpolation for quaternions.
        result.x = (scale0 * q1.x) + (scale1 * q2.x);
        result.y = (scale0 * q1.y) + (scale1 * q2.y);
        result.z = (scale0 * q1.z) + (scale1 * q2.z);
        result.w = (scale0 * q1.w) + (scale1 * q2.w);

        // Return the interpolated quaternion
        return result;
    }

//    public static Quaternion shortMix(Quaternion x, Quaternion y, float a)
//    {
//        if(a <= typename detail::tquat<T>::value_type(0)) return x;
//        if(a >= typename detail::tquat<T>::value_type(1)) return y;
//
//        T fCos = dot(x, y);
//        detail::tquat<T> y2(y); //BUG!!! tquat<T> y2;
//        if(fCos < T(0))
//        {
//            y2 = -y;
//            fCos = -fCos;
//        }
//
//        //if(fCos > 1.0f) // problem
//        T k0, k1;
//        if(fCos > T(0.9999))
//        {
//            k0 = T(1) - a;
//            k1 = T(0) + a; //BUG!!! 1.0f + a;
//        }
//        else
//        {
//            T fSin = sqrt(T(1) - fCos * fCos);
//            T fAngle = atan(fSin, fCos);
//            T fOneOverSin = T(1) / fSin;
//            k0 = sin((T(1) - a) * fAngle) * fOneOverSin;
//            k1 = sin((T(0) + a) * fAngle) * fOneOverSin;
//        }
//
//        return detail::tquat<T>(
//                k0 * x.w + k1 * y2.w,
//        k0 * x.x + k1 * y2.x,
//                k0 * x.y + k1 * y2.y,
//                k0 * x.z + k1 * y2.z);
//    }

//    template <typename T>
//    GLM_FUNC_QUALIFIER detail::tquat<T> mix
//        (
//                detail::tquat<T> const & x,
//                detail::tquat<T> const & y,
//                typename detail::tquat<T>::value_type const & a
//        )
//{
//    if(a <= typename detail::tquat<T>::value_type(0)) return x;
//    if(a >= typename detail::tquat<T>::value_type(1)) return y;
//
//    float fCos = dot(x, y);
//    detail::tquat<T> y2(y); //BUG!!! tquat<T> y2;
//    if(fCos < typename detail::tquat<T>::value_type(0))
//    {
//        y2 = -y;
//        fCos = -fCos;
//    }
//
//    //if(fCos > 1.0f) // problem
//    float k0, k1;
//    if(fCos > typename detail::tquat<T>::value_type(0.9999))
//    {
//        k0 = typename detail::tquat<T>::value_type(1) - a;
//        k1 = typename detail::tquat<T>::value_type(0) + a; //BUG!!! 1.0f + a;
//    }
//    else
//    {
//        typename detail::tquat<T>::value_type fSin = sqrt(T(1) - fCos * fCos);
//        typename detail::tquat<T>::value_type fAngle = atan(fSin, fCos);
//        typename detail::tquat<T>::value_type fOneOverSin = T(1) / fSin;
//        k0 = sin((typename detail::tquat<T>::value_type(1) - a) * fAngle) * fOneOverSin;
//        k1 = sin((typename detail::tquat<T>::value_type(0) + a) * fAngle) * fOneOverSin;
//    }
//
//    return detail::tquat<T>(
//            k0 * x.w + k1 * y2.w,
//    k0 * x.x + k1 * y2.x,
//            k0 * x.y + k1 * y2.y,
//            k0 * x.z + k1 * y2.z);
//}

    public static Matrix4f quatToMatrix4f(Quaternion q)
    {
        Matrix4f matrix = new Matrix4f();
        matrix.m00 = 1.0f - 2.0f * ( q.getY() * q.getY() + q.getZ() * q.getZ() );
        matrix.m01 = 2.0f * (q.getX() * q.getY() + q.getZ() * q.getW());
        matrix.m02 = 2.0f * (q.getX() * q.getZ() - q.getY() * q.getW());
        matrix.m03 = 0.0f;

        // Second row
        matrix.m10 = 2.0f * ( q.getX() * q.getY() - q.getZ() * q.getW() );
        matrix.m11 = 1.0f - 2.0f * ( q.getX() * q.getX() + q.getZ() * q.getZ() );
        matrix.m12 = 2.0f * (q.getZ() * q.getY() + q.getX() * q.getW() );
        matrix.m13 = 0.0f;

        // Third row
        matrix.m20 = 2.0f * ( q.getX() * q.getZ() + q.getY() * q.getW() );
        matrix.m21 = 2.0f * ( q.getY() * q.getZ() - q.getX() * q.getW() );
        matrix.m22 = 1.0f - 2.0f * ( q.getX() * q.getX() + q.getY() * q.getY() );
        matrix.m23 = 0.0f;

        // Fourth row
        matrix.m30 = 0;
        matrix.m31 = 0;
        matrix.m32 = 0;
        matrix.m33 = 1.0f;

        return matrix;
    }

    public static FloatBuffer quatToMatrix4fFloatBuf(Quaternion q)
    {
        FloatBuffer fb = GLUtils.createFloatBuffer(4*4);
        Matrix4f mat4f = quatToMatrix4f(q);
        mat4f.store(fb);
        fb.flip();
        return fb;
    }
}
