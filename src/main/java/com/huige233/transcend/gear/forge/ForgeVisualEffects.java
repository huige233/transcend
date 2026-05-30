package com.huige233.transcend.gear.forge;

import com.huige233.transcend.gear.GearForgeData;
import com.huige233.transcend.init.ModParticles;
import com.huige233.transcend.particle.TranscendDustParticleOptions;
import com.huige233.transcend.particle.TranscendGlitterParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * R88: 造物之道"传奇"视觉/音频特效工具类。
 *
 * <p>把已锻装备从单纯的属性容器升格为可感知的传奇——按 aspect/blessing 主题色喷发
 * 自定义彩色 dust + glitter 粒子，配合阶段性音效；tier 越高效果越夸张。
 *
 * <h2>所有方法均为静态，可在事件钩子任意位置调用</h2>
 */
public final class ForgeVisualEffects {

    private ForgeVisualEffects() {}

    // ─── Aspect → 颜色映射 ───────────────────────────────────────────

    /** 取装备的"主题色"：优先 blessing > aspect > 默认白色。 */
    public static Vector3f getThemeColor(ItemStack stack) {
        GearForgeData.CelestialBlessing bless = GearForgeData.getCelestial(stack);
        if (bless != null) {
            BlessingDef def = BlessingRegistry.byId(bless.blessing());
            if (def != null && def != BlessingRegistry.INDETERMINATE) {
                return hexToVec(def.color());
            }
        }
        GearForgeData.CrucibleData crucible = GearForgeData.getCrucible(stack);
        if (crucible != null) {
            AspectDef def = AspectRegistry.byId(crucible.aspect());
            if (def != null && def != AspectRegistry.INDETERMINATE) {
                return hexToVec(def.color());
            }
        }
        return new Vector3f(1.0f, 1.0f, 1.0f);
    }

    /** 把 0xRRGGBB 转 [0..1] Vector3f。 */
    private static Vector3f hexToVec(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;
        return new Vector3f(r, g, b);
    }

    // ─── 1. 命中粒子爆发（hit）─────────────────────────────────────

