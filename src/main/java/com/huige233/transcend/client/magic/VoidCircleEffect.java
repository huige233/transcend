package com.huige233.transcend.client.magic;

import com.huige233.transcend.ModDamageTypes;
import com.huige233.transcend.network.S2CParticleBatchPack;
import com.huige233.transcend.network.S2CRuneBatchPack;
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

public class VoidCircleEffect extends AbstractMagicCircle {

    private static final double EFFECT_RADIUS = 6.0;

    public VoidCircleEffect(ServerLevel level, Vec3 center) {
        super(level, center);
    }

    @Override
    protected void tickParticles(float scaleFactor) {
        double cx = center.x, cy = center.y, cz = center.z;
        double rotSpeed = Math.PI / 45.0;
        double rotation = age * rotSpeed;
        double counterRotation = -age * rotSpeed * 1.2;
        int particleLifetime = 4;

        {
            double radius = 6.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 80, rotation, UP);
            sendBatch(entries, baseR, baseG, baseB, 0.7F, particleLifetime);
        }

        {
            double radius = 5.2 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildStar(
                    cx, cy, cz, radius, 5, 2, 15, rotation + Math.PI, UP);
            sendBatch(entries, accentR, accentG, accentB, 1.0F, particleLifetime);
        }

        if (age >= 5) {
            double arcRadius = 6.3 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildDottedCircle(
                    cx, cy, cz, arcRadius, 5, 10, 0.3, rotation + Math.PI / 5.0, UP);
            float mixR = baseR * 0.5F + accentR * 0.5F;
            float mixG = baseG * 0.5F + accentG * 0.5F;
            float mixB = baseB * 0.5F + accentB * 0.5F;
            sendBatch(entries, mixR, mixG, mixB, 0.5F, particleLifetime);
        }

