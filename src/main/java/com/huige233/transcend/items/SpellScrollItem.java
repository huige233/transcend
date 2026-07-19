package com.huige233.transcend.items;

import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.spell.SpellCarrier;
import com.huige233.transcend.spell.SpellEffect;
import com.huige233.transcend.spell.SpellElement;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpellScrollItem extends Item {

    public SpellScrollItem() {
        super(new Properties().stacksTo(1));
        ModItems.ITEMS.add(this);
    }

    public static ItemStack createScroll(SpellCarrier carrier, SpellElement element,
                                         @Nullable SpellEffect effect, float power, float cooldown) {
        ItemStack stack = new ItemStack(ModItems.spell_scroll.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("carrier", carrier.id);
        tag.putString("element", element.id);
        tag.putString("effect", effect != null ? effect.id : "");
        tag.putFloat("base_power", power);
        tag.putFloat("base_cooldown", cooldown);
        return stack;
    }

    public static SpellCarrier getCarrier(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("carrier")) {
            return SpellCarrier.ORB;
        }
        String id = tag.getString("carrier");
        if (id.isEmpty()) return SpellCarrier.ORB;
        SpellCarrier carrier = SpellCarrier.getById(id);
        if (carrier == null) throw new IllegalArgumentException("Unknown spell carrier id: " + id);
        return carrier;
    }

    public static SpellElement getElement(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("element")) {
            return SpellElement.FIRE;
        }
        String id = tag.getString("element");
        if (id.isEmpty()) return SpellElement.FIRE;
        SpellElement element = SpellElement.getById(id);
        if (element == null) throw new IllegalArgumentException("Unknown spell element id: " + id);
        return element;
    }

    @Nullable
    public static SpellEffect getEffect(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("effect")) {
            return null;
        }
        String id = tag.getString("effect");
        if (id.isEmpty()) return null;
        SpellEffect effect = SpellEffect.getById(id);
        if (effect == null) throw new IllegalArgumentException("Unknown spell effect id: " + id);
        return effect;
    }

    public static float getBasePower(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("base_power")) {
            return 1.0F;
        }
        return tag.getFloat("base_power");
    }

    public static float getBaseCooldown(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("base_cooldown")) {
            return 1.0F;
        }
        return tag.getFloat("base_cooldown");
    }

    public static int getManaCost(ItemStack stack) {
        SpellElement element = getElement(stack);
        SpellEffect effect = getEffect(stack);
        int cost = element.getManaCost();
        if (effect != null) {
            cost += effect.getExtraManaCost();
        }
        return cost;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getEffect(stack) != null;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        if (getEffect(stack) != null) {
            return Rarity.EPIC;
        }
        return Rarity.RARE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        player.displayClientMessage(Component.translatable("msg.transcend.scroll.migration")
                .withStyle(ChatFormatting.YELLOW), true);
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        SpellCarrier carrier = getCarrier(stack);
        SpellElement element = getElement(stack);
        SpellEffect effect = getEffect(stack);
        float power = getBasePower(stack);
        float cooldown = getBaseCooldown(stack);
        int upgradeLevel = stack.getOrCreateTag().getInt("upgrade_level");

        if (upgradeLevel > 0) {
            tooltip.add(Component.translatable("tooltip.transcend.scroll.upgrade_level", upgradeLevel)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        tooltip.add(Component.translatable("tooltip.transcend.spell_scroll.carrier")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" "))
                .append(Component.translatable(carrier.getDisplayKey()).withStyle(ChatFormatting.GOLD)));

        ChatFormatting elementColor = getElementColor(element);
        tooltip.add(Component.translatable("tooltip.transcend.spell_scroll.element")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" "))
                .append(Component.translatable(element.getDisplayKey()).withStyle(elementColor)));

        if (effect != null) {
            tooltip.add(Component.translatable("tooltip.transcend.spell_scroll.effect")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(" "))
                    .append(Component.translatable(effect.getDisplayKey()).withStyle(ChatFormatting.LIGHT_PURPLE)));
        }

        tooltip.add(Component.empty());

        if (power != 1.0F) {
            tooltip.add(Component.translatable("tooltip.transcend.spell_scroll.power",
                            String.format("%.1fx", power))
                    .withStyle(ChatFormatting.GREEN));
        }

        if (cooldown != 1.0F) {
            tooltip.add(Component.translatable("tooltip.transcend.spell_scroll.cooldown",
                            String.format("%.1fx", cooldown))
                    .withStyle(ChatFormatting.AQUA));
        }

        tooltip.add(Component.translatable("tooltip.transcend.spell_scroll.mana_cost", getManaCost(stack))
                .withStyle(ChatFormatting.AQUA));

        float effectiveDamage = element.getBaseDamage() * power;
        tooltip.add(Component.translatable("tooltip.transcend.spell_scroll.base_damage",
                        String.format("%.1f", effectiveDamage))
                .withStyle(elementColor));

        int effectiveCd = (int) (carrier.getBaseCooldown() * cooldown);
        tooltip.add(Component.translatable("tooltip.transcend.spell_scroll.cooldown_ticks", effectiveCd)
                .withStyle(ChatFormatting.AQUA));

        if (carrier.getAoeRadius() > 0) {
            tooltip.add(Component.translatable("tooltip.transcend.spell_scroll.aoe",
                            String.format("%.1f", carrier.getAoeRadius()))
                    .withStyle(ChatFormatting.GOLD));
        }
        if (carrier.getProjectileSpeed() > 0) {
            tooltip.add(Component.translatable("tooltip.transcend.spell_scroll.proj_speed",
                            carrier.getProjectileSpeed())
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static ChatFormatting getElementColor(SpellElement element) {
        return switch (element) {
            case METAL -> ChatFormatting.GOLD;
            case WOOD -> ChatFormatting.GREEN;
            case WATER -> ChatFormatting.AQUA;
            case FIRE -> ChatFormatting.RED;
            case EARTH -> ChatFormatting.GOLD;
            case CHAOS -> ChatFormatting.LIGHT_PURPLE;
        };
    }
}
