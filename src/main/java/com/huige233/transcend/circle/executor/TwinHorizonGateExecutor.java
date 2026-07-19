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

public class TwinHorizonGateExecutor implements CircleFunctionExecutor {

    private static final int TELEPORT_COST = 20;

    private static final int PLAYER_COOLDOWN_TICKS = 200;

    private static final double TRIGGER_RADIUS = 2.0;

    private final Map<BlockPos, Map<UUID, Long>> cooldowns = new HashMap<>();

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {

    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        ServerLevel level = ctx.getLevel();
        if (level == null) {
            return;
        }

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

        Iterator<Map.Entry<UUID, Long>> it = playerCooldowns.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue() > PLAYER_COOLDOWN_TICKS) {
                it.remove();
            }
        }

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
