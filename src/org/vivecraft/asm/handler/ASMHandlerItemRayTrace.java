package org.vivecraft.asm.handler;

import org.vivecraft.asm.ASMClassHandler;
import org.vivecraft.asm.ASMMethodHandler;
import org.vivecraft.asm.ASMUtil;
import org.vivecraft.asm.MethodTuple;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ASMHandlerItemRayTrace extends ASMClassHandler {
	@Override
	public String getDesiredClass() {
		return "net/minecraft/item/Item";
	}

	@Override
	public ASMMethodHandler[] getMethodHandlers() {
		return new ASMMethodHandler[]{new RayTraceMethodHandler()};
	}

	public static class RayTraceMethodHandler implements ASMMethodHandler {
		@Override
		public MethodTuple getDesiredMethod() {
			return new MethodTuple("func_219968_a", "(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/RayTraceContext$FluidMode;)Lnet/minecraft/util/math/BlockRayTraceResult;"); //rayTrace
		}

		@Override
		public void patchMethod(MethodNode methodNode, ClassNode classNode) {
			AbstractInsnNode insertInsn = ASMUtil.findFirstInstruction(methodNode, Opcodes.ASTORE, 5);
			InsnList insnList = new InsnList();
			insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
			insnList.add(new VarInsnNode(Opcodes.FLOAD, 3));
			insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/vivecraft/asm/ASMDelegator", "itemRayTracePitch", "(Lnet/minecraft/entity/player/PlayerEntity;F)F", false));
			insnList.add(new VarInsnNode(Opcodes.FSTORE, 3));
			insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
			insnList.add(new VarInsnNode(Opcodes.FLOAD, 4));
			insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/vivecraft/asm/ASMDelegator", "itemRayTraceYaw", "(Lnet/minecraft/entity/player/PlayerEntity;F)F", false));
			insnList.add(new VarInsnNode(Opcodes.FSTORE, 4));
			insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
			insnList.add(new VarInsnNode(Opcodes.ALOAD, 5));
			insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/vivecraft/asm/ASMDelegator", "itemRayTracePos", "(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/vector/Vector3d;)Lnet/minecraft/util/math/vector/Vector3d;", false));
			insnList.add(new VarInsnNode(Opcodes.ASTORE, 5));
			methodNode.instructions.insert(insertInsn, insnList);
			System.out.println("Inserted raytrace override");
		}
	}
}
