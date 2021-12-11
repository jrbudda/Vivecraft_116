package net.minecraftforge.client.extensions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.optifine.reflect.Reflector;

public interface IForgeMinecraft
{
    default Minecraft getSelf() { return (Minecraft)this; }

    default void pushGuiLayer(Screen screen)
    {
    	Reflector.call(Reflector.ForgeHooksClient_pushGuiLayer, getSelf(), screen);
    }

    default void popGuiLayer()
    {
    	Reflector.call(Reflector.ForgeHooksClient_popGuiLayer, getSelf());
    }
}