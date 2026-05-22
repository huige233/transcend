package com.huige233.transcend.circle.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 百器归全 — 6格内最多64件可修复物品共计修复1800点耐久。
 * 单件被修复100点以上则获得速度I 200tick。
 */
public class UnbrokenArsenalEffect implements ScrollEffect {

    private static final int RADIUS = 6;
    private static final int MAX_ITEMS = 64;
    private static final int TOTAL_REPAIR = 1800;
    private static final int SPEED_THRESHOLD = 100;
    private static final int SPEED_DURATION = 200;

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        // 收集附近玩家所有可修复装备
        List<Player> players = level.getEntitiesOfClass(
                Player.class, ScrollEffectUtil.radiusBox(pos, RADIUS), p -> true);
        List<ItemStack> repairable = new ArrayList<>();
        List<Player> stackOwners = new ArrayList<>();
        for (Player p : players) {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack stack = p.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.isDamageableItem() && stack.getDamageValue() > 0) {
                    repairable.add(stack);
                    stackOwners.add(p);
                    if (repairable.size() >= MAX_ITEMS) break;
                }
            }
            if (repairable.size() >= MAX_ITEMS) break;
        }

        // 同样修复地上的可修复物品实体
        if (repairable.size() < MAX_ITEMS) {
            List<ItemEntity> drops = level.getEntitiesOfClass(
                    ItemEntity.class, ScrollEffectUtil.radiusBox(pos, RADIUS), e -> true);
            for (ItemEntity ie : drops) {
                ItemStack stack = ie.getItem();
                if (!stack.isEmpty() && stack.isDamageableItem() && stack.getDamageValue() > 0) {
                    repairable.add(stack);
                    stackOwners.add(null);
                    if (repairable.size() >= MAX_ITEMS) break;
                }
            }
        }

        if (repairable.isEmpty()) {
            return false;
        }

        // 平均分配 1800 点耐久
        int budget = TOTAL_REPAIR;
        int n = repairable.size();
        int perItem = Math.max(1, budget / n);

        for (int i = 0; i < n && budget > 0; i++) {
            ItemStack stack = repairable.get(i);
            int damage = stack.getDamageValue();
            int repair = Math.min(damage, Math.min(perItem, budget));
            if (repair <= 0) continue;
            stack.setDamageValue(damage - repair);
            budget -= repair;
            if (repair >= SPEED_THRESHOLD) {
                Player owner = stackOwners.get(i);
                if (owner != null) {
                    owner.addEffect(new MobEffectInstance(
                            MobEffects.MOVEMENT_SPEED, SPEED_DURATION, 0, false, true));
                }
            }
        }

        // 把剩余预算分配给损坏最严重的物品
        if (budget > 0) {
            for (int i = 0; i < n && budget > 0; i++) {
                ItemStack stack = repairable.get(i);
                int damage = stack.getDamageValue();
                if (damage <= 0) continue;
                int repair = Math.min(damage, budget);
                stack.setDamageValue(damage - repair);
                budget -= repair;
            }
        }

        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.unbroken_arsenal_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
