package com.huige233.transcend.network;

import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：修改法环功能设置。
 *
 * <p>由 {@code CircleCoreScreen} 中点击 / 拖动设置控件触发。
 * 服务端会校验玩家与法环核心的距离（≤ 8 格）后调用
 * {@link MagicCircleCoreBlockEntity#setSettingValue(String, int)}。
 *
 * <p>注册示例：
 * <pre>{@code
 * CHANNEL.registerMessage(id++, C2SCircleSettingChange.class,
 *         C2SCircleSettingChange::write, C2SCircleSettingChange::new, C2SCircleSettingChange::run,
 *         Optional.of(NetworkDirection.PLAY_TO_SERVER));
 * }</pre>
 */
public class C2SCircleSettingChange {

    /** 玩家与法环交互的最大允许距离（方块，平方）。 */
    private static final double MAX_DISTANCE_SQ = 8.0 * 8.0;

    private final BlockPos corePos;
    private final String settingId;
    private final int value;

    public C2SCircleSettingChange(BlockPos corePos, String settingId, int value) {
        this.corePos = corePos;
        this.settingId = settingId == null ? "" : settingId;
        this.value = value;
    }

    // 解码构造器
    public C2SCircleSettingChange(FriendlyByteBuf buf) {
        this.corePos = buf.readBlockPos();
        // 限制 settingId 长度（防止恶意客户端发送超长字符串）
        this.settingId = buf.readUtf(64);
        this.value = buf.readVarInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(corePos);
        buf.writeUtf(settingId, 64);
        buf.writeVarInt(value);
    }

    public void run(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (settingId == null || settingId.isEmpty()) return;

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

            core.setSettingValue(settingId, value);
        });
        ctx.setPacketHandled(true);
    }
}
