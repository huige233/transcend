package com.huige233.transcend.client.magic;

import com.huige233.transcend.network.S2CParticleBatchPack;
import com.huige233.transcend.network.S2CVanillaParticleBatchPack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ThunderCircleEffect extends AbstractMagicCircle {

    private static final double EFFECT_RADIUS = 7.0;

    public ThunderCircleEffect(ServerLevel level, Vec3 center) {
        super(level, center);
    }

    @Override
    protected void tickParticles(float scaleFactor) {
        double cx = center.x, cy = center.y, cz = center.z;
        double rotSpeed = Math.PI / 35.0;
        double rotation = age * rotSpeed;
        double counterRotation = -age * rotSpeed * 1.5;
        int particleLifetime = 3;

        {
            double radius = 6.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 100, rotation, UP);
            sendBatch(entries, baseR, baseG, baseB, 0.8F, particleLifetime);
        }

        {
            double radius = 5.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildStar(
                    cx, cy, cz, radius, 8, 3, 12, rotation, UP);
            sendBatch(entries, accentR, accentG, accentB, 1.0F, particleLifetime);
        }

        if (age >= 10) {
            double starRadius = 5.0 * scaleFactor;
            for (int i = 0; i < 8; i++) {
                double angle = rotation + 2.0 * Math.PI * i / 8;
                double px = cx + starRadius * Math.cos(angle);
                double pz = cz + starRadius * Math.sin(angle);
                List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                        px, cy, pz, 0.35 * scaleFactor, 8, counterRotation * 2, UP);
                sendBatchAsGlitter(entries, baseR, baseG, baseB, 0.4F, particleLifetime);
            }
        }

        {
            double radius = 2.5 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 40, counterRotation, UP);
            sendBatch(entries, accentR, accentG, accentB, 0.6F, particleLifetime);
        }

        if (age >= 8) {
            double inner = 2.0 * scaleFactor;
            double outer = 4.5 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildRadialLines(
                    cx, cy, cz, inner, outer, 8, 6, rotation + Math.PI / 8.0, UP);
            sendBatch(entries, baseR * 0.8F + accentR * 0.2F, baseG * 0.8F + accentG * 0.2F,
                    baseB * 0.8F + accentB * 0.2F, 0.6F, particleLifetime);
        }

        if (age >= 10 && age % 3 == 0) {
            spawnChainLightningVisuals(cx, cy, cz, scaleFactor);
        }

        if (age % 10 == 0) {
            List<S2CParticleBatchPack.ParticleEntry> flash = MagicCircleGeometry.buildCircle(
                    cx, cy + 0.1, cz, 1.5 * scaleFactor, 24, age * 0.5, UP);
            sendBatch(flash, 1.0F, 1.0F, 1.0F, 1.2F, 2);
        }

        {
            float pulseScale = 0.4F + 0.3F * (float) Math.sin(age * 0.5);
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy + 0.1, cz, 0.5 * scaleFactor, 10, age * 0.6, UP);
            sendBatch(entries, 1.0F, 1.0F, 0.6F, pulseScale + 0.5F, particleLifetime);
        }

        // === Enhanced effects ===

        // --- End rod particles at lightning strike points (when lightning tick) ---
        if (age % 30 == 0 && age >= 10) {
            Random rng = new Random();
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> endRods = new ArrayList<>();
            double starR = 5.0 * scaleFactor;
            for (int i = 0; i < 8; i++) {
                double angle = rotation + 2.0 * Math.PI * i / 8;
                double px = cx + starR * Math.cos(angle);
                double pz = cz + starR * Math.sin(angle);
                endRods.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.1, pz, 0, 0.06, 0));
            }
            sendVanillaBatch(endRods, "end_rod");
        }

        // --- Crackling sparks: high velocity yellow particles ---
        if (age % 2 == 0) {
            Random rng = new Random();
            List<S2CParticleBatchPack.ParticleEntry> sparks = new ArrayList<>();
            int sparkCount = 2 + rng.nextInt(2);
            for (int i = 0; i < sparkCount; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 5.0 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                float vx = (rng.nextFloat() - 0.5F) * 0.15F;
                float vy = rng.nextFloat() * 0.1F;
                float vz = (rng.nextFloat() - 0.5F) * 0.15F;
                sparks.add(new S2CParticleBatchPack.ParticleEntry(px, cy + 0.1, pz, vx, vy, vz));
            }
            sendBatch(sparks, 1.0F, 1.0F, 0.3F, 0.4F, 3);
        }

        // --- Electric arc glitter between adjacent star vertices ---
        if (age % 5 == 0 && age >= 10) {
            Random rng = new Random();
            double starR2 = 5.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> arcEntries = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                int next = (i + 1) % 8;
                double a1 = rotation + 2.0 * Math.PI * i / 8;
                double a2 = rotation + 2.0 * Math.PI * next / 8;
                double x1 = cx + starR2 * Math.cos(a1);
                double z1 = cz + starR2 * Math.sin(a1);
                double x2 = cx + starR2 * Math.cos(a2);
                double z2 = cz + starR2 * Math.sin(a2);
                int arcPts = 4;
                for (int p = 0; p <= arcPts; p++) {
                    double t = (double) p / arcPts;
                    double px = x1 + t * (x2 - x1) + (rng.nextDouble() - 0.5) * 0.15;
                    double pz = z1 + t * (z2 - z1) + (rng.nextDouble() - 0.5) * 0.15;
                    arcEntries.add(new S2CParticleBatchPack.ParticleEntry(px, cy + 0.1, pz, 0, 0, 0));
                }
            }
            sendBatchAsGlitter(arcEntries, 0.6F, 0.8F, 1.0F, 0.3F, 3);
        }

        // --- Thunder sound ---
        if (age % 15 == 0) {
            level.playSound(null, BlockPos.containing(center), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 0.15F, 2.0F);
        }
    }

    private void spawnChainLightningVisuals(double cx, double cy, double cz, float scaleFactor) {
        Random rng = new Random();
        double r = EFFECT_RADIUS;
        AABB area = new AABB(cx - r, cy - 2, cz - r, cx + r, cy + 4, cz + r);
        List<Mob> mobs = getMobsInRadius(Mob.class, EFFECT_RADIUS);

        List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();

        if (mobs.size() >= 2) {
            int bolts = Math.min(3, mobs.size() - 1);
            for (int b = 0; b < bolts; b++) {
                Mob from = mobs.get(b % mobs.size());
                Mob to = mobs.get((b + 1) % mobs.size());
                addLightningBolt(entries, rng,
                        from.getX(), from.getY() + from.getBbHeight() * 0.5,from.getZ(),
                        to.getX(), to.getY() + to.getBbHeight() * 0.5, to.getZ());
            }
        } else {
            double starR = 5.0 * scaleFactor;
            int idx1 = rng.nextInt(8);
            int idx2 = (idx1 + 3 + rng.nextInt(3)) % 8;
            double rot = age * Math.PI / 35.0;
            double a1 = rot + 2.0 * Math.PI * idx1 / 8;
            double a2 = rot + 2.0 * Math.PI * idx2 / 8;
            addLightningBolt(entries, rng,
                    cx + starR * Math.cos(a1), cy + 0.2, cz + starR * Math.sin(a1),
                    cx + starR * Math.cos(a2), cy + 0.2, cz + starR * Math.sin(a2));
        }

        if (!entries.isEmpty()) {
            sendBatch(entries, 0.5F, 0.7F, 1.0F, 0.5F, 4);
        }
    }

    private void addLightningBolt(List<S2CParticleBatchPack.ParticleEntry> entries, Random rng,
                                   double x1, double y1, double z1,
                                   double x2, double y2, double z2) {
        int segments = 5 + rng.nextInt(3);
        double curX = x1, curY = y1, curZ = z1;
        double segX = (x2 - x1) / segments;
        double segY = (y2 - y1) / segments;
        double segZ = (z2 - z1) / segments;

        for (int seg = 0; seg < segments; seg++) {
            double nextX = curX + segX + (rng.nextDouble() - 0.5) * 0.4;
            double nextY = curY + segY + (rng.nextDouble() - 0.5) * 0.2;
            double nextZ = curZ + segZ + (rng.nextDouble() - 0.5) * 0.4;
            if (seg == segments - 1) { nextX = x2; nextY = y2; nextZ = z2; }

            for (int p = 0; p <= 3; p++) {
                double t = (double) p / 3;
                entries.add(new S2CParticleBatchPack.ParticleEntry(
                        curX + t * (nextX - curX),
                        curY + t * (nextY - curY),
                        curZ + t * (nextZ - curZ),
                        (rng.nextFloat() - 0.5F) * 0.03F, 0, (rng.nextFloat() - 0.5F) * 0.03F));
            }

            curX = nextX;
            curY = nextY;
            curZ = nextZ;
        }
    }

    @Override
    protected void tickEffect(float scaleFactor) {
        if (age < 10) return;

        double r = EFFECT_RADIUS;
        AABB area = new AABB(center.x - r, center.y - 2, center.z - r,
                center.x + r, center.y + 4, center.z + r);

        List<Mob> mobs = getMobsInRadius(Mob.class, EFFECT_RADIUS);

        if (age % 15 == 0) {
            for (Mob mob : mobs) {
                hurtCompat(mob, level.damageSources().lightningBolt(), 3.0F * powerMultiplier);

                for (Mob other : mobs) {
                    if (other == mob) continue;
                    double dx = other.getX() - mob.getX();
                    double dz = other.getZ() - mob.getZ();
                    if (Math.sqrt(dx * dx + dz * dz) <= 4.0 + specialLevel * 1.2) {
                        hurtCompat(other, level.damageSources().lightningBolt(), 1.5F * powerMultiplier);
                    }
                }
            }
        }

        if (age % 30 == 0 && !mobs.isEmpty()) {
            Random rng = new Random();
            Mob target = mobs.get(rng.nextInt(mobs.size()));
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
            if (lightning != null) {
                lightning.moveTo(target.getX(), target.getY(), target.getZ());
                lightning.setVisualOnly(true);
                level.addFreshEntity(lightning);
            }
        }
    }

    @Override
    protected void onRemove() {
    }
}
