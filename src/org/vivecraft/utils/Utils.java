package org.vivecraft.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;
import org.apache.logging.log4j.LogManager;
import org.vivecraft.render.VRShaders;
import org.vivecraft.tweaker.MinecriftClassTransformer;
import org.vivecraft.utils.lwjgl.Matrix3f;
import org.vivecraft.utils.lwjgl.Matrix4f;
import org.vivecraft.utils.lwjgl.Vector2f;
import org.vivecraft.utils.lwjgl.Vector3f;
import org.vivecraft.utils.lwjgl.Vector4f;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.mojang.blaze3d.platform.GlStateManager;

import jopenvr.HmdMatrix34_t;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.io.IOUtils;

public class Utils
{
	// Magic list from a C# snippet, don't question it
	private static final char[] illegalChars = {34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47};

	static {
		// Needs to be sorted for binary search
		Arrays.sort(illegalChars);
	}

	public static String sanitizeFileName(String fileName) {
		StringBuilder sanitized = new StringBuilder();
		for (int i = 0; i < fileName.length(); i++) {
			char ch = fileName.charAt(i);
			if (Arrays.binarySearch(illegalChars, ch) < 0)
				sanitized.append(ch);
			else
				sanitized.append('_');
		}
		return sanitized.toString();
	}

	public static org.vivecraft.utils.Vector3 convertToOVRVector(Vector3f vector) {
		return new Vector3(vector.x, vector.y, vector.z);
	}

	public static org.vivecraft.utils.Vector3 convertToOVRVector(Vec3d vector) {
		return new Vector3((float)vector.x, (float)vector.y, (float)vector.z);
	}
	
	public static Matrix4f convertOVRMatrix(org.vivecraft.utils.Matrix4f matrix) {
		Matrix4f mat = new Matrix4f();
		mat.m00 = matrix.M[0][0];
		mat.m01 = matrix.M[0][1];
		mat.m02 = matrix.M[0][2];
		mat.m03 = matrix.M[0][3];
		mat.m10 = matrix.M[1][0];
		mat.m11 = matrix.M[1][1];
		mat.m12 = matrix.M[1][2];
		mat.m13 = matrix.M[1][3];
		mat.m20 = matrix.M[2][0];
		mat.m21 = matrix.M[2][1];
		mat.m22 = matrix.M[2][2];
		mat.m23 = matrix.M[2][3];
		mat.m30 = matrix.M[3][0];
		mat.m31 = matrix.M[3][1];
		mat.m32 = matrix.M[3][2];
		mat.m33 = matrix.M[3][3];
		mat.transpose(mat);
		return mat;
	}
	
	public static org.vivecraft.utils.Matrix4f convertToOVRMatrix(Matrix4f matrixIn) {
		Matrix4f matrix = new Matrix4f();
		matrixIn.transpose(matrix);
		org.vivecraft.utils.Matrix4f mat = new org.vivecraft.utils.Matrix4f();
		mat.M[0][0] = matrix.m00;
		mat.M[0][1] = matrix.m01;
		mat.M[0][2] = matrix.m02;
		mat.M[0][3] = matrix.m03;
		mat.M[1][0] = matrix.m10;
		mat.M[1][1] = matrix.m11;
		mat.M[1][2] = matrix.m12;
		mat.M[1][3] = matrix.m13;
		mat.M[2][0] = matrix.m20;
		mat.M[2][1] = matrix.m21;
		mat.M[2][2] = matrix.m22;
		mat.M[2][3] = matrix.m23;
		mat.M[3][0] = matrix.m30;
		mat.M[3][1] = matrix.m31;
		mat.M[3][2] = matrix.m32;
		mat.M[3][3] = matrix.m33;
		return mat;
	}
	
