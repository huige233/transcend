package com.huige233.transcend.network;

import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.block.circle.CircleCoreMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端→服务端法阵设置变更包。 */
public class C2SCircleSettingChange {

    private static final double MAX_DISTANCE_SQ = 8.0 * 8.0;

    private final BlockPos corePos;
    private final String settingId;
    private final int value;

    public C2SCircleSettingChange(BlockPos corePos, String settingId, int value) {
        this.corePos = corePos;
        this.settingId = settingId == null ? "" : settingId;
        this.value = value;
    }

    public C2SCircleSettingChange(FriendlyByteBuf buf) {
        this.corePos = buf.readBlockPos();

        this.settingId = buf.readUtf(64);
        this.value = buf.readVarInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(corePos);
        buf.writeUtf(settingId, 64);
        buf.writeVarInt(value);
    }

    static boolean validateRequest(BlockPos requestedTarget, BlockPos openTarget,
                                   double distanceSq, String settingId) {
        return requestedTarget != null
                && requestedTarget.equals(openTarget)
                && Double.isFinite(distanceSq)
                && distanceSq <= MAX_DISTANCE_SQ
                && settingId != null
                && !settingId.isEmpty()
                && settingId.length() <= 64;
    }

    public void run(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            double dSq = player.distanceToSqr(
                    corePos.getX() + 0.5,
                    corePos.getY() + 0.5,
                    corePos.getZ() + 0.5);
            BlockPos openTarget = player.containerMenu instanceof CircleCoreMenu menu
                    ? menu.getCorePos() : null;
            if (!validateRequest(corePos, openTarget, dSq, settingId)) {
                return;
            }

            BlockEntity be = level.getBlockEntity(corePos);
            if (!(be instanceof MagicCircleCoreBlockEntity core)) {
                return;
            }
            if (!core.claimOrAuthorize(player)) {
                return;
            }

            core.setSettingValue(settingId, value);
        });
        ctx.setPacketHandled(true);
    }
}
