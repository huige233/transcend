package com.huige233.transcend.entity.boss;

import com.huige233.transcend.TranscendGameRules;
import com.huige233.transcend.network.S2CParticleBatchPack;
import com.huige233.transcend.network.S2CVanillaParticleBatchPack;
import com.huige233.transcend.util.EntityCompatUtil;
import com.huige233.transcend.visual.ServerVisualBroadcaster;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

final class AvatarMeteorEffect {
    private final ServerLevel level;
    private final Vec3 center;
    private final float powerMultiplier;
    private final float radiusMultiplier;
    private final int specialLevel;
    private final int maxAge;
    private final UUID ownerUUID;
    private final Random rng = new Random();
    private int age;
    private boolean removed;
    private MeteorStrike meteor;

    AvatarMeteorEffect(ServerLevel level, Vec3 center, float powerMultiplier, float radiusMultiplier,
                       int specialLevel, int maxAge, UUID ownerUUID) {
        this.level = level;
        this.center = center;
        this.powerMultiplier = powerMultiplier;
        this.radiusMultiplier = radiusMultiplier;
        this.specialLevel = specialLevel;
        this.maxAge = maxAge;
        this.ownerUUID = ownerUUID;
    }

    boolean isRemoved() {
        return removed;
    }

    int age() {
        return age;
    }

    void tick() {
        if (removed) return;
        if (age >= maxAge) {
            meteor = null;
            removed = true;
            return;
        }

        float scaleFactor;
        if (age < 10) {
            scaleFactor = age / 10.0F;
        } else if (age > maxAge - 15) {
            scaleFactor = (maxAge - age) / 15.0F;
        } else {
            scaleFactor = 1.0F;
        }

        tickParticles(scaleFactor);
        tickEffect();
        if (age % 6 == 0) submitShaderCircle(scaleFactor);
        age++;
    }

