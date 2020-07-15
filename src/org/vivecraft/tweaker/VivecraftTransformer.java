package org.vivecraft.tweaker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import cpw.mods.modlauncher.TransformerHolder;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerActivity;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import optifine.AccessFixer;
import optifine.OptiFineTransformer;
import optifine.Utils;

public class VivecraftTransformer implements ITransformer<ClassNode>
{
    private static final Logger LOGGER = LogManager.getLogger();
    private ZipFile ZipFile;
    
    public List<ITransformer> undeadClassTransformers = new ArrayList<ITransformer>();
    public List<ITransformer> lostdMethodTransformers = new ArrayList<ITransformer>();
    public List<ITransformer> fieldTransformersOftheDamned  = new ArrayList<ITransformer>();

    private List<String> exclusions = Arrays.asList(
            "net/minecraft/item/Item",
            "net/minecraft/item/Item$Properties",
            "net/minecraft/client/gui/screen/inventory/ContainerScreen",
            "net/minecraft/client/gui/screen/inventory/CreativeScreen",
            "net/minecraft/fluid/FluidState"
    );
    
    public VivecraftTransformer(ZipFile ZipFile)
    {
        this.ZipFile = ZipFile;
    }

    @Override
    public TransformerVoteResult castVote(ITransformerVotingContext context)
    {
        return TransformerVoteResult.YES;
    }

    @Override
    public Set<Target> targets()
    {
        Set<Target> set = new HashSet<>();
        String[] astring = this.getNamesMatching("srg/", ".clsrg");

        for (int i = 0; i < astring.length; ++i)
        {
            String s = astring[i];
            s = Utils.removePrefix(s, new String[] {"srg/"});
            s = Utils.removeSuffix(s, new String[] {".clsrg"});
            Target target = Target.targetClass(s);
            if(exclusions.contains(s))
        		continue;
            set.add(target);
        }

        LOGGER.info("Targets: " + set.size());
        return set;
    }

    @Override
    public ClassNode transform(ClassNode input, ITransformerVotingContext context)
    {
        ClassNode classnode = input;
        String s = context.getClassName();
        String s1 = s.replace('.', '/');
        byte[] abyte = this.getResourceBytes("srg/" + s1 + ".clsrg");

        if (abyte != null)
        {
            InputStream inputstream = new ByteArrayInputStream(abyte);
            ClassNode classnode1 = this.loadClass(inputstream);
        	System.out.println("Vivecraft Replacing " + s + " History ... ");
			ITransformerActivity a;
		
        	for (ITransformerActivity act : context.getAuditActivities()){
        	  	System.out.println("... " + act.getActivityString());
        	}
			
            if (classnode1 != null)
            {
                this.debugClass(classnode1);
                AccessFixer.fixMemberAccess(input, classnode1);
                classnode = classnode1;
            }
        }

        for(ITransformer<ClassNode> it: undeadClassTransformers) {
        	TransformerHolder<ClassNode> t = (TransformerHolder<ClassNode>) it;
        	if(t.owner().name().contains("OptiFine")) //NOT U
        		continue;
        	if(t.owner().name().contains("Vivecraft")) //NOT U EITHER *not needed*
        		continue;
        	for(Target target: t.targets()) {
        		if(target.getClassName().equals(context.getClassName())) {
        			classnode = t.transform(classnode, context);
            	  	System.out.println("ARISE! " + target.getClassName() + " " + target.getElementName() + " " + target.getElementDescriptor());
        		}
        	}
        }
        
    	List<MethodNode> ms = new ArrayList<>();
    	for (MethodNode n: (List<MethodNode>)classnode.methods) {
	        for(ITransformer<MethodNode> t: lostdMethodTransformers) {
	        	//  	System.out.println("SPAM1 " + context.getClassName() + " " + n.name + " " + n.desc);
	        		for(Target target: t.targets()) {
	            	//  	System.out.println("SPAM2 " + target.getClassName() + " " + target.getElementName() + " " + target.getElementDescriptor());
			        	if(target.getClassName().equals(context.getClassName() ) && 
			        			target.getElementName().equals(n.name) && 
			        			target.getElementDescriptor().equals(n.desc)){    		
		            	  	System.out.println("ARISE! " + target.getClassName() + " " + target.getElementName() + " " + target.getElementDescriptor());
			        		n = t.transform(n, context);
			        	}
	        		}        
	        	}
			ms.add(n);
        }
    	classnode.methods = ms;
    	
    	List<FieldNode> fs = new ArrayList<>();
    	for (FieldNode f: (List<FieldNode>)classnode.fields) {
    		for(ITransformer<FieldNode> t: fieldTransformersOftheDamned) {
    			for(Target target: t.targets()) {
    				if(target.getClassName().equals(context.getClassName() ) && 
    						target.getElementName().equals(f.name)){  
	            	  	System.out.println("ARISE! " + target.getClassName() + " " + target.getElementName() + " " + target.getElementDescriptor());
    					f = t.transform(f, context);
    				}
    			}
    		}
			fs.add(f);
    	}
    	classnode.fields = fs;
    	
        return classnode;
    }

    private void debugClass(ClassNode classNode)
    {
    }

    private ClassNode loadClass(InputStream in)
    {
        try
        {
            ClassReader classreader = new ClassReader(in);
            ClassNode classnode = new ClassNode(393216);
            classreader.accept(classnode, 0);
            return classnode;
        }
        catch (IOException ioexception)
        {
            ioexception.printStackTrace();
            return null;
        }
    }

    private String[] getNamesMatching(String prefix, String suffix)
    {
        List<String> list = new ArrayList<>();
        Enumeration <? extends ZipEntry > enumeration = this.ZipFile.entries();

        while (enumeration.hasMoreElements())
        {
            ZipEntry zipentry = enumeration.nextElement();
            String s = zipentry.getName();

            if (s.startsWith(prefix) && s.endsWith(suffix))
            {
                list.add(s);
            }
        }

        String[] astring = list.toArray(new String[list.size()]);
        return astring;
    }

    private byte[] getResourceBytes(String name)
    {
        try
        {
        	name = Utils.ensurePrefix(name, "/");
        	
            InputStream inputstream = this.getClass().getResourceAsStream(name);

            if (inputstream == null)
            {
                return null;
            }
            else
            {
                byte[] abyte = Utils.readAll(inputstream);
                inputstream.close();
                return abyte;
            }
        }
        catch (IOException ioexception)
        {
            ioexception.printStackTrace();
            return null;
        }
    }

}
