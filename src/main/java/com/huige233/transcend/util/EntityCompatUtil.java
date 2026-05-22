package com.huige233.transcend.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

public final class EntityCompatUtil {

    private static final String GOETY_MODID = "goety";
    private static final String BOTANIA_MODID = "botania";
    private static final String GOETY_OBSIDIAN_MONOLITH = "obsidian_monolith";
    private static final String BOTANIA_GAIA_GUARDIAN = "doppleganger";

    private EntityCompatUtil() {
    }

    public static boolean isGoetyObsidianMonolith(Entity entity) {
        return entity != null
                && ModList.get().isLoaded(GOETY_MODID)
                && isEntityId(entity, GOETY_MODID, GOETY_OBSIDIAN_MONOLITH);
    }

    public static boolean isBotaniaGaiaGuardian(Entity entity) {
        return entity != null
                && ModList.get().isLoaded(BOTANIA_MODID)
                && isEntityId(entity, BOTANIA_MODID, BOTANIA_GAIA_GUARDIAN);
    }

    public static boolean isProtectedPlayer(Entity entity) {
        return entity instanceof Player player && (player.isCreative() || player.isSpectator());
    }

    public static Player findNearestValidPlayer(Level level, Entity center, double radius) {
        if (level == null || center == null) return null;
        // Real player only: Gaia and similar bosses reject creative/spectator sources.
        return level.getNearestPlayer(center.getX(), center.getY(), center.getZ(), radius,
                p -> p != null && p.isAlive() && !isProtectedPlayer(p));
    }

    public static DamageSource adaptGaiaDamageSource(Level level, Entity sourceEntity,
                                                     LivingEntity target, DamageSource fallback) {
        if (!isBotaniaGaiaGuardian(target)) {
            return fallback;
        }
        if (sourceEntity instanceof Player player && player.isAlive() && !isProtectedPlayer(player)) {
            return player.damageSources().playerAttack(player);
        }
        Player nearest = findNearestValidPlayer(level, target, 64.0);
        if (nearest != null) {
            return nearest.damageSources().playerAttack(nearest);
        }
        return fallback;
    }

    private static boolean isEntityId(Entity entity, String namespace, String path) {
        ResourceLocation id = EntityType.getKey(entity.getType());
        return id != null && namespace.equals(id.getNamespace()) && path.equals(id.getPath());
    }
}
