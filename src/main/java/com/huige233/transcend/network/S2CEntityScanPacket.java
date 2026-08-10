package com.huige233.transcend.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 超越编辑 S2C：服务端下发目标实体的方法/字段扫描结果。
 */
public class S2CEntityScanPacket {

    private final int entityId;
    private final List<String> methodLines;
    private final List<String> fieldLines;

    public S2CEntityScanPacket(int entityId, List<String> methods, List<String> fields) {
        this.entityId = entityId;
        this.methodLines = new ArrayList<>(methods);
        this.fieldLines = new ArrayList<>(fields);
    }

    public S2CEntityScanPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        int m = buf.readVarInt();
        methodLines = new ArrayList<>();
        for (int i = 0; i < m; i++) methodLines.add(buf.readUtf(256));
        int f = buf.readVarInt();
        fieldLines = new ArrayList<>();
        for (int i = 0; i < f; i++) fieldLines.add(buf.readUtf(256));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeVarInt(methodLines.size());
        for (String s : methodLines) buf.writeUtf(s, 256);
        buf.writeVarInt(fieldLines.size());
        for (String s : fieldLines) buf.writeUtf(s, 256);
    }

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc.screen instanceof com.huige233.transcend.client.TranscendEditScreen screen) {
                        screen.receiveScan(entityId, methodLines, fieldLines);
                    }
                })
        );
        ctx.get().setPacketHandled(true);
    }
}