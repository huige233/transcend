package com.huige233.transcend.gear.forge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * R82: 坩埚 aspect 解析表（24 命名 aspect + INDETERMINATE 回退）。
 *
 * <h2>4 槽 → aspect 解析规则（不计槽位顺序，仅看 catalyst 重数）</h2>
 * <ul>
 *   <li><b>4×same</b> → 6 种 <b>纯净 aspect</b>（{@link AspectKind#values()} 各 1 个）</li>
 *   <li><b>3+1</b>    → 18 种 <b>双系 aspect</b>（每个主元素绑定 3 个"亲和"副元素；其余 12 个非亲和组合 → INDETERMINATE）</li>
 *   <li>其它（2+2 / 2+1+1 / 1+1+1+1） → 全部 INDETERMINATE（中性零 offset）</li>
 * </ul>
 *
 * <p>合计 6 + 18 = <b>24 命名 aspect</b>。亲和表见 {@link #AFFINITY_ACCENTS}。
 *
 * <h2>使用方式</h2>
 * <pre>{@code
 * AspectKind[] catalysts = { FIRE, FIRE, FIRE, WIND };
 * AspectDef def = AspectRegistry.resolve(catalysts);     // → 炎风 (Pyroclasm)
 * GearForgeData.writeCrucible(stack, def.id(), def.offset(), processId);
 * }</pre>
 */
public final class AspectRegistry {

    /** 主元素 → 3 个亲和副元素（决定哪 18 个 3+1 组合是命名 aspect）。 */
    private static final Map<AspectKind, AspectKind[]> AFFINITY_ACCENTS = new EnumMap<>(AspectKind.class);
    static {
        AFFINITY_ACCENTS.put(AspectKind.FIRE,   new AspectKind[]{AspectKind.WIND,  AspectKind.EARTH, AspectKind.VOID});
        AFFINITY_ACCENTS.put(AspectKind.WATER,  new AspectKind[]{AspectKind.EARTH, AspectKind.WIND,  AspectKind.SPIRIT});
        AFFINITY_ACCENTS.put(AspectKind.EARTH,  new AspectKind[]{AspectKind.FIRE,  AspectKind.WATER, AspectKind.SPIRIT});
        AFFINITY_ACCENTS.put(AspectKind.WIND,   new AspectKind[]{AspectKind.FIRE,  AspectKind.WATER, AspectKind.VOID});
        AFFINITY_ACCENTS.put(AspectKind.SPIRIT, new AspectKind[]{AspectKind.WATER, AspectKind.EARTH, AspectKind.VOID});
        AFFINITY_ACCENTS.put(AspectKind.VOID,   new AspectKind[]{AspectKind.FIRE,  AspectKind.WIND,  AspectKind.SPIRIT});
    }

    /** 不可解析回退 aspect（2+2、2+1+1、1+1+1+1、非亲和 3+1 全部走这里）。 */
    public static final AspectDef INDETERMINATE = new AspectDef(
            "indeterminate", "indeterminate",
            AspectKind.SPIRIT, AspectKind.SPIRIT,
            0.00f, 0x888888);

    /** id → 定义索引（运行时按 id 查回 AspectDef，例如 NBT 反序列化）。 */
    private static final Map<String, AspectDef> BY_ID = new HashMap<>();

    /** (dominant, accent) → AspectDef。pure 用 (k,k)；dual 用 (主,副)；非亲和返回空。 */
    private static final Map<AspectKind, EnumMap<AspectKind, AspectDef>> BY_PAIR = new EnumMap<>(AspectKind.class);

    /** 全部 24 个命名 aspect 的不可变列表（按枚举顺序：6 纯 + 18 双）。 */
    private static final List<AspectDef> ALL = new ArrayList<>(24);

    static {
        // ── 6 纯净 aspect ──────────────────────────────────────────────
        registerPure(AspectKind.FIRE,   "fire_pure",   0.20f, 0xFF5500);
        registerPure(AspectKind.WATER,  "water_pure",  0.20f, 0x3399FF);
        registerPure(AspectKind.EARTH,  "earth_pure",  0.20f, 0x886633);
        registerPure(AspectKind.WIND,   "wind_pure",   0.20f, 0xCCFFCC);
        registerPure(AspectKind.SPIRIT, "spirit_pure", 0.20f, 0xFFFF99);
        registerPure(AspectKind.VOID,   "void_pure",   0.20f, 0x550055);

        // ── 18 双系 aspect（亲和 3+1）──────────────────────────────────
        // 每对的 offset 在 +6%~+15% 之间，按主元素特征调味
        registerDual(AspectKind.FIRE,   AspectKind.WIND,   "pyroclasm",     0.15f, 0xFF7733);
        registerDual(AspectKind.FIRE,   AspectKind.EARTH,  "magma",         0.12f, 0xCC4400);
        registerDual(AspectKind.FIRE,   AspectKind.VOID,   "pyrevoid",      0.10f, 0x991133);

        registerDual(AspectKind.WATER,  AspectKind.EARTH,  "mire",          0.08f, 0x445522);
        registerDual(AspectKind.WATER,  AspectKind.WIND,   "mist",          0.10f, 0x99CCFF);
        registerDual(AspectKind.WATER,  AspectKind.SPIRIT, "cleansing",     0.13f, 0xCCEEFF);

        registerDual(AspectKind.EARTH,  AspectKind.FIRE,   "caldera",       0.13f, 0xAA5511);
        registerDual(AspectKind.EARTH,  AspectKind.WATER,  "loam",          0.08f, 0x556633);
        registerDual(AspectKind.EARTH,  AspectKind.SPIRIT, "hallowed",      0.12f, 0xBBAA55);

        registerDual(AspectKind.WIND,   AspectKind.FIRE,   "wildfire",      0.14f, 0xFFAA33);
        registerDual(AspectKind.WIND,   AspectKind.WATER,  "seabreeze",     0.10f, 0x77CCDD);
        registerDual(AspectKind.WIND,   AspectKind.VOID,   "voidwind",      0.11f, 0x996699);

        registerDual(AspectKind.SPIRIT, AspectKind.WATER,  "spring",        0.11f, 0xCCFFEE);
        registerDual(AspectKind.SPIRIT, AspectKind.EARTH,  "mountain",      0.09f, 0xCCAA77);
        registerDual(AspectKind.SPIRIT, AspectKind.VOID,   "wraith",        0.12f, 0x886688);

        registerDual(AspectKind.VOID,   AspectKind.FIRE,   "voidflame",     0.15f, 0x661144);
        registerDual(AspectKind.VOID,   AspectKind.WIND,   "voidbreath",    0.10f, 0x442266);
        registerDual(AspectKind.VOID,   AspectKind.SPIRIT, "abyssal",       0.13f, 0x331144);

        // INDETERMINATE 不进 ALL/BY_PAIR/BY_ID，只能由 resolve 回退返回（避免被误用为合法 aspect）
    }

    private AspectRegistry() {} // 静态工具类

    private static void registerPure(AspectKind k, String langSubKey, float offset, int color) {
        AspectDef def = new AspectDef(langSubKey, langSubKey, k, k, offset, color);
        ALL.add(def);
        BY_ID.put(def.id(), def);
        BY_PAIR.computeIfAbsent(k, x -> new EnumMap<>(AspectKind.class)).put(k, def);
    }

    private static void registerDual(AspectKind dom, AspectKind acc, String langSubKey, float offset, int color) {
        if (dom == acc) throw new IllegalArgumentException("dual aspect requires dom != acc");
        AspectDef def = new AspectDef(langSubKey, langSubKey, dom, acc, offset, color);
        ALL.add(def);
        BY_ID.put(def.id(), def);
        BY_PAIR.computeIfAbsent(dom, x -> new EnumMap<>(AspectKind.class)).put(acc, def);
    }

    // ─── 公共 API ─────────────────────────────────────────────────────

    /** 按 id 查 AspectDef（NBT 反序列化）；找不到返回 INDETERMINATE。 */
    public static AspectDef byId(String id) {
        if (id == null || id.isEmpty()) return INDETERMINATE;
        return BY_ID.getOrDefault(id, INDETERMINATE);
    }

    /** 不可变的全部命名 aspect 列表（24 个；不含 INDETERMINATE）。 */
    public static List<AspectDef> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** (dominant, accent) 命名查询；非命名组合返回空。 */
    public static Optional<AspectDef> byPair(AspectKind dom, AspectKind acc) {
        EnumMap<AspectKind, AspectDef> sub = BY_PAIR.get(dom);
        if (sub == null) return Optional.empty();
        return Optional.ofNullable(sub.get(acc));
    }

    /**
     * 解析 4 个 catalyst（不计槽位顺序）→ AspectDef。
     *
     * <p>规则：
     * <ol>
     *   <li>统计每个 AspectKind 的重数</li>
     *   <li>有 1 种 catalyst 重数=4 → 对应纯净 aspect</li>
     *   <li>有 1 种 catalyst 重数=3 + 另 1 种 重数=1 →
     *       若副元素是主元素的"亲和"副元素 → 命名双系 aspect；
     *       否则 → INDETERMINATE</li>
     *   <li>其它分布（2+2 / 2+1+1 / 1+1+1+1） → INDETERMINATE</li>
     * </ol>
     *
     * @param catalysts 长度必须为 4 的 AspectKind 数组（任何 null → INDETERMINATE）
     */
    public static AspectDef resolve(AspectKind[] catalysts) {
        if (catalysts == null || catalysts.length != 4) return INDETERMINATE;
        for (AspectKind c : catalysts) if (c == null) return INDETERMINATE;

        EnumMap<AspectKind, Integer> counts = new EnumMap<>(AspectKind.class);
        for (AspectKind c : catalysts) counts.merge(c, 1, Integer::sum);

        AspectKind quad = null;     // 重数=4
        AspectKind triple = null;   // 重数=3
        AspectKind single = null;   // 与 triple 配对的重数=1
        for (var e : counts.entrySet()) {
            int cnt = e.getValue();
            AspectKind k = e.getKey();
            if (cnt == 4) quad = k;
            else if (cnt == 3) triple = k;
            else if (cnt == 1) single = k;
        }

        // 4× same → pure
        if (quad != null) {
            return BY_PAIR.get(quad).get(quad);
        }

        // 3+1 → 看亲和
        if (triple != null && single != null && counts.size() == 2) {
            AspectKind[] affinity = AFFINITY_ACCENTS.get(triple);
            if (Arrays.asList(affinity).contains(single)) {
                AspectDef def = BY_PAIR.get(triple).get(single);
                if (def != null) return def;
            }
        }

        return INDETERMINATE;
    }
}
