package com.huige233.transcend.network;

import com.huige233.transcend.block.MiniUniverseGeneratorBlockEntity;
import com.huige233.transcend.menu.MiniUniverseGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;


/** 校验玩家当前机器菜单后，在服务端切换微型宇宙发电机超频模式。 */
public final class C2SMiniUniverseOverclockPacket {
    private final BlockPos pos;
    public C2SMiniUniverseOverclockPacket(BlockPos pos) { this.pos = pos; }
    public C2SMiniUniverseOverclockPacket(FriendlyByteBuf buf) { pos = buf.readBlockPos(); }
    public void write(FriendlyByteBuf buf) { buf.writeBlockPos(pos); }
    public void run(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.containerMenu instanceof MiniUniverseGeneratorMenu menu)
                    || !menu.machinePos().equals(pos) || !menu.stillValid(player)) return;
            if (player.level().getBlockEntity(pos) instanceof MiniUniverseGeneratorBlockEntity generator) {
                generator.setOverclocked(!generator.overclocked());
                menu.broadcastChanges();
            }
        });
        context.setPacketHandled(true);
    }
}
