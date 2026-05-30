package com.huige233.transcend.gear.forge;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.items.forge.SoulInscriberItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * R84: Soul Inscriber 击杀捕获事件钩子。
 *
 * <p>触发条件（全部成立）：
 * <ol>
 *   <li>{@link LivingDeathEvent#getEntity()} 是真实生物（非玩家、非物品）</li>
 *   <li>击杀来源 = 玩家</li>
 *   <li>玩家主手持 {@link SoulInscriberItem}</li>
 *   <li>该 inscriber 当前 NBT 中尚未捕获 mobId</li>
 * </ol>
 *
 * <p>命中则把死亡生物的 EntityType registry id 写入 inscriber NBT；
 * 后续玩家右键 + 副手装备即可铭刻 SoulEcho。
 *
 * <p>玩家死亡时不触发（避免捕获其他玩家）。
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SoulInscribeHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim == null) return;
        if (victim instanceof Player) return; // 不捕获玩家

        // 击杀来源解析为玩家
        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        if (killer.level().isClientSide) return;

        ItemStack mainHand = killer.getItemInHand(InteractionHand.MAIN_HAND);
        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof SoulInscriberItem)) return;

        boolean captured = SoulInscriberItem.tryCapture(mainHand, victim);
        if (captured) {
            killer.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "msg.transcend.soul_inscriber.captured",
                            victim.getType().getDescription())
                            .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
        }
    }
}
