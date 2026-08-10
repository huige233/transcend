package com.huige233.transcend.circle.scroll;

import com.huige233.transcend.Transcend;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Transcend.MODID)
/** 纸兵替身效果处理类。 */
public class PaperLegionDecoyHandler {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide) return;
        var pdata = e.getPersistentData();
        if (!pdata.contains(PaperLegionEffect.TAG_DECOY_EXPIRY)) return;
        long expiry = pdata.getLong(PaperLegionEffect.TAG_DECOY_EXPIRY);
        if (e.level().getGameTime() >= expiry) {

            if (e.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                        e.getX(), e.getY() + 0.5, e.getZ(),
                        12, 0.4, 0.4, 0.4, 0.05);
            }
            e.discard();
        }
    }
}
