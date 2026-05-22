package com.huige233.transcend.network;

import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
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

/**
 * 客户端 → 服务端：法环 GUI 操作请求。
 *
 * <p>由 {@code CircleCoreScreen} 中的按钮触发，将玩家的操作意图发送到服务端。
 * 服务端会校验玩家与法环核心的距离（≤ 8 格），然后调用 BE 上的对应方法。
 */
public class C2SCircleAction {

    /** 玩家与法环交互的最大允许距离（方块，平方）。 */
    private static final double MAX_DISTANCE_SQ = 8.0 * 8.0;

    /** 幽灵预览的持续 tick 数。 */
    private static final int GHOST_DURATION_TICKS = 200;

    /** 操作类型。 */
    public enum ActionType {
        /** 激活法环。 */
        ACTIVATE,
        /** 关闭法环。 */
        DEACTIVATE,
        /** 切换魔力源模式（保留，TODO 实装）。 */
        CYCLE_SOURCE_MODE,
        /** 请求显示选定 tier 的结构幽灵预览。 */
        PREVIEW_GHOST
    }

    private final BlockPos corePos;
    private final ActionType action;
    /** 操作携带的可选整型参数（PREVIEW_GHOST 时为 selectedTier）。 */
    private final int param;

    public C2SCircleAction(BlockPos corePos, ActionType action) {
        this(corePos, action, 0);
    }

    public C2SCircleAction(BlockPos corePos, ActionType action, int param) {
        this.corePos = corePos;
        this.action = action;
        this.param = param;
    }

    // 解码构造器
    public C2SCircleAction(FriendlyByteBuf buf) {
        this.corePos = buf.readBlockPos();
        this.action = buf.readEnum(ActionType.class);
        this.param = buf.readVarInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(corePos);
        buf.writeEnum(action);
        buf.writeVarInt(param);
    }

    public void run(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            // 距离校验（≤ 8 方块）
            double dSq = player.distanceToSqr(
                    corePos.getX() + 0.5,
                    corePos.getY() + 0.5,
                    corePos.getZ() + 0.5);
            if (dSq > MAX_DISTANCE_SQ) {
                return;
            }

            BlockEntity be = level.getBlockEntity(corePos);
            if (!(be instanceof MagicCircleCoreBlockEntity core)) {
                return;
            }

            switch (action) {
                case ACTIVATE -> core.activate();
                case DEACTIVATE -> core.deactivate();
                case CYCLE_SOURCE_MODE -> {
                    // TODO: 实装魔力源模式切换
                }
                case PREVIEW_GHOST -> handlePreviewGhost(level, player, core);
            }
        });
        ctx.setPacketHandled(true);
    }

    /**
     * 服务端处理幽灵预览请求：
     * 对所选 tier 重新校验结构，将缺失方块发给请求玩家。
     * 若结构已完整，发送清除包。
     */
    private void handlePreviewGhost(ServerLevel level, ServerPlayer player,
                                     MagicCircleCoreBlockEntity core) {
        int requestedTier = Math.max(1, Math.min(5, param));
        CircleTier targetTier = CircleTier.fromLevel(requestedTier);
        CircleStructureCache cache = CircleStructureValidator.validateForTier(level, corePos, targetTier);
        // 即使无效也展示缺失（玩家正想看缺什么）
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
