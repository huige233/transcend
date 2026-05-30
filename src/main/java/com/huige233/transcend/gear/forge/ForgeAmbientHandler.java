package com.huige233.transcend.gear.forge;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.gear.GearCategory;
import com.huige233.transcend.gear.GearForgeData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * R88: 高 tier 已锻武器的持物闲置光环。
 *
 * <p>玩家持 tier ≥ 3 的已锻 WEAPON 时，每 20 server tick 在武器位置喷发
 * 主题色 dust 粒子；tier 越高粒子数越多。
 *
 * <p>性能保护：
 * <ul>
 *   <li>仅 ServerPlayer 触发</li>
 *   <li>每 20 tick（每秒）1 次</li>
 *   <li>tier ≥ 3 是硬下限（普通装备不喷）</li>
 *   <li>仅 main hand 主武器（不遍历副手或护甲）</li>
 * </ul>
 *
 * <p>护甲已通过 {@link ForgeBattleHandler#onLivingHurt} 的 defense aura 处理
 * （受击时被动喷粒子；不需要持续 tick）。
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeAmbientHandler {

    /** 闲置 aura 触发间隔（server tick）。 */
    public static final int AURA_INTERVAL = 20;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % AURA_INTERVAL != 0) return;

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (mainHand.isEmpty()) return;
        if (!GearForgeData.isInPipeline(mainHand)) return;
        if (GearCategory.classify(mainHand) != GearCategory.WEAPON) return;

        if (player.level() instanceof ServerLevel serverLevel) {
            ForgeVisualEffects.spawnIdleAura(serverLevel, player, mainHand);
        }
    }
}
