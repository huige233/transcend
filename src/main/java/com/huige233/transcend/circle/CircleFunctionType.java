package com.huige233.transcend.circle;

/**
 * 魔法阵功能类型枚举。
 * <p>
 * 涵盖所有可在魔法阵多方块结构上激活的功能，分布于若干 {@link CircleCategory 分类} 之中。
 * 每个功能都规定了最低可用等级 {@link CircleTier}，以及每分钟的基础魔力消耗（CM/min）。
 * 实际运行消耗会被 {@link CircleManaMath#computeFinalUpkeep 公式} 根据符文加成与环境因素进一步调整。
 */
public enum CircleFunctionType {

    // ===== 魔力物流（基础设计） =====
    /** 灵脉汲取：自给自足地从所在区块抽取魔力。 */
    LEYLINE_SIPHON("leyline_siphon", CircleCategory.MANA_LOGISTICS, CircleTier.INITIATE, 0f),
    /** 远程魔力链接：与远处节点建立点对点的魔力输送通道。 */
    REMOTE_MANA_LINK("remote_mana_link", CircleCategory.MANA_LOGISTICS, CircleTier.ADEPT, 2f),
    /** 奥术增幅：放大其它魔法阵或法器的输出效率。 */
    ARCANE_AMPLIFIER("arcane_amplifier", CircleCategory.MANA_LOGISTICS, CircleTier.ADEPT, 6f),
    /** 涌泉复苏：缓慢补充区块魔力浓度。 */
    WELLSPRING_RENEWAL("wellspring_renewal", CircleCategory.MANA_LOGISTICS, CircleTier.MASTER, 8f),
    /** 灵脉汇聚：在大范围内整合多条魔力脉络。 */
    LEYLINE_CONVERGENCE("leyline_convergence", CircleCategory.MANA_LOGISTICS, CircleTier.ARCHON, 60f),

    // ===== 玩家增益 =====
    /** 守护神盾：为范围内玩家提供伤害减免。 */
    WARDING_AEGIS("warding_aegis", CircleCategory.PLAYER_BUFF, CircleTier.INITIATE, 3f),
    /** 旅人疾行：提升范围内玩家移动速度。 */
    WAYFARERS_HASTE("wayfarers_haste", CircleCategory.PLAYER_BUFF, CircleTier.INITIATE, 2f),
    /** 深视面纱：赋予黑暗视觉与隐匿效果。 */
    DEEP_SIGHT_VEIL("deep_sight_veil", CircleCategory.PLAYER_BUFF, CircleTier.ADEPT, 2f),
    /** 翠绿复元：定期恢复生命与饥饿值。 */
    VERDANT_RESTORATION("verdant_restoration", CircleCategory.PLAYER_BUFF, CircleTier.ADEPT, 8f),
    /** 苍穹披风：赋予缓慢飘落与跌落保护。 */
    SKY_MANTLE("sky_mantle", CircleCategory.PLAYER_BUFF, CircleTier.MASTER, 24f),

    // ===== 世界交互 =====
    /** 气象敕令：操控天气状态。 */
    WEATHER_EDICT("weather_edict", CircleCategory.WORLD_INTERACTION, CircleTier.MASTER, 5f),
    /** 时序织机：在范围内加快或减慢时间流逝。 */
    CHRONO_LOOM("chrono_loom", CircleCategory.WORLD_INTERACTION, CircleTier.ARCHON, 20f),
    /** 静谧结界：抑制范围内的噪音与生物刷新。 */
    QUIET_BOUNDARY("quiet_boundary", CircleCategory.WORLD_INTERACTION, CircleTier.ADEPT, 6f),
    /** 永明曼陀罗：提供恒定光照而无需光源方块。 */
    EVERLIGHT_MANDALA("everlight_mandala", CircleCategory.WORLD_INTERACTION, CircleTier.INITIATE, 1f),
    /** 双天之门：连接两点形成传送门廊。 */
    TWIN_HORIZON_GATE("twin_horizon_gate", CircleCategory.WORLD_INTERACTION, CircleTier.MASTER, 12f),
    /** 炉灶稳衡：稳定范围内的火焰、岩浆等不稳定方块。 */
    HEARTH_STABILITY("hearth_stability", CircleCategory.WORLD_INTERACTION, CircleTier.ADEPT, 10f),

