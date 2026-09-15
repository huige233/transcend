package com.huige233.transcend.tech.ammo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

   
                                                 
  
                                                
                                     
   
/** 维护单一弹种的弹匣余量，在资源足额支付后装填并持久化逐发消耗状态。 */
public final class Magazine {

    public static final String NBT_CHARGES = "MagazineCharges";
    public static final String NBT_AMMO_ID = "MagazineAmmo";

    
    @Nullable
    private String loadedAmmoId;
    
    private int charges;

    public Magazine() {
        this.loadedAmmoId = null;
        this.charges = 0;
    }

    

    @Nullable
    public String loadedAmmoId() {
        return loadedAmmoId;
    }

    public int charges() {
        return charges;
    }

    public boolean isEmpty() {
        return charges <= 0 || loadedAmmoId == null;
    }

    
    public boolean isLoadedWith(AmmoType ammo) {
        return ammo != null && ammo.id().equals(loadedAmmoId) && charges > 0;
    }

    

       
                                         
                                     
      
                                              
                                                         
                               
                                                         
                                     
                          
       
    public boolean tryReload(Player player, ItemStack gun, AmmoType ammo,
                             int capacity, long resourceCost) {
        if (ammo == null || capacity <= 0 || resourceCost < 0) return false;
        AmmoResource res = ammo.resource();
        if (!res.isAvailable(player)) return false;
        if (res.available(player, gun) < resourceCost) return false;
        if (res.consume(player, gun, resourceCost) != resourceCost) return false;
        this.loadedAmmoId = ammo.id();
        this.charges = capacity;
        return true;
    }

    
    public boolean tryConsumeRound() {
        if (isEmpty()) return false;
        charges--;
        if (charges <= 0) {
            charges = 0;
        }
        return true;
    }

    
    public void clear() {
        this.loadedAmmoId = null;
        this.charges = 0;
    }

    

    public void save(CompoundTag tag) {
        tag.putInt(NBT_CHARGES, charges);
        if (loadedAmmoId != null && charges > 0) {
            tag.putString(NBT_AMMO_ID, loadedAmmoId);
        } else {
            tag.remove(NBT_AMMO_ID);
        }
    }

    public void load(CompoundTag tag) {
        this.charges = Math.max(0, tag.getInt(NBT_CHARGES));
        this.loadedAmmoId = tag.contains(NBT_AMMO_ID) ? tag.getString(NBT_AMMO_ID) : null;
        if (this.charges == 0 || this.loadedAmmoId == null || this.loadedAmmoId.isBlank()) {
            clear();
        }
    }
}
