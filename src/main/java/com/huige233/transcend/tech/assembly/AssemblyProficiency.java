package com.huige233.transcend.tech.assembly;

import com.huige233.transcend.Transcend;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 保存各工艺的玩家经验与高收益次数，发放制造经验并在玩家克隆时继承数据。 */
@Mod.EventBusSubscriber(modid = Transcend.MODID)
public final class AssemblyProficiency {
    static final String KEY = "TranscendAssembly";
    private static final String COUNT_SUFFIX = "HighGainCount";

    private AssemblyProficiency() {}

    private static String countKey(String process) { return process + COUNT_SUFFIX; }

    public static int xp(Player player, String process) {
        return Math.max(0, Math.min(AssemblyRules.MAX_XP,
                player.getPersistentData().getCompound(KEY).getInt(process)));
    }

    public static int highGainCount(Player player, String process) {
        return Math.max(0, player.getPersistentData().getCompound(KEY).getInt(countKey(process)));
    }

    public static void award(Player player, String process, int tier) {
        if (player.level().isClientSide) return;
        CompoundTag data = player.getPersistentData().getCompound(KEY);
        int previous = xp(player, process);
        int count = highGainCount(player, process);
        int amount = AssemblyRules.gain(previous, tier, count);
        data.putInt(process, Math.min(AssemblyRules.MAX_XP, previous + amount));
        data.putInt(countKey(process), count == Integer.MAX_VALUE ? count : count + 1);
        player.getPersistentData().put(KEY, data);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        event.getEntity().getPersistentData().put(KEY,
                event.getOriginal().getPersistentData().getCompound(KEY).copy());
    }
}
