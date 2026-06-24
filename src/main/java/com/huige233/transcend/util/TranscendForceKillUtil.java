package com.huige233.transcend.util;

import com.huige233.transcend.TranscendDamage;
import com.huige233.transcend.entity.RainbowLightning;
import com.huige233.transcend.mixinitf.ITranscendMarked;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TranscendForceKillUtil {

    private static final UUID TRANSCEND_HEALTH_CRUSH_UUID = UUID.fromString("c7a1e2b3-4d5f-6a7b-8c9d-0e1f2a3b4c5d");
    private static final Vec3 FORCE_TELEPORT_POS = new Vec3(1.0E9, 0.0, 0.0);

    private TranscendForceKillUtil() {
    }

    public static void forceKill(Entity target, @Nullable Entity attacker) {
        if (target == null || target.level().isClientSide) return;
        doForceKill(target, attacker);
        if (isPersisting(target)) {
            com.huige233.transcend.handle.TranscendRekillScheduler.schedule(target, attacker);
        }
    }

    public static void rekillTick(Entity target, @Nullable Entity attacker) {
        if (target == null || target.level().isClientSide) return;
        doForceKill(target, attacker);
    }

    private static void doForceKill(Entity target, @Nullable Entity attacker) {
        // 真身在静态列表里每 tick 重生傀儡的 boss:唯一不可通用的残留,先处理。
        if (TranscendUnsafeKill.neutralizeStaticRespawnBoss(target)) return;

        if (target instanceof EnderDragonPart part) {
            forceKillDragon(part.parentMob, attacker);
            return;
        }

        if (target instanceof EnderDragon dragon) {
            forceKillDragon(dragon, attacker);
            return;
        }

        if (target instanceof LivingEntity living) {
            forceKillLiving(living, attacker);
            return;
        }

        forceRemove(target, Entity.RemovalReason.KILLED);
    }

    public static boolean isPersisting(Entity entity) {
        return TranscendUnsafeKill.stillPresent(entity);
    }

    public static void forceKillLiving(LivingEntity living, @Nullable Entity attacker) {
        if (living == null || living.level().isClientSide) return;
        boolean wasMarked = markIfNeeded(living);
        try {
            strip(living);
            crushAttributes(living);
            // 通用:把同步血量直接写 0(替代各家 mod 专属 crushHealth)。
            TranscendUnsafeKill.crushSyncedHealth(living);

            DamageSource ds = attacker != null
                    ? TranscendDamage.kill(living.level(), attacker)
                    : living.damageSources().genericKill();
            ds = EntityCompatUtil.adaptGaiaDamageSource(living.level(), attacker, living, ds);

            if (living instanceof ServerPlayer serverPlayer) {
                killPlayer(serverPlayer, ds);
            } else {
                killLivingDirect(living, ds);
            }
        } finally {
            unmarkIfNeeded(living, wasMarked);
        }
    }

    public static int forceKillRange(Level level, Player attacker, int range) {
        AABB aabb = new AABB(
                attacker.getX() - range, attacker.getY() - range, attacker.getZ() - range,
                attacker.getX() + range, attacker.getY() + range, attacker.getZ() + range
        );
        List<Entity> entities = level.getEntities(attacker, aabb, e ->
                !(e instanceof Player) && !(e instanceof ArmorStand) && !(e instanceof RainbowLightning)
        );
        int count = 0;
        for (Entity entity : entities) {
            if (entity.isAlive()) {
                if (level instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 5; i++) {
                        RainbowLightning bolt = new RainbowLightning(serverLevel,
                                entity.getX(), entity.getY(), entity.getZ());
                        serverLevel.addFreshEntity(bolt);
                    }
                }
                forceKill(entity, attacker);
                count++;
            }
        }
        return count;
    }

    public static void forceRemove(Entity entity, Entity.RemovalReason reason) {
        if (entity == null || entity.level().isClientSide) return;
        // 通用 Unsafe 移除:写死同步血量 + removalReason/dead，再走标准 remove/setRemoved/discard。
        TranscendUnsafeKill.forceRemove(entity, reason);
        hardCleanupIfPersisting(entity, reason);
    }

    public static void forceSetRemoved(Entity entity, Entity.RemovalReason reason) {
        forceRemove(entity, reason);
    }

    public static void forceHurt(LivingEntity target, @Nullable Entity attacker, DamageSource source, float amount) {
        if (target == null || target.level().isClientSide) return;
        boolean wasMarked = markIfNeeded(target);
        try {
            DamageSource actualSource = EntityCompatUtil.adaptGaiaDamageSource(target.level(), attacker, target, source);
            target.setInvulnerable(false);
            target.invulnerableTime = 0;
            target.hurtTime = 10;
            target.hurtDuration = 10;
            target.removeAllEffects();
            target.setAbsorptionAmount(0.0F);
            target.hurt(actualSource, amount);
        } finally {
            unmarkIfNeeded(target, wasMarked);
        }
    }

    public static void forceSetHealth(LivingEntity living, float health) {
        if (living == null || living.level().isClientSide) return;
        boolean wasMarked = markIfNeeded(living);
        try {
            living.setHealth(health);
        } finally {
            unmarkIfNeeded(living, wasMarked);
        }
    }

    private static void killLivingDirect(LivingEntity living, DamageSource ds) {
        living.invulnerableTime = 0;
        living.setInvulnerable(false);
        living.removeAllEffects();
        living.setAbsorptionAmount(0);
        living.hurt(ds, Float.MAX_VALUE);

        if (living.isAlive()) {
            living.setHealth(0.0F);
            try {
                living.die(ds);
            } catch (Throwable ignored) {
            }
        }
        forceRemove(living, Entity.RemovalReason.KILLED);
        living.discard();
    }

    private static void hardCleanupIfPersisting(Entity entity, Entity.RemovalReason reason) {
        if (!TranscendUnsafeKill.stillPresent(entity)) return;

        entity.setPos(FORCE_TELEPORT_POS.x, FORCE_TELEPORT_POS.y, FORCE_TELEPORT_POS.z);

        if (entity instanceof LivingEntity living) {
            try {
                living.die(living.damageSources().genericKill());
            } catch (Throwable ignored) {
            }
        }

        TranscendUnsafeKill.forceRemove(entity, reason);
    }

    private static void killPlayer(ServerPlayer player, DamageSource ds) {
        player.setHealth(0.0F);
        player.getCombatTracker().recordDamage(ds, 0.0F);
        try {
            player.die(ds);
        } catch (Throwable ignored) {
        }
        player.deathTime = 19;
        player.discard();
    }

    private static void forceKillDragon(EnderDragon dragon, @Nullable Entity attacker) {
        if (dragon == null || dragon.level().isClientSide) return;
        boolean wasMarked = markIfNeeded(dragon);
        try {
            dragon.setInvulnerable(false);
            dragon.removeAllEffects();
            DamageSource ds = attacker != null
                    ? TranscendDamage.kill(dragon.level(), attacker)
                    : dragon.damageSources().genericKill();
            dragon.getPhaseManager().setPhase(EnderDragonPhase.DYING);
            dragon.invulnerableTime = 0;
            dragon.hurt(dragon.head, ds, Float.MAX_VALUE);
            dragon.setHealth(0.0F);
            dragon.die(ds);
            dragon.discard();
        } finally {
            unmarkIfNeeded(dragon, wasMarked);
        }
    }

    private static void strip(LivingEntity living) {
        living.setInvulnerable(false);
        living.stopUsingItem();
        living.clearFire();
        living.removeAllEffects();
        living.setAbsorptionAmount(0.0F);
        living.invulnerableTime = 0;
        living.hurtTime = 0;
    }

    private static void crushAttributes(LivingEntity living) {
        AttributeInstance maxHealth = living.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(TRANSCEND_HEALTH_CRUSH_UUID);
            maxHealth.addTransientModifier(new AttributeModifier(
                    TRANSCEND_HEALTH_CRUSH_UUID, "transcend_kill",
                    -1.01, AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }

        clearDefensiveModifiers(living, Attributes.ARMOR);
        clearDefensiveModifiers(living, Attributes.ARMOR_TOUGHNESS);
    }

    private static void clearDefensiveModifiers(LivingEntity living, Attribute attribute) {
        AttributeInstance instance = living.getAttribute(attribute);
        if (instance != null) {
            new ArrayList<>(instance.getModifiers()).forEach(mod -> instance.removeModifier(mod.getId()));
        }
    }

    private static boolean markIfNeeded(LivingEntity living) {
        if (living instanceof ITranscendMarked marked) {
            marked.transcend$mark();
            return true;
        }
        return false;
    }

    private static void unmarkIfNeeded(LivingEntity living, boolean wasMarked) {
        if (wasMarked && living instanceof ITranscendMarked marked) {
            marked.transcend$unmark();
        }
    }
}
