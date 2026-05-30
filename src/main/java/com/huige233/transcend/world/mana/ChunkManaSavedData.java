package com.huige233.transcend.world.mana;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 区块魔力浓度持久化存储。
 *
 * <p>每个维度独立存储，键为 ChunkPos.toLong()。
 * 首次访问的区块随机生成 100~10000 的初始魔力。
 *
 * <p>R56 加入 4 档地脉分级与 leyline_stabilizer 注册表：
 * <ul>
 *   <li>{@link Tier} 按浓度自动判定，影响 mana well 等抽取设备</li>
 *   <li>{@link #addStabilizer(ChunkPos)} / {@link #removeStabilizer(ChunkPos)} 管理稳定器存在</li>
 *   <li>稳定器存在时，{@link Tier#WEAK} 的抽取惩罚减半，且抽取下限从 1000 → 750</li>
 * </ul>
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

    // === 区块间魔力均衡化 (R-rework) ===
    /**
     * 均衡化触发频率：每 N tick 一次。
     * <p>从旧版 200 tick 大幅下调到 10 tick — 结合新的对称差值算法，
     * 单次转移更小但更高频，最终收敛速度与旧版相近，但流动更平滑。
     */
    public static final int EQUALIZE_INTERVAL_TICKS = 10;
    /**
     * 每对相邻区块单次转移强度：{@code transfer = (A.mana − B.mana) × RATE}。
     *
     * <p>设计推导：用户给定的稳态例子是「中心被抽干，4 邻居各 5000 → 每邻居出 750」。
     * 单对差值 5000，转移 750 ⇒ RATE = 0.15。
     *
     * <p>「再外圈填补内圈 ≤ 内圈支出 1/4」的约束等价于 {@code RATE ≤ 0.25}：
     * 当 r=1 区块在 t=0 输出 X 后，t=1 时 r=2 区块（仍为初始值）与 r=1 的差值正比于 X，
     * 转移量 = X × RATE。RATE = 0.15 给出 60% 的安全余量。
     */
    public static final float EQUALIZE_RATE = 0.15F;
    /**
     * 单对单次转移的硬上限比例（安全网，作为 RATE 设定的最后防线）。
     * <p>{@code |transfer| ≤ max(A, B) × PER_PAIR_LIMIT_RATIO}
     */
    public static final float PER_PAIR_LIMIT_RATIO = 0.25F;

    // === R56: 地脉分级阈值 (设计稿 D5) ===
    /** Exhausted ↑ Weak 边界（无稳定器）。 */
    public static final float TIER_WEAK_FLOOR = 1000.0F;
    /** Weak ↑ Stable 边界。 */
    public static final float TIER_STABLE_FLOOR = 2500.0F;
    /** Stable ↑ Rich 边界。 */
    public static final float TIER_RICH_FLOOR = 7500.0F;
    /** 有稳定器时的抽取下限（取代 1000）。 */
    public static final float STABILIZED_EXTRACT_FLOOR = 750.0F;

    /**
     * 地脉浓度分级。
     */
    public enum Tier {
        /** 0-999: 枯竭 — 抽取设备停工。 */
        EXHAUSTED,
        /** 1000-2499: 虚弱 — 抽取速率 -20%（或稳定器下 -10%）。 */
        WEAK,
        /** 2500-7499: 稳定 — 标准速率。 */
        STABLE,
        /** 7500+: 丰沛 — 抽取速率 +10%。 */
        RICH
    }

    private final Map<Long, Float> manaMap = new HashMap<>();
    /** R56: chunk pos long → 当前已注册稳定器的位置集合。一个区块只允许一个稳定器。 */
    private final Set<Long> stabilizedChunks = new HashSet<>();

    public ChunkManaSavedData() {}

    // ─── 访问器 ─────────────────────────────────────────────────────

    /** 获取指定区块的当前魔力浓度（首次访问时随机初始化） */
    public float getMana(ChunkPos pos) {
        long key = pos.toLong();
        Float value = manaMap.get(key);
        if (value == null) {
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

    public void equalizeMana(ChunkPos pos, ChunkPos[] neighbors) {
        // 旧的单向流动算法已被 equalizePass 替代。保留方法是为了兼容旧调用签名。
        // 新逻辑：对当前区块与每个邻居用对称差值算法，但只是单点处理（不推荐，应改用 equalizePass）。
        Set<ChunkPos> set = new HashSet<>();
        set.add(pos);
        for (ChunkPos n : neighbors) set.add(n);
        equalizePass(set);
    }

    /**
     * 区块魔力对称均衡化 — 一次性处理一组已加载区块的所有 cardinal 相邻关系。
     *
     * <p>算法：
     * <ol>
     *   <li>对每个区块只考虑 +X 和 +Z 方向的邻居（自动去重，每对相邻区块只算一次）</li>
     *   <li>对每对 (A, B)：{@code transfer = (A.mana − B.mana) × EQUALIZE_RATE}（可正可负，无方向）</li>
     *   <li>每对转移量再钳制到 {@code ±max(A, B) × PER_PAIR_LIMIT_RATIO} 内（安全网）</li>
     *   <li>所有 transfer 累积到 deltas，最后一次性应用到 manaMap</li>
     * </ol>
     *
     * <p>守恒性：单步内 A 减去多少，B 就加上多少，总量守恒。
     * 唯一可能破坏守恒的是 {@link #setMana} 的边界钳制（mana 不能负或超 MAX_MANA），
     * 这种"溢出/欠流"在物理上视作泄漏，由自然恢复填补。
     *
     * <p>例子验证（中心 0，4 邻居 5000）：
     * <pre>
     *   每对 (邻居=5000, 中心=0)：transfer = (5000 − 0) × 0.15 = 750
     *   中心 4 个方向各 +750 ⇒ +3000，新值 3000
     *   每个邻居 −750，新值 4250
     *   总和：3000 + 4 × 4250 = 20000 = 4 × 5000 + 0  ✓
     * </pre>
     */
    public void equalizePass(Set<ChunkPos> loadedChunks) {
        if (loadedChunks == null || loadedChunks.size() < 2) return;

        // 累积每个区块的 net delta，避免迭代过程中边读边写造成的次序依赖
        Map<Long, Float> deltas = new HashMap<>(loadedChunks.size());

        for (ChunkPos a : loadedChunks) {
            float manaA = getMana(a);
            // 仅 +X、+Z 两个方向 → 每对相邻区块只算一次
            ChunkPos[] outDirs = {
                    new ChunkPos(a.x + 1, a.z),
                    new ChunkPos(a.x, a.z + 1)
            };
            for (ChunkPos b : outDirs) {
                if (!loadedChunks.contains(b)) continue;
                float manaB = getMana(b);
                float diff = manaA - manaB;
                if (diff == 0F) continue;

                float transfer = diff * EQUALIZE_RATE;
                // 安全网：硬上限 max(A,B) × PER_PAIR_LIMIT_RATIO
                float cap = Math.max(manaA, manaB) * PER_PAIR_LIMIT_RATIO;
                if (transfer >  cap) transfer =  cap;
                if (transfer < -cap) transfer = -cap;

                // A 流出 transfer，B 流入 transfer
                deltas.merge(a.toLong(), -transfer, Float::sum);
                deltas.merge(b.toLong(),  transfer, Float::sum);
            }
        }

        if (deltas.isEmpty()) return;
        for (Map.Entry<Long, Float> e : deltas.entrySet()) {
            ChunkPos pos = new ChunkPos(e.getKey());
            setMana(pos, getMana(pos) + e.getValue());
        }
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

    public boolean hasMana(ChunkPos pos, float amount) {
        return getMana(pos) >= amount;
    }

    public float getManaPercent(ChunkPos pos) {
        return getMana(pos) / DEFAULT_MANA;
    }

    public int getTrackedChunkCount() {
        return manaMap.size();
    }

    // ─── R56: 地脉分级 + 稳定器 API ─────────────────────────────────

    /**
     * 按当前魔力浓度判定该区块的地脉分级（不考虑稳定器；稳定器仅影响数值修正而非分级判定）。
     */
    public Tier getTier(ChunkPos pos) {
        float mana = getMana(pos);
        if (mana < TIER_WEAK_FLOOR) return Tier.EXHAUSTED;
        if (mana < TIER_STABLE_FLOOR) return Tier.WEAK;
        if (mana < TIER_RICH_FLOOR) return Tier.STABLE;
        return Tier.RICH;
    }

    /** 该区块是否注册了稳定器（活跃与否需结合 BE 是否在线，本类只记录存在性）。 */
    public boolean isStabilized(ChunkPos pos) {
        return stabilizedChunks.contains(pos.toLong());
    }

    /** 注册稳定器；返回 true 若注册成功，false 表示该区块已存在稳定器。 */
    public boolean addStabilizer(ChunkPos pos) {
        boolean added = stabilizedChunks.add(pos.toLong());
        if (added) setDirty();
        return added;
    }

    /** 注销稳定器（方块被破坏时调用）。 */
    public void removeStabilizer(ChunkPos pos) {
        if (stabilizedChunks.remove(pos.toLong())) {
            setDirty();
        }
    }

    /**
     * 抽取速率修正（mana well 等抽取设备使用）。
     * <ul>
     *   <li>EXHAUSTED: 0（拒绝抽取）</li>
     *   <li>WEAK: 0.8（无稳定器）/ 0.9（有稳定器）</li>
     *   <li>STABLE: 1.0</li>
     *   <li>RICH: 1.1</li>
     * </ul>
     * 本方法不修改任何状态，仅查询。
     */
    public float getExtractMultiplier(ChunkPos pos) {
        Tier t = getTier(pos);
        boolean stabilized = isStabilized(pos);
        return switch (t) {
            case EXHAUSTED -> 0.0F;
            case WEAK -> stabilized ? 0.9F : 0.8F;
            case STABLE -> 1.0F;
            case RICH -> 1.1F;
        };
    }

    /**
     * 抽取下限：低于此值时拒绝抽取。
     * 默认 1000；有稳定器时降为 750。
     */
    public float getExtractFloor(ChunkPos pos) {
        return isStabilized(pos) ? STABILIZED_EXTRACT_FLOOR : TIER_WEAK_FLOOR;
    }

    /**
     * 安全抽取：考虑分级、稳定器与下限。返回实际消耗量。
     */
    public float consumeManaSafe(ChunkPos pos, float requested) {
        if (requested <= 0) return 0;
        float current = getMana(pos);
        float floor = getExtractFloor(pos);
        if (current <= floor) return 0;
        float available = current - floor;
        float multiplier = getExtractMultiplier(pos);
        if (multiplier <= 0) return 0;
        float effective = Math.min(requested, available);
        float consumed = effective * multiplier;
        setMana(pos, current - consumed);
        return consumed;
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
        // R56 兼容：存在则加载，缺失则空集（旧存档不丢失）
        if (tag.contains("Stabilizers", Tag.TAG_LONG_ARRAY)) {
            long[] arr = tag.getLongArray("Stabilizers");
            for (long k : arr) data.stabilizedChunks.add(k);
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
        // R56: 稳定器集合
        long[] arr = new long[stabilizedChunks.size()];
        int i = 0;
        for (Long k : stabilizedChunks) arr[i++] = k;
        tag.put("Stabilizers", new LongArrayTag(arr));
        return tag;
    }

    public static ChunkManaSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                ChunkManaSavedData::load,
                ChunkManaSavedData::new,
                DATA_NAME
        );
    }
}
