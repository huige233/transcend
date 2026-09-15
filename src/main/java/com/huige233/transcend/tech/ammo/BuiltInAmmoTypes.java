package com.huige233.transcend.tech.ammo;

import com.huige233.transcend.tech.TechConfig;
import com.huige233.transcend.tech.attribute.AttributeContainer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;


/** 集中定义并按标识查询蓄能、能量和法术三种内置弹药及其默认参数。 */
public final class BuiltInAmmoTypes {
    public static final String CHARGE_ID = "charge";
    public static final String ENERGY_ID = "energy";
    public static final String SPELL_ID = "spell";

    private static final Map<String, AmmoType> TYPES = new LinkedHashMap<>();

    static {
        register(new BasicAmmoType(CHARGE_ID, Component.translatable("ammo.transcend.charge"),
                LegacyChargeAmmoResource.INSTANCE, () -> 1, TechConfig::chargeAmmoReloadCost,
                0xFF00E5FF, 1.0F, 1.0F, BoltBehavior.direct()));
        register(new BasicAmmoType(ENERGY_ID, Component.translatable("ammo.transcend.energy"),
                ExternalFeAmmoResource.INSTANCE, TechConfig::energyAmmoReloadCapacity, TechConfig::energyAmmoReloadCost,
                0xFF7EEBFF, 1.15F, 1.35F, new BoltBehavior(1, 0.0F, false)));
        register(new BasicAmmoType(SPELL_ID, Component.translatable("ammo.transcend.spell"),
                SpellAmmoResource.INSTANCE, TechConfig::spellAmmoReloadCapacity, TechConfig::spellAmmoReloadCost,
                0xFFB388FF, 1.25F, 0.90F, new BoltBehavior(0, 2.5F, false)));
    }

    private BuiltInAmmoTypes() {
    }

    private static void register(AmmoType type) {
        TYPES.put(type.id(), type);
    }

    @Nullable
    public static AmmoType byId(@Nullable String id) {
        return id == null ? null : TYPES.get(id);
    }

    public static AmmoType charge() {
        return TYPES.get(CHARGE_ID);
    }

    public static int penetrationStrength(String ammoId) {
        return "energy".equals(ammoId) ? 1 : 0;
    }

    /** 将内置弹药参数实现为弹种接口，并合并科技属性提供弹体穿透与溅射行为。 */
    private record BasicAmmoType(String id, Component name, AmmoResource resource,
                                 java.util.function.IntSupplier capacity,
                                 java.util.function.LongSupplier cost, int color,
                                 float damageMultiplier, float speedMultiplier,
                                 BoltBehavior baseBehavior) implements AmmoType {
        @Override public String displayName() { return name.getString(); }
        @Override public DamageSource createDamageSource(Level level, @Nullable Entity owner) {
            return level.damageSources().magic();
        }
        @Override public BoltVisual visual() {
            return new BoltVisual(Component.translatable("ammo.transcend." + id), color,
                    ParticleTypes.END_ROD, id.equals(SPELL_ID) ? 1.2F : 1.0F, true);
        }
        @Override public BoltBehavior behavior(AttributeContainer attrs) {
            int pierce = Math.max(baseBehavior.pierce, (int) Math.round(attrs.getValue(
                    com.huige233.transcend.tech.attribute.TechAttribute.PIERCE_COUNT)));
            float splash = Math.max(baseBehavior.splashRadius, (float) attrs.getValue(
                    com.huige233.transcend.tech.attribute.TechAttribute.SPLASH_RADIUS));
            return new BoltBehavior(pierce, splash, baseBehavior.explode);
        }
        @Override public long reloadCost() { return cost.getAsLong(); }
        @Override public int reloadCapacity() { return capacity.getAsInt(); }
    }
}
