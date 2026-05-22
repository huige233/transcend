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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SanctumCircleEffect extends AbstractMagicCircle {

    private static final double EFFECT_RADIUS = 5.5;

    public SanctumCircleEffect(ServerLevel level, Vec3 center) {
        super(level, center);
    }

    @Override
    protected void tickParticles(float scaleFactor) {
        double cx = center.x, cy = center.y, cz = center.z;
        double rotSpeed = Math.PI / 45.0;
        double rotation = age * rotSpeed;
        double counterRotation = -age * rotSpeed * 0.9;
        int particleLifetime = 5;

        {
            double radius = 5.5 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 80, rotation, UP);
            sendBatch(entries, baseR, baseG, baseB, 0.8F, particleLifetime);
        }

        {
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCross(
                    cx, cy, cz, 4.5 * scaleFactor, 0.5 * scaleFactor, 12,
                    rotation * 0.3, UP);
            sendBatch(entries, accentR, accentG, accentB, 0.9F, particleLifetime);
        }

        if (age >= 10) {
            double runeRadius = 5.0 * scaleFactor;
            List<S2CRuneBatchPack.RuneEntry> runes = new ArrayList<>();
            int runeCount = 12;
            for (int i = 0; i < runeCount; i++) {
                double angle = rotation * 0.5 + 2.0 * Math.PI * i / runeCount;
                double px = cx + runeRadius * Math.cos(angle);
                double pz = cz + runeRadius * Math.sin(angle);
                runes.add(new S2CRuneBatchPack.RuneEntry(px, cy + 0.15, pz, 0, 0, 0));
            }
            sendRuneBatch(runes, 0.3F, 0.9F, 0.3F, 0.9F, 3, true);
        }

        {
            double radius = 2.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 40, counterRotation * 1.5, UP);
            sendBatch(entries, accentR, accentG, accentB, 0.5F, particleLifetime);
        }

        if (age >= 5) {
            double arcRadius = 5.8 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildDottedCircle(
                    cx, cy, cz, arcRadius, 4, 10, 0.2, rotation + Math.PI / 4.0, UP);
            sendBatch(entries, baseR, baseG, baseB, 0.5F, particleLifetime);
        }

        if (age >= 8 && age % 2 == 0) {
            Random rng = new Random();
            List<S2CParticleBatchPack.ParticleEntry> heals = new ArrayList<>();
            int count = 4 + rng.nextInt(3);
            for (int i = 0; i < count; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 4.0 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                double vy = 0.04 + rng.nextDouble() * 0.03;
                heals.add(new S2CParticleBatchPack.ParticleEntry(
                        px, cy + 0.1, pz, 0, (float) vy, 0));
            }
            sendBatchAsGlitter(heals, 0.4F, 1.0F, 0.5F, 0.4F, 25);
        }

        {
            float pulseScale = 0.3F + 0.2F * (float) Math.sin(age * 0.35);
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy + 0.1, cz, 0.7 * scaleFactor, 14, age * 0.4, UP);
            sendBatch(entries, 1.0F, 1.0F, 0.8F, pulseScale + 0.5F, particleLifetime);
        }

        // === Enhanced effects ===

        // --- Enchant vanilla particles rising within radius ---
        if (age % 3 == 0) {
            Random rng = new Random();
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> enchants = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 4.0 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                enchants.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.1, pz, 0, 0.04, 0));
            }
            sendVanillaBatch(enchants, "enchant");
        }

        // --- End rod vanilla particles at cross tips ---
        if (age % 5 == 0 && age >= 10) {
            double armLen = 4.5 * scaleFactor;
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> endRods = new ArrayList<>();
            for (int arm = 0; arm < 4; arm++) {
                double armAngle = rotation * 0.3 + arm * Math.PI / 2.0;
                double tipX = cx + armLen * Math.cos(armAngle);
                double tipZ = cz + armLen * Math.sin(armAngle);
                endRods.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        tipX, cy + 0.2, tipZ, 0, 0.03, 0));
            }
            sendVanillaBatch(endRods, "end_rod");
        }

        // --- Golden halo ring at Y+0.3, gentle rotation ---
        {
            double haloRadius = 3.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> halo = MagicCircleGeometry.buildCircle(
                    cx, cy + 0.3, cz, haloRadius, 30, age * Math.PI / 80.0, UP);
            sendBatch(halo, 1.0F, 0.85F, 0.3F, 0.5F, particleLifetime);
        }

        // --- Amethyst chime sound ---
        if (age % 30 == 0) {
            level.playSound(null, BlockPos.containing(center), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.4F, 1.5F);
        }
    }

    @Override
    protected void tickEffect(float scaleFactor) {
        if (age < 10) return;

        double effectRadius = EFFECT_RADIUS + specialLevel * 0.5;
        double r = effectRadius;
        AABB area = new AABB(center.x - r, center.y - 2, center.z - r,
                center.x + r, center.y + 4, center.z + r);

        List<Player> players = level.getEntitiesOfClass(Player.class, area, player -> {
            if (!player.isAlive()) return false;
            double dx = player.getX() - center.x;
            double dz = player.getZ() - center.z;
            return Math.sqrt(dx * dx + dz * dz) <= effectRadius;
        });

        for (Player player : players) {
            if (age % 20 == 0) {
                player.heal(1.0F * powerMultiplier);
            }
            if (age % 40 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
            }
        }
    }

    @Override
    protected void onRemove() {
    }
}