    private void tickParticles(float scaleFactor) {
        double rot = age * Math.PI / 28.0;
        float r = 0.95F;
        float g = 0.20F + 0.20F * (float) Math.sin(age * 0.10F);
        List<S2CParticleBatchPack.ParticleEntry> outer = buildCircle(9.5 * scaleFactor, 120, rot);
        emitShaderFromDustBatch(outer, r, g, 0.10F, 0.85F, 6);

        if (age % 4 == 0) {
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> embers = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 9.0 * scaleFactor;
                double px = center.x + Math.cos(angle) * dist;
                double pz = center.z + Math.sin(angle) * dist;
                embers.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, center.y + 0.2 + rng.nextDouble() * 0.6, pz,
                        0, 0.02 + rng.nextDouble() * 0.04, 0));
            }
            Vec3 centroid = centroidVanilla(embers);
            ServerVisualBroadcaster.shockwave(level, centroid,
                    estimateRadiusFromVanilla(embers, centroid), 0.9F, 0.9F, 0.9F, 16);
        }
    }

    private void submitShaderCircle(float scale) {
        float radius = 10.0F * radiusMultiplier * Math.max(0.35F, scale);
        ServerVisualBroadcaster.circle(level, center, radius, 1.0F, 0.35F, 0.12F,
                18, 40, "pentagram");
        if ((age & 3) == 0) {
            ServerVisualBroadcaster.shieldRipple(level, center, radius * 0.85F,
                    1.0F, 0.65F, 0.2F, 16);
        }
        if (age % 6 == 0) {
            ServerVisualBroadcaster.circle(level, center.add(0, 0.05, 0), radius * 0.55F,
                    1.0F, 0.8F, 0.3F, 14, 28, "hexagram");
        }
        if (age % 12 == 0) {
            ServerVisualBroadcaster.shockwave(level, center, radius * 0.7F,
                    1.0F, 0.4F, 0.1F, 20);
        }
    }

    private void tickEffect() {
        if (meteor == null && age >= 10) scheduleMeteor();
        if (meteor != null && --meteor.fuse <= 0) {
            resolveMeteor(meteor);
            meteor = null;
        }
    }

    private void scheduleMeteor() {
        double angle = rng.nextDouble() * Math.PI * 2;
        double dist = rng.nextDouble() * (2.0 + radiusMultiplier * 1.75);
        double x = center.x + Math.cos(angle) * dist;
        double z = center.z + Math.sin(angle) * dist;
        double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z)) + 0.1;
        int fuse = specialLevel >= 4 ? 40 : 30;
        float radius = (specialLevel >= 4 ? 6.2F : 5.2F) * radiusMultiplier;
        double spawnY = y + 15.0;
        meteor = new MeteorStrike(x, y, z, fuse, radius, rng.nextInt(5));

        FallingBlockEntity meteorBlock = FallingBlockEntity.fall(level,
                BlockPos.containing(x, spawnY, z), Blocks.MAGMA_BLOCK.defaultBlockState());
        meteorBlock.setPos(x, spawnY, z);
        meteorBlock.time = 590;
        meteorBlock.dropItem = false;
        meteorBlock.setHurtsEntities(0.0F, 0);
        meteorBlock.setGlowingTag(true);
        level.addFreshEntity(meteorBlock);

        ServerVisualBroadcaster.beam(level, new Vec3(x, spawnY + 5, z), new Vec3(x, y + 0.5, z),
                1.0F, 0.4F, 0.1F, fuse + 5, "meteor");
        ServerVisualBroadcaster.shockwave(level, new Vec3(x, y + 0.1, z), radius * 0.5F,
                1.0F, 0.5F, 0.15F, fuse);
        level.playSound(null, BlockPos.containing(x, y, z), SoundEvents.BLAZE_SHOOT,
                SoundSource.HOSTILE, 2.5F, 0.3F);
    }

    private void resolveMeteor(MeteorStrike strike) {
        BlockPos pos = BlockPos.containing(strike.x, strike.y, strike.z);
        AABB cleanupArea = new AABB(strike.x - 3, strike.y - 2, strike.z - 3,
                strike.x + 3, strike.y + 20, strike.z + 3);
        for (Entity entity : level.getEntities((Entity) null, cleanupArea)) {
            if (entity instanceof FallingBlockEntity falling && !falling.isRemoved() && falling.time >= 585) {
                falling.discard();
            }
        }

        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 3.0F, 0.4F);
        level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 2.5F, 0.5F);
        level.playSound(null, pos, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, 0.6F);

        Vec3 impactCenter = new Vec3(strike.x, strike.y + 0.06, strike.z);
        ServerVisualBroadcaster.shockwave(level, impactCenter, strike.radius + 15.0F,
                1.0F, 0.2F, 0.02F, 45);
        ServerVisualBroadcaster.shockwave(level, impactCenter.add(0, 0.2, 0), strike.radius + 8.0F,
                1.0F, 0.5F, 0.1F, 35);
        ServerVisualBroadcaster.shockwave(level, impactCenter.add(0, 0.4, 0), strike.radius + 4.0F,
                1.0F, 0.8F, 0.3F, 25);
        ServerVisualBroadcaster.beam(level,
                new Vec3(strike.x, strike.y + strike.radius * 5.0, strike.z),
                new Vec3(strike.x, strike.y + 0.1, strike.z),
                1.0F, 0.35F, 0.08F, 40, "meteor");
        for (int i = 0; i < 3; i++) {
            double ox = (rng.nextDouble() - 0.5) * 2.0;
            double oz = (rng.nextDouble() - 0.5) * 2.0;
            ServerVisualBroadcaster.beam(level,
                    new Vec3(strike.x + ox, strike.y + strike.radius * 3.0 + i, strike.z + oz),
                    new Vec3(strike.x + ox, strike.y + 0.2, strike.z + oz),
                    1.0F, 0.6F, 0.15F, 30, "beam");
        }

        spawnDebris(strike);
        shakePlayers(strike);
        if (TranscendGameRules.canBossMassSpellGrief(level)) {
            level.explode(null, strike.x, strike.y, strike.z,
                    5.0F + rng.nextFloat() * 2.0F, Level.ExplosionInteraction.MOB);
        }
        damageTargets(strike);
    }

    private void spawnDebris(MeteorStrike strike) {
        int debrisCount = 12 + specialLevel * 4;
        for (int i = 0; i < debrisCount; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double distance = 1.0 + rng.nextDouble() * (strike.radius * 0.6);
            int bx = Mth.floor(strike.x + Math.cos(angle) * distance);
            int bz = Mth.floor(strike.z + Math.sin(angle) * distance);
            int by = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) - 1;
            BlockPos debrisPos = new BlockPos(bx, by, bz);
            var debrisState = level.getBlockState(debrisPos);
            if (debrisState.isAir() || debrisState.liquid()) continue;
            FallingBlockEntity debris = FallingBlockEntity.fall(level, debrisPos, debrisState);
            debris.setPos(bx + 0.5, by + 1.0, bz + 0.5);
            double launchAngle = Math.atan2(bz + 0.5 - strike.z, bx + 0.5 - strike.x);
            double horizontalSpeed = 0.3 + rng.nextDouble() * 0.6;
            debris.setDeltaMovement(Math.cos(launchAngle) * horizontalSpeed,
                    0.5 + rng.nextDouble() * 0.8, Math.sin(launchAngle) * horizontalSpeed);
            debris.time = 595;
            debris.dropItem = false;
            debris.setHurtsEntities(2.0F, 20);
            level.addFreshEntity(debris);
            if (TranscendGameRules.canBossMassSpellGrief(level)) level.destroyBlock(debrisPos, false);
        }
    }

    private void shakePlayers(MeteorStrike strike) {
        double shakeRange = strike.radius + 30.0;
        for (var player : level.getPlayers(p ->
                p.distanceToSqr(strike.x, strike.y, strike.z) < shakeRange * shakeRange)) {
            double kx = player.getX() - strike.x;
            double kz = player.getZ() - strike.z;
            double distance = Math.sqrt(kx * kx + kz * kz);
            float strength = (float) (1.5 * (1.0 - Math.min(1.0, distance / shakeRange)));
            if (distance > 0.1) player.knockback(strength * 0.7, -kx / distance, -kz / distance);
            else player.setDeltaMovement(player.getDeltaMovement().add(0, 0.6, 0));
            player.hurtMarked = true;
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3, false, false));
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket(
                    player.getId(), rng.nextFloat() * 360.0F));
        }
    }

    private void damageTargets(MeteorStrike strike) {
        double blastRadius = strike.radius + 8.0;
        AABB area = new AABB(strike.x - blastRadius, strike.y - 4.0, strike.z - blastRadius,
                strike.x + blastRadius, strike.y + 8.0, strike.z + blastRadius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> shouldAffect(entity) && !EntityCompatUtil.isProtectedPlayer(entity));
        for (LivingEntity target : targets) {
            double distanceSquared = target.distanceToSqr(strike.x, strike.y, strike.z);
            double maxDistanceSquared = blastRadius * blastRadius;
            if (distanceSquared > maxDistanceSquared) continue;
            float falloff = 1.0F - 0.6F * (float) Math.sqrt(distanceSquared / maxDistanceSquared);
            float damage = Math.max(8.0F, (25.0F + rng.nextFloat() * 20.0F) * powerMultiplier * falloff);
            target.hurt(adaptMeteorDamageSource(target, selectDamageSource(strike.damageMode)), damage);
            applySecondaryEffect(target, strike.damageMode);
            double kx = target.getX() - strike.x;
            double kz = target.getZ() - strike.z;
            double distance = Math.sqrt(kx * kx + kz * kz);
            if (distance > 0.1) {
                target.knockback(falloff * 1.5F, -kx / distance, -kz / distance);
                target.hurtMarked = true;
            }
        }
    }

    private boolean shouldAffect(Entity entity) {
        if (!entity.isAlive() || entity.getUUID().equals(ownerUUID)) return false;
        if (entity instanceof com.huige233.transcend.entity.SpellGuardian
                || entity instanceof com.huige233.transcend.entity.SpellWisp) return false;
        Entity owner = level.getEntity(ownerUUID);
        if (owner instanceof AbstractTranscendBoss ownerBoss && entity instanceof AbstractTranscendBoss targetBoss) {
            return ownerBoss.getFaction().isHostileTo(targetBoss.getFaction());
        }
        return true;
    }

    private DamageSource selectDamageSource(int mode) {
        return switch (mode) {
            case 0 -> level.damageSources().magic();
            case 1 -> level.damageSources().inFire();
            case 2 -> level.damageSources().lightningBolt();
            case 3 -> level.damageSources().wither();
            default -> level.damageSources().freeze();
        };
    }

    private DamageSource adaptMeteorDamageSource(LivingEntity target, DamageSource fallback) {
        return fallback;
    }

    private void applySecondaryEffect(LivingEntity target, int mode) {
        switch (mode) {
            case 0 -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true));
            case 1 -> target.setSecondsOnFire(4 + specialLevel);
            case 2 -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 1, false, true));
            case 3 -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 50, 1, false, true));
            default -> target.setTicksFrozen(Math.max(target.getTicksFrozen(), 80));
        }
    }

    private List<S2CParticleBatchPack.ParticleEntry> buildCircle(double radius, int points, double rotation) {
        List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double angle = rotation + 2.0 * Math.PI * i / points;
            entries.add(new S2CParticleBatchPack.ParticleEntry(
                    center.x + radius * Math.sin(angle), center.y, center.z + radius * Math.cos(angle)));
        }
        return entries;
    }

    private void emitShaderFromDustBatch(List<S2CParticleBatchPack.ParticleEntry> entries,
                                         float r, float g, float b, float scale, int lifetime) {
        if ((age & 1) != 0) return;
        Vec3 centroid = centroid(entries);
        float radius = estimateRadius(entries, centroid, scale);
        int life = Math.max(8, Math.min(40, lifetime + 2));
        ServerVisualBroadcaster.circle(level, centroid, radius, r, g, b, life, 28, "hexagram");
        if (entries.size() > 24 && age % 8 == 0) {
            ServerVisualBroadcaster.shockwave(level, centroid, radius * 0.9F, r, g, b, Math.max(10, life - 4));
        }
    }

    private static Vec3 centroid(List<S2CParticleBatchPack.ParticleEntry> entries) {
        double x = 0, y = 0, z = 0;
        for (var entry : entries) { x += entry.x; y += entry.y; z += entry.z; }
        double inverse = 1.0 / entries.size();
        return new Vec3(x * inverse, y * inverse, z * inverse);
    }

    private static Vec3 centroidVanilla(List<S2CVanillaParticleBatchPack.VanillaParticleEntry> entries) {
        double x = 0, y = 0, z = 0;
        for (var entry : entries) { x += entry.x; y += entry.y; z += entry.z; }
        double inverse = 1.0 / entries.size();
        return new Vec3(x * inverse, y * inverse, z * inverse);
    }

    private static float estimateRadius(List<S2CParticleBatchPack.ParticleEntry> entries, Vec3 center, float scale) {
        double maximum = 0;
        for (var entry : entries) {
            double dx = entry.x - center.x;
            double dz = entry.z - center.z;
            maximum = Math.max(maximum, Math.sqrt(dx * dx + dz * dz));
        }
        return (float) Math.max(0.7, maximum + scale * 0.6F);
    }

    private static float estimateRadiusFromVanilla(
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> entries, Vec3 center) {
        double maximum = 0;
        for (var entry : entries) {
            double dx = entry.x - center.x;
            double dz = entry.z - center.z;
            maximum = Math.max(maximum, Math.sqrt(dx * dx + dz * dz));
        }
        return (float) Math.max(0.8, maximum + 0.5F);
    }

    private static final class MeteorStrike {
        private final double x;
        private final double y;
        private final double z;
        private int fuse;
        private final float radius;
        private final int damageMode;

        private MeteorStrike(double x, double y, double z, int fuse, float radius, int damageMode) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.fuse = fuse;
            this.radius = radius;
            this.damageMode = damageMode;
        }
    }
}