	public static HmdMatrix34_t convertToMatrix34(Matrix4f matrix) {
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

	public static double lerp(double from, double to, double percent){
		return from+(to-from)*percent;
	}

	public static double lerpMod(double from, double to, double percent, double mod){
		if(Math.abs(to-from) < mod/2){
			return from+(to-from)*percent;
		}else{
			return from+(to-from -Math.signum(to-from)*mod)*percent;
		}
	}

	public static double absLerp(double value, double target, double stepSize){
		double step=Math.abs(stepSize);
		if (target-value>step){
			return value+step;
		}
		else if (target-value<-step){
			return value-step;
		}else {
			return target;
		}
	}
	
	public static void glRotate(Quaternion quaternion){
		GlStateManager.multMatrix(Convert.matrix(quaternion.inverse()).toMCMatrix4f());
	}
	
	public static Vector3f directionFromMatrix(Matrix4f matrix, float x, float y, float z) {
		Vector4f vec = new Vector4f(x, y, z, 0);
		Matrix4f.transform(matrix, vec, vec);
		vec.normalise(vec);
		return new Vector3f(vec.x, vec.y, vec.z);
	}
	
	/* With thanks to http://ramblingsrobert.wordpress.com/2011/04/13/java-word-wrap-algorithm/ */
    public static void wordWrap(String in, int length, ArrayList<String> wrapped)
    {
        String newLine = "\n";
        String wrappedLine;
        boolean quickExit = false;

        // Remove carriage return
        in = in.replace("\r", "");

        if(in.length() < length)
        {
            quickExit = true;
            length = in.length();
        }

        // Split on a newline if present
        if(in.substring(0, length).contains(newLine))
        {
            wrappedLine = in.substring(0, in.indexOf(newLine)).trim();
            wrapped.add(wrappedLine);
            wordWrap(in.substring(in.indexOf(newLine) + 1), length, wrapped);
            return;
        }
        else if (quickExit)
        {
            wrapped.add(in);
            return;
        }

        // Otherwise, split along the nearest previous space / tab / dash
        int spaceIndex = Math.max(Math.max( in.lastIndexOf(" ", length),
                in.lastIndexOf("\t", length)),
                in.lastIndexOf("-", length));

        // If no nearest space, split at length
        if(spaceIndex == -1)
            spaceIndex = length;

        // Split!
        wrappedLine = in.substring(0, spaceIndex).trim();
        wrapped.add(wrappedLine);
        wordWrap(in.substring(spaceIndex), length, wrapped);
    }
    
	public static Vector2f convertVector(Vector2 vector) {
		return new Vector2f(vector.getX(), vector.getY());
	}

	public static Vector2 convertVector(Vector2f vector) {
		return new Vector2(vector.getX(), vector.getY());
	}

	public static Vector3f convertVector(Vector3 vector) {
		return new Vector3f(vector.getX(), vector.getY(), vector.getZ());
	}

	public static Vector3 convertVector(Vector3f vector) {
		return new Vector3(vector.getX(), vector.getY(), vector.getZ());
	}

	public static Vector3 convertVector(Vec3d vector) {
		return new Vector3((float)vector.x, (float)vector.y, (float)vector.z);
	}

	public static Vector3f convertToVector3f(Vec3d vector) {
		return new Vector3f((float)vector.x, (float)vector.y, (float)vector.z);
	}

	public static Vec3d convertToVec3d(Vector3 vector) {
		return new Vec3d(vector.getX(), vector.getY(), vector.getZ());
	}

	public static Vec3d convertToVec3d(Vector3f vector) {
		return new Vec3d(vector.x, vector.y, vector.z);
	}

	public static Vector3f transformVector(Matrix4f matrix, Vector3f vector, boolean point) {
    	Vector4f vec = Matrix4f.transform(matrix, new Vector4f(vector.x, vector.y, vector.z, point ? 1 : 0), null);
    	return new Vector3f(vec.x, vec.y, vec.z);
	}

	public static Quaternion quatLerp(Quaternion start, Quaternion end, float fraction) {
		Quaternion quat = new Quaternion();
		quat.w = start.w + (end.w - start.w) * fraction;
		quat.x = start.x + (end.x - start.x) * fraction;
		quat.y = start.y + (end.y - start.y) * fraction;
		quat.z = start.z + (end.z - start.z) * fraction;
		return quat;
	}

	public static Matrix4f matrix3to4(Matrix3f matrix) {
		Matrix4f mat = new Matrix4f();
		mat.m00 = matrix.m00;
		mat.m01 = matrix.m01;
		mat.m02 = matrix.m02;
		mat.m10 = matrix.m10;
		mat.m11 = matrix.m11;
		mat.m12 = matrix.m12;
		mat.m20 = matrix.m20;
		mat.m21 = matrix.m21;
		mat.m22 = matrix.m22;
		return mat;
	}

	/**
	 * HSB to RGB conversion, pinched from java.awt.Color.
	 * @param hue (0..1.0f)
	 * @param saturation (0..1.0f)
	 * @param brightness (0..1.0f)
	 */
	public static GlStateManager.Color colorFromHSB(float hue, float saturation, float brightness) {
		GlStateManager.Color color = new GlStateManager.Color();
		if (saturation == 0.0F) {
			color.red = color.green = color.blue = brightness;
		} else {
			float f3 = (hue - (float) Math.floor(hue)) * 6F;
			float f4 = f3 - (float) Math.floor(f3);
			float f5 = brightness * (1.0F - saturation);
			float f6 = brightness * (1.0F - saturation * f4);
			float f7 = brightness * (1.0F - saturation * (1.0F - f4));
			switch ((int) f3) {
				case 0 :
					color.red = brightness;
					color.green = f7;
					color.blue = f5;
					break;
				case 1 :
					color.red = f6;
					color.green = brightness;
					color.blue = f5;
					break;
				case 2 :
					color.red = f5;
					color.green = brightness;
					color.blue = f7;
					break;
				case 3 :
					color.red = f5;
					color.green = f6;
					color.blue = brightness;
					break;
				case 4 :
					color.red = f7;
					color.green = f5;
					color.blue = brightness;
					break;
				case 5 :
					color.red = brightness;
					color.green = f5;
					color.blue = f6;
					break;
			}
		}
		return color;
	}

	public static InputStream getAssetAsStream(String name, boolean required) {
		InputStream is = null;
		try {
			is = VRShaders.class.getResourceAsStream("/assets/vivecraft/" + name);
			if (is == null) {
				//uhh debugging?
				Path dir = Paths.get(System.getProperty("user.dir")); // ../mcpxxx/jars/
				Path p5 = dir.getParent().resolve("src/resources/assets/vivecraft/" + name);
				if (!p5.toFile().exists()) {
					p5 = dir.getParent().getParent().resolve("resources/assets/vivecraft/" + name);
				}
				if (p5.toFile().exists()) {
					is = new FileInputStream(p5.toFile());
				}
			}
		} catch (Exception e) {
			handleAssetException(e, name, required);
		}

		return is;
	}

	public static byte[] loadAsset(String name, boolean required) {
		InputStream is = getAssetAsStream(name, required);

		try {
			byte[] out = IOUtils.toByteArray(is);
			is.close();
			return out;
		} catch (IOException e) {
			handleAssetException(e, name, required);
		}

		return null;
	}
	
	public static String loadAssetAsString(String name, boolean required) {
		return new String(loadAsset(name, required), Charsets.UTF_8);
	}

	public static void loadAssetToFile(String name, File file, boolean required) {
		InputStream is = getAssetAsStream(name, required);
		try {
			writeStreamToFile(is, file);
			is.close();
		} catch (IOException e) {
			handleAssetException(e, name, required);
		}
	}

	private static void handleAssetException(Throwable e, String name, boolean required) {
			if (required) {
				throw new RuntimeException("Failed to load asset: " + name, e);
			} else {
				System.out.println("Failed to load asset: " + name);
				e.printStackTrace();
			}
		}
	
	public static void unpackNatives(String directory) {
		try {
			new File("openvr/" + directory).mkdirs();
			// TODO: Uncomment this when OpenComposite supports SteamVR Input
			//if (new File("openvr/" + directory + "/opencomposite.ini").exists())
			//	return;
				
			// dev environment
			try {
				Path dir = Paths.get(System.getProperty("user.dir")); // ..\mcpxxx\jars\
				Path path = dir.getParent().resolve("src/resources/natives/" + directory);
				if (!path.toFile().exists()) {
					path = dir.getParent().getParent().resolve("resources/natives/" + directory);
				}
				if (path.toFile().exists()) { 
					System.out.println("Copying " + directory + " natives...");
					for (File file : path.toFile().listFiles()) {
						System.out.println(file.getName());
						Files.copy(file, new File("openvr/" + directory + "/" + file.getName()));
					}
					return;
				}
	
			} catch (Exception e) {
			}
			//
			
			//Live
			System.out.println("Unpacking " + directory + " natives...");
			ZipFile zip = MinecriftClassTransformer.findMinecriftZipFile();
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.getName().startsWith("natives/" + directory)) {
					String name = Paths.get(entry.getName()).getFileName().toString();
					System.out.println(name);
					writeStreamToFile(zip.getInputStream(entry), new File("openvr/" + directory + "/" + name));
				}
			}
			zip.close();
			//
		} catch (Exception e) {
			System.out.println("Failed to unpack natives");
			e.printStackTrace();
		}
	}
	
	public static void writeStreamToFile(InputStream is, File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		byte[] buffer = new byte[4096];
		int count;
		while ((count = is.read(buffer, 0, buffer.length)) != -1) {
			fos.write(buffer, 0, count);
		}
		fos.flush();
		fos.close();
		is.close();
	}

	public static String httpReadLine(String url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setReadTimeout(3000);
		conn.setUseCaches(false);
		conn.setDoInput(true);
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line = br.readLine();
		br.close();
		conn.disconnect();
		return line;
	}

	public static byte[] httpReadAll(String url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setReadTimeout(3000);
		conn.setUseCaches(false);
		conn.setDoInput(true);
		InputStream is = conn.getInputStream();
		ByteArrayOutputStream bout = new ByteArrayOutputStream(conn.getContentLength());
		byte[] bytes = new byte[4096];
		int count;
		while ((count = is.read(bytes, 0, bytes.length)) != -1) {
			bout.write(bytes, 0, count);
		}
		is.close();
		conn.disconnect();
		return bout.toByteArray();
	}

	public static String httpReadAllString(String url) throws IOException {
		return new String(httpReadAll(url), StandardCharsets.UTF_8);
	}

	public static void httpReadToFile(String url, File file, boolean writeWhenComplete) throws MalformedURLException, IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setReadTimeout(3000);
		conn.setUseCaches(false);
		conn.setDoInput(true);
		InputStream is = conn.getInputStream();
		if (writeWhenComplete) {
			ByteArrayOutputStream bout = new ByteArrayOutputStream(conn.getContentLength());
			byte[] bytes = new byte[4096];
			int count;
			while ((count = is.read(bytes, 0, bytes.length)) != -1) {
				bout.write(bytes, 0, count);
			}
			OutputStream out = new FileOutputStream(file);
			out.write(bout.toByteArray());
			out.flush();
			out.close();
		} else {
			OutputStream out = new FileOutputStream(file);
			byte[] bytes = new byte[4096];
			int count;
			while ((count = is.read(bytes, 0, bytes.length)) != -1) {
				out.write(bytes, 0, count);
			}
			out.flush();
			out.close();
		}
		is.close();
		conn.disconnect();
	}
	
    public static void httpReadToFile(String url, File file) throws IOException {
        httpReadToFile(url, file, false);
    }

	public static List<String> httpReadList(String url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setReadTimeout(3000);
		conn.setUseCaches(false);
		conn.setDoInput(true);
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		List<String> list = new ArrayList<String>();
		String line;
		while ((line = br.readLine()) != null) {
			list.add(line);
		}
		br.close();
		conn.disconnect();
		return list;
	}

	public static String getFileChecksum(File file, String algorithm) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
		InputStream is = new FileInputStream(file);
		byte[] bytes = new byte[(int)file.length()];
		is.read(bytes);
		is.close();
		MessageDigest md = MessageDigest.getInstance(algorithm);
		md.update(bytes);
		Formatter fmt = new Formatter();
		for (byte b : md.digest()) {
			fmt.format("%02x", b);
		}
		String str = fmt.toString();
		fmt.close();
		return str;
	}

	public static byte[] readFile(File file) throws FileNotFoundException, IOException {
		FileInputStream is = new FileInputStream(file);
		return readFully(is);
	}

	public static String readFileString(File file) throws FileNotFoundException, IOException {
		return new String(readFile(file), "UTF-8");
	}
	
	public static byte[] readFully(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] bytes = new byte[4096];
		int count;
		while ((count = in.read(bytes, 0, bytes.length)) != -1) {
			out.write(bytes, 0, count);
		}
		in.close();
		return out.toByteArray();
	}

	public static Quaternion slerp(Quaternion start, Quaternion end, float alpha) {
		final float d = start.x * end.x + start.y * end.y + start.z * end.z + start.w * end.w;
		float absDot = d < 0.f ? -d : d;

		// Set the first and second scale for the interpolation
		float scale0 = 1f - alpha;
		float scale1 = alpha;

		// Check if the angle between the 2 quaternions was big enough to
		// warrant such calculations
		if ((1 - absDot) > 0.1) {// Get the angle between the 2 quaternions,
			// and then store the sin() of that angle
			final float angle = (float)Math.acos(absDot);		
			final float invSinTheta = 1f / (float)Math.sin(angle);

			// Calculate the scale for q1 and q2, according to the angle and
			// it's sine value
			scale0 = ((float)Math.sin((1f - alpha) * angle) * invSinTheta);
			scale1 = ((float)Math.sin((alpha * angle)) * invSinTheta);
		}

		if (d < 0.f) scale1 = -scale1;
	
		// Calculate the x, y, z and w values for the quaternion by using a
		// special form of linear interpolation for quaternions.
		float x = (scale0 * start.x) + (scale1 * end.x);
		float y = (scale0 * start.y) + (scale1 * end.y);
		float z = (scale0 * start.z) + (scale1 * end.z);
		float w = (scale0 * start.w) + (scale1 * end.w);

		// Return the interpolated quaternion
		return new Quaternion(w, x, y, z);
	}
	public static Vec3d vecLerp(Vec3d start, Vec3d end, double fraction) {
		double x = start.x + (end.x - start.x) * fraction;
		double y = start.y + (end.y - start.y) * fraction;
		double z = start.z + (end.z - start.z) * fraction;
		return new Vec3d(x, y, z);
	}

	public static float applyDeadzone(float axis, float deadzone) {
		final float scalar = 1 / (1 - deadzone);
		float newAxis = 0;
		if (Math.abs(axis) > deadzone)
			newAxis = (Math.abs(axis) - deadzone) * scalar * Math.signum(axis);
		return newAxis;
	}
	
	private static final Random avRandomizer = new Random();
	public static void spawnParticles(IParticleData type, int count, Vec3d position, Vec3d size, double speed ){
		Minecraft mc=Minecraft.getInstance();
		for (int k = 0; k < count; ++k)
		{
			double d1 = avRandomizer.nextGaussian() * size.x;
			double d3 = avRandomizer.nextGaussian() * size.y;
			double d5 = avRandomizer.nextGaussian() * size.z;
			double d6 = avRandomizer.nextGaussian() * speed;
			double d7 = avRandomizer.nextGaussian() * speed;
			double d8 = avRandomizer.nextGaussian() * speed;
			
			try
			{
				mc.world.addParticle(type,  position.x + d1, position.y + d3, position.z + d5, d6, d7, d8);
			}
			catch (Throwable var16)
			{
				LogManager.getLogger().warn("Could not spawn particle effect {}", (Object)type);
				return;
			}
		}
	}

	public static long microTime() {
		return System.nanoTime() / 1000L;
	}

	public static long milliTime() {
		return System.nanoTime() / 1000000L;
	}

	public static void printStackIfContainsClass(String className) {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		boolean print = false;
		for (StackTraceElement stackEl : stack) {
			if (stackEl.getClassName().equals(className)) {
				print = true;
				break;
			}
		}

		if (print)
			Thread.dumpStack();
	}

}
