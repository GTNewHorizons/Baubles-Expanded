package baubles.client.gui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;

public class GuiScreenReflectionHelper {

    private static final Field BUTTON_LIST = ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n");
    private static final Field GUI_LEFT = ReflectionHelper.findField(GuiContainer.class, "guiLeft", "field_147003_i");
    private static final Field GUI_TOP = ReflectionHelper.findField(GuiContainer.class, "guiTop", "field_147009_r");
    private static final Method DRAW_HOVERING_TEXT = ReflectionHelper.findMethod(GuiScreen.class, null,
        new String[] {"drawHoveringText"}, List.class, int.class, int.class, FontRenderer.class);

    private GuiScreenReflectionHelper() {}

    @SuppressWarnings("unchecked")
    public static List<GuiButton> getButtons(GuiScreen screen) {
        try {
            return (List<GuiButton>) BUTTON_LIST.get(screen);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not read screen buttons", e);
        }
    }

    public static int getLeft(GuiContainer screen) {
        try {
            return GUI_LEFT.getInt(screen);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not read container position", e);
        }
    }

    public static int getTop(GuiContainer screen) {
        try {
            return GUI_TOP.getInt(screen);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not read container position", e);
        }
    }

    public static void drawTooltip(GuiScreen screen, List<String> text, int mouseX, int mouseY) {
        try {
            DRAW_HOVERING_TEXT.invoke(screen, text, mouseX, mouseY, screen.mc.fontRenderer);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not draw screen tooltip", e);
        }
    }
}
