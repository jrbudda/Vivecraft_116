package org.vivecraft.utils;

import org.vivecraft.utils.lwjgl.Matrix3f;
import org.vivecraft.utils.lwjgl.Matrix4f;
import net.minecraft.util.math.Vec3d;


/**
 *
 * @author Techjar
 */
public class Quaternion {
	public float w;
	public float x;
	public float y;
	public float z;

	public Quaternion() {
		this.w = 1;
	}

	public Quaternion(float w, float x, float y, float z) {
		this.w = w;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Quaternion(Quaternion other) {
		this.w = other.w;
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
	}

	public Quaternion(Vector3 vector, float rotation) {
		rotation = (float)Math.toRadians(rotation);
		float sinRot = (float)Math.sin(rotation / 2);
		w = (float)Math.cos(rotation / 2);
		x = vector.x * sinRot;
		y = vector.y * sinRot;
		z = vector.z * sinRot;
	}
	public Quaternion(Axis axis, float rotation) {
		this(axis.getVector(), rotation);
	}

	public Quaternion(float pitch, float yaw, float roll, Angle.Order order) {
		// X Pitch Y Yaw Z Roll
		// YXZ Order
		Quaternion quatX = new Quaternion(new Vector3(1, 0, 0), pitch);
		Quaternion quatY = new Quaternion(new Vector3(0, 1, 0), yaw);
		Quaternion quatZ = new Quaternion(new Vector3(0, 0, 1), roll);
		Quaternion quat = null;
		switch (order) {
			case XYZ: quat = quatX.multiply(quatY).multiply(quatZ); break;
			case ZYX: quat = quatZ.multiply(quatY).multiply(quatX); break;
			case YXZ: quat = quatY.multiply(quatX).multiply(quatZ); break;
			case ZXY: quat = quatZ.multiply(quatX).multiply(quatY); break;
			case YZX: quat = quatY.multiply(quatZ).multiply(quatX); break;
			case XZY: quat = quatX.multiply(quatZ).multiply(quatY); break;
		}
		w = quat.w;
		x = quat.x;
		y = quat.y;
		z = quat.z;
	}

	public Quaternion(float pitch, float yaw, float roll) {
		this(pitch, yaw, roll, Angle.Order.YXZ);
	}

	public Quaternion(Angle angle) {
		this(angle.getPitch(), angle.getYaw(), angle.getRoll(), angle.getOrder());
	}

	public Quaternion(Matrix3f matrix) {
		this(matrix.m00, matrix.m01, matrix.m02, matrix.m10, matrix.m11, matrix.m12, matrix.m20, matrix.m21, matrix.m22);
	}

	public Quaternion(Matrix4f matrix) {
		this(matrix.m00, matrix.m01, matrix.m02, matrix.m10, matrix.m11, matrix.m12, matrix.m20, matrix.m21, matrix.m22);
	}
	
	public Quaternion(org.vivecraft.utils.Matrix4f matrix) {
		this(matrix.M[0][0], matrix.M[0][1], matrix.M[0][2], matrix.M[1][0], matrix.M[1][1], matrix.M[1][2], matrix.M[2][0], matrix.M[2][1], matrix.M[2][2]);
	}
	

//	public Quaternion(org.vivecraft.utils.Matrix4f matrix){
//		this(matrix.M[0][0],matrix.M[0][1],matrix.M[0][2],matrix.M[1][0],matrix.M[1][1],matrix.M[1][2],matrix.M[2][0],matrix.M[2][1],matrix.M[2][2]);
//	}

	private Quaternion(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {
		float s;
		float tr = m00 + m11 + m22;
		if (tr >= 0.0) {
			s = (float)Math.sqrt(tr + 1.0);
			w = s * 0.5f;
			s = 0.5f / s;
			x = (m21 - m12) * s;
			y = (m02 - m20) * s;
			z = (m10 - m01) * s;
		} else {
			float max = Math.max(Math.max(m00, m11), m22);
			if (max == m00) {
				s = (float)Math.sqrt(m00 - (m11 + m22) + 1.0);
				x = s * 0.5f;
				s = 0.5f / s;
				y = (m01 + m10) * s;
				z = (m20 + m02) * s;
				w = (m21 - m12) * s;
			} else if (max == m11) {
				s = (float)Math.sqrt(m11 - (m22 + m00) + 1.0);
				y = s * 0.5f;
				s = 0.5f / s;
				z = (m12 + m21) * s;
				x = (m01 + m10) * s;
				w = (m02 - m20) * s;
			} else {
				s = (float)Math.sqrt(m22 - (m00 + m11) + 1.0);
				z = s * 0.5f;
				s = 0.5f / s;
				x = (m20 + m02) * s;
				y = (m12 + m21) * s;
				w = (m10 - m01) * s;
			}
		}
	}

	public Quaternion copy() {
		return new Quaternion(this);
	}

	public float getW() {
		return w;
	}

	public void setW(float w) {
		this.w = w;
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public float getZ() {
		return z;
	}

	public void setZ(float z) {
		this.z = z;
	}

	public void set(Quaternion other) {
		this.w = other.w;
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
	}

	public void normalize() {
		float norm = (float)Math.sqrt(w * w + x * x + y * y + z * z);
		if (norm > 0) {
			w /= norm;
			x /= norm;
			y /= norm;
			z /= norm;
		} else {
			w = 1;
			x = 0;
			y = 0;
			z = 0;
		}
	}

	public Quaternion normalized() {
		float newW, newX, newY, newZ;
		float norm = (float)Math.sqrt(w * w + x * x + y * y + z * z);
		if (norm > 0) {
			newW = w / norm;
			newX = x / norm;
			newY = y / norm;
			newZ = z / norm;
		} else {
			newW = 1;
			newX = 0;
			newY = 0;
			newZ = 0;
		}
		return new Quaternion(newW, newX, newY, newZ);
	}

	public Angle toEuler(){
		Angle eulerAngles = new Angle();
		Quaternion q = this;
		eulerAngles.setYaw((float)Math.toDegrees(Math.atan2( 2*(q.x*q.z + q.w*q.y), q.w*q.w - q.x*q.x - q.y*q.y + q.z*q.z )));
		eulerAngles.setPitch((float)Math.toDegrees(Math.asin ( -2*(q.y*q.z - q.w*q.x) )));
		eulerAngles.setRoll((float)Math.toDegrees(Math.atan2( 2*(q.x*q.y + q.w*q.z), q.w*q.w - q.x*q.x + q.y*q.y - q.z*q.z )));

		return eulerAngles;
	}
	

	public Quaternion rotate(Axis axis, float degrees, boolean local) {
		if (local) {
			return this.multiply(new Quaternion(axis, degrees));
		} else {
			Matrix4f matrix = getMatrix();
			matrix.rotate((float)Math.toRadians(degrees), Utils.convertVector(axis.getVector()));
			return new Quaternion(matrix);
		}
	}

	public Quaternion multiply(Quaternion other) {
		float newW = w * other.w - x * other.x - y * other.y - z * other.z;
		float newX = w * other.x + other.w * x + y * other.z - z * other.y;
		float newY = w * other.y + other.w * y - x * other.z + z * other.x;
		float newZ = w * other.z + other.w * z + x * other.y - y * other.x;
		return new Quaternion(newW, newX, newY, newZ);
	}

	public Matrix4f getMatrix() {
		Matrix4f matrix = new Matrix4f();
		float sqw = w * w;
		float sqx = x * x;
		float sqy = y * y;
		float sqz = z * z;

		// invs (inverse square length) is only required if quaternion is not already normalised
		float invs = 1 / (sqx + sqy + sqz + sqw);
		matrix.m00 = (sqx - sqy - sqz + sqw) * invs; // since sqw + sqx + sqy + sqz =1/invs*invs
		matrix.m11 = (-sqx + sqy - sqz + sqw) * invs;
		matrix.m22 = (-sqx - sqy + sqz + sqw) * invs;

		float tmp1 = x * y;
		float tmp2 = z * w;
		matrix.m10 = 2 * (tmp1 + tmp2) * invs;
		matrix.m01 = 2 * (tmp1 - tmp2) * invs;

		tmp1 = x * z;
		tmp2 = y * w;
		matrix.m20 = 2 * (tmp1 - tmp2) * invs;
		matrix.m02 = 2 * (tmp1 + tmp2) * invs;

		tmp1 = y * z;
		tmp2 = x * w;
		matrix.m21 = 2 * (tmp1 + tmp2) * invs;
		matrix.m12 = 2 * (tmp1 - tmp2) * invs;

		return matrix;
	}

	public Quaternion inverse(){
		return new Quaternion(w,-x,-y,-z);
	}

	public static Quaternion createFromToVector(Vector3 from, Vector3 to){
		Vector3 cross = from.cross(to);
		float w=(float) (Math.sqrt(Math.pow(from.length(), 2) * Math.pow(to.length(), 2)) + from.dot(to));

		return new Quaternion(w,cross.x,cross.y,cross.z).normalized();
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 23 * hash + Float.floatToIntBits(this.w);
		hash = 23 * hash + Float.floatToIntBits(this.x);
		hash = 23 * hash + Float.floatToIntBits(this.y);
		hash = 23 * hash + Float.floatToIntBits(this.z);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Quaternion other = (Quaternion)obj;
		if (Float.floatToIntBits(this.w) != Float.floatToIntBits(other.w)) {
			return false;
		}
		if (Float.floatToIntBits(this.x) != Float.floatToIntBits(other.x)) {
			return false;
		}
		if (Float.floatToIntBits(this.y) != Float.floatToIntBits(other.y)) {
			return false;
		}
		if (Float.floatToIntBits(this.z) != Float.floatToIntBits(other.z)) {
			return false;
		}
		return true;
	}

	public Vector3 multiply(Vector3 vec) {
	     float num = this.x * 2f;
	     float num2 = this.y * 2f;
	     float num3 = this.z * 2f;
	     float num4 = this.x * num;
	     float num5 = this.y * num2;
	     float num6 = this.z * num3;
	     float num7 = this.x * num2;
	     float num8 = this.x * num3;
	     float num9 = this.y * num3;
	     float num10 = this.w * num;
	     float num11 = this.w * num2;
	     float num12 = this.w * num3;
	     Vector3 result = new Vector3();
	     result.x = (1f - (num5 + num6)) * vec.x + (num7 - num12) * vec.y + (num8 + num11) * vec.z;
	     result.y = (num7 + num12) * vec.x + (1f - (num4 + num6)) * vec.y + (num9 - num10) * vec.z;
	     result.z = (num8 - num11) * vec.x + (num9 + num10) * vec.y + (1f - (num4 + num5)) * vec.z;
	     return result;
	 }

	 public Vec3d multiply(Vec3d vec){
		return multiply(new Vector3(vec)).toVec3d();
	 }
	
	
	@Override
	public String toString() {
		return "Quaternion{" + "w=" + w + ", x=" + x + ", y=" + y + ", z=" + z + '}';
	}
}