package baubles.client.gui;

import baubles.api.IBauble;
import baubles.api.expanded.IBaubleExpanded;
import codechicken.lib.vec.Rectangle4i;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;

import net.minecraft.util.StatCollector;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static baubles.common.BaublesConfig.useOldGuiRendering;
import static net.minecraft.client.gui.inventory.GuiInventory.func_147046_a;

@Optional.Interface(iface = "codechicken.nei.api.INEIGuiHandler", modid = "NotEnoughItems")
public class GuiPlayerExpanded extends GuiContainer implements INEIGuiHandler {

    public static final ResourceLocation background = new ResourceLocation("baubles","textures/gui/bauble_inventory.png");
    public static final ResourceLocation gui_background = new ResourceLocation("baubles","textures/gui/bauble_background.png");
    private static final ResourceLocation creative_inventory_tabs = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");

    private static final boolean hasLwjgl3 = Loader.isModLoaded("lwjgl3ify");

	/**
     * x size of the inventory window in pixels. Defined as float, passed as int.
     */
    private float xSizeFloat;
    /**
     * y size of the inventory window in pixels. Defined as float, passed as int.
     */
    private float ySizeFloat;

    public boolean showActivePotionEffects;

    /** Amount scrolled in inventory (0 = top, 1 = bottom) */
    private float currentScroll;
    /** True if the scrollbar is being dragged */
    private boolean isScrolling;
    /** True if the left mouse button was held down last time drawScreen was called. */
    private boolean wasClicking;

