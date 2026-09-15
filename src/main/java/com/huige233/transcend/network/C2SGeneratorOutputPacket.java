package com.huige233.transcend.network;

import com.huige233.transcend.block.FEGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;


/** 经菜单、位置与距离校验后，在服务端限幅调整创造发电机的输出功率。 */
public class C2SGeneratorOutputPacket {
    private final BlockPos pos;
    private final int delta;
    public C2SGeneratorOutputPacket(BlockPos pos, int delta) { this.pos = pos; this.delta = delta; }
    public C2SGeneratorOutputPacket(FriendlyByteBuf buf) { pos = buf.readBlockPos(); delta = buf.readInt(); }
    public void write(FriendlyByteBuf buf) { buf.writeBlockPos(pos); buf.writeInt(delta); }
    public void run(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.containerMenu == null || !player.containerMenu.stillValid(player)
                    || !(player.containerMenu instanceof com.huige233.transcend.menu.GeneratorMenu menu)
                    || !menu.machinePos().equals(pos)
                    || player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > 64
                    || !player.level().hasChunkAt(pos)) return;
            if (player.level().getBlockEntity(pos) instanceof FEGeneratorBlockEntity be
                    && be.getMode() == FEGeneratorBlockEntity.Mode.CREATIVE
                    && menu.mode() == FEGeneratorBlockEntity.Mode.CREATIVE) {
                be.adjustConfiguredOutput(Math.max(-100000, Math.min(100000, delta)));
            }
        });
        context.setPacketHandled(true);
    }
}
