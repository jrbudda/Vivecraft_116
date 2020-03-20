package org.vivecraft.asm;

import java.util.ArrayList;
import java.util.Iterator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class ASMClassHandler {
	public abstract ClassTuple getDesiredClass();
	public abstract ASMMethodHandler[] getMethodHandlers();
	public abstract boolean getComputeFrames();
	
	public boolean shouldPatchClass() {
		return true;
	}
	
	protected void patchClass(ClassNode node, boolean obfuscated) {
	}
	
	public final byte[] patchClass(byte[] bytes, boolean obfuscated) {
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		patchClass(classNode, obfuscated);
		ASMMethodHandler[] handlers = getMethodHandlers();
		ArrayList<MethodNode> methodsCopy = new ArrayList<MethodNode>(classNode.methods);
		for (Iterator<MethodNode> methods = methodsCopy.iterator(); methods.hasNext(); ) {
			MethodNode method = methods.next();
			for (ASMMethodHandler handler : handlers) {
				MethodTuple tuple = handler.getDesiredMethod();
				if ((method.name.equals(tuple.methodName) && method.desc.equals(tuple.methodDesc)) || (method.name.equals(tuple.methodNameObf) && method.desc.equals(tuple.methodDescObf))) {
					System.out.println("Patching method: " + method.name + method.desc + (obfuscated ? " (" + tuple.methodName + tuple.methodDesc + ")" : ""));
					handler.patchMethod(method, classNode, obfuscated);
				}
			}
		}
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | (getComputeFrames() ? ClassWriter.COMPUTE_FRAMES : 0));
		classNode.accept(writer);
		return writer.toByteArray();
	}
}
