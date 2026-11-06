package baubles.api.expanded;

import baubles.api.BaublesApi;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BaubleItemHelper {

    /**
     * Adds one compact line of compatible slots below the item name.
     * Empty and inactive types do not add a tooltip. Repeated identical entries are ignored.
     *
     * @param tooltip The tooltip list.
     * @param types The item's types.
     * @return If shift was held.
     */
    @SideOnly(Side.CLIENT)
    public static boolean addSlotInformation(List<String> tooltip, String[] types) {
        boolean shiftHeld = GuiScreen.isShiftKeyDown();
        if (types == null || types.length == 0) return shiftHeld;
        Set<String> names = new LinkedHashSet<>();
        for (String type : types) {
            if (type == null) continue;
            type = type.trim();
            if (type.isEmpty() || BaubleExpandedSlots.unknownType.equals(type)) continue;
            String key = "slot." + type;
            String name = StatCollector.canTranslate(key) ? StatCollector.translateToLocal(key)
                    : type.substring(0, 1).toUpperCase(Locale.ROOT) + type.substring(1).toLowerCase(Locale.ROOT);
            name = EnumChatFormatting.getTextWithoutFormattingCodes(name);
            if (BaubleExpandedSlots.universalType.equals(type)) {
                names.clear();
                names.add(name);
                break;
            }
            names.add(name);
        }
        if (names.isEmpty()) return shiftHeld;

        String text = EnumChatFormatting.GOLD + StatCollector.translateToLocal("tooltip.compatibleslots") + " "
                + EnumChatFormatting.YELLOW + String.join(", ", names);
        if (!tooltip.contains(text)) tooltip.add(Math.min(1, tooltip.size()), text);
        return shiftHeld;
    }


    /**
     * Equips a bauble in an appropriate slot when right-clicked.
     *
     * @param itemStackIn The item stack being right-clicked with.
     * @param worldIn The world instance.
     * @param player The player that right-clicked.
     * @return The item stack.
     */
    public static ItemStack onBaubleRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer player) {
        if (itemStackIn == null || itemStackIn.stackSize <= 0) return itemStackIn;
        IInventory baubles = BaublesApi.getBaubles(player);
        if(baubles == null) {
            return itemStackIn;
        }
        for(int slotIndex = 0; slotIndex < baubles.getSizeInventory(); slotIndex++) {
            if(baubles.getStackInSlot(slotIndex) == null && baubles.isItemValidForSlot(slotIndex, itemStackIn)) {
                if(!worldIn.isRemote) {
                    ItemStack equipped = itemStackIn.copy();
                    equipped.stackSize = 1;
                    if (!player.capabilities.isCreativeMode) {
                        itemStackIn.stackSize--;
                    }
                    baubles.setInventorySlotContents(slotIndex, equipped);
                    if (!player.capabilities.isCreativeMode && itemStackIn.stackSize <= 0
                            && player.inventory.getCurrentItem() == itemStackIn) {
                        player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                    }
                    return itemStackIn;
                }
            }
        }
        return itemStackIn;
    }

}
