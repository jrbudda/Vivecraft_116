package org.vivecraft.utils;

import org.vivecraft.utils.lwjgl.Matrix4f;

/**
 *
 * @author Techjar
 */
public class Angle {
	public enum Order {
		XYZ,
		ZYX,
		YXZ,
		ZXY,
		YZX,
		XZY;
	}

	private float pitch;
	private float yaw;
	private float roll;
	private Order order;

	public Angle() {
		this.order = Order.YXZ;
	}

	public Angle(Order order) {
		this.order = order;
	}

	public Angle(float pitch, float yaw, float roll, Order order) {
		this.pitch = pitch;
		this.yaw = yaw;
		this.roll = roll;
		this.order = order;
	}

	public Angle(float pitch, float yaw, float roll) {
		this(pitch, yaw, roll, Order.YXZ);
	}

	public Angle(float pitch, float yaw) {
		this(pitch, yaw, 0, Order.YXZ);
	}

	public Angle(Angle other) {
		this.pitch = other.pitch;
		this.yaw = other.yaw;
		this.roll = other.roll;
		this.order = other.order;
	}

	/*public Angle(Quaternion quat) {
        float squareW = quat.getW() * quat.getW();
        float squareX = quat.getX() * quat.getX();
        float squareY = quat.getY() * quat.getY();
        float squareZ = quat.getZ() * quat.getZ();
        //float unit = squareX + squareY + squareZ + squareW;
        //float test = quat.getX() * quat.getZ() - quat.getW() * quat.getY();
        if (test > 0.499F * unit) { // singularity at north pole
            yaw = (float)Math.toDegrees(2 * Math.atan2(quat.getX(), quat.getW()));
            pitch = 90;
            roll = 0;
            return;
        }
        if (test < -0.499F * unit) { // singularity at south pole
            yaw = (float)Math.toDegrees(-2 * Math.atan2(quat.getX(), quat.getW()));
            pitch = -90;
            roll = 0;
            return;
        }
        //pitch = (float)Math.toDegrees(Math.atan2(2 * (quat.getY() * quat.getZ() + quat.getW() * quat.getX()), squareW - squareX - squareY + squareZ));
        //yaw = (float)Math.toDegrees(Math.asin(-2 * (quat.getX() * quat.getZ() - quat.getW() * quat.getY())));
        //roll = (float)Math.toDegrees(Math.atan2(2 * (quat.getX() * quat.getY() + quat.getW() * quat.getZ()), squareW + squareX - squareY - squareZ));
        roll = (float)Math.toDegrees(Math.atan2(2 * (quat.getX() * quat.getZ() + quat.getW() * quat.getY()), squareW - squareX - squareY + squareZ));
        yaw = (float)Math.toDegrees(Math.asin(-2 * (quat.getY() * quat.getZ() - quat.getW() * quat.getX())));
        pitch = (float)Math.toDegrees(Math.atan2(2 * (quat.getX() * quat.getY() + quat.getW() * quat.getZ()), squareW - squareX + squareY - squareZ));
        //yaw = (float)Math.toDegrees(Math.atan2(2 * (quat.getX() * quat.getZ() + quat.getW() * quat.getY()), squareW - squareX - squareY + squareZ));
        //pitch = (float)Math.toDegrees(Math.asin(-2 * (quat.getY() * quat.getZ() - quat.getW() * quat.getX())));
        //roll = (float)Math.toDegrees(Math.atan2(2 * (quat.getX() * quat.getY() + quat.getW() * quat.getZ()), squareW - squareX + squareY - squareZ));
    }*/

	public Angle copy() {
		return new Angle(this);
	}

	public void set(float pitch, float yaw, float roll) {
		this.pitch = pitch;
		this.yaw = yaw;
		this.roll = roll;
	}

	public void set(Angle other) {
		pitch = other.pitch;
		yaw = other.yaw;
		roll = other.roll;
	}

