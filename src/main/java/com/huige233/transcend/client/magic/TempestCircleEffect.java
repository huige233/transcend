package com.huige233.transcend.client.magic;

import com.huige233.transcend.network.S2CParticleBatchPack;
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

public class TempestCircleEffect extends AbstractMagicCircle {

    private static final double EFFECT_RADIUS = 6.0;

    public TempestCircleEffect(ServerLevel level, Vec3 center) {
        super(level, center);
    }

    @Override
    protected void tickParticles(float scaleFactor) {
        double cx = center.x, cy = center.y, cz = center.z;
        double rotSpeed = Math.PI / 30.0;
        double rotation = age * rotSpeed;
        int particleLifetime = 4;

        {
            double radius = 6.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 80, rotation, UP);
            sendBatch(entries, baseR, baseG, baseB, 0.8F, particleLifetime);
        }

        {
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildConcentricRings(
                    cx, cy, cz, 4, 5.5 * scaleFactor, 1.2, 0.7,
                    20, rotation, UP);
            float mixR = baseR * 0.6F + accentR * 0.4F;
            float mixG = baseG * 0.6F + accentG * 0.4F;
            float mixB = baseB * 0.6F + accentB * 0.4F;
            sendBatch(entries, mixR, mixG, mixB, 0.6F, particleLifetime);
        }

        if (age >= 8) {
            double inner = 1.0 * scaleFactor;
            double outer = 5.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> lines = MagicCircleGeometry.buildRadialLines(
                    cx, cy, cz, inner, outer, 8, 6, rotation + Math.PI / 8.0, UP);
            List<S2CParticleBatchPack.ParticleEntry> windLines = new ArrayList<>();
            for (S2CParticleBatchPack.ParticleEntry e : lines) {
                windLines.add(new S2CParticleBatchPack.ParticleEntry(
                        e.x, e.y, e.z, 0, 0.02F, 0));
            }
            sendBatch(windLines, accentR, accentG, accentB, 0.5F, particleLifetime);
        }

        if (age >= 6 && age % 2 == 0) {
            Random rng = new Random();
            List<S2CParticleBatchPack.ParticleEntry> streaks = new ArrayList<>();
            int count = 6 + rng.nextInt(3);
            for (int i = 0; i < count; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = 1.0 + rng.nextDouble() * 4.0 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                double vy = 0.10 + rng.nextDouble() * 0.08;
                double vx = (rng.nextDouble() - 0.5) * 0.04;
                double vz = (rng.nextDouble() - 0.5) * 0.04;
                streaks.add(new S2CParticleBatchPack.ParticleEntry(px, cy + 0.1, pz, (float) vx, (float) vy, (float) vz));
            }
            sendBatchAsGlitter(streaks, 1.0F, 1.0F, 1.0F, 0.3F, 15);
        }

        {
            double radius = 1.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 16, -rotation * 2.5, UP);
            sendBatch(entries, accentR, accentG, accentB, 0.7F, particleLifetime);
        }

        {
            float pulseScale = 0.3F + 0.2F * (float) Math.sin(age * 0.4);
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy + 0.1, cz, 0.4 * scaleFactor, 8, age * 0.7, UP);
            sendBatch(entries, 1.0F, 1.0F, 1.0F, pulseScale + 0.5F, particleLifetime);
        }

        // === Enhanced effects ===

        // --- Campfire smoke particles spiraling upward ---
        if (age % 3 == 0) {
            Random rng = new Random();
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> smokeSpiral = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                double angle = rotation + 2.0 * Math.PI * i / 5;
                double dist = (2.0 + rng.nextDouble() * 2.0) * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                smokeSpiral.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.2, pz, 0, 0.08, 0));
            }
            sendVanillaBatch(smokeSpiral, "campfire_cosy_smoke");
        }

        // --- Leaf/debris particles with strong horizontal velocity ---
        if (age % 2 == 0) {
            Random rng = new Random();
            List<S2CParticleBatchPack.ParticleEntry> debris = new ArrayList<>();
            int debrisCount = 4 + rng.nextInt(3);
            for (int i = 0; i < debrisCount; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 4.0 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                double tangentX = -Math.sin(angle) * (0.05 + rng.nextDouble() * 0.05);
                double tangentZ = Math.cos(angle) * (0.05 + rng.nextDouble() * 0.05);
                debris.add(new S2CParticleBatchPack.ParticleEntry(
                        px, cy + 0.3 + rng.nextDouble() * 0.5, pz,
                        (float) tangentX, 0.03F, (float) tangentZ));
            }
            sendBatch(debris, 0.5F, 0.7F, 0.3F, 0.3F, 10);
        }

        // --- Third ascending ring at Y+2.0 with smaller radius ---
        {
            double ascRadius = 2.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> ascRing = MagicCircleGeometry.buildCircle(
                    cx, cy + 2.0, cz, ascRadius, 30, rotation * 2.0, UP);
            sendBatch(ascRing, accentR * 0.7F, accentG * 0.7F, accentB * 0.7F, 0.4F, particleLifetime);
        }

        // --- Elytra flying sound ---
        if (age % 20 == 0) {
            level.playSound(null, BlockPos.containing(center), SoundEvents.ELYTRA_FLYING, SoundSource.BLOCKS, 0.3F, 1.5F);
        }
    }

    @Override
    protected void tickEffect(float scaleFactor) {
        if (age < 10) return;

        double r = EFFECT_RADIUS;
        AABB area = new AABB(center.x - r, center.y - 2, center.z - r,
                center.x + r, center.y + 6, center.z + r);

        List<Mob> mobs = getMobsInRadius(Mob.class, EFFECT_RADIUS);

        for (Mob mob : mobs) {
            Vec3 motion = mob.getDeltaMovement();
            mob.setDeltaMovement(motion.add(0, 0.12 + specialLevel * 0.036, 0));

            double dx = mob.getX() - center.x;
            double dz = mob.getZ() - center.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0.5) {
                mob.setDeltaMovement(mob.getDeltaMovement().add(
                        dx / dist * 0.05, 0, dz / dist * 0.05));
            }

            if (age % 20 == 0) {
                hurtCompat(mob, level.damageSources().magic(), 1.0F * powerMultiplier);
            }
        }
    }

    @Override
    protected void onRemove() {
    }
}
