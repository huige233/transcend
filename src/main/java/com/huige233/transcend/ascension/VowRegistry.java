package com.huige233.transcend.ascension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VowRegistry {
    private static final Map<String, AscensionVow> VOWS = new LinkedHashMap<>();

    public static final AscensionVow VOW_OF_DESTRUCTION = register(
        AscensionVow.builder("vow_of_destruction", 3)
            .health(-60)
            .spellDamage(0.40f)
            .build()
    );

    public static final AscensionVow VOW_OF_IMMORTALITY = register(
        AscensionVow.builder("vow_of_immortality", 3)
            .spellDamage(-0.20f)
            .build()
    );

    public static final AscensionVow VOW_OF_GREED = register(
        AscensionVow.builder("vow_of_greed", 3)
            .cooldown(1.40f)
            .manaCap(300f)
            .manaRegen(0.50f)
            .build()
    );

    public static final AscensionVow VOW_OF_SOLITUDE = register(
        AscensionVow.builder("vow_of_solitude", 3)
            .spellDamage(0.60f)
            .build()
    );

    public static final AscensionVow VOW_OF_RESONANCE = register(
        AscensionVow.builder("vow_of_resonance", 3)
            .spellDamage(-0.30f)
            .reaction(1.00f)
            .build()
    );

    public static final AscensionVow VOW_OF_DEVOTION = register(
        AscensionVow.builder("vow_of_devotion", 3)
            .critChance(-1.0f)
            .healing(2.00f)
            .build()
    );

    private static AscensionVow register(AscensionVow vow) {
        VOWS.put(vow.getId(), vow);
        return vow;
    }

    public static AscensionVow get(String id) {
        return VOWS.get(id);
    }

    public static List<AscensionVow> getVowsForStage(int stage) {
        List<AscensionVow> result = new ArrayList<>();
        for (AscensionVow v : VOWS.values()) {
            if (v.getStage() == stage) {
                result.add(v);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static Collection<AscensionVow> all() {
        return Collections.unmodifiableCollection(VOWS.values());
    }
}