	public float getPitch() {
		return pitch;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	public float getYaw() {
		return yaw;
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
	}

	public float getRoll() {
		return roll;
	}

	public void setRoll(float roll) {
		this.roll = roll;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public Angle rotate(Axis axis, float degrees) {
		switch (axis) {
			case PITCH: return new Angle(pitch + degrees, yaw, roll);
			case YAW: return new Angle(pitch, yaw + degrees, roll);
			case ROLL: return new Angle(pitch, yaw, roll + degrees);
			default: break;
		}
		return new Angle(this);
	}

	public Angle add(Angle other) {
		return new Angle(pitch + other.pitch, yaw + other.yaw, roll + other.roll, order);
	}

	public Angle subtract(Angle other) {
		return new Angle(pitch - other.pitch, yaw - other.yaw, roll - other.roll, order);
	}

	public Matrix4f getMatrix() {
		/*double radPitch = Math.toRadians(pitch);
        double radYaw = Math.toRadians(yaw);
        double radRoll = Math.toRadians(roll);
        float sinPitch = (float)Math.sin(radPitch);
        float sinYaw = (float)Math.sin(radYaw);
        float sinRoll = (float)Math.sin(radRoll);
        float cosPitch = (float)Math.cos(radPitch);
        float cosYaw = (float)Math.cos(radYaw);
        float cosRoll = (float)Math.cos(radRoll);
        // X Pitch Y Roll Z Yaw
        // ZXY Order
        Matrix3f matrix = new Matrix3f();
        matrix.m00 = cosRoll * cosYaw - sinPitch * sinRoll * sinYaw;
        matrix.m01 = -cosPitch * sinYaw;
        matrix.m02 = cosYaw * sinRoll + cosRoll * sinPitch * sinYaw;
        matrix.m10 = cosYaw * sinPitch * sinRoll + cosRoll * sinPitch;
        matrix.m11 = cosPitch * cosYaw;
        matrix.m12 = -cosRoll * cosYaw * sinPitch + sinRoll * sinYaw;
        matrix.m20 = -cosPitch * sinRoll;
        matrix.m21 = sinPitch;
        matrix.m22 = cosPitch * cosRoll;
        return matrix;*/
		//return new Quaternion(this).getMatrix(); // lol this is easier
		return new Quaternion(this).getMatrix();
	}

	public Vector3 forward() {
		/*double radPitch = Math.toRadians(pitch);
        double radYaw = Math.toRadians(yaw);
        double sinPitch = Math.sin(radPitch);
        double cosYaw = Math.cos(radYaw);
        //double cosPitch = Math.cos(radPitch);
        //return new Vector3((float)(Math.cos(radYaw) * sinPitch), (float)(Math.sin(radYaw) * sinPitch), (float)Math.cos(radPitch));
        //return new Vector3((float)Math.sin(radYaw), -(float)(Math.sin(radPitch) * cosYaw), -(float)(Math.cos(radPitch) * cosYaw));
        //return new Vector3((float)(Math.cos(radYaw) * cosPitch), (float)(Math.sin(radYaw) * cosPitch), -(float)Math.sin(radPitch));
        return new Vector3(-(float)(Math.sin(radYaw) * sinPitch), (float)Math.cos(radPitch), -(float)(Math.cos(radYaw) * sinPitch));*/
		return new Vector3(0, 0, -1).multiply(getMatrix());
	}

	public Vector3 up() {
		return new Vector3(0, 1, 0).multiply(getMatrix());
	}

	public Vector3 right() {
		return new Vector3(1, 0, 0).multiply(getMatrix());
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 89 * hash + Float.floatToIntBits(this.pitch);
		hash = 89 * hash + Float.floatToIntBits(this.yaw);
		hash = 89 * hash + Float.floatToIntBits(this.roll);
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
		final Angle other = (Angle)obj;
		if (Float.floatToIntBits(this.pitch) != Float.floatToIntBits(other.pitch)) {
			return false;
		}
		if (Float.floatToIntBits(this.yaw) != Float.floatToIntBits(other.yaw)) {
			return false;
		}
		if (Float.floatToIntBits(this.roll) != Float.floatToIntBits(other.roll)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Angle{" + "pitch=" + pitch + ", yaw=" + yaw + ", roll=" + roll + '}';
	}
}