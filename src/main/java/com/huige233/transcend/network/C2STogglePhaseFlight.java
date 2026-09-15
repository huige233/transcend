package com.huige233.transcend.network;

import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.util.ArmorUtils;
import com.huige233.transcend.util.PhaseFlightState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;


/** 请求切换相位飞行，服务端校验完整套装与方块碰撞状态后更新权威开关并回传。 */
public class C2STogglePhaseFlight {

    public static final String NBT_PHASE_FLIGHT = PhaseFlightState.NBT_PHASE_FLIGHT;

    public C2STogglePhaseFlight() {
    }

    public C2STogglePhaseFlight(FriendlyByteBuf buf) {
    }

    public void write(FriendlyByteBuf buf) {
    }

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            
            if (!ArmorUtils.fullEquipped(player)) {
                player.sendSystemMessage(Component.translatable(
                        "msg.transcend.phase_flight.requires_armor")
                        .withStyle(net.minecraft.ChatFormatting.RED));
                
                PhaseFlightState.pushToClient(player);
                return;
            }

            boolean current = PhaseFlightState.isEnabled(player);

            
            
            if (current && !player.level().noCollision(player, player.getBoundingBox())) {
                player.sendSystemMessage(Component.translatable(
                        "msg.transcend.phase_flight.cant_disable_in_wall")
                        .withStyle(net.minecraft.ChatFormatting.RED));
                PhaseFlightState.pushToClient(player);
                return;
            }

            boolean next = !current;
            
            PhaseFlightState.set(player, next);

            if (next) {
                player.sendSystemMessage(Component.translatable(
                        "msg.transcend.phase_flight.enabled")
                        .withStyle(net.minecraft.ChatFormatting.GREEN));
            } else {
                player.sendSystemMessage(Component.translatable(
                        "msg.transcend.phase_flight.disabled")
                        .withStyle(net.minecraft.ChatFormatting.YELLOW));
                
                player.noPhysics = false;
                player.setNoGravity(false);
            }
        });
        ctx.get().setPacketHandled(true);
    }

                                                    
                                                     
    public static void send() {
        net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) {
            NetworkHandler.CHANNEL.sendToServer(new C2STogglePhaseFlight());
            return;
        }
        boolean has = player.getPersistentData().contains(NBT_PHASE_FLIGHT);
        boolean cur = has && player.getPersistentData().getBoolean(NBT_PHASE_FLIGHT);
        boolean next = !cur;
        player.getPersistentData().putBoolean(NBT_PHASE_FLIGHT, next);
        if (!next) {
            
            player.noPhysics = false;
            player.setNoGravity(false);
        }
        NetworkHandler.CHANNEL.sendToServer(new C2STogglePhaseFlight());
    }
}

