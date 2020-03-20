package org.vivecraft.main;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.vivecraft.asm.VivecraftASMTransformer;
import org.vivecraft.tweaker.MinecriftClassTransformer;
import org.vivecraft.tweaker.MinecriftClassTransformer.Stage;

import com.google.common.base.Throwables;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class VivecraftMain
{
	private static final String[] encapsulatedTransformers = new String[]{
	};
	private static final String[] removedTransformers = new String[]{
		"guichaguri.betterfps.transformers.PatcherTransformer",
		"sampler.asm.Transformer"
	};
	
	public static void main(String[] p_main_0_)
    {
		LaunchClassLoader load = (LaunchClassLoader) Thread.currentThread().getContextClassLoader();
		
		try {
			Field f = load.getClass().getDeclaredField("transformers");
			f.setAccessible(true);
			
			
			List<IClassTransformer> transformers = (List<IClassTransformer>) f.get(load);
			List<IClassTransformer> encapsulateObf = new ArrayList<IClassTransformer>();
			List<IClassTransformer> encapsulateDeobf = new ArrayList<IClassTransformer>();

			boolean passedDeobf = false;
			System.out.println("************** Vivecraft classloader pre-filter ***************");
			for (final Iterator it = transformers.iterator(); it.hasNext(); ) {
				IClassTransformer t = (IClassTransformer) it.next();

				System.out.println(t.getClass().getName());

				if (t.getClass().getName().equals("net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer")) {
					passedDeobf = true;
				}
				for (String dt : encapsulatedTransformers) {
				    if (t.getClass().getName().equals(dt) || t.getClass().getName().equals("$wrapper." + dt)) {
				    	if (passedDeobf) {
				    		encapsulateDeobf.add(t);
				    	} else {
				    		encapsulateObf.add(t);
				    	}
				    	it.remove();
				    	break;
				    }
				}
				for (String dt : removedTransformers) {
				    if (t.getClass().getName().equals(dt) || t.getClass().getName().equals("$wrapper." + dt)) {
				    	it.remove();
				    	break;
				    }
				}
			}

			transformers.add(2, new VivecraftASMTransformer(true));
			transformers.add(2, new MinecriftClassTransformer(Stage.MAIN, null));
			int forgeObfIndex = 0;
			for (int i = 0; i < transformers.size(); i++) {
				IClassTransformer t = transformers.get(i);
				if (t.getClass().getName().equals("$wrapper.net.minecraftforge.fml.common.asm.transformers.EventSubscriberTransformer")) {
					forgeObfIndex = i + 1;
					break;
				}
			}

			if (encapsulateObf.size() > 0) { //Dirty Harry Potter.
				HashMap<String, byte[]> cacheMap = new HashMap<String, byte[]>();
				transformers.add(forgeObfIndex, new MinecriftClassTransformer(Stage.CACHE, cacheMap));
				transformers.addAll(forgeObfIndex + 1, encapsulateObf);
				transformers.add(forgeObfIndex + encapsulateObf.size() + 1, new MinecriftClassTransformer(Stage.REPLACE, cacheMap));
				forgeObfIndex += encapsulateObf.size() + 2;
			}
			if (encapsulateDeobf.size() > 0) { //Dirtier Harry Potter.
				HashMap<String, byte[]> cacheMap = new HashMap<String, byte[]>();
				transformers.add(transformers.size() - 1, new MinecriftClassTransformer(Stage.CACHE, cacheMap));
				transformers.addAll(transformers.size() - 1, encapsulateDeobf);
				transformers.add(transformers.size() - 1, new MinecriftClassTransformer(Stage.REPLACE, cacheMap));
			}

			/*try {
				Class.forName("net.minecraftforge.fml.common.API"); // don't ask
				transformers.add(forgeObfIndex, new MinecriftForgeClassTransformer());
				transformers.add(transformers.size() - 1, new MinecriftForgeLateClassTransformer());
			} catch (ClassNotFoundException e) {}*/

	    	System.out.println("************** Vivecraft classloader filter ***************");
			for (final Iterator it = transformers.iterator(); it.hasNext(); ) {
				IClassTransformer t = (IClassTransformer) it.next();
				System.out.println(t.getClass().getName());
			}
			
		} catch (Exception e) {
			System.out.println("************** Vivecraft filter error ***************");
			e.printStackTrace();
		}

		try {
			final String launchTarget = "net.minecraft.client.main.Main";
       	 	final Class<?> clazz = Class.forName(launchTarget, false, load);
         	final Method mainMethod = clazz.getMethod("main", String[].class);
         	mainMethod.invoke(null, (Object) p_main_0_);
		} catch (Exception e) {
	    	System.out.println("************** Vivecraft critical error ***************");
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
    	
    }

}