    /**
     * 攻击命中时在 victim 身上喷发主题色粒子。
     * tier 越高粒子越多（tier 0→6 颗，tier 5→24 颗）。
     */
    public static void spawnHitBurst(ServerLevel level, LivingEntity victim, ItemStack weapon) {
        Vector3f color = getThemeColor(weapon);
        int forgeTier = GearForgeData.getTier(weapon);
        int particleCount = 4 + forgeTier * 4;  // 4..24

        Vec3 pos = victim.position().add(0, victim.getBbHeight() * 0.5, 0);
        TranscendGlitterParticleOptions glitter = new TranscendGlitterParticleOptions(
                color, 0.18f, 14, true);
        level.sendParticles(glitter,
                pos.x, pos.y, pos.z, particleCount,
                0.3, 0.3, 0.3, 0.08);

        // tier ≥ 2 → 加 dust 粒子营造"裂痕"
        if (forgeTier >= 2) {
            TranscendDustParticleOptions dust = new TranscendDustParticleOptions(
                    color, 0.22f, 16, true);
            level.sendParticles(dust,
                    pos.x, pos.y, pos.z, forgeTier * 2,
                    0.4, 0.4, 0.4, 0.1);
        }

        // tier ≥ 4 → 加"冲击波"环（vanilla EXPLOSION）
        if (forgeTier >= 4) {
            level.sendParticles(ParticleTypes.FLASH,
                    pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
    }

    // ─── 2. 暴击爆发（Spark crit）────────────────────────────────

    /**
     * Spark 暴击触发时的视觉强化：双色 dust + ENCHANT + 强音效。
     * 一击命中显示 "✦ CRIT!"。
     */
    public static void spawnCritBurst(ServerLevel level, LivingEntity victim,
                                       ItemStack weapon, @Nullable Player attacker) {
        Vector3f color = getThemeColor(weapon);
        Vec3 pos = victim.position().add(0, victim.getBbHeight() * 0.5, 0);

        TranscendDustParticleOptions dust = new TranscendDustParticleOptions(
                color, 0.30f, 22, true);
        level.sendParticles(dust, pos.x, pos.y, pos.z, 24, 0.5, 0.5, 0.5, 0.15);
        level.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y + 0.5, pos.z,
                30, 0.4, 0.4, 0.4, 1.2);
        level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z,
                20, 0.4, 0.4, 0.4, 0.3);

        // 音效：高音 amethyst + 弱钟声
        level.playSound(null, victim.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9F, 1.6F);
        level.playSound(null, victim.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5F, 1.8F);

        if (attacker != null) {
            attacker.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("msg.transcend.forge.crit")
                            .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE,
                                    net.minecraft.ChatFormatting.BOLD), true);
        }
    }

    // ─── 3. 击杀处决（kill）──────────────────────────────────────

    /**
     * 已锻武器击杀目标时的处决特效：大型主题色环 + 螺旋 + 强音效。
     * tier ≥ 3 触发更隆重的"光柱"效果。
     */
    public static void spawnKillExecution(ServerLevel level, LivingEntity victim, ItemStack weapon) {
        Vector3f color = getThemeColor(weapon);
        int forgeTier = GearForgeData.getTier(weapon);
        Vec3 pos = victim.position().add(0, victim.getBbHeight() * 0.5, 0);

        // 环形粒子（24 颗均分 360°）
        int ringCount = 16 + forgeTier * 4;
        TranscendDustParticleOptions dust = new TranscendDustParticleOptions(
                color, 0.25f, 24, true);
        for (int i = 0; i < ringCount; i++) {
            double angle = (Math.PI * 2.0 * i) / ringCount;
            double rad = 0.8;
            double dx = Math.cos(angle) * rad;
            double dz = Math.sin(angle) * rad;
            level.sendParticles(dust,
                    pos.x + dx, pos.y, pos.z + dz, 1,
                    0, 0.1, 0, 0.02);
        }

        // 中央 SOUL（灵魂被解放）+ FLASH
        level.sendParticles(ParticleTypes.SOUL,
                pos.x, pos.y, pos.z, 8, 0.2, 0.4, 0.2, 0.05);
        level.sendParticles(ParticleTypes.FLASH,
                pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);

        // tier ≥ 3 → 加"光柱"
        if (forgeTier >= 3) {
            TranscendGlitterParticleOptions glitter = new TranscendGlitterParticleOptions(
                    color, 0.22f, 30, true);
            for (int y = 0; y < 12; y++) {
                level.sendParticles(glitter,
                        pos.x, pos.y + y * 0.3, pos.z, 2,
                        0.15, 0.0, 0.15, 0.02);
            }
        }

        // tier ≥ 5 → 远雷音
        SoundEvent killSound = forgeTier >= 5
                ? SoundEvents.LIGHTNING_BOLT_THUNDER
                : SoundEvents.ZOMBIE_VILLAGER_CONVERTED;
        float vol = forgeTier >= 5 ? 0.5F : 0.6F;
        level.playSound(null, victim.blockPosition(), killSound,
                SoundSource.PLAYERS, vol, 1.2F);
    }

    // ─── 4. 持武器闲置光环（ambient）─────────────────────────────

    /**
     * 玩家持已锻武器时，每个 ambient tick 在武器位置喷发少量主题色粒子。
     * tier ≥ 3 才触发；粒子数随 tier 增加。
     */
    public static void spawnIdleAura(ServerLevel level, Player player, ItemStack weapon) {
        int forgeTier = GearForgeData.getTier(weapon);
        if (forgeTier < 3) return;

        Vector3f color = getThemeColor(weapon);
        // 武器位置 ≈ 玩家右手前方
        Vec3 lookVec = player.getLookAngle();
        Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
        Vec3 handPos = player.position()
                .add(0, player.getEyeHeight() - 0.4, 0)
                .add(rightVec.scale(0.35))
                .add(lookVec.scale(0.3));

        int n = forgeTier - 2;  // tier 3=1, 4=2, 5=3
        TranscendDustParticleOptions dust = new TranscendDustParticleOptions(
                color, 0.12f, 12, true);
        level.sendParticles(dust, handPos.x, handPos.y, handPos.z, n,
                0.08, 0.08, 0.08, 0.005);
    }

    // ─── 5. 防御反弹（穿戴护甲受击时，每 N tick 触发一次）───────

    /**
     * 穿戴已锻护甲的玩家受击时，喷发主题色"护盾"粒子。
     * tier ≥ 2 触发。
     */
    public static void spawnDefenseAura(ServerLevel level, Player player, ItemStack armor) {
        int forgeTier = GearForgeData.getTier(armor);
        if (forgeTier < 2) return;

        Vector3f color = getThemeColor(armor);
        Vec3 pos = player.position().add(0, player.getBbHeight() * 0.6, 0);
        TranscendGlitterParticleOptions glitter = new TranscendGlitterParticleOptions(
                color, 0.15f, 12, true);
        level.sendParticles(glitter, pos.x, pos.y, pos.z, 6,
                0.4, 0.5, 0.4, 0.05);
    }
}
