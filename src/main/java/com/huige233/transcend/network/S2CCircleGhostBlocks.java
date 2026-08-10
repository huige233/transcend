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

/** 服务端→客户端法阵幽灵方块包。 */
public class S2CCircleGhostBlocks {

    private static final int MAX_POSITIONS = 512;

    private final BlockPos corePos;
    private final int selectedTier;
    private final int durationTicks;
    private final List<GhostEntry> entries;

    public record GhostEntry(BlockPos pos, BlockRole role, int minBlockTier) {}

    public S2CCircleGhostBlocks(BlockPos corePos, int selectedTier, int durationTicks,
                                 List<GhostEntry> entries) {
        this.corePos = corePos;
        this.selectedTier = selectedTier;
        this.durationTicks = durationTicks;
        this.entries = entries == null ? Collections.emptyList() : List.copyOf(entries);
    }

    public static S2CCircleGhostBlocks fromMissingEntries(BlockPos corePos, int selectedTier,
                                                           int durationTicks,
                                                           List<CircleStructureCache.MissingEntry> missingEntries) {
        List<GhostEntry> entries = new ArrayList<>(missingEntries.size());
        for (var me : missingEntries) {
            entries.add(new GhostEntry(me.pos(), me.role(), me.minBlockTier()));
        }
        return new S2CCircleGhostBlocks(corePos, selectedTier, durationTicks, entries);
    }

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

    private void handleClient() {
        com.huige233.transcend.client.circle.CircleGhostClientState.showOrClear(
                corePos, selectedTier, durationTicks, entries);
    }

    public static S2CCircleGhostBlocks clear(BlockPos corePos) {
        return new S2CCircleGhostBlocks(corePos, 0, 0, Collections.emptyList());
    }
}
