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

/** 锻造视觉效果工具类。 */
public final class ForgeVisualEffects {

    private ForgeVisualEffects() {}

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

    private static Vector3f hexToVec(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;
        return new Vector3f(r, g, b);
    }

    public static void spawnHitBurst(ServerLevel level, LivingEntity victim, ItemStack weapon) {
        Vector3f color = getThemeColor(weapon);
        int forgeTier = GearForgeData.getTier(weapon);
        int particleCount = 4 + forgeTier * 4;

        Vec3 pos = victim.position().add(0, victim.getBbHeight() * 0.5, 0);
        TranscendGlitterParticleOptions glitter = new TranscendGlitterParticleOptions(
                color, 0.18f, 14, true);
        level.sendParticles(glitter,
                pos.x, pos.y, pos.z, particleCount,
                0.3, 0.3, 0.3, 0.08);

        if (forgeTier >= 2) {
            TranscendDustParticleOptions dust = new TranscendDustParticleOptions(
                    color, 0.22f, 16, true);
            level.sendParticles(dust,
                    pos.x, pos.y, pos.z, forgeTier * 2,
                    0.4, 0.4, 0.4, 0.1);
        }

        if (forgeTier >= 4) {
            level.sendParticles(ParticleTypes.FLASH,
                    pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
    }

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

    public static void spawnKillExecution(ServerLevel level, LivingEntity victim, ItemStack weapon) {
        Vector3f color = getThemeColor(weapon);
        int forgeTier = GearForgeData.getTier(weapon);
        Vec3 pos = victim.position().add(0, victim.getBbHeight() * 0.5, 0);

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

        level.sendParticles(ParticleTypes.SOUL,
                pos.x, pos.y, pos.z, 8, 0.2, 0.4, 0.2, 0.05);
        level.sendParticles(ParticleTypes.FLASH,
                pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);

        if (forgeTier >= 3) {
            TranscendGlitterParticleOptions glitter = new TranscendGlitterParticleOptions(
                    color, 0.22f, 30, true);
            for (int y = 0; y < 12; y++) {
                level.sendParticles(glitter,
                        pos.x, pos.y + y * 0.3, pos.z, 2,
                        0.15, 0.0, 0.15, 0.02);
            }
        }

        SoundEvent killSound = forgeTier >= 5
                ? SoundEvents.LIGHTNING_BOLT_THUNDER
                : SoundEvents.ZOMBIE_VILLAGER_CONVERTED;
        float vol = forgeTier >= 5 ? 0.5F : 0.6F;
        level.playSound(null, victim.blockPosition(), killSound,
                SoundSource.PLAYERS, vol, 1.2F);
    }

    public static void spawnIdleAura(ServerLevel level, Player player, ItemStack weapon) {
        int forgeTier = GearForgeData.getTier(weapon);
        if (forgeTier < 3) return;

        Vector3f color = getThemeColor(weapon);

        Vec3 lookVec = player.getLookAngle();
        Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
        Vec3 handPos = player.position()
                .add(0, player.getEyeHeight() - 0.4, 0)
                .add(rightVec.scale(0.35))
                .add(lookVec.scale(0.3));

        int n = forgeTier - 2;
        TranscendDustParticleOptions dust = new TranscendDustParticleOptions(
                color, 0.12f, 12, true);
        level.sendParticles(dust, handPos.x, handPos.y, handPos.z, n,
                0.08, 0.08, 0.08, 0.005);
    }

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
