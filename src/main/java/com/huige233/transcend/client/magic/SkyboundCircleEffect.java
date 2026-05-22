package com.huige233.transcend.client.magic;

import com.huige233.transcend.network.S2CParticleBatchPack;
import com.huige233.transcend.network.S2CRuneBatchPack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SkyboundCircleEffect extends AbstractMagicCircle {

    private static final double EFFECT_RADIUS = 8.0;
    private static final double MAX_HEIGHT = 2.0;

    public SkyboundCircleEffect(ServerLevel level, Vec3 center) {
        super(level, center);
    }

    @Override
    protected void tickParticles(float scaleFactor) {
        double cx = center.x, cy = center.y, cz = center.z;
        double rotSpeed = Math.PI / 40.0;
        double rotation = age * rotSpeed;
        int particleLifetime = 4;

        float redR = 0.9F, redG = 0.1F, redB = 0.1F;
        float darkRedR = 0.6F, darkRedG = 0.05F, darkRedB = 0.05F;

        {
            double radius = 6.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 80, rotation, UP);
            sendBatch(entries, redR, redG, redB, 0.8F, particleLifetime);
        }

        {
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCross(
                    cx, cy, cz, 4.0 * scaleFactor, 0.3 * scaleFactor, 10,
                    rotation * 0.3, UP);
            sendBatch(entries, darkRedR, darkRedG, darkRedB, 0.9F, particleLifetime);
        }

        if (age >= 5) {
            double arcRadius = 6.5 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildDottedCircle(
                    cx, cy, cz, arcRadius, 8, 6, 0.3, -rotation * 0.8, UP);
            sendBatch(entries, redR, redG, redB, 0.5F, particleLifetime);
        }

        if (age >= 8) {
            double inner = 2.0 * scaleFactor;
            double outer = 4.5 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildRadialLines(
                    cx, cy, cz, inner, outer, 4, 8, rotation + Math.PI / 4.0, UP);
            sendBatch(entries, darkRedR, darkRedG, darkRedB, 0.6F, particleLifetime);
        }

        {
            double radius = 2.0 * scaleFactor;
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy, cz, radius, 40, -rotation * 1.5, UP);
            sendBatch(entries, accentR, accentG, accentB, 0.5F, particleLifetime);
        }

        {
            float pulseScale = 0.4F + 0.3F * (float) Math.sin(age * 0.4);
            List<S2CParticleBatchPack.ParticleEntry> entries = MagicCircleGeometry.buildCircle(
                    cx, cy + 0.1, cz, 0.5 * scaleFactor, 10, age * 0.6, UP);
            sendBatch(entries, darkRedR, darkRedG, darkRedB, pulseScale + 0.5F, particleLifetime);
        }

        if (age >= 10) {
            List<LivingEntity> highEntities = getHighEntities();

            for (LivingEntity entity : highEntities) {
                double ex = entity.getX(), ey = entity.getY(), ez = entity.getZ();

                List<S2CParticleBatchPack.ParticleEntry> chain = MagicCircleGeometry.buildLine(
                        cx, cy + 0.5, cz, ex, ey + entity.getBbHeight() * 0.5, ez, 20);
                sendBatch(chain, 1.0F, 0.15F, 0.1F, 0.9F, 3);

                double entityRot = age * Math.PI / 20.0;
                double ringRadius = entity.getBbWidth() * 0.8 + 0.3;
                double ringY = ey + entity.getBbHeight() * 0.5;
                List<S2CParticleBatchPack.ParticleEntry> ring = MagicCircleGeometry.buildCircle(
                        ex, ringY, ez, ringRadius, 24, entityRot, UP);
                sendBatch(ring, 1.0F, 0.2F, 0.1F, 0.7F, 3);

                List<S2CParticleBatchPack.ParticleEntry> ring2 = MagicCircleGeometry.buildCircle(
                        ex, ringY - 0.3, ez, ringRadius * 0.8, 18, -entityRot * 1.3, UP);
                sendBatch(ring2, 0.8F, 0.1F, 0.05F, 0.5F, 3);

                if (age % 3 == 0) {
                    List<S2CParticleBatchPack.ParticleEntry> hexagram = new ArrayList<>();
                    double hexR = ringRadius * 1.2;
                    hexagram.addAll(MagicCircleGeometry.buildPolygon(
                            ex, ringY, ez, hexR, 3, 6, entityRot, UP));
                    hexagram.addAll(MagicCircleGeometry.buildPolygon(
                            ex, ringY, ez, hexR, 3, 6, entityRot + Math.PI / 3.0, UP));
                    sendBatch(hexagram, 1.0F, 0.3F, 0.15F, 0.6F, 4);
                }

                if (age % 4 == 0) {
                    Random rng = new Random();
                    List<S2CRuneBatchPack.RuneEntry> runes = new ArrayList<>();
                    for (int i = 0; i < 4; i++) {
                        double runeAngle = entityRot + Math.PI * i / 2.0;
                        double rx = ex + ringRadius * 1.1 * Math.cos(runeAngle);
                        double rz = ez + ringRadius * 1.1 * Math.sin(runeAngle);
                        runes.add(new S2CRuneBatchPack.RuneEntry(
                                rx, ringY + 0.1, rz, 0, 0, 0));
                    }
                    sendRuneBatch(runes, 1.0F, 0.2F, 0.1F, 0.7F, 5, true);
                }

                if (age % 5 == 0) {
                    List<S2CParticleBatchPack.ParticleEntry> radials = MagicCircleGeometry.buildRadialLines(
                            ex, ringY, ez, ringRadius * 0.3, ringRadius, 6, 4, entityRot * 0.7, UP);
                    sendBatch(radials, 0.9F, 0.15F, 0.1F, 0.5F, 3);
                }
            }
        }

        if (age % 30 == 0) {
            level.playSound(null, BlockPos.containing(center), SoundEvents.CHAIN_PLACE,
                    SoundSource.BLOCKS, 0.3F, 0.6F);
        }
    }

    private List<LivingEntity> getHighEntities() {
        double cx = center.x, cy = center.y, cz = center.z;
        AABB scanArea = new AABB(cx - EFFECT_RADIUS, cy - 2, cz - EFFECT_RADIUS,
                cx + EFFECT_RADIUS, cy + 20, cz + EFFECT_RADIUS);

        return level.getEntitiesOfClass(LivingEntity.class, scanArea, entity -> {
            if (!entity.isAlive()) return false;
            if (entity instanceof Mob && !shouldAffect(entity)) return false;
            double dx = entity.getX() - cx;
            double dz = entity.getZ() - cz;
            return Math.sqrt(dx * dx + dz * dz) <= EFFECT_RADIUS
                    && entity.getY() > cy + MAX_HEIGHT;
        });
    }

    @Override
    protected void tickEffect(float scaleFactor) {
        if (age < 10) return;

        List<LivingEntity> entities = getHighEntities();
        double pullForce = (0.25 + specialLevel * 0.05) * powerMultiplier;

        for (LivingEntity entity : entities) {
            double excess = entity.getY() - (center.y + MAX_HEIGHT);
            double scaledForce = pullForce * Math.min(excess / 3.0, 2.0);
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, -scaledForce, 0));
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.4, 1.0, 0.4));

            if (age % 20 == 0) {
                hurtCompat(entity, level.damageSources().magic(), 1.0F * powerMultiplier);
            }
        }
    }

    @Override
    protected void onRemove() {
    }
}
