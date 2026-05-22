package com.huige233.transcend.circle.scroll;

import com.huige233.transcend.balance.BalanceConfig;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

/**
 * 灵脉归一 — 区块魔力恢复。Round 37 重平衡：cost 750 → 1500，center 500 → 400，
 * neighbor 200 → 150，确保净消耗为正（防止 mana farming 漏洞）。
 *
 * <p>旧逻辑：cost 750，回 500 + 4×200 = 1300，净 -550（玩家会刷）。
 * 新逻辑：cost 1500，回 400 + 4×150 = 1000，净 +500（合理消耗）。
 */
public class LeylineResyncEffect implements ScrollEffect {

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        // Round 39: 翠绿涟漪 — 灵脉归一
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
