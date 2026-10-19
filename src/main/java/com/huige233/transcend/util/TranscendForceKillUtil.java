package com.huige233.transcend.util;

import com.huige233.transcend.TranscendDamage;
import com.huige233.transcend.combat.ReentryGuard;
import com.huige233.transcend.combat.attack.HealthAccess;
import com.huige233.transcend.combat.attack.TranscendHealthLedger;
import com.huige233.transcend.combat.attack.TranscendKillSettlement;
import com.huige233.transcend.entity.RainbowLightning;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/** 按玩家、生物和末影龙分别执行防御剥离、生命压制及强制移除，并为残留目标安排重复击杀。 */
public final class TranscendForceKillUtil {

    private static final UUID TRANSCEND_HEALTH_CRUSH_UUID = UUID.fromString("c7a1e2b3-4d5f-6a7b-8c9d-0e1f2a3b4c5d");
    private static final Vec3 FORCE_TELEPORT_POS = new Vec3(1.0E9, 0.0, 0.0);
    private static final ReentryGuard<Entity> KILLS = new ReentryGuard<>(8);
    private static final ReentryGuard<Entity> REMOVALS = new ReentryGuard<>(8);

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

    public static boolean isKilling(Entity entity) {
        return KILLS.isActive(entity);
    }

    public static void forceKillLiving(LivingEntity living, @Nullable Entity attacker) {
        if (living == null || living.level().isClientSide) return;
        KILLS.run(living, () -> killLiving(living, attacker));
    }

    private static void killLiving(LivingEntity living, @Nullable Entity attacker) {
        DamageSource ds = attacker != null
                ? TranscendDamage.kill(living.level(), attacker)
                : living.damageSources().genericKill();
        TranscendKillSettlement.prepare(living, ds, attacker);
        strip(living);
        crushAttributes(living);
        HealthAccess.write(living, 0.0F);
        TranscendHealthLedger.forceSet(living, 0.0F);
        TranscendUnsafeKill.crushSyncedHealth(living, 0.0F);
        TranscendHealthLedger.converge(living, 0.0F);
        if (living instanceof ServerPlayer serverPlayer) {
            killPlayer(serverPlayer, ds);
        } else {
            killLivingDirect(living, ds);
        }
        HealthAccess.write(living, 0.0F);
        TranscendHealthLedger.forceSet(living, 0.0F);
        TranscendHealthLedger.converge(living, 0.0F);
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
        REMOVALS.run(entity, () -> {
            TranscendUnsafeKill.forceRemove(entity, reason);
            TranscendEntityPurge.purgeFromLevel(entity, true);
            hardCleanupIfPersisting(entity, reason);
        });
    }

    private static void killLivingDirect(LivingEntity living, DamageSource ds) {
        living.invulnerableTime = 0;
        living.setInvulnerable(false);
        living.removeAllEffects();
        living.setAbsorptionAmount(0);

        try {
            living.hurt(ds, Float.MAX_VALUE);
        } catch (Throwable ignored) {
            try {
                living.hurt(ds, Float.MAX_VALUE);
            } catch (Throwable ignored2) {
            }
        }
        TranscendUnsafeKill.catchSetTrueHealth(living, 0.0F);

        try {
            living.gameEvent(GameEvent.ENTITY_DIE);
        } catch (Throwable ignored) {
        }

        if (!TranscendKillSettlement.hasSettledLoot(living)) {
            try {
                living.setHealth(0.0F);
            } catch (Throwable ignored) {
            }
            try {
                living.die(ds);
            } catch (Throwable ignored) {
            }
        }

        TranscendKillSettlement.finishMob(living, ds);

        TranscendUnsafeKill.deleteStyleWipe(living);
        forceRemove(living, Entity.RemovalReason.KILLED);
        try {
            living.discard();
        } catch (Throwable ignored) {
        }
    }

    private static void hardCleanupIfPersisting(Entity entity, Entity.RemovalReason reason) {
        if (!TranscendUnsafeKill.stillPresent(entity)) return;

        try {
            entity.setPos(FORCE_TELEPORT_POS.x, FORCE_TELEPORT_POS.y, FORCE_TELEPORT_POS.z);
            entity.setOldPosAndRot();
        } catch (Throwable ignored) {
        }

        if (entity instanceof LivingEntity living) {
            try {
                living.die(living.damageSources().genericKill());
            } catch (Throwable ignored) {
            }
        }

        TranscendUnsafeKill.forceRemove(entity, reason);
        TranscendEntityPurge.purgeFromLevel(entity, false);
        TranscendEntityPurge.clearBossBars(entity);
    }

    private static void killPlayer(ServerPlayer player, DamageSource ds) {
        TranscendUnsafeKill.catchSetTrueHealth(player, 0.0F);
        try {
            player.hurt(ds, Float.MAX_VALUE);
        } catch (Throwable ignored) {
        }
        try {
            player.setHealth(0.0F);
        } catch (Throwable ignored) {
        }
        try {
            player.die(ds);
        } catch (Throwable ignored) {
        }
        player.deathTime = 19;
        try {
            player.gameEvent(GameEvent.ENTITY_DIE);
        } catch (Throwable ignored) {
        }
        try {
            player.discard();
        } catch (Throwable ignored) {
        }
    }

    private static void forceKillDragon(EnderDragon dragon, @Nullable Entity attacker) {
        if (dragon == null || dragon.level().isClientSide) return;
        KILLS.run(dragon, () -> killDragon(dragon, attacker));
    }

    private static void killDragon(EnderDragon dragon, @Nullable Entity attacker) {
        DamageSource ds = attacker != null
                ? TranscendDamage.kill(dragon.level(), attacker)
                : dragon.damageSources().genericKill();
        TranscendKillSettlement.prepare(dragon, ds, attacker);
        dragon.setInvulnerable(false);
        dragon.removeAllEffects();
        dragon.getPhaseManager().setPhase(EnderDragonPhase.DYING);
        dragon.invulnerableTime = 0;
        dragon.hurt(dragon.head, ds, Float.MAX_VALUE);
        dragon.setHealth(0.0F);
        dragon.die(ds);
        // 末影龙保留原版独立战斗/经验流程，不补用普通生物的掉落方法。
        dragon.discard();
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

}
