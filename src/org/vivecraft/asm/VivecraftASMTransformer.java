package org.vivecraft.asm;

import java.util.ArrayList;
import java.util.List;

import org.vivecraft.asm.handler.ASMHandlerFixITeleporter;
import org.vivecraft.asm.handler.ASMHandlerGuiContainer;
import org.vivecraft.asm.handler.ASMHandlerGuiContainerCreative;
import org.vivecraft.asm.handler.ASMHandlerGuiIngameForge;

import net.minecraft.launchwrapper.IClassTransformer;

public class VivecraftASMTransformer implements IClassTransformer {
	private final List<ASMClassHandler> asmHandlers = new ArrayList<ASMClassHandler>();
	
	public VivecraftASMTransformer() {
		this(false);
	}
	
	public VivecraftASMTransformer(boolean forge) {
		//asmHandlers.add(new ASMHandlerGuiContainer());
		//asmHandlers.add(new ASMHandlerGuiContainerCreative());
		//asmHandlers.add(new ASMHandlerGuiIngameForge());
		//if (forge) {
		//	asmHandlers.add(new ASMHandlerFixITeleporter());
		//}
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		for (ASMClassHandler handler : asmHandlers) {
			if (!handler.shouldPatchClass()) continue;
			ClassTuple tuple = handler.getDesiredClass();
			if (name.equals(tuple.classNameObf)) {
				System.out.println("Patching class: " + name + " (" + tuple.className + ")");
				bytes = handler.patchClass(bytes, true);
			} else if (name.equals(tuple.className)) {
				System.out.println("Patching class: " + name);
				bytes = handler.patchClass(bytes, false);
			}
		}
		return bytes;
	}
}
