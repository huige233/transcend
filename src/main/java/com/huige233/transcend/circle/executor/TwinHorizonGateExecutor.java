package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.items.circle.BoundAetherPearlItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 双生境门（Twin Horizon Gate）功能执行器 — 配对传送门（T3+）。
 *
 * <p>v1 简化逻辑：
 * <ul>
 *     <li>检查催化剂中是否存在已绑定坐标的 {@link BoundAetherPearlItem}。</li>
 *     <li>当玩家在核心方块上潜行时，将其传送到绑定坐标（同维度）。</li>
 *     <li>每名玩家有 200 tick 冷却，防止反复触发。</li>
 *     <li>每次传送消耗 20 CM。</li>
 * </ul>
 */
public class TwinHorizonGateExecutor implements CircleFunctionExecutor {

    /** 单次传送消耗的 CM。 */
    private static final int TELEPORT_COST = 20;
    /** 同一玩家两次传送之间的冷却（tick）。 */
    private static final int PLAYER_COOLDOWN_TICKS = 200;
    /** 玩家触发传送所需要的核心方块周围水平距离。 */
    private static final double TRIGGER_RADIUS = 2.0;

    /**
     * 每个核心位置下的玩家冷却映射。
     * <p>外层 key 必须使用 {@link BlockPos#immutable()} 的实例，因为执行器是单例。
     */
    private final Map<BlockPos, Map<UUID, Long>> cooldowns = new HashMap<>();

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        // 无需特殊初始化
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        ServerLevel level = ctx.getLevel();
        if (level == null) {
            return;
        }

        // 查找绑定后的以太珠
        ItemStack pearl = findBoundPearl(ctx);
        if (pearl == null) {
            return;
        }
        Optional<BlockPos> boundPos = BoundAetherPearlItem.getBoundPos(pearl);
        if (boundPos.isEmpty()) {
            return;
        }

        BlockPos corePos = ctx.getCorePos().immutable();
        Map<UUID, Long> playerCooldowns = cooldowns.computeIfAbsent(corePos, k -> new HashMap<>());
        long now = level.getGameTime();

        // 清理过期冷却条目，避免无限增长
        Iterator<Map.Entry<UUID, Long>> it = playerCooldowns.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue() > PLAYER_COOLDOWN_TICKS) {
                it.remove();
            }
        }

        // 扫描核心附近的玩家
        AABB area = new AABB(
                corePos.getX() - TRIGGER_RADIUS, corePos.getY() - 1, corePos.getZ() - TRIGGER_RADIUS,
                corePos.getX() + TRIGGER_RADIUS + 1, corePos.getY() + 3, corePos.getZ() + TRIGGER_RADIUS + 1
        );
        List<Player> players = level.getEntitiesOfClass(Player.class, area);
        if (players.isEmpty()) {
            return;
        }

        BlockPos dest = boundPos.get();
        for (Player player : players) {
            if (!player.isShiftKeyDown()) {
                continue;
            }
            UUID id = player.getUUID();
            Long last = playerCooldowns.get(id);
            if (last != null && now - last < PLAYER_COOLDOWN_TICKS) {
                continue;
            }
            // 必须有足够魔力才能传送
            if (!ctx.consumeMana(TELEPORT_COST)) {
                return;
            }
            player.teleportTo(dest.getX() + 0.5, dest.getY() + 1.0, dest.getZ() + 0.5);
            playerCooldowns.put(id, now);
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        cooldowns.remove(ctx.getCorePos().immutable());
    }

    /** 在催化剂列表中查找第一枚已绑定坐标的以太珠。 */
    private ItemStack findBoundPearl(CircleFunctionContext ctx) {
        for (ItemStack stack : ctx.getCatalystStacks()) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof BoundAetherPearlItem
                    && BoundAetherPearlItem.isBound(stack)) {
                return stack;
            }
        }
        return null;
    }
}
