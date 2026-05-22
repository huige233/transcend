package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.items.SpellElementItem;
import com.huige233.transcend.spell.SpellElement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 元素熔炉（Elemental Crucible）功能执行器（v1 简化实现）。
 * <p>
 * 每 200 次 tick 调用扫描第一个催化剂槽位，如果是 {@link SpellElementItem}，
 * 则消耗一个并产出一个不同元素的元素结晶作为掉落物。
 */
public class ElementalCrucibleExecutor implements CircleFunctionExecutor {

    private static final int CONVERSION_INTERVAL = 200;
    private static final int MANA_COST = 5;

    private final Map<BlockPos, Integer> tickCounters = new HashMap<>();

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        // 无需初始化
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        BlockPos key = ctx.getCorePos();
        int counter = tickCounters.getOrDefault(key, 0) + 1;
        if (counter < CONVERSION_INTERVAL) {
            tickCounters.put(key.immutable(), counter);
            return;
        }
        tickCounters.put(key.immutable(), 0);

        List<ItemStack> catalysts = ctx.getCatalystStacks();
        if (catalysts.isEmpty()) {
            return;
        }

        ItemStack slot0 = catalysts.get(0);
        if (slot0.isEmpty() || !(slot0.getItem() instanceof SpellElementItem inputItem)) {
            return;
        }

        SpellElement inputElement = inputItem.getElement();
        SpellElement[] all = SpellElement.values();
        if (all.length <= 1) {
            return;
        }

        ServerLevel level = ctx.getLevel();
        RandomSource random = level.getRandom();

        // 选一个不同的元素
        SpellElement target;
        do {
            target = all[random.nextInt(all.length)];
        } while (target == inputElement);

        Item outputItem = findElementItem(target);
        if (outputItem == null) {
            return;
        }

        if (!ctx.consumeMana(MANA_COST)) {
            return;
        }

        // 消耗催化剂中的一个元素（注意：catalystStacks 是上下文副本，但 BE 缓存的是引用列表
        // 中的同一 ItemStack 对象 — 减小 count 会反映回原栏位）
        slot0.shrink(1);

        BlockPos core = ctx.getCorePos();
        ItemEntity entity = new ItemEntity(level,
                core.getX() + 0.5, core.getY() + 1.0, core.getZ() + 0.5,
                new ItemStack(outputItem));
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    /** 在 ModItems 中查找对应元素的注册物品。 */
    private Item findElementItem(SpellElement target) {
        for (RegistryObject<Item> obj : ModItems.ITEMS_REGISTRY.getEntries()) {
            Item item = obj.get();
            if (item instanceof SpellElementItem se && se.getElement() == target) {
                return item;
            }
        }
        return null;
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        tickCounters.remove(ctx.getCorePos());
    }
}
