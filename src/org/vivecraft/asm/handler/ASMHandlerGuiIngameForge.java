package org.vivecraft.asm.handler;

import org.vivecraft.asm.ASMClassHandler;
import org.vivecraft.asm.ASMMethodHandler;
import org.vivecraft.asm.ASMUtil;
import org.vivecraft.asm.ClassTuple;
import org.vivecraft.asm.MethodTuple;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class ASMHandlerGuiIngameForge extends ASMClassHandler {
	@Override
	public ClassTuple getDesiredClass() {
		return new ClassTuple("net.minecraftforge.client.GuiIngameForge");
	}

	@Override
	public ASMMethodHandler[] getMethodHandlers() {
		return new ASMMethodHandler[]{new RemoveCrosshairMethodHandler()};
	}

	@Override
	public boolean getComputeFrames() {
		return false;
	}

	public static class RemoveCrosshairMethodHandler implements ASMMethodHandler {
		@Override
		public MethodTuple getDesiredMethod() {
			return new MethodTuple("func_175180_a", "(F)V"); //renderGameOverlay
		}

		@Override
		public void patchMethod(MethodNode methodNode, ClassNode classNode, boolean obfuscated) {
			MethodInsnNode insn = (MethodInsnNode)ASMUtil.findFirstInstruction(methodNode, Opcodes.INVOKEVIRTUAL, "net/minecraftforge/client/GuiIngameForge", "renderCrosshairs", "(F)V", false);
			ASMUtil.deleteInstructions(methodNode, methodNode.instructions.indexOf(insn) - 2, 3);
			System.out.println("Deleted renderCrosshairs call");
			
		}
	}
}
