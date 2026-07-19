package com.huige233.transcend.effect;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.init.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
