package com.huige233.transcend.client.magic;

import com.huige233.transcend.network.S2CParticleBatchPack;
import com.huige233.transcend.network.S2CRuneBatchPack;
import com.huige233.transcend.network.S2CVanillaParticleBatchPack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class MagicCircleEffectServer1 extends AbstractMagicCircle {

    private static final double BIND_RADIUS = 7.0;
    private final Set<Mob> boundEntities = new HashSet<>();

    public MagicCircleEffectServer1(ServerLevel level, Vec3 center) {
        super(level, center);
    }

    @Override
    public MagicCircleEffectServer1 withColor(float r, float g, float b) {
        super.withColor(r, g, b);
        return this;
    }

    @Override
    public MagicCircleEffectServer1 withAccentColor(float r, float g, float b) {
        super.withAccentColor(r, g, b);
        return this;
    }

    @Override
    public MagicCircleEffectServer1 withMaxAge(int ticks) {
        super.withMaxAge(ticks);
        return this;
    }

    @Override
    protected void tickParticles(float scaleFactor) {
        double cx = center.x, cy = center.y, cz = center.z;
        double rotSpeed = Math.PI / 40.0;
        double rotation = age * rotSpeed;
        double counterRotation = -age * rotSpeed * 1.3;
        int particleLifetime = 4;

        // === Outer band ===

        // --- Outer ring ---
        {
            double radius = 6.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 120, rotation, UP);
            sendBatch(entries, baseR, baseG, baseB, 0.8F, particleLifetime);
        }
        
 // --- Gold enchantment runes hugging the inside of the fucking outer ring ---
        if (age >= 10) {
            double runeRadius = 5.9 * scaleFactor;
            spawnGoldRuneRing(cx, cy, cz, runeRadius, age * Math.PI / 3.0);
        }
        
        // --- Outer decorative arcs: 6 arcs bridging hexagram vertices ---
        if (age >= 5) {
            double arcRadius = 6.5 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildDottedCircle(
                    cx, cy, cz, arcRadius, 6, 12, 0.2, rotation + Math.PI / 6.0, UP);
            sendBatch(entries, baseR * 0.7F + accentR * 0.3F, baseG * 0.7F + accentG * 0.3F,
                    baseB * 0.7F + accentB * 0.3F, 0.5F, particleLifetime);
        }

        // --- Hexagram (two overlaid triangles) ---
        {
            double radius = 5.2 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();
            entries.addAll(MagicCircleGeometry.buildPolygon(cx, cy, cz, radius, 3, 20, rotation, UP));
            entries.addAll(MagicCircleGeometry.buildPolygon(cx, cy, cz, radius, 3, 20, rotation + Math.PI / 3.0, UP));
            sendBatch(entries, accentR, accentG, accentB, 1.0F, particleLifetime);
        }

        // --- Hexagram vertex circles ---
        if (age >= 10) {
            double starRadius = 5.2 * scaleFactor;
            for (int i = 0; i < 6; i++) {
                double angle = rotation + Math.PI * i / 3.0;
                double px = cx + starRadius * Math.cos(angle);
                double pz = cz + starRadius * Math.sin(angle);
                List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                        px, cy, pz, 0.5 * scaleFactor, 12, counterRotation * 2, UP);
                sendBatch(entries, accentR, accentG, accentB, 0.4F, particleLifetime);
            }
        }

        // --- Inner ring ---
        {
            double radius = 4.5 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 90, counterRotation, UP);
            sendBatch(entries, baseR, baseG, baseB, 0.6F, particleLifetime);
        }

        // === Mid band ===

        // --- Radial lines from core to inner ring ---
        if (age >= 8) {
            double inner = 2.2 * scaleFactor;
            double outer = 4.3 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildRadialLines(
                    cx, cy, cz, inner, outer, 6, 8, rotation + Math.PI / 6.0, UP);
            sendBatch(entries, accentR * 0.8F + baseR * 0.2F, accentG * 0.8F + baseG * 0.2F,
                    accentB * 0.8F + baseB * 0.2F, 0.6F, particleLifetime);
        }

        // --- Rotating inner square ---
        if (age >= 12) {
            double radius = 3.2 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildPolygon(
                    cx, cy, cz, radius, 4, 15, counterRotation * 0.7, UP);
            sendBatch(entries, baseR * 0.5F + accentR * 0.5F, baseG * 0.5F + accentG * 0.5F,
                    baseB * 0.5F + accentB * 0.5F, 0.7F, particleLifetime);
        }

        // === Core area ===

        // --- Core circle ---
        {
            double radius = 2.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 50, counterRotation * 1.5, UP);
            sendBatch(entries, accentR, accentG, accentB, 0.5F, particleLifetime);
        }

        // --- Red runes floating up from the circle ---
        if (age >= 8 && age % 3 == 0) {
            spawnFloatingRedRunes(cx, cy, cz, 5.0 * scaleFactor);
        }

        // --- Pulsing center point ---
        {
            float pulseScale = 0.3F + 0.2F * (float) Math.sin(age * 0.3);
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy + 0.1, cz, 0.3 * scaleFactor, 8, age * 0.5, UP);
            sendBatch(entries, 1.0F, 1.0F, 1.0F, pulseScale + 0.5F, particleLifetime);
        }

        // === Enhanced effects ===

        // --- End rod vanilla particles at star vertices ---
        if (age % 4 == 0 && age >= 10) {
            double starRadius = 5.2 * scaleFactor;
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> endRods = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                double angle = rotation + Math.PI * 2.0 * i / 6;
                double px = cx + starRadius * Math.cos(angle);
                double pz = cz + starRadius * Math.sin(angle);
                endRods.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.1, pz, 0, 0.05, 0));
            }
            sendVanillaBatch(endRods, "end_rod");
        }

        // --- Spiral overlay counter-rotating, accent color ---
        {
            List<S2CParticleBatchPack.ParticleEntry> spiralEntries = MagicCircleGeometry.buildSpiral(
                    cx, cy, cz, 1.0 * scaleFactor, 5.0 * scaleFactor,
                    1.5, 40, counterRotation, UP);
            sendBatch(spiralEntries, accentR, accentG, accentB, 0.5F, particleLifetime);
        }

        // --- Activation sound ---
        if (age % 40 == 0) {
            level.playSound(null, BlockPos.containing(center), SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.3F, 0.8F);
        }
    }

    private void spawnGoldRuneRing(double cx, double cy, double cz,
                                    double radius, double rotation) {
        List<S2CRuneBatchPack.RuneEntry> entries = new ArrayList<>();
        int runeCount = 8;
        for (int i = 0; i < runeCount; i++) {
            double angle = rotation + 2.0 * Math.PI * i / runeCount;
            double px = cx + radius * Math.cos(angle);
            double pz = cz + radius * Math.sin(angle);
            entries.add(new S2CRuneBatchPack.RuneEntry(
                    px, cy + 0.15, pz,
                    0, 0, 0));
        }
        sendRuneBatch(entries, 1.0F, 0.85F, 0.2F, 1.0F, 4, true);
    }

    private void spawnFloatingRedRunes(double cx, double cy, double cz, double areaRadius) {
        Random rng = new Random();
        List<S2CRuneBatchPack.RuneEntry> entries = new ArrayList<>();
        int count = 3 + rng.nextInt(3);
        for (int i = 0; i < count; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double dist = rng.nextDouble() * areaRadius;
            double px = cx + dist * Math.cos(angle);
            double pz = cz + dist * Math.sin(angle);
            double vy = 0.04 + rng.nextDouble() * 0.04;
            double vx = (rng.nextDouble() - 0.5) * 0.005;
            double vz = (rng.nextDouble() - 0.5) * 0.005;
            entries.add(new S2CRuneBatchPack.RuneEntry(
                    px, cy + 0.1, pz,
                    vx, vy, vz));
        }
        sendRuneBatch(entries, 0.9F, 0.05F, 0.05F, 0.8F, 35, true);
    }

    @Override
    protected void tickEffect(float scaleFactor) {
        if (age >= 10) {
            tickBinding();
        }
    }

    @Override
    protected void onRemove() {
        releaseAll();
    }

    private void tickBinding() {
        double r = BIND_RADIUS;
        AABB area = new AABB(center.x - r, center.y - 2, center.z - r,
                center.x + r, center.y + 4, center.z + r);

        List<Mob> mobsInRange = level.getEntitiesOfClass(Mob.class, area, mob -> {
            if (!shouldAffect(mob)) return false;
            net.minecraft.world.entity.player.Player nearest = level.getNearestPlayer(center.x, center.y, center.z, r * 2, false);
            return nearest == null || !mob.isAlliedTo(nearest);
        });

        for (Mob mob : mobsInRange) {
            double dx = mob.getX() - center.x;
            double dz = mob.getZ() - center.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > BIND_RADIUS) continue;

            if (boundEntities.add(mob)) {
                bindEntity(mob);
            }
        }

        Iterator<Mob> it = boundEntities.iterator();
        while (it.hasNext()) {
            Mob mob = it.next();
            if (!mob.isAlive() || mob.isRemoved()) {
                it.remove();
                continue;
            }

            mob.setDeltaMovement(Vec3.ZERO);
            mob.getNavigation().stop();
            mob.setTarget(null);
            mob.setNoAi(true);

            if (age % 20 == 0) {
                DamageSources sources = level.damageSources();
                hurtCompat(mob, sources.magic(), (1.0F + specialLevel * 0.3F) * powerMultiplier);
            }

            if (age % 4 == 0) {
                spawnLightningParticles(mob);
            }
        }
    }

    private void bindEntity(Mob mob) {
        mob.setNoAi(true);
        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.setDeltaMovement(Vec3.ZERO);
    }

    private void releaseAll() {
        for (Mob mob : boundEntities) {
            if (mob.isAlive() && !mob.isRemoved()) {
                mob.setNoAi(false);
            }
        }
        boundEntities.clear();

        double r = BIND_RADIUS;
        AABB area = new AABB(center.x - r, center.y - 2, center.z - r,
                center.x + r, center.y + 4, center.z + r);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
            if (mob.isAlive() && mob.isNoAi()) {
                mob.setNoAi(false);
            }
        }
    }

    private void spawnLightningParticles(LivingEntity entity) {
        Random rng = new Random();
        double ex = entity.getX();
        double ey = entity.getY();
        double ez = entity.getZ();
        double height = entity.getBbHeight();

        List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();

        int boltCount = 2 + rng.nextInt(2);
        for (int bolt = 0; bolt < boltCount; bolt++) {
            double startY = ey + height * (0.2 + rng.nextDouble() * 0.6);
            double endY = startY + height * (0.3 + rng.nextDouble() * 0.4);
            double startX = ex + (rng.nextDouble() - 0.5) * 0.4;
            double startZ = ez + (rng.nextDouble() - 0.5) * 0.4;

            int segments = 4 + rng.nextInt(3);
            double curX = startX, curY = startY, curZ = startZ;
            double segHeight = (endY - startY) / segments;

            for (int seg = 0; seg < segments; seg++) {
                double nextX = curX + (rng.nextDouble() - 0.5) * 0.5;
                double nextY = curY + segHeight;
                double nextZ = curZ + (rng.nextDouble() - 0.5) * 0.5;

                int pointsPerSeg = 3;
                for (int p = 0; p <= pointsPerSeg; p++) {
                    double t = (double) p / pointsPerSeg;
                    double px = curX + t * (nextX - curX);
                    double py = curY + t * (nextY - curY);
                    double pz = curZ + t * (nextZ - curZ);
                    float jitterX = (rng.nextFloat() - 0.5F) * 0.05F;
                    float jitterZ = (rng.nextFloat() - 0.5F) * 0.05F;
                    entries.add(new S2CParticleBatchPack.ParticleEntry(px, py, pz, jitterX, 0, jitterZ));
                }

                curX = nextX;
                curY = nextY;
                curZ = nextZ;
            }
        }

        int sparkCount = 3 + rng.nextInt(3);
        for (int i = 0; i < sparkCount; i++) {
            double sx = ex + (rng.nextDouble() - 0.5) * 0.6;
            double sy = ey + rng.nextDouble() * height;
            double sz = ez + (rng.nextDouble() - 0.5) * 0.6;
            float vx = (rng.nextFloat() - 0.5F) * 0.3F;
            float vy = rng.nextFloat() * 0.2F;
            float vz = (rng.nextFloat() - 0.5F) * 0.3F;
            entries.add(new S2CParticleBatchPack.ParticleEntry(sx, sy, sz, vx, vy, vz));
        }

        sendBatch(entries, 0.6F, 0.2F, 1.0F, 0.5F, 5);
    }
}
