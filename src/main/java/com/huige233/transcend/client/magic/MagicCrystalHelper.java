package com.huige233.transcend.client.magic;

import com.huige233.transcend.TranscendAttributes;
import com.huige233.transcend.items.MagicCrystalItem;
import com.huige233.transcend.items.ManaStorageItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MagicCrystalHelper {

    /** NBT key for the player's innate mana pool (server-side only). */
    public static final String INNATE_MANA_TAG = "transcend_innate_mana";

    /** Get the player's innate mana stored in persistent NBT. */
    public static int getInnateMana(Player player) {
        return player.getPersistentData().getInt(INNATE_MANA_TAG);
    }

    /** Get the player's innate mana cap from the MAX_MANA attribute. */
    public static int getInnateMaxMana(Player player) {
        return (int) player.getAttributeValue(TranscendAttributes.MAX_MANA.get());
    }

    /** Set innate mana, clamped to [0, max]. */
    public static void setInnateMana(Player player, int value) {
        int max = getInnateMaxMana(player);
        player.getPersistentData().putInt(INNATE_MANA_TAG, Math.max(0, Math.min(value, max)));
    }

    public static int countMana(Player player) {
        Inventory inv = player.getInventory();
        int total = getInnateMana(player); // include innate pool first
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack slot = inv.getItem(i);
            if (slot.getItem() instanceof ManaStorageItem) {
                total += ManaStorageItem.getStoredMana(slot);
            } else if (slot.getItem() instanceof MagicCrystalItem crystal) {
                total += crystal.getCrystalValue() * slot.getCount();
            }
        }
        return total;
    }

    public static boolean hasEnoughMana(Player player, int amount) {
        return countMana(player) >= amount;
    }

    /**
     * Consume mana.
     *
     * <p>v6 drain order: ManaStorageItem → MagicCrystalItem → innate pool.
     * 优先消耗身上的魔力容器（有限资源），把会自然/环境回复的内禀池留到最后。
     */
    public static boolean consumeMana(Player player, int amount) {
        if (player.isCreative()) return true;
        if (!hasEnoughMana(player, amount)) return false;

        int remaining = amount;
        Inventory inv = player.getInventory();

        // 1. ManaStorageItems 优先
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack slot = inv.getItem(i);
            if (!(slot.getItem() instanceof ManaStorageItem)) continue;

            int stored = ManaStorageItem.getStoredMana(slot);
            int drain = Math.min(stored, remaining);
            ManaStorageItem.setStoredMana(slot, stored - drain);
            remaining -= drain;
        }
        if (remaining <= 0) return true;

        // 2. MagicCrystalItem stacks
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack slot = inv.getItem(i);
            if (!(slot.getItem() instanceof MagicCrystalItem crystal)) continue;

            int value = crystal.getCrystalValue();
            int itemsNeeded = ceilDiv(remaining, value);
            int itemsUsed = Math.min(slot.getCount(), itemsNeeded);
            int consumed = itemsUsed * value;

            slot.shrink(itemsUsed);
            remaining -= consumed;
        }
        if (remaining <= 0) return true;

        // 3. Innate pool 最后兜底
        int innate = getInnateMana(player);
        if (innate > 0) {
            int drainInnate = Math.min(innate, remaining);
            setInnateMana(player, innate - drainInnate);
            remaining -= drainInnate;
        }

        return remaining <= 0;
    }

    private static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }
}
