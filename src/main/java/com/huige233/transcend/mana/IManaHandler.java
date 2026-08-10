package com.huige233.transcend.mana;

/** 魔力处理器接口。 */
public interface IManaHandler {

    int getManaStored();

    int getMaxManaStored();

    int receiveMana(int amount, boolean simulate);

    int extractMana(int amount, boolean simulate);

    boolean canReceive();

    boolean canExtract();
}
