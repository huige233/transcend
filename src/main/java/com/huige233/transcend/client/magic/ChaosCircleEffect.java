package com.huige233.transcend.client.magic;

import com.huige233.transcend.network.S2CParticleBatchPack;
import com.huige233.transcend.network.S2CVanillaParticleBatchPack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChaosCircleEffect extends AbstractMagicCircle {

    private static final double EFFECT_RADIUS = 6.0;
    private final Random rng = new Random();

    private static final SoundEvent[] CHAOS_SOUNDS = {
            SoundEvents.ENDERMAN_TELEPORT,
            SoundEvents.CHORUS_FRUIT_TELEPORT,
            SoundEvents.ENDER_DRAGON_GROWL
    };

    private static final String[] VANILLA_PARTICLE_CYCLE = {
            "flame", "soul_fire_flame", "portal", "witch", "end_rod"
    };

    private static final MobEffect[] RANDOM_DEBUFFS = {
            MobEffects.POISON, MobEffects.WITHER, MobEffects.BLINDNESS,
            MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS, MobEffects.CONFUSION
    };

    private static final MobEffect[] RANDOM_BUFFS = {
            MobEffects.MOVEMENT_SPEED, MobEffects.DAMAGE_BOOST, MobEffects.DAMAGE_RESISTANCE
    };

    public ChaosCircleEffect(ServerLevel level, Vec3 center) {
        super(level, center);
    }

    @Override
    protected void tickParticles(float scaleFactor) {
        double cx = center.x, cy = center.y, cz = center.z;
        double rotSpeed = Math.PI / 35.0;
        double rotation = age * rotSpeed;
        int particleLifetime = 4;

        // Color shifts based on (age % 60)
        float phase = (age % 60) / 60.0F;
        float chaosR = 0.5F + 0.5F * (float) Math.sin(phase * Math.PI * 2);
        float chaosG = 0.5F + 0.5F * (float) Math.sin(phase * Math.PI * 2 + Math.PI * 2 / 3);
        float chaosB = 0.5F + 0.5F * (float) Math.sin(phase * Math.PI * 2 + Math.PI * 4 / 3);

        // Outer ring — 80pts with shifting colors
        {
            double radius = 6.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 80, rotation, UP);
            sendBatch(entries, chaosR, chaosG, chaosB, 0.8F, particleLifetime);
        }

        // Star(7,3) — 7 pointed for chaotic asymmetry
        {
            double radius = 5.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildStar(
                    cx, cy, cz, radius, 7, 3, 12, rotation * 1.3, UP);
            float invertR = 1.0F - chaosR;
            float invertG = 1.0F - chaosG;
            float invertB = 1.0F - chaosB;
            sendBatch(entries, invertR, invertG, invertB, 1.0F, particleLifetime);
        }

        // Random spiral count (1-3) that changes every 20 ticks
        {
            int spiralCount = 1 + ((age / 20) % 3);
            for (int s = 0; s < spiralCount; s++) {
                double spiralOffset = s * (2.0 * Math.PI / spiralCount);
                List<S2CParticleBatchPack.ParticleEntry> spiral = MagicCircleGeometry.buildSpiral(
                        cx, cy, cz, 0.5 * scaleFactor, 5.0 * scaleFactor,
                        2.0, 30, rotation * 0.8 + spiralOffset, UP);
                float sR = (s == 0) ? chaosR : (s == 1 ? chaosG : chaosB);
                float sG = (s == 0) ? chaosG : (s == 1 ? chaosB : chaosR);
                float sB = (s == 0) ? chaosB : (s == 1 ? chaosR : chaosG);
                sendBatch(spiral, sR, sG, sB, 0.6F, particleLifetime);
            }
        }

        // Radial lines with varying point count
        if (age >= 6) {
            int lineCount = 4 + (age % 5);
            int pointsPerLine = 5 + (age % 4);
            double inner = 1.5 * scaleFactor;
            double outer = 4.5 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildRadialLines(
                    cx, cy, cz, inner, outer, lineCount, pointsPerLine,
                    rotation + (age % 7) * 0.3, UP);
            sendBatch(entries, chaosG, chaosB, chaosR, 0.5F, particleLifetime);
        }

        // Random vanilla particles — cycle based on age % 5
        if (age >= 4) {
            String particleType = VANILLA_PARTICLE_CYCLE[age % 5];
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> vanillaEntries = new ArrayList<>();
            int count = 4 + rng.nextInt(4);
            for (int i = 0; i < count; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 5.0 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                double vx = (rng.nextDouble() - 0.5) * 0.06;
                double vy = (rng.nextDouble() - 0.5) * 0.06;
                double vz = (rng.nextDouble() - 0.5) * 0.06;
                vanillaEntries.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.1 + rng.nextDouble() * 0.5, pz, vx, vy, vz));
            }
            sendVanillaBatch(vanillaEntries, particleType);
        }

        // Random velocity particles scattered every tick (3-5 particles)
        {
            List<S2CParticleBatchPack.ParticleEntry> scattered = new ArrayList<>();
            int scatterCount = 3 + rng.nextInt(3);
            for (int i = 0; i < scatterCount; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 5.5 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                float vx = (rng.nextFloat() - 0.5F) * 0.1F;
                float vy = (rng.nextFloat() - 0.5F) * 0.08F;
                float vz = (rng.nextFloat() - 0.5F) * 0.1F;
                scattered.add(new S2CParticleBatchPack.ParticleEntry(
                        px, cy + rng.nextDouble() * 0.8, pz, vx, vy, vz));
            }
            float randR = rng.nextFloat();
            float randG = rng.nextFloat();
            float randB = rng.nextFloat();
            sendBatch(scattered, randR, randG, randB, 0.6F, 8);
        }

        // Inner chaotic polygon
        {
            int sides = 3 + (age % 6);
            double innerRadius = 2.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildPolygon(
                    cx, cy, cz, innerRadius, sides, 8, rotation * 2.0, UP);
            sendBatch(entries, chaosB, chaosR, chaosG, 0.5F, particleLifetime);
        }

        // Flashing random-colored center
        {
            float flashR = rng.nextFloat();
            float flashG = rng.nextFloat();
            float flashB = rng.nextFloat();
            float pulseScale = 0.5F + 0.4F * (float) Math.sin(age * 0.6);
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy + 0.1, cz, 0.5 * scaleFactor, 10, age * 0.7, UP);
            sendBatch(entries, flashR, flashG, flashB, pulseScale + 0.5F, particleLifetime);
        }

        // Sound — random every 10 ticks
        if (age % 10 == 0) {
            SoundEvent sound = CHAOS_SOUNDS[rng.nextInt(CHAOS_SOUNDS.length)];
            float pitch = 0.5F + rng.nextFloat() * 1.5F;
            level.playSound(null, BlockPos.containing(center), sound,
                    SoundSource.BLOCKS, 0.1F, pitch);
        }
    }

    @Override
    protected void tickEffect(float scaleFactor) {
        if (age < 10) return;

        double r = EFFECT_RADIUS;
        AABB area = new AABB(center.x - r, center.y - 2, center.z - r,
                center.x + r, center.y + 4, center.z + r);

        List<Mob> mobs = getMobsInRadius(Mob.class, EFFECT_RADIUS);

        // Random chaos effect every 10 ticks
        if (age % Math.max(4, 10 - specialLevel) == 0) {
            for (Mob mob : mobs) {
                int effect = rng.nextInt(6);
                switch (effect) {
                    case 0 -> {
                        // Damage
                        hurtCompat(mob, level.damageSources().magic(), 3.0F * powerMultiplier);
                    }
                    case 1 -> {
                        // Random teleport 5-10 blocks
                        double dist = 5.0 + rng.nextDouble() * 5.0;
                        double angle = rng.nextDouble() * Math.PI * 2;
                        double newX = mob.getX() + dist * Math.cos(angle);
                        double newZ = mob.getZ() + dist * Math.sin(angle);
                        mob.teleportTo(newX, mob.getY(), newZ);
                    }
                    case 2 -> {
                        // Random potion debuff
                        MobEffect debuff = RANDOM_DEBUFFS[rng.nextInt(RANDOM_DEBUFFS.length)];
                        mob.addEffect(new MobEffectInstance(debuff, 60, rng.nextInt(2)));
                    }
                    case 3 -> {
                        // Set on fire 2s
                        mob.setSecondsOnFire(2);
                    }
                    case 4 -> {
                        // Freeze 40 ticks
                        mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), 40));
                    }
                    case 5 -> {
                        // Levitation 20 ticks
                        mob.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 1));
                    }
                }
            }
        }

        // 20% chance every 20 ticks to buff a random mob
        if (age % 20 == 0 && !mobs.isEmpty() && rng.nextFloat() < 0.2F) {
            Mob luckyMob = mobs.get(rng.nextInt(mobs.size()));
            MobEffect buff = RANDOM_BUFFS[rng.nextInt(RANDOM_BUFFS.length)];
            luckyMob.addEffect(new MobEffectInstance(buff, 100, 1));
        }
    }

    @Override
    protected void onRemove() {
    }
}
