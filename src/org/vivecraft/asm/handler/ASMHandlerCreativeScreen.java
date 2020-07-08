package org.vivecraft.asm.handler;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.vivecraft.asm.ASMClassHandler;
import org.vivecraft.asm.ASMMethodHandler;
import org.vivecraft.asm.ASMUtil;
import org.vivecraft.asm.MethodTuple;

public class ASMHandlerCreativeScreen extends ASMClassHandler {
	@Override
	public String getDesiredClass() {
		return "net/minecraft/client/gui/screen/inventory/CreativeScreen";
	}

	@Override
	public ASMMethodHandler[] getMethodHandlers() {
		return new ASMMethodHandler[]{new AddTabsMethodHandler(), new AddSearchMethodHandler()};
	}

	public static class AddTabsMethodHandler implements ASMMethodHandler {
		@Override
		public MethodTuple getDesiredMethod() {
			return new MethodTuple("func_147050_b", "(Lnet/minecraft/item/ItemGroup;)V"); //setCurrentCreativeTab
		}

		@Override
		public void patchMethod(MethodNode methodNode, ClassNode classNode) {
			InsnList insnList = new InsnList();
			insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
			insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
			insnList.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/gui/screen/inventory/CreativeScreen", "field_147002_h", "Lnet/minecraft/inventory/container/Container;"));
			insnList.add(new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/client/gui/screen/inventory/CreativeScreen$CreativeContainer"));
			insnList.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/gui/screen/inventory/CreativeScreen$CreativeContainer", "field_148330_a", "Lnet/minecraft/util/NonNullList;"));
			insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/vivecraft/asm/ASMDelegator", "addCreativeItems", "(Lnet/minecraft/item/ItemGroup;Lnet/minecraft/util/NonNullList;)V", false));
			AbstractInsnNode insertInsn = ASMUtil.findFirstInstruction(methodNode, Opcodes.INVOKEVIRTUAL, "net/minecraft/item/ItemGroup", "func_78018_a", "(Lnet/minecraft/util/NonNullList;)V", false);
			methodNode.instructions.insert(insertInsn, insnList);
			System.out.println("Inserted call to delegator");
		}
	}

	public static class AddSearchMethodHandler implements ASMMethodHandler {
		@Override
		public MethodTuple getDesiredMethod() {
			return new MethodTuple("func_147053_i", "()V"); //updateCreativeSearch
		}

		@Override
		public void patchMethod(MethodNode methodNode, ClassNode classNode) {
			InsnList insnList = new InsnList();
			insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
			insnList.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/gui/screen/inventory/CreativeScreen", "field_147062_A", "Lnet/minecraft/client/gui/widget/TextFieldWidget;"));
			insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/client/gui/widget/TextFieldWidget", "func_146179_b", "()Ljava/lang/String;", false));
			insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
			insnList.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/gui/screen/inventory/CreativeScreen", "field_147002_h", "Lnet/minecraft/inventory/container/Container;"));
			insnList.add(new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/client/gui/screen/inventory/CreativeScreen$CreativeContainer"));
			insnList.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/gui/screen/inventory/CreativeScreen$CreativeContainer", "field_148330_a", "Lnet/minecraft/util/NonNullList;"));
			insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/vivecraft/asm/ASMDelegator", "addCreativeSearch", "(Ljava/lang/String;Lnet/minecraft/util/NonNullList;)V", false));
			AbstractInsnNode insertInsn = ASMUtil.findNthInstruction(methodNode, 1, Opcodes.PUTFIELD, "net/minecraft/client/gui/screen/inventory/CreativeScreen", "field_147067_x", "F");
			ASMUtil.insertInstructionsRelative(methodNode, insertInsn, -3, insnList);
			System.out.println("Inserted call to delegator");
		}
	}
}
