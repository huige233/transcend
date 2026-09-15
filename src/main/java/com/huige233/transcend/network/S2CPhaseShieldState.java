package com.huige233.transcend.network;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.tech.shield.PhaseShieldStatus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;


/** 同步指定玩家的相位护盾显示状态与电量比例至客户端缓存。 */
public final class S2CPhaseShieldState {
    private final UUID playerId;
    private final PhaseShieldStatus status;
    private final float chargeRatio;

    public S2CPhaseShieldState(UUID playerId, PhaseShieldStatus status, float chargeRatio) {
        this.playerId = playerId;
        this.status = status;
        this.chargeRatio = Math.max(0.0F, Math.min(1.0F, chargeRatio));
    }

    public S2CPhaseShieldState(FriendlyByteBuf buffer) {
        playerId = buffer.readUUID();
        status = PhaseShieldStatus.fromNetwork(buffer.readVarInt());
        chargeRatio = Math.max(0.0F, Math.min(1.0F, buffer.readFloat()));
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerId);
        buffer.writeVarInt(status.ordinal());
        buffer.writeFloat(chargeRatio);
    }

    public void run(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                PhaseShieldClientState.set(playerId, status, chargeRatio)));
        context.get().setPacketHandled(true);
    }
}
