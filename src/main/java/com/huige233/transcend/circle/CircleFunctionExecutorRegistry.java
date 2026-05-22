package com.huige233.transcend.circle;

import com.huige233.transcend.circle.executor.*;

import java.util.EnumMap;
import java.util.Map;

/**
 * 法环功能执行器注册表。
 * 将 CircleFunctionType 映射到对应的 CircleFunctionExecutor 实例。
 */
public class CircleFunctionExecutorRegistry {

    private static final Map<CircleFunctionType, CircleFunctionExecutor> EXECUTORS = new EnumMap<>(CircleFunctionType.class);

    static {
        // 魔力物流
        reg(CircleFunctionType.LEYLINE_SIPHON, new LeySiphonExecutor());
        reg(CircleFunctionType.REMOTE_MANA_LINK, new RemoteManaLinkExecutor());
        reg(CircleFunctionType.ARCANE_AMPLIFIER, new ArcaneAmplifierExecutor());
        reg(CircleFunctionType.WELLSPRING_RENEWAL, new WellspringRenewalExecutor());
        reg(CircleFunctionType.LEYLINE_CONVERGENCE, new LeylineConvergenceExecutor());

        // 玩家增益
        reg(CircleFunctionType.WARDING_AEGIS, new WardingAegisExecutor());
        reg(CircleFunctionType.WAYFARERS_HASTE, new WayfarersHasteExecutor());
        reg(CircleFunctionType.DEEP_SIGHT_VEIL, new DeepSightVeilExecutor());
        reg(CircleFunctionType.VERDANT_RESTORATION, new VerdantRestorationExecutor());
        reg(CircleFunctionType.SKY_MANTLE, new SkyMantleExecutor());

        // 世界交互
        reg(CircleFunctionType.WEATHER_EDICT, new WeatherEdictExecutor());
        reg(CircleFunctionType.CHRONO_LOOM, new ChronoLoomExecutor());
        reg(CircleFunctionType.QUIET_BOUNDARY, new QuietBoundaryExecutor());
        reg(CircleFunctionType.EVERLIGHT_MANDALA, new EverlightMandalaExecutor());
        reg(CircleFunctionType.TWIN_HORIZON_GATE, new TwinHorizonGateExecutor());
        reg(CircleFunctionType.HEARTH_STABILITY, new HearthStabilityExecutor());

        // 高级
        reg(CircleFunctionType.DIMENSIONAL_ANCHOR, new DimensionalAnchorExecutor());
        reg(CircleFunctionType.ELEMENTAL_CRUCIBLE, new ElementalCrucibleExecutor());
        reg(CircleFunctionType.SPELL_RESONANCE_NEXUS, new SpellResonanceNexusExecutor());
        reg(CircleFunctionType.NEXUS_GATEHOUSE, new NexusGatehouseExecutor());
        reg(CircleFunctionType.PRIMORDIAL_SYNCHRONY, new PrimordialSynchronyExecutor());

        // 扩展功能
        reg(CircleFunctionType.VERDANT_REAPING, new VerdantReapingExecutor());
        reg(CircleFunctionType.MINERAL_CONVERGENCE, new MineralConvergenceExecutor());
        reg(CircleFunctionType.BROOD_HEARTH, new BroodHearthExecutor());
        reg(CircleFunctionType.AEGIS_LATTICE, new AegisLatticeExecutor());
        reg(CircleFunctionType.SENTINEL_ALARM, new SentinelAlarmExecutor());
        reg(CircleFunctionType.TRAPWEAVER_RELAY, new TrapweaverRelayExecutor());
        reg(CircleFunctionType.COVENANT_RESERVOIR, new CovenantReservoirExecutor());
        reg(CircleFunctionType.CONCORDANT_BANNER, new ConcordantBannerExecutor());
        reg(CircleFunctionType.CARTOGRAPHERS_EYE, new CartographersEyeExecutor());
        reg(CircleFunctionType.BIOME_RESONANCE, new BiomeResonanceExecutor());
        reg(CircleFunctionType.ARCANIST_FORGE_FIELD, new ArcanistForgeFieldExecutor());
        reg(CircleFunctionType.RESTORATION_HALO, new RestorationHaloExecutor());
        reg(CircleFunctionType.PRISMATIC_ATTUNEMENT, new PrismaticAttunementExecutor());
        reg(CircleFunctionType.AURORA_THEATRE, new AuroraTheatreExecutor());
        reg(CircleFunctionType.VOID_BORE, new VoidBoreExecutor());
    }

    private static void reg(CircleFunctionType type, CircleFunctionExecutor executor) {
        EXECUTORS.put(type, executor);
    }

    /**
     * 获取指定功能类型的执行器实例。
     * @return 执行器，如果未注册则返回 null
     */
    public static CircleFunctionExecutor get(CircleFunctionType type) {
        return EXECUTORS.get(type);
    }
}