        if (age >= 10) {
            double starRadius = 5.2 * scaleFactor;
            for (int i = 0; i < 5; i++) {
                double angle = rotation + Math.PI + 2.0 * Math.PI * i / 5;
                double px = cx + starRadius * Math.cos(angle);
                double pz = cz + starRadius * Math.sin(angle);
                List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                        px, cy, pz, 0.35 * scaleFactor, 8, counterRotation * 2, UP);
                sendBatch(entries, accentR, accentG, accentB, 0.4F, particleLifetime);
            }
        }

        {
            double radius = 2.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 40, counterRotation * 1.5, UP);
            float darkR = accentR * 0.5F;
            float darkG = accentG * 0.5F;
            float darkB = accentB * 0.5F;
            sendBatch(entries, darkR, darkG, darkB, 0.5F, particleLifetime);
        }

        if (age >= 6 && age % 2 == 0) {
            Random rng = new Random();
            List<S2CParticleBatchPack.ParticleEntry> tendrils = new ArrayList<>();
            int count = 6 + rng.nextInt(3);
            for (int i = 0; i < count; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = 3.0 + rng.nextDouble() * 3.0 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                double pullX = (cx - px) * 0.04;
                double pullZ = (cz - pz) * 0.04;
                tendrils.add(new S2CParticleBatchPack.ParticleEntry(
                        px, cy + rng.nextDouble() * 0.3, pz,
                        (float) pullX, -0.01F, (float) pullZ));
            }
            sendBatch(tendrils, baseR, baseG, baseB, 0.4F, 12);
        }

        if (age >= 8 && age % 3 == 0) {
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> witchParticles = new ArrayList<>();
            Random rng = new Random();
            for (int i = 0; i < 5; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 4.0 * scaleFactor;
                witchParticles.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        cx + dist * Math.cos(angle), cy + 0.1 + rng.nextDouble() * 0.5,
                        cz + dist * Math.sin(angle), 0, 0.01, 0));
            }
            sendVanillaBatch(witchParticles, "witch");
        }

        if (age >= 10 && age % 4 == 0) {
            double runeRadius = 4.5 * scaleFactor;
            List<S2CRuneBatchPack.RuneEntry> runes = new ArrayList<>();
            int runeCount = 8;
            for (int i = 0; i < runeCount; i++) {
                double angle = rotation * 0.4 + 2.0 * Math.PI * i / runeCount;
                double px = cx + runeRadius * Math.cos(angle);
                double pz = cz + runeRadius * Math.sin(angle);
                runes.add(new S2CRuneBatchPack.RuneEntry(px, cy + 0.15, pz, 0, 0, 0));
            }
            sendRuneBatch(runes, 0.4F, 0.0F, 0.1F, 0.8F, 5, true);
        }

        {
            float pulseScale = 0.5F - 0.2F * (float) Math.sin(age * 0.35);
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy + 0.1, cz, 0.4 * scaleFactor, 8, age * 0.5, UP);
            sendBatch(entries, 0.1F, 0.0F, 0.1F, pulseScale + 0.3F, particleLifetime);
        }

        // === Enhanced effects ===

        // --- End rod particles sucked inward from outer ring ---
        if (age % 3 == 0) {
            Random rng = new Random();
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> endRods = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = 6.0 + rng.nextDouble() * 2.0;
                double px = cx + dist * scaleFactor * Math.cos(angle);
                double pz = cz + dist * scaleFactor * Math.sin(angle);
                double inwardX = (cx - px) * 0.05;
                double inwardZ = (cz - pz) * 0.05;
                endRods.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.2 + rng.nextDouble() * 0.5, pz, inwardX, 0, inwardZ));
            }
            sendVanillaBatch(endRods, "end_rod");
        }

        // --- Portal vanilla particles at center ---
        if (age % 4 == 0) {
            Random rng = new Random();
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> portals = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 1.0;
                portals.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        cx + dist * Math.cos(angle), cy + 0.1 + rng.nextDouble() * 0.3,
                        cz + dist * Math.sin(angle), 0, 0.02, 0));
            }
            sendVanillaBatch(portals, "portal");
        }

        // --- Decay ring: dotted circle pulsing inversely, dark red ---
        {
            float inversePulse = 1.0F - 0.2F * (float) Math.sin(age * 0.3);
            double decayRadius = 4.0 * scaleFactor * inversePulse;
            List<S2CParticleBatchPack.ParticleEntry> decayRing = MagicCircleGeometry.buildDottedCircle(
                    cx, cy, cz, decayRadius, 5, 8, 0.3, counterRotation * 0.7, UP);
            sendBatch(decayRing, 0.4F, 0.05F, 0.05F, 0.5F, particleLifetime);
        }

        // --- Wither effect particles: dark particles with random velocity near mobs ---
        if (age % 3 == 0 && age >= 10) {
            Random rng = new Random();
            double r = EFFECT_RADIUS;
            AABB mobArea = new AABB(cx - r, cy - 2, cz - r, cx + r, cy + 4, cz + r);
            List<Mob> nearbyMobs = getMobsInRadius(Mob.class, EFFECT_RADIUS);
            List<S2CParticleBatchPack.ParticleEntry> witherParts = new ArrayList<>();
            int count = Math.min(3, nearbyMobs.size() * 3);
            if (count == 0) count = 3;
            for (int i = 0; i < count; i++) {
                double px, py, pz;
                if (!nearbyMobs.isEmpty()) {
                    Mob mob = nearbyMobs.get(rng.nextInt(nearbyMobs.size()));
                    px = mob.getX() + (rng.nextDouble() - 0.5) * 0.8;
                    py = mob.getY() + rng.nextDouble() * mob.getBbHeight();
                    pz = mob.getZ() + (rng.nextDouble() - 0.5) * 0.8;
                } else {
                    double angle = rng.nextDouble() * Math.PI * 2;
                    double dist = rng.nextDouble() * 3.0 * scaleFactor;
                    px = cx + dist * Math.cos(angle);
                    py = cy + 0.2 + rng.nextDouble() * 0.5;
                    pz = cz + dist * Math.sin(angle);
                }
                float vx = (rng.nextFloat() - 0.5F) * 0.02F;
                float vy = (rng.nextFloat() - 0.5F) * 0.02F;
                float vz = (rng.nextFloat() - 0.5F) * 0.02F;
                witherParts.add(new S2CParticleBatchPack.ParticleEntry(px, py, pz, vx, vy, vz));
            }
            sendBatch(witherParts, 0.15F, 0.05F, 0.15F, 0.3F, 8);
        }

        // --- Wither ambient sound ---
        if (age % 25 == 0) {
            level.playSound(null, BlockPos.containing(center), SoundEvents.WITHER_AMBIENT, SoundSource.BLOCKS, 0.15F, 0.5F);
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
            if (age % 20 == 0) {
                mob.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1 + specialLevel / 2));
                hurtCompat(mob, level.damageSources().magic(), 2.0F * powerMultiplier);
            }

            if (age % 40 == 0) {
                hurtCompat(mob, ModDamageTypes.causeRandomDamage(level, null), 1.0F * powerMultiplier);
            }
        }
    }

    @Override
    protected void onRemove() {
    }
}
