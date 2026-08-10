package com.huige233.transcend.network;

import com.huige233.transcend.ascension.resource.ClassResourceType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端→客户端职业资源同步包。 */
public class S2CClassResourceSync {

    private final int resourceOrdinal;
    private final float value;
    private final float maxValue;
    private final boolean inWindow;
    private final boolean overflowed;

    public S2CClassResourceSync(ClassResourceType type, float value, float maxValue,
                                boolean inWindow, boolean overflowed) {
        this.resourceOrdinal = type.ordinal();
        this.value = value;
        this.maxValue = maxValue;
        this.inWindow = inWindow;
        this.overflowed = overflowed;
    }

    public S2CClassResourceSync(ClassResourceType type,
                                com.huige233.transcend.ascension.resource.ClassResourceData res) {
        this(type, res.getValue(), type.getMaxValue(), res.isWindowActive(), res.isOverflowing());
    }

    public S2CClassResourceSync(FriendlyByteBuf buf) {
        this.resourceOrdinal = buf.readVarInt();
        this.value = buf.readFloat();
        this.maxValue = buf.readFloat();
        this.inWindow = buf.readBoolean();
        this.overflowed = buf.readBoolean();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(resourceOrdinal);
        buf.writeFloat(value);
        buf.writeFloat(maxValue);
        buf.writeBoolean(inWindow);
        buf.writeBoolean(overflowed);
    }

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClassResourceType type = ClassResourceType.values()[resourceOrdinal];
            com.huige233.transcend.client.ClientClassResourceCache.update(
                    type, value, maxValue, inWindow, overflowed);
        });
        ctx.get().setPacketHandled(true);
    }
}
