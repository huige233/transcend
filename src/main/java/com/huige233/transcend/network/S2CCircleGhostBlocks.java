package com.huige233.transcend.network;

import com.huige233.transcend.circle.CircleStructureCache;
import com.huige233.transcend.circle.CircleStructurePattern.BlockRole;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：法环结构预览的幽灵方块位置同步。
 *
 * <p>当玩家与法环核心交互并请求结构预览时，服务端计算缺失的方块位置并推送本数据包。
 * 客户端收到后在指定时长内以幽灵方块形式高亮显示缺失位置；若 selectedTier 为 0 则清除预览。
 * 每个位置携带 BlockRole 和 minBlockTier，客户端按角色着色并显示方块名。
 */
public class S2CCircleGhostBlocks {

    private static final int MAX_POSITIONS = 512;

    private final BlockPos corePos;
    private final int selectedTier;
    private final int durationTicks;
    private final List<GhostEntry> entries;

    /** 携带角色信息的 ghost 条目 */
    public record GhostEntry(BlockPos pos, BlockRole role, int minBlockTier) {}

    public S2CCircleGhostBlocks(BlockPos corePos, int selectedTier, int durationTicks,
                                 List<GhostEntry> entries) {
        this.corePos = corePos;
        this.selectedTier = selectedTier;
        this.durationTicks = durationTicks;
        this.entries = entries == null ? Collections.emptyList() : List.copyOf(entries);
    }

    /** 从 MissingEntry 列表构造（服务端便利方法） */
    public static S2CCircleGhostBlocks fromMissingEntries(BlockPos corePos, int selectedTier,
                                                           int durationTicks,
                                                           List<CircleStructureCache.MissingEntry> missingEntries) {
        List<GhostEntry> entries = new ArrayList<>(missingEntries.size());
        for (var me : missingEntries) {
            entries.add(new GhostEntry(me.pos(), me.role(), me.minBlockTier()));
        }
        return new S2CCircleGhostBlocks(corePos, selectedTier, durationTicks, entries);
    }

    // 解码构造器
    public S2CCircleGhostBlocks(FriendlyByteBuf buf) {
        this.corePos = buf.readBlockPos();
        this.selectedTier = buf.readVarInt();
        this.durationTicks = buf.readVarInt();

        int count = buf.readVarInt();
        if (count > MAX_POSITIONS) {
            throw new IllegalStateException(
                    "S2CCircleGhostBlocks count exceeds cap: " + count + " > " + MAX_POSITIONS);
        }

        BlockRole[] roles = BlockRole.values();
        List<GhostEntry> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int dx = buf.readShort();
            int dy = buf.readShort();
            int dz = buf.readShort();
            int roleOrdinal = buf.readByte();
            int minTier = buf.readByte();
            BlockRole role = (roleOrdinal >= 0 && roleOrdinal < roles.length) ? roles[roleOrdinal] : BlockRole.FOUNDATION;
            list.add(new GhostEntry(corePos.offset(dx, dy, dz), role, minTier));
        }
        this.entries = Collections.unmodifiableList(list);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(corePos);
        buf.writeVarInt(selectedTier);
        buf.writeVarInt(durationTicks);

        int count = entries.size();
        buf.writeVarInt(count);
        for (GhostEntry entry : entries) {
            buf.writeShort(entry.pos().getX() - corePos.getX());
            buf.writeShort(entry.pos().getY() - corePos.getY());
            buf.writeShort(entry.pos().getZ() - corePos.getZ());
            buf.writeByte(entry.role().ordinal());
            buf.writeByte(entry.minBlockTier());
        }
    }

    public void run(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::handleClient));
        ctx.setPacketHandled(true);
    }

    /** 客户端实际处理逻辑：交由幽灵方块渲染状态管理。 */
    private void handleClient() {
        com.huige233.transcend.client.circle.CircleGhostClientState.showOrClear(
                corePos, selectedTier, durationTicks, entries);
    }

    /** 构造一个用于清除预览的空数据包。 */
    public static S2CCircleGhostBlocks clear(BlockPos corePos) {
        return new S2CCircleGhostBlocks(corePos, 0, 0, Collections.emptyList());
    }
}
