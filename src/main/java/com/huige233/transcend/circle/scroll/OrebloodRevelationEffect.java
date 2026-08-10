package com.huige233.transcend.circle.scroll;

import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.network.S2COreRevealPack;
import com.huige233.transcend.network.S2COreRevealPack.OreEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** 矿脉显迹卷轴效果实现。 */
public class OrebloodRevelationEffect implements ScrollEffect {

    private static int durationTicks() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.oreblood_duration;
    }

    private static final int RADIUS = 24;

    private static final int MAX_ORES = 1024;

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        int dur = durationTicks();

        com.huige233.transcend.circle.scroll.ScrollVisualHelper.circle(
                level, pos, RADIUS, 0.95F, 0.6F, 0.2F, dur, "hexagram");
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.shieldRipple(
                level, pos, RADIUS * 0.5F, 0.95F, 0.6F, 0.2F, dur);

        caster.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,
                dur, 0, false, true));

        List<OreEntry> entries = scanOres(level, pos);
        if (entries.isEmpty()) {

            return true;
        }

        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> caster),
                new S2COreRevealPack(pos, dur, entries));

        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.oreblood_revelation_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }

    private static List<OreEntry> scanOres(ServerLevel level, BlockPos center) {
        List<OreEntry> hits = new ArrayList<>();
        int r = RADIUS;
        int rSq = r * r;

        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int dxSq = dx * dx + dz * dz;
                if (dxSq > rSq) continue;
                for (int dy = -r; dy <= r; dy++) {
                    if (dxSq + dy * dy > rSq) continue;

                    mp.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(mp);
                    if (!isOre(state)) continue;

                    int color = oreColor(state);
                    hits.add(new OreEntry(mp.immutable(), color));

                    if (hits.size() >= MAX_ORES) {
                        return hits;
                    }
                }
            }
        }
        return hits;
    }

    private static boolean isOre(BlockState state) {

        return state.is(BlockTags.COAL_ORES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.EMERALD_ORES)
                || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.REDSTONE_ORES)
                || state.is(BlockTags.COPPER_ORES);
    }

    private static int oreColor(BlockState state) {
        if (state.is(BlockTags.DIAMOND_ORES))   return 0x29DDE6;
        if (state.is(BlockTags.EMERALD_ORES))   return 0x39C25C;
        if (state.is(BlockTags.GOLD_ORES))      return 0xFFE34A;
        if (state.is(BlockTags.LAPIS_ORES))     return 0x3D72E6;
        if (state.is(BlockTags.REDSTONE_ORES))  return 0xE5251F;
        if (state.is(BlockTags.COPPER_ORES))    return 0xE07A41;
        if (state.is(BlockTags.IRON_ORES))      return 0xC8AA8A;
        if (state.is(BlockTags.COAL_ORES))      return 0x222222;
        return 0xFF55FF;
    }
}
