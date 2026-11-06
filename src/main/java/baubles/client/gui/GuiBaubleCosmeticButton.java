package baubles.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class GuiBaubleCosmeticButton extends GuiButton {

    private static final ResourceLocation OFF = new ResourceLocation("baubles", "textures/gui/cosmetic_off.png");
    private static final ResourceLocation OFF_HIGHLIGHTED = new ResourceLocation("baubles", "textures/gui/cosmetic_off_highlighted.png");
    private static final ResourceLocation ON = new ResourceLocation("baubles", "textures/gui/cosmetic_on.png");
    private static final ResourceLocation ON_HIGHLIGHTED = new ResourceLocation("baubles", "textures/gui/cosmetic_on_highlighted.png");
    private final boolean cosmeticView;

    public GuiBaubleCosmeticButton(int id, int x, int y, boolean cosmeticView) {
        super(id, x, y, 20, 17, I18n.format("button.baubles.cosmetics"));
        this.cosmeticView = cosmeticView;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        field_146123_n = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
        ResourceLocation texture = cosmeticView ? (field_146123_n ? ON_HIGHLIGHTED : ON)
                : (field_146123_n ? OFF_HIGHLIGHTED : OFF);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(texture);
        func_146110_a(xPosition, yPosition, 0, 0, width, height, width, height);
        GL11.glPopAttrib();
    }
}
