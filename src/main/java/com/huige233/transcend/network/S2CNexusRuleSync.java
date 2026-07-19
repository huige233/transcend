package com.huige233.transcend.network;

import com.huige233.transcend.world.nexus.NexusType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

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

            NexusClientState.setDestroyedNexuses(destroyedIds);
        });
        ctx.get().setPacketHandled(true);
    }

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
