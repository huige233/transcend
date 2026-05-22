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

public class TerraCircleEffect extends AbstractMagicCircle {

    private static final double EFFECT_RADIUS = 6.0;

    public TerraCircleEffect(ServerLevel level, Vec3 center) {
        super(level, center);
    }

    @Override
    protected void tickParticles(float scaleFactor) {
        double cx = center.x, cy = center.y, cz = center.z;
        double rotSpeed = Math.PI / 60.0;
        double rotation = age * rotSpeed;
        int particleLifetime = 5;

        {
            double radius = 6.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 80, rotation, UP);
            sendBatch(entries, baseR, baseG, baseB, 0.8F, particleLifetime);
        }

        {
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildSquareMandala(
                    cx, cy, cz, 3, 5.5 * scaleFactor, 15, rotation, UP);
            float r1 = baseR * 0.6F + accentR * 0.4F;
            float g1 = baseG * 0.6F + accentG * 0.4F;
            float b1 = baseB * 0.6F + accentB * 0.4F;
            sendBatch(entries, r1, g1, b1, 0.7F, particleLifetime);
        }

        if (age >= 10) {
            double cornerR = 5.5 * scaleFactor / Math.sqrt(2);
            for (int i = 0; i < 4; i++) {
                double angle = rotation + Math.PI / 4.0 + i * Math.PI / 2.0;
                double px = cx + cornerR * Math.cos(angle);
                double pz = cz + cornerR * Math.sin(angle);
                List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                        px, cy, pz, 0.4 * scaleFactor, 10, -rotation * 2, UP);
                sendBatchAsGlitter(entries, accentR, accentG, accentB, 0.4F, particleLifetime);
            }
        }

        if (age >= 8) {
            double runeRadius = 5.2 * scaleFactor;
            List<S2CRuneBatchPack.RuneEntry> runes = new ArrayList<>();
            int runeCount = 8;
            for (int i = 0; i < runeCount; i++) {
                double angle = rotation * 0.3 + 2.0 * Math.PI * i / runeCount;
                double px = cx + runeRadius * Math.cos(angle);
                double pz = cz + runeRadius * Math.sin(angle);
                runes.add(new S2CRuneBatchPack.RuneEntry(px, cy + 0.15, pz, 0, 0, 0));
            }
            sendRuneBatch(runes, 0.7F, 0.55F, 0.15F, 0.9F, 3, true);
        }

        {
            double radius = 1.5 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 30, -rotation * 0.5, UP);
            sendBatch(entries, accentR, accentG, accentB, 0.5F, particleLifetime);
        }

        if (age >= 10 && age % 5 == 0) {
            Random rng = new Random();
            List<S2CParticleBatchPack.ParticleEntry> stones = new ArrayList<>();
            int count = 3 + rng.nextInt(2);
            for (int i = 0; i < count; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 4.0 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                stones.add(new S2CParticleBatchPack.ParticleEntry(
                        px, cy + 0.05, pz, 0, 0.01F, 0));
            }
            sendBatch(stones, 0.5F, 0.4F, 0.2F, 0.6F, 20);
        }

        {
            float pulseScale = 0.25F + 0.1F * (float) Math.sin(age * 0.2);
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy + 0.1, cz, 0.5 * scaleFactor, 10, age * 0.3, UP);
            sendBatch(entries, 0.6F, 0.5F, 0.2F, pulseScale + 0.5F, particleLifetime);
        }

        // === Enhanced effects ===

        // --- Enchant vanilla particles near ground ---
        if (age % 6 == 0) {
            Random rng = new Random();
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> enchants = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 4.0 * scaleFactor;
                double px = cx + dist * Math.cos(angle);
                double pz = cz + dist * Math.sin(angle);
                enchants.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.05, pz, 0, 0.005, 0));
            }
            sendVanillaBatch(enchants, "enchant");
        }

        // --- Second mandala layer: inner square mandala in accent color ---
        {
            List<S2CParticleBatchPack.ParticleEntry> mandala2 = MagicCircleGeometry.buildSquareMandala(
                    cx, cy, cz, 2, 2.5 * scaleFactor, 10, -rotation * 0.8, UP);
            sendBatch(mandala2, accentR, accentG, accentB, 0.5F, particleLifetime);
        }

        // --- Ground impact particles: ring pulsing outward ---
        if (age % 8 == 0) {
            List<S2CParticleBatchPack.ParticleEntry> impact = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                double angle = 2.0 * Math.PI * i / 8 + rotation;
                double px = cx + 2.0 * scaleFactor * Math.cos(angle);
                double pz = cz + 2.0 * scaleFactor * Math.sin(angle);
                float vx = (float) (Math.cos(angle) * 0.02);
                float vz = (float) (Math.sin(angle) * 0.02);
                impact.add(new S2CParticleBatchPack.ParticleEntry(px, cy + 0.02, pz, vx, 0, vz));
            }
            sendBatch(impact, 0.5F, 0.4F, 0.15F, 0.5F, 10);
        }

        // --- Anvil land sound ---
        if (age % 35 == 0) {
            level.playSound(null, BlockPos.containing(center), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.1F, 0.3F);
        }
    }

    @Override
    protected void tickEffect(float scaleFactor) {
        if (age < 10) return;

        double r = EFFECT_RADIUS;
        AABB area = new AABB(center.x - r, center.y - 2, center.z - r,
                center.x + r, center.y + 4, center.z + r);

        if (age % 40 == 0) {
            List<Player> players = level.getEntitiesOfClass(Player.class, area, player -> {
                if (!player.isAlive()) return false;
                double dx = player.getX() - center.x;
                double dz = player.getZ() - center.z;
                return Math.sqrt(dx * dx + dz * dz) <= EFFECT_RADIUS;
            });
            for (Player player : players) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, Math.min(specialLevel / 2, 2)));
            }
        }

        if (age % 20 == 0) {
            List<Mob> mobs = getMobsInRadius(Mob.class, EFFECT_RADIUS);
            for (Mob mob : mobs) {
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                mob.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 0));
            }
        }
    }

    @Override
    protected void onRemove() {
    }
}
