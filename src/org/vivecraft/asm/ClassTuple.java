package org.vivecraft.asm;

public class ClassTuple {
	public final String className;
	public final String classNameObf;
	
	public ClassTuple(String className, String classNameObf) {
		this.className = className;
		this.classNameObf = classNameObf;
	}

	/**
	 * This constructor will automatically resolve the obfuscated class name.
	 */
	public ClassTuple(String className) {
		this(className, ObfNames.resolveClass(className, true).replace('/', '.'));
	}
}
