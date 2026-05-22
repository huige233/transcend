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

public class ChronoCircleEffect extends AbstractMagicCircle {

    private static final double EFFECT_RADIUS = 6.0;
    private long initialDayTime;

    public ChronoCircleEffect(ServerLevel level, Vec3 center) {
        super(level, center);
        this.initialDayTime = level.getDayTime();
    }

    @Override
    protected void tickParticles(float scaleFactor) {
        double cx = center.x, cy = center.y, cz = center.z;
        int particleLifetime = 4;

        long currentTime = level.getDayTime();
        double dayFraction = (currentTime % 24000) / 24000.0;
        double clockAngle = dayFraction * 2.0 * Math.PI;

        double slowHandAngle = clockAngle;
        double fastHandAngle = (currentTime % 2000) / 2000.0 * 2.0 * Math.PI;

        float sunT = (float) Math.max(0, Math.cos((dayFraction - 0.25) * 2.0 * Math.PI));
        float moonT = (float) Math.max(0, Math.cos((dayFraction - 0.75) * 2.0 * Math.PI));

        float goldR = 1.0F, goldG = 0.85F, goldB = 0.2F;
        float silverR = 0.7F, silverG = 0.75F, silverB = 0.9F;

        float ringR = goldR * sunT + silverR * moonT;
        float ringG = goldG * sunT + silverG * moonT;
        float ringB = goldB * sunT + silverB * moonT;
        float norm = Math.max(0.3F, Math.max(ringR, Math.max(ringG, ringB)));
        ringR /= norm; ringG /= norm; ringB /= norm;

        {
            double radius = 6.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 100, clockAngle * 0.1, UP);
            sendBatch(entries, ringR, ringG, ringB, 0.8F, particleLifetime);
        }

        {
            double radius = 5.5 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildDottedCircle(
                    cx, cy, cz, radius, 12, 6, 0.3, 0, UP);
            sendBatch(entries, goldR * 0.8F, goldG * 0.8F, goldB, 0.6F, particleLifetime);
        }

        {
            double handLength = 4.5 * scaleFactor;
            double hx = cx + handLength * Math.cos(fastHandAngle);
            double hz = cz + handLength * Math.sin(fastHandAngle);
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildLine(
                    cx, cy, cz, hx, cy, hz, 15);
            sendBatch(entries, 1.0F, 1.0F, 0.6F, 0.7F, particleLifetime);
        }

        {
            double handLength = 3.0 * scaleFactor;
            double hx = cx + handLength * Math.cos(slowHandAngle);
            double hz = cz + handLength * Math.sin(slowHandAngle);
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildLine(
                    cx, cy, cz, hx, cy, hz, 10);
            sendBatch(entries, 1.0F, 0.95F, 0.5F, 0.9F, particleLifetime);
        }

        {
            double radius = 3.5 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 60, -clockAngle * 0.5, UP);
            sendBatch(entries, accentR, accentG, accentB, 0.6F, particleLifetime);
        }

        {
            double coreRadius = 1.5 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> coreCircle = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, coreRadius, 30, clockAngle, UP);
            sendBatch(coreCircle, goldR, goldG, goldB, 0.5F, particleLifetime);

            List<S2CParticleBatchPack.ParticleEntry> star = MagicCircleGeometry.buildStar(
                    cx, cy, cz, coreRadius, 6, 1, 10, -clockAngle * 0.5, UP);
            sendBatch(star, 1.0F, 1.0F, 0.8F, 0.7F, particleLifetime);
        }

        {
            double sunArcR = 4.0 * scaleFactor;
            double sunAngle = -dayFraction * 2.0 * Math.PI;
            List<S2CParticleBatchPack.ParticleEntry> sunArc = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                double a = sunAngle + Math.PI * i / 20;
                sunArc.add(new S2CParticleBatchPack.ParticleEntry(
                        cx + sunArcR * Math.cos(a), cy + 0.3, cz + sunArcR * Math.sin(a)));
            }
            sendBatch(sunArc, 1.0F, 0.9F, 0.3F, 0.6F * sunT + 0.2F, particleLifetime);
        }

        {
            double moonArcR = 4.0 * scaleFactor;
            double moonAngle = -dayFraction * 2.0 * Math.PI + Math.PI;
            List<S2CParticleBatchPack.ParticleEntry> moonArc = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                double a = moonAngle + Math.PI * i / 20;
                moonArc.add(new S2CParticleBatchPack.ParticleEntry(
                        cx + moonArcR * Math.cos(a), cy + 0.3, cz + moonArcR * Math.sin(a)));
            }
            sendBatch(moonArc, silverR, silverG, silverB, 0.6F * moonT + 0.2F, particleLifetime);
        }

        if (age % 4 == 0) {
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> endRods = new ArrayList<>();
            double markRadius = 5.5 * scaleFactor;
            for (int i = 0; i < 12; i++) {
                double angle = 2.0 * Math.PI * i / 12;
                double px = cx + markRadius * Math.cos(angle);
                double pz = cz + markRadius * Math.sin(angle);
                endRods.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.15, pz, 0, 0.01, 0));
            }
            sendVanillaBatch(endRods, "end_rod");
        }

        if (age % 3 == 0) {
            Random rng = new Random();
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> enchants = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 4.0 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                enchants.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.1, pz, 0, 0.06 + rng.nextDouble() * 0.04, 0));
            }
            sendVanillaBatch(enchants, "enchant");
        }

        if (age % 5 == 0) {
            double spiralRadius = 2.0 * scaleFactor;
            for (int i = 0; i < 4; i++) {
                double angle = clockAngle + i * Math.PI / 2.0;
                double px = cx + spiralRadius * Math.cos(angle);
                double pz = cz + spiralRadius * Math.sin(angle);
                List<S2CParticleBatchPack.ParticleEntry> transEntries = new ArrayList<>();
                transEntries.add(new S2CParticleBatchPack.ParticleEntry(
                        px, cy + 0.1, pz, 0, 0.04F, 0));
                if (i % 2 == 0) {
                    sendBatch(transEntries, 1.0F, 0.9F, 0.3F, 0.5F, 10);
                } else {
                    sendBatch(transEntries, silverR, silverG, silverB, 0.5F, 10);
                }
            }
        }

        {
            float pulseScale = 0.4F + 0.3F * (float) Math.sin(age * 0.4);
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy + 0.1, cz, 0.5 * scaleFactor, 12, clockAngle, UP);
            sendBatch(entries, ringR, ringG, ringB, pulseScale + 0.5F, particleLifetime);
        }

        if (age % 20 == 0) {
            level.playSound(null, BlockPos.containing(center), SoundEvents.NOTE_BLOCK_CHIME.value(),
                    SoundSource.BLOCKS, 0.3F, 1.0F);
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
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            }
            if (age % 20 == 0) {
                hurtCompat(mob, level.damageSources().magic(), 1.5F * powerMultiplier);
            }
            if (age % 30 == 0) {
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
            }
        }

        int timeAdvance = 5 + specialLevel * 2;
        level.setDayTime(level.getDayTime() + timeAdvance);
    }

    @Override
    protected void onRemove() {
    }
}
