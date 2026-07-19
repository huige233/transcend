package com.huige233.transcend.network;

import com.huige233.transcend.client.ClientInnateManaCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CInnateManaSync {

    private final int currentMana;
    private final float absorbPerSec;

    public S2CInnateManaSync(int currentMana, float absorbPerSec) {
        this.currentMana = currentMana;
        this.absorbPerSec = absorbPerSec;
    }

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
