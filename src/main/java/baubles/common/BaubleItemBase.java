package baubles.common;

import baubles.api.expanded.BaubleItemHelper;
import baubles.api.expanded.IBaubleExpanded;
import baubles.common.Baubles;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.List;

public abstract class BaubleItemBase extends Item implements IBaubleExpanded {

    public BaubleItemBase(){
        super();
        setCreativeTab(CreativeTabs.tabTools);
        this.setMaxStackSize(1);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer player) {
        return BaubleItemHelper.onBaubleRightClick(itemStackIn, worldIn, player);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean debug) {
        BaubleItemHelper.addSlotInformation(tooltip, getBaubleTypes(stack));
    }

    @Override
    public boolean hasEffect(ItemStack itemStack, int a) {
        return true;
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    @Override
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    @Override
    public void onWornTick(ItemStack itemStack, EntityLivingBase player) {}

    @Override
    public void onEquipped(ItemStack itemStack, EntityLivingBase player) {}

    @Override
    public void onUnequipped(ItemStack itemStack, EntityLivingBase player) {}
}
