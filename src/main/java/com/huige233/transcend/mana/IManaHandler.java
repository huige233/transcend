package com.huige233.transcend.mana;

/**
 * 魔力处理器接口 — 类似 Forge IEnergyStorage 但专用于法环魔力系统。
 * 不使用 Forge Energy 以保持独立的魔法风格平衡。
 */
public interface IManaHandler {
    /** 当前存储的魔力（CM 单位） */
    int getManaStored();

    /** 最大容量 */
    int getMaxManaStored();

    /** 接收魔力，simulate=true 时仅模拟不实际修改。返回实际/模拟接收量。 */
    int receiveMana(int amount, boolean simulate);

    /** 提取魔力，simulate=true 时仅模拟。返回实际/模拟提取量。 */
    int extractMana(int amount, boolean simulate);

    /** 是否可以接收 */
    boolean canReceive();

    /** 是否可以提取 */
    boolean canExtract();
}
