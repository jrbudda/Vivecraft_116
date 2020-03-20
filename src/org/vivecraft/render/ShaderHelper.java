package org.vivecraft.render;

import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * Created with IntelliJ IDEA.
 * User: Engineer
 * Date: 8/6/13
 * Time: 10:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class ShaderHelper
{
    private static int createShader(String shaderGLSL, int shaderType) throws Exception
    {
        int shader = 0;
        try {
            shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType);
            if(shader == 0)
                return 0;

            ARBShaderObjects.glShaderSourceARB(shader, shaderGLSL);
            ARBShaderObjects.glCompileShaderARB(shader);

            if (ARBShaderObjects.glGetObjectParameteriARB(shader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
                throw new RuntimeException("Error creating shader: " + getLogInfo(shader));

            return shader;
        }
        catch(Exception exc) {
            ARBShaderObjects.glDeleteObjectARB(shader);
            throw exc;
        }
    }

    public static int checkGLError(String par1Str)
    {
        int var2 = GL11.glGetError();

        if (var2 != 0)
        {
            String var3 = "";//TODO: Fix? GLUtil.gluErrorString(var2);
            System.out.println("########## GL ERROR ##########");
            System.out.println("@ " + par1Str);
            System.out.println(var2 + ": " + var3);
        }

        return var2;
    }

    private static String getLogInfo(int obj) {
        return ARBShaderObjects.glGetInfoLogARB(obj, ARBShaderObjects.glGetObjectParameteriARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB));
    }

    public static int initShaders(String vertexShaderGLSL, String fragmentShaderGLSL, boolean doAttribs)
    {
        int vertShader = 0, pixelShader = 0;
        int program = 0;

        try {
            vertShader = createShader(vertexShaderGLSL, ARBVertexShader.GL_VERTEX_SHADER_ARB);
            pixelShader = createShader(fragmentShaderGLSL, ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);
        }
        catch(Exception exc) {
            exc.printStackTrace();
            return 0;
        }
        finally {
            if(vertShader == 0 || pixelShader == 0)
                return 0;
        }

        program = ARBShaderObjects.glCreateProgramObjectARB();
        if(program == 0)
            return 0;

        /*
        * if the fragment shaders setup sucessfully,
        * attach them to the shader program, link the shader program
        * into the GL context and validate
        */
        ARBShaderObjects.glAttachObjectARB(program, vertShader);
        ARBShaderObjects.glAttachObjectARB(program, pixelShader);

        if (doAttribs)
        {
            // Position information will be attribute 0
            GL20.glBindAttribLocation(program, 0, "in_Position");
            checkGLError("@2");
            // Color information will be attribute 1
            GL20.glBindAttribLocation(program, 1, "in_Color");
            checkGLError("@2a");
            // Texture information will be attribute 2
            GL20.glBindAttribLocation(program, 2, "in_TextureCoord");
            checkGLError("@3");
        }

        ARBShaderObjects.glLinkProgramARB(program);
        checkGLError("Link");

        if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
            System.out.println(getLogInfo(program));
            return 0;
        }

        ARBShaderObjects.glValidateProgramARB(program);
        if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_VALIDATE_STATUS_ARB) == GL11.GL_FALSE) {
            System.out.println(getLogInfo(program));
            return 0;
        }

        return program;
    }
}
