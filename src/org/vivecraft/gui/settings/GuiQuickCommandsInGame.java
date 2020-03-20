package org.vivecraft.gui.settings;

import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class GuiQuickCommandsInGame extends Screen
{
    public GuiQuickCommandsInGame() {
		super(new StringTextComponent(""));
	}

	private int field_146445_a;
    private int field_146444_f;
    private static final String __OBFID = "CL_00000703";
    
    @Override
    public void init()
    {
    	KeyBinding.unPressAllKeys();
    	this.field_146445_a = 0;
    	this.buttons.clear();
    	byte var1 = -16;
    	boolean var2 = true;

    	String[] chatcommands = minecraft.vrSettings.vrQuickCommands;

    	int w = 0;
    	for (int i = 0; i < chatcommands.length; i++) {
    		
    		w = i > 5 ? 1 : 0;
    		String com  = chatcommands[i];
    		this.addButton(new Button(this.width / 2 - 125 + 127 * w, 36 + (i-6*w) * 24, 125, 20, com.toString(), (p) -> { 
    				minecraft.displayGuiScreen(null);
    				minecraft.player.sendChatMessage(p.getMessage());
    		}));     
    	}
    	this.addButton(new Button( this.width / 2 -50, this.height -30  + var1, 100, 20, "Cancel", (p) -> { 
				minecraft.displayGuiScreen(new IngameMenuScreen(false));
		}));   
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground();
        this.drawCenteredString(this.font, "Quick Commands", this.width / 2, 16, 16777215);
        super.render(mouseX, mouseY, partialTicks);
    }
}