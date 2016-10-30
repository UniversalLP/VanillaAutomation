package de.universallp.va.core.tile;

import de.universallp.va.core.block.VABlocks;
import de.universallp.va.core.container.handler.FilteredItemHandler;
import de.universallp.va.core.util.ICustomField;
import de.universallp.va.core.util.Utils;
import de.universallp.va.core.util.libs.LibLocalization;
import de.universallp.va.core.util.libs.LibReflection;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by universallp on 31.03.2016 15:12 16:31.
 * This file is part of VanillaAutomation which is licenced
 * under the MOZILLA PUBLIC LICENCE 2.0 - mozilla.org/en-US/MPL/2.0/
 * github.com/UniversalLP/VanillaAutomation
 */
public class TileFilteredHopper extends TileEntityHopper implements ICustomField {

    private EnumFilter filterMode = EnumFilter.WHITELIST;

    private boolean matchMeta = false;
    private boolean matchNBT = false;
    private boolean matchMod = false;

    public TileFilteredHopper() {
        setCustomName(LibLocalization.GUI_FILTEREDHOPPER);
        ReflectionHelper.setPrivateValue(TileEntityHopper.class, this, new ItemStack[10], LibReflection.HOPPER_INVENTORY);
    }

    private static boolean captureDrops(TileFilteredHopper hopper) {
        IInventory iinventory = getHopperInventory(hopper);

        EnumFacing enumfacing = EnumFacing.DOWN;

        if (iinventory != null) {
            if (isInventoryEmpty(iinventory, enumfacing)) {
                return false;
            }

            if (iinventory instanceof ISidedInventory) {
                ISidedInventory isidedinventory = (ISidedInventory) iinventory;
                int[] aint = isidedinventory.getSlotsForFace(enumfacing);

                for (int i : aint) {
                    if (pullItemFromSlot(hopper, iinventory, i, enumfacing)) {
                        return true;
                    }
                }
            } else {
                int j = iinventory.getSizeInventory();

                for (int k = 0; k < j; ++k) {
                    if (pullItemFromSlot(hopper, iinventory, k, enumfacing)) {
                        return true;
                    }
                }
            }
        } else {
            for (EntityItem entityitem : getCaptureItems(hopper.getWorld(), hopper.getXPos(), hopper.getYPos(), hopper.getZPos())) {
                if (hopper.isItemValid(entityitem.getEntityItem()))
                    if (putDropInInventoryAllSlots(hopper, entityitem)) {
                        return true;
                    }
            }
        }

        return false;
    }

    private static boolean pullItemFromSlot(TileFilteredHopper hopper, IInventory inventoryIn, int index, EnumFacing direction) {
        ItemStack itemstack = inventoryIn.getStackInSlot(index);

        if (itemstack != null && canExtractItemFromSlot(inventoryIn, itemstack, index, direction) && hopper.isItemValid(itemstack)) {
            ItemStack itemstack1 = itemstack.copy();
            ItemStack itemstack2 = putStackInInventoryAllSlots(hopper, inventoryIn.decrStackSize(index, 1), null);

            if (itemstack2 == null || itemstack2.stackSize == 0) {
                inventoryIn.markDirty();
                return true;
            }

            inventoryIn.setInventorySlotContents(index, itemstack1);
        }

        return false;
    }

    private static boolean canExtractItemFromSlot(IInventory inventoryIn, ItemStack stack, int index, EnumFacing side) {
        return !(inventoryIn instanceof ISidedInventory) || ((ISidedInventory) inventoryIn).canExtractItem(index, stack, side);
    }

