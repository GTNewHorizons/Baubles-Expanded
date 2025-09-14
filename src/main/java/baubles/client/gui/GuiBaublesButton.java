package baubles.client.gui;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.OpenGlHelper;

import static baubles.common.BaublesConfig.useOldGuiButton;

public class GuiBaublesButton extends GuiButton {

    public GuiBaublesButton(int buttonId, int xIn, int yIn, int widthIn, int heightIn, String resource) {
        super(buttonId, xIn, yIn, widthIn, heightIn, resource);
    }

	public void drawButton(Minecraft mc, int xx, int yy) {
        if (!this.visible) {
            return;
        }

        FontRenderer fontrenderer = mc.fontRenderer;
        if (useOldGuiButton) {
            mc.getTextureManager().bindTexture(GuiPlayerExpanded.background);
        } else {
            mc.getTextureManager().bindTexture(GuiPlayerExpanded.gui_background);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.field_146123_n = xx >= this.xPosition && yy >= this.yPosition && xx < this.xPosition + this.width && yy < this.yPosition + this.height;
        int hover = this.getHoverState(this.field_146123_n);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        if (hover == 1) {
            if (useOldGuiButton) {
                this.drawTexturedModalRect(this.xPosition, this.yPosition, 200, 48, 10, 10);
            } else {
                this.drawTexturedModalRect(this.xPosition, this.yPosition, 50, 0, 14, 14);
            }
            return;
        }

        if (useOldGuiButton) {
            this.drawTexturedModalRect(this.xPosition, this.yPosition, 210, 48, 10, 10);
            this.drawCenteredString(fontrenderer, this.displayString,
                this.xPosition + 5, this.yPosition + this.height, 0xffffff);
        } else {
            this.drawTexturedModalRect(this.xPosition, this.yPosition, 50, 14, 14, 14);

            int labelWidth = fontrenderer.getStringWidth(this.displayString);

            int labelX = this.xPosition + 20;
            int labelY = this.yPosition - this.height;
            int labelHeight = 8;

            int borderColorDark  = 0xF0100010;
            int borderColorLight = 0x505000FF;
            int borderColorLightFaded = (borderColorLight & 0xFEFEFE) >> 1 | borderColorLight & 0xFF000000;

            this.drawGradientRect(labelX - 3, labelY - 4, labelX + labelWidth + 3, labelY - 3, borderColorDark, borderColorDark);
            this.drawGradientRect(labelX - 3, labelY + labelHeight + 3, labelX + labelWidth + 3, labelY + labelHeight + 4, borderColorDark, borderColorDark);
            this.drawGradientRect(labelX - 3, labelY - 3, labelX + labelWidth + 3, labelY + labelHeight + 3, borderColorDark, borderColorDark);
            this.drawGradientRect(labelX - 4, labelY - 3, labelX - 3, labelY + labelHeight + 3, borderColorDark, borderColorDark);
            this.drawGradientRect(labelX + labelWidth + 3, labelY - 3, labelX + labelWidth + 4, labelY + labelHeight + 3, borderColorDark, borderColorDark);

            this.drawGradientRect(labelX - 3, labelY - 2, labelX - 2, labelY + labelHeight + 2, borderColorLight, borderColorLightFaded);
            this.drawGradientRect(labelX + labelWidth + 2, labelY - 2, labelX + labelWidth + 3, labelY + labelHeight + 2, borderColorLight, borderColorLightFaded);
            this.drawGradientRect(labelX - 3, labelY - 3, labelX + labelWidth + 3, labelY - 2, borderColorLight, borderColorLight);
            this.drawGradientRect(labelX - 3, labelY + labelHeight + 2, labelX + labelWidth + 3, labelY + labelHeight + 3, borderColorLightFaded, borderColorLightFaded);

            this.drawString(fontrenderer, this.displayString, labelX, labelY, 0xFFFFFF);
        }
        this.mouseDragged(mc, xx, yy);
    }
}
