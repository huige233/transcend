package com.huige233.transcend.combat.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;


/** 依据伤害类型与标签生成伤害类别及各项防御绕过标志的不可变快照。 */
public record DamageClassification(int categoryIndex, boolean bypassesArmor, boolean bypassesResistance,
                                   boolean bypassesEffects, boolean bypassesInvulnerability,
                                   boolean bypassesShield, boolean voidDamage, boolean commandDamage) {
    public static final int UNKNOWN = -1, MELEE = 0, PROJECTILE = 1, EXPLOSION = 2,
            FIRE = 3, MAGIC = 4, ENVIRONMENT = 5;
    public static final TagKey<DamageType> IS_MAGIC = tag("is_magic");
    public static final TagKey<DamageType> IS_ENVIRONMENT = tag("is_environment");
    public static final TagKey<DamageType> IS_MELEE = tag("is_melee");

    private static TagKey<DamageType> tag(String name) {
        return TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("transcend", name));
    }

    public static DamageClassification snapshot(DamageSource source) {
        boolean voidDamage = source.is(DamageTypes.FELL_OUT_OF_WORLD);
        boolean commandDamage = source.is(DamageTypes.GENERIC_KILL);
        int category;
        
        
        if (source.is(DamageTypeTags.IS_EXPLOSION)) category = EXPLOSION;
        else if (source.is(DamageTypeTags.IS_FIRE)) category = FIRE;
        else if (source.is(IS_MAGIC) || source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.DRAGON_BREATH)) category = MAGIC;
        else if (source.is(IS_ENVIRONMENT) || source.is(DamageTypeTags.IS_FALL)
                || source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypeTags.IS_FREEZING)
                || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING)
                || source.is(DamageTypes.STARVE) || source.is(DamageTypes.CACTUS)
                || source.is(DamageTypes.SWEET_BERRY_BUSH) || source.is(DamageTypes.LIGHTNING_BOLT)
                || voidDamage || commandDamage) category = ENVIRONMENT;
        else if (source.is(DamageTypeTags.IS_PROJECTILE)) category = PROJECTILE;
        else if (source.is(IS_MELEE) || source.is(DamageTypes.PLAYER_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)) category = MELEE;
        else category = UNKNOWN;
        return new DamageClassification(category, source.is(DamageTypeTags.BYPASSES_ARMOR),
                source.is(DamageTypeTags.BYPASSES_RESISTANCE), source.is(DamageTypeTags.BYPASSES_EFFECTS),
                source.is(DamageTypeTags.BYPASSES_INVULNERABILITY), source.is(DamageTypeTags.BYPASSES_SHIELD),
                voidDamage, commandDamage);
    }
}
