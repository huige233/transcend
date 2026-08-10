package com.huige233.transcend.gear.forge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 专属特性注册表。 */
public final class AspectRegistry {

    private static final Map<AspectKind, AspectKind[]> AFFINITY_ACCENTS = new EnumMap<>(AspectKind.class);
    static {
        AFFINITY_ACCENTS.put(AspectKind.FIRE,   new AspectKind[]{AspectKind.WIND,  AspectKind.EARTH, AspectKind.VOID});
        AFFINITY_ACCENTS.put(AspectKind.WATER,  new AspectKind[]{AspectKind.EARTH, AspectKind.WIND,  AspectKind.SPIRIT});
        AFFINITY_ACCENTS.put(AspectKind.EARTH,  new AspectKind[]{AspectKind.FIRE,  AspectKind.WATER, AspectKind.SPIRIT});
        AFFINITY_ACCENTS.put(AspectKind.WIND,   new AspectKind[]{AspectKind.FIRE,  AspectKind.WATER, AspectKind.VOID});
        AFFINITY_ACCENTS.put(AspectKind.SPIRIT, new AspectKind[]{AspectKind.WATER, AspectKind.EARTH, AspectKind.VOID});
        AFFINITY_ACCENTS.put(AspectKind.VOID,   new AspectKind[]{AspectKind.FIRE,  AspectKind.WIND,  AspectKind.SPIRIT});
    }

    public static final AspectDef INDETERMINATE = new AspectDef(
            "indeterminate", "indeterminate",
            AspectKind.SPIRIT, AspectKind.SPIRIT,
            0.00f, 0x888888);

    private static final Map<String, AspectDef> BY_ID = new HashMap<>();

    private static final Map<AspectKind, EnumMap<AspectKind, AspectDef>> BY_PAIR = new EnumMap<>(AspectKind.class);

    private static final List<AspectDef> ALL = new ArrayList<>(24);

    static {

        registerPure(AspectKind.FIRE,   "fire_pure",   0.20f, 0xFF5500);
        registerPure(AspectKind.WATER,  "water_pure",  0.20f, 0x3399FF);
        registerPure(AspectKind.EARTH,  "earth_pure",  0.20f, 0x886633);
        registerPure(AspectKind.WIND,   "wind_pure",   0.20f, 0xCCFFCC);
        registerPure(AspectKind.SPIRIT, "spirit_pure", 0.20f, 0xFFFF99);
        registerPure(AspectKind.VOID,   "void_pure",   0.20f, 0x550055);

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

    }

    private AspectRegistry() {}

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

    public static AspectDef byId(String id) {
        if (id == null || id.isEmpty()) return INDETERMINATE;
        return BY_ID.getOrDefault(id, INDETERMINATE);
    }

    public static List<AspectDef> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static Optional<AspectDef> byPair(AspectKind dom, AspectKind acc) {
        EnumMap<AspectKind, AspectDef> sub = BY_PAIR.get(dom);
        if (sub == null) return Optional.empty();
        return Optional.ofNullable(sub.get(acc));
    }

    public static AspectDef resolve(AspectKind[] catalysts) {
        if (catalysts == null || catalysts.length != 4) return INDETERMINATE;
        for (AspectKind c : catalysts) if (c == null) return INDETERMINATE;

        EnumMap<AspectKind, Integer> counts = new EnumMap<>(AspectKind.class);
        for (AspectKind c : catalysts) counts.merge(c, 1, Integer::sum);

        AspectKind quad = null;
        AspectKind triple = null;
        AspectKind single = null;
        for (var e : counts.entrySet()) {
            int cnt = e.getValue();
            AspectKind k = e.getKey();
            if (cnt == 4) quad = k;
            else if (cnt == 3) triple = k;
            else if (cnt == 1) single = k;
        }

        if (quad != null) {
            return BY_PAIR.get(quad).get(quad);
        }

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