    // ===== 高级 =====
    /** 维度之锚：阻止范围内的玩家与生物被传送。 */
    DIMENSIONAL_ANCHOR("dimensional_anchor", CircleCategory.ADVANCED, CircleTier.ARCHON, 40f),
    /** 元素熔炉：在区域内合成与转化元素。 */
    ELEMENTAL_CRUCIBLE("elemental_crucible", CircleCategory.ADVANCED, CircleTier.MASTER, 10f),
    /** 法术共鸣核心：增强范围内施法者的法术效果。 */
    SPELL_RESONANCE_NEXUS("spell_resonance_nexus", CircleCategory.ADVANCED, CircleTier.ARCHON, 15f),
    /** 关枢门户：跨维度的稳定传送门基础。 */
    NEXUS_GATEHOUSE("nexus_gatehouse", CircleCategory.ADVANCED, CircleTier.ARCHON, 20f),
    /** 原初共时：T5 限定的世界级共鸣效应。 */
    PRIMORDIAL_SYNCHRONY("primordial_synchrony", CircleCategory.ADVANCED, CircleTier.PRIMORDIAL, 120f),

    // ===== 扩展 A：农业 =====
    /** 翠绿收割：自动加速并收割作物。 */
    VERDANT_REAPING("verdant_reaping", CircleCategory.FARMING, CircleTier.ADEPT, 5f),
    /** 矿脉汇聚：缓慢生成或浮现矿物。 */
    MINERAL_CONVERGENCE("mineral_convergence", CircleCategory.FARMING, CircleTier.MASTER, 12f),
    /** 育巢炉灶：增加生物刷新与繁殖速度。 */
    BROOD_HEARTH("brood_hearth", CircleCategory.FARMING, CircleTier.ADEPT, 6f),

    // ===== 扩展 A：防御 =====
    /** 神盾矩阵：组合式防御网，抵御实体入侵。 */
    AEGIS_LATTICE("aegis_lattice", CircleCategory.DEFENSE, CircleTier.MASTER, 9f),
    /** 哨卫警报：感知并提示范围内的敌对生物。 */
    SENTINEL_ALARM("sentinel_alarm", CircleCategory.DEFENSE, CircleTier.INITIATE, 1.5f),
    /** 织陷中继：联动陷阱与触发器。 */
    TRAPWEAVER_RELAY("trapweaver_relay", CircleCategory.DEFENSE, CircleTier.ADEPT, 4.2f),

    // ===== 扩展 A：社交 =====
    /** 盟约储库：在玩家间共享魔力与资源。 */
    COVENANT_RESERVOIR("covenant_reservoir", CircleCategory.SOCIAL, CircleTier.ADEPT, 6f),
    /** 协奏旗帜：为团队提供持续的小幅增益。 */
    CONCORDANT_BANNER("concordant_banner", CircleCategory.SOCIAL, CircleTier.MASTER, 7.2f),

    // ===== 扩展 A：探索 =====
    /** 制图者之眼：扩展小地图与世界地图视野。 */
    CARTOGRAPHERS_EYE("cartographers_eye", CircleCategory.EXPLORATION, CircleTier.ADEPT, 2.4f),
    /** 群系共鸣：感知附近的稀有生物群系与结构。 */
    BIOME_RESONANCE("biome_resonance", CircleCategory.EXPLORATION, CircleTier.MASTER, 4.8f),

    // ===== 扩展 A：制作 =====
    /** 奥术锻场域：为附近工作站提供加速与附魔加成。 */
    ARCANIST_FORGE_FIELD("arcanist_forge_field", CircleCategory.CRAFTING, CircleTier.ADEPT, 3f),
    /** 复原光环：自动修复范围内可修复的物品与方块。 */
    RESTORATION_HALO("restoration_halo", CircleCategory.CRAFTING, CircleTier.MASTER, 6f),

    // ===== 扩展 A：美学 =====
    /** 棱彩共鸣：随情境变化的彩光特效。 */
    PRISMATIC_ATTUNEMENT("prismatic_attunement", CircleCategory.AESTHETIC, CircleTier.ADEPT, 6f),
    /** 极光剧场：在天空中呈现极光特效。 */
    AURORA_THEATRE("aurora_theatre", CircleCategory.AESTHETIC, CircleTier.MASTER, 0.9f),

    // ===== 扩展 A：危险 =====
    /** 虚空钻探：穿透世界底部抽取深渊魔力，伴随显著副作用。 */
    VOID_BORE("void_bore", CircleCategory.DANGEROUS, CircleTier.PRIMORDIAL, 42f);

    /** 功能字符串 ID，用于本地化键、配方与数据驱动查找。 */
    private final String id;
    /** 所属功能分类。 */
    private final CircleCategory category;
    /** 可激活该功能的最低魔法阵等级。 */
    private final CircleTier minTier;
    /** 基础每分钟魔力消耗（CM/min），未经符文与环境修正。 */
    private final float baseUpkeepPerMinute;
    /** 自动生成的本地化键。 */
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
