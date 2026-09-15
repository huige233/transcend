package com.mega.uom.event.eventhandler.common;

import com.mega.uom.event.projectile.ArrowHitEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;

public class CommonHooks {
    public static boolean onProjectileHitBefore(Projectile projectile, HitResult result) {
        ArrowHitEvent event;
        event = new ArrowHitEvent.Pre(projectile, result);
        return MinecraftForge.EVENT_BUS.post(event);
    }

    public static boolean onProjectileHitDamage(Projectile projectile, HitResult result, float damage, DamageSource source) {
        ArrowHitEvent event;
        event = new ArrowHitEvent.Damage(projectile, result, damage, source);
        return MinecraftForge.EVENT_BUS.post(event);
    }

    public static void onProjectileHitAfter(Projectile projectile, HitResult result, float damage, DamageSource source) {
        MinecraftForge.EVENT_BUS.post(new ArrowHitEvent.Post(projectile, result, damage, source));
    }
}
