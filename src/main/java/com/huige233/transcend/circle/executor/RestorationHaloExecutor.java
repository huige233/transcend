package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 修复光环（Restoration Halo）功能执行器。
 * <p>
 * 每 100 tick 为半径内每位玩家身上的 4 件护甲各回复 1 点耐久。
 * 仅对处于已损坏状态（{@link ItemStack#isDamaged()}）的装备生效。
 */
public class RestorationHaloExecutor implements CircleFunctionExecutor {

    /** 修复周期（tick）。 */
    private static final int REPAIR_INTERVAL_TICKS = 100;

    /** 护甲槽位列表。 */
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[] {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private int timer = 0;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ADEPT.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        timer = 0;
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        if (ctx.getLevel() == null) {
            return;
        }

        timer += 20;
        if (timer < REPAIR_INTERVAL_TICKS) {
            return;
        }
        timer = 0;

        List<Player> players = getPlayersInRadius(ctx);
        for (Player player : players) {
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty() && stack.isDamaged()) {
                    stack.setDamageValue(stack.getDamageValue() - 1);
                }
            }
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        timer = 0;
    }

    /** 在以核心方块为中心范围内查找所有玩家。 */
    private List<Player> getPlayersInRadius(CircleFunctionContext ctx) {
        double r = ctx.getBaseRadius();
        BlockPos pos = ctx.getCorePos();
        AABB area = new AABB(
                pos.getX() - r, pos.getY() - 2, pos.getZ() - r,
                pos.getX() + r, pos.getY() + 4, pos.getZ() + r
        );
        return ctx.getLevel().getEntitiesOfClass(Player.class, area);
    }
}
