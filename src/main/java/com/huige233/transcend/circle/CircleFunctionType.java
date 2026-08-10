package com.huige233.transcend.circle;

/** 法阵功能类型枚举。 */
public enum CircleFunctionType {

    LEYLINE_SIPHON("leyline_siphon", CircleCategory.MANA_LOGISTICS, CircleTier.INITIATE, 0f),

    REMOTE_MANA_LINK("remote_mana_link", CircleCategory.MANA_LOGISTICS, CircleTier.ADEPT, 2f),

    ARCANE_AMPLIFIER("arcane_amplifier", CircleCategory.MANA_LOGISTICS, CircleTier.ADEPT, 6f),

    WELLSPRING_RENEWAL("wellspring_renewal", CircleCategory.MANA_LOGISTICS, CircleTier.MASTER, 8f),

    LEYLINE_CONVERGENCE("leyline_convergence", CircleCategory.MANA_LOGISTICS, CircleTier.ARCHON, 60f),

    WARDING_AEGIS("warding_aegis", CircleCategory.PLAYER_BUFF, CircleTier.INITIATE, 3f),

    WAYFARERS_HASTE("wayfarers_haste", CircleCategory.PLAYER_BUFF, CircleTier.INITIATE, 2f),

    DEEP_SIGHT_VEIL("deep_sight_veil", CircleCategory.PLAYER_BUFF, CircleTier.ADEPT, 2f),

    VERDANT_RESTORATION("verdant_restoration", CircleCategory.PLAYER_BUFF, CircleTier.ADEPT, 8f),

    SKY_MANTLE("sky_mantle", CircleCategory.PLAYER_BUFF, CircleTier.MASTER, 24f),

    WEATHER_EDICT("weather_edict", CircleCategory.WORLD_INTERACTION, CircleTier.MASTER, 5f),

    CHRONO_LOOM("chrono_loom", CircleCategory.WORLD_INTERACTION, CircleTier.ARCHON, 20f),

    QUIET_BOUNDARY("quiet_boundary", CircleCategory.WORLD_INTERACTION, CircleTier.ADEPT, 6f),

    EVERLIGHT_MANDALA("everlight_mandala", CircleCategory.WORLD_INTERACTION, CircleTier.INITIATE, 1f),

    TWIN_HORIZON_GATE("twin_horizon_gate", CircleCategory.WORLD_INTERACTION, CircleTier.MASTER, 12f),

    HEARTH_STABILITY("hearth_stability", CircleCategory.WORLD_INTERACTION, CircleTier.ADEPT, 10f),

    DIMENSIONAL_ANCHOR("dimensional_anchor", CircleCategory.ADVANCED, CircleTier.ARCHON, 40f),

    ELEMENTAL_CRUCIBLE("elemental_crucible", CircleCategory.ADVANCED, CircleTier.MASTER, 10f),

    SPELL_RESONANCE_NEXUS("spell_resonance_nexus", CircleCategory.ADVANCED, CircleTier.ARCHON, 15f),

    NEXUS_GATEHOUSE("nexus_gatehouse", CircleCategory.ADVANCED, CircleTier.ARCHON, 20f),

    PRIMORDIAL_SYNCHRONY("primordial_synchrony", CircleCategory.ADVANCED, CircleTier.PRIMORDIAL, 120f),

    VERDANT_REAPING("verdant_reaping", CircleCategory.FARMING, CircleTier.ADEPT, 5f),

    MINERAL_CONVERGENCE("mineral_convergence", CircleCategory.FARMING, CircleTier.MASTER, 12f),

    BROOD_HEARTH("brood_hearth", CircleCategory.FARMING, CircleTier.ADEPT, 6f),

    AEGIS_LATTICE("aegis_lattice", CircleCategory.DEFENSE, CircleTier.MASTER, 9f),

    SENTINEL_ALARM("sentinel_alarm", CircleCategory.DEFENSE, CircleTier.INITIATE, 1.5f),

    TRAPWEAVER_RELAY("trapweaver_relay", CircleCategory.DEFENSE, CircleTier.ADEPT, 4.2f),

    COVENANT_RESERVOIR("covenant_reservoir", CircleCategory.SOCIAL, CircleTier.ADEPT, 6f),

    CONCORDANT_BANNER("concordant_banner", CircleCategory.SOCIAL, CircleTier.MASTER, 7.2f),

    CARTOGRAPHERS_EYE("cartographers_eye", CircleCategory.EXPLORATION, CircleTier.ADEPT, 2.4f),

    BIOME_RESONANCE("biome_resonance", CircleCategory.EXPLORATION, CircleTier.MASTER, 4.8f),

    ARCANIST_FORGE_FIELD("arcanist_forge_field", CircleCategory.CRAFTING, CircleTier.ADEPT, 3f),

    RESTORATION_HALO("restoration_halo", CircleCategory.CRAFTING, CircleTier.MASTER, 6f),

    PRISMATIC_ATTUNEMENT("prismatic_attunement", CircleCategory.AESTHETIC, CircleTier.ADEPT, 6f),

    AURORA_THEATRE("aurora_theatre", CircleCategory.AESTHETIC, CircleTier.MASTER, 0.9f),

    VOID_BORE("void_bore", CircleCategory.DANGEROUS, CircleTier.PRIMORDIAL, 42f);

    private final String id;

    private final CircleCategory category;

    private final CircleTier minTier;

    private final float baseUpkeepPerMinute;

    private final String translationKey;

    CircleFunctionType(String id, CircleCategory category, CircleTier minTier, float baseUpkeepPerMinute) {
        this.id = id;
        this.category = category;
        this.minTier = minTier;
        this.baseUpkeepPerMinute = baseUpkeepPerMinute;
        this.translationKey = "circle.transcend.function." + id;
    }

    public String getId() {
        return id;
    }

    public CircleCategory getCategory() {
        return category;
    }

    public CircleTier getMinTier() {
        return minTier;
    }

    public float getBaseUpkeepPerMinute() {
        return baseUpkeepPerMinute;
    }

    public String getTranslationKey() {
        return translationKey;
    }
}
