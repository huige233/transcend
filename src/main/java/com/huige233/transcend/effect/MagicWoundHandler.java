package com.huige233.transcend.effect;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.init.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Round 37: MagicWound 伤害放大处理器。
 *
 * <p>检测 LivingHurtEvent 中的受害者，若身上有 MagicWound effect → 按 amplifier 放大 damage。
 * 注册到 Forge MinecraftForge.EVENT_BUS。
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID)
public class MagicWoundHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        var instance = victim.getEffect(ModEffects.MAGIC_WOUND.get());
        if (instance == null) return;
        float multiplier = MagicWoundEffect.getDamageMultiplier(instance.getAmplifier());
        if (multiplier <= 1.0F) return;
        event.setAmount(event.getAmount() * multiplier);
    }
}
