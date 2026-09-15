package com.huige233.transcend.tech.combat;

import com.huige233.transcend.entity.projectile.ParticleBolt;
import com.huige233.transcend.mixinitf.ITranscendMarked;
import com.huige233.transcend.util.TranscendGuard;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

   
                                                                                             
                                                                                                   
                                                                                                  
   
/** 按精确实体类型注册首领防御适配器，并在目标保护校验后计算允许附加的百分比伤害。 */
public final class BossDefenseRegistry {
    private static final Map<ResourceLocation, BossDefenseAdapter> ADAPTERS = new ConcurrentHashMap<>();

    private BossDefenseRegistry() {}

    public static void register(ResourceLocation entityType, BossDefenseAdapter adapter) {
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(adapter, "adapter");
        if (adapter.defenseTier() < 1) throw new IllegalArgumentException("Boss defense tier must be positive");
        if (ADAPTERS.putIfAbsent(entityType, adapter) != null) {
            throw new IllegalArgumentException("Boss adapter already registered for " + entityType);
        }
    }

    static BossDefenseAdapter adapter(ResourceLocation entityType) {
        return entityType == null ? null : ADAPTERS.get(entityType);
    }

    
    public static boolean allowsPercentDamage(LivingEntity target, DamageSource source) {
        if (target == null || target.level().isClientSide || target instanceof Player || !target.isAlive()
                || source == null || target.isInvulnerable() || target.isInvulnerableTo(source)) return false;
        BossDefenseAdapter adapter = adapter(ForgeRegistries.ENTITY_TYPES.getKey(target.getType()));
        return adapter != null && adapter.allowsPercentDamage(target, source);
    }

    public static float percentDamage(LivingEntity target, DamageSource source, PenetrationProfile profile) {
        if (target.level().isClientSide || target instanceof Player || !target.isAlive()
                || !(source.getDirectEntity() instanceof ParticleBolt)
                || profile == null || !profile.hasDirectPercentDamage()
                || target.isInvulnerable() || target.isInvulnerableTo(source)
                || target instanceof ITranscendMarked marked && marked.transcend$isMarked()
                || TranscendGuard.isProtected(target)) return 0.0F;
        BossDefenseAdapter adapter = adapter(ForgeRegistries.ENTITY_TYPES.getKey(target.getType()));
        if (adapter == null || !adapter.allowsPercentDamage(target, source)) return 0.0F;
        return PenetrationPolicy.directPercentDamage(profile, adapter.defenseTier(), target.getMaxHealth());
    }
}
