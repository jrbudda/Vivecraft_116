package org.vivecraft.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Matrix4f {
   public float[][] M = new float[4][4];

   public Matrix4f(float m11, float m12, float m13, float m14, float m21, float m22, float m23, float m24, float m31, float m32, float m33, float m34, float m41, float m42, float m43, float m44) {
      this.M[0][0] = m11;
      this.M[0][1] = m12;
      this.M[0][2] = m13;
      this.M[0][3] = m14;
      this.M[1][0] = m21;
      this.M[1][1] = m22;
      this.M[1][2] = m23;
      this.M[1][3] = m24;
      this.M[2][0] = m31;
      this.M[2][1] = m32;
      this.M[2][2] = m33;
      this.M[2][3] = m34;
      this.M[3][0] = m41;
      this.M[3][1] = m42;
      this.M[3][2] = m43;
      this.M[3][3] = m44;
   }

   public Matrix4f(float m11, float m12, float m13, float m21, float m22, float m23, float m31, float m32, float m33) {
      this.M[0][0] = m11;
      this.M[0][1] = m12;
      this.M[0][2] = m13;
      this.M[0][3] = 0.0F;
      this.M[1][0] = m21;
      this.M[1][1] = m22;
      this.M[1][2] = m23;
      this.M[1][3] = 0.0F;
      this.M[2][0] = m31;
      this.M[2][1] = m32;
      this.M[2][2] = m33;
      this.M[2][3] = 0.0F;
      this.M[3][0] = 0.0F;
      this.M[3][1] = 0.0F;
      this.M[3][2] = 0.0F;
      this.M[3][3] = 1.0F;
   }

   public Matrix4f(Quaternion q) {
      float ww = q.w * q.w;
      float xx = q.x * q.x;
      float yy = q.y * q.y;
      float zz = q.z * q.z;
      this.M[0][0] = ww + xx - yy - zz;
      this.M[0][1] = 2.0F * (q.x * q.y - q.w * q.z);
      this.M[0][2] = 2.0F * (q.x * q.z + q.w * q.y);
      this.M[0][3] = 0.0F;
      this.M[1][0] = 2.0F * (q.x * q.y + q.w * q.z);
      this.M[1][1] = ww - xx + yy - zz;
      this.M[1][2] = 2.0F * (q.y * q.z - q.w * q.x);
      this.M[1][3] = 0.0F;
      this.M[2][0] = 2.0F * (q.x * q.z - q.w * q.y);
      this.M[2][1] = 2.0F * (q.y * q.z + q.w * q.x);
      this.M[2][2] = ww - xx - yy + zz;
      this.M[2][3] = 0.0F;
      this.M[3][0] = 0.0F;
      this.M[3][1] = 0.0F;
      this.M[3][2] = 0.0F;
      this.M[3][3] = 1.0F;
   }

   public Matrix4f() {
      this.SetIdentity();
   }

   void SetIdentity() {
      this.M[0][0] = this.M[1][1] = this.M[2][2] = this.M[3][3] = 1.0F;
      this.M[0][1] = this.M[1][0] = this.M[2][3] = this.M[3][1] = 0.0F;
      this.M[0][2] = this.M[1][2] = this.M[2][0] = this.M[3][2] = 0.0F;
      this.M[0][3] = this.M[1][3] = this.M[2][1] = this.M[3][0] = 0.0F;
   }

   Matrix4f(Matrix4f c) {
      for(int i = 0; i < 4; ++i) {
         for(int j = 0; j < 4; ++j) {
            this.M[i][j] = c.M[i][j];
         }
      }

   }

   public Matrix4f inverted() {
      float det = this.Determinant();
      return det == 0.0F ? null : this.Adjugated().Multiply(1.0F / det);
   }

   Matrix4f Multiply(float s) {
      Matrix4f d = new Matrix4f(this);

      for(int i = 0; i < 4; ++i) {
         for(int j = 0; j < 4; ++j) {
            float[] var10000 = d.M[i];
            var10000[j] *= s;
         }
      }

      return d;
   }

   public static Matrix4f multiply(Matrix4f a, Matrix4f b) {
      int i = 0;
      Matrix4f d = new Matrix4f();

      do {
         d.M[i][0] = a.M[i][0] * b.M[0][0] + a.M[i][1] * b.M[1][0] + a.M[i][2] * b.M[2][0] + a.M[i][3] * b.M[3][0];
         d.M[i][1] = a.M[i][0] * b.M[0][1] + a.M[i][1] * b.M[1][1] + a.M[i][2] * b.M[2][1] + a.M[i][3] * b.M[3][1];
         d.M[i][2] = a.M[i][0] * b.M[0][2] + a.M[i][1] * b.M[1][2] + a.M[i][2] * b.M[2][2] + a.M[i][3] * b.M[3][2];
         d.M[i][3] = a.M[i][0] * b.M[0][3] + a.M[i][1] * b.M[1][3] + a.M[i][2] * b.M[2][3] + a.M[i][3] * b.M[3][3];
         ++i;
      } while(i < 4);

      return d;
   }

   public Matrix4f transposed() {
      return new Matrix4f(this.M[0][0], this.M[1][0], this.M[2][0], this.M[3][0], this.M[0][1], this.M[1][1], this.M[2][1], this.M[3][1], this.M[0][2], this.M[1][2], this.M[2][2], this.M[3][2], this.M[0][3], this.M[1][3], this.M[2][3], this.M[3][3]);
   }

   float SubDet(int[] rows, int[] cols) {
      return this.M[rows[0]][cols[0]] * (this.M[rows[1]][cols[1]] * this.M[rows[2]][cols[2]] - this.M[rows[1]][cols[2]] * this.M[rows[2]][cols[1]]) - this.M[rows[0]][cols[1]] * (this.M[rows[1]][cols[0]] * this.M[rows[2]][cols[2]] - this.M[rows[1]][cols[2]] * this.M[rows[2]][cols[0]]) + this.M[rows[0]][cols[2]] * (this.M[rows[1]][cols[0]] * this.M[rows[2]][cols[1]] - this.M[rows[1]][cols[1]] * this.M[rows[2]][cols[0]]);
   }

   float Cofactor(int I, int J) {
      int[][] indices = new int[][]{{1, 2, 3}, {0, 2, 3}, {0, 1, 3}, {0, 1, 2}};
      return (I + J & 1) != 0 ? -this.SubDet(indices[I], indices[J]) : this.SubDet(indices[I], indices[J]);
   }

   float Determinant() {
      return this.M[0][0] * this.Cofactor(0, 0) + this.M[0][1] * this.Cofactor(0, 1) + this.M[0][2] * this.Cofactor(0, 2) + this.M[0][3] * this.Cofactor(0, 3);
   }

   Matrix4f Adjugated() {
      return new Matrix4f(this.Cofactor(0, 0), this.Cofactor(1, 0), this.Cofactor(2, 0), this.Cofactor(3, 0), this.Cofactor(0, 1), this.Cofactor(1, 1), this.Cofactor(2, 1), this.Cofactor(3, 1), this.Cofactor(0, 2), this.Cofactor(1, 2), this.Cofactor(2, 2), this.Cofactor(3, 2), this.Cofactor(0, 3), this.Cofactor(1, 3), this.Cofactor(2, 3), this.Cofactor(3, 3));
   }

   public FloatBuffer toFloatBuffer() {
      FloatBuffer buf = ByteBuffer.allocateDirect(16*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
      buf.put(this.M[0][0]);
      buf.put(this.M[0][1]);
      buf.put(this.M[0][2]);
      buf.put(this.M[0][3]);
      buf.put(this.M[1][0]);
      buf.put(this.M[1][1]);
      buf.put(this.M[1][2]);
      buf.put(this.M[1][3]);
      buf.put(this.M[2][0]);
      buf.put(this.M[2][1]);
      buf.put(this.M[2][2]);
      buf.put(this.M[2][3]);
      buf.put(this.M[3][0]);
      buf.put(this.M[3][1]);
      buf.put(this.M[3][2]);
      buf.put(this.M[3][3]);
      buf.flip();
      return buf;
   }

   public static Matrix4f rotationY(float angle) {
      double sina = Math.sin((double)angle);
      double cosa = Math.cos((double)angle);
      return new Matrix4f((float)cosa, 0.0F, (float)sina, 0.0F, 1.0F, 0.0F, -((float)sina), 0.0F, (float)cosa);
   }

   public Vector3 transform(Vector3 v) {
      float rcpW = 1.0F / (this.M[3][0] * v.x + this.M[3][1] * v.y + this.M[3][2] * v.z + this.M[3][3]);
      return new Vector3((this.M[0][0] * v.x + this.M[0][1] * v.y + this.M[0][2] * v.z + this.M[0][3]) * rcpW, (this.M[1][0] * v.x + this.M[1][1] * v.y + this.M[1][2] * v.z + this.M[1][3]) * rcpW, (this.M[2][0] * v.x + this.M[2][1] * v.y + this.M[2][2] * v.z + this.M[2][3]) * rcpW);
   }

   public static Matrix4f translation(Vector3 v) {
      Matrix4f t = new Matrix4f();
      t.M[0][3] = v.x;
      t.M[1][3] = v.y;
      t.M[2][3] = v.z;
      return t;
   }

   public static Matrix4f lookAtRH(Vector3 eye, Vector3 at, Vector3 up) {
      Vector3 z = eye.subtract(at).normalized();
      Vector3 x = up.cross(z).normalized();
      Vector3 y = z.cross(x);
      Matrix4f m = new Matrix4f(x.x, x.y, x.z, -x.dot(eye), y.x, y.y, y.z, -y.dot(eye), z.x, z.y, z.z, -z.dot(eye), 0.0F, 0.0F, 0.0F, 1.0F);
      return m;
   }
}
