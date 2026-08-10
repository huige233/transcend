package com.huige233.transcend.ascension;

import com.huige233.transcend.Transcend;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
/** 玩家飞升(领域)数据的 Capability 封装。 */
public class AscensionCapability {

    public static final Capability<PlayerAscensionData> ASCENSION =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation ID = Transcend.rl("ascension");

    public static PlayerAscensionData get(Player player) {
        return player.getCapability(ASCENSION).orElseGet(PlayerAscensionData::new);
    }

    public static void ifPresent(Player player, net.minecraftforge.common.util.NonNullConsumer<PlayerAscensionData> action) {
        player.getCapability(ASCENSION).ifPresent(action);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
        if (!(event.getObject() instanceof Player)) return;
        event.addCapability(ID, new Provider());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {

        event.getOriginal().reviveCaps();
        try {
            PlayerAscensionData from = get(event.getOriginal());
            PlayerAscensionData to = get(event.getEntity());
            to.copyFrom(from);
        } finally {
            event.getOriginal().invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {

        if (event.getEntity() instanceof ServerPlayer sp) {
            PlayerAscensionData data = get(sp);
            AscensionHandler.applyPersistentStats(sp, data);
            AscensionHandler.syncToClient(sp, data);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PlayerAscensionData data = get(sp);
            AscensionHandler.applyPersistentStats(sp, data);
            AscensionHandler.syncToClient(sp, data);
        }
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {

        private final PlayerAscensionData data = new PlayerAscensionData();
        private final LazyOptional<PlayerAscensionData> optional = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return ASCENSION.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.save();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.load(nbt);
        }

        public void invalidate() {
            optional.invalidate();
        }
    }
}
