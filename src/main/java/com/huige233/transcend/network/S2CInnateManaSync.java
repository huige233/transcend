package com.huige233.transcend.network;

import com.huige233.transcend.client.ClientInnateManaCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: 同步玩家当前内在魔力到客户端。
 *
 * <p>{@code player.getPersistentData()} 不会自动同步，HUD 客户端永远读到 0；
 * 因此服务端 tick 中检测到 innate mana 变化后通过本包推送。
 *
 * <p>客户端在 {@link ClientInnateManaCache} 中缓存最新值，HUD 显示时读取。
 */
public class S2CInnateManaSync {

    private final int currentMana;
    private final float absorbPerSec;

    public S2CInnateManaSync(int currentMana, float absorbPerSec) {
        this.currentMana = currentMana;
        this.absorbPerSec = absorbPerSec;
    }

    /** 兼容旧调用点：无吸收数据 */
    public S2CInnateManaSync(int currentMana) {
        this(currentMana, 0f);
    }

    public S2CInnateManaSync(FriendlyByteBuf buf) {
        this.currentMana = buf.readVarInt();
        this.absorbPerSec = buf.readFloat();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(currentMana);
        buf.writeFloat(absorbPerSec);
    }

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientInnateManaCache.update(currentMana, absorbPerSec));
        ctx.get().setPacketHandled(true);
    }
}
