package com.huige233.transcend.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/**
 * 法术组件全局战利品修饰器。
 * 通过 JSON 数据包配置，向怪物掉落和结构宝箱中注入法术组件。
 */
public class SpellComponentLootModifier extends LootModifier {

    private final Item addedItem;
    private final float chance;

    public static final Codec<SpellComponentLootModifier> CODEC = RecordCodecBuilder.create(inst ->
        codecStart(inst).and(
            net.minecraftforge.registries.ForgeRegistries.ITEMS.getCodec()
                .fieldOf("item").forGetter(m -> m.addedItem)
        ).and(
            Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance)
        ).apply(inst, SpellComponentLootModifier::new)
    );

    public SpellComponentLootModifier(LootItemCondition[] conditions, Item item, float chance) {
        super(conditions);
        this.addedItem = item;
        this.chance = chance;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() < chance) {
            generatedLoot.add(new ItemStack(addedItem));
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
