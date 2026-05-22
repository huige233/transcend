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

public class GravityCircleEffect extends AbstractMagicCircle {

    private static final double EFFECT_RADIUS = 7.0;

    public GravityCircleEffect(ServerLevel level, Vec3 center) {
        super(level, center);
    }

    @Override
    protected void tickParticles(float scaleFactor) {
        double cx = center.x, cy = center.y, cz = center.z;
        double rotSpeed = Math.PI / 35.0;
        double rotation = age * rotSpeed;
        int particleLifetime = 4;

        {
            double radius = 6.0 * scaleFactor;
            Random rng = new Random(age);
            List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();
            int points = 80;
            for (int i = 0; i < points; i++) {
                double angle = rotation + 2.0 * Math.PI * i / points;
                double px = cx + radius * Math.cos(angle);
                double pz = cz + radius * Math.sin(angle);
                double inwardX = (cx - px) * 0.01;
                double inwardZ = (cz - pz) * 0.01;
                entries.add(new S2CParticleBatchPack.ParticleEntry(px, cy, pz,
                        (float) inwardX, 0, (float) inwardZ));
            }
            sendBatch(entries, baseR, baseG, baseB, 0.7F, particleLifetime);
        }

        for (int arm = 0; arm < 3; arm++) {
            double armRotation = rotation + arm * 2.0 * Math.PI / 3.0;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildSpiral(
                    cx, cy, cz, 0.5 * scaleFactor, 5.5 * scaleFactor,
                    2.0, 60, armRotation, UP);
            float mixR = baseR * 0.7F + accentR * 0.3F;
            float mixG = baseG * 0.7F + accentG * 0.3F;
            float mixB = baseB * 0.7F + accentB * 0.3F;
            sendBatch(entries, mixR, mixG, mixB, 0.6F, particleLifetime);
        }

        {
            double radius = 1.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 20, -rotation * 2, UP);
            sendBatch(entries, accentR, accentG, accentB, 0.9F, particleLifetime);
        }

        if (age >= 8 && age % 2 == 0) {
            Random rng = new Random();
            List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();
            int count = 8;
            for (int i = 0; i < count; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = 2.0 + rng.nextDouble() * 4.0 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                double pullX = (cx - px) * 0.03;
                double pullZ = (cz - pz) * 0.03;
                entries.add(new S2CParticleBatchPack.ParticleEntry(
                        px, cy + rng.nextDouble() * 0.5, pz,
                        (float) pullX, 0.01F, (float) pullZ));
            }
            sendBatch(entries, baseR, baseG, baseB, 0.5F, 15);
        }

        if (age >= 10 && age % 4 == 0) {
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> portals = new ArrayList<>();
            Random rng = new Random();
            for (int i = 0; i < 6; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 1.5;
                portals.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        cx + dist * Math.cos(angle), cy + 0.2 + rng.nextDouble() * 0.5,
                        cz + dist * Math.sin(angle), 0, 0.02, 0));
            }
            sendVanillaBatch(portals, "portal");
        }

        {
            float pulseScale = 0.5F + 0.3F * (float) Math.sin(age * 0.5);
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy + 0.05, cz, 0.3 * scaleFactor, 8, age * 0.8, UP);
            sendBatch(entries, 0.1F, 0.0F, 0.15F, pulseScale + 0.3F, particleLifetime);
        }

        // === Enhanced effects ===

        // --- Smoke vanilla particles being pulled inward ---
        if (age % 3 == 0) {
            Random rng = new Random();
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> smoke = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = 5.0 + rng.nextDouble() * 2.0;
                double px = cx + dist * scaleFactor * Math.cos(angle);
                double pz = cz + dist * scaleFactor * Math.sin(angle);
                double inwardX = (cx - px) * 0.02;
                double inwardZ = (cz - pz) * 0.02;
                smoke.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.2 + rng.nextDouble() * 0.5, pz, inwardX, 0, inwardZ));
            }
            sendVanillaBatch(smoke, "smoke");
        }

        // --- Outer distortion ring: dotted circle with pulsing scale ---
        {
            float pulseDistort = 0.8F + 0.2F * (float) Math.sin(age * 0.3);
            double distortRadius = 7.5 * scaleFactor * pulseDistort;
            List<S2CParticleBatchPack.ParticleEntry> distortRing = MagicCircleGeometry.buildDottedCircle(
                    cx, cy, cz, distortRadius, 10, 8, 0.3, rotation * 0.5, UP);
            sendBatch(distortRing, baseR * 0.6F, baseG * 0.6F, baseB * 0.6F, 0.3F, particleLifetime);
        }

        // --- Dark particle cascade: spawned at Y+2.0 falling with inward pull ---
        if (age % 5 == 0) {
            Random rng = new Random();
            List<S2CParticleBatchPack.ParticleEntry> cascade = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = 1.0 + rng.nextDouble() * 3.0;
                double px = cx + dist * scaleFactor * Math.cos(angle);
                double pz = cz + dist * scaleFactor * Math.sin(angle);
                float pullX = (float) ((cx - px) * 0.01);
                float pullZ = (float) ((cz - pz) * 0.01);
                cascade.add(new S2CParticleBatchPack.ParticleEntry(
                        px, cy + 2.0, pz, pullX, -0.03F, pullZ));
            }
            sendBatch(cascade, 0.05F, 0.0F, 0.1F, 0.6F, 15);
        }

        // --- Portal ambient sound ---
        if (age % 25 == 0) {
            level.playSound(null, BlockPos.containing(center), SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.2F, 0.5F);
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
            double dx = center.x - mob.getX();
            double dz = center.z - mob.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0.5) {
                double force = 0.08 + specialLevel * 0.024;
                Vec3 pull = new Vec3(dx / dist * force, 0, dz / dist * force);
                mob.setDeltaMovement(mob.getDeltaMovement().add(pull));
            }

            if (age % 20 == 0) {
                hurtCompat(mob, level.damageSources().magic(), 0.5F * powerMultiplier);
            }
        }
    }

    @Override
    protected void onRemove() {
    }
}
