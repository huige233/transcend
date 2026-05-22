package com.huige233.transcend.spell;

import com.huige233.transcend.client.renderer.ShaderSpellRenderer;
import com.huige233.transcend.util.EntityCompatUtil;
import com.huige233.transcend.entity.boss.AbstractTranscendBoss;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ElementReaction {

    private static final String TAG_PREFIX = "transcend_element_";
    private static final int MARK_DURATION = 100;

    private static DamageSource getReactionDamageSource(ServerLevel level, LivingEntity target, LivingEntity caster) {
        DamageSource fallback = level.damageSources().indirectMagic(target, caster);
        return EntityCompatUtil.adaptGaiaDamageSource(level, caster, target, fallback);
    }

    public static void markElement(LivingEntity target, SpellElement element) {
        target.getPersistentData().putInt(TAG_PREFIX + element.id, MARK_DURATION);
    }

    public static boolean hasElement(LivingEntity target, String elementId) {
        return target.getPersistentData().getInt(TAG_PREFIX + elementId) > 0;
    }

    public static void clearElement(LivingEntity target, String elementId) {
        target.getPersistentData().remove(TAG_PREFIX + elementId);
    }

    public static void tryReaction(LivingEntity target, SpellElement incoming, float damage, LivingEntity caster) {
        if (!(target.level() instanceof ServerLevel level)) return;

        // ELEMENTAL_MASTERY rune: +50% reaction damage
        if (caster != null && caster.getPersistentData().getInt("transcend_elemental_mastery") > 0) {
            damage *= 1.5F;
        }
        if (caster instanceof AbstractTranscendBoss boss) {
            damage *= switch (boss.getCurrentPhase()) {
                case PHASE_1 -> 1.20F;
                case PHASE_2 -> 1.40F;
                case PHASE_3 -> 1.70F;
                case PHASE_4 -> 2.00F;
            };
        }

        if (incoming == SpellElement.FIRE && hasElement(target, "ice")) {
            steamExplosion(level, target, damage, caster);
            clearElement(target, "ice");
        }
        else if (incoming == SpellElement.ICE && hasElement(target, "fire")) {
            steamExplosion(level, target, damage, caster);
            clearElement(target, "fire");
        }

        else if (incoming == SpellElement.FIRE && hasElement(target, "wind")) {
            firestorm(level, target, damage, caster);
            clearElement(target, "wind");
        }
        else if (incoming == SpellElement.WIND && hasElement(target, "fire")) {
            firestorm(level, target, damage, caster);
            clearElement(target, "fire");
        }

        else if (incoming == SpellElement.THUNDER && hasElement(target, "ice")) {
            superconduct(level, target, damage, caster);
            clearElement(target, "ice");
        }
        else if (incoming == SpellElement.ICE && hasElement(target, "thunder")) {
            superconduct(level, target, damage, caster);
            clearElement(target, "thunder");
        }

        else if (incoming == SpellElement.THUNDER && hasElement(target, "fire")) {
            overload(level, target, damage, caster);
            clearElement(target, "fire");
        }
        else if (incoming == SpellElement.FIRE && hasElement(target, "thunder")) {
            overload(level, target, damage, caster);
            clearElement(target, "thunder");
        }

        else if (incoming == SpellElement.WIND && hasElement(target, "thunder")) {
            electrify(level, target, damage, caster);
            clearElement(target, "thunder");
        }
        else if (incoming == SpellElement.THUNDER && hasElement(target, "wind")) {
            electrify(level, target, damage, caster);
            clearElement(target, "wind");
        }

        else if (incoming == SpellElement.EARTH && hasElement(target, "ice")) {
            permafrost(level, target, damage, caster);
            clearElement(target, "ice");
        }
        else if (incoming == SpellElement.ICE && hasElement(target, "earth")) {
            permafrost(level, target, damage, caster);
            clearElement(target, "earth");
        }

        else if (incoming == SpellElement.VOID && hasElement(target, "holy")) {
            annihilate(level, target, damage, caster);
            clearElement(target, "holy");
        }
        else if (incoming == SpellElement.HOLY && hasElement(target, "void")) {
            annihilate(level, target, damage, caster);
            clearElement(target, "void");
        }

        else if (incoming == SpellElement.POISON && hasElement(target, "fire")) {
            toxicFlame(level, target, damage, caster);
            clearElement(target, "fire");
        }
        else if (incoming == SpellElement.FIRE && hasElement(target, "poison")) {
            toxicFlame(level, target, damage, caster);
            clearElement(target, "poison");
        }

        // Dark + Light → Twilight Burst
        else if (incoming == SpellElement.DARK && hasElement(target, "light")) {
            twilightBurst(level, target, damage, caster);
            clearElement(target, "light");
        }
        else if (incoming == SpellElement.LIGHT && hasElement(target, "dark")) {
            twilightBurst(level, target, damage, caster);
            clearElement(target, "dark");
        }

        // Nature + Poison → Toxic Bloom
        else if (incoming == SpellElement.NATURE && hasElement(target, "poison")) {
            toxicBloom(level, target, damage, caster);
            clearElement(target, "poison");
        }
        else if (incoming == SpellElement.POISON && hasElement(target, "nature")) {
            toxicBloom(level, target, damage, caster);
            clearElement(target, "nature");
        }

        // Time + Space → Dimensional Rift
        else if (incoming == SpellElement.TIME && hasElement(target, "space")) {
            dimensionalRift(level, target, damage, caster);
            clearElement(target, "space");
        }
        else if (incoming == SpellElement.SPACE && hasElement(target, "time")) {
            dimensionalRift(level, target, damage, caster);
            clearElement(target, "time");
        }

        // Blood + Dark → Soul Drain
        else if (incoming == SpellElement.BLOOD && hasElement(target, "dark")) {
            soulDrain(level, target, damage, caster);
            clearElement(target, "dark");
        }
        else if (incoming == SpellElement.DARK && hasElement(target, "blood")) {
            soulDrain(level, target, damage, caster);
            clearElement(target, "blood");
        }

        // Sonic + Thunder → Shockwave
        else if (incoming == SpellElement.SONIC && hasElement(target, "thunder")) {
            shockwave(level, target, damage, caster);
            clearElement(target, "thunder");
        }
        else if (incoming == SpellElement.THUNDER && hasElement(target, "sonic")) {
            shockwave(level, target, damage, caster);
            clearElement(target, "sonic");
        }

        // Chaos + any element → Wild Surge
        else if (incoming == SpellElement.CHAOS) {
            for (SpellElement el : SpellElement.values()) {
                if (el != SpellElement.CHAOS && hasElement(target, el.id)) {
                    wildSurge(level, target, damage, caster);
                    clearElement(target, el.id);
                    break;
                }
            }
        }
        else if (hasElement(target, "chaos")) {
            wildSurge(level, target, damage, caster);
            clearElement(target, "chaos");
        }

        markElement(target, incoming);
    }

    private static void steamExplosion(ServerLevel level, LivingEntity target, float damage, LivingEntity caster) {
        target.hurt(getReactionDamageSource(level, target, caster), damage * 1.5F);
        AABB area = target.getBoundingBox().inflate(4.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != target && e.isAlive())) {
            e.hurt(getReactionDamageSource(level, target, caster), damage * 0.5F);
        }
        spawnReactionShader(target, 3.6F, 0.9F, 0.95F, 1.0F, "hexagram");
        level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.5F);
    }

    private static void firestorm(ServerLevel level, LivingEntity target, float damage, LivingEntity caster) {
        target.hurt(getReactionDamageSource(level, target, caster), damage * 1.2F);
        target.setSecondsOnFire(6);
        AABB area = target.getBoundingBox().inflate(5.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != target && e.isAlive())) {
            e.setSecondsOnFire(4);
            e.hurt(getReactionDamageSource(level, target, caster), damage * 0.4F);
        }
        spawnReactionShader(target, 4.2F, 1.0F, 0.4F, 0.1F, "pentagram");
    }

    private static void superconduct(ServerLevel level, LivingEntity target, float damage, LivingEntity caster) {
        target.hurt(getReactionDamageSource(level, target, caster), damage * 1.3F);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 2));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
        spawnReactionShader(target, 3.0F, 0.5F, 0.85F, 1.0F, "hexagram");
    }

    private static void overload(ServerLevel level, LivingEntity target, float damage, LivingEntity caster) {
        level.explode(null, target.getX(), target.getY(), target.getZ(), 3.0F, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        target.hurt(getReactionDamageSource(level, target, caster), damage * 2.0F);
    }

    private static void electrify(ServerLevel level, LivingEntity target, float damage, LivingEntity caster) {
        target.hurt(getReactionDamageSource(level, target, caster), damage * 1.0F);
        AABB area = target.getBoundingBox().inflate(6.0);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != target && e.isAlive());
        for (LivingEntity e : nearby) {
            e.hurt(getReactionDamageSource(level, target, caster), damage * 0.6F);
        }
        spawnReactionShader(target, 4.8F, 1.0F, 1.0F, 0.4F, "hexagram");
    }

    private static void permafrost(ServerLevel level, LivingEntity target, float damage, LivingEntity caster) {
        target.hurt(getReactionDamageSource(level, target, caster), damage * 1.0F);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4));
        target.setTicksFrozen(300);
        spawnReactionShader(target, 3.4F, 0.75F, 0.9F, 1.0F, "hexagram");
    }

    private static void annihilate(ServerLevel level, LivingEntity target, float damage, LivingEntity caster) {
        target.hurt(getReactionDamageSource(level, target, caster), damage * 3.0F);
        spawnReactionShader(target, 4.0F, 1.0F, 0.95F, 0.65F, "pentagram");
        level.playSound(null, target.blockPosition(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 0.6F, 1.5F);
    }

    private static void toxicFlame(ServerLevel level, LivingEntity target, float damage, LivingEntity caster) {
        target.hurt(getReactionDamageSource(level, target, caster), damage * 1.0F);
        target.setSecondsOnFire(4);
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 2));
        AABB area = target.getBoundingBox().inflate(3.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != target && e.isAlive())) {
            e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
        }
    }

    private static void twilightBurst(ServerLevel level, LivingEntity target, float damage, LivingEntity caster) {
        target.hurt(getReactionDamageSource(level, target, caster), damage * 2.0F);
        AABB area = target.getBoundingBox().inflate(4.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != target && e.isAlive())) {
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
        }
        spawnReactionShader(target, 3.8F, 0.8F, 0.7F, 1.0F, "hexagram");
        level.playSound(null, target.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    private static void toxicBloom(ServerLevel level, LivingEntity target, float damage, LivingEntity caster) {
        target.hurt(getReactionDamageSource(level, target, caster), damage * 1.2F);
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 2));
        AABB area = target.getBoundingBox().inflate(3.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != target && e.isAlive())) {
            e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
        }
        if (caster != null) {
            caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        }
        spawnReactionShader(target, 3.2F, 0.35F, 0.95F, 0.35F, "hexagram");
    }

    private static void dimensionalRift(ServerLevel level, LivingEntity target, float damage, LivingEntity caster) {
        target.hurt(getReactionDamageSource(level, target, caster), damage * 2.5F);
        AABB area = target.getBoundingBox().inflate(5.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive())) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2;
            double dist = 3.0 + level.getRandom().nextDouble() * 5.0;
            e.teleportTo(e.getX() + Math.cos(angle) * dist, e.getY(), e.getZ() + Math.sin(angle) * dist);
        }
        spawnReactionShader(target, 5.0F, 0.55F, 0.35F, 0.95F, "pentagram");
        level.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.5F);
    }

    private static void wildSurge(ServerLevel level, LivingEntity target, float damage, LivingEntity caster) {
        int roll = level.getRandom().nextInt(6);
        switch (roll) {
            case 0 -> {
                target.hurt(getReactionDamageSource(level, target, caster), damage * 3.0F);
                ShaderSpellRenderer.addShockwave(
                        new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ()),
                        2.2F, 1.0F, 0.7F, 0.95F, 12);
            }
            case 1 -> {
                target.setSecondsOnFire(10);
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 3));
            }
            case 2 -> {
                AABB area = target.getBoundingBox().inflate(6.0);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive())) {
                    e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 1));
                }
            }
            case 3 -> {
                target.hurt(getReactionDamageSource(level, target, caster), damage * 2.0F);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 2));
            }
            case 4 -> {
                target.hurt(getReactionDamageSource(level, target, caster), damage * 1.5F);
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
            }
            case 5 -> {
                level.explode(null, target.getX(), target.getY(), target.getZ(), 4.0F, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            }
        }
        spawnReactionShader(target, 4.4F, 0.7F, 0.2F, 0.95F, "pentagram");
    }

    private static void soulDrain(ServerLevel level, LivingEntity target, float damage, LivingEntity caster) {
        float soulDamage = damage * 2.0F;
        target.hurt(getReactionDamageSource(level, target, caster), soulDamage);
        if (caster != null) {
            caster.heal(soulDamage * 0.5F);
        }
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
        spawnReactionShader(target, 3.4F, 0.55F, 0.05F, 0.1F, "hexagram");
        level.playSound(null, target.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.7F, 0.6F);
    }

    private static void shockwave(ServerLevel level, LivingEntity target, float damage, LivingEntity caster) {
        target.hurt(getReactionDamageSource(level, target, caster), damage * 1.8F);
        AABB area = target.getBoundingBox().inflate(5.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != target && e.isAlive())) {
            Vec3 knockDir = e.position().subtract(target.position()).normalize();
            e.knockback(3.0F, -knockDir.x, -knockDir.z);
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
            e.hurt(getReactionDamageSource(level, target, caster), damage * 0.5F);
        }
        spawnReactionShader(target, 5.2F, 0.75F, 0.9F, 1.0F, "hexagram");
        level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    public static void tickMarks(LivingEntity entity) {
        boolean hasAnyMark = false;
        SpellElement strongestMark = null;
        int strongestRemaining = 0;

        for (SpellElement el : SpellElement.values()) {
            String key = TAG_PREFIX + el.id;
            int remaining = entity.getPersistentData().getInt(key);
            if (remaining > 0) {
                entity.getPersistentData().putInt(key, remaining - 1);
                hasAnyMark = true;
                if (remaining > strongestRemaining) {
                    strongestRemaining = remaining;
                    strongestMark = el;
                }
            } else if (remaining == 0 && entity.getPersistentData().contains(key)) {
                entity.getPersistentData().remove(key);
            }
        }

        if (hasAnyMark && strongestMark != null && entity.level() instanceof ServerLevel sl
                && entity.tickCount % 5 == 0) {
            spawnElementAura(sl, entity, strongestMark);
        }

        int mark = entity.getPersistentData().getInt("transcend_mark");
        if (mark > 0) {
            entity.getPersistentData().putInt("transcend_mark", mark - 1);
        } else if (mark == 0 && entity.getPersistentData().contains("transcend_mark")) {
            entity.getPersistentData().remove("transcend_mark");
        }

        int mastery = entity.getPersistentData().getInt("transcend_elemental_mastery");
        if (mastery > 0) {
            entity.getPersistentData().putInt("transcend_elemental_mastery", mastery - 1);
        } else if (mastery == 0 && entity.getPersistentData().contains("transcend_elemental_mastery")) {
            entity.getPersistentData().remove("transcend_elemental_mastery");
        }
    }

    private static void spawnElementAura(ServerLevel level, LivingEntity entity, SpellElement element) {
        if (entity.tickCount % 10 != 0) return;
        Vec3 center = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ());
        ShaderSpellRenderer.addShieldRipple(center, 1.2F + entity.getBbWidth() * 0.6F,
                element.getParticleR(), element.getParticleG(), element.getParticleB(), 12);
    }

    public static void spawnHitFlash(ServerLevel level, LivingEntity target, SpellElement element) {
        Vec3 center = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
        ShaderSpellRenderer.addShockwave(center, 2.2F,
                Math.min(1.0F, element.getParticleR() * 1.25F),
                Math.min(1.0F, element.getParticleG() * 1.25F),
                Math.min(1.0F, element.getParticleB() * 1.25F), 14);
        ShaderSpellRenderer.addShieldRipple(center, 1.5F, element.getParticleR(), element.getParticleG(), element.getParticleB(), 12);
    }

    private static void spawnReactionShader(LivingEntity target, float radius,
                                            float r, float g, float b, String pattern) {
        Vec3 center = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.45, target.getZ());
        ShaderSpellRenderer.addShockwave(center, radius, r, g, b, 20);
        ShaderSpellRenderer.addCircle(center, radius * 0.75F, r, g, b, 16, 30, pattern);
    }
}
