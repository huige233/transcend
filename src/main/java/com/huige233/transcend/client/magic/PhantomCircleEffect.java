package com.huige233.transcend.client.magic;

import com.huige233.transcend.network.S2CParticleBatchPack;
import com.huige233.transcend.network.S2CVanillaParticleBatchPack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PhantomCircleEffect extends AbstractMagicCircle {

    private static final double EFFECT_RADIUS = 6.0;

    public PhantomCircleEffect(ServerLevel level, Vec3 center) {
        super(level, center);
    }

    @Override
    protected void tickParticles(float scaleFactor) {
        double cx = center.x, cy = center.y, cz = center.z;
        double rotSpeed = Math.PI / 55.0;
        double rotation = age * rotSpeed;
        double counterRotation = -age * rotSpeed * 0.9;
        int particleLifetime = 6;

        float ghostR = 0.5F, ghostG = 0.55F, ghostB = 0.7F;
        float shadowR = 0.2F, shadowG = 0.2F, shadowB = 0.35F;

        // Fade factor based on sin(age) — ethereal pulsing
        float fadeFactor = 0.5F + 0.5F * (float) Math.sin(age * 0.15);

        // Outer ring — translucent gray-blue, 60pts, tiny particles (scale 0.4)
        {
            double radius = 6.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 60, rotation, UP);
            sendBatch(entries, ghostR * fadeFactor, ghostG * fadeFactor, ghostB * fadeFactor,
                    0.4F, particleLifetime);
        }

        // Polygon(8) at radius 4.5 — fading in/out with sin(age * 0.15)
        {
            float polyAlpha = (float) Math.max(0.0, Math.sin(age * 0.15));
            if (polyAlpha > 0.1F) {
                double radius = 4.5 * scaleFactor;
                List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildPolygon(
                        cx, cy, cz, radius, 8, 8, counterRotation, UP);
                sendBatch(entries, ghostR * polyAlpha, ghostG * polyAlpha, ghostB * polyAlpha,
                        0.35F * polyAlpha, particleLifetime);
            }
        }

        // 3 ghost rings at different Y-heights with alternating visibility
        {
            double[] yOffsets = {0.0, 0.5, 1.0};
            for (int ring = 0; ring < 3; ring++) {
                // Alternating visibility — each ring visible on different tick phases
                float ringAlpha = (float) Math.max(0.0,
                        Math.sin(age * 0.12 + ring * Math.PI * 2 / 3));
                if (ringAlpha < 0.15F) continue;

                double radius = (4.0 - ring * 0.8) * scaleFactor;
                int points = 35 - ring * 5;
                double yOff = yOffsets[ring];
                List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                        cx, cy + yOff, cz, radius, points,
                        rotation * (ring % 2 == 0 ? 1.0 : -1.0), UP);
                sendBatch(entries, shadowR + ghostR * ringAlpha * 0.3F,
                        shadowG + ghostG * ringAlpha * 0.3F,
                        shadowB + ghostB * ringAlpha * 0.3F,
                        0.35F * ringAlpha, particleLifetime);
            }
        }

        // Shadow tendrils: 6 lines from center outward with slight random drift
        if (age >= 6) {
            Random rng = new Random(age / 4);
            for (int i = 0; i < 6; i++) {
                double angle = rotation * 0.6 + 2.0 * Math.PI * i / 6;
                double outerDist = (4.5 + rng.nextDouble() * 1.0) * scaleFactor;
                double ox = cx + outerDist * Math.cos(angle + rng.nextDouble() * 0.15);
                double oz = cz + outerDist * Math.sin(angle + rng.nextDouble() * 0.15);
                List<S2CParticleBatchPack.ParticleEntry> tendril = MagicCircleGeometry.buildLine(
                        cx, cy, cz, ox, cy + rng.nextDouble() * 0.3, oz, 8);
                sendBatch(tendril, shadowR, shadowG, shadowB, 0.3F, particleLifetime);
            }
        }

        // Dotted circles that appear and disappear
        if (age >= 5) {
            float dottedAlpha = (float) Math.max(0.0, Math.sin(age * 0.1 + 1.0));
            if (dottedAlpha > 0.2F) {
                double dottedRadius = 5.5 * scaleFactor;
                List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildDottedCircle(
                        cx, cy, cz, dottedRadius, 8, 5, 0.4, counterRotation * 0.5, UP);
                sendBatch(entries, ghostR * dottedAlpha, ghostG * dottedAlpha, ghostB * dottedAlpha,
                        0.3F * dottedAlpha, particleLifetime);
            }
        }

        // End rod particles every 3 ticks — 4 pts, very slow random drift
        if (age % 3 == 0) {
            Random rng = new Random();
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> endRods = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 4.5 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                double vx = (rng.nextDouble() - 0.5) * 0.01;
                double vy = (rng.nextDouble() - 0.5) * 0.01;
                double vz = (rng.nextDouble() - 0.5) * 0.01;
                endRods.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.1 + rng.nextDouble() * 0.6, pz, vx, vy, vz));
            }
            sendVanillaBatch(endRods, "end_rod");
        }

        // Smoke vanilla particles every 4 ticks — 3 pts
        if (age % 4 == 0) {
            Random rng = new Random();
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> smokeEntries = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 3.5 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                smokeEntries.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.05 + rng.nextDouble() * 0.2, pz, 0, 0.005, 0));
            }
            sendVanillaBatch(smokeEntries, "smoke");
        }

        // Ethereal pulsing core — very dim
        {
            float pulseScale = 0.2F + 0.15F * (float) Math.sin(age * 0.3);
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy + 0.1, cz, 0.4 * scaleFactor, 8, age * 0.4, UP);
            sendBatch(entries, ghostR, ghostG, ghostB, pulseScale + 0.3F, particleLifetime);
        }

        // Sound — phantom ambient every 30 ticks
        if (age % 30 == 0) {
            level.playSound(null, BlockPos.containing(center), SoundEvents.PHANTOM_AMBIENT,
                    SoundSource.BLOCKS, 0.2F, 1.2F);
        }
    }

    @Override
    protected void tickEffect(float scaleFactor) {
        if (age < 10) return;

        double r = EFFECT_RADIUS;
        AABB area = new AABB(center.x - r, center.y - 2, center.z - r,
                center.x + r, center.y + 4, center.z + r);

        // Mob effects
        List<Mob> mobs = getMobsInRadius(Mob.class, EFFECT_RADIUS);

        for (Mob mob : mobs) {
            // Blindness I for 40 ticks every 20 ticks
            if (age % 20 == 0) {
                mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, Math.min(specialLevel / 2, 2)));
            }

            // Magic damage every 15 ticks
            if (age % 15 == 0) {
                hurtCompat(mob, level.damageSources().magic(), 1.0F * powerMultiplier);
            }

            // Darkness for 60 ticks every 30 ticks
            if (age % 30 == 0) {
                mob.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
            }
        }

        // Invisibility to nearest player every 40 ticks for 60 ticks
        if (age % 40 == 0) {
            List<Player> players = level.getEntitiesOfClass(Player.class, area, player -> {
                if (!player.isAlive()) return false;
                double dx = player.getX() - center.x;
                double dz = player.getZ() - center.z;
                return Math.sqrt(dx * dx + dz * dz) <= EFFECT_RADIUS;
            });

            if (!players.isEmpty()) {
                Player nearest = null;
                double nearestDist = Double.MAX_VALUE;
                for (Player player : players) {
                    double dx = player.getX() - center.x;
                    double dz = player.getZ() - center.z;
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = player;
                    }
                }
                if (nearest != null) {
                    nearest.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0));
                }
            }
        }
    }

    @Override
    protected void onRemove() {
    }
}
