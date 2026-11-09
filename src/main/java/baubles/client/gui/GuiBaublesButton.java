package baubles.client.gui;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;

import static baubles.common.BaublesConfig.useOldGuiButton;

public class GuiBaublesButton extends GuiButton {

    private static final ResourceLocation SMALL = new ResourceLocation("baubles", "textures/gui/button_small.png");
    private static final ResourceLocation SMALL_HIGHLIGHTED = new ResourceLocation("baubles", "textures/gui/button_small_highlighted.png");
    private final boolean small;

    public GuiBaublesButton(int buttonId, int xIn, int yIn, int widthIn, int heightIn, String resource) {
        this(buttonId, xIn, yIn, widthIn, heightIn, resource, false);
    }

    public GuiBaublesButton(int buttonId, int xIn, int yIn, int widthIn, int heightIn, String resource, boolean small) {
        super(buttonId, xIn, yIn, widthIn, heightIn, resource);
        this.small = small;
    }

    @Override
    public void drawButton(Minecraft mc, int xx, int yy) {
        if (!this.visible) {
            return;
        }

        this.field_146123_n = xx >= this.xPosition && yy >= this.yPosition && xx < this.xPosition + this.width && yy < this.yPosition + this.height;
        int hover = this.getHoverState(this.field_146123_n);
        if (small) {
            mc.getTextureManager().bindTexture(hover == 2 ? SMALL_HIGHLIGHTED : SMALL);
        } else if (useOldGuiButton) {
            mc.getTextureManager().bindTexture(GuiPlayerExpanded.background);
        } else {
            mc.getTextureManager().bindTexture(GuiPlayerExpanded.gui_background);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        if (small) {
            func_146110_a(this.xPosition, this.yPosition, 0, 0, this.width, this.height, this.width, this.height);
            this.mouseDragged(mc, xx, yy);
            return;
        }

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
        } else {
            this.drawTexturedModalRect(this.xPosition, this.yPosition, 50, 14, 14, 14);
        }
        this.mouseDragged(mc, xx, yy);
    }
}
