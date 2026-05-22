package com.huige233.transcend.network;

import com.huige233.transcend.client.circle.OreRevealClientState;
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
 * 服务端 → 客户端：地脉照骨（Oreblood Revelation）矿石高亮。
 *
 * <p>携带卷轴施法位置周围的矿石坐标列表，每个矿石带 24-bit RGB 颜色。
 * 客户端持续 durationTicks 显示高亮方块轮廓。
 *
 * <p>包格式：center(BlockPos) | duration(varInt) | count(varInt) |
 * 每条 dx(short) dy(short) dz(short) color(int)。
 */
public class S2COreRevealPack {

    /** 单包最多 1024 个矿石，避免大包 */
    private static final int MAX_POSITIONS = 1024;

    private final BlockPos center;
    private final int durationTicks;
    private final List<OreEntry> entries;

    /** 单个矿石条目：绝对坐标 + 颜色 */
    public record OreEntry(BlockPos pos, int color) {}

    public S2COreRevealPack(BlockPos center, int durationTicks, List<OreEntry> entries) {
        this.center = center;
        this.durationTicks = durationTicks;
        this.entries = entries == null ? Collections.emptyList() : List.copyOf(entries);
    }

    /** 解码构造器 */
    public S2COreRevealPack(FriendlyByteBuf buf) {
        this.center = buf.readBlockPos();
        this.durationTicks = buf.readVarInt();
        int count = buf.readVarInt();
        if (count > MAX_POSITIONS) {
            throw new IllegalStateException(
                    "S2COreRevealPack count exceeds cap: " + count + " > " + MAX_POSITIONS);
        }
        List<OreEntry> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int dx = buf.readShort();
            int dy = buf.readShort();
            int dz = buf.readShort();
            int color = buf.readInt();
            list.add(new OreEntry(center.offset(dx, dy, dz), color));
        }
        this.entries = Collections.unmodifiableList(list);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(center);
        buf.writeVarInt(durationTicks);
        buf.writeVarInt(entries.size());
        for (OreEntry e : entries) {
            buf.writeShort(e.pos().getX() - center.getX());
            buf.writeShort(e.pos().getY() - center.getY());
            buf.writeShort(e.pos().getZ() - center.getZ());
            buf.writeInt(e.color());
        }
    }

    public void run(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::handleClient));
        ctx.setPacketHandled(true);
    }

    /** 客户端实际处理：写入 OreRevealClientState 缓存。 */
    private void handleClient() {
        OreRevealClientState.show(center, durationTicks, entries);
    }
}
