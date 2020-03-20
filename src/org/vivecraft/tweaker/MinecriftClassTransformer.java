package org.vivecraft.tweaker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.launchwrapper.IClassTransformer;

// With apologies to Optifine. Copyright sp614x, this is built on his work.
// The existing classes are overwritten by all of the classes in the minecrift library. The
// minecrift code implements all of the Forge event handlers via reflection so we are 'Forge
// compatible' for non-core mods. Forge coremods most likely wont play nicely with us however.

public class MinecriftClassTransformer implements IClassTransformer
{
	private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("legacy.debugClassLoading", "false"));
	
    private ZipFile mcZipFile = null;
    private static URL mcZipURL = null;
    private final Stage stage;
    
    private final Map<String, byte[]> cache;
	private static Set<String> myClasses = new HashSet<String>();
    
    public MinecriftClassTransformer() {
    	this(Stage.MAIN, null);
    }

    public MinecriftClassTransformer(Stage stage, Map<String, byte[]> cache)
    {
    	this.stage = stage;
    	this.cache = cache;
    	if (stage == Stage.MAIN) {
	        try
	        {
	            this.mcZipFile = findMinecriftZipFile();
	        }
	        catch (Exception var6)
	        {
	            var6.printStackTrace();
	        }
	
	        if (this.mcZipFile == null)
	        {
	            debug("*** Can not find the Minecrift JAR in the classpath ***");
	            debug("*** Minecrift will not be loaded! ***");
	        }
    	} else if (cache == null) {
    		throw new IllegalArgumentException("Cache map required for cache/replace stage");
    	}
    }

    private static ZipFile getMinecriftZipFile(URL url)
    {
        try
        {
            URI uri = url.toURI();
            File file = new File(uri);
            ZipFile zipFile = new ZipFile(file);

            if (zipFile.getEntry("org/vivecraft/provider/MCOpenVR.class") == null)
            {
                zipFile.close();
                return null;
            }
            else
            {
                return zipFile;
            }
        }
        catch (Exception var4)
        {
            return null;
        }
    }
    
    public static ZipFile findMinecriftZipFile() {
    	if (mcZipURL != null) {
    		try {
    			return new ZipFile(new File(mcZipURL.toURI()));
    		} catch (Exception e) {
    			e.printStackTrace();
    			return null;
    		}
    	}
    	
    	URLClassLoader e = (URLClassLoader)MinecriftClassTransformer.class.getClassLoader();
        URL[] urls = e.getURLs();

        for (int i = 0; i < urls.length; ++i)
        {
            URL url = urls[i];
            ZipFile zipFile = getMinecriftZipFile(url);

            if (zipFile != null)
            {
                debug("Vivecraft ClassTransformer");
                debug("Vivecraft URL: " + url);
                debug("Vivecraft ZIP file: " + zipFile.getName());
                mcZipURL = url;
                return zipFile;
            }
        }
        return null;
    }
   
    
    public byte[] transform(String name, String transformedName, byte[] bytes)
    {
    	switch (stage) {
    		case MAIN:
		    	byte[] minecriftClass = this.getMinecriftClass(name);
		
		    	if (minecriftClass == null) {
		    		if (DEBUG) debug(String.format("Vivecraft: Passthrough %s %s", name, transformedName));
		    		//if (DEBUG) writeToFile("original", transformedName , "", bytes);
		    	}
		    	else {
		    		myClasses.add(name);
		    		
		    		// Perform any additional mods using ASM
		    		minecriftClass = performAsmModification(minecriftClass, transformedName);
		    		
		    		if(bytes.length != minecriftClass.length) {
		    			debug(String.format("Vivecraft: Overwrite %s %s (%d != %d)", name, transformedName, bytes.length, minecriftClass.length));
		    			myClasses.add(transformedName);
		    		}
		    		//if (DEBUG) writeToFile("original", transformedName, "", bytes);
		    		//if (DEBUG) writeToFile("transformed", transformedName, name, minecriftClass);
		    	}
		    	
		    	return minecriftClass != null ? minecriftClass : bytes;
    		case CACHE:
    			if(myClasses.contains(transformedName)) {
    				if(DEBUG) debug(String.format("Cache '%s' - '%s'", name, transformedName));
    				cache.put(transformedName, bytes);
    			}
    			return bytes;
    		case REPLACE:
    			if(cache.containsKey(transformedName)){
    				if(DEBUG) debug(String.format("Replace '%s' - '%s'", name, transformedName));
    				return cache.get(transformedName);
    			}
    			return bytes;
    	}
    	return bytes;
    }

    private void writeToFile(String dir, String transformedName, String name, byte[] bytes)
    {
        FileOutputStream stream = null;
        ;
        String filepath = String.format("%s/%s/%s/%s%s.%s", System.getProperty("user.home"), "minecrift_transformed_classes", dir, transformedName.replace(".", "/"), name, "class");
        File file = new File(filepath);
        debug("Writing to: " + filepath);
        try {
            File dir1 = file.getParentFile();
            dir1.mkdirs();
            file.createNewFile();
            stream = new FileOutputStream(filepath);
            stream.write(bytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private byte[] getMinecriftClass(String name)
    {
        if (this.mcZipFile == null)
        {
            return null;
        }
        else
        {
            String fullName = name + ".class";
            ZipEntry ze = this.mcZipFile.getEntry(fullName);

            if (ze == null) {
            	fullName = name + ".clazz";
            	ze = this.mcZipFile.getEntry(fullName);
            }
            
            if (ze == null)
            { 
                	return null;
            }
            else
            {
                try
                {
                    InputStream e = this.mcZipFile.getInputStream(ze);
                    byte[] bytes = readAll(e);

                    if ((long)bytes.length != ze.getSize())
                    {
                        debug("Invalid size for " + fullName + ": " + bytes.length + ", should be: " + ze.getSize());
                        return null;
                    }
                    else
                    {
                        return bytes;
                    }
                }
                catch (IOException var6)
                {
                    var6.printStackTrace();
                    return null;
                }
            }
        }
    }

    public static byte[] readAll(InputStream is) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];

        while (true)
        {
            int bytes = is.read(buf);

            if (bytes < 0)
            {
                is.close();
                byte[] bytes1 = baos.toByteArray();
                return bytes1;
            }

            baos.write(buf, 0, bytes);
        }
    }

    private static void debug(String str)
    {
        System.out.println(str);
    }

    private byte[] performAsmModification(final byte[] origBytecode, String className)
    {
//        if (className.equals("net.minecraft.entity.Entity")) {
//            debug("Further transforming class " + className + " via ASM");
//            ClassReader cr = new ClassReader(origBytecode);
//            ClassWriter cw = new ClassWriter(cr, 0);
//            EntityTransform et = new EntityTransform(cw);
//            cr.accept(et, 0);
//            return cw.toByteArray();
//        }

        return origBytecode;
    }

	public enum Stage {
		MAIN,
		CACHE,
		REPLACE
	}
}