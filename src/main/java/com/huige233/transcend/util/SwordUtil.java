package com.huige233.transcend.util;


import com.huige233.transcend.TranscendDamage;
import com.huige233.transcend.entity.RainbowLightning;
import com.huige233.transcend.mixinitf.ITranscendMarked;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SwordUtil {

    private static final UUID TRANSCEND_HEALTH_CRUSH_UUID = UUID.fromString("c7a1e2b3-4d5f-6a7b-8c9d-0e1f2a3b4c5d");

    public static void annihilate(Entity target, @Nullable Player attacker) {
        TranscendForceKillUtil.forceKill(target, attacker);
    }

    private static void killLiving(LivingEntity living, DamageSource ds) {
        living.invulnerableTime = 0;
        living.setInvulnerable(false);
        living.removeAllEffects();
        living.setAbsorptionAmount(0);
        living.hurt(ds, Float.MAX_VALUE);

        if (living.isAlive()) {
            living.setHealth(0.0F);
            try {
                living.die(ds);
            } catch (Throwable ignored) {}
        }
        if (living.isAlive()) {
            living.remove(Entity.RemovalReason.KILLED);
        }
    }

    private static void killPlayer(ServerPlayer player, DamageSource ds) {
        player.setHealth(0.0F);
        player.getCombatTracker().recordDamage(ds, 0.0F);

        try {
            player.die(ds);
        } catch (Throwable ignored) {}

        player.deathTime = 19;
    }

    private static void killEntity(Entity entity) {
        entity.kill();
    }

    private static void killDragon(EnderDragon dragon, @Nullable Player attacker) {
        Level level = dragon.level();
        boolean wasMarked = false;
        try {
            if (dragon instanceof ITranscendMarked markable) {
                markable.transcend$mark();
                wasMarked = true;
            }

            dragon.setInvulnerable(false);
            dragon.removeAllEffects();

            DamageSource ds = attacker != null
                    ? TranscendDamage.kill(level, attacker)
                    : dragon.damageSources().genericKill();

            dragon.getPhaseManager().setPhase(EnderDragonPhase.DYING);

            dragon.invulnerableTime = 0;
            dragon.hurt(dragon.head, ds, Float.MAX_VALUE);

            dragon.setHealth(0.0F);
            dragon.die(ds);
//            dragon.dragonDeathTime = 200;
        } finally {
            if (wasMarked && dragon instanceof ITranscendMarked markable) {
                markable.transcend$unmark();
            }
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

    private static void clearDefensiveModifiers(LivingEntity living, net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        AttributeInstance instance = living.getAttribute(attribute);
        if (instance != null) {
            new ArrayList<>(instance.getModifiers()).forEach(mod -> instance.removeModifier(mod.getId()));
        }
    }

    public static void kill(Entity target, @Nullable Entity attacker) {
        TranscendForceKillUtil.forceKill(target, attacker);
    }

    public static int killRange(Level level, Player attacker, int range) {
        return TranscendForceKillUtil.forceKillRange(level, attacker, range);
    }

    public static int removeAllEntities(Level level, @Nullable Player attacker) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return 0;
        }

        List<Entity> snapshot = new ArrayList<>();
        serverLevel.getAllEntities().forEach(snapshot::add);

        int count = 0;
        for (Entity entity : snapshot) {
            if (entity == null || entity instanceof Player) {
                continue;
            }
            if (attacker != null && entity == attacker) {
                continue;
            }
            TranscendForceKillUtil.forceKill(entity, attacker);
            count++;
        }
        return count;
    }

    public static void erase(Entity entity) {
        TranscendForceKillUtil.forceRemove(entity, Entity.RemovalReason.KILLED);
    }
}
