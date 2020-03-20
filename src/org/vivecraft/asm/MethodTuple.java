package org.vivecraft.asm;

public class MethodTuple {
	public final String methodName;
	public final String methodDesc;
	public final String methodNameObf;
	public final String methodDescObf;
	
	public MethodTuple(String methodName, String methodDesc, String methodNameObf, String methodDescObf) {
		this.methodName = methodName;
		this.methodDesc = methodDesc;
		this.methodNameObf = methodNameObf;
		this.methodDescObf = methodDescObf;
	}

	/**
	 * This constructor will automatically resolve the obfuscated descriptor.
	 */
	public MethodTuple(String methodName, String methodDesc) {
		this(methodName, methodDesc, ObfNames.resolveMethod(methodName, true), ObfNames.resolveDescriptor(methodDesc, true));
	}
}
