package org.vivecraft.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.util.math.vector.Matrix4f;

public class GLUtils {
    private static FloatBuffer matrixBuffer = GLAllocation.createDirectFloatBuffer(16);

    public static synchronized ByteBuffer createByteBuffer(int size)
    {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    public static FloatBuffer createFloatBuffer(int size)
    {
        return createByteBuffer(size << 2).asFloatBuffer();
    }


    // Temporary measure because I am lazy
    // TODO: Use VBO for cloud renderer
    public static synchronized int generateDisplayLists(int range)
    {
        int i = GL12.glGenLists(range);

        if (i == 0)
        {
            int j = GlStateManager.getError();
            String s = "No error code reported";

            if (j != 0)
            {
                s = GLX.getErrorString(j);
            }

            throw new IllegalStateException("glGenLists returned an ID of 0 for a count of " + range + ", GL error (" + j + "): " + s);
        }
        else
        {
            return i;
        }
    }
}