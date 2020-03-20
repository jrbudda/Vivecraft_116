package org.vivecraft.asm.handler;

import org.vivecraft.asm.ASMClassHandler;
import org.vivecraft.asm.ASMMethodHandler;
import org.vivecraft.asm.ASMUtil;
import org.vivecraft.asm.ClassTuple;
import org.vivecraft.asm.MethodTuple;
import org.vivecraft.asm.ObfNames;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class ASMHandlerGuiContainer extends ASMClassHandler {
	@Override
	public ClassTuple getDesiredClass() {
		return new ClassTuple("net.minecraft.client.gui.inventory.GuiContainer");
	}

	@Override
	public ASMMethodHandler[] getMethodHandlers() {
		return new ASMMethodHandler[]{new FakeShiftMethodHandler(), new ColorMaskMethodHandler()};
	}

	@Override
	public boolean getComputeFrames() {
		return false;
	}

	public static class FakeShiftMethodHandler implements ASMMethodHandler {
		@Override
		public MethodTuple getDesiredMethod() {
			return new MethodTuple("func_73864_a", "(III)V"); //mouseClicked
		}

		@Override
		public void patchMethod(MethodNode methodNode, ClassNode classNode, boolean obfuscated) {
			MethodInsnNode insn = (MethodInsnNode)ASMUtil.findFirstInstructionPattern(methodNode, new Object[]{Opcodes.BIPUSH, 54}, new Object[]{Opcodes.INVOKESTATIC, "org/lwjgl/input/Keyboard", "isKeyDown", "(I)Z", false});
			JumpInsnNode jumpInsn = (JumpInsnNode)methodNode.instructions.get(methodNode.instructions.indexOf(insn) - 2);
			InsnList insnList = new InsnList();
			insnList.add(new JumpInsnNode(Opcodes.IFNE, jumpInsn.label));
			insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ObfNames.resolveClass("net/minecraft/client/gui/inventory/GuiContainer", obfuscated), "isFakeShift", "()Z", false));
			methodNode.instructions.insert(insn, insnList);
			System.out.println("Inserted pressShiftFake check");
		}
	}

	public static class ColorMaskMethodHandler implements ASMMethodHandler {
		@Override
		public MethodTuple getDesiredMethod() {
			return new MethodTuple("func_73863_a", "(IIF)V"); //drawScreen
		}

		@Override
		public void patchMethod(MethodNode methodNode, ClassNode classNode, boolean obfuscated) {
			AbstractInsnNode findInsn = ASMUtil.findFirstInstruction(methodNode, Opcodes.INVOKEVIRTUAL, ObfNames.resolveClass("net/minecraft/inventory/Slot", obfuscated), ObfNames.resolveMethod("func_111238_b", obfuscated), "()Z", false);
			AbstractInsnNode insn = methodNode.instructions.get(methodNode.instructions.indexOf(findInsn) + 1);
			InsnList insnList = new InsnList();
			insnList.add(new InsnNode(Opcodes.ICONST_1));
			insnList.add(new InsnNode(Opcodes.ICONST_1));
			insnList.add(new InsnNode(Opcodes.ICONST_1));
			insnList.add(new InsnNode(Opcodes.ICONST_0));
			insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ObfNames.resolveClass("net/minecraft/client/renderer/GlStateManager", obfuscated), ObfNames.resolveMethod("func_179135_a", obfuscated), "(ZZZZ)V", false));
			methodNode.instructions.insert(insn, insnList);
			insn = (MethodInsnNode)ASMUtil.findFirstInstruction(methodNode, Opcodes.INVOKESPECIAL, ObfNames.resolveClass("net/minecraft/client/gui/inventory/GuiContainer", obfuscated), ObfNames.resolveMethod("func_146977_a", obfuscated), ObfNames.resolveDescriptor("(Lnet/minecraft/inventory/Slot;)V", obfuscated), false);
			insnList.clear();
			insnList.add(new InsnNode(Opcodes.ICONST_1));
			insnList.add(new InsnNode(Opcodes.ICONST_1));
			insnList.add(new InsnNode(Opcodes.ICONST_1));
			insnList.add(new InsnNode(Opcodes.ICONST_1));
			insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ObfNames.resolveClass("net/minecraft/client/renderer/GlStateManager", obfuscated), ObfNames.resolveMethod("func_179135_a", obfuscated), "(ZZZZ)V", false));
			insn = ASMUtil.findFirstInstruction(methodNode, Opcodes.INVOKESTATIC, ObfNames.resolveClass("net/minecraft/client/renderer/GlStateManager", obfuscated), ObfNames.resolveMethod("func_179121_F", obfuscated), "()V", false);
			methodNode.instructions.insert(insn, insnList); // same call
			System.out.println("Inserted colorMask calls");
		}
	}
}
