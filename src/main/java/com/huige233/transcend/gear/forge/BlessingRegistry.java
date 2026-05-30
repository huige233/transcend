package com.huige233.transcend.gear.forge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * R86: 16 命名 blessing + INDETERMINATE 回退 静态注册表。
 *
 * <h2>4 槽 → blessing 解析规则（不计顺序）</h2>
 * <ul>
 *   <li>4×same → <b>4 纯 blessing</b>（solar / lunar / stellar / abyss）</li>
 *   <li>3+1    → <b>12 双系 blessing</b>（4 dominant × 3 accents = 12，全部为亲和）</li>
 *   <li>其它（2+2 / 2+1+1 / 1+1+1+1）→ INDETERMINATE</li>
 * </ul>
 *
 * <p>合计 4 + 12 = <b>16 命名 blessing</b>。与 R82 不同的是：CelestialKind 只有 4 个，
 * 所以所有 3+1 组合都是合法的（无非亲和概念）；INDETERMINATE 仅来自非 4-same / 非 3+1 的分布。
 */
public final class BlessingRegistry {

    public static final BlessingDef INDETERMINATE = new BlessingDef(
            "indeterminate", "indeterminate",
            CelestialKind.STAR, CelestialKind.STAR,
            0x888888);

    private static final Map<String, BlessingDef> BY_ID = new HashMap<>();
    private static final Map<CelestialKind, EnumMap<CelestialKind, BlessingDef>> BY_PAIR =
            new EnumMap<>(CelestialKind.class);
    private static final List<BlessingDef> ALL = new ArrayList<>(16);

    static {
        // ── 4 纯 blessing ──────────────────────────────────────────────
        registerPure(CelestialKind.SUN,   "solar_crown",    0xFFB833);
        registerPure(CelestialKind.MOON,  "lunar_crown",    0x88CCDD);
        registerPure(CelestialKind.STAR,  "stellar_crown",  0xFFFF88);
        registerPure(CelestialKind.ABYSS, "abyss_crown",    0x553377);

        // ── 12 双系 blessing（3+1 全亲和）──────────────────────────────
        // 3 SUN + 1 X
        registerDual(CelestialKind.SUN,   CelestialKind.MOON,  "eclipse_crown",     0xFFAA77);
        registerDual(CelestialKind.SUN,   CelestialKind.STAR,  "constellation_crown", 0xFFCC55);
        registerDual(CelestialKind.SUN,   CelestialKind.ABYSS, "flare_crown",       0xFF6644);

        // 3 MOON + 1 X
        registerDual(CelestialKind.MOON,  CelestialKind.SUN,   "halo_crown",        0xDDDDAA);
        registerDual(CelestialKind.MOON,  CelestialKind.STAR,  "midnight_crown",    0x99CCFF);
        registerDual(CelestialKind.MOON,  CelestialKind.ABYSS, "eclipse_void_crown",0x556699);

        // 3 STAR + 1 X
        registerDual(CelestialKind.STAR,  CelestialKind.SUN,   "daystars_crown",    0xFFEE99);
        registerDual(CelestialKind.STAR,  CelestialKind.MOON,  "nightstars_crown",  0xAACCFF);
        registerDual(CelestialKind.STAR,  CelestialKind.ABYSS, "blackstars_crown",  0x886699);

        // 3 ABYSS + 1 X
        registerDual(CelestialKind.ABYSS, CelestialKind.SUN,   "emberabyss_crown",  0x882244);
        registerDual(CelestialKind.ABYSS, CelestialKind.MOON,  "frostabyss_crown",  0x445577);
        registerDual(CelestialKind.ABYSS, CelestialKind.STAR,  "voidstars_crown",   0x331144);
    }

    private BlessingRegistry() {} // static

    private static void registerPure(CelestialKind k, String sub, int color) {
        BlessingDef def = new BlessingDef(sub, sub, k, k, color);
        ALL.add(def);
        BY_ID.put(def.id(), def);
        BY_PAIR.computeIfAbsent(k, x -> new EnumMap<>(CelestialKind.class)).put(k, def);
    }

    private static void registerDual(CelestialKind dom, CelestialKind acc, String sub, int color) {
        if (dom == acc) throw new IllegalArgumentException("dual requires dom != acc");
        BlessingDef def = new BlessingDef(sub, sub, dom, acc, color);
        ALL.add(def);
        BY_ID.put(def.id(), def);
        BY_PAIR.computeIfAbsent(dom, x -> new EnumMap<>(CelestialKind.class)).put(acc, def);
    }

    // ─── 公共 API ─────────────────────────────────────────────────────

    public static BlessingDef byId(String id) {
        if (id == null || id.isEmpty()) return INDETERMINATE;
        return BY_ID.getOrDefault(id, INDETERMINATE);
    }

    public static List<BlessingDef> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static Optional<BlessingDef> byPair(CelestialKind dom, CelestialKind acc) {
        EnumMap<CelestialKind, BlessingDef> sub = BY_PAIR.get(dom);
        if (sub == null) return Optional.empty();
        return Optional.ofNullable(sub.get(acc));
    }

    /**
     * 解析 4 个碎片 → BlessingDef。
     * 规则：4×same → pure；3+1 → dual（4 kinds 时全合法）；其它 → INDETERMINATE。
     */
    public static BlessingDef resolve(CelestialKind[] fragments) {
        if (fragments == null || fragments.length != 4) return INDETERMINATE;
        for (CelestialKind c : fragments) if (c == null) return INDETERMINATE;

        EnumMap<CelestialKind, Integer> counts = new EnumMap<>(CelestialKind.class);
        for (CelestialKind c : fragments) counts.merge(c, 1, Integer::sum);

        CelestialKind quad = null, triple = null, single = null;
        for (var e : counts.entrySet()) {
            int n = e.getValue();
            if      (n == 4) quad   = e.getKey();
            else if (n == 3) triple = e.getKey();
            else if (n == 1) single = e.getKey();
        }

        if (quad != null) return BY_PAIR.get(quad).get(quad);
        if (triple != null && single != null && counts.size() == 2) {
            BlessingDef d = BY_PAIR.get(triple).get(single);
            if (d != null) return d;
        }
        return INDETERMINATE;
    }
}
