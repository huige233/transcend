package com.huige233.transcend.tech;

import com.huige233.transcend.tech.attribute.TechAttribute;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

   
            
  
                                                   
                             
   
/** 定义并读取枪械、弹药资源、热量、相位护盾和科技属性基础值的通用配置。 */
public final class TechConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final Map<TechAttribute, ForgeConfigSpec.DoubleValue> ATTRIBUTE_BASES =
            new EnumMap<>(TechAttribute.class);

    private static final ForgeConfigSpec.DoubleValue PARTICLE_GUN_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue PARTICLE_GUN_MAX_DAMAGE;
    private static final ForgeConfigSpec.IntValue PARTICLE_GUN_CHARGE_TICKS;
    private static final ForgeConfigSpec.DoubleValue PARTICLE_GUN_BOLT_SPEED;
    private static final ForgeConfigSpec.BooleanValue AUTO_RELOAD;

    private static final ForgeConfigSpec.IntValue ENERGY_AMMO_RELOAD_CAPACITY;
    private static final ForgeConfigSpec.LongValue ENERGY_AMMO_RELOAD_COST;
    private static final ForgeConfigSpec.IntValue CHARGE_AMMO_MAX;
    private static final ForgeConfigSpec.DoubleValue CHARGE_AMMO_REGEN_PER_TICK;
    private static final ForgeConfigSpec.LongValue CHARGE_AMMO_RELOAD_COST;
    private static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> SPELL_PROVIDER_PRIORITY;
    private static final ForgeConfigSpec.IntValue SPELL_AMMO_RELOAD_CAPACITY;
    private static final ForgeConfigSpec.LongValue SPELL_AMMO_RELOAD_COST;

    private static final ForgeConfigSpec.DoubleValue DEFAULT_HEAT_CAPACITY;
    private static final ForgeConfigSpec.DoubleValue DEFAULT_HEAT_PER_SHOT;
    private static final ForgeConfigSpec.DoubleValue DEFAULT_COOLING_RATE;
    private static final ForgeConfigSpec.DoubleValue OVERHEAT_PENALTY;

    private static final ForgeConfigSpec.DoubleValue PHASE_SHIELD_CAPACITY;
    private static final ForgeConfigSpec.DoubleValue PHASE_SHIELD_REGEN;
    private static final ForgeConfigSpec.DoubleValue PHASE_SHIELD_EFFICIENCY;
    private static final ForgeConfigSpec.BooleanValue PHASE_SHIELD_FE_COMPATIBLE;
    private static final ForgeConfigSpec.DoubleValue SHIELD_DIRECTIONAL_RATIO;
    private static final ForgeConfigSpec.DoubleValue SHIELD_OMNIDIRECTIONAL_RATIO;
    private static final ForgeConfigSpec.DoubleValue SHIELD_FOCUS_RATIO_CAP;
    private static final ForgeConfigSpec.IntValue SHIELD_CLUSTER_MAX_LAYERS;

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("guns");
        PARTICLE_GUN_DAMAGE = BUILDER.comment("Particle-gun minimum charged damage.")
                .defineInRange("particleDamage", 4.0D, 0.0D, 1_000_000.0D);
        PARTICLE_GUN_MAX_DAMAGE = BUILDER.comment("Particle-gun full-charge damage.")
                .defineInRange("particleMaxDamage", 20.0D, 0.0D, 1_000_000.0D);
        PARTICLE_GUN_CHARGE_TICKS = BUILDER.comment("Ticks needed for a fully charged particle-gun shot.")
                .defineInRange("particleChargeTicks", 30, 1, 72_000);
        PARTICLE_GUN_BOLT_SPEED = BUILDER.comment("Base projectile speed multiplier.")
                .defineInRange("particleBoltSpeed", 1.0D, 0.001D, 1_000.0D);
        AUTO_RELOAD = BUILDER.comment("Allow gun facades to attempt automatic reload when their magazine is empty.")
                .define("autoReload", true);
        BUILDER.pop();

        BUILDER.push("energy_ammo");
        ENERGY_AMMO_RELOAD_CAPACITY = BUILDER.comment("Rounds loaded by an energy-ammunition reload.")
                .defineInRange("reloadCapacity", 12, 1, 4_096);
        ENERGY_AMMO_RELOAD_COST = BUILDER.comment("External battery FE consumed by one energy-ammunition reload.",
                        "Guns do not own an FE cache; a future adapter reads batteries through IEnergyStorage.")
                .defineInRange("reloadCost", 2_400L, 0L, Long.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("charge_ammo");
        CHARGE_AMMO_MAX = BUILDER.comment("Maximum internal charge used by charge ammunition.")
                .defineInRange("maxCharge", 100, 0, Integer.MAX_VALUE);
        CHARGE_AMMO_REGEN_PER_TICK = BUILDER.comment("Passive internal-charge regeneration per tick.")
                .defineInRange("regenPerTick", 0.25D, 0.0D, 1_000_000.0D);
        CHARGE_AMMO_RELOAD_COST = BUILDER.comment("Internal charge consumed by one charge-ammunition reload.")
                .defineInRange("reloadCost", 20L, 0L, Long.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("spell_ammo");
        SPELL_PROVIDER_PRIORITY = BUILDER.comment("External spell-resource provider order used for spell ammunition.")
                .defineListAllowEmpty("providerPriority", java.util.List.of("irons_spellbooks", "goety"),
                        entry -> entry instanceof String value && !value.trim().isEmpty());
        SPELL_AMMO_RELOAD_CAPACITY = BUILDER.comment("Rounds loaded by a spell-ammunition reload.")
                .defineInRange("reloadCapacity", 12, 1, 4_096);
        SPELL_AMMO_RELOAD_COST = BUILDER.comment("Spell resource consumed by one spell-ammunition reload.")
                .defineInRange("reloadCost", 50L, 0L, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("heat");
        DEFAULT_HEAT_CAPACITY = BUILDER.comment("Default technology-device heat capacity.")
                .defineInRange("capacity", 100.0D, 0.001D, 1_000_000.0D);
        DEFAULT_HEAT_PER_SHOT = BUILDER.comment("Default heat added by a shot.")
                .defineInRange("perShot", 10.0D, 0.0D, 1_000_000.0D);
        DEFAULT_COOLING_RATE = BUILDER.comment("Default passive cooling per tick.")
                .defineInRange("coolingRate", 0.5D, 0.0D, 1_000_000.0D);
        OVERHEAT_PENALTY = BUILDER.comment("Future overheat recovery-time multiplier.")
                .defineInRange("overheatPenalty", 1.0D, 0.001D, 1_000_000.0D);
        BUILDER.pop();

        BUILDER.push("phase_shield");
        PHASE_SHIELD_CAPACITY = BUILDER.comment("Default phase-shield energy capacity.")
                .defineInRange("capacity", 200.0D, 0.0D, 1_000_000.0D);
        PHASE_SHIELD_REGEN = BUILDER.comment("Default phase-shield regeneration per tick.")
                .defineInRange("regen", 0.4D, 0.0D, 1_000_000.0D);
        PHASE_SHIELD_EFFICIENCY = BUILDER.comment("Default shield energy cost multiplier per absorbed damage.")
                .defineInRange("efficiency", 1.0D, 0.001D, 1_000_000.0D);
        PHASE_SHIELD_FE_COMPATIBLE = BUILDER.comment("Permit a future phase-shield facade to accept FE charging.")
                .define("feCompatible", false);
        SHIELD_DIRECTIONAL_RATIO = BUILDER.comment("Damage prevented by one shield-energy unit for matching directional damage.")
                .defineInRange("directionalRatio", 3.0D, 1.0D, 20.0D);
        SHIELD_OMNIDIRECTIONAL_RATIO = BUILDER.comment("Damage prevented by one shield-energy unit for omnidirectional protection.")
                .defineInRange("omnidirectionalRatio", 1.0D, 1.0D, 20.0D);
        SHIELD_FOCUS_RATIO_CAP = BUILDER.comment("Maximum damage-per-shield-energy ratio allowed after conversion modules.")
                .defineInRange("focusRatioCap", 20.0D, 3.0D, 20.0D);
        SHIELD_CLUSTER_MAX_LAYERS = BUILDER.comment("Maximum shield-generator layers stored in one technology cluster.")
                .defineInRange("clusterMaxLayers", 8, 1, 64);
        BUILDER.pop();

        BUILDER.push("attributes");
        for (TechAttribute attribute : TechAttribute.values()) {
            ATTRIBUTE_BASES.put(attribute, BUILDER.comment("Base value for technology attribute " + attribute.id + ".")
                    .defineInRange(attribute.id, attribute.defaultValue, -1_000_000_000.0D, 1_000_000_000.0D));
        }
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private TechConfig() {
    }

    public static double attributeBase(TechAttribute attribute) {
        ForgeConfigSpec.DoubleValue value = ATTRIBUTE_BASES.get(attribute);
        if (value == null) return attribute.defaultValue;
        try {
            return value.get();
        } catch (IllegalStateException ignored) {
            
            return attribute.defaultValue;
        }
    }

    public static double particleGunDamage() { return PARTICLE_GUN_DAMAGE.get(); }
    public static double particleGunMaxDamage() { return PARTICLE_GUN_MAX_DAMAGE.get(); }
    public static int particleGunChargeTicks() { return PARTICLE_GUN_CHARGE_TICKS.get(); }
    public static double particleGunBoltSpeed() { return PARTICLE_GUN_BOLT_SPEED.get(); }
    public static boolean autoReload() { return AUTO_RELOAD.get(); }
    public static int energyAmmoReloadCapacity() { return ENERGY_AMMO_RELOAD_CAPACITY.get(); }
    public static long energyAmmoReloadCost() { return ENERGY_AMMO_RELOAD_COST.get(); }
    public static int chargeAmmoMax() { return CHARGE_AMMO_MAX.get(); }
    public static double chargeAmmoRegenPerTick() { return CHARGE_AMMO_REGEN_PER_TICK.get(); }
    public static long chargeAmmoReloadCost() { return CHARGE_AMMO_RELOAD_COST.get(); }
    public static int spellAmmoReloadCapacity() { return SPELL_AMMO_RELOAD_CAPACITY.get(); }
    public static long spellAmmoReloadCost() { return SPELL_AMMO_RELOAD_COST.get(); }
    public static double defaultHeatCapacity() { return DEFAULT_HEAT_CAPACITY.get(); }
    public static double defaultHeatPerShot() { return DEFAULT_HEAT_PER_SHOT.get(); }
    public static double defaultCoolingRate() { return DEFAULT_COOLING_RATE.get(); }
    public static double overheatPenalty() { return OVERHEAT_PENALTY.get(); }
    public static double phaseShieldCapacity() { return PHASE_SHIELD_CAPACITY.get(); }
    public static double phaseShieldRegen() { return PHASE_SHIELD_REGEN.get(); }
    public static double phaseShieldEfficiency() { return PHASE_SHIELD_EFFICIENCY.get(); }
    public static boolean phaseShieldFeCompatible() { return PHASE_SHIELD_FE_COMPATIBLE.get(); }
    public static float shieldDirectionalRatio() { return clampRatio(configValue(SHIELD_DIRECTIONAL_RATIO, 3.0D)); }
    public static float shieldOmnidirectionalRatio() { return clampRatio(configValue(SHIELD_OMNIDIRECTIONAL_RATIO, 1.0D)); }
    public static float shieldFocusRatioCap() { return clampRatio(configValue(SHIELD_FOCUS_RATIO_CAP, 20.0D)); }
    public static int shieldClusterMaxLayers() { return Math.max(1, configValue(SHIELD_CLUSTER_MAX_LAYERS, 8)); }

    private static float clampRatio(double value) {
        return (float) Math.max(1.0D, Math.min(shieldFocusRatioCapRaw(), value));
    }

    private static double shieldFocusRatioCapRaw() {
        return Math.max(3.0D, Math.min(20.0D, configValue(SHIELD_FOCUS_RATIO_CAP, 20.0D)));
    }

    private static <T> T configValue(ForgeConfigSpec.ConfigValue<T> value, T fallback) {
        try {
            return value.get();
        } catch (IllegalStateException ignored) {
            return fallback;
        }
    }

    
    public static java.util.List<String> spellProviderPriority() {
        return SPELL_PROVIDER_PRIORITY.get().stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }
}
