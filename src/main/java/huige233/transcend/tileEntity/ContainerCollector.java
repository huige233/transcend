package huige233.transcend.tileEntity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;


public class ContainerCollector extends Container {
    private final TileEntityCollerctor tileentity;
    private int cookTime, totalCookTime;

    public ContainerCollector(InventoryPlayer player, TileEntityCollerctor tileentity)
    {
        this.tileentity = tileentity;
        IItemHandler handler = tileentity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        this.addSlotToContainer(new SlotItemHandler(handler, 0, 80, 35));

        for(int y = 0; y < 3; y++)
        {
            for(int x = 0; x < 9; x++)
            {
                this.addSlotToContainer(new Slot(player, x + y*9 + 9, 8 + x*18, 84 + y*18));
            }
        }

        for(int x = 0; x < 9; x++)
        {
            this.addSlotToContainer(new Slot(player, x, 8 + x * 18, 142));
        }
    }

    @Override
    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();

        for(int i = 0; i < this.listeners.size(); ++i)
        {
            IContainerListener listener = (IContainerListener)this.listeners.get(i);
            if(this.cookTime != this.tileentity.getField(0)) listener.sendWindowProperty(this, 0, this.tileentity.getField(0));
            if(this.totalCookTime != this.tileentity.getField(1)) listener.sendWindowProperty(this, 1, this.tileentity.getField(1));
        }

        this.cookTime = this.tileentity.getField(0);
        this.totalCookTime = this.tileentity.getField(1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data)
    {
        this.tileentity.setField(id, data);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn)
    {
        return this.tileentity.isUsableByPlayer(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotNumber)
    {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = (Slot)this.inventorySlots.get(slotNumber);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (slotNumber == 0) {
                if (!this.mergeItemStack(itemstack1, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onSlotChange(itemstack1, itemstack);
            } else if (slotNumber >= 1 && slotNumber < 28) {
                if (!this.mergeItemStack(itemstack1, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotNumber >= 28 && slotNumber < 37 && !this.mergeItemStack(itemstack1, 1, 28, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }
}
