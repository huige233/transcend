package com.huige233.transcend.circle.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 万箱归序 — 按物品ID排序施法者背包主区域，可堆叠物品合并成完整堆。
 */
public class OrderedVaultEffect implements ScrollEffect {

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        Inventory inv = caster.getInventory();
        // 仅排序主背包 36 格（0..35），不动盔甲、副手与热键之外的特殊位
        int size = 36;
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) stacks.add(s.copy());
        }

        // 合并相同物品到完整堆
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack s : stacks) {
            boolean placed = false;
            for (ItemStack m : merged) {
                if (ItemStack.isSameItemSameTags(m, s) && m.getCount() < m.getMaxStackSize()) {
                    int canAdd = Math.min(s.getCount(), m.getMaxStackSize() - m.getCount());
                    m.grow(canAdd);
                    s.shrink(canAdd);
                    if (s.isEmpty()) {
                        placed = true;
                        break;
                    }
                }
            }
            if (!placed && !s.isEmpty()) {
                merged.add(s);
            }
        }

        // 按 item id 排序
        merged.sort(Comparator.comparing(stack -> {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return id == null ? "" : id.toString();
        }));

        // 写回背包
        for (int i = 0; i < size; i++) {
            if (i < merged.size()) {
                inv.setItem(i, merged.get(i));
            } else {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }

        caster.inventoryMenu.broadcastChanges();
        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.ordered_vault_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
