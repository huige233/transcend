package com.huige233.transcend.items;

import com.huige233.transcend.world.mana.ChunkManaSavedData;

/** 法术施放费用记录内部类。 */
final class SpellCastPayment {
    interface Access {
        long availableMana();

        boolean consumeMana(int amount);

        void setEchoPending(boolean pending);
    }

    record Result(boolean paid, int manaCost) {
    }

    private SpellCastPayment() {
    }

    static Result attempt(int canonicalCost, float chunkMultiplier,
                          boolean ascensionFree, boolean echoPending, Access access) {
        if (access == null) throw new IllegalArgumentException("payment access must not be null");
        int payable = ChunkManaSavedData.applyManaCostMultiplier(canonicalCost, chunkMultiplier);
        if (ascensionFree || echoPending) {
            if (echoPending) access.setEchoPending(false);
            return new Result(true, 0);
        }
        if (payable > 0 && access.availableMana() < payable) return new Result(false, payable);
        return new Result(access.consumeMana(payable), payable);
    }
}
