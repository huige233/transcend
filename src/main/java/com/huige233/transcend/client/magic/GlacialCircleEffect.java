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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GlacialCircleEffect extends AbstractMagicCircle {

    private static final double EFFECT_RADIUS = 6.0;

    public GlacialCircleEffect(ServerLevel level, Vec3 center) {
        super(level, center);
    }

    @Override
    protected void tickParticles(float scaleFactor) {
        double cx = center.x, cy = center.y, cz = center.z;
        double rotSpeed = Math.PI / 50.0;
        double rotation = age * rotSpeed;
        double counterRotation = -age * rotSpeed * 0.8;
        int particleLifetime = 5;

        {
            double radius = 6.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 100, rotation, UP);
            sendBatch(entries, baseR, baseG, baseB, 0.8F, particleLifetime);
        }

        {
            double radius = 5.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildPolygon(
                    cx, cy, cz, radius, 6, 18, rotation, UP);
            sendBatch(entries, 1.0F, 1.0F, 1.0F, 0.9F, particleLifetime);
        }

        if (age >= 6) {
            double radius = 4.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildHexagonalSnowflake(
                    cx, cy, cz, radius * 0.6, 6, radius * 0.4, counterRotation, UP);
            sendBatch(entries, baseR, baseG, baseB, 0.7F, particleLifetime);
        }

        if (age >= 5) {
            double arcRadius = 6.3 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildDottedCircle(
                    cx, cy, cz, arcRadius, 6, 10, 0.25, rotation + Math.PI / 6.0, UP);
            sendBatch(entries, 1.0F, 1.0F, 1.0F, 0.5F, particleLifetime);
        }

        if (age >= 10) {
            double hexRadius = 5.0 * scaleFactor;
            for (int i = 0; i < 6; i++) {
                double angle = rotation + Math.PI * i / 3.0;
                double px = cx + hexRadius * Math.cos(angle);
                double pz = cz + hexRadius * Math.sin(angle);
                List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                        px, cy, pz, 0.4 * scaleFactor, 10, counterRotation * 2, UP);
                sendBatchAsGlitter(entries, 1.0F, 1.0F, 1.0F, 0.4F, particleLifetime);
            }
        }

        {
            double radius = 1.5 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 30, counterRotation * 1.5, UP);
            sendBatch(entries, accentR, accentG, accentB, 0.5F, particleLifetime);
        }

        if (age >= 8 && age % 3 == 0) {
            Random rng = new Random();
            List<S2CParticleBatchPack.ParticleEntry> frost = new ArrayList<>();
            int count = 6 + rng.nextInt(5);
            for (int i = 0; i < count; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 5.0 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                double vy = -0.01 - rng.nextDouble() * 0.02;
                frost.add(new S2CParticleBatchPack.ParticleEntry(
                        px, cy + 0.5 + rng.nextDouble() * 1.0, pz, 0, (float) vy, 0));
            }
            sendBatchAsGlitter(frost, 0.8F, 0.9F, 1.0F, 0.3F, 20);
        }

        {
            float pulseScale = 0.3F + 0.15F * (float) Math.sin(age * 0.25);
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy + 0.1, cz, 0.5 * scaleFactor, 10, age * 0.3, UP);
            sendBatch(entries, 1.0F, 1.0F, 1.0F, pulseScale + 0.5F, particleLifetime);
        }

        // === Enhanced effects ===

        // --- Snowflake vanilla particles falling from above ---
        if (age % 4 == 0) {
            Random rng = new Random();
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> snowflakes = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 5.0 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                double spawnY = cy + 1.5 + rng.nextDouble() * 1.0;
                snowflakes.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, spawnY, pz, 0, -0.02, 0));
            }
            sendVanillaBatch(snowflakes, "snowflake");
        }

        // --- Second hexagon ring counter-rotating ---
        {
            double hexRadius2 = 3.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> hex2 = MagicCircleGeometry.buildPolygon(
                    cx, cy, cz, hexRadius2, 6, 14, counterRotation * 1.2, UP);
            sendBatch(hex2, baseR * 0.8F, baseG * 0.8F, baseB * 0.8F, 0.6F, particleLifetime);
        }

        // --- Ice crackle particles at ground level ---
        if (age % 6 == 0) {
            Random rng = new Random();
            List<S2CParticleBatchPack.ParticleEntry> crackle = new ArrayList<>();
            int crackleCount = 3 + rng.nextInt(2);
            for (int i = 0; i < crackleCount; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 4.5 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                crackle.add(new S2CParticleBatchPack.ParticleEntry(px, cy + 0.02, pz, 0, 0, 0));
            }
            sendBatch(crackle, 1.0F, 1.0F, 1.0F, 0.5F, 3);
        }

        // --- Ice crackle sound ---
        if (age % 30 == 0) {
            level.playSound(null, BlockPos.containing(center), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.2F, 1.8F);
        }
    }

    @Override
    protected void tickEffect(float scaleFactor) {
        if (age < 10) return;

        double r = EFFECT_RADIUS;
        AABB area = new AABB(center.x - r, center.y - 2, center.z - r,
                center.x + r, center.y + 4, center.z + r);

        List<Mob> mobs = getMobsInRadius(Mob.class, EFFECT_RADIUS);

        for (Mob mob : mobs) {
            mob.setTicksFrozen(Math.min(mob.getTicksFrozen() + 5 + specialLevel * 2, 140 + specialLevel * 20));

            if (age % 20 == 0) {
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                hurtCompat(mob, level.damageSources().magic(), 1.5F * powerMultiplier);
            }

            if (age % 40 == 0) {
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
            }
        }
    }

    @Override
    protected void onRemove() {
    }
}
