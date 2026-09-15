package com.huige233.transcend.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

                         
                                                       
                                               
/** 将服务端权威相位飞行开关写入本地玩家的客户端持久数据镜像。 */
public class S2CPhaseFlightSync {

    private final boolean enabled;

    public S2CPhaseFlightSync(boolean enabled) {
        this.enabled = enabled;
    }

    public S2CPhaseFlightSync(FriendlyByteBuf buf) {
        this.enabled = buf.readBoolean();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
    }

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.getPersistentData().putBoolean(C2STogglePhaseFlight.NBT_PHASE_FLIGHT, enabled);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
