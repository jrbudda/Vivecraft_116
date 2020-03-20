package org.vivecraft.gui.framework;

import java.util.ArrayList;
import java.util.List;

import org.vivecraft.gui.framework.VROptionLayout.Position;
import org.vivecraft.settings.VRSettings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.text.StringTextComponent;
import net.optifine.gui.TooltipManager;
import org.lwjgl.glfw.GLFW;

public abstract class GuiVROptionsBase extends Screen
{
	public static final int DONE_BUTTON = 200;
	public static final int DEFAULTS_BUTTON = 201;

	protected final Screen lastScreen;
	protected final VRSettings settings;
	private TooltipManager tooltipManager = new TooltipManager(this, new TooltipProviderVROptions());

	protected boolean reinit;
	protected boolean drawDefaultButtons = true;
	protected ExtendedList visibleList = null;
	private int nextButtonIndex = 0;
	public String vrTitle ="Title";
	
	private Button btnDone;
	private Button btnDefaults;

	public GuiVROptionsBase(Screen lastScreen)
	{
		super(new StringTextComponent(""));
		this.lastScreen = lastScreen;
		this.settings = Minecraft.getInstance().vrSettings;
	}

	protected void addDefaultButtons() {
		
		this.addButton(btnDone = new Button(this.width / 2 + 5, this.height - 30, 150, 20, I18n.format("gui.back"), (p) ->
		{
			if (!GuiVROptionsBase.this.onDoneClicked()) {
				GuiVROptionsBase.this.minecraft.vrSettings.saveOptions();
				GuiVROptionsBase.this.minecraft.displayGuiScreen(GuiVROptionsBase.this.lastScreen);
			}
		}));
		
		this.addButton(btnDefaults = new Button(this.width / 2 - 155, this.height - 30, 150, 20, "Load Defaults", (p) ->
		{
			GuiVROptionsBase.this.loadDefaults();
			GuiVROptionsBase.this.minecraft.vrSettings.saveOptions();
			GuiVROptionsBase.this.reinit = true;
		}));
	}

	protected boolean onDoneClicked() {
		return false;
	}

	/**
	 * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
	 * window resizes, the buttonList is cleared beforehand.
	 */
	protected void init(VROptionLayout[] settings, boolean clear)
	{ // specify options and layout
		
		if (clear) {
	        this.buttons.clear();
	        this.children.clear();
		}	
		
		int i = 0;
		for (VROptionLayout layout : settings)
		{
			if (layout.getOption() !=null && layout.getOption().getEnumFloat())
			{ // Option Slider
				this.addButton(new GuiVROptionSlider(layout.getOrdinal(), layout.getX(this.width),layout.getY(this.height), layout.getOption(), layout.getOption().getValueMin(), layout.getOption().getValueMax()) {
					public void onClick(double mouseX, double mouseY) {
						if (layout.getCustomHandler() != null && layout.getCustomHandler().apply(this, new Vec2f((float)mouseX, (float)mouseY)))
							return;
						super.onClick(mouseX, mouseY);
					}
				});
			}
			else if (layout.getOption() != null)
			{ // Option Button
				this.addButton(
						new GuiVROptionButton(layout.getOrdinal(), layout.getX(this.width), layout.getY(this.height), layout.getOption(), layout.getButtonText(), (p) -> {
						if (layout.getCustomHandler() != null && layout.getCustomHandler().apply((GuiVROptionButton) p, new Vec2f((float)0, (float)0)))
							return;
						GuiVROptionsBase.this.settings.setOptionValue(((GuiVROptionButton)p).getOption());
						p.setMessage(layout.getButtonText());
				}));
			}
			else if (layout.getScreen() != null)
			{ // Screen button
				this.addButton(new GuiVROptionButton(layout.getOrdinal(), layout.getX(this.width), layout.getY(this.height), layout.getButtonText(), (p) -> {
						try {
							if (layout.getCustomHandler() != null && layout.getCustomHandler().apply((GuiVROptionButton) p, new Vec2f((float)0, (float)0)))
								return;
							GuiVROptionsBase.this.settings.saveOptions();
							GuiVROptionsBase.this.minecraft.displayGuiScreen(layout.getScreen().getConstructor(Screen.class).newInstance(GuiVROptionsBase.this));
						} catch (ReflectiveOperationException e) {
							e.printStackTrace();
						}
				}));
			}
			else if (layout.getCustomHandler() != null)
			{ // Custom click handler button
				this.addButton(new GuiVROptionButton(layout.getOrdinal(), layout.getX(this.width), layout.getY(this.height), layout.getButtonText(), (p) -> {
						layout.getCustomHandler().apply((GuiVROptionButton) p, new Vec2f((float)0, (float)0));
				}));
			}
			else { //just a button, do something with it on your own time.
				this.addButton(new GuiVROptionButton(layout.getOrdinal(), layout.getX(this.width), layout.getY(this.height), layout.getButtonText(), (p) -> {
				}));
			}
		}
		++i;
	}
	
	protected void loadDefaults() {
		
	}
	
