package com.huige233.transcend.client.magic;

import com.huige233.transcend.network.S2CParticleBatchPack;
import com.huige233.transcend.network.S2CRuneBatchPack;
import com.huige233.transcend.network.S2CVanillaParticleBatchPack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class InfernoCircleEffect extends AbstractMagicCircle {

    private static final double EFFECT_RADIUS = 6.0;

    public InfernoCircleEffect(ServerLevel level, Vec3 center) {
        super(level, center);
    }

    @Override
    protected void tickParticles(float scaleFactor) {
        double cx = center.x, cy = center.y, cz = center.z;
        double rotSpeed = Math.PI / 40.0;
        double rotation = age * rotSpeed;
        double counterRotation = -age * rotSpeed * 1.3;
        int particleLifetime = 4;

        float fireR = 1.0F, fireG = 0.4F, fireB = 0.1F;

        {
            double radius = 6.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 100, rotation, UP);
            sendBatch(entries, baseR, baseG, baseB, 0.8F, particleLifetime);
        }

        {
            double radius = 5.2 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildStar(
                    cx, cy, cz, radius, 5, 2, 15, rotation, UP);
            sendBatch(entries, accentR, accentG, accentB, 1.0F, particleLifetime);
        }

        if (age >= 8) {
            double inner = 2.0 * scaleFactor;
            double outer = 4.5 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildRadialLines(
                    cx, cy, cz, inner, outer, 5, 8, rotation + Math.PI / 5.0, UP);
            sendBatch(entries, fireR, fireG, fireB, 0.6F, particleLifetime);
        }

        if (age >= 10) {
            double starRadius = 5.2 * scaleFactor;
            for (int i = 0; i < 5; i++) {
                double angle = rotation + 2.0 * Math.PI * i / 5;
                double px = cx + starRadius * Math.cos(angle);
                double pz = cz + starRadius * Math.sin(angle);
                List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                        px, cy, pz, 0.4 * scaleFactor, 10, counterRotation * 2, UP);
                sendBatchAsGlitter(entries, accentR, accentG, accentB, 0.5F, particleLifetime);
            }
        }

        if (age >= 12) {
            double radius = 3.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildPolygon(
                    cx, cy, cz, radius, 3, 12, counterRotation * 0.8, UP);
            sendBatch(entries, fireR, fireG, fireB, 0.7F, particleLifetime);
        }

        {
            double radius = 2.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 40, counterRotation * 1.5, UP);
            sendBatch(entries, accentR, accentG, accentB, 0.5F, particleLifetime);
        }

        if (age >= 8 && age % 2 == 0) {
            Random rng = new Random();
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> flames = new ArrayList<>();
            int count = 8 + rng.nextInt(5);
            for (int i = 0; i < count; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 5.0 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                double vy = 0.03 + rng.nextDouble() * 0.05;
                flames.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.1, pz, 0, vy, 0));
            }
            sendVanillaBatch(flames, "soul_fire_flame");
        }

        if (age >= 6 && age % 4 == 0) {
            spawnFireRunes(cx, cy, cz, 5.0 * scaleFactor);
        }

        {
            float pulseScale = 0.4F + 0.3F * (float) Math.sin(age * 0.4);
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy + 0.1, cz, 0.6 * scaleFactor, 12, age * 0.5, UP);
            sendBatch(entries, 1.0F, 0.9F, 0.5F, pulseScale + 0.5F, particleLifetime);
        }

        // === Enhanced effects ===

        // --- Regular flame + soul fire vanilla particles ---
        if (age % 3 == 0) {
            Random rng = new Random();
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> flames = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 4.5 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                double vy = 0.05 + rng.nextDouble() * 0.05;
                flames.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.1, pz, 0, vy, 0));
            }
            sendVanillaBatch(flames, "flame");
        }

        // --- Smoke vanilla particles at edges ---
        if (age % 4 == 0) {
            Random rng = new Random();
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> smokeEntries = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double px = cx + 5.5 * scaleFactor * Math.cos(angle);
                double pz = cz + 5.5 * scaleFactor * Math.sin(angle);
                smokeEntries.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.2, pz, 0, 0.02, 0));
            }
            sendVanillaBatch(smokeEntries, "smoke");
        }

        // --- Heat shimmer: second outer ring with slight upward velocity ---
        {
            double shimmerRadius = 6.5 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> shimmer = new ArrayList<>();
            int shimmerPts = 60;
            for (int i = 0; i < shimmerPts; i++) {
                double angle = counterRotation * 0.5 + 2.0 * Math.PI * i / shimmerPts;
                double px = cx + shimmerRadius * Math.cos(angle);
                double pz = cz + shimmerRadius * Math.sin(angle);
                shimmer.add(new S2CParticleBatchPack.ParticleEntry(px, cy, pz, 0, 0.005F, 0));
            }
            sendBatch(shimmer, fireR * 0.5F, fireG * 0.5F, fireB * 0.5F, 0.3F, particleLifetime);
        }

        // --- Fire ambient sound ---
        if (age % 20 == 0) {
            level.playSound(null, BlockPos.containing(center), SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.5F, 0.8F);
        }
    }

    private void spawnFireRunes(double cx, double cy, double cz, double areaRadius) {
        Random rng = new Random();
        List<S2CRuneBatchPack.RuneEntry> entries = new ArrayList<>();
        int count = 3 + rng.nextInt(3);
        for (int i = 0; i < count; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double dist = rng.nextDouble() * areaRadius;
            double px = cx + dist * Math.cos(angle);
            double pz = cz + dist * Math.sin(angle);
            double vy = 0.05 + rng.nextDouble() * 0.04;
            entries.add(new S2CRuneBatchPack.RuneEntry(px, cy + 0.1, pz, 0, vy, 0));
        }
        sendRuneBatch(entries, 1.0F, 0.5F, 0.1F, 0.8F, 30, true);
    }

    @Override
    protected void tickEffect(float scaleFactor) {
        if (age < 10) return;

        double r = EFFECT_RADIUS;
        AABB area = new AABB(center.x - r, center.y - 2, center.z - r,
                center.x + r, center.y + 4, center.z + r);

        List<Mob> mobs = getMobsInRadius(Mob.class, EFFECT_RADIUS);

        for (Mob mob : mobs) {
            if (age % 10 == 0) {
                mob.setSecondsOnFire(3 + specialLevel);
            }
            if (age % 20 == 0) {
                hurtCompat(mob, level.damageSources().inFire(), 2.0F * powerMultiplier);
            }
        }
    }

    @Override
    protected void onRemove() {
    }
}
