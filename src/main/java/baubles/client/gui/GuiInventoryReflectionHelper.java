package baubles.client.gui;

import java.lang.reflect.Field;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.gui.inventory.GuiInventory;

final class GuiInventoryReflectionHelper {

    private static final Field MOUSE_X = ReflectionHelper.findField(
        GuiInventory.class,
        "xSizeFloat",
        "field_147048_u");
    private static final Field MOUSE_Y = ReflectionHelper.findField(
        GuiInventory.class,
        "ySizeFloat",
        "field_147047_v");

    private GuiInventoryReflectionHelper() {}

    static void setMousePosition(GuiInventory inventory, float mouseX, float mouseY) {
        try {
            MOUSE_X.setFloat(inventory, mouseX);
            MOUSE_Y.setFloat(inventory, mouseY);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not initialize the inventory mouse position", e);
        }
    }
}
