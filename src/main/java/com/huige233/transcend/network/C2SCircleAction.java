package com.huige233.transcend.network;

import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.block.circle.CircleCoreMenu;
import com.huige233.transcend.circle.CircleStructureCache;
import com.huige233.transcend.circle.CircleStructureValidator;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.handle.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** 客户端→服务端法阵操作包。 */
public class C2SCircleAction {

    private static final double MAX_DISTANCE_SQ = 8.0 * 8.0;

    private static final int GHOST_DURATION_TICKS = 200;

    public enum ActionType {

        ACTIVATE,

        DEACTIVATE,

        CYCLE_SOURCE_MODE,

        PREVIEW_GHOST
    }

    private final BlockPos corePos;
    private final ActionType action;

    private final int param;

    public C2SCircleAction(BlockPos corePos, ActionType action) {
        this(corePos, action, 0);
    }

    public C2SCircleAction(BlockPos corePos, ActionType action, int param) {
        this.corePos = corePos;
        this.action = action;
        this.param = param;
    }

    public C2SCircleAction(FriendlyByteBuf buf) {
        this.corePos = buf.readBlockPos();
        int actionOrdinal = buf.readVarInt();
        ActionType[] actions = ActionType.values();
        this.action = actionOrdinal >= 0 && actionOrdinal < actions.length ? actions[actionOrdinal] : null;
        this.param = buf.readVarInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(corePos);
        buf.writeVarInt(action == null ? -1 : action.ordinal());
        buf.writeVarInt(param);
    }

    static boolean validateRequest(BlockPos requestedTarget, BlockPos openTarget,
                                   double distanceSq, int actionOrdinal) {
        return requestedTarget != null
                && requestedTarget.equals(openTarget)
                && Double.isFinite(distanceSq)
                && distanceSq <= MAX_DISTANCE_SQ
                && actionOrdinal >= 0
                && actionOrdinal < ActionType.values().length;
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
            int actionOrdinal = action == null ? -1 : action.ordinal();
            if (!validateRequest(corePos, openTarget, dSq, actionOrdinal)) {
                return;
            }

            BlockEntity be = level.getBlockEntity(corePos);
            if (!(be instanceof MagicCircleCoreBlockEntity core)) {
                return;
            }
            if (!core.claimOrAuthorize(player)) {
                return;
            }

            switch (action) {
                case ACTIVATE -> core.activate();
                case DEACTIVATE -> core.deactivate();
                case CYCLE_SOURCE_MODE -> {

                }
                case PREVIEW_GHOST -> handlePreviewGhost(level, player, core);
            }
        });
        ctx.setPacketHandled(true);
    }

    private void handlePreviewGhost(ServerLevel level, ServerPlayer player,
                                     MagicCircleCoreBlockEntity core) {
        int requestedTier = Math.max(1, Math.min(5, param));
        CircleTier targetTier = CircleTier.fromLevel(requestedTier);
        CircleStructureCache cache = CircleStructureValidator.validateForTier(level, corePos, targetTier);

        var missingEntries = cache.getMissingEntries();
        if (missingEntries == null || missingEntries.isEmpty()) {
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    S2CCircleGhostBlocks.clear(corePos));
            return;
        }
        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                S2CCircleGhostBlocks.fromMissingEntries(corePos, requestedTier,
                        GHOST_DURATION_TICKS, missingEntries));
    }
}
