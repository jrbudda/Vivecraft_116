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
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import io.github.classgraph.ClassGraph;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.resources.IResource;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TextPropertiesManager;
import net.minecraft.world.IBlockDisplayReader;
import optifine.OptiFineTransformer;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.vivecraft.render.VRShaders;
import org.vivecraft.tweaker.MinecriftClassTransformer;
import org.vivecraft.utils.lwjgl.Matrix3f;
import org.vivecraft.utils.lwjgl.Matrix4f;
import org.vivecraft.utils.lwjgl.Vector2f;
import org.vivecraft.utils.lwjgl.Vector3f;
import org.vivecraft.utils.lwjgl.Vector4f;
import org.vivecraft.utils.math.Convert;
import org.vivecraft.utils.math.Quaternion;
import org.vivecraft.utils.math.Vector2;
import org.vivecraft.utils.math.Vector3;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.Minecraft;
import net.minecraft.particles.IParticleData;
import net.minecraft.util.math.vector.Vector3d;

public class Utils
{
	// Magic list from a C# snippet, don't question it
	private static final char[] illegalChars = {34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47};
	private static final int CONNECT_TIMEOUT = 5000;
	private static final int READ_TIMEOUT = 20000;

	private static URI vivecraftZipURI;

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

	public static org.vivecraft.utils.math.Vector3 convertToOVRVector(Vector3f vector) {
		return new Vector3(vector.x, vector.y, vector.z);
	}

	public static org.vivecraft.utils.math.Vector3 convertToOVRVector(Vector3d vector) {
		return new Vector3((float)vector.x, (float)vector.y, (float)vector.z);
	}
	
