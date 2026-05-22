package com.huige233.transcend.client.magic;

import com.huige233.transcend.TranscendGameRules;
import com.huige233.transcend.client.renderer.ShaderSpellRenderer;
import com.huige233.transcend.network.S2CParticleBatchPack;
import com.huige233.transcend.network.S2CVanillaParticleBatchPack;
import com.huige233.transcend.util.EntityCompatUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class AvatarMeteorCircleEffect extends AbstractMagicCircle {

    private static final double MAX_RADIUS = 13.0;
    private final Random rng = new Random();
    private MeteorStrike meteor;

    public AvatarMeteorCircleEffect(ServerLevel level, Vec3 center) {
        super(level, center);
    }

    @Override
    protected float getBaseRadius() {
        return 10.0F;
    }

    @Override
    protected String getCirclePattern() {
        return "hexagram";
    }

    @Override
    protected void tickParticles(float scaleFactor) {
        double cx = center.x, cy = center.y, cz = center.z;
        double rot = age * Math.PI / 28.0;
        int particleLifetime = 6;

        float r = 0.95F;
        float g = 0.20F + 0.20F * (float) Math.sin(age * 0.10F);
        float b = 0.10F;

        List<S2CParticleBatchPack.ParticleEntry> outer = MagicCircleGeometry.buildCircle(
                cx, cy, cz, 9.5 * scaleFactor, 120, rot, UP);
        sendBatch(outer, r, g, b, 0.85F, particleLifetime);

        if (age % 4 == 0) {
            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> embers = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double dist = rng.nextDouble() * 9.0 * scaleFactor;
                double px = cx + Math.cos(angle) * dist;
                double pz = cz + Math.sin(angle) * dist;
                embers.add(new S2CVanillaParticleBatchPack.VanillaParticleEntry(
                        px, cy + 0.2 + rng.nextDouble() * 0.6, pz, 0, 0.02 + rng.nextDouble() * 0.04, 0));
            }
            sendVanillaBatch(embers, "ash");
        }
    }

    @Override
    protected void submitShaderCircle(float scale) {
        float radius = getBaseRadius() * radiusMultiplier * Math.max(0.35F, scale);
        // 主法阵 — 大且持续
        ShaderSpellRenderer.addCircle(center, radius, 1.0F, 0.35F, 0.12F, 18, 40, "pentagram");
        // 外圈脉冲
        if ((age & 3) == 0) {
            ShaderSpellRenderer.addShieldRipple(center, radius * 0.85F, 1.0F, 0.65F, 0.2F, 16);
        }
        // 内圈旋转六芒星 — 增加视觉层次
        if ((age % 6) == 0) {
            ShaderSpellRenderer.addCircle(center.add(0, 0.05, 0),
                    radius * 0.55F, 1.0F, 0.8F, 0.3F, 14, 28, "hexagram");
        }
        // 持续冲击波脉冲 — 让法阵区域有明确的危险感
        if ((age % 12) == 0) {
            ShaderSpellRenderer.addShockwave(center, radius * 0.7F, 1.0F, 0.4F, 0.1F, 20);
        }
    }

    @Override
    protected void onRemove() {
        meteor = null;
    }

    private void scheduleMeteor() {
        double angle = rng.nextDouble() * Math.PI * 2;
        double dist = rng.nextDouble() * (2.0 + radiusMultiplier * 1.75);
        double x = center.x + Math.cos(angle) * dist;
        double z = center.z + Math.sin(angle) * dist;
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
        double y = groundY + 0.1;
        int fuse = (specialLevel >= 4) ? 40 : 30; // 短fuse：快速砸下
        float radius = (specialLevel >= 4 ? 6.2F : 5.2F) * radiusMultiplier;
        int damageMode = rng.nextInt(5);

        double spawnY = y + 15.0; // 15格高度——玩家肯定能看到
        meteor = new MeteorStrike(x, y, z, spawnY, fuse, radius, damageMode);

        // ─── 生成陨石实体：用FallingBlock（岩浆块）直线下落 ───
        FallingBlockEntity meteorBlock = FallingBlockEntity.fall(level,
                BlockPos.containing(x, spawnY, z), Blocks.MAGMA_BLOCK.defaultBlockState());
        meteorBlock.setPos(x, spawnY, z);
        meteorBlock.time = 590; // 接近600时自动discard不放置方块
        meteorBlock.dropItem = false;
        meteorBlock.setHurtsEntities(0.0F, 0); // 伤害由resolveMeteor处理
        meteorBlock.setGlowingTag(true);
        // 正常重力下落（约0.6秒落到地面）
        level.addFreshEntity(meteorBlock);

        // ─── Shader辅助视觉 ───
        // 短lifetime的下落特效beam
        ShaderSpellRenderer.addSpellEffect(
                new Vec3(x, spawnY + 5, z), new Vec3(x, y + 0.5, z),
                1.0F, 0.4F, 0.1F, fuse + 5, "meteor");
        // 落点预警
        ShaderSpellRenderer.addShockwave(
                new Vec3(x, y + 0.1, z),
                radius * 0.5F, 1.0F, 0.5F, 0.15F, fuse);
        // 音效
        level.playSound(null, BlockPos.containing(x, y, z),
                SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.5F, 0.3F);
    }

    @Override
    protected void tickEffect(float scaleFactor) {
        if (meteor == null && age >= 10) {
            scheduleMeteor();
        }

        if (meteor != null) {
            meteor.fuse--;
            if (meteor.fuse <= 0) {
                resolveMeteor(meteor);
                meteor = null;
            }
        }
    }

    private void resolveMeteor(MeteorStrike strike) {
        BlockPos pos = BlockPos.containing(strike.x, strike.y, strike.z);

        // ─── 清除陨石FallingBlock（防止放置方块） ───
        AABB cleanupArea = new AABB(strike.x - 3, strike.y - 2, strike.z - 3,
                strike.x + 3, strike.y + 20, strike.z + 3);
        for (Entity e : level.getEntities((Entity) null, cleanupArea)) {
            if (e instanceof FallingBlockEntity fb && !fb.isRemoved() && fb.time >= 585) {
                fb.discard();
            }
        }

        // ─── 音效 ───
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 3.0F, 0.4F);
        level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 2.5F, 0.5F);
        level.playSound(null, pos, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, 0.6F);

        Vec3 impactCenter = new Vec3(strike.x, strike.y + 0.06, strike.z);

        // ─── 巨大多层冲击波 ───
        ShaderSpellRenderer.addShockwave(impactCenter,
                strike.radius + 15.0F, 1.0F, 0.2F, 0.02F, 45);
        ShaderSpellRenderer.addShockwave(impactCenter.add(0, 0.2, 0),
                strike.radius + 8.0F, 1.0F, 0.5F, 0.1F, 35);
        ShaderSpellRenderer.addShockwave(impactCenter.add(0, 0.4, 0),
                strike.radius + 4.0F, 1.0F, 0.8F, 0.3F, 25);

        // ─── 火柱冲天 ───
        ShaderSpellRenderer.addSpellEffect(
                new Vec3(strike.x, strike.y + strike.radius * 5.0, strike.z),
                new Vec3(strike.x, strike.y + 0.1, strike.z),
                1.0F, 0.35F, 0.08F, 40, "meteor");
        for (int i = 0; i < 3; i++) {
            double ox = (rng.nextDouble() - 0.5) * 2.0;
            double oz = (rng.nextDouble() - 0.5) * 2.0;
            ShaderSpellRenderer.addSpellEffect(
                    new Vec3(strike.x + ox, strike.y + strike.radius * 3.0 + i, strike.z + oz),
                    new Vec3(strike.x + ox, strike.y + 0.2, strike.z + oz),
                    1.0F, 0.6F, 0.15F, 30, "beam");
        }

        // ─── 方块碎片飞溅 — 将落点附近地面方块变成FallingBlock弹飞 ───
        int debrisCount = 12 + specialLevel * 4;
        for (int i = 0; i < debrisCount; i++) {
            double dAngle = rng.nextDouble() * Math.PI * 2;
            double dDist = 1.0 + rng.nextDouble() * (strike.radius * 0.6);
            int bx = Mth.floor(strike.x + Math.cos(dAngle) * dDist);
            int bz = Mth.floor(strike.z + Math.sin(dAngle) * dDist);
            int by = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) - 1;
            BlockPos debrisPos = new BlockPos(bx, by, bz);
            net.minecraft.world.level.block.state.BlockState debrisState = level.getBlockState(debrisPos);
            if (debrisState.isAir() || debrisState.liquid()) continue;
            // 生成飞溅的FallingBlock
            FallingBlockEntity debris = FallingBlockEntity.fall(level, debrisPos, debrisState);
            debris.setPos(bx + 0.5, by + 1.0, bz + 0.5);
            double launchAngle = Math.atan2(bz + 0.5 - strike.z, bx + 0.5 - strike.x);
            double horizSpeed = 0.3 + rng.nextDouble() * 0.6;
            double vertSpeed = 0.5 + rng.nextDouble() * 0.8;
            debris.setDeltaMovement(
                    Math.cos(launchAngle) * horizSpeed,
                    vertSpeed,
                    Math.sin(launchAngle) * horizSpeed);
            debris.time = 595; // 很快自动消失不放置
            debris.dropItem = false;
            debris.setHurtsEntities(2.0F, 20);
            level.addFreshEntity(debris);
            // 移除原方块（坑洞效果）
            if (TranscendGameRules.canBossMassSpellGrief(level)) {
                level.destroyBlock(debrisPos, false);
            }
        }

        // ─── 视角晃动 ───
        double shakeRange = strike.radius + 30.0;
        for (net.minecraft.server.level.ServerPlayer sp :
                level.getPlayers(p -> p.distanceToSqr(strike.x, strike.y, strike.z) < shakeRange * shakeRange)) {
            double kx = sp.getX() - strike.x;
            double kz = sp.getZ() - strike.z;
            double kDist = Math.sqrt(kx * kx + kz * kz);
            float strength = (float) (1.5 * (1.0 - Math.min(1.0, kDist / shakeRange)));
            if (kDist > 0.1) {
                sp.knockback(strength * 0.7, -kx / kDist, -kz / kDist);
                sp.hurtMarked = true;
            } else {
                sp.setDeltaMovement(sp.getDeltaMovement().add(0, 0.6, 0));
                sp.hurtMarked = true;
            }
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3, false, false));
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket(
                    sp.getId(), rng.nextFloat() * 360.0F));
        }

        // ─── 爆炸（如果允许） ───
        if (TranscendGameRules.canBossMassSpellGrief(level)) {
            level.explode(null, strike.x, strike.y, strike.z,
                    5.0F + rng.nextFloat() * 2.0F,
                    Level.ExplosionInteraction.MOB);
        }

        double blastRadius = strike.radius + 8.0;
        AABB area = new AABB(
                strike.x - blastRadius, strike.y - 4.0, strike.z - blastRadius,
                strike.x + blastRadius, strike.y + 8.0, strike.z + blastRadius);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> shouldAffect(e) && !EntityCompatUtil.isProtectedPlayer(e));

        for (LivingEntity target : targets) {
            double dist = target.distanceToSqr(strike.x, strike.y, strike.z);
            double maxDist = blastRadius * blastRadius;
            if (dist > maxDist) continue;

            float falloff = 1.0F - 0.6F * (float) Math.sqrt(dist / maxDist);
            float rawDamage = (25.0F + rng.nextFloat() * 20.0F) * powerMultiplier;
            float damage = Math.max(8.0F, rawDamage * falloff);

            DamageSource source = selectDamageSource(strike.damageMode);
            target.hurt(adaptMeteorDamageSource(target, source), damage);
            applySecondaryEffect(target, strike.damageMode);

            // 击退
            double kx = target.getX() - strike.x;
            double kz = target.getZ() - strike.z;
            double kDist = Math.sqrt(kx * kx + kz * kz);
            if (kDist > 0.1) {
                float kb = falloff * 1.5F;
                target.knockback(kb, -kx / kDist, -kz / kDist);
                target.hurtMarked = true;
            }
        }
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
        if (!EntityCompatUtil.isBotaniaGaiaGuardian(target)) {
            return fallback;
        }
        Player owner = getOwnerPlayer();
        if (owner != null && owner.isAlive() && !EntityCompatUtil.isProtectedPlayer(owner)) {
            return owner.damageSources().playerAttack(owner);
        }
        Player compat = EntityCompatUtil.findNearestValidPlayer(level, target, 64.0);
        if (compat != null) {
            return compat.damageSources().playerAttack(compat);
        }
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

    private static final class MeteorStrike {
        final double x;
        final double y;
        final double z;
        final double startY;
        int fuse;
        final float radius;
        final int damageMode;

        private MeteorStrike(double x, double y, double z, double startY, int fuse, float radius, int damageMode) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.startY = startY;
            this.fuse = fuse;
            this.radius = radius;
            this.damageMode = damageMode;
        }
    }
}
