package com.huige233.transcend.network;

import com.huige233.transcend.entity.TestDummy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2STestDummySettingsPack {

    private final int entityId;
    private final int action;
    private final int value;

    public C2STestDummySettingsPack(int entityId, int action, int value) {
        this.entityId = entityId;
        this.action = action;
        this.value = value;
    }

    public C2STestDummySettingsPack(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.action = buf.readByte();
        this.value = buf.readInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeByte(action);
        buf.writeInt(value);
    }

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            Entity entity = player.serverLevel().getEntity(entityId);
            if (!(entity instanceof TestDummy dummy)) return;
            if (player.distanceTo(dummy) > 10) return;

            switch (action) {
                case 0 -> dummy.resetData();
                case 1 -> dummy.toggleAnnounce();
                case 2 -> {
                    int armor = Math.max(0, Math.min(30, value));
                    dummy.getAttribute(Attributes.ARMOR).setBaseValue(armor);
                }
                case 3 -> {
                    int resist = Math.max(0, Math.min(4, value));
                    dummy.setResistanceLevel(resist);
                }
                case 4 -> dummy.discard();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
