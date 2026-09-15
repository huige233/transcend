package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.util.PhaseGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


/** 在本地玩家相位飞行期间维持无重力、非着地和零坠落距离，并按需输出飞行诊断日志。 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PhaseFlightClient {

    private PhaseFlightClient() {
    }

    private static boolean pfDiag = false;
    private static int pfTick = 0;

    
    public static void setDiagnostics(boolean enabled) {
        pfDiag = enabled;
    }

    @SubscribeEvent
    public static void onClientTickEND(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || event.player != player) return;
        boolean active = PhaseGuard.isPhaseEnabled(player) && player.getAbilities().flying;
        
        if (pfDiag && ++pfTick % 20 == 0) {
            boolean jump = player.input != null && player.input.jumping;
            boolean shift = player.input != null && player.input.shiftKeyDown;
            double dy = player.getDeltaMovement().y;
            org.slf4j.LoggerFactory.getLogger("PhaseFlightClient")
                .info("[PF] tick={} phaseOn={} fly={} noPhy={} jump={} shift={} dy={}",
                    player.tickCount, PhaseGuard.isPhaseEnabled(player),
                    player.getAbilities().flying, player.noPhysics, jump, shift, dy);
        }
        if (active) {

            player.setNoGravity(true);
            player.setOnGround(false);
            player.fallDistance = 0.0F;
        } else {

            player.setNoGravity(false);
        }
    }
}