package org.vivecraft.asm.handler;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.vivecraft.asm.ASMClassHandler;
import org.vivecraft.asm.ASMMethodHandler;
import org.vivecraft.asm.ASMUtil;
import org.vivecraft.asm.MethodTuple;

public class ASMHandlerContainerScreen extends ASMClassHandler {
	@Override
	public String getDesiredClass() {
		return "net/minecraft/client/gui/screen/inventory/ContainerScreen";
	}

	@Override
	public ASMMethodHandler[] getMethodHandlers() {
		return new ASMMethodHandler[]{new DragSplitFixMethodHandler()};
	}

	public static class DragSplitFixMethodHandler implements ASMMethodHandler {
		@Override
		public MethodTuple getDesiredMethod() {
			return new MethodTuple("mouseDragged", "(DDIDD)Z"); //mouseDragged
		}

		@Override
		public void patchMethod(MethodNode methodNode, ClassNode classNode) {
			AbstractInsnNode findInsn = ASMUtil.findFirstInstruction(methodNode, Opcodes.GETFIELD, "net/minecraft/client/gui/screen/inventory/ContainerScreen", "dragSplitting", "Z");
			LabelNode label = ((JumpInsnNode)methodNode.instructions.get(methodNode.instructions.indexOf(findInsn) + 1)).label;
			InsnList insnList = new InsnList();
			insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/minecraft/client/gui/screen/Screen", "hasShiftDown", "()Z", false));
			insnList.add(new JumpInsnNode(Opcodes.IFNE, label));
			ASMUtil.insertInstructionsRelative(methodNode, findInsn, -2, insnList);
			System.out.println("Inserted hasShiftDown call");
		}
	}
}