	protected void init(VROptionEntry[] settings, boolean clear)
	{ //auto-layout a list of options.
		
		if (clear) {
	        this.buttons.clear();
	        this.children.clear();
	        this.nextButtonIndex = 0;
		}	
		
		ArrayList<VROptionLayout> layouts = new ArrayList<>();
		if (this.nextButtonIndex < this.buttons.size())
			this.nextButtonIndex = this.buttons.size();
		int j = this.nextButtonIndex;
		for (int i = 0; i < settings.length; i++) {
			Position pos = settings[i].center ? Position.POS_CENTER : (j % 2 == 0 ? Position.POS_LEFT : Position.POS_RIGHT);
			if (settings[i].center && j % 2 != 0) ++j;
			if (settings[i].option != null) {
				if (settings[i].option != VRSettings.VrOptions.DUMMY) {
					layouts.add(new VROptionLayout(settings[i].option, settings[i].customHandler, pos, (float) Math.floor(j / 2f), VROptionLayout.ENABLED, settings[i].title));
				}
			} else if (settings[i].customHandler != null) {
				layouts.add(new VROptionLayout(settings[i].customHandler, pos, (float) Math.floor(j / 2f), VROptionLayout.ENABLED, settings[i].title));
			}
			if (settings[i].center) ++j;
			++j;
		}
		this.nextButtonIndex = j;

		this.init(layouts.toArray(new VROptionLayout[0]), false);
	}

	protected void init(VRSettings.VrOptions[] settings, boolean clear) {
		VROptionEntry[] entries = new VROptionEntry[settings.length];
		for (int i = 0; i < settings.length; i++) {
			entries[i] = new VROptionEntry(settings[i]);
		}

		this.init(entries, clear);
	}


	@Override
	public void render(int mouseX, int mouseY, float partialTicks)
	{
		if (reinit) {
			this.reinit = false;
			this.init();
		}
		this.renderBackground();
		if (visibleList != null)
			visibleList.render(mouseX, mouseY, partialTicks);
		this.drawCenteredString(this.font, this.vrTitle, this.width / 2, 15, 16777215);

		if (btnDefaults != null)
			btnDefaults.visible = drawDefaultButtons;
		if (btnDone != null)
			btnDone.visible = drawDefaultButtons;

		super.render(mouseX, mouseY, partialTicks);
		this.tooltipManager.drawTooltips(mouseX, mouseY, buttons);
	}
    protected void actionPerformed(Widget button)
    {
    }

    protected void actionPerformedRightClick(Widget button)
    {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
    {
    	boolean flag = super.mouseClicked(mouseX, mouseY, mouseButton);
    	Widget Button = getSelectedButton((int)mouseX, (int)mouseY, this.buttons);

        if (Button != null)
        {
            if (!(Button instanceof GuiVROptionSlider))
            {
                Button.playDownSound(minecraft.getSoundHandler());
            }

            if (mouseButton == 0)
            {
                this.actionPerformed(Button);
            }
            else if (mouseButton == 1)
            {
                this.actionPerformedRightClick(Button);
            }
        }
        else
        {
        	if (visibleList!=null)
        		return visibleList.mouseClicked(mouseX, mouseY, mouseButton);
        }       
        return flag;

    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
    	if (visibleList != null)
    		return visibleList.mouseReleased(mouseX, mouseY, button);
    	return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    	if (visibleList != null)
    		return visibleList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    	return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
	public boolean mouseScrolled(double p_mouseScrolled_1_, double p_mouseScrolled_3_, double p_mouseScrolled_5_) {
    	if (visibleList != null)
    		visibleList.mouseScrolled(p_mouseScrolled_1_, p_mouseScrolled_3_, p_mouseScrolled_5_);
    	return super.mouseScrolled(p_mouseScrolled_1_, p_mouseScrolled_3_, p_mouseScrolled_5_);
    }

    private Widget getSelectedButton(int x, int y, List<Widget> listButtons)
    {
        for (int i = 0; i < listButtons.size(); ++i)
        {
        	Widget Button = listButtons.get(i);

            if (Button.visible && Button.isHovered())
            {
            	return Button;
            }
        }

        return null;
    }

    @Override
    public boolean keyPressed(int key, int action, int mods)
    {
        if (key == GLFW.GLFW_KEY_ESCAPE) //esc
		{
			if(!this.onDoneClicked()) {
				this.minecraft.vrSettings.saveOptions();
				this.minecraft.displayGuiScreen(GuiVROptionsBase.this.lastScreen);
			}
			return true;
        }
        else
        {
        	if(visibleList != null && visibleList.keyPressed(key, action, mods))
        		return true;
            return super.keyPressed(key, action, mods);
        }
    }
    
    @Override
    public boolean charTyped(char p_charTyped_1_, int p_charTyped_2_) {
    	if (visibleList != null && visibleList.charTyped(p_charTyped_1_, p_charTyped_2_))
    		return true;
    	return super.charTyped(p_charTyped_1_, p_charTyped_2_);
    }
}
