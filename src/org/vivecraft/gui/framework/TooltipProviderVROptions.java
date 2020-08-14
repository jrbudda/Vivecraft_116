package org.vivecraft.gui.framework;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.vivecraft.settings.VRSettings;
import org.vivecraft.utils.Utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.optifine.Lang;
import net.optifine.gui.TooltipProvider;

public class TooltipProviderVROptions implements TooltipProvider
{
    public Rectangle getTooltipBounds(Screen guiScreen, int x, int y)
    {
        int i = guiScreen.width / 2 - 150;
        int j = guiScreen.height / 6 - 7;

        if (y <= j + 98)
        {
            j += 105;
        }

        int k = i + 150 + 150;
        int l = j + 84 + 10;
        return new Rectangle(i, j, k - i, l - j);
    }

    public boolean isRenderBorder()
    {
        return false;
    }

    public String[] getTooltipLines(Widget btn, int width)
	{
    	if (!(btn instanceof GuiVROptionButton))
    		return null;

		VRSettings.VrOptions option = ((GuiVROptionButton)btn).getOption();
		if (option == null)
			return null;

		String key = "vivecraft.options." + option.name() + ".tooltip";
		String str = Lang.get(key, null);

		if (str == null)
			return null;

		String[] lines = str.split("\\r?\\n", -1);

		List<String> newLines = new ArrayList<>();
		for (String line : lines) {
			if (line.isEmpty()) {
				newLines.add(line);
				continue;
			}

			int spaceCount = line.indexOf(line.trim().charAt(0));
			StringTextComponent spaces = spaceCount > 0 ? new StringTextComponent(String.join("", Collections.nCopies(spaceCount, " "))) : null;
			List<ITextProperties> list = Utils.wrapText(new StringTextComponent(line), width, Minecraft.getInstance().fontRenderer, spaces);

			Style style = Style.EMPTY;
			for (ITextProperties text : list) {
				newLines.add(Utils.styleToFormatString(style) + text.getString());

				String s = text.getString();
				for (int i = 0; i < s.length(); i++) {
					if (s.charAt(i) == '\u00a7') {
						if (i + 1 >= s.length())
							break;

						char c = s.charAt(i + 1);
						TextFormatting format = TextFormatting.fromFormattingCode(c);
						if (format != null)
							style = style.forceFormatting(format);

						i++;
					}
				}
			}
		}

		return newLines.toArray(new String[0]);
    }
}
