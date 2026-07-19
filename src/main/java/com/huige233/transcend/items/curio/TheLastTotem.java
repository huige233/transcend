package com.huige233.transcend.items.curio;

import com.huige233.transcend.ModRarities;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.function.Consumer;

public class TheLastTotem extends Item {
    public TheLastTotem(){
        super(new Item.Properties().stacksTo(1).durability(32767).rarity(ModRarities.COSMIC));
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
        if (!entity.level().isClientSide) {
            RandomSource random = entity.getRandom();
            float r = random.nextFloat();
            if (r < 0.0001f) {stack.setDamageValue(0);}
            else if (r < 0.0101f) {repair(stack, 5);}
            else if (r < 0.0401f) {repair(stack, 3);}
            else if (r < 0.0901f) {repair(stack, 1);}
        }
        return amount;
    }

    private static void repair(ItemStack stack, int amount) {stack.setDamageValue(Math.max(0, stack.getDamageValue() - amount));}

    public static boolean isOpt(ItemStack stack) {
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT).trim();
        return name.equals("optifine");
    }
}
