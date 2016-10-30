package de.universallp.va.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

import java.awt.*;

/**
 * Created by universallp on 21.03.2016 17:53 16:31.
 * This file is part of VanillaAutomation which is licenced
 * under the MOZILLA PUBLIC LICENCE 2.0 - mozilla.org/en-US/MPL/2.0/
 * github.com/UniversalLP/VanillaAutomation
 */
public class ButtonLabel extends GuiButton {

    private Color textColor;
    private ButtonIcon.IconType icon;

    public ButtonLabel(String unlocalizedText, int id, int x, int y) {
        super(id, x, y, unlocalizedText);
        textColor = new Color(255, 255, 255);
        width = Minecraft.getMinecraft().fontRendererObj.getStringWidth(I18n.format(unlocalizedText));
        height = Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
    }

    public ButtonLabel(String unlocalizedText, ButtonIcon.IconType icon, int id, int x, int y) {
        super(id, x, y, unlocalizedText);
        this.icon = icon;
        textColor = new Color(255, 255, 255);
        width = Minecraft.getMinecraft().fontRendererObj.getStringWidth(I18n.format(unlocalizedText)) + 2 + icon.getWidht();
        if (icon != ButtonIcon.IconType.CHECKED && icon != ButtonIcon.IconType.UNCHECKED)
            height = Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT > icon.getHeight() ? Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT : icon.getHeight();
        else
            height = Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
    }

    @Override
    public void drawButtonForegroundLayer(int mouseX, int mouseY) {

    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        int offset = 0;

        if (icon != null) {
            offset = icon.getWidht() + 3;
            icon.draw(this, true);
        }
        if (visible)
            if (enabled)
                if (isMouseOver(mouseX, mouseY))
                    mc.fontRendererObj.drawString(I18n.format(displayString), xPosition + offset, yPosition, 16777120, true);
                else
                    mc.fontRendererObj.drawString(I18n.format(displayString), xPosition + offset, yPosition, textColor.getRGB(), true);
            else
                mc.fontRendererObj.drawString(I18n.format(displayString), xPosition + offset, yPosition, 10526880, true);
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX > xPosition && mouseX < xPosition + width + 4 && mouseY > yPosition && mouseY < yPosition + height;
    }

    public ButtonIcon.IconType getIcon() {
        return icon;
    }

    public void setIcon(ButtonIcon.IconType icon) {
        this.icon = icon;
    }
}
