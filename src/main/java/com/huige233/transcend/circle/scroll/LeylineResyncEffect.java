package com.huige233.transcend.circle.scroll;

import com.huige233.transcend.balance.BalanceConfig;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public class LeylineResyncEffect implements ScrollEffect {

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {

        com.huige233.transcend.circle.scroll.ScrollVisualHelper.shieldRipple(level, pos, 16.0F, 0.4F, 1.0F, 0.5F, 400);
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.circle(level, pos, 12.0F, 0.5F, 0.95F, 0.4F, 400, "hexagram");

        BalanceConfig.ScrollBalance s = BalanceConfig.get().scroll;
        ChunkManaSavedData data = ChunkManaSavedData.get(level);
        ChunkPos center = new ChunkPos(pos);
        data.setMana(center, data.getMana(center) + s.leyline_resync_center_restore);

        ChunkPos[] neighbors = new ChunkPos[]{
                new ChunkPos(center.x + 1, center.z),
                new ChunkPos(center.x - 1, center.z),
                new ChunkPos(center.x, center.z + 1),
                new ChunkPos(center.x, center.z - 1)
        };
        for (ChunkPos n : neighbors) {
            data.setMana(n, data.getMana(n) + s.leyline_resync_neighbor_restore);
        }
        return true;
    }

    @Override
    public int getManaCost() {
        return BalanceConfig.get().scroll.leyline_resync_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
