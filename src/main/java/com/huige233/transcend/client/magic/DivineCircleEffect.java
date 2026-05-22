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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DivineCircleEffect extends AbstractMagicCircle {

    private static final double EFFECT_RADIUS = 6.5;

    public DivineCircleEffect(ServerLevel level, Vec3 center) {
        super(level, center);
    }

    @Override
    protected void tickParticles(float scaleFactor) {
        double cx = center.x, cy = center.y, cz = center.z;
        double rotSpeed = Math.PI / 40.0;
        double rotation = age * rotSpeed;
        double counterRotation = -age * rotSpeed * 0.8;
        int particleLifetime = 5;

        float goldR = 1.0F, goldG = 0.9F, goldB = 0.4F;
        float brightR = 1.0F, brightG = 1.0F, brightB = 0.85F;

        // Outer ring — bright gold, 120pts
        {
            double radius = 6.5 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 120, rotation, UP);
            sendBatch(entries, goldR, goldG, goldB, 0.8F, particleLifetime);
        }

        // Double cross — 8 arms total
        // First cross: 4 arms at armLength 5.5
        {
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCross(
                    cx, cy, cz, 5.5 * scaleFactor, 0.4 * scaleFactor, 12,
                    rotation * 0.3, UP);
            sendBatch(entries, brightR, brightG, brightB, 0.9F, particleLifetime);
        }

        // Second cross: 4 arms rotated 45deg at armLength 4.0
        {
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCross(
                    cx, cy, cz, 4.0 * scaleFactor, 0.35 * scaleFactor, 10,
                    rotation * 0.3 + Math.PI / 4.0, UP);
            sendBatch(entries, goldR, goldG, goldB, 0.8F, particleLifetime);
        }

        // 3 concentric circles at radii 5.0, 3.5, 2.0
        {
            List<S2CParticleBatchPack.ParticleEntry> ring1 = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, 5.0 * scaleFactor, 50, counterRotation, UP);
            sendBatch(ring1, goldR, goldG, goldB, 0.6F, particleLifetime);

            List<S2CParticleBatchPack.ParticleEntry> ring2 = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, 3.5 * scaleFactor, 40, rotation * 1.2, UP);
            sendBatch(ring2, brightR, brightG, brightB, 0.5F, particleLifetime);

            List<S2CParticleBatchPack.ParticleEntry> ring3 = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, 2.0 * scaleFactor, 30, counterRotation * 1.5, UP);
            sendBatch(ring3, goldR, goldG, goldB, 0.5F, particleLifetime);
        }

        // 16 golden runes in outer ring
        if (age >= 8 && age % 3 == 0) {
            List<S2CRuneBatchPack.RuneEntry> runes = new ArrayList<>();
            double runeRadius = 5.8 * scaleFactor;
            for (int i = 0; i < 16; i++) {
                double angle = rotation * 0.4 + 2.0 * Math.PI * i / 16;
                double px = cx + runeRadius * Math.cos(angle);
                double pz = cz + runeRadius * Math.sin(angle);
                runes.add(new S2CRuneBatchPack.RuneEntry(px, cy + 0.15, pz, 0, 0.02, 0));
            }
            sendRuneBatch(runes, goldR, goldG, 0.2F, 0.9F, 20, true);
        }

        // Enchant particles every 2 ticks — 10 pts, strong upward
        if (age % 2 == 0) {
            Random rng = new Random();
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> enchants = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 5.0 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                enchants.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.1, pz, 0, 0.08 + rng.nextDouble() * 0.06, 0));
            }
            sendVanillaBatch(enchants, "enchant");
        }

        // End rod particles at 8 cross tips every 4 ticks
        if (age >= 10 && age % 4 == 0) {
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> endRods = new ArrayList<>();
            double tipRadius = 5.5 * scaleFactor;
            double crossAngle = rotation * 0.3;
            for (int i = 0; i < 8; i++) {
                double angle = crossAngle + Math.PI / 4.0 * i;
                double px = cx + tipRadius * Math.cos(angle);
                double pz = cz + tipRadius * Math.sin(angle);
                endRods.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.2, pz, 0, 0.03, 0));
            }
            sendVanillaBatch(endRods, "end_rod");
        }

        // Halo ring at Y+0.5, radius 4.0
        {
            double haloRadius = 4.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> halo = MagicCircleGeometry.buildCircle(
                    cx, cy + 0.5, cz, haloRadius, 40, counterRotation * 0.7, UP);
            sendBatchAsGlitter(halo, brightR, brightG, brightB, 0.4F, particleLifetime);
        }

        // Bright white-gold core
        {
            float pulseScale = 0.5F + 0.3F * (float) Math.sin(age * 0.35);
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy + 0.1, cz, 0.6 * scaleFactor, 14, age * 0.5, UP);
            sendBatch(entries, brightR, brightG, brightB, pulseScale + 0.6F, particleLifetime);
        }

        // Sound — amethyst chime every 15 ticks
        if (age % 15 == 0) {
            level.playSound(null, BlockPos.containing(center), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 0.5F, 1.8F);
        }
    }

    @Override
    protected void tickEffect(float scaleFactor) {
        if (age < 10) return;

        double effectRadius = EFFECT_RADIUS + specialLevel * 0.5;
        double r = effectRadius;
        AABB area = new AABB(center.x - r, center.y - 2, center.z - r,
                center.x + r, center.y + 4, center.z + r);

        // Get all living entities (players AND mobs)
        List<LivingEntity> livingEntities = level.getEntitiesOfClass(LivingEntity.class, area, entity -> {
            if (!entity.isAlive()) return false;
            if (entity instanceof Mob && !shouldAffect(entity)) return false;
            double dx = entity.getX() - center.x;
            double dz = entity.getZ() - center.z;
            return Math.sqrt(dx * dx + dz * dz) <= effectRadius;
        });

        for (LivingEntity entity : livingEntities) {
            // Heal ALL living entities every 20 ticks
            if (age % 20 == 0) {
                entity.heal(1.5F * powerMultiplier);
            }

            // Glowing to all entities for 40 ticks every 20 ticks (holy revelation)
            if (age % 20 == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0));
            }

            // Player-specific buffs
            if (entity instanceof Player player) {
                // Regeneration II every 30 ticks for 60 ticks
                if (age % 30 == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1));
                }

                // Damage Resistance I every 40 ticks for 60 ticks
                if (age % 40 == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0));
                }
            }
        }
    }

    @Override
    protected void onRemove() {
    }
}
