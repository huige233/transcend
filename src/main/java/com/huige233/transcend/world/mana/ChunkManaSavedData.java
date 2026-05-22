package com.huige233.transcend.world.mana;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 区块魔力浓度持久化存储。
 *
 * 每个维度独立存储，键为 ChunkPos.toLong()。
 * 首次访问的区块随机生成 100~10000 的初始魔力。
 *
 * 设计参数：
 * - 初始浓度：100~10000 随机
 * - 最大浓度：15000
 * - 自然恢复：每100tick +1.0（缓慢自然回复到初始值）
 * - 魔力均衡化：高魔力区块向低魔力区块缓慢转移
 * - 均衡化速率：每200tick，每对相邻区块转移差值的 2%
 */
public class ChunkManaSavedData extends SavedData {

    private static final String DATA_NAME = "transcend_chunk_mana";
    private static final Random RAND = new Random();

    /** 初始魔力范围 */
    public static final float MIN_INITIAL_MANA = 100.0F;
    public static final float MAX_INITIAL_MANA = 10000.0F;
    /** 用于显示/百分比计算的参考基准值 */
    public static final float DEFAULT_MANA = 5000.0F;
    /** 魔力浓度绝对上限 */
    public static final float MAX_MANA = 15000.0F;
    /** 自然恢复速率：每次回复量 */
    public static final float REGEN_AMOUNT = 1.0F;
    /** 均衡化比率：每次转移差值的百分比 */
    public static final float EQUALIZE_RATE = 0.02F;

    /** 标记值：表示区块尚未初始化（不会被保存到 NBT） */
    private static final float UNINITIALIZED = -1.0F;

    private final Map<Long, Float> manaMap = new HashMap<>();

    public ChunkManaSavedData() {}

    // ─── 访问器 ─────────────────────────────────────────────────────

    /** 获取指定区块的当前魔力浓度（首次访问时随机初始化） */
    public float getMana(ChunkPos pos) {
        long key = pos.toLong();
        Float value = manaMap.get(key);
        if (value == null) {
            // 首次访问：随机生成初始魔力
            float initial = MIN_INITIAL_MANA + RAND.nextFloat() * (MAX_INITIAL_MANA - MIN_INITIAL_MANA);
            manaMap.put(key, initial);
            setDirty();
            return initial;
        }
        return value;
    }

    /** 设置指定区块的魔力浓度（自动钳制在 [0, MAX_MANA]） */
    public void setMana(ChunkPos pos, float value) {
        float clamped = Math.max(0, Math.min(value, MAX_MANA));
        manaMap.put(pos.toLong(), clamped);
        setDirty();
    }

    /** 自然恢复：增加魔力，不超过初始值（取 DEFAULT_MANA 作为上限） */
    public void regenMana(ChunkPos pos, float amount) {
        float current = getMana(pos);
        if (current >= DEFAULT_MANA) return;
        setMana(pos, Math.min(DEFAULT_MANA, current + amount));
    }

    /**
     * 魔力均衡化：高魔力区块向低魔力相邻区块转移魔力。
     * @param pos 当前区块
     * @param neighbors 相邻区块列表（通常是上下左右4个）
     */
    public void equalizeMana(ChunkPos pos, ChunkPos[] neighbors) {
        float myMana = getMana(pos);
        for (ChunkPos neighbor : neighbors) {
            float neighborMana = getMana(neighbor);
            if (myMana > neighborMana) {
                float diff = myMana - neighborMana;
                float transfer = diff * EQUALIZE_RATE;
                if (transfer < 0.1F) continue; // 忽略极小量
                myMana -= transfer;
                setMana(neighbor, neighborMana + transfer);
            }
        }
        setMana(pos, myMana);
    }

    /**
     * 从区块中消耗指定量魔力。
     * @return 实际消耗的量
     */
    public float consumeMana(ChunkPos pos, float amount) {
        float current = getMana(pos);
        if (current <= 0) return 0;
        float consumed = Math.min(current, amount);
        setMana(pos, current - consumed);
        return consumed;
    }

    /** 检查区块是否有足够的魔力 */
    public boolean hasMana(ChunkPos pos, float amount) {
        return getMana(pos) >= amount;
    }

    /** 获取区块魔力浓度百分比（基于DEFAULT_MANA） */
    public float getManaPercent(ChunkPos pos) {
        return getMana(pos) / DEFAULT_MANA;
    }

    /** 已记录的区块数量（用于调试） */
    public int getTrackedChunkCount() {
        return manaMap.size();
    }

    // ─── 序列化 ─────────────────────────────────────────────────────

    public static ChunkManaSavedData load(CompoundTag tag) {
        ChunkManaSavedData data = new ChunkManaSavedData();
        ListTag list = tag.getList("Chunks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            long key = entry.getLong("Pos");
            float mana = entry.getFloat("Mana");
            data.manaMap.put(key, mana);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, Float> entry : manaMap.entrySet()) {
            CompoundTag chunkTag = new CompoundTag();
            chunkTag.putLong("Pos", entry.getKey());
            chunkTag.putFloat("Mana", entry.getValue());
            list.add(chunkTag);
        }
        tag.put("Chunks", list);
        return tag;
    }

    // ─── 工厂 ────────────────────────────────────────────────────────

    public static ChunkManaSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                ChunkManaSavedData::load,
                ChunkManaSavedData::new,
                DATA_NAME
        );
    }
}
