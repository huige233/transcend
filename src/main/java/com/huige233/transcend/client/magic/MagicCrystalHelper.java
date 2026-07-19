package com.huige233.transcend.client.magic;

import net.minecraft.world.entity.player.Player;

import java.util.function.IntSupplier;

@Deprecated(forRemoval = false)
public final class MagicCrystalHelper {
    public static final String INNATE_MANA_TAG =
            com.huige233.transcend.spell.MagicCrystalHelper.INNATE_MANA_TAG;

    private MagicCrystalHelper() {}

    public static int getInnateMana(Player player) {
        return com.huige233.transcend.spell.MagicCrystalHelper.getInnateMana(player);
    }

    public static int getInnateMaxMana(Player player) {
        return com.huige233.transcend.spell.MagicCrystalHelper.getInnateMaxMana(player);
    }

    public static void setInnateMana(Player player, int value) {
        com.huige233.transcend.spell.MagicCrystalHelper.setInnateMana(player, value);
    }

    public static int countMana(Player player) {
        return com.huige233.transcend.spell.MagicCrystalHelper.countMana(player);
    }

    public static long countManaLong(Player player) {
        return com.huige233.transcend.spell.MagicCrystalHelper.countManaLong(player);
    }

    public static boolean hasEnoughMana(Player player, int amount) {
        return com.huige233.transcend.spell.MagicCrystalHelper.hasEnoughMana(player, amount);
    }

    public static boolean consumeMana(Player player, int amount) {
        return com.huige233.transcend.spell.MagicCrystalHelper.consumeMana(player, amount);
    }

    public static boolean consumeMana(Player player, int amount, IntSupplier innateMaxMana) {
        return com.huige233.transcend.spell.MagicCrystalHelper.consumeMana(player, amount, innateMaxMana);
    }

    static int ceilDiv(int a, int b) {
        return com.huige233.transcend.spell.MagicCrystalHelper.ceilDiv(a, b);
    }

    static long saturatingManaAdd(long total, long candidate) {
        return com.huige233.transcend.spell.MagicCrystalHelper.saturatingManaAdd(total, candidate);
    }
}
