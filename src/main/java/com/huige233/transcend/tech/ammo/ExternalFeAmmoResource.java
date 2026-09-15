package com.huige233.transcend.tech.ammo;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

   
                                                                                                
                                                                                               
   
/** 从玩家背包或副手选择单个可足额供能的电池，为能量弹装填扣除外部 FE。 */
public final class ExternalFeAmmoResource implements AmmoResource {
    public static final ExternalFeAmmoResource INSTANCE = new ExternalFeAmmoResource();

    private ExternalFeAmmoResource() {
    }

    @Override public String id() { return BuiltInAmmoTypes.ENERGY_ID; }
    @Override public Component displayName() { return Component.translatable("ammo.transcend.energy"); }

    @Override
    public long available(Player holder, ItemStack gun) {
        IEnergyStorage storage = findStorage(holder, gun, 0);
        return storage == null ? 0L : storage.getEnergyStored();
    }

    @Override
    public long consume(Player holder, ItemStack gun, long cost) {
        if (cost < 0 || cost > Integer.MAX_VALUE) return 0L;
        IEnergyStorage storage = findStorage(holder, gun, (int) cost);
        if (storage == null) return 0L;
        int extracted = storage.extractEnergy((int) cost, false);
        return extracted == cost ? cost : 0L;
    }

    @Override
    public boolean isAvailable(Player holder) {
        return holder != null;
    }

    private static IEnergyStorage findStorage(Player player, ItemStack gun, int required) {
        if (player == null) return null;
        for (ItemStack candidate : player.getInventory().items) {
            IEnergyStorage storage = energy(candidate, gun);
            if (storage != null && storage.extractEnergy(required, true) >= required) return storage;
        }
        for (ItemStack candidate : player.getInventory().offhand) {
            IEnergyStorage storage = energy(candidate, gun);
            if (storage != null && storage.extractEnergy(required, true) >= required) return storage;
        }
        return null;
    }

    private static IEnergyStorage energy(ItemStack stack, ItemStack gun) {
        if (stack.isEmpty() || stack == gun) return null;
        return stack.getCapability(ForgeCapabilities.ENERGY).resolve().orElse(null);
    }
}
