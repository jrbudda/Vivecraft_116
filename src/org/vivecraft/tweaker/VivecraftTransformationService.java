package org.vivecraft.tweaker;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.MethodNode;
import org.vivecraft.asm.VivecraftASMTransformer;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.utils.Utils;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformList;
import cpw.mods.modlauncher.TransformStore;
import cpw.mods.modlauncher.TransformTargetLabel;
import cpw.mods.modlauncher.TransformerHolder;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;

public class VivecraftTransformationService implements ITransformationService
{
    private static final Logger LOGGER = LogManager.getLogger();
    public static URL ZipFileUrl;
    private static ZipFile ZipFile;
    private static VivecraftTransformer transformer;

    @Override
    public String name()
    {
        return "Vivecraft";
    }

    @Override
    public void initialize(IEnvironment environment)
    {
        LOGGER.info("VivecraftTransformationService.initialize");
    }

    @Override
    public void beginScanning(IEnvironment environment)
    {
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException
    {
        LOGGER.info("VivecraftTransformationService.onLoad");

        try {
            ZipFileUrl = Utils.getVivecraftZipLocation().toURL();
            ZipFile = Utils.getVivecraftZip();
            transformer = new VivecraftTransformer(ZipFile);
        }
        catch (Exception exception)
        {
            LOGGER.error("Error loading ZIP file: " + ZipFileUrl, (Throwable)exception);
            throw new IncompatibleEnvironmentException("Error loading ZIP file: " + ZipFileUrl);
        }
    }

    @Override
    public Entry<Set<String>, Supplier<Function<String, Optional<URL>>>> additionalResourcesLocator()
    {
        return ITransformationService.super.additionalResourcesLocator();
    }

    @Override
    public Entry<Set<String>, Supplier<Function<String, Optional<URL>>>> additionalClassesLocator()
    {
        Set<String> set = new HashSet<>();
        set.add("org.vivecraft.");
        Supplier<Function<String, Optional<URL>>> supplier = () ->
        {
            return this::getResourceUrl;
        };
        Entry<Set<String>, Supplier<Function<String, Optional<URL>>>> entry = new SimpleEntry<>(set, supplier);
        LOGGER.info("additionalClassesLocator: " + set);
        return entry;
    }

    public Optional<URL> getResourceUrl(String name)
    {
        if (name.endsWith(".class") && !name.startsWith("optifine/"))
        {
            name = "srg/" + name;
        }

        if (transformer == null)
        {
            return Optional.empty();
        }
        else
        {
            ZipEntry zipentry = ZipFile.getEntry(name);

            if (zipentry == null)
            {
                return Optional.empty();
            }
            else
            {
                try
                {
                    String s = ZipFileUrl.toExternalForm();
                    URL url = new URL("jar:" + s + "!/" + name);
                    return Optional.of(url);
                }
                catch (IOException ioexception)
                {
                    LOGGER.error(ioexception);
                    return Optional.empty();
                }
            }
        }
    }

    @Override
    public List<ITransformer> transformers()
    {
        LOGGER.info("VivecraftTransformationService.transformers");
        List<ITransformer> list = new ArrayList<>();

        if (transformer != null)
        {
            list.add(transformer);
        }

        list.add(new VivecraftASMTransformer());

//		try {
//	        Object transformationServicesHandler;
//			transformationServicesHandler = FieldUtils.readDeclaredField(Launcher.INSTANCE, "transformationServicesHandler", true);
//	        TransformStore transformStore = (TransformStore) FieldUtils.readDeclaredField(transformationServicesHandler, "transformStore", true);
//	        EnumMap<TransformTargetLabel.LabelType, TransformList<?>> transformers = (EnumMap<TransformTargetLabel.LabelType, TransformList<?>>) FieldUtils.readDeclaredField(transformStore, "transformers", true);
//        
//	        Map<TransformTargetLabel, List<ITransformer<?>>> classTransformers = (Map<TransformTargetLabel, List<ITransformer<?>>>) FieldUtils.readDeclaredField(transformers.get(TransformTargetLabel.LabelType.CLASS), "transformers", true);
//	        for(List<ITransformer<?>> c: classTransformers.values()) {
//	        	for(ITransformer ct:c) {
//	        		TransformerHolder it = (TransformerHolder) ct;
//	        		if(transformer.ofTargets == null && it.owner().name().equals(("OptiFine"))) 
//	        			transformer.ofTargets = it.targets();
//	        		else
//	        			transformer.undeadClassTransformers.add(ct);
//	        	}
//	        }
//	        LOGGER.info("VivecraftTransformationService.necromancy: Reviving " + transformer.undeadClassTransformers.size() + " classTransformers ");
//
//	        Map<TransformTargetLabel, List<ITransformer<?>>> methodTransformers = (Map<TransformTargetLabel, List<ITransformer<?>>>) FieldUtils.readDeclaredField(transformers.get(TransformTargetLabel.LabelType.METHOD), "transformers", true);
//	        for(List c: methodTransformers.values())
//	        	transformer.lostMethodTransformers.addAll(c);
//	        LOGGER.info("VivecraftTransformationService.necromancy: Finding " + transformer.lostMethodTransformers.size() + " methodTransformers ");
//
//	        Map<TransformTargetLabel, List<ITransformer<?>>> FieldTransformers = (Map<TransformTargetLabel, List<ITransformer<?>>>) FieldUtils.readDeclaredField(transformers.get(TransformTargetLabel.LabelType.FIELD), "transformers", true);
//	        for(List c: FieldTransformers.values())
//	        	transformer.fieldTransformersOftheDamned.addAll(c);
//	        LOGGER.info("VivecraftTransformationService.necromancy: De-cursing " + transformer.fieldTransformersOftheDamned.size() + " fieldTransformers ");
//	                    
//		} catch (Exception e) {
//	        LOGGER.info("VivecraftTransformationService.necromancy Trans-necromancy has failed, sire! " + e.getLocalizedMessage());
//		}

        
        return list;
    }

    public static VivecraftTransformer getTransformer()
    {
        return transformer;
    }
}
