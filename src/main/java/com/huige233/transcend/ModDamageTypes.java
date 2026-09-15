package com.huige233.transcend;

import com.huige233.transcend.combat.attack.AttackLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;


/** 维护模组伤害类型的注册键，并将非移除类攻击等级映射到对应伤害类型。 */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> TRANSCEND = key("transcend");
    public static final ResourceKey<DamageType> TRANSCEND_KILL = key("transcend_kill");
    private static final Map<AttackLevel, ResourceKey<DamageType>> ATTACK_TYPES = new EnumMap<>(AttackLevel.class);
    static {
        for (AttackLevel level : AttackLevel.values()) {
            if (level != AttackLevel.HARD_DELETE && level != AttackLevel.WORLD_PURGE) {
                ATTACK_TYPES.put(level, key(level.name().toLowerCase(Locale.ROOT)));
            }
        }
    }
    private ModDamageTypes() {}
    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(Transcend.MODID, path));
    }
    public static ResourceKey<DamageType> attack(AttackLevel level) {
        ResourceKey<DamageType> key = ATTACK_TYPES.get(level);
        if (key == null) throw new IllegalArgumentException("Removal is not damage: " + level);
        return key;
    }
}
