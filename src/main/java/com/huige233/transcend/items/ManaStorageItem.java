package com.huige233.transcend.items;

import com.huige233.transcend.ModRarities;
import com.huige233.transcend.client.magic.MagicCrystalHelper;
import com.huige233.transcend.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ManaStorageItem extends Item {

    private static final String KEY_MANA = "mana_stored";
    private static final String KEY_MAX_MANA = "mana_max";
    /** 输送模式: 0=关闭, 1=缓慢, 2=快速。Shift+右键循环。 */
    private static final String KEY_MODE = "transfer_mode";
    public static final int MAX_MANA = 256;

    public static final int MODE_OFF = 0;
    public static final int MODE_SLOW = 1;
    public static final int MODE_FAST = 2;

    /** 缓慢模式: 每 20 tick (1 秒) 输送 2 点魔力到玩家内在池 */
    private static final int SLOW_INTERVAL_TICKS = 20;
    private static final int SLOW_RATE = 2;
    /** 快速模式: 每 4 tick (0.2 秒) 输送 4 点魔力 = 20/秒 */
    private static final int FAST_INTERVAL_TICKS = 4;
    private static final int FAST_RATE = 4;

    private final int maxMana;

    public ManaStorageItem() {
        this(256);
    }

    public ManaStorageItem(int maxMana) {
        super(new Properties().stacksTo(1));
        this.maxMana = maxMana;
        ModItems.ITEMS.add(this);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!stack.getOrCreateTag().contains(KEY_MAX_MANA)) {
            stack.getOrCreateTag().putInt(KEY_MAX_MANA, this.maxMana);
        }
        if (level.isClientSide) return;
        if (!(entity instanceof Player player)) return;

        // ── 自动输送到玩家内在魔力池 ────────────────────────────────────
        int mode = getMode(stack);
        if (mode == MODE_OFF) return;

        int interval = (mode == MODE_FAST) ? FAST_INTERVAL_TICKS : SLOW_INTERVAL_TICKS;
        if (level.getGameTime() % interval != 0) return;

        int rate = (mode == MODE_FAST) ? FAST_RATE : SLOW_RATE;
        int stored = getStoredMana(stack);
        if (stored <= 0) return;

        int playerMax = MagicCrystalHelper.getInnateMaxMana(player);
        int playerCurrent = MagicCrystalHelper.getInnateMana(player);
        if (playerMax <= 0 || playerCurrent >= playerMax) return;

        int space = playerMax - playerCurrent;
        int transfer = Math.min(rate, Math.min(stored, space));
        if (transfer <= 0) return;

        setStoredMana(stack, stored - transfer);
        MagicCrystalHelper.setInnateMana(player, playerCurrent + transfer);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) return InteractionResultHolder.success(player.getItemInHand(hand));

        ItemStack storage = player.getItemInHand(hand);

        // ── Shift+右键: 切换输送模式 ─────────────────────────────────
        if (player.isShiftKeyDown()) {
            int next = (getMode(storage) + 1) % 3;
            setMode(storage, next);
            String modeKey = switch (next) {
                case MODE_SLOW -> "msg.transcend.mana_storage.mode_slow";
                case MODE_FAST -> "msg.transcend.mana_storage.mode_fast";
                default -> "msg.transcend.mana_storage.mode_off";
            };
            ChatFormatting color = switch (next) {
                case MODE_SLOW -> ChatFormatting.AQUA;
                case MODE_FAST -> ChatFormatting.GOLD;
                default -> ChatFormatting.GRAY;
            };
            player.displayClientMessage(Component.translatable(modeKey).withStyle(color), true);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, next == MODE_OFF ? 0.8F : 1.4F);
            return InteractionResultHolder.success(storage);
        }

        // ── 普通右键: 副手水晶充能 (原行为) ─────────────────────────
        InteractionHand otherHand = (hand == InteractionHand.MAIN_HAND)
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherStack = player.getItemInHand(otherHand);

        if (otherStack.getItem() instanceof MagicCrystalItem crystal) {
            int currentMana = getStoredMana(storage);
            int maxMana = getMaxMana(storage);
            int space = maxMana - currentMana;
            if (space <= 0) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.mana_storage_full")
                                .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(storage);
            }

            int valuePerItem = crystal.getCrystalValue();
            int maxItems = Math.min(otherStack.getCount(), ceilDiv(space, valuePerItem));
            int manaToAdd = maxItems * valuePerItem;
            manaToAdd = Math.min(manaToAdd, space);

            int itemsUsed = ceilDiv(manaToAdd, valuePerItem);
            manaToAdd = itemsUsed * valuePerItem;
            if (currentMana + manaToAdd > maxMana) {
                manaToAdd = maxMana - currentMana;
                itemsUsed = ceilDiv(manaToAdd, valuePerItem);
                manaToAdd = itemsUsed * valuePerItem;
                if (currentMana + manaToAdd > maxMana) {
                    manaToAdd = maxMana - currentMana;
                }
            }

            if (itemsUsed <= 0) return InteractionResultHolder.fail(storage);

            setStoredMana(storage, currentMana + manaToAdd);
            if (!player.isCreative()) {
                otherStack.shrink(itemsUsed);
            }

            player.displayClientMessage(
                    Component.translatable("msg.transcend.mana_storage_charged",
                            manaToAdd, getStoredMana(storage), maxMana)
                            .withStyle(ChatFormatting.AQUA), true);

            level.playSound(null, player.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);

            player.getCooldowns().addCooldown(this, 5);
            return InteractionResultHolder.success(storage);
        }

        player.displayClientMessage(
                Component.translatable("msg.transcend.mana_storage_need_crystal")
                        .withStyle(ChatFormatting.GRAY), true);
        return InteractionResultHolder.fail(storage);
    }

    public static int getMaxMana(ItemStack stack) {
        if (stack.getTag() != null && stack.getTag().contains(KEY_MAX_MANA)) {
            return stack.getTag().getInt(KEY_MAX_MANA);
        }
        return MAX_MANA;
    }

    public static int getStoredMana(ItemStack stack) {
        if (stack.getTag() == null) return 0;
        return stack.getTag().getInt(KEY_MANA);
    }

    public static void setStoredMana(ItemStack stack, int mana) {
        stack.getOrCreateTag().putInt(KEY_MANA, Math.max(0, Math.min(mana, getMaxMana(stack))));
    }

    public static boolean consumeMana(ItemStack stack, int amount) {
        int current = getStoredMana(stack);
        if (current < amount) return false;
        setStoredMana(stack, current - amount);
        return true;
    }

    public static int getMode(ItemStack stack) {
        if (stack.getTag() == null) return MODE_OFF;
        return stack.getTag().getInt(KEY_MODE);
    }

    public static void setMode(ItemStack stack, int mode) {
        stack.getOrCreateTag().putInt(KEY_MODE, Math.max(0, Math.min(2, mode)));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getStoredMana(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int maxMana = getMaxMana(stack);
        return maxMana == 0 ? 0 : Math.round(13.0F * getStoredMana(stack) / maxMana);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int mode = getMode(stack);
        // 模式着色: OFF=灰, SLOW=青, FAST=金
        if (mode == MODE_FAST) return 0xFFAA22;  // gold/amber
        if (mode == MODE_SLOW) return 0x44CCEE;  // aqua
        int maxMana = getMaxMana(stack);
        if (maxMana == 0) return 0;
        float ratio = (float) getStoredMana(stack) / maxMana;
        int r = (int) (80 * (1 - ratio) + 100 * ratio);
        int g = (int) (180 * ratio);
        int b = (int) (200 + 55 * ratio);
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getStoredMana(stack) >= getMaxMana(stack) || getMode(stack) == MODE_FAST;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        int maxMana = getMaxMana(stack);
        if (maxMana >= 4096) {
            return ModRarities.LEGEND;
        }
        if (maxMana >= 1024) {
            return ModRarities.COSMIC;
        }
        return Rarity.RARE;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.mana_storage.desc")
                .withStyle(ChatFormatting.LIGHT_PURPLE));

        int mana = getStoredMana(stack);
        int maxMana = getMaxMana(stack);
        ChatFormatting manaColor = mana >= maxMana ? ChatFormatting.GREEN :
                mana > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY;
        tooltip.add(Component.translatable("tooltip.transcend.mana_storage.mana", mana, maxMana)
                .withStyle(manaColor));

        // 输送模式行
        int mode = getMode(stack);
        String modeText;
        ChatFormatting modeColor;
        switch (mode) {
            case MODE_FAST -> { modeText = "tooltip.transcend.mana_storage.mode_fast"; modeColor = ChatFormatting.GOLD; }
            case MODE_SLOW -> { modeText = "tooltip.transcend.mana_storage.mode_slow"; modeColor = ChatFormatting.AQUA; }
            default        -> { modeText = "tooltip.transcend.mana_storage.mode_off"; modeColor = ChatFormatting.GRAY; }
        }
        tooltip.add(Component.translatable(modeText).withStyle(modeColor));

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.transcend.mana_storage.usage")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.mana_storage.shift_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }
}