    private static boolean isInventoryEmpty(IInventory inventoryIn, EnumFacing side) {
        if (inventoryIn instanceof ISidedInventory) {
            ISidedInventory isidedinventory = (ISidedInventory) inventoryIn;
            int[] aint = isidedinventory.getSlotsForFace(side);

            for (int i : aint) {
                if (isidedinventory.getStackInSlot(i) != null) {
                    return false;
                }
            }
        } else {
            int j = inventoryIn.getSizeInventory();

            for (int k = 0; k < j; ++k) {
                if (inventoryIn.getStackInSlot(k) != null) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setByte("filtermode", (byte) filterMode.ordinal());
        compound.setBoolean("matchMeta", matchMeta);
        compound.setBoolean("matchNBT", matchNBT);
        compound.setBoolean("matchMod", matchMod);
        return super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        if (compound.hasKey("filtermode"))
            filterMode = EnumFilter.values()[compound.getByte("filtermode")];

        if (compound.hasKey("matchMeta"))
            matchMeta = compound.getBoolean("matchMeta");
        if (compound.hasKey("matchNBT"))
            matchNBT = compound.getBoolean("matchNBT");
        if (compound.hasKey("matchMod"))
            matchMod = compound.getBoolean("matchMod");
    }

    @Override
    protected IItemHandler createUnSidedHandler() {
        return new FilteredItemHandler(this);
    }

    @Override
    public boolean updateHopper() {
        if (this.worldObj != null && !this.worldObj.isRemote) {
            if (!this.isOnTransferCooldown() && BlockHopper.isEnabled(this.getBlockMetadata())) {
                boolean flag = false;

                if (!this.isEmpty()) {
                    flag = this.transferItemsOut();
                }

                if (!this.isFull()) {
                    flag = captureDrops(this) || flag;
                }

                if (flag) {
                    this.setTransferCooldown(8);
                    this.markDirty();
                    return true;
                }
            }

            return false;
        } else
            return false;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index < 5)
            return !getHasItemFilter() || isItemValid(stack);

        return false;
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        return !newSate.getBlock().equals(VABlocks.filterHopper);
    }

    /**
     * Returns true if an item is allowed by the current filter settings
     */
    public boolean isItemValid(ItemStack s) {
        if (s == null)
            return false;


        if (!getHasItemFilter())
            return true;
        switch (getFilterMode()) {
            case BLACKLIST:
                boolean flag = true;
                for (ItemStack stack : getItemFilter()) {
                    if (!matchMod) {
                        if (stack.getItem().equals(s.getItem())) {
                            if (matchMeta && matchNBT && ItemStack.areItemStackTagsEqual(stack, s) && s.getItemDamage() == stack.getItemDamage())
                                flag = false;
                            else if (matchMeta && !matchNBT && s.getItemDamage() == stack.getItemDamage())
                                flag = false;
                            else if (!matchMeta && matchNBT && ItemStack.areItemStackTagsEqual(stack, s))
                                flag = false;
                            else if (!matchMeta && !matchNBT)
                                flag = false;
                        }
                    } else {
                        if (Utils.getModName(s).equals(Utils.getModName(stack))) {
                            if (matchMeta && matchNBT && ItemStack.areItemStackTagsEqual(stack, s) && s.getItemDamage() == stack.getItemDamage())
                                flag = false;
                            else if (matchMeta && !matchNBT && s.getItemDamage() == stack.getItemDamage())
                                flag = false;
                            else if (!matchMeta && matchNBT && ItemStack.areItemStackTagsEqual(stack, s))
                                flag = false;
                            else if (!matchMeta && !matchNBT)
                                flag = false;
                        }
                    }
                }
                return flag;
            case WHITELIST:
                flag = false;
                for (ItemStack stack : getItemFilter()) {
                    if (!matchMod) {
                        if (stack.getItem().equals(s.getItem())) {
                            if (matchMeta && matchNBT && ItemStack.areItemStackTagsEqual(stack, s) && s.getItemDamage() == stack.getItemDamage())
                                flag = true;
                            else if (matchMeta && !matchNBT && s.getItemDamage() == stack.getItemDamage())
                                flag = true;
                            else if (!matchMeta && matchNBT && ItemStack.areItemStackTagsEqual(stack, s))
                                flag = true;
                            else if (!matchMeta && !matchNBT)
                                flag = true;
                        }
                    } else {
                        if (Utils.getModName(s).equals(Utils.getModName(stack))) {
                            if (matchMeta && matchNBT && ItemStack.areItemStackTagsEqual(stack, s) && s.getItemDamage() == stack.getItemDamage())
                                flag = true;
                            else if (matchMeta && !matchNBT && s.getItemDamage() == stack.getItemDamage())
                                flag = true;
                            else if (!matchMeta && matchNBT && ItemStack.areItemStackTagsEqual(stack, s))
                                flag = true;
                            else if (!matchMeta && !matchNBT)
                                flag = true;
                        }
                    }
                }
                return flag;
        }
        return false;
    }

    public List<ItemStack> getItemFilter() {
        List<ItemStack> filter = new ArrayList<ItemStack>();

        for (int i = TileXPHopper.hopperInv; i < getSizeInventory(); i++) {
            if (getStackInSlot(i) != null)
                filter.add(getStackInSlot(i));
        }
        return filter;
    }

    public EnumFilter getFilterMode() {
        return filterMode;
    }

    public void setFilterMode(EnumFilter filterMode) {
        this.filterMode = filterMode;
    }

    // Copied Vanilla code

    public boolean getHasItemFilter() {
        return getItemFilter() != null && getItemFilter().size() > 0;
    }

    @Override
    public void setField(int id, int value) {
        if (id == 0)
            filterMode = EnumFilter.values()[value];
        else if (id == 1)
            matchMeta = value != 0;
        else if (id == 2)
            matchNBT = value != 0;
        else if (id == 3)
            matchMod = value != 0;
        else
            super.setField(id, value);
        markDirty();
    }

    @Override
    public int getField(int id) {
        if (id == 0)
            return filterMode.ordinal();
        else if (id == 1)
            return matchMeta ? 1 : 0;
        else if (id == 2)
            return matchNBT ? 1 : 0;
        else if (id == 3)
            return matchMod ? 1 : 0;
        else
            return super.getField(id);
    }

    @Override
    public void setStringField(int id, String val) {
        if (id == 0)
            setCustomName(val);
    }

    @Override
    public String getStringField(int id) {
        if (id == 0)
            return getName();
        return null;
    }

    private boolean isEmpty() {
        for (int i = 0; i < TileXPHopper.hopperInv; i++) {
            ItemStack itemstack = getStackInSlot(i);
            if (itemstack != null)
                return false;
        }
        return true;
    }

    private boolean isFull() {
        for (int i = 0; i < TileXPHopper.hopperInv; i++) {
            ItemStack itemstack = getStackInSlot(i);
            if (itemstack == null || itemstack.stackSize != itemstack.getMaxStackSize())
                return false;
        }

        return true;
    }

    private boolean transferItemsOut() {
        if (net.minecraftforge.items.VanillaInventoryCodeHooks.insertHook(this))
            return true;
        IInventory iinventory = this.getInventoryForHopperTransfer();

        if (iinventory == null) {
            return false;
        } else {
            EnumFacing enumfacing = BlockHopper.getFacing(this.getBlockMetadata()).getOpposite();

            if (this.isInventoryFull(iinventory, enumfacing)) {
                return false;
            } else {
                for (int i = 0; i < TileXPHopper.hopperInv; ++i) {
                    if (this.getStackInSlot(i) != null) {
                        ItemStack itemstack = this.getStackInSlot(i).copy();
                        ItemStack itemstack1 = putStackInInventoryAllSlots(iinventory, this.decrStackSize(i, 1), enumfacing);

                        if (itemstack1 == null || itemstack1.stackSize == 0) {
                            iinventory.markDirty();
                            return true;
                        }

                        this.setInventorySlotContents(i, itemstack);
                    }
                }

                return false;
            }
        }
    }

    private boolean isInventoryFull(IInventory inventoryIn, EnumFacing side) {
        if (inventoryIn instanceof ISidedInventory) {
            ISidedInventory isidedinventory = (ISidedInventory) inventoryIn;
            int[] aint = isidedinventory.getSlotsForFace(side);

            for (int k = 0; k < aint.length; ++k) {
                ItemStack itemstack1 = isidedinventory.getStackInSlot(aint[k]);

                if (itemstack1 == null || itemstack1.stackSize != itemstack1.getMaxStackSize())
                    return false;
            }
        } else {
            int i = inventoryIn.getSizeInventory();

            for (int j = 0; j < i; ++j) {
                ItemStack itemstack = inventoryIn.getStackInSlot(j);

                if (itemstack == null || itemstack.stackSize != itemstack.getMaxStackSize()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
            return true;
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
            return (T) new FilteredItemHandler(this);
        return super.getCapability(capability, facing);
    }

    private IInventory getInventoryForHopperTransfer() {
        EnumFacing enumfacing = BlockHopper.getFacing(this.getBlockMetadata());
        return getInventoryAtPosition(this.getWorld(), this.getXPos() + (double) enumfacing.getFrontOffsetX(), this.getYPos() + (double) enumfacing.getFrontOffsetY(), this.getZPos() + (double) enumfacing.getFrontOffsetZ());
    }


    public enum EnumFilter {
        WHITELIST,
        BLACKLIST
    }

}
