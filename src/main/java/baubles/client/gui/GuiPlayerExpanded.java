package baubles.client.gui;

import codechicken.lib.vec.Rectangle4i;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import cpw.mods.fml.common.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import baubles.api.expanded.BaubleExpandedSlots;
import baubles.common.Baubles;
import baubles.common.BaublesConfig;
import baubles.common.container.ContainerPlayerExpanded;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.achievement.GuiAchievements;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static net.minecraft.client.gui.inventory.GuiInventory.func_147046_a;

@Optional.Interface(iface = "codechicken.nei.api.INEIGuiHandler", modid = "NotEnoughItems")
public class GuiPlayerExpanded extends GuiContainer implements INEIGuiHandler {

    public static final ResourceLocation gui_background = new ResourceLocation("baubles","textures/gui/bauble_background.png");

    private static final ResourceLocation creative_inventory_tabs = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");

	/**
     * x size of the inventory window in pixels. Defined as  float, passed as int.
     */
    private float xSizeFloat;
    /**
     * y size of the inventory window in pixels. Defined as  float, passed as int.
     */
    private float ySizeFloat;

    public boolean showActivePotionEffects;

    public GuiPlayerExpanded(EntityPlayer player) {
        super(new ContainerPlayerExpanded(player.inventory, !player.worldObj.isRemote, player));
        allowUserInput = true;
    }

