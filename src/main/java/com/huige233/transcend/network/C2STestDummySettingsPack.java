package com.huige233.transcend.network;

import com.huige233.transcend.entity.TestDummy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端→服务端木桩设置包。 */
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
            double distance = player.distanceTo(dummy);
            if (!validateRequest(entityId, dummy.getId(), distance, action)
                    || !isValidValue(action, value)
                    || !dummy.canConfigureExistingOwner(player)) {
                return;
            }

            switch (action) {
                case 0 -> dummy.resetData();
                case 1 -> dummy.toggleAnnounce();
                case 2 -> {
                    dummy.getAttribute(Attributes.ARMOR).setBaseValue(value);
                }
                case 3 -> dummy.setResistanceLevel(value);
                case 4 -> dummy.discard();
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static boolean validateRequest(int requestedEntityId, int resolvedEntityId,
                                          double distance, int action) {
        return requestedEntityId == resolvedEntityId
                && Double.isFinite(distance)
                && distance >= 0.0
                && distance <= 10.0
                && action >= 0
                && action <= 4;
    }

    private static boolean isValidValue(int action, int value) {
        return switch (action) {
            case 2 -> value >= 0 && value <= 30;
            case 3 -> value >= 0 && value <= 4;
            default -> value == 0;
        };
    }
}
