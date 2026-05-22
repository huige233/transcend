package com.huige233.transcend.block.circle;

import com.huige233.transcend.Transcend;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * Round 45: 法环近距离怪物死亡献祭 — 血魔法风味 #2。
 *
 * <p>当任何 mob（非玩家）在法环核心 8 格球内死亡 →
 * 把 {@code maxHealth × 5} mana 注入该核心（封顶 100 mana / kill）。
 *
 * <p>玩家可故意把怪引到法环旁宰杀 → 比 mana_lens 慢慢吸取快得多。
 * 与 {@link com.huige233.transcend.items.SacrificialKnifeItem} 共同构成
 * "血魔法" 主题的快速 mana 注入路径。
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID)
public class CoreBloodSacrificeHandler {

    private static final int SEARCH_RADIUS = 8;
    private static final int MAX_MANA_PER_KILL = 100;
    private static final float MANA_PER_HP = 5.0F;

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide) return;
        if (victim instanceof Player) return; // 玩家死不算
        if (!(victim instanceof Mob)) return;  // 仅 mob

        // 寻找最近的有效 core
        BlockPos vPos = victim.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        MagicCircleCoreBlockEntity nearest = null;
        double bestDistSq = Double.MAX_VALUE;
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    if (dx*dx + dy*dy + dz*dz > SEARCH_RADIUS * SEARCH_RADIUS) continue;
                    cursor.set(vPos.getX() + dx, vPos.getY() + dy, vPos.getZ() + dz);
                    BlockEntity be = level.getBlockEntity(cursor);
                    if (be instanceof MagicCircleCoreBlockEntity core && core.isStructureValid()) {
                        double d = cursor.distSqr(vPos);
                        if (d < bestDistSq) {
                            bestDistSq = d;
                            nearest = core;
                        }
                    }
                }
            }
        }
        if (nearest == null) return;

        // 计算注入量
        int amount = (int) Math.min(MAX_MANA_PER_KILL, victim.getMaxHealth() * MANA_PER_HP);
        if (amount <= 0) return;
        int inserted = nearest.insertMana(amount);
        if (inserted <= 0) return;

        // 视觉：红色粒子从受害者飞向 core
        if (level instanceof ServerLevel sl) {
            BlockPos cp = nearest.getBlockPos();
            for (int i = 0; i < 12; i++) {
                double t = i / 12.0;
                double x = victim.getX() + (cp.getX() + 0.5 - victim.getX()) * t;
                double y = victim.getY() + victim.getBbHeight() * 0.5 + (cp.getY() + 0.5 - victim.getY() - victim.getBbHeight() * 0.5) * t;
                double z = victim.getZ() + (cp.getZ() + 0.5 - victim.getZ()) * t;
                sl.sendParticles(new DustParticleOptions(new Vector3f(0.75F, 0.0F, 0.05F), 1.2F),
                        x, y, z, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }
}
