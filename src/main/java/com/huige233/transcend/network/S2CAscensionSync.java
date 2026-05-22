package com.huige233.transcend.network;

import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.PlayerAscensionData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：同步飞升数据
 */
public class S2CAscensionSync {

    private final CompoundTag data;

    public S2CAscensionSync(PlayerAscensionData ascensionData) {
        this.data = ascensionData.save();
    }

    public S2CAscensionSync(FriendlyByteBuf buf) {
        this.data = buf.readNbt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Player localPlayer = Minecraft.getInstance().player;
                if (localPlayer != null) {
                    AscensionCapability.ifPresent(localPlayer, d -> d.load(data));
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
