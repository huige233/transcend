package com.huige233.transcend.circle.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;

/**
 * 百纸将军 — 召唤 16 只无敌静默的鸡作为诱饵。
 *
 * <p>Round 37 修复：之前召唤的鸡 INVULNERABLE 永不消失（违反 desc 的 "75秒"）。
 * 现在通过 NBT TAG <code>transcend_decoy_expiry</code> 记录死亡 game time，
 * 由 {@link PaperLegionDecoyHandler} 监听 LivingTickEvent 在到期时 discard。
 */
public class PaperLegionEffect implements ScrollEffect {

    private static final int COUNT = 16;
    private static final double SPAWN_RADIUS = 2.5D;
    /** 写入到 chicken NBT 的 key — Round 37 ticker reads this */
    public static final String TAG_DECOY_EXPIRY = "transcend_decoy_expiry";

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        // Round 39: 白绿小法环（百纸将军 — 召唤诱饵）
        ScrollVisualHelper.circle(level, pos, (float)(SPAWN_RADIUS + 1), 0.8F, 1.0F, 0.7F, 240, "hexagram");

        int lifetime = com.huige233.transcend.balance.BalanceConfig.get().scroll.paper_legion_lifetime_ticks;
        long expiry = level.getGameTime() + lifetime;
        for (int i = 0; i < COUNT; i++) {
            Chicken chicken = EntityType.CHICKEN.create(level);
            if (chicken == null) continue;
            double angle = (Math.PI * 2.0D * i) / COUNT;
            double x = pos.getX() + 0.5D + Math.cos(angle) * SPAWN_RADIUS;
            double z = pos.getZ() + 0.5D + Math.sin(angle) * SPAWN_RADIUS;
            chicken.moveTo(x, pos.getY(), z, level.random.nextFloat() * 360F, 0F);
            chicken.setInvulnerable(true);
            chicken.setSilent(true);
            chicken.addTag("transcend_decoy");
            // 关键：persistent NBT tag — survives chunk unload
            chicken.getPersistentData().putLong(TAG_DECOY_EXPIRY, expiry);
            chicken.setCustomName(net.minecraft.network.chat.Component.literal("Paper Soldier"));
            chicken.setCustomNameVisible(false);
            // 设定起火秒数为 0（确保不在燃烧）
            chicken.setSecondsOnFire(0);
            level.addFreshEntity(chicken);
        }
        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.paper_legion_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
