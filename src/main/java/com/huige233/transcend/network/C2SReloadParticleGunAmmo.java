package com.huige233.transcend.network;

import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.items.tech.ParticleGun;
import com.huige233.transcend.tech.ammo.AmmoType;
import com.huige233.transcend.tech.ammo.BuiltInAmmoTypes;
import com.huige233.transcend.tech.gun.GunInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;


/** 校验内置弹药白名单与主手粒子枪后请求服务端换弹，并同步背包和换弹结果。 */
public final class C2SReloadParticleGunAmmo {
    private static final int MAX_AMMO_ID_LENGTH = 16;
    private final String ammoId;

    public C2SReloadParticleGunAmmo(String ammoId) {
        this.ammoId = ammoId == null ? "" : ammoId;
    }

    public C2SReloadParticleGunAmmo(FriendlyByteBuf buffer) {
        this(buffer.readUtf(MAX_AMMO_ID_LENGTH));
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(ammoId, MAX_AMMO_ID_LENGTH);
    }

    public void run(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.isUsingItem()) return;

            ItemStack stack = player.getMainHandItem();
            AmmoType ammo = requestedAmmo(ammoId);
            if (!(stack.getItem() instanceof ParticleGun) || ammo == null) {
                player.displayClientMessage(Component.translatable("msg.transcend.particle_gun.reload_failed")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }

            GunInstance gun = GunInstance.of(stack);
            if (!gun.reload(player, ammo)) {
                player.displayClientMessage(Component.translatable("msg.transcend.particle_gun.reload_failed")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }

            player.inventoryMenu.broadcastChanges();
            player.displayClientMessage(Component.translatable("msg.transcend.particle_gun.reloaded",
                    ammo.displayName(), gun.magazine().charges()).withStyle(ChatFormatting.GREEN), true);
        });
        context.setPacketHandled(true);
    }

    
    public static AmmoType requestedAmmo(String ammoId) {
        if (ammoId == null || ammoId.isBlank() || ammoId.length() > MAX_AMMO_ID_LENGTH) return null;
        return BuiltInAmmoTypes.byId(ammoId);
    }

    public static void send(String ammoId) {
        NetworkHandler.CHANNEL.sendToServer(new C2SReloadParticleGunAmmo(ammoId));
    }
}
