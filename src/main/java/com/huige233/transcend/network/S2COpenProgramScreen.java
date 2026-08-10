package com.huige233.transcend.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端→客户端打开卡牌编程界面包。 */
public class S2COpenProgramScreen {

    public S2COpenProgramScreen() {}

    public S2COpenProgramScreen(FriendlyByteBuf buf) {}

    public void write(FriendlyByteBuf buf) {}

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.huige233.transcend.client.CardProgramScreen.open()
            )
        );
        ctx.get().setPacketHandled(true);
    }
}
