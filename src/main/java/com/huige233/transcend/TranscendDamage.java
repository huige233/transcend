package com.huige233.transcend;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class TranscendDamage {

    public static DamageSource kill(Level level, @Nullable Entity attacker) {
        Holder<DamageType> type = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ResourceKey.create(
                        Registries.DAMAGE_TYPE,
                        new ResourceLocation("transcend", "transcend_kill")
                ));

        return attacker == null
                ? new DamageSource(type)
                : new DamageSource(type, attacker);
    }
}
