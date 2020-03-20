package org.vivecraft.gui.settings;

import org.vivecraft.gui.framework.GuiVROptionsBase;
import org.vivecraft.gui.framework.VROptionLayout;
import org.vivecraft.gui.framework.VROptionLayout.Position;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.settings.VRSettings;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import org.apache.commons.lang3.ArrayUtils;

public class GuiRadialConfiguration extends GuiVROptionsBase
{
	static VROptionLayout[] options = new VROptionLayout[] 
			{
					new VROptionLayout(VRSettings.VrOptions.RADIAL_MODE_HOLD,Position.POS_LEFT, 0, true, "")
			};

	public GuiRadialConfiguration(Screen guiScreen) {
		super( guiScreen );
	}

	private String[] arr;
	private boolean isShift = false;
	private int selectedIndex = -1;
	private GuiRadialItemsList list;
	private boolean isselectmode = false;

	public void setKey(KeyBinding key) {

		if(key != null)
			arr[selectedIndex] = key.getKeyDescription();
		else
			arr[selectedIndex] = "";

		this.selectedIndex = -1;
		this.isselectmode = false;
		this.reinit = true;
		this.visibleList = null;

		if(!this.isShift)
			minecraft.vrSettings.vrRadialItems = ArrayUtils.clone(arr);
		else
			minecraft.vrSettings.vrRadialItemsAlt = ArrayUtils.clone(arr);

		minecraft.vrSettings.saveOptions();
	}


	@Override
	public void init()
	{
		vrTitle = "Radial Menu Configuration";
		list = new GuiRadialItemsList(this, minecraft);

		this.buttons.clear();
		this.children.clear();

		if(this.isselectmode) {

			this.addButton(new Button(this.width / 2 - 155 ,  this.height -25 ,150,20, "Cancel", (p) -> {
				isselectmode = false;
				reinit = true;
				visibleList = null;
			}));
			this.addButton(new Button(this.width / 2 - 155 ,  25 ,150,20, "Clear", (p) -> {
				setKey(null);
		}));
		}else {
			if(this.isShift)
				this.addButton(new Button(this.width / 2 +2, 30, 150, 20, "Main Set", (p) -> {
						GuiRadialConfiguration.this.isShift = !GuiRadialConfiguration.this.isShift;
						GuiRadialConfiguration.this.reinit = true;
				}));      
			else
				this.addButton(new Button(this.width / 2 +2,30, 150, 20, "Alternate Set", (p) -> {
						GuiRadialConfiguration.this.isShift = !GuiRadialConfiguration.this.isShift;
						GuiRadialConfiguration.this.reinit = true;
				}));     

			super.init(options, false);

			int numButts = 8;
			int buttonwidthMin = 120;
			int degreesPerButt = 360 / numButts;
			int dist = 48;
			int centerx = this.width / 2;
			int centery = this.height / 2;
			arr = ArrayUtils.clone(minecraft.vrSettings.vrRadialItems);
			String[] alt = ArrayUtils.clone(minecraft.vrSettings.vrRadialItemsAlt);

			if(this.isShift)
				arr = alt;

			for (int i = 0; i < numButts; i++)
			{
				KeyBinding b = null;
				for (KeyBinding kb: minecraft.gameSettings.keyBindings) {
					if(kb.getKeyDescription().equalsIgnoreCase(arr[i]))
						b = kb;				
				}

				String str = ""; 
				if(b!=null)		

					str = I18n.format(b.getKeyDescription());
				int buttonwidth =  Math.max(buttonwidthMin, font.getStringWidth(str));

				int x=0,y=0;
				if(i==0) {
					x = 0;
					y = -dist; 				
				}
				else if (i==1) {
					x = buttonwidth/2 + 8;
					y = -dist/2;
				}
				else if (i==2) {
					x = buttonwidth/2 + 32;
					y = 0; 	
				}
				else if (i==3) {
					x = buttonwidth/2 + 8;
					y = dist/2;      	
				}
				else if (i==4) {
					x = 0;
					y = dist; 	
				}
				else if (i==5) {
					x = -buttonwidth/2 - 8;
					y = dist/2;      	
				}
				else if (i==6) {
					x = -buttonwidth/2 - 32;
					y = 0; 	
				}
				else if (i==7) {
					x = -buttonwidth/2 - 8;
					y = -dist/2;
				}

				final int idx = i;

				this.addButton(new Button(centerx + x - buttonwidth/2 , centery+y, buttonwidth, 20, str , (p) -> {
						GuiRadialConfiguration.this.selectedIndex = idx;
						GuiRadialConfiguration.this.isselectmode = true;
						//GuiRadialConfiguration.this.list.setEnabled(true);
						GuiRadialConfiguration.this.reinit = true;
						GuiRadialConfiguration.this.visibleList = list;
				}));    

				super.addDefaultButtons();
			}
		}
	}    

	@Override
	protected void loadDefaults() {
		this.settings.radialModeHold = true;
		this.settings.vrRadialItems = this.settings.getRadialItemsDefault();
		this.settings.vrRadialItemsAlt = new String[8];
	}

	@Override
	protected boolean onDoneClicked() {
		if(isselectmode) {
			this.isselectmode = false;
			this.reinit = true;
			GuiRadialConfiguration.this.visibleList = null;
			return true;
		}
		return false;
	}

	@Override
	public void render(int par1, int par2, float par3) {
		super.render(par1, par2, par3);

		if (GuiRadialConfiguration.this.visibleList == null)
			this.drawCenteredString(minecraft.fontRenderer, "Make sure Open Radial Menu is bound.", this.width / 2, this.height - 50, 0x55FF55);

		if(this.isShift)
			this.drawCenteredString(minecraft.fontRenderer, "Hold (Keyboard Shift) with the radial menu open to switch to this set", this.width / 2, this.height - 36, 13777015);
	}
}