    /**
     * Called from the main game loop to update the screen.
     */
    @Override
    public void updateScreen() {
    	try {
			((ContainerPlayerExpanded)inventorySlots).baubles.blockEvents = false;
		} catch (Exception e) {}
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    @Override
    public void initGui() {
        buttonList.clear();
        super.initGui();

        if (!this.mc.thePlayer.getActivePotionEffects().isEmpty()) {
            this.showActivePotionEffects = true;
        }
    }

    /**
     * Draws the screen and all the components in it.
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        boolean flag = Mouse.isButtonDown(0);
        xSizeFloat = (float) mouseX;
        ySizeFloat = (float) mouseY;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRendererObj.drawString(I18n.format("container.crafting"), 86, 16, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(GuiInventory.field_147001_a);
        this.drawBaubleSlots();
        if (showActivePotionEffects) {
            drawPotionEffects();
        }

        //Player Model
        func_147046_a(guiLeft + 51, guiTop + 75, 30, (float)(guiLeft + 51) - xSizeFloat, (float)(guiTop + 25) - ySizeFloat, mc.thePlayer);
    }

    private void drawBaubleSlots() {
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        int upperHeight = 7 + BaubleExpandedSlots.slotsCurrentlyUsed() * 18;
        this.mc.getTextureManager().bindTexture(gui_background);
        this.drawTexturedModalRect(this.guiLeft - 26, this.guiTop + 4, 0, 0, 27, upperHeight);

        final int slotOffset = 18;
        final int slotStartX = guiLeft - 26;
        final int slotStartY = 12;

        if (BaubleExpandedSlots.slotsCurrentlyUsed() <= 8 || !BaublesConfig.showUnusedSlots) {
            this.drawTexturedModalRect(this.guiLeft - 26, this.guiTop + 4 + upperHeight, 0, 151, 27, 7);
        } else {
            this.drawTexturedModalRect(this.guiLeft - 42, this.guiTop + 4, 27, 0, 23, 158);
            this.mc.getTextureManager().bindTexture(creative_inventory_tabs);
            //this.drawTexturedModalRect(this.guiLeft - 34, this.guiTop + 12 + (int) (127f * this.currentScroll), 232, 0, 12, 15);
        }

        //bauble slot backgrounds
        for (int slotIndex = 0; slotIndex < BaubleExpandedSlots.slotLimit; slotIndex++) {
            String slotType = BaubleExpandedSlots.getSlotType(slotIndex);
            if (BaublesConfig.showUnusedSlots || !slotType.equals(BaubleExpandedSlots.unknownType)) {
                //Slot slot = (Slot)inventorySlots.inventorySlots.get(slotIndex + 4);
                drawTexturedModalRect(slotStartX + (slotOffset * (slotIndex / 4)), slotStartY + (slotOffset * slotIndex), 200, 0, 18, 18);
            }
        }
    }

    private void drawPotionEffects() {
        int positionHorizontal = guiLeft - 26 - 124;
        int positionVertical = guiTop;
        Collection<PotionEffect> potionCollection = this.mc.thePlayer.getActivePotionEffects();

        if (!potionCollection.isEmpty()) {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glDisable(GL11.GL_LIGHTING);
            int maxNumber = 33;

            if (potionCollection.size() > 5) {
                maxNumber = 132 / (potionCollection.size() - 1);
            }

            for (Iterator iterator = this.mc.thePlayer.getActivePotionEffects().iterator(); iterator.hasNext(); positionVertical += maxNumber) {
                PotionEffect potioneffect = (PotionEffect)iterator.next();
                Potion potion = Potion.potionTypes[potioneffect.getPotionID()];
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                this.mc.getTextureManager().bindTexture(field_147001_a);
                this.drawTexturedModalRect(positionHorizontal, positionVertical, 0, 166, 140, 32);

                if (potion.hasStatusIcon()) {
                    int potionIconIndex = potion.getStatusIconIndex();
                    this.drawTexturedModalRect(positionHorizontal + 6, positionVertical + 7, potionIconIndex % 8 * 18, 198 + potionIconIndex / 8 * 18, 18, 18);
                }

                potion.renderInventoryEffect(positionHorizontal, positionVertical, potioneffect, mc);
                if (!potion.shouldRenderInvText(potioneffect)) continue;
                String potionName = I18n.format(potion.getName());

                if (potioneffect.getAmplifier() >= 1) {
                    potionName = potionName + " " + I18n.format("enchantment.level." + potioneffect.getAmplifier());
                }
                this.fontRendererObj.drawStringWithShadow(potionName, positionHorizontal + 10 + 18, positionVertical + 6, 16777215);
                String s = Potion.getDurationString(potioneffect);
                this.fontRendererObj.drawStringWithShadow(s, positionHorizontal + 10 + 18, positionVertical + 6 + 10, 8355711);
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            mc.displayGuiScreen(new GuiAchievements(this, mc.thePlayer.getStatFileWriter()));
        } else if (button.id == 1) {
            mc.displayGuiScreen(new GuiStats(this, mc.thePlayer.getStatFileWriter()));
        }
    }

	@Override
	protected void keyTyped(char par1, int keyCode) {
		if (keyCode == Baubles.proxy.keyHandler.key.getKeyCode()) {
            mc.thePlayer.closeScreen();
        } else {
        	super.keyTyped(par1, keyCode);
        }
	}

    @Override
    @Optional.Method(modid = "NotEnoughItems")
    public VisiblityData modifyVisiblity(GuiContainer gui, VisiblityData currentVisibility) {
        return null;
    }

    @Override
    @Optional.Method(modid = "NotEnoughItems")
    public Iterable<Integer> getItemSpawnSlots(GuiContainer gui, ItemStack item) {
        return null;
    }

    @Override
    @Optional.Method(modid = "NotEnoughItems")
    public List<TaggedInventoryArea> getInventoryAreas(GuiContainer gui) {
        return Collections.emptyList();
    }

    @Override
    @Optional.Method(modid = "NotEnoughItems")
    public boolean handleDragNDrop(GuiContainer gui, int mousex, int mousey, ItemStack draggedStack, int button) {
        return false;
    }

    @Override
    @Optional.Method(modid = "NotEnoughItems")
    public boolean hideItemPanelSlot(GuiContainer gui, int slotX, int slotY, int slotW, int slotH) {
        int upperHeight = 7 + BaubleExpandedSlots.slotsCurrentlyUsed() * 18;
        if (gui instanceof GuiPlayerExpanded) {
            if (NEIClientConfig.ignorePotionOverlap()) {
                return (new Rectangle4i( guiLeft - 26, guiTop + 4, 18, upperHeight + 4).intersects(new Rectangle4i(slotX, slotY, slotW, slotH)));
            }
            int x = this.guiLeft - 124 - 26;
            int y = this.guiTop;
            Minecraft minecraft = gui.mc;
            if (minecraft == null) {
                return false;
            }
            EntityPlayerSP player = minecraft.thePlayer;
            if (player == null) {
                return false;
            }
            Collection<PotionEffect> activePotionEffects = player.getActivePotionEffects();
            if (activePotionEffects.isEmpty()) {
                return (new Rectangle4i( guiLeft - 26, guiTop + 4, 18, upperHeight + 4).intersects(new Rectangle4i(slotX, slotY, slotW, slotH)));
            }
            int height = 33;
            if (activePotionEffects.size() > 5) {
                height = 132 / (activePotionEffects.size() - 1);
            }
            Rectangle4i slotRect = new Rectangle4i(slotX, slotY, slotW, slotH);
            Rectangle4i baubleSlots = new Rectangle4i( guiLeft - 26, guiTop + 4, 18, upperHeight + 4);
            for (PotionEffect potioneffect : activePotionEffects) {
                Rectangle4i box = new Rectangle4i(x, y, 140, 32);
                box.include(baubleSlots);
                if (box.intersects(slotRect)) return true;
                y += height;
            }
        }
        return false;
    }
}
