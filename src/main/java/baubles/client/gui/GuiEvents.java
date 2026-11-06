package baubles.client.gui;

import java.lang.reflect.Method;
import java.util.Collections;

import baubles.common.network.PacketHandler;
import baubles.common.network.PacketOpenBaublesInventory;
import baubles.common.network.PacketOpenNormalInventory;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.client.event.GuiScreenEvent;

import static baubles.common.BaublesConfig.useOldGuiButton;

public class GuiEvents {

	@SideOnly(value = Side.CLIENT)
	@SubscribeEvent
	public void guiPostInit(GuiScreenEvent.InitGuiEvent.Post event) {
        boolean creativeInventory = event.gui instanceof GuiContainerCreative;
        if (!(event.gui instanceof GuiInventory) && !(event.gui instanceof GuiPlayerExpanded) && !creativeInventory) {
            return;
        }

        int xSize = creativeInventory ? 195 : 176;
        int ySize = creativeInventory ? 136 : 166;

        int guiLeft = (event.gui.width - xSize) / 2;
        int guiTop = (event.gui.height - ySize) / 2;

        if (!event.gui.mc.thePlayer.getActivePotionEffects().isEmpty() && isNeiHidden()) {
            guiLeft = 160 + (event.gui.width - xSize - 200) / 2;
        }

        String tooltip = I18n.format((event.gui instanceof GuiPlayerExpanded) ? "button.normal" : "button.baubles");
        if (creativeInventory) {
            GuiBaublesButton button = new GuiBaublesButton(55, guiLeft, guiTop, 8, 8, tooltip, true);
            positionCreativeButton((GuiContainerCreative) event.gui, button);
            event.buttonList.add(button);
        } else if (useOldGuiButton) {
            event.buttonList.add(new GuiBaublesButton(55, guiLeft + 66, guiTop + 9, 10, 10,
                tooltip));
        } else {
            event.buttonList.add(new GuiBaublesButton(55, guiLeft + 26, guiTop + 9, 10, 10,
                tooltip));
        }

    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void guiPreDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (event.gui instanceof GuiContainerCreative creativeInventory) {
            for (GuiButton button : GuiScreenReflectionHelper.getButtons(event.gui)) {
                if (button instanceof GuiBaublesButton) positionCreativeButton(creativeInventory, button);
            }
        }
    }

    private void positionCreativeButton(GuiContainerCreative inventory, GuiButton button) {
        boolean playerInventory = inventory.func_147056_g() == CreativeTabs.tabInventory.getTabIndex();
        button.xPosition = GuiScreenReflectionHelper.getLeft(inventory)
            + (playerInventory ? 30 : 195 - button.getButtonWidth() - 6);
        button.yPosition = GuiScreenReflectionHelper.getTop(inventory) + (playerInventory ? 8 : 6);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void guiPostDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event.gui == null) return;
        if (event.gui instanceof GuiPlayerExpanded inventory) {
            inventory.drawRenderToggleTooltip(event.mouseX, event.mouseY);
        }
        for (GuiButton button : GuiScreenReflectionHelper.getButtons(event.gui)) {
            if ((button instanceof GuiBaublesButton || button instanceof GuiBaubleCosmeticButton) && button.visible && button.func_146115_a()) {
                GuiScreenReflectionHelper.drawTooltip(event.gui, Collections.singletonList(button.displayString),
                    event.mouseX, event.mouseY);
                return;
            }
        }
    }

	@SideOnly(value = Side.CLIENT)
	@SubscribeEvent
	public void guiPostAction(GuiScreenEvent.ActionPerformedEvent.Post event) {
		if (event.gui instanceof GuiInventory || event.gui instanceof GuiContainerCreative) {
			if (event.button.id == 55) {
				PacketHandler.INSTANCE.sendToServer(new PacketOpenBaublesInventory(event.gui.mc.thePlayer));
			}
		}

		if (event.gui instanceof GuiPlayerExpanded) {
			if (event.button.id == 55) {
				GuiPlayerExpanded expandedInventory = (GuiPlayerExpanded) event.gui;
                PacketOpenNormalInventory close = new PacketOpenNormalInventory(event.gui.mc.thePlayer);
				GuiInventory inventory = new GuiInventory(event.gui.mc.thePlayer);
				GuiInventoryReflectionHelper.setMousePosition(
					inventory,
					expandedInventory.getMouseX(),
					expandedInventory.getMouseY());
				event.gui.mc.displayGuiScreen(inventory);
				PacketHandler.INSTANCE.sendToServer(close);
			}
		}
	}

	static Method isNEIHidden;
	boolean isNeiHidden() {
		boolean hidden = true;
		try {
			if (isNEIHidden == null) {
				Class<?> fake = Class.forName("codechicken.nei.NEIClientConfig");
			    isNEIHidden = fake.getMethod("isHidden");
			}
			hidden = (Boolean) isNEIHidden.invoke(null);
	    } catch (Exception ignored) {}
		return hidden;
	}
}
