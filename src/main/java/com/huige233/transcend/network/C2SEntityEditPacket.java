package com.huige233.transcend.network;

import com.huige233.transcend.util.TranscendEditService;
import com.huige233.transcend.util.TranscendGuard;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

   
                                           
                                                                          
   
/** 传输实体编辑请求，经服务端持有资格与距离校验后执行扫描、调用、改值、返回值覆盖或冻结。 */
public class C2SEntityEditPacket {

    public static final int MODE_INVOKE_METHOD = 0;
    public static final int MODE_SET_FIELD = 1;
    public static final int MODE_FORCE_RETURN = 2;
    public static final int MODE_RESET_RETURN = 3;
    public static final int MODE_SCAN = 4;
    public static final int MODE_FREEZE = 5;
    public static final int MAX_DISTANCE = 40;

    private final int entityId;
    private final int mode;
    private final String targetName;
    private final String paramTypes;
    private final String paramValues;

    public C2SEntityEditPacket(int entityId, int mode, String targetName,
                               String paramTypes, String paramValues) {
        this.entityId = entityId;
        this.mode = mode;
        this.targetName = targetName == null ? "" : targetName;
        this.paramTypes = paramTypes == null ? "" : paramTypes;
        this.paramValues = paramValues == null ? "" : paramValues;
    }

    public C2SEntityEditPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.mode = buf.readByte();
        this.targetName = buf.readUtf(256);
        this.paramTypes = buf.readUtf(1024);
        this.paramValues = buf.readUtf(2048);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeByte(mode);
        buf.writeUtf(targetName, 256);
        buf.writeUtf(paramTypes, 1024);
        buf.writeUtf(paramValues, 2048);
    }

    
    public static String scanMethods(net.minecraft.world.entity.Entity entity) {
        StringBuilder sb = new StringBuilder();
        for (java.lang.reflect.Method m : entity.getClass().getMethods()) {
            if (java.lang.reflect.Modifier.isStatic(m.getModifiers())) continue;
            int p = m.getParameterCount();
            if (p > 4) continue;
            sb.append(m.getName()).append("(");
            for (int i = 0; i < p; i++) {
                if (i > 0) sb.append(",");
                sb.append(m.getParameterTypes()[i].getSimpleName());
            }
            sb.append("):").append(m.getReturnType().getSimpleName()).append("\n");
        }
        return sb.toString();
    }

    
    public static String scanFields(net.minecraft.world.entity.Entity entity) {
        StringBuilder sb = new StringBuilder();
        for (java.lang.reflect.Field f : entity.getClass().getFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
            try {
                sb.append(f.getName()).append(":").append(f.getType().getSimpleName())
                        .append("=").append(f.get(entity)).append("\n");
            } catch (Exception e) {
                sb.append(f.getName()).append(":").append(f.getType().getSimpleName()).append("=error\n");
            }
        }
        return sb.toString();
    }

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            boolean holdingEditor = player.getMainHandItem().getItem() instanceof com.huige233.transcend.items.tools.TranscendEditWand
                    || player.getOffhandItem().getItem() instanceof com.huige233.transcend.items.tools.TranscendEditWand;
            if (!holdingEditor && !TranscendGuard.isProtected(player)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.transcend.editor.hold_required"));
                return;
            }
            Entity entity = player.serverLevel().getEntity(entityId);
            if (!(entity instanceof LivingEntity living)) return;
            if (player.distanceTo(entity) > MAX_DISTANCE) return;

            switch (mode) {
                case MODE_SCAN -> TranscendEditService.scan(player, living);
                case MODE_INVOKE_METHOD -> TranscendEditService.invokeMethod(player, living, targetName, paramTypes, paramValues);
                case MODE_SET_FIELD -> TranscendEditService.setField(player, living, targetName, paramValues);
                case MODE_FORCE_RETURN -> TranscendEditService.forceReturn(player, living, targetName, paramTypes, paramValues);
                case MODE_RESET_RETURN -> TranscendEditService.resetReturn(player, living, targetName, paramTypes);
                case MODE_FREEZE -> TranscendEditService.setFrozen(player, living, !paramValues.isEmpty() && paramValues.equals("1"));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}