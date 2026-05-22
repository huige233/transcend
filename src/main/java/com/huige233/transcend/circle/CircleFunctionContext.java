package com.huige233.transcend.circle;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 法环功能执行上下文 — 封装核心方块实体的状态供执行器使用。
 */
public class CircleFunctionContext {
    private final ServerLevel level;
    private final BlockPos corePos;
    private final CircleTier tier;
    private final CircleFunctionType functionType;
    private final UUID owner;

    // 魔力访问
    private int storedMana;
    private final int maxMana;
    private final int throughputPerMinute;

    // 催化剂
    private final List<ItemStack> catalystStacks;

    // 强化等级
    private final int powerLevel;
    private final int durationLevel;
    private final int efficiencyLevel;
    private final int specialLevel;

    public CircleFunctionContext(ServerLevel level, BlockPos corePos, CircleTier tier,
                                 CircleFunctionType functionType, UUID owner,
                                 int storedMana, int maxMana, int throughputPerMinute,
                                 List<ItemStack> catalystStacks,
                                 int powerLevel, int durationLevel,
                                 int efficiencyLevel, int specialLevel) {
        this.level = level;
        this.corePos = corePos;
        this.tier = tier;
        this.functionType = functionType;
        this.owner = owner;
        this.storedMana = storedMana;
        this.maxMana = maxMana;
        this.throughputPerMinute = throughputPerMinute;
        this.catalystStacks = catalystStacks != null ? catalystStacks : Collections.emptyList();
        this.powerLevel = powerLevel;
        this.durationLevel = durationLevel;
        this.efficiencyLevel = efficiencyLevel;
        this.specialLevel = specialLevel;
    }

    // ===== Getters =====

    public ServerLevel getLevel() {
        return level;
    }

    public BlockPos getCorePos() {
        return corePos;
    }

    public CircleTier getTier() {
        return tier;
    }

    public CircleFunctionType getFunctionType() {
        return functionType;
    }

    public UUID getOwner() {
        return owner;
    }

    public int getStoredMana() {
        return storedMana;
    }

    public int getMaxMana() {
        return maxMana;
    }

    public int getThroughputPerMinute() {
        return throughputPerMinute;
    }

    public List<ItemStack> getCatalystStacks() {
        return catalystStacks;
    }

    public int getPowerLevel() {
        return powerLevel;
    }

    public int getDurationLevel() {
        return durationLevel;
    }

    public int getEfficiencyLevel() {
        return efficiencyLevel;
    }

    public int getSpecialLevel() {
        return specialLevel;
    }

    /** 基础半径（来自层级） */
    public double getBaseRadius() {
        return tier.getBaseRadius();
    }

    /** 核心方块所在的区块坐标 */
    public ChunkPos getChunkPos() {
        return new ChunkPos(corePos);
    }

    /**
     * 消耗魔力。如果 storedMana >= amount，扣除并返回 true；
     * 否则不扣除并返回 false。
     */
    public boolean consumeMana(int amount) {
        if (amount <= 0) return true;
        if (storedMana >= amount) {
            storedMana -= amount;
            return true;
        }
        return false;
    }

    /**
     * 向缓冲区注入魔力，不超过最大容量。
     * @param amount 注入量
     * @return 实际注入的量
     */
    public int insertMana(int amount) {
        if (amount <= 0) return 0;
        int space = maxMana - storedMana;
        int inserted = Math.min(amount, space);
        storedMana += inserted;
        return inserted;
    }

    /**
     * 在以核心为中心的指定半径内查找指定类型的实体。
     */
    public <T extends Entity> List<T> getMobsInRadius(Class<T> clazz, double radius) {
        AABB aabb = new AABB(
                corePos.getX() - radius, corePos.getY() - radius, corePos.getZ() - radius,
                corePos.getX() + radius + 1, corePos.getY() + radius + 1, corePos.getZ() + radius + 1
        );
        return level.getEntitiesOfClass(clazz, aabb);
    }
}
