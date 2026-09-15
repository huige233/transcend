package com.huige233.transcend.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

   
                                
                                        
                                                            
   
/** 将实体方法和字段的显示文本及运行时真名下发至因果编辑器界面。 */
public class S2CEntityScanPacket {

    private final int entityId;
    private final List<String> methodLines;
    private final List<String> methodRtNames;
    private final List<String> fieldLines;
    private final List<String> fieldRtNames;

    public S2CEntityScanPacket(int entityId, List<String> methods, List<String> methodRt,
                               List<String> fields, List<String> fieldRt) {
        this.entityId = entityId;
        this.methodLines = new ArrayList<>(methods);
        this.methodRtNames = methodRt == null ? new ArrayList<>() : new ArrayList<>(methodRt);
        this.fieldLines = new ArrayList<>(fields);
        this.fieldRtNames = fieldRt == null ? new ArrayList<>() : new ArrayList<>(fieldRt);
    }

    public S2CEntityScanPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        int m = buf.readVarInt();
        methodLines = new ArrayList<>();
        methodRtNames = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            methodLines.add(buf.readUtf(256));
            methodRtNames.add(buf.readUtf(128));
        }
        int f = buf.readVarInt();
        fieldLines = new ArrayList<>();
        fieldRtNames = new ArrayList<>();
        for (int i = 0; i < f; i++) {
            fieldLines.add(buf.readUtf(256));
            fieldRtNames.add(buf.readUtf(128));
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeVarInt(methodLines.size());
        for (int i = 0; i < methodLines.size(); i++) {
            buf.writeUtf(methodLines.get(i), 256);
            buf.writeUtf(methodRtNames.get(i), 128);
        }
        buf.writeVarInt(fieldLines.size());
        for (int i = 0; i < fieldLines.size(); i++) {
            buf.writeUtf(fieldLines.get(i), 256);
            buf.writeUtf(fieldRtNames.get(i), 128);
        }
    }

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc.screen instanceof com.huige233.transcend.client.TranscendEditScreen screen) {
                        screen.receiveScan(entityId, methodLines, methodRtNames, fieldLines, fieldRtNames);
                    }
                })
        );
        ctx.get().setPacketHandled(true);
    }
}