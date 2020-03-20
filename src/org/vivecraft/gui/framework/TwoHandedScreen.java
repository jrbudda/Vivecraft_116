package org.vivecraft.gui.framework;

import org.vivecraft.control.ControllerType;
import org.vivecraft.provider.MCOpenVR;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;


public abstract class TwoHandedScreen extends Screen
{
	protected TwoHandedScreen( ) {
		super(new StringTextComponent(""));
	}

	public float cursorX1, cursorY1;
	public float cursorX2, cursorY2;
	private Widget lastHoveredButtonId1 = null, lastHoveredButtonId2 = null;
	protected boolean reinit;
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) 
	{
		if(super.mouseClicked(mouseX, mouseY, mouseButton))
		{	
			double d0 = Math.min(Math.max((int)cursorX2, 0), minecraft.mainWindow.getWidth())
					 * (double)minecraft.mainWindow.getScaledWidth() / (double)minecraft.mainWindow.getWidth();
			return true;
		}
		return false;
	}
	
	@Override
	public void render(int mouseX, int mouseY, float partialTicks) {
		
        if (reinit)
        {
            init();
            reinit = false;
        }
		       
        double mX1 = (cursorX1 * this.width / this.minecraft.mainWindow.getScaledWidth())
        		 * (double)minecraft.mainWindow.getScaledWidth() / (double)minecraft.mainWindow.getWidth();
        double mY1 = (cursorY1 * this.height / this.minecraft.mainWindow.getScaledHeight())
        		 * (double)minecraft.mainWindow.getScaledWidth() / (double)minecraft.mainWindow.getWidth();
        double mX2 = (cursorX2 * this.width / this.minecraft.mainWindow.getScaledWidth())
        		 * (double)minecraft.mainWindow.getScaledWidth() / (double)minecraft.mainWindow.getWidth();
        double mY2 = (cursorY2 * this.height / this.minecraft.mainWindow.getScaledHeight())
        		 * (double)minecraft.mainWindow.getScaledWidth() / (double)minecraft.mainWindow.getWidth();

        Widget hoveredButtonId1 = null, hoveredButtonId2 = null;
        for (int i = 0; i < this.buttons.size(); ++i)
        {
        	Widget butt = (Widget)this.buttons.get(i);
        	boolean buttonhovered1 = mX1 >= butt.x && mY1 >= butt.y && mX1 < butt.x + butt.getWidth() && mY1 < butt.y + 20;
        	boolean buttonhovered2 = mX2 >= butt.x && mY2 >= butt.y && mX2 < butt.x + butt.getWidth() && mY2 < butt.y + 20;
        	if(buttonhovered1)
        		butt.render((int)mX1, (int)mY1, partialTicks);
        	else
        		butt.render((int)mX2, (int)mY2, partialTicks);
        	
        	if (buttonhovered1)
        		hoveredButtonId1 = butt;
        	if (buttonhovered2)
        		hoveredButtonId2 = butt;
        }

        if (hoveredButtonId1 == null) {
        	lastHoveredButtonId1 = null;
        } else if (hoveredButtonId1 instanceof Button && lastHoveredButtonId1 != hoveredButtonId1) {
			MCOpenVR.triggerHapticPulse(ControllerType.LEFT, 300);
    		lastHoveredButtonId1 = hoveredButtonId1;
    	}
        if (hoveredButtonId2 == null) {
        	lastHoveredButtonId2 = null;
        } else if (hoveredButtonId1 instanceof Button && lastHoveredButtonId2 != hoveredButtonId2) {
			MCOpenVR.triggerHapticPulse(ControllerType.RIGHT, 300);
    		lastHoveredButtonId2 = hoveredButtonId2;
    	}
        
    	this.minecraft.ingameGUI.drawMouseMenuQuad((int)mX1, (int)mY1);
    	this.minecraft.ingameGUI.drawMouseMenuQuad((int)mX2, (int)mY2);

	}
}
