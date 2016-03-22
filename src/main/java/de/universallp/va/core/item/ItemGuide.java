package de.universallp.va.core.item;

import de.universallp.va.VanillaAutomation;
import de.universallp.va.core.util.References;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

/**
 * Created by universallp on 21.03.2016 15:43.
 */
public class ItemGuide extends ItemVA {

    public ItemGuide() {
        super(References.ITEM_GUIDE);
        setCreativeTab(CreativeTabs.tabTools);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn, EnumHand hand) {
        playerIn.openGui(VanillaAutomation.instance, 1, worldIn, 0, 0, 0);
        return super.onItemRightClick(itemStackIn, worldIn, playerIn, hand);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced) {
        tooltip.add(TextFormatting.GRAY + I18n.format("item.va:vaguide.desc"));
        super.addInformation(stack, playerIn, tooltip, advanced);
    }
}
