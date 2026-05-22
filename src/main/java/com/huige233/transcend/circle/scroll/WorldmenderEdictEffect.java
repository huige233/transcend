package com.huige233.transcend.circle.scroll;

import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 修复万物诏 — 32格半径，玩家治疗20HP+再生III 200tick，作物随机生长x5，区块魔力+500。
 */
public class WorldmenderEdictEffect implements ScrollEffect {

    private static final int RADIUS = 32;
    private static final float HEAL_AMOUNT = 20.0F;
    private static final int REGEN_DURATION = 200;
    private static final int REGEN_AMP = 2; // III
    private static final int CROP_TICKS = 5;

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        // Round 39: 翠绿涟漪 + 治愈法环
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.shieldRipple(level, pos, RADIUS, 0.5F, 1.0F, 0.6F, 400);
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.circle(level, pos, RADIUS * 0.7F, 0.4F, 1.0F, 0.5F, 400, "hexagram");

        // 治疗玩家 + 再生
        List<Player> players = level.getEntitiesOfClass(
                Player.class, ScrollEffectUtil.radiusBox(pos, RADIUS), p -> true);
        for (Player player : players) {
            player.heal(HEAL_AMOUNT);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_DURATION, REGEN_AMP, false, true));
        }

        // 作物随机生长
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int r = RADIUS;
        int r2 = r * r;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2) continue;
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.getBlock() instanceof CropBlock crop) {
                        BlockPos immutable = cursor.immutable();
                        for (int i = 0; i < CROP_TICKS; i++) {
                            BlockState s = level.getBlockState(immutable);
                            if (s.getBlock() instanceof CropBlock c) {
                                c.randomTick(s, level, immutable, level.random);
                            }
                        }
                    }
                }
            }
        }

        // 恢复区块魔力（Round 37: 数据驱动）
        ChunkManaSavedData data = ChunkManaSavedData.get(level);
        ChunkPos cpos = new ChunkPos(pos);
        data.setMana(cpos, data.getMana(cpos) +
                com.huige233.transcend.balance.BalanceConfig.get().scroll.worldmender_mana_restore);

        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.worldmender_edict_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
