package baubles.client.event;

import baubles.api.IBauble;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.BaubleItemHelper;
import baubles.api.expanded.IBaubleExpanded;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

@SideOnly(Side.CLIENT)
public class EventHandlerItemTooltip {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.itemStack;
        if (stack == null || !(stack.getItem() instanceof IBauble bauble)) return;

        String header = StatCollector.translateToLocal("tooltip.compatibleslots");
        for (String line : event.toolTip) {
            String plain = EnumChatFormatting.getTextWithoutFormattingCodes(line);
            if (plain.equals(header) || plain.startsWith(header + " ")) return;
        }
        String[] types = bauble instanceof IBaubleExpanded expanded ? expanded.getBaubleTypes(stack)
                : new String[] {BaubleExpandedSlots.getTypeFromBaubleType(bauble.getBaubleType(stack))};
        BaubleItemHelper.addSlotInformation(event.toolTip, types);
    }
}
