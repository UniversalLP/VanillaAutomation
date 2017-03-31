package de.universallp.va.core.tile;

import de.universallp.va.core.block.VABlocks;
import de.universallp.va.core.container.handler.XPHopperItemHandler;
import de.universallp.va.core.util.ICustomField;
import de.universallp.va.core.util.Utils;
import de.universallp.va.core.util.libs.LibLocalization;
import de.universallp.va.core.util.libs.LibReflection;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

/**
 * Created by universallp on 27.03.2016 23:58 16:31.
 * This file is part of VanillaAutomation which is licenced
 * under the MOZILLA PUBLIC LICENCE 2.0 - mozilla.org/en-US/MPL/2.0/
 * github.com/UniversalLP/VanillaAutomation
 */
public class TileXPHopper extends TileEntityHopper implements ICustomField {

    public static final int xpPerBottle = 13;
    public static final int hopperInv = 5;
    private int progress;

    private int transferCooldown = 0;
    private long tickedGameTime;

    public TileXPHopper() {
        setCustomName(LibLocalization.GUI_XPHOPPER);
        ReflectionHelper.setPrivateValue(TileEntityHopper.class, this, NonNullList.<ItemStack>withSize(6, ItemStack.EMPTY), LibReflection.HOPPER_INVENTORY); // Welp, seems to work
    }

    /**
     * Get an empty slot to put filled bottles into
     * @param inv
     * @return
     */
    public static int getBottleSlot(IInventory inv) {
        for (int i = 0; i < hopperInv; i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s.isEmpty() || s.getItem().equals(Items.EXPERIENCE_BOTTLE) && s.getCount() < s.getMaxStackSize())
                return i;
        }
        return -1;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setInteger("progress", progress);
        return super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("progress"))
            progress = compound.getInteger("progress");
    }

    private boolean isOnTransferCooldown() {
        return this.transferCooldown > 0;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (!stack.isEmpty()&& stack.getItem().equals(Items.GLASS_BOTTLE)) {
            ItemStack bottles = getStackInSlot(5);
            if (getStackInSlot(index).isEmpty() && index == 5)
                return true;

            if (!bottles.isEmpty() && bottles.getCount() < bottles.getMaxStackSize() && index == 5)
                return true;

            if (!bottles.isEmpty() && bottles.getCount() >= bottles.getMaxStackSize() && index != 5)
                return false;
            return false;
        }
        return true;
    }

    @Override
    public void setField(int id, int value) {
        if (id == 0 && value <= xpPerBottle)
            progress = value;
        markDirty();
    }

    @Override
    public int getField(int id) {
        if (id == 0)
            return progress;
        return super.getField(id);
    }

    @Override
    public void update() {

        if (this.world != null && !this.world.isRemote) {
            --this.transferCooldown;
            this.tickedGameTime = this.world.getTotalWorldTime();
            if (!this.isOnTransferCooldown())
            {
                this.setTransferCooldown(0);
                this.updateHopper();
            }

        }
    }

    @Override
    public void setTransferCooldown(int transferCooldown) {
        this.transferCooldown = transferCooldown;
    }

    private void updateHopper() {
        BlockPos overHopper = getPos().up();
        List<EntityXPOrb> orbs = getWorld().getEntitiesWithinAABB(EntityXPOrb.class, new AxisAlignedBB(overHopper).expandXyz(1));

        if (!orbs.isEmpty() && BlockHopper.isEnabled(this.getBlockMetadata()))
            for (EntityXPOrb orb : orbs) {
                int slot = getBottleSlot(this);
                int resultXP = orb.xpValue + progress;
                ItemStack bottles = getStackInSlot(5);

                if (!bottles.isEmpty() && bottles.getCount() > 0)
                    if (resultXP >= xpPerBottle && slot > -1) { // If there's space for a new bottle and adding the xp of the current orb will result in a new bottle
                        ItemStack xpBottle = new ItemStack(Items.EXPERIENCE_BOTTLE, 1);
                        getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP).insertItem(slot, xpBottle, false);
                        setInventorySlotContents(5, Utils.decreaseStack(bottles, 1));
                        markDirty();
                        progress = resultXP - xpPerBottle;
                    } else {
                        if (slot == -1 && resultXP > xpPerBottle) // If theres no space for a new bottle and adding the xp of the current orb would result in a new bottle
                            break;

                        // If there's no space for a new bottle but adding the xp of the current orb won't result in  a new bottle
                        progress = resultXP;
                        getWorld().removeEntity(orb);
                    }
            }

        if (this.world != null && !this.world.isRemote) {
            if (!this.isOnTransferCooldown() && BlockHopper.isEnabled(this.getBlockMetadata())) {
                boolean flag = false;

                if (!this.isEmpty()) {
                   flag = this.transferItemsOut();
                }

                if (!this.isFull()) {
                    flag = captureDroppedItems(this) || flag;
                }

                if (flag) {
                    this.setTransferCooldown(8);
                    this.markDirty();
                }
            }

        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        return !newSate.getBlock().equals(VABlocks.xpHopper);
    }

    public int getProgress() {
        return progress;
    }

    @Override
    protected IItemHandler createUnSidedHandler() {
        return new XPHopperItemHandler(this);
    }

    // Copied vanilla code cause methods are private

    private boolean isFull() {
        for (int i = 0; i < getSizeInventory() - 1; i++) {
            ItemStack itemstack = getStackInSlot(i);
            if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize())
                return false;
        }

        return true;
    }

    private boolean transferItemsOut() {
//        if (net.minecraftforge.items.VanillaInventoryCodeHooks.insertHook(this)) {
//            return true;
//        }
        IInventory iinventory = this.getInventoryForHopperTransfer();

        if (iinventory == null) {
            return false;
        } else {
            EnumFacing enumfacing = BlockHopper.getFacing(this.getBlockMetadata()).getOpposite();

            if (this.isInventoryFull(iinventory, enumfacing)) {
                return false;
            } else {
                for (int i = 0; i < this.getSizeInventory() - 1; ++i) {
                    if (!this.getStackInSlot(i).isEmpty()) {

                        ItemStack itemstack = this.getStackInSlot(i).copy();
                        ItemStack itemstack1 = putStackInInventoryAllSlots(this, iinventory, this.decrStackSize(i, 1), enumfacing);

                        if (itemstack1.isEmpty()) {
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

            for (int k : aint) {
                ItemStack itemstack1 = isidedinventory.getStackInSlot(aint[k]);

                if (itemstack1.isEmpty() || itemstack1.getCount() != itemstack1.getMaxStackSize())
                    return false;
            }
        } else {
            int i = inventoryIn.getSizeInventory();

            for (int j = 0; j < i; ++j) {
                ItemStack itemstack = inventoryIn.getStackInSlot(j);

                if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize()) {
                    return false;
                }
            }
        }

        return true;
    }

    private IInventory getInventoryForHopperTransfer() {
        EnumFacing enumfacing = BlockHopper.getFacing(this.getBlockMetadata());
        return getInventoryAtPosition(this.getWorld(), this.getXPos() + (double) enumfacing.getFrontOffsetX(), this.getYPos() + (double) enumfacing.getFrontOffsetY(), this.getZPos() + (double) enumfacing.getFrontOffsetZ());
    }

    @Override
    public void setStringField(int id, String val) {
        switch (id) {
            case 0:
                setCustomName(val);
                break;
        }
    }

    @Override
    public String getStringField(int id) {
        switch (id) {
            case 0:
                return getName();
            default:
                return null;
        }
    }
}
