package com.huige233.transcend.spell;

import com.huige233.transcend.TranscendAttributes;
import com.huige233.transcend.items.MagicCrystalItem;
import com.huige233.transcend.items.ManaStorageItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.IntSupplier;

public final class MagicCrystalHelper {
    public static final String INNATE_MANA_TAG = "transcend_innate_mana";

    private MagicCrystalHelper() {}

    public static int getInnateMana(Player player) {
        return player.getPersistentData().getInt(INNATE_MANA_TAG);
    }

    public static int getInnateMaxMana(Player player) {
        return (int) player.getAttributeValue(TranscendAttributes.MAX_MANA.get());
    }

    public static void setInnateMana(Player player, int value) {
        int max = getInnateMaxMana(player);
        player.getPersistentData().putInt(INNATE_MANA_TAG, Math.max(0, Math.min(value, max)));
    }

    public static int countMana(Player player) {
        return (int) Math.min(Integer.MAX_VALUE, countManaLong(player));
    }

    public static long countManaLong(Player player) {
        Inventory inventory = player.getInventory();
        long total = Math.max(0, getInnateMana(player));
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot.getItem() instanceof ManaStorageItem) {
                total = saturatingManaAdd(total, ManaStorageItem.getStoredMana(slot));
            } else if (slot.getItem() instanceof MagicCrystalItem crystal) {
                long crystalMana = Math.max(0L, (long) crystal.getCrystalValue())
                        * Math.max(0L, (long) slot.getCount());
                total = saturatingManaAdd(total, crystalMana);
            }
        }
        return total;
    }

    public static boolean hasEnoughMana(Player player, int amount) {
        return amount <= 0 || countManaLong(player) >= amount;
    }

    public static boolean consumeMana(Player player, int amount) {
        return consumeMana(player, amount, () -> getInnateMaxMana(player));
    }

    public static boolean consumeMana(Player player, int amount, IntSupplier innateMaxMana) {
        if (amount <= 0 || player.isCreative()) return true;
        if (!hasEnoughMana(player, amount)) return false;
        int remaining = amount;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack slot = inventory.getItem(i);
            if (!(slot.getItem() instanceof ManaStorageItem)) continue;
            int stored = Math.max(0, ManaStorageItem.getStoredMana(slot));
            int drain = Math.min(stored, remaining);
            ManaStorageItem.setStoredMana(slot, stored - drain);
            remaining -= drain;
        }
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack slot = inventory.getItem(i);
            if (!(slot.getItem() instanceof MagicCrystalItem crystal)) continue;
            int value = crystal.getCrystalValue();
            if (value <= 0) continue;
            int itemsUsed = Math.min(slot.getCount(), ceilDiv(remaining, value));
            long consumed = (long) itemsUsed * value;
            slot.shrink(itemsUsed);
            remaining = consumed >= remaining ? 0 : remaining - (int) consumed;
        }
        if (remaining > 0) {
            int innate = getInnateMana(player);
            int drain = Math.min(innate, remaining);
            int max = Math.max(0, innateMaxMana.getAsInt());
            player.getPersistentData().putInt(INNATE_MANA_TAG,
                    Math.max(0, Math.min(innate - drain, max)));
            remaining -= drain;
        }
        return remaining <= 0;
    }

    public static int ceilDiv(int a, int b) {
        return a / b + (a % b == 0 ? 0 : 1);
    }

    public static long saturatingManaAdd(long total, long candidate) {
        if (candidate <= 0) return total;
        return total >= Long.MAX_VALUE - candidate ? Long.MAX_VALUE : total + candidate;
    }
}
