package com.huige233.transcend.client.magic;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BloodCircleEffect extends AbstractMagicCircle {

    private static final double EFFECT_RADIUS = 6.0;

    public BloodCircleEffect(ServerLevel level, Vec3 center) {
        super(level, center);
    }

    @Override
    protected void tickParticles(float scaleFactor) {
        double cx = center.x, cy = center.y, cz = center.z;
        double rotSpeed = Math.PI / 50.0;
        double rotation = age * rotSpeed;
        double counterRotation = -age * rotSpeed * 1.1;
        int particleLifetime = 5;

        float bloodR = 0.6F, bloodG = 0.0F, bloodB = 0.05F;
        float crimsonR = 0.8F, crimsonG = 0.1F, crimsonB = 0.1F;

        // Outer ring — dark red, 80pts
        {
            double radius = 6.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 80, rotation, UP);
            sendBatch(entries, bloodR, bloodG, bloodB, 0.8F, particleLifetime);
        }

        // Inverted pentagram — star(5,2) with offset rotation for inverted appearance
        {
            double radius = 5.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildStar(
                    cx, cy, cz, radius, 5, 2, 15, rotation + Math.PI, UP);
            sendBatch(entries, crimsonR, crimsonG, crimsonB, 1.0F, particleLifetime);
        }

        // 8 radial lines from inner to outer in crimson
        if (age >= 6) {
            double inner = 1.5 * scaleFactor;
            double outer = 5.5 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildRadialLines(
                    cx, cy, cz, inner, outer, 8, 8, rotation + Math.PI / 8.0, UP);
            sendBatch(entries, crimsonR, crimsonG, crimsonB, 0.6F, particleLifetime);
        }

        // Inner pulsing circle
        {
            double radius = 2.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 40, counterRotation * 1.5, UP);
            sendBatch(entries, 0.9F, 0.05F, 0.05F, 0.5F, particleLifetime);
        }

        // Dotted arcs in dark red
        if (age >= 5) {
            double arcRadius = 6.3 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildDottedCircle(
                    cx, cy, cz, arcRadius, 5, 8, 0.35, counterRotation * 0.6, UP);
            sendBatch(entries, bloodR, bloodG, bloodB, 0.5F, particleLifetime);
        }

        // Red particles with DOWNWARD velocity — dripping blood effect every 2 ticks
        if (age % 2 == 0) {
            Random rng = new Random();
            List<S2CParticleBatchPack.ParticleEntry> drips = new ArrayList<>();
            int count = 8 + rng.nextInt(5);
            for (int i = 0; i < count; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 5.0 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                drips.add(new S2CParticleBatchPack.ParticleEntry(
                        px, cy + 0.5 + rng.nextDouble() * 1.0, pz,
                        0, (float) (-0.03 - rng.nextDouble() * 0.04), 0));
            }
            sendBatch(drips, crimsonR, crimsonG, crimsonB, 0.5F, 15);
        }

        // Floating red runes every 3 ticks
        if (age >= 8 && age % 3 == 0) {
            Random rng = new Random();
            List<S2CRuneBatchPack.RuneEntry> runes = new ArrayList<>();
            int runeCount = 6 + rng.nextInt(3);
            for (int i = 0; i < runeCount; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = 2.0 + rng.nextDouble() * 3.5 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                runes.add(new S2CRuneBatchPack.RuneEntry(
                        px, cy + 0.1 + rng.nextDouble() * 0.4, pz,
                        0, 0.02 + rng.nextDouble() * 0.02, 0));
            }
            sendRuneBatch(runes, crimsonR, bloodG, bloodB, 0.8F, 25, true);
        }

        // Witch particles every 4 ticks — corruption feel
        if (age >= 6 && age % 4 == 0) {
            Random rng = new Random();
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> witchParts = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 4.5 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                witchParts.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.1 + rng.nextDouble() * 0.3, pz, 0, 0.01, 0));
            }
            sendVanillaBatch(witchParts, "witch");
        }

        // Pentagram tip circles
        if (age >= 10) {
            double starRadius = 5.0 * scaleFactor;
            for (int i = 0; i < 5; i++) {
                double angle = rotation + Math.PI + 2.0 * Math.PI * i / 5;
                double px = cx + starRadius * Math.cos(angle);
                double pz = cz + starRadius * Math.sin(angle);
                List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                        px, cy, pz, 0.35 * scaleFactor, 8, counterRotation * 2, UP);
                sendBatchAsGlitter(entries, crimsonR, crimsonG, crimsonB, 0.4F, particleLifetime);
            }
        }

        // Pulsing scarlet core
        {
            float pulseScale = 0.4F + 0.3F * (float) Math.sin(age * 0.45);
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy + 0.1, cz, 0.5 * scaleFactor, 12, age * 0.5, UP);
            sendBatch(entries, 1.0F, 0.1F, 0.1F, pulseScale + 0.5F, particleLifetime);
        }

        // Sound — wither hurt every 25 ticks
        if (age % 25 == 0) {
            level.playSound(null, BlockPos.containing(center), SoundEvents.WITHER_HURT,
                    SoundSource.BLOCKS, 0.15F, 0.6F);
        }
    }

    @Override
    protected void tickEffect(float scaleFactor) {
        if (age < 10) return;

        double r = EFFECT_RADIUS;
        AABB area = new AABB(center.x - r, center.y - 2, center.z - r,
                center.x + r, center.y + 4, center.z + r);

        List<Mob> mobs = getMobsInRadius(Mob.class, EFFECT_RADIUS);

        float totalDamageDealt = 0;

        for (Mob mob : mobs) {
            // Magic damage every 15 ticks
            if (age % 15 == 0) {
                float damage = 2.5F * powerMultiplier;
                hurtCompat(mob, level.damageSources().magic(), damage);
                totalDamageDealt += damage;
            }

            // Hunger I every 40 ticks
            if (age % 40 == 0) {
                mob.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 0));
            }
        }

        // Lifesteal: heal nearest player for 25% of damage dealt
        if (age % 15 == 0 && totalDamageDealt > 0) {
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
                    nearest.heal(totalDamageDealt * (0.25F + specialLevel * 0.08F));
                }
            }
        }
    }

    @Override
    protected void onRemove() {
    }
}
