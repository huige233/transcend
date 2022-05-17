package huige233.transcend.tileEntity;

import huige233.transcend.init.ModItems;
import huige233.transcend.util.BlockUtils;
import huige233.transcend.util.ItemUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;

public class TileEntityCollerctor extends TileEntity implements IInventory {
    public static final int PRODUCTION_TICKS = 7111;
    private ItemStack neutrons;
    private int progress;
    protected boolean isActive;
    protected EnumFacing facing;

    public TileEntityCollerctor() {
        this.neutrons = ItemStack.EMPTY;
    }

    public void doWork() {
        if (++this.progress >= 7111) {
            if (this.neutrons.isEmpty()) {
                this.neutrons = ItemUtils.copyStack(new ItemStack(ModItems.BEDROCK_CHEN), 1);
            } else if (ItemUtils.areStacksSameType(this.neutrons, new ItemStack(ModItems.BEDROCK_CHEN)) && this.neutrons.getCount() < 64) {
                this.neutrons.grow(1);
            }

            this.progress = 0;
            this.markDirty();
        }

    }

    protected void onWorkStopped() {
        this.progress = 0;
    }

    protected boolean canWork() {
        return this.neutrons.isEmpty() || this.neutrons.getCount() < 64;
    }

    public int getProgress() {
        return this.progress;
    }

    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("Neutrons")) {
            this.neutrons = new ItemStack(tag.getCompoundTag("Neutrons"));
        }

        this.progress = tag.getInteger("Progress");
    }

    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setInteger("Progress", this.progress);
        if (this.neutrons != null) {
            NBTTagCompound produce = new NBTTagCompound();
            this.neutrons.writeToNBT(produce);
            tag.setTag("Neutrons", produce);
        } else {
            tag.removeTag("Neutrons");
        }

        return super.writeToNBT(tag);
    }

    public boolean hasCapability(Capability<?> capability, @javax.annotation.Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @javax.annotation.Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing side) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new InvWrapper(this)) : super.getCapability(capability, side);
    }

    public int getSizeInventory() {
        return 1;
    }

    public boolean isEmpty() {
        return this.neutrons.isEmpty();
    }

    public ItemStack getStackInSlot(int slot) {
        return this.neutrons;
    }

    public ItemStack decrStackSize(int slot, int decrement) {
        if (this.neutrons.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            ItemStack take;
            if (decrement < this.neutrons.getCount()) {
                take = this.neutrons.splitStack(decrement);
                if (this.neutrons.getCount() <= 0) {
                    this.neutrons = ItemStack.EMPTY;
                }

                return take;
            } else {
                take = this.neutrons;
                this.neutrons = ItemStack.EMPTY;
                return take;
            }
        }
    }

    public void openInventory(EntityPlayer player) {
    }

    public void closeInventory(EntityPlayer player) {
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.world.getTileEntity(this.getPos()) == this && BlockUtils.isEntityInRange(this.getPos(), player, 64);
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    public int getInventoryStackLimit() {
        return 64;
    }

    public void setInventorySlotContents(int slot, ItemStack stack) {
        this.neutrons = stack;
    }

    public String getName() {
        return "container.neutron";
    }

    public boolean hasCustomName() {
        return false;
    }

    public ItemStack removeStackFromSlot(int slot) {
        return ItemStack.EMPTY;
    }

    public int getField(int id) {
        return 0;
    }

    public void setField(int id, int value) {
    }

    public int getFieldCount() {
        return 2;
    }

    public void clear() {
    }

    public EnumFacing getFacing() {
        return this.facing;
    }

    public void setFacing(EnumFacing facing) {
        this.facing = facing;
    }

    public boolean isActive() {
        return this.isActive;
    }
}
