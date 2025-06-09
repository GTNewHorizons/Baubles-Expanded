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
import net.minecraft.util.MathHelper;

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

import static baubles.common.BaublesConfig.useOldGuiRendering;
import static net.minecraft.client.gui.inventory.GuiInventory.func_147046_a;

@Optional.Interface(iface = "codechicken.nei.api.INEIGuiHandler", modid = "NotEnoughItems")
public class GuiPlayerExpanded extends GuiContainer implements INEIGuiHandler {

    public static final ResourceLocation background = new ResourceLocation("baubles","textures/gui/bauble_inventory.png");
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

    /** Amount scrolled in inventory (0 = top, 1 = bottom) */
    private float currentScroll;
    /** True if the scrollbar is being dragged */
    private boolean isScrolling;
    /** True if the left mouse button was held down last time drawScreen was called. */
    private boolean wasClicking;

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
			((ContainerPlayerExpanded) inventorySlots).baubles.blockEvents = false;
		} catch (Exception e) {}
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    @Override
    public void initGui() {
        buttonList.clear();
        super.initGui();

        if (!this.mc.thePlayer.getActivePotionEffects().isEmpty() && !useOldGuiRendering) {
            this.showActivePotionEffects = true;
        }
    }

    /**
     * Draws the screen and all the components in it.
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        xSizeFloat = (float) mouseX;
        ySizeFloat = (float) mouseY;

        handleScrollbar(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        if (!useOldGuiRendering) {
            this.fontRendererObj.drawString(I18n.format("container.crafting"), 86, 16, 4210752);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (useOldGuiRendering) {
            mc.getTextureManager().bindTexture(background);
        } else {
            mc.getTextureManager().bindTexture(GuiInventory.field_147001_a);
        }

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
        if (!useOldGuiRendering) {
            this.mc.getTextureManager().bindTexture(gui_background);
        }

        final int slotOffset = 18;
        int slotStartX = guiLeft - 26;
        int slotStartY = 12;

        if (useOldGuiRendering) {
            slotStartX = guiLeft + 79;
            slotStartY = guiTop +7;
        } else {
            if (BaubleExpandedSlots.slotsCurrentlyUsed() <= 8) {
                this.drawTexturedModalRect(this.guiLeft - 26, this.guiTop + 4, 0, 0, 27, upperHeight);
                this.drawTexturedModalRect(this.guiLeft - 26, this.guiTop + 4 + upperHeight, 0, 151, 27, 7);
            } else {
                this.drawTexturedModalRect(this.guiLeft - 26, this.guiTop + 4, 0, 0, 27, 158);
                this.drawTexturedModalRect(this.guiLeft - 42, this.guiTop + 4, 27, 0, 23, 158);
                this.mc.getTextureManager().bindTexture(creative_inventory_tabs);
                this.drawTexturedModalRect(this.guiLeft - 34, this.guiTop + 12 + (int) (127f * this.currentScroll), 232, 0, 12, 15);
            }
        }

        //bauble slot backgrounds
        for (int slotIndex = 0; slotIndex < BaubleExpandedSlots.slotLimit; slotIndex++) {
            String slotType = BaubleExpandedSlots.getSlotType(slotIndex);
            if (BaublesConfig.showUnusedSlots || !slotType.equals(BaubleExpandedSlots.unknownType)) {
                if (useOldGuiRendering) {
                    drawTexturedModalRect(slotStartX + (slotOffset * (slotIndex / 4)), slotStartY + (slotOffset * (slotIndex % 4)), 200, 0, 18, 18);
                } else {
                    drawTexturedModalRect(slotStartX + (slotOffset * (slotIndex / 4)), slotStartY + (slotOffset * slotIndex), 200, 0, 18, 18);
                }
            }
        }
    }

    private void drawPotionEffects() {
        int slotIndent = 26;
        if (BaubleExpandedSlots.slotsCurrentlyUsed() > 8) {
            slotIndent = 42;
        }
        int positionHorizontal = guiLeft - slotIndent - 124;
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

    private boolean needsScrollBars() {
        return ((ContainerPlayerExpanded) this.inventorySlots).canScroll();
    }

    private void handleScrollbar(int mouseX, int mouseY) {
        boolean flag = Mouse.isButtonDown(0);
        int k = this.guiLeft;
        int l = this.guiTop;
        int i1 = k - 34;
        int j1 = l + 12;
        int k1 = i1 + 14;
        int l1 = j1 + 139;

        if (!this.wasClicking && flag && mouseX >= i1 && mouseY >= j1 && mouseX < k1 && mouseY < l1) {
            this.isScrolling = this.needsScrollBars();
        }

        if (!flag) {
            this.isScrolling = false;
        }

        this.wasClicking = flag;

        if (this.isScrolling) {
            this.currentScroll = ((float)(mouseY - j1) - 7.5F) / ((float)(l1 - j1) - 15.0F);

            if (this.currentScroll < 0.0F) {
                this.currentScroll = 0.0F;
            }

            if (this.currentScroll > 1.0F) {
                this.currentScroll = 1.0F;
            }

            ((ContainerPlayerExpanded) this.inventorySlots).scrollTo(this.currentScroll);
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (this.needsScrollBars()) {
            int i = BaubleExpandedSlots.slotsCurrentlyUsed();
            this.currentScroll = (float)((double)this.currentScroll - wheel / (double) i);
            this.currentScroll = MathHelper.clamp_float(this.currentScroll, 0.0F, 1.0F);
            ((ContainerPlayerExpanded)this.inventorySlots).scrollTo(this.currentScroll);
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
        if (gui instanceof GuiPlayerExpanded && !useOldGuiRendering) {

            int slotIndent = 26;
            int slotWidth = 18;
            if (BaubleExpandedSlots.slotsCurrentlyUsed() > 8) {
                slotIndent = 42;
                slotWidth = 36;
            }
            if (NEIClientConfig.ignorePotionOverlap()) {
                return (new Rectangle4i( guiLeft - slotIndent, guiTop + 4, slotWidth, upperHeight + 4).intersects(new Rectangle4i(slotX, slotY, slotW, slotH)));
            }
            int x = this.guiLeft - 124 - slotIndent;
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
                return (new Rectangle4i( guiLeft - slotIndent, guiTop + 4, slotWidth, upperHeight + 4).intersects(new Rectangle4i(slotX, slotY, slotW, slotH)));
            }
            int height = 33;
            if (activePotionEffects.size() > 5) {
                height = 132 / (activePotionEffects.size() - 1);
            }
            Rectangle4i slotRect = new Rectangle4i(slotX, slotY, slotW, slotH);
            Rectangle4i baubleSlots = new Rectangle4i( guiLeft - slotIndent, guiTop + 4, slotWidth, upperHeight + 4);
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
