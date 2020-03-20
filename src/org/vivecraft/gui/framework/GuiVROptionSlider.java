package org.vivecraft.gui.framework;

import org.vivecraft.settings.VRSettings;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.util.math.MathHelper;

public class GuiVROptionSlider extends GuiVROptionButton
{
    private double sliderValue;
    public boolean dragging;
    private final double minValue;
    private final double maxValue;

    public GuiVROptionSlider(int id, int x, int y, int width, int height, VRSettings.VrOptions option, double min, double max)
    {
        super(id, x, y, width, height, option, "", (p) -> {});
        this.sliderValue = 1.0D;
        this.minValue = min;
        this.maxValue = max;
        Minecraft minecraft = Minecraft.getInstance();
        this.sliderValue = enumOptions.normalizeValue(minecraft.vrSettings.getOptionFloatValue(enumOptions));
        this.setMessage(minecraft.vrSettings.getButtonDisplayString(enumOptions));
    }

    public GuiVROptionSlider(int id, int x, int y, VRSettings.VrOptions option, double min, double max)
    {
        this(id, x, y, 150, 20, option, min, max);
    }

    /**
     * Returns 0 if the button is disabled, 1 if the mouse is NOT hovering over this button and 2 if it IS hovering over
     * this button.
     */
    protected int getHoverState(boolean mouseOver)
    {
        return 0;
    }

    protected void onDrag(double p_onDrag_1_, double p_onDrag_3_, double p_onDrag_5_, double p_onDrag_7_)
    {
        this.setValueFromMouse(p_onDrag_1_);
        super.onDrag(p_onDrag_1_, p_onDrag_3_, p_onDrag_5_, p_onDrag_7_);
    }
    
    private void setValueFromMouse(double p_setValueFromMouse_1_)
    {
    	Minecraft mc = Minecraft.getInstance();
        this.sliderValue = (double)((float)(p_setValueFromMouse_1_ - (this.x + 4)) / (float)(this.width - 8));
        this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0D, 1.0D);
        double d0 = this.enumOptions.denormalizeValue((float) this.sliderValue);
        mc.vrSettings.setOptionFloatValue(this.enumOptions, (float) d0);
        this.sliderValue = this.enumOptions.normalizeValue((float) d0);
        this.setMessage(mc.vrSettings.getButtonDisplayString(this.enumOptions));
    }
    
    /**
     * Fired when the mouse button is dragged. Equivalent of MouseListener.mouseDragged(MouseEvent e).
     */
    protected void renderBg(Minecraft mc, int mouseX, int mouseY)
    {
        if (this.visible)
        {
            mc.getTextureManager().bindTexture(WIDGETS_LOCATION);
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            int i = (this.isHovered() ? 2 : 1) * 20;
            this.blit(this.x + (int)(this.sliderValue * (double)(this.width - 8)), this.y, 0, 46 + i, 4, 20);
            this.blit(this.x + (int)(this.sliderValue * (double)(this.width - 8)) + 4, this.y, 196, 46 + i, 4, 20);
        }
    }

    /**
     * Called when the left mouse button is pressed over this button. This method is specific to GuiButton.
     */
    public void onClick(double mouseX, double mouseY)
    {
        this.sliderValue = (mouseX - (double)(this.x + 4)) / (double)(this.width - 8);
        this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0D, 1.0D);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.vrSettings.setOptionFloatValue(this.enumOptions, (float) this.enumOptions.denormalizeValue((float) this.sliderValue));
        this.setMessage(minecraft.vrSettings.getButtonDisplayString(this.enumOptions));
        this.dragging = true;
    }
    
    @Override
    protected int getYImage(boolean p_getYImage_1_)
    {
        return 0;
    }
    
   
    @Override
    public void onRelease(double mouseX, double mouseY)
    {
        this.dragging = false;
    }
}
