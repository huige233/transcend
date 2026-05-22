package com.huige233.transcend.circle.scroll;

import com.huige233.transcend.Transcend;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Round 37: PaperLegion decoy expiry handler.
 *
 * <p>每个 tick 检查身上带有 <code>transcend_decoy_expiry</code> NBT 的 LivingEntity，
 * 若 game time 超过 expiry → discard。
 *
 * <p>为何用 LivingTickEvent：决斗诱饵跨 chunk reload / world reload 时仍有效，
 * 而非依赖临时 mod state map。NBT persists across saves.
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID)
public class PaperLegionDecoyHandler {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide) return;
        var pdata = e.getPersistentData();
        if (!pdata.contains(PaperLegionEffect.TAG_DECOY_EXPIRY)) return;
        long expiry = pdata.getLong(PaperLegionEffect.TAG_DECOY_EXPIRY);
        if (e.level().getGameTime() >= expiry) {
            // 在消失前喷一圈粒子（仅 server → client 自动同步）
            if (e.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                        e.getX(), e.getY() + 0.5, e.getZ(),
                        12, 0.4, 0.4, 0.4, 0.05);
            }
            e.discard();
        }
    }
}
