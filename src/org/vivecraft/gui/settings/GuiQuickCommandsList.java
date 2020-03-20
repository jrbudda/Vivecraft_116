package org.vivecraft.gui.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.ExtendedList;

public class GuiQuickCommandsList extends ExtendedList<GuiQuickCommandsList.CommandEntry>
{
    private final GuiQuickCommandEditor parent;
    private final Minecraft mc;
        
    public GuiQuickCommandsList(GuiQuickCommandEditor parent, Minecraft mc)
    {
        super(mc, parent.width, parent.height, 32, parent.height - 32, 20);
        this.parent = parent;
        this.mc = mc;
        
        String[] commands = minecraft.vrSettings.vrQuickCommands;
               
        String var5 = null;
        int var4 = 0;
        int var7 = commands.length;
        for (int i = 0; i < var7; i++)
        {
        	String kb = commands[i];
            int width = minecraft.fontRenderer.getStringWidth(kb);
            this.addEntry(new GuiQuickCommandsList.CommandEntry(kb, this));
        }
    }
        
    public class CommandEntry extends ExtendedList.AbstractListEntry<GuiQuickCommandsList.CommandEntry>
    {
        private final Button btnDelete;
        public final TextFieldWidget txt;
        
        private CommandEntry(String command, GuiQuickCommandsList parent)
        {
            txt = new TextFieldWidget(minecraft.fontRenderer, parent.width / 2 - 100, 60, 200, 20, "");
            txt.setText(command);
            this.btnDelete = new Button(0, 0, 18, 18, "X", (p) -> {
                	CommandEntry.this.txt.setText("");
                	CommandEntry.this.txt.changeFocus(true);
            });          
        }
               
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
        	if(btnDelete.mouseClicked(mouseX, mouseY, button))
        		return true;
        	if(txt.mouseClicked(mouseX, mouseY, button))
        		return true;
        	return super.mouseClicked(mouseX, mouseY, button);
        }
        
        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        	if(btnDelete.mouseDragged(mouseX, mouseY, button, deltaX, deltaY))
        		return true;
        	if(txt.mouseDragged(mouseX, mouseY, button, deltaX, deltaY))
        		return true;
        	return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        
        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
        	if(btnDelete.mouseReleased(mouseX, mouseY, button))
        		return true;
        	if(txt.mouseReleased(mouseX, mouseY, button))
        		return true;
        	return super.mouseReleased(mouseX, mouseY, button);
        }     
        
        @Override
        public boolean mouseScrolled(double p_mouseScrolled_1_, double p_mouseScrolled_3_, double p_mouseScrolled_5_) {
          	if(btnDelete.mouseScrolled( p_mouseScrolled_1_,  p_mouseScrolled_3_,  p_mouseScrolled_5_))
        		return true;
        	if(txt.mouseScrolled( p_mouseScrolled_1_,  p_mouseScrolled_3_,  p_mouseScrolled_5_))
        		return true;
        	return super.mouseScrolled(p_mouseScrolled_1_, p_mouseScrolled_3_, p_mouseScrolled_5_);
        }
        
        @Override
        public boolean charTyped(char p_charTyped_1_, int p_charTyped_2_) {
        	if (txt.isFocused()) 
        		return txt.charTyped(p_charTyped_1_, p_charTyped_2_);
        	return super.charTyped(p_charTyped_1_, p_charTyped_2_);
        }
        
        @Override
        public boolean keyPressed(int key, int action, int mods) {
        	if (txt.isFocused()) 
        		return txt.keyPressed(key, action, mods);
        	return super.keyPressed(key, action, mods);
        }
        
        @Override
		public void render(int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean p_194999_5_,float partialTicks)
        {
        	txt.x = x;
        	txt.y = y;
        	
        	txt.render(mouseX, mouseY, partialTicks);
        	//GuiQuickCommandsList.this.minecraft.fontRenderer.drawString(command, x + 40  - GuiQuickCommandsList.this.maxListLabelWidth, y + p_148279_5_ / 2 - GuiQuickCommandsList.this.minecraft.fontRenderer.FONT_HEIGHT / 2, 16777215);

        	this.btnDelete.x =txt.x+txt.getWidth() + 2;
        	this.btnDelete.y= txt.y;
        	this.btnDelete.visible = true;
        	this.btnDelete.render(mouseX, mouseY, partialTicks);

        }

    }
}
