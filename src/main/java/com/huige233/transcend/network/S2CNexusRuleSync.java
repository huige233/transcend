package com.huige233.transcend.network;

import com.huige233.transcend.world.nexus.NexusType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：同步已摧毁枢纽的ID列表。
 * 客户端据此知道哪些法则已被打破，用于视觉提示。
 */
public class S2CNexusRuleSync {

    private final Set<String> destroyedIds;

    public S2CNexusRuleSync(Set<String> destroyedIds) {
        this.destroyedIds = new HashSet<>(destroyedIds);
    }

    public S2CNexusRuleSync(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        this.destroyedIds = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            destroyedIds.add(buf.readUtf(64));
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(destroyedIds.size());
        for (String id : destroyedIds) {
            buf.writeUtf(id, 64);
        }
    }

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Store on client for HUD/visual indicators
            NexusClientState.setDestroyedNexuses(destroyedIds);
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * 客户端存储 — 记住已摧毁的枢纽列表供渲染使用。
     */
    public static class NexusClientState {
        private static final Set<String> destroyedNexuses = new HashSet<>();

        public static void setDestroyedNexuses(Set<String> ids) {
            destroyedNexuses.clear();
            destroyedNexuses.addAll(ids);
        }

        public static boolean isDestroyed(NexusType type) {
            return destroyedNexuses.contains(type.id);
        }

        public static int getDestroyedCount() {
            return NexusType.countDestroyed(destroyedNexuses);
        }

        public static boolean allDestroyed() {
            return getDestroyedCount() >= NexusType.values().length;
        }
    }
}
