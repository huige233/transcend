package com.huige233.transcend.circle;

import com.huige233.transcend.circle.executor.*;

import java.util.EnumMap;
import java.util.Map;

/** 法阵功能执行器注册表。 */
public class CircleFunctionExecutorRegistry {

    private static final Map<CircleFunctionType, CircleFunctionExecutor> EXECUTORS = new EnumMap<>(CircleFunctionType.class);

    static {

        reg(CircleFunctionType.LEYLINE_SIPHON, new LeySiphonExecutor());
        reg(CircleFunctionType.REMOTE_MANA_LINK, new RemoteManaLinkExecutor());
        reg(CircleFunctionType.ARCANE_AMPLIFIER, new ArcaneAmplifierExecutor());
        reg(CircleFunctionType.WELLSPRING_RENEWAL, new WellspringRenewalExecutor());
        reg(CircleFunctionType.LEYLINE_CONVERGENCE, new LeylineConvergenceExecutor());

        reg(CircleFunctionType.WARDING_AEGIS, new WardingAegisExecutor());
        reg(CircleFunctionType.WAYFARERS_HASTE, new WayfarersHasteExecutor());
        reg(CircleFunctionType.DEEP_SIGHT_VEIL, new DeepSightVeilExecutor());
        reg(CircleFunctionType.VERDANT_RESTORATION, new VerdantRestorationExecutor());
        reg(CircleFunctionType.SKY_MANTLE, new SkyMantleExecutor());

        reg(CircleFunctionType.WEATHER_EDICT, new WeatherEdictExecutor());
        reg(CircleFunctionType.CHRONO_LOOM, new ChronoLoomExecutor());
        reg(CircleFunctionType.QUIET_BOUNDARY, new QuietBoundaryExecutor());
        reg(CircleFunctionType.EVERLIGHT_MANDALA, new EverlightMandalaExecutor());
        reg(CircleFunctionType.TWIN_HORIZON_GATE, new TwinHorizonGateExecutor());
        reg(CircleFunctionType.HEARTH_STABILITY, new HearthStabilityExecutor());

        reg(CircleFunctionType.DIMENSIONAL_ANCHOR, new DimensionalAnchorExecutor());
        reg(CircleFunctionType.ELEMENTAL_CRUCIBLE, new ElementalCrucibleExecutor());
        reg(CircleFunctionType.SPELL_RESONANCE_NEXUS, new SpellResonanceNexusExecutor());
        reg(CircleFunctionType.NEXUS_GATEHOUSE, new NexusGatehouseExecutor());
        reg(CircleFunctionType.PRIMORDIAL_SYNCHRONY, new PrimordialSynchronyExecutor());

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

    public static CircleFunctionExecutor get(CircleFunctionType type) {
        return EXECUTORS.get(type);
    }
}
