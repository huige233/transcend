package com.huige233.transcend.circle.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 千里归乡 — 将施法者传送至重生点（床或世界出生点）。
 */
public class ThousandLeagueReturnEffect implements ScrollEffect {

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        BlockPos target = caster.getRespawnPosition();
        if (target == null) {
            target = level.getSharedSpawnPos();
        }
        // Round 39: 出发点 + 终点双法环（千里归乡）
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.circle(level, pos, 3.0F, 0.5F, 0.9F, 1.0F, 400, "hexagram");
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.circle(
                level, target, 3.0F, 0.5F, 0.9F, 1.0F, 40, "hexagram");
        caster.teleportTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.thousand_league_return_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
