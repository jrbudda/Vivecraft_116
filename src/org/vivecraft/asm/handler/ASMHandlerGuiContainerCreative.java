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
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ASMHandlerGuiContainerCreative extends ASMClassHandler {
	@Override
	public ClassTuple getDesiredClass() {
		return new ClassTuple("net.minecraft.client.gui.inventory.GuiContainerCreative");
	}

	@Override
	public ASMMethodHandler[] getMethodHandlers() {
		return new ASMMethodHandler[]{new MouseDownMethodHandler(), new AddItemsMethodHandler(), new AddSearchMethodHandler()};
	}

	@Override
	public boolean getComputeFrames() {
		return false;
	}

	public static class MouseDownMethodHandler implements ASMMethodHandler {
		@Override
		public MethodTuple getDesiredMethod() {
			return new MethodTuple("func_73863_a", "(IIF)V"); //drawScreen
		}

		@Override
		public void patchMethod(MethodNode methodNode, ClassNode classNode, boolean obfuscated) {
			MethodInsnNode insn = (MethodInsnNode)ASMUtil.findFirstInstruction(methodNode, Opcodes.INVOKESTATIC, "org/lwjgl/input/Mouse", "isButtonDown", "(I)Z", false);
			insn.owner = "com/mtbs3d/minecrift/utils/ASMDelegator";
			insn.name = "containerCreativeMouseDown";
			insn.desc = "(I)Z";
			System.out.println("Redirected Mouse.isButtonDown() to delegator");
		}
	}

	public static class AddItemsMethodHandler implements ASMMethodHandler {
		@Override
		public MethodTuple getDesiredMethod() {
			return new MethodTuple("func_147050_b", "(Lnet/minecraft/creativetab/CreativeTabs;)V"); //setCurrentCreativeTab
		}

		@Override
		public void patchMethod(MethodNode methodNode, ClassNode classNode, boolean obfuscated) {
			InsnList newInsns = new InsnList();
			newInsns.add(new VarInsnNode(Opcodes.ALOAD, 1));
			newInsns.add(new VarInsnNode(Opcodes.ALOAD, 3));
			newInsns.add(new FieldInsnNode(Opcodes.GETFIELD, ObfNames.resolveClass("net/minecraft/client/gui/inventory/GuiContainerCreative$ContainerCreative", obfuscated), ObfNames.resolveField("field_148330_a", obfuscated), ObfNames.resolveDescriptor("Lnet/minecraft/util/NonNullList;", obfuscated)));
			newInsns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/mtbs3d/minecrift/utils/ASMDelegator", "addCreativeItems", ObfNames.resolveDescriptor("(Lnet/minecraft/creativetab/CreativeTabs;Lnet/minecraft/util/NonNullList;)V", obfuscated), false));
			AbstractInsnNode insn = ASMUtil.findFirstInstruction(methodNode, Opcodes.INVOKEVIRTUAL, ObfNames.resolveClass("net/minecraft/creativetab/CreativeTabs", obfuscated), ObfNames.resolveMethod("func_78018_a", obfuscated), ObfNames.resolveDescriptor("(Lnet/minecraft/util/NonNullList;)V", obfuscated), false);
			methodNode.instructions.insert(insn, newInsns);
			System.out.println("Inserted call to delegator");
		}
	}

	public static class AddSearchMethodHandler implements ASMMethodHandler {
		@Override
		public MethodTuple getDesiredMethod() {
			return new MethodTuple("func_147053_i", "()V"); //updateCreativeSearch
		}

		@Override
		public void patchMethod(MethodNode methodNode, ClassNode classNode, boolean obfuscated) {
			InsnList newInsns = new InsnList();
			newInsns.add(new VarInsnNode(Opcodes.ALOAD, 0));
			newInsns.add(new FieldInsnNode(Opcodes.GETFIELD, ObfNames.resolveClass("net/minecraft/client/gui/inventory/GuiContainerCreative", obfuscated), ObfNames.resolveField("field_147062_A", obfuscated), ObfNames.resolveDescriptor("Lnet/minecraft/client/gui/GuiTextField;", obfuscated)));
			newInsns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, ObfNames.resolveClass("net/minecraft/client/gui/GuiTextField", obfuscated), ObfNames.resolveMethod("func_146179_b", obfuscated), "()Ljava/lang/String;", false));
			newInsns.add(new VarInsnNode(Opcodes.ALOAD, 1));
			newInsns.add(new FieldInsnNode(Opcodes.GETFIELD, ObfNames.resolveClass("net/minecraft/client/gui/inventory/GuiContainerCreative$ContainerCreative", obfuscated), ObfNames.resolveField("field_148330_a", obfuscated), ObfNames.resolveDescriptor("Lnet/minecraft/util/NonNullList;", obfuscated)));
			newInsns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/mtbs3d/minecrift/utils/ASMDelegator", "addCreativeSearch", ObfNames.resolveDescriptor("(Ljava/lang/String;Lnet/minecraft/util/NonNullList;)V", obfuscated), false));
			AbstractInsnNode insn = ASMUtil.findFirstInstruction(methodNode, Opcodes.PUTFIELD, ObfNames.resolveClass("net/minecraft/client/gui/inventory/GuiContainerCreative", obfuscated), ObfNames.resolveField("field_147067_x", obfuscated), "F");
			ASMUtil.insertInstructionsRelative(methodNode, insn, -3, newInsns);
			System.out.println("Inserted call to delegator");
		}
	}
}
