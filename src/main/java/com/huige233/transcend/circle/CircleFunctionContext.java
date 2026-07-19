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

public class CircleFunctionContext {
    private final ServerLevel level;
    private final BlockPos corePos;
    private final CircleTier tier;
    private final CircleFunctionType functionType;
    private final UUID owner;

    private int storedMana;
    private final int maxMana;
    private final int throughputPerMinute;

    private final List<ItemStack> catalystStacks;

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

    public double getBaseRadius() {
        return tier.getBaseRadius();
    }

    public ChunkPos getChunkPos() {
        return new ChunkPos(corePos);
    }

    public boolean consumeMana(int amount) {
        if (amount <= 0) return true;
        if (storedMana >= amount) {
            storedMana -= amount;
            return true;
        }
        return false;
    }

    public int insertMana(int amount) {
        if (amount <= 0) return 0;
        int space = maxMana - storedMana;
        int inserted = Math.min(amount, space);
        storedMana += inserted;
        return inserted;
    }

    public <T extends Entity> List<T> getMobsInRadius(Class<T> clazz, double radius) {
        AABB aabb = new AABB(
                corePos.getX() - radius, corePos.getY() - radius, corePos.getZ() - radius,
                corePos.getX() + radius + 1, corePos.getY() + radius + 1, corePos.getZ() + radius + 1
        );
        return level.getEntitiesOfClass(clazz, aabb);
    }
}