	public static Matrix4f convertOVRMatrix(org.vivecraft.utils.math.Matrix4f matrix) {
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
	
	public static org.vivecraft.utils.math.Matrix4f convertToOVRMatrix(Matrix4f matrixIn) {
		Matrix4f matrix = new Matrix4f();
		matrixIn.transpose(matrix);
		org.vivecraft.utils.math.Matrix4f mat = new org.vivecraft.utils.math.Matrix4f();
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
	
	public static float angleDiff(float a, float b) {
		float d = Math.abs(a - b) % 360;
		float r = d > 180 ? 360 - d : d;
		int sign = (a - b >= 0 && a - b <= 180) || (a - b <= -180 && a - b >= -360) ? 1 : -1;
		return r * sign;
	}
	
	public static float angleNormalize(float angle) {
		angle %= 360;
		if (angle < 0)
			angle += 360;
		return angle;
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

	public static Vector3 convertVector(Vector3d vector) {
		return new Vector3((float)vector.x, (float)vector.y, (float)vector.z);
	}

	public static Vector3f convertToVector3f(Vector3d vector) {
		return new Vector3f((float)vector.x, (float)vector.y, (float)vector.z);
	}

	public static Vector3d convertToVector3d(Vector3 vector) {
		return new Vector3d(vector.getX(), vector.getY(), vector.getZ());
	}

	public static Vector3d convertToVector3d(Vector3f vector) {
		return new Vector3d(vector.x, vector.y, vector.z);
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
			try {
				IResource resource = Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation("vivecraft", name));
				is = resource.getInputStream();
			} catch (FileNotFoundException | NullPointerException e) { // might be called super early
				is = VRShaders.class.getResourceAsStream("/assets/vivecraft/" + name);
			}

			if (is == null) {
				//uhh debugging?
				Path dir = Paths.get(System.getProperty("user.dir")); // ../mcpxxx/jars/
				if (dir.getParent() != null) {
					Path p5 = dir.getParent().resolve("src/resources/assets/vivecraft/" + name);
					if (!p5.toFile().exists() && dir.getParent().getParent() != null)
						p5 = dir.getParent().getParent().resolve("resources/assets/vivecraft/" + name);
					if (p5.toFile().exists())
						is = new FileInputStream(p5.toFile());
				}
			}
		} catch (Exception e) {
			handleAssetException(e, name, required);
			return null;
		}

		if (is == null)
			handleAssetException(new FileNotFoundException(name), name, required);

		return is;
	}

	public static byte[] loadAsset(String name, boolean required) {
		InputStream is = getAssetAsStream(name, required);
		if (is == null)
			return null;

		try {
			byte[] out = IOUtils.toByteArray(is);
			is.close();
			return out;
		} catch (Exception e) {
			handleAssetException(e, name, required);
		}

		return null;
	}
	
	public static String loadAssetAsString(String name, boolean required) {
		byte[] bytes = loadAsset(name, required);
		if (bytes == null)
			return null;

		return new String(bytes, Charsets.UTF_8);
	}

	public static void loadAssetToFile(String name, File file, boolean required) {
		InputStream is = getAssetAsStream(name, required);
		if (is == null)
			return;

		try {
			writeStreamToFile(is, file);
			is.close();
		} catch (Exception e) {
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
			ZipFile zip = getVivecraftZip();
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

	public static URI getVivecraftZipLocation() {
		if (vivecraftZipURI != null)
			return vivecraftZipURI;

		List<URI> uris = new ClassGraph().getClasspathURIs();
		for (URI uri : uris) {
			try (ZipFile zipFile = new ZipFile(new File(uri))) {
				if (zipFile.getEntry("org/vivecraft/provider/MCVR.class") != null) {
					System.out.println("Found Vivecraft zip: " + uri.toString());
					vivecraftZipURI = uri;
					break;
				}
			} catch (IOException e) {
			}
		}

		if (vivecraftZipURI == null)
			throw new RuntimeException("Could not find Vivecraft zip");
		return vivecraftZipURI;
	}

	public static ZipFile getVivecraftZip() {
		URI uri = getVivecraftZipLocation();

		try {
			File f = new File(uri);
			return new ZipFile(f);
		} catch (IOException e) {
			throw new RuntimeException(e);
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
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setReadTimeout(READ_TIMEOUT);
		conn.setUseCaches(false);
		conn.setDoInput(true);
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line = br.readLine();
		br.close();
		conn.disconnect();
		return line;
	}

	public static List<String> httpReadAllLines(String url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setReadTimeout(READ_TIMEOUT);
		conn.setUseCaches(false);
		conn.setDoInput(true);
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		ArrayList<String> list = new ArrayList<>();
		String line;
		while ((line = br.readLine()) != null) {
			list.add(line);
		}
		br.close();
		conn.disconnect();
		return list;
	}

	public static byte[] httpReadAll(String url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setReadTimeout(READ_TIMEOUT);
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
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setReadTimeout(READ_TIMEOUT);
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
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setReadTimeout(READ_TIMEOUT);
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
	public static Vector3d vecLerp(Vector3d start, Vector3d end, double fraction) {
		double x = start.x + (end.x - start.x) * fraction;
		double y = start.y + (end.y - start.y) * fraction;
		double z = start.z + (end.z - start.z) * fraction;
		return new Vector3d(x, y, z);
	}

	public static float applyDeadzone(float axis, float deadzone) {
		final float scalar = 1 / (1 - deadzone);
		float newAxis = 0;
		if (Math.abs(axis) > deadzone)
			newAxis = (Math.abs(axis) - deadzone) * scalar * Math.signum(axis);
		return newAxis;
	}
	
	private static final Random avRandomizer = new Random();
	public static void spawnParticles(IParticleData type, int count, Vector3d position, Vector3d size, double speed ){
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

	public static int getCombinedLightWithMin(IBlockDisplayReader lightReader, BlockPos pos, int minLight) {
		int light = WorldRenderer.getCombinedLight(lightReader, pos);
		int blockLight = (light >> 4) & 0xF;
		if (blockLight < minLight) {
			light &= 0xFFFFFF00;
			light |= minLight << 4;
		}
		return light;
	}

	public static void takeScreenshot(Framebuffer fb) {
		Minecraft mc = Minecraft.getInstance();
		ScreenShotHelper.saveScreenshot(mc.gameDir, fb.framebufferWidth, fb.framebufferHeight, fb, text -> {
			mc.execute(() -> mc.ingameGUI.getChatGUI().printChatMessage(text));
		});
	}

	public static List<ITextProperties> wrapText(ITextProperties text, int width, FontRenderer fontRenderer, @Nullable ITextProperties linePrefix)
	{
		TextPropertiesManager manager = new TextPropertiesManager();
		text.getComponentWithStyle((style, str) -> {
			manager.func_238155_a_(ITextProperties.func_240653_a_(str, style));
			return Optional.empty();
		}, Style.EMPTY);
		//List<IReorderingProcessor> list = Lists.newArrayList();
		//IReorderingProcessor prefixer = IReorderingProcessor.func_242239_a(linePrefix, Style.EMPTY);
		//fontRenderer.func_238420_b_().func_243242_a(manager.func_238156_b_(), width, Style.EMPTY, (p_243256_1_, p_243256_2_) ->
		//{
		//	IReorderingProcessor ireorderingprocessor = LanguageMap.getInstance().func_241870_a(p_243256_1_);
		//	list.add(p_243256_2_ ? IReorderingProcessor.func_242234_a(prefixer, ireorderingprocessor) : ireorderingprocessor);
		//});
		//return list.isEmpty() ? Lists.newArrayList(IReorderingProcessor.field_242232_a) : list;
		List<ITextProperties> list = Lists.newArrayList();
		fontRenderer.getCharacterManager().func_243242_a(manager.func_238156_b_(), width, Style.EMPTY, (lineText, sameLine) ->
		{
			list.add(sameLine && linePrefix != null ? ITextProperties.func_240655_a_(linePrefix, lineText) : lineText);
		});
		return list.isEmpty() ? Lists.newArrayList(ITextProperties.field_240651_c_) : list;
	}

	public static List<TextFormatting> styleToFormats(Style style) {
		if (style.isEmpty())
			return new ArrayList<>();

		ArrayList<TextFormatting> list = new ArrayList<>();
		if (style.getColor() != null)
			list.add(TextFormatting.getValueByName(style.getColor().getName()));
		if (style.getBold())
			list.add(TextFormatting.BOLD);
		if (style.getItalic())
			list.add(TextFormatting.ITALIC);
		if (style.getStrikethrough())
			list.add(TextFormatting.STRIKETHROUGH);
		if (style.getUnderlined())
			list.add(TextFormatting.UNDERLINE);
		if (style.getObfuscated())
			list.add(TextFormatting.OBFUSCATED);

		return list;
	}

	public static String formatsToString(List<TextFormatting> formats) {
		if (formats.size() == 0)
			return "";

		StringBuilder sb = new StringBuilder();
		formats.forEach(sb::append);
		return sb.toString();
	}

	public static String styleToFormatString(Style style) {
		return formatsToString(styleToFormats(style));
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
	public static net.minecraft.util.math.vector.Matrix4f Matrix4fFromOpenVR(jopenvr.HmdMatrix44_t in) {
		//do not transpose on 1.15
		net.minecraft.util.math.vector.Matrix4f out = new net.minecraft.util.math.vector.Matrix4f();
		out.m00 = in.m[0];
		out.m01 = in.m[1];
		out.m02 = in.m[2];
		out.m03 = in.m[3];
		out.m10 = in.m[4];
		out.m11 = in.m[5];
		out.m12 = in.m[6];
		out.m13 = in.m[7];
		out.m20 = in.m[8];
		out.m21 = in.m[9];
		out.m22 = in.m[10];
		out.m23 = in.m[11];
		out.m30 = in.m[12];
		out.m31 = in.m[13];
		out.m32 = in.m[14];
		out.m33 = in.m[15];
		return out;
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
    public static org.vivecraft.utils.math.Matrix4f rotationXMatrix(float angle) {
        float sina = (float) Math.sin((double)angle);
        float cosa = (float) Math.cos((double)angle);
        return new org.vivecraft.utils.math.Matrix4f(1.0F, 0.0F, 0.0F,
                            0.0F, cosa, -sina,
                            0.0F, sina, cosa);
    }

    public static org.vivecraft.utils.math.Matrix4f rotationZMatrix(float angle) {
        float sina = (float) Math.sin((double)angle);
        float cosa = (float) Math.cos((double)angle);
        return new org.vivecraft.utils.math.Matrix4f(cosa, -sina, 0.0F,
                sina, cosa, 0.0f,
                0.0F, 0.0f, 1.0f);
    }
    public static Vector3 convertMatrix4ftoTranslationVector(org.vivecraft.utils.math.Matrix4f mat) {
        return new Vector3(mat.M[0][3], mat.M[1][3], mat.M[2][3]);
    }
    // VIVE START
    public static void Matrix4fSet(org.vivecraft.utils.math.Matrix4f mat, float m11, float m12, float m13, float m14, float m21, float m22, float m23, float m24, float m31, float m32, float m33, float m34, float m41, float m42, float m43, float m44)
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

    public static void Matrix4fCopy(org.vivecraft.utils.math.Matrix4f source, org.vivecraft.utils.math.Matrix4f dest)
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

    public static org.vivecraft.utils.math.Matrix4f Matrix4fSetIdentity(org.vivecraft.utils.math.Matrix4f mat)
    {
        mat.M[0][0] = mat.M[1][1] = mat.M[2][2] = mat.M[3][3] = 1.0F;
        mat.M[0][1] = mat.M[1][0] = mat.M[2][3] = mat.M[3][1] = 0.0F;
        mat.M[0][2] = mat.M[1][2] = mat.M[2][0] = mat.M[3][2] = 0.0F;
        mat.M[0][3] = mat.M[1][3] = mat.M[2][1] = mat.M[3][0] = 0.0F;
        return mat;
    }
}
