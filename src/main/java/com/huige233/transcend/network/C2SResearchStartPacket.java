package com.huige233.transcend.network;

import com.huige233.transcend.menu.ResearchStationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;


/** 校验研究站菜单和研究者身份后执行研究点准备、研究启动或暂停并返回结果。 */
public final class C2SResearchStartPacket {
    private static final int PREPARE = 0, START = 1, PAUSE = 2, PREPARE_BULK = 3, AUTO_PREPARE = 4;
    private final BlockPos pos;
    private final String id;
    private final int containerId, action;

    private C2SResearchStartPacket(BlockPos pos, int containerId, String id, int action) {
        this.pos = pos; this.containerId = containerId; this.id = id; this.action = action;
    }
    public C2SResearchStartPacket(BlockPos pos, int containerId, String id) {
        this(pos, containerId, id, id.isEmpty() ? PAUSE : START);
    }
    public C2SResearchStartPacket(String id) { this(BlockPos.ZERO, -1, id); }
    public static C2SResearchStartPacket prepare(BlockPos pos, int containerId) {
        return new C2SResearchStartPacket(pos, containerId, "", PREPARE);
    }
    public static C2SResearchStartPacket prepare(BlockPos pos, int containerId, boolean bulk) {
        return new C2SResearchStartPacket(pos, containerId, "", bulk ? PREPARE_BULK : PREPARE);
    }
    public static C2SResearchStartPacket toggleAuto(BlockPos pos, int containerId) {
        return new C2SResearchStartPacket(pos, containerId, "", AUTO_PREPARE);
    }
    public C2SResearchStartPacket(FriendlyByteBuf b) {
        pos = b.readBlockPos(); containerId = b.readVarInt(); action = b.readByte(); id = b.readUtf(64);
    }
    public void write(FriendlyByteBuf b) {
        b.writeBlockPos(pos); b.writeVarInt(containerId); b.writeByte(action); b.writeUtf(id, 64);
    }
    public void run(Supplier<NetworkEvent.Context> sup) {
        var context = sup.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.containerMenu instanceof ResearchStationMenu menu)
                    || menu.containerId != containerId || !menu.stillValid(player)
                    || menu.station() == null || !menu.station().getBlockPos().equals(pos)) return;
            var station = menu.station();
            String result;
            if (action < PREPARE || action > AUTO_PREPARE) return;
            if (station.researcher() != null && !station.researcher().equals(player.getUUID())) {
                result = "busy";
            } else if (action == AUTO_PREPARE) {
                station.setAutoPrepare(!station.autoPrepare());
                result = station.autoPrepare() ? "auto_on" : "auto_off";
            } else if (action == PREPARE || action == PREPARE_BULK) {
                if (station.inputs().getStackInSlot(0).isEmpty() || station.inputs().getStackInSlot(1).isEmpty())
                    result = "missing_inputs";
                else if (station.energyStored() < station.energyPerBatch())
                    result = "insufficient_energy";
                else result = station.preparePoints(action == PREPARE_BULK) > 0 ? "prepared" : "prepare_failed";
            } else if (action == START) {
                result = station.startResearch(player, id) ? "started" : "start_failed";
            } else {
                boolean running = player.getUUID().equals(station.researcher());
                station.pauseResearch(player);
                result = running ? "paused" : "not_running";
            }
            player.displayClientMessage(Component.translatable("gui.transcend.research.action." + result), true);
            menu.refreshSnapshot();
            menu.broadcastChanges();
        });
        context.setPacketHandled(true);
    }
}
