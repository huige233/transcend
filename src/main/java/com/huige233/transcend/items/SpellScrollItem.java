package com.huige233.transcend.items;

import com.huige233.transcend.client.magic.MagicCrystalHelper;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.spell.SpellCarrier;
import com.huige233.transcend.spell.SpellEffect;
import com.huige233.transcend.spell.SpellElement;
import com.huige233.transcend.spell.SpellProjectile;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

    // ── Factory ──

    /**
     * Creates a spell scroll ItemStack with the three-layer NBT structure.
     *
     * @param carrier  the spell carrier (delivery method)
     * @param element  the spell element (damage type)
     * @param effect   the spell effect modifier, or null for none
     * @param power    base power multiplier (default 1.0)
     * @param cooldown base cooldown multiplier (default 1.0)
     * @return a fully configured spell scroll ItemStack
     */
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

    // ── NBT readers ──

    public static SpellCarrier getCarrier(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("carrier")) {
            return SpellCarrier.ORB;
        }
        return SpellCarrier.getById(tag.getString("carrier"));
    }

    public static SpellElement getElement(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("element")) {
            return SpellElement.FIRE;
        }
        return SpellElement.getById(tag.getString("element"));
    }

    @Nullable
    public static SpellEffect getEffect(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("effect")) {
            return null;
        }
        return SpellEffect.getById(tag.getString("effect"));
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

    /**
     * Computes the total mana cost: element base cost + effect extra cost (if present).
     * Round 10: 改用 {@code element.getManaCost()} 读取数据驱动覆盖。
     */
    public static int getManaCost(ItemStack stack) {
        SpellElement element = getElement(stack);
        SpellEffect effect = getEffect(stack);
        int cost = element.getManaCost(); // 数据驱动可覆盖
        if (effect != null) {
            cost += effect.getExtraManaCost(); // Round 11: 同上
        }
        return cost;
    }

    // ── Visual overrides ──

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

    // ── 独立施法（右键消耗玩家全魔力池：innate + storage + crystals） ──

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 客户端直接通过
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.pass(stack);
        }

        // 验证 scroll 内容
        SpellCarrier carrier = getCarrier(stack);
        SpellElement element = getElement(stack);
        SpellEffect effect = getEffect(stack);

        // 必须含有有效的 carrier
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("carrier") || tag.getString("carrier").isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.wand.scroll_blank")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        // 计算消耗 — scroll 独立施法成本翻倍（少了法杖的减损 & XP 加成）
        int manaCost = getManaCost(stack) * 2;
        if (!MagicCrystalHelper.hasEnoughMana(player, manaCost)) {
            player.displayClientMessage(
                    Component.translatable("scroll.transcend.not_enough_crystals", manaCost)
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        // 抽取魔力（统一管线: innate → storage → crystals）
        MagicCrystalHelper.consumeMana(player, manaCost);

        // 简化施法路径 — 投射类直接生成 SpellProjectile，其它类型给基础视觉反馈
        boolean isProjectile = (carrier == SpellCarrier.ORB || carrier == SpellCarrier.ARROW
                || carrier == SpellCarrier.VORTEX || carrier == SpellCarrier.TRAP);
        float basePower = getBasePower(stack);

        if (isProjectile) {
            float speed = Math.max(0.5F, carrier.projectileSpeed * 0.15F);
            int shotCount = (effect == SpellEffect.MULTISHOT) ? 3 : 1;
            for (int shot = 0; shot < shotCount; shot++) {
                SpellProjectile proj = new SpellProjectile(serverLevel, player, carrier, element,
                        effect == SpellEffect.MULTISHOT ? null : effect, basePower);
                float yawOffset = (shotCount > 1) ? (shot - 1) * 7.5F : 0;
                proj.shootFromRotation(player, player.getXRot(),
                        player.getYRot() + yawOffset, 0.0F, speed, 1.0F);
                serverLevel.addFreshEntity(proj);
            }
        } else {
            // 非投射载体走简化路径 — 仅播放音效 + 反馈提示玩家载入法杖效果更好
            player.displayClientMessage(
                    Component.translatable("msg.transcend.scroll.use_wand_for_better")
                            .withStyle(ChatFormatting.GRAY), true);
        }

        // 音效 + 冷却（防止刷屏）
        serverLevel.playSound(null, player.blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6F, 1.2F);
        player.getCooldowns().addCooldown(this, 30);

        // 消耗一张卷轴（非创造模式）
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.consume(stack);
    }

    // ── Tooltip ──

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

        // Carrier name
        tooltip.add(Component.translatable("tooltip.transcend.spell_scroll.carrier")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" "))
                .append(Component.translatable(carrier.getDisplayKey()).withStyle(ChatFormatting.GOLD)));

        // Element name (colored by element)
        ChatFormatting elementColor = getElementColor(element);
        tooltip.add(Component.translatable("tooltip.transcend.spell_scroll.element")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" "))
                .append(Component.translatable(element.getDisplayKey()).withStyle(elementColor)));

        // Effect name (if present)
        if (effect != null) {
            tooltip.add(Component.translatable("tooltip.transcend.spell_scroll.effect")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(" "))
                    .append(Component.translatable(effect.getDisplayKey()).withStyle(ChatFormatting.LIGHT_PURPLE)));
        }

        tooltip.add(Component.empty());

        // Power multiplier
        if (power != 1.0F) {
            tooltip.add(Component.translatable("tooltip.transcend.spell_scroll.power",
                            String.format("%.1fx", power))
                    .withStyle(ChatFormatting.GREEN));
        }

        // Cooldown multiplier
        if (cooldown != 1.0F) {
            tooltip.add(Component.translatable("tooltip.transcend.spell_scroll.cooldown",
                            String.format("%.1fx", cooldown))
                    .withStyle(ChatFormatting.AQUA));
        }

        // Mana cost
        tooltip.add(Component.translatable("tooltip.transcend.spell_scroll.mana_cost", getManaCost(stack))
                .withStyle(ChatFormatting.AQUA));

        // Round 18: 现代化 stat 块 — 直接读 getter，反映 JSON 覆盖后的实际数值
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
            case FIRE -> ChatFormatting.RED;
            case ICE -> ChatFormatting.AQUA;
            case THUNDER -> ChatFormatting.YELLOW;
            case WIND -> ChatFormatting.GREEN;
            case EARTH -> ChatFormatting.GOLD;
            case VOID -> ChatFormatting.DARK_PURPLE;
            case HOLY -> ChatFormatting.WHITE;
            case BLOOD -> ChatFormatting.DARK_RED;
            case DARK -> ChatFormatting.DARK_GRAY;
            case LIGHT -> ChatFormatting.YELLOW;
            case POISON -> ChatFormatting.DARK_GREEN;
            case TIME -> ChatFormatting.GOLD;
            case SPACE -> ChatFormatting.LIGHT_PURPLE;
            case NATURE -> ChatFormatting.GREEN;
            case CHAOS -> ChatFormatting.LIGHT_PURPLE;
            case ACID -> ChatFormatting.DARK_GREEN;
            case SONIC -> ChatFormatting.WHITE;
            case ELDRITCH -> ChatFormatting.DARK_PURPLE;
        };
    }
}
