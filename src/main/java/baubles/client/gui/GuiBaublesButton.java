package baubles.client.gui;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.OpenGlHelper;

public class GuiBaublesButton extends GuiButton {

    public GuiBaublesButton(int buttonId, int xIn, int yIn, int widthIn, int heightIn, String resource) {
        super(buttonId, xIn, yIn, widthIn, heightIn, resource);
    }

	public void drawButton(Minecraft mc, int xx, int yy) {

        if (this.visible) {
            FontRenderer fontrenderer = mc.fontRenderer;
            mc.getTextureManager().bindTexture(GuiPlayerExpanded.gui_background);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.field_146123_n = xx >= this.xPosition && yy >= this.yPosition && xx < this.xPosition + this.width && yy < this.yPosition + this.height;
            int hover = this.getHoverState(this.field_146123_n);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);


            if (hover == 1) {
            	this.drawTexturedModalRect(this.xPosition, this.yPosition, 50, 0, 14, 14);
            } else {
                this.drawTexturedModalRect(this.xPosition, this.yPosition, 50, 14, 14, 14);

                int k = 0;
                int w = fontrenderer.getStringWidth(this.displayString);

                if (w > k) {
                    k = w;
                }

                int j2 = this.xPosition + 20;
                int k2 = this.yPosition - this.height;
                int i1 = 8;

                int j1 = -267386864;
                this.drawGradientRect(j2 - 3, k2 - 4, j2 + k + 3, k2 - 3, j1, j1);
                this.drawGradientRect(j2 - 3, k2 + i1 + 3, j2 + k + 3, k2 + i1 + 4, j1, j1);
                this.drawGradientRect(j2 - 3, k2 - 3, j2 + k + 3, k2 + i1 + 3, j1, j1);
                this.drawGradientRect(j2 - 4, k2 - 3, j2 - 3, k2 + i1 + 3, j1, j1);
                this.drawGradientRect(j2 + k + 3, k2 - 3, j2 + k + 4, k2 + i1 + 3, j1, j1);
                int k1 = 1347420415;
                int l1 = (k1 & 16711422) >> 1 | k1 & -16777216;
                this.drawGradientRect(j2 - 3, k2 - 3 + 1, j2 - 3 + 1, k2 + i1 + 3 - 1, k1, l1);
                this.drawGradientRect(j2 + k + 2, k2 - 3 + 1, j2 + k + 3, k2 + i1 + 3 - 1, k1, l1);
                this.drawGradientRect(j2 - 3, k2 - 3, j2 + k + 3, k2 - 3 + 1, k1, k1);
                this.drawGradientRect(j2 - 3, k2 + i1 + 2, j2 + k + 3, k2 + i1 + 3, l1, l1);

            	this.drawString(fontrenderer, this.displayString, this.xPosition + 20,
                    this.yPosition - this.height, 0xffffff);
            }

            this.mouseDragged(mc, xx, yy);

        }
    }

}
