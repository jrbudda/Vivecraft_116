package org.vivecraft.tweaker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import optifine.AccessFixer;
import optifine.Utils;

public class VivecraftTransformer implements ITransformer<ClassNode>
{
    private static final Logger LOGGER = LogManager.getLogger();
    private ZipFile ZipFile;

    public VivecraftTransformer(ZipFile ZipFile)
    {
        this.ZipFile = ZipFile;
    }

    public TransformerVoteResult castVote(ITransformerVotingContext context)
    {
        return TransformerVoteResult.YES;
    }

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
            set.add(target);
        }

        LOGGER.info("Targets: " + set.size());
        return set;
    }

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

            if (classnode1 != null)
            {
                this.debugClass(classnode1);
                AccessFixer.fixMemberAccess(input, classnode1);
                classnode = classnode1;
            }
        }

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