    private int tooltipIndexCache = -1;
    private final List<String> tooltipCache = new ArrayList<>(2);

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
		} catch (Exception ignored) {}
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

        if(BaublesConfig.displayTooltipOnHover) {
            handleMouseHover(mouseX, mouseY);
        }

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

        // Player model
        func_147046_a(guiLeft + 51, guiTop + 75, 30, (float) (guiLeft + 51) - xSizeFloat, (float) (guiTop + 25) - ySizeFloat, mc.thePlayer);
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
            slotStartY = guiTop + 7;
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

        // Bauble slot backgrounds
        for (int slotIndex = 0; slotIndex < BaubleExpandedSlots.slotLimit; slotIndex++) {
            String slotType = BaubleExpandedSlots.getSlotType(slotIndex);
            if (!BaublesConfig.showUnusedSlots && slotType.equals(BaubleExpandedSlots.unknownType)) {
                continue;
            }
            if (useOldGuiRendering) {
                drawTexturedModalRect(slotStartX + (slotOffset * (slotIndex / 4)), slotStartY + (slotOffset * (slotIndex % 4)), 200, 0, 18, 18);
            } else {
                drawTexturedModalRect(slotStartX + (slotOffset * (slotIndex / 4)), slotStartY + (slotOffset * slotIndex), 200, 0, 18, 18);
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

        if (potionCollection.isEmpty()) {
            return;
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_LIGHTING);
        int maxNumber = 33;

        if (potionCollection.size() > 5) {
            maxNumber = 132 / (potionCollection.size() - 1);
        }

        for (PotionEffect effect : potionCollection) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.mc.getTextureManager().bindTexture(field_147001_a);
            this.drawTexturedModalRect(positionHorizontal, positionVertical, 0, 166, 140, 32);

            if (potion.hasStatusIcon()) {
                int potionIconIndex = potion.getStatusIconIndex();
                this.drawTexturedModalRect(positionHorizontal + 6, positionVertical + 7, potionIconIndex % 8 * 18, 198 + potionIconIndex / 8 * 18, 18, 18);
            }

            potion.renderInventoryEffect(positionHorizontal, positionVertical, effect, mc);
            if (!potion.shouldRenderInvText(effect)) continue;
            String potionName = I18n.format(potion.getName());

            if (effect.getAmplifier() >= 1) {
                potionName = potionName + " " + I18n.format("enchantment.level." + effect.getAmplifier());
            }
            this.fontRendererObj.drawStringWithShadow(potionName, positionHorizontal + 10 + 18, positionVertical + 6, 16777215);
            String s = Potion.getDurationString(effect);
            this.fontRendererObj.drawStringWithShadow(s, positionHorizontal + 10 + 18, positionVertical + 6 + 10, 8355711);
            positionVertical += maxNumber;
        }
    }

    private void handleMouseHover(int mouseX, int mouseY) {
        ContainerPlayerExpanded expandedInventory = (ContainerPlayerExpanded) this.inventorySlots;

        // Check the last cached slot first, as it is the most likely one to be hovered out of all of them
        if(tooltipIndexCache != -1) {
            Slot slot = expandedInventory.getBaubleSlot(tooltipIndexCache);

            // Cursor inside slot rect
            if (this.func_146978_c(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, mouseX, mouseY)) {
                ItemStack stack = expandedInventory.baubles.getStackInSlot(tooltipIndexCache);
                if(stack == null || stack.stackSize == 0) {
                    // drawHoveringText with default font
                    func_146283_a(tooltipCache, mouseX, mouseY);
                    return;
                }
            }
        }

        // Check the other slots
        for (int slotIndex = 0; slotIndex < expandedInventory.getBaubleSlotCount(); slotIndex++) {
            if(slotIndex == tooltipIndexCache) continue;

            Slot slot = expandedInventory.getBaubleSlot(slotIndex);

            // Cursor inside slot rect
            if (!this.func_146978_c(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, mouseX, mouseY)) continue;

            ItemStack stack = expandedInventory.baubles.getStackInSlot(slotIndex);
            if(stack != null && stack.stackSize > 0) continue; // Only show tooltip on empty slots

            tooltipIndexCache = slotIndex;

            String slotType = BaubleExpandedSlots.getSlotType(slotIndex);

            tooltipCache.clear();

            // Strip formatting codes
            String strippedType = StatCollector.translateToLocal("slot." + slotType).replaceAll("§[0-9a-fklmnor]", "");
            tooltipCache.add(strippedType);

            ItemStack heldItem = mc.thePlayer.inventory.getItemStack();

            if (heldItem != null && heldItem.stackSize > 0) {
                boolean fitsInSlot = false;
                if (heldItem.getItem() instanceof IBaubleExpanded baubleExpandedItem) {
                    String[] itemBaubleTypes = baubleExpandedItem.getBaubleTypes(heldItem);
                    for(String itemBaubleType : itemBaubleTypes) {
                        if (itemBaubleType.equals(BaubleExpandedSlots.universalType) || slotType.equals(itemBaubleType)) {
                            fitsInSlot = true;
                            break;
                        }
                    }
                }
                else if(heldItem.getItem() instanceof IBauble baubleItem) {
                    String itemBaubleType = BaubleExpandedSlots.getTypeFromBaubleType(baubleItem.getBaubleType(heldItem));
                    if (itemBaubleType.equals(BaubleExpandedSlots.universalType) || slotType.equals(itemBaubleType)) {
                        fitsInSlot = true;
                    }
                }

                tooltipCache.add(fitsInSlot
                    ? StatCollector.translateToLocal("tooltip.fitsInSlot")
                    : StatCollector.translateToLocal("tooltip.doesNotFitInSlot"));
            }

            // drawHoveringText with default font
            func_146283_a(tooltipCache, mouseX, mouseY);
            return;
        }

        tooltipIndexCache = -1;
    }

    private boolean needsScrollBars() {
        return ((ContainerPlayerExpanded) this.inventorySlots).canScroll();
    }

    private void handleScrollbar(int mouseX, int mouseY) {
        boolean leftMouseDown = Mouse.isButtonDown(0);

        if (!this.wasClicking && leftMouseDown && isClickInScrollbar(mouseX, mouseY)) {
            this.isScrolling = this.needsScrollBars();
        }

        if (!leftMouseDown) {
            this.isScrolling = false;
        }

        this.wasClicking = leftMouseDown;

        if (this.isScrolling) {
            int scrollbarYStart = this.guiTop + 12;
            int scrollbarYEnd = scrollbarYStart + 139;

            this.currentScroll = ((float) (mouseY - scrollbarYStart) - 7.5F) /
                ((float) (scrollbarYEnd - scrollbarYStart) - 15.0F);

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
        if (wheel == 0 || !this.needsScrollBars()) {
            return;
        }
        if (!hasLwjgl3) {
            // LWJGL2 reports different scroll values for every platform, 120 for one tick on Windows.
            // LWJGL3 reports the delta in exact scroll ticks.
            // Round away from zero to avoid dropping small scroll events
            if (wheel > 0) {
                wheel = Math.addExact(Math.addExact(wheel, 120), -1) / 120;
            } else {
                wheel = -(int) Math.addExact(Math.addExact(-wheel, 120), -1) / 120;
            }
        }
        int i = BaubleExpandedSlots.slotsCurrentlyUsed();
        this.currentScroll = (float) ((double) this.currentScroll - wheel / (double) i);
        this.currentScroll = MathHelper.clamp_float(this.currentScroll, 0.0F, 1.0F);
        ((ContainerPlayerExpanded) this.inventorySlots).scrollTo(this.currentScroll);
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
    protected void handleMouseClick(Slot slotIn, int slotId, int clickedButton, int clickType) {
        if (slotIn != null && clickType == 4 && slotIn.xDisplayPosition < 0 && !useOldGuiRendering) {
            clickType = 0;
        }
        super.handleMouseClick(slotIn, slotId, clickedButton, clickType);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isClickInUI(mouseX, mouseY)) { // Prevent dropping items when clicking in UI
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int mouseButton) {
        if (isClickInUI(mouseX, mouseY)) { // Prevent dropping items when clicking in UI
            return;
        }

        super.mouseMovedOrUp(mouseX, mouseY, mouseButton);
    }

    /**
     * Returns true if the mouse is clicked in the scroll bar.
     */
    private boolean isClickInScrollbar(int mouseX, int mouseY) {
        int scrollbarXStart = this.guiLeft - 34;
        int scrollbarYStart = this.guiTop + 12;
        int scrollbarXEnd = scrollbarXStart + 14;
        int scrollbarYEnd = scrollbarYStart + 139;

        return mouseX >= scrollbarXStart && mouseY >= scrollbarYStart &&
            mouseX < scrollbarXEnd && mouseY < scrollbarYEnd;
    }

    /**
     * Returns true if the mouse is clicked in the scrollbar or the surrounding area.
     */
    private boolean isClickInUI(int mouseX, int mouseY) {
        int scrollbarXStart = this.guiLeft - 42;
        int scrollbarYStart = this.guiTop + 5;
        int scrollbarXEnd = scrollbarXStart + 27;
        int scrollbarYEnd = scrollbarYStart + 156;

        return mouseX >= scrollbarXStart && mouseY >= scrollbarYStart &&
            mouseX < scrollbarXEnd && mouseY < scrollbarYEnd;
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
        if (!(gui instanceof GuiPlayerExpanded) || useOldGuiRendering) {
            return false;
        }
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
        for (PotionEffect effect : activePotionEffects) {
            Rectangle4i box = new Rectangle4i(x, y, 140, 32);
            box.include(baubleSlots);
            if (box.intersects(slotRect)) return true;
            y += height;
        }
        return false;
    }
}
