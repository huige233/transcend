package com.mega.uom.event.projectile;

import com.mega.uom.event.eventhandler.common.CommonHooks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.<br>
 * This event is fired when an abstract arrow entity hit on entity/block.<br>
 * This event is fired via
 * {@link CommonHooks#onProjectileHitBefore(Projectile, HitResult)}
 * {@link CommonHooks#onProjectileHitDamage(Projectile, HitResult, float, DamageSource) (only hit on entity)}
 * {@link CommonHooks#onProjectileHitAfter(Projectile, HitResult, float, DamageSource)}
 * This event is fired for all abstract arrow entities
 */
public abstract class ArrowHitEvent extends EntityEvent {
    private final HitResult ray;
    private final Projectile projectile;

    public ArrowHitEvent(Projectile projectile, HitResult ray) {
        super(projectile);
        this.ray = ray;
        this.projectile = projectile;
    }

    public HitResult getRayTraceResult() {
        return ray;
    }

    public Projectile getProjectile() {
        return projectile;
    }

    public abstract Progress getProgress();

    public enum Progress {
        BEFORE,
        AFTER,
        DAMAGE
    }

    @Cancelable
    public static class Pre extends ArrowHitEvent {

        public Pre(Projectile projectile, HitResult ray) {
            super(projectile, ray);
        }

        @Override
        public Progress getProgress() {
            return Progress.BEFORE;
        }
    }

    @Cancelable
    public static class Damage extends ArrowHitEvent {
        protected float damage;
        protected DamageSource arrowSource;

        public Damage(Projectile projectile, HitResult ray, float damage, DamageSource source) {
            super(projectile, ray);
            this.damage = damage;
            this.arrowSource = source;
        }

        public float getDamage() {
            return damage;
        }

        public DamageSource getArrowSource() {
            return arrowSource;
        }

        @Override
        public Progress getProgress() {
            return Progress.AFTER;
        }
    }

    public static class Post extends ArrowHitEvent {
        protected float damage;
        protected DamageSource arrowSource;

        public Post(Projectile projectile, HitResult ray, float damage, DamageSource source) {
            super(projectile, ray);
            this.damage = damage;
            this.arrowSource = source;
        }

        public float getDamage() {
            return damage;
        }

        public DamageSource getArrowSource() {
            return arrowSource;
        }

        @Override
        public Progress getProgress() {
            return Progress.AFTER;
        }
    }
}
