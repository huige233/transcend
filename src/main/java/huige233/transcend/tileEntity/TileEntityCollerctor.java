package huige233.transcend.tileEntity;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class TileEntityCollerctor extends TileEntity implements ITickable {
    public ItemStackHandler handler = new ItemStackHandler(1);
    private String customName;
    private ItemStack bedrock;
    private int spawnTime=0;
    private int Time=120;

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing){
        if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        else return false;
    }
    @Override
    public <T> T getCapability(Capability<T> capability,EnumFacing facing) {
        if(capability==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)return (T)this.handler;
        return super.getCapability(capability, facing);
    }
    public boolean hasCustomName(){
        return this.customName!=null&&!this.customName.isEmpty();
    }
    public void setCustomName(String customName) {
        this.customName = customName;
    }
    @Override
    public ITextComponent getDisplayName(){
        return this.hasCustomName()?new TextComponentString(this.customName):new TextComponentTranslation("container.collerctor");
    }
    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.handler.deserializeNBT(compound.getCompoundTag("Inventory"));
        this.spawnTime = compound.getInteger("SpawnTime");
        this.Time = compound.getInteger("Time");

        if(compound.hasKey("CustomName", 8)) this.setCustomName(compound.getString("CustomName"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setInteger("SpawnTime", (short)this.spawnTime);
        compound.setInteger("Time", this.Time);
        compound.setTag("Inventory", this.handler.serializeNBT());

        if(this.hasCustomName()) compound.setString("CustomName", this.customName);
        return compound;
    }

    public void setField(int id,int value){
        switch (id){
            case 0:
                this.spawnTime=value;
                break;
            case 1:
                this.Time=value;
                break;
        }
    }
    public int getField(int id){
        switch (id){
            case 0:
                return this.spawnTime;
            case 1:
                return this.Time;
            default:
                return 0;
        }
    }

    @Override
    public void update() {
        ItemStack itemstack = this.handler.getStackInSlot(0);
        if (!this.world.isRemote) {
            if (itemstack.getCount()<64) {
                ++this.spawnTime;
                if (this.spawnTime == this.Time) {
                    this.spawnTime = 0;
                    if(itemstack.getCount()<64) {
                        itemstack.grow(1);
                        this.markDirty();
                    }
                }
            }
        }
    }
}
