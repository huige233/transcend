package com.huige233.transcend.items;

import com.huige233.transcend.spell.MagicCrystalHelper;
import com.huige233.transcend.spell.SpellCarrier;
import com.huige233.transcend.spell.SpellEffect;
import com.huige233.transcend.spell.SpellElement;
import com.huige233.transcend.spell.SpellProjectile;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

/** 法术书物品（存储法术）。 */
public class SpellBookItem extends Item {

    public enum BookTier {
        APPRENTICE(3, Rarity.UNCOMMON),
        ADEPT(5, Rarity.RARE),
        MASTER(7, Rarity.RARE),
        ARCHON(9, Rarity.EPIC),
        TRANSCENDENT(12, Rarity.EPIC);

        public final int slots;
        public final Rarity rarity;

        BookTier(int slots, Rarity rarity) {
            this.slots = slots;
            this.rarity = rarity;
        }
    }

    private final BookTier tier;

    public SpellBookItem(BookTier tier) {
        super(new Properties().stacksTo(1).rarity(tier.rarity));
        this.tier = tier;
    }

    public BookTier getTier() {
        return tier;
    }

    private static ListTag getSlots(ItemStack stack) {
        return stack.getOrCreateTag().getList("slots", Tag.TAG_COMPOUND);
    }

    private void setSlots(ItemStack stack, ListTag list) {
        stack.getOrCreateTag().put("slots", list);
    }

    public int getActiveSlot(ItemStack stack) {
        return stack.getOrCreateTag().getInt("active_slot");
    }

    private void setActiveSlot(ItemStack stack, int idx) {
        stack.getOrCreateTag().putInt("active_slot", Math.max(0, idx) % tier.slots);
    }

    public int cycleActiveSlot(ItemStack stack, int delta) {
        int used = getUsedSlots(stack);
        if (used <= 0) return -1;
        int active = getActiveSlot(stack);
        int step = (delta == 0) ? 1 : (delta > 0 ? 1 : -1);
        int next = ((active + step) % used + used) % used;
        stack.getOrCreateTag().putInt("active_slot", next);
        return next;
    }

    public int getUsedSlots(ItemStack stack) {
        return getSlots(stack).size();
    }

    @Nullable
    public CompoundTag getSlotData(ItemStack stack, int idx) {
        ListTag list = getSlots(stack);
        if (idx < 0 || idx >= list.size()) return null;
        return list.getCompound(idx);
    }

    public int inscribeScroll(ItemStack book, ItemStack scrollStack) {
        if (!(scrollStack.getItem() instanceof SpellScrollItem)) return -1;
        CompoundTag scrollTag = scrollStack.getTag();
        if (scrollTag == null || !scrollTag.contains("carrier")) return -1;

        ListTag list = getSlots(book);
        if (list.size() >= tier.slots) return -1;

        CompoundTag slot = new CompoundTag();
        slot.putString("carrier", scrollTag.getString("carrier"));
        slot.putString("element", scrollTag.getString("element"));
        slot.putString("effect", scrollTag.getString("effect"));
        slot.putFloat("base_power", scrollTag.contains("base_power") ? scrollTag.getFloat("base_power") : 1.0F);
        slot.putFloat("base_cooldown", scrollTag.contains("base_cooldown") ? scrollTag.getFloat("base_cooldown") : 1.0F);
        list.add(slot);
        setSlots(book, list);
        return list.size() - 1;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.pass(stack);
        }

        ItemStack otherHand = hand == InteractionHand.MAIN_HAND
                ? player.getOffhandItem()
                : player.getMainHandItem();
        if (otherHand.getItem() instanceof SpellScrollItem) {
            int slotIdx = inscribeScroll(stack, otherHand);
            if (slotIdx < 0) {
                player.displayClientMessage(
                        Component.translatable("spellbook.transcend.full",
                                getUsedSlots(stack), tier.slots).withStyle(ChatFormatting.YELLOW), true);
                return InteractionResultHolder.fail(stack);
            }

            if (!player.getAbilities().instabuild) {
                otherHand.shrink(1);
            }
            serverLevel.playSound(null, player.blockPosition(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.5F);
            player.displayClientMessage(
                    Component.translatable("spellbook.transcend.inscribed",
                            slotIdx, getUsedSlots(stack), tier.slots).withStyle(ChatFormatting.GREEN), true);
            return InteractionResultHolder.consume(stack);
        }

        if (otherHand.getItem() instanceof com.huige233.transcend.items.SpellGlyphItem glyphItem) {
            int active = getActiveSlot(stack);
            CompoundTag slotData = getSlotData(stack, active);
            if (slotData == null) {
                player.displayClientMessage(
                        Component.translatable("spellbook.transcend.empty_slot")
                                .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            com.huige233.transcend.spell.SpellAugment aug = glyphItem.getAugment();
            int[] currentAugs = slotData.contains("augments") ? slotData.getIntArray("augments") : new int[0];

            int existing = 0;
            for (int o : currentAugs) if (o == aug.ordinal()) existing++;
            if (existing >= aug.maxStack) {
                player.displayClientMessage(
                        Component.translatable("glyph.transcend.max_reached", aug.maxStack)
                                .withStyle(ChatFormatting.YELLOW), true);
                return InteractionResultHolder.fail(stack);
            }

            int[] updated = new int[currentAugs.length + 1];
            System.arraycopy(currentAugs, 0, updated, 0, currentAugs.length);
            updated[currentAugs.length] = aug.ordinal();
            slotData.putIntArray("augments", updated);

            net.minecraft.nbt.ListTag list = stack.getOrCreateTag().getList("slots", net.minecraft.nbt.Tag.TAG_COMPOUND);
            list.set(active, slotData);
            stack.getOrCreateTag().put("slots", list);

            if (!player.getAbilities().instabuild) {
                otherHand.shrink(1);
            }
            serverLevel.playSound(null, player.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.6F);
            player.displayClientMessage(
                    Component.translatable("glyph.transcend.applied", aug.id, existing + 1, aug.maxStack)
                            .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return InteractionResultHolder.consume(stack);
        }

        if (player.isShiftKeyDown()) {
            int used = getUsedSlots(stack);
            if (used == 0) {
                player.displayClientMessage(
                        Component.translatable("spellbook.transcend.empty")
                                .withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            int active = getActiveSlot(stack);
            int next = (active + 1) % used;
            setActiveSlot(stack, next);
            CompoundTag slotData = getSlotData(stack, next);
            String summary = slotData != null
                    ? String.format("%s/%s%s", slotData.getString("carrier"), slotData.getString("element"),
                            slotData.getString("effect").isEmpty() ? "" : "+" + slotData.getString("effect"))
                    : "?";
            player.displayClientMessage(
                    Component.translatable("spellbook.transcend.switched",
                            next + 1, used, summary).withStyle(ChatFormatting.AQUA), true);
            serverLevel.playSound(null, player.blockPosition(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.6F, 1.0F);
            return InteractionResultHolder.success(stack);
        }

        return castActiveSlot(serverLevel, player, stack);
    }

    private InteractionResultHolder<ItemStack> castActiveSlot(ServerLevel serverLevel,
                                                               Player player, ItemStack stack) {
        player.displayClientMessage(Component.translatable("msg.transcend.spellbook.migration")
                .withStyle(ChatFormatting.YELLOW), true);
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getUsedSlots(stack) >= tier.slots;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        int used = getUsedSlots(stack);
        tooltip.add(Component.translatable("spellbook.transcend.tier."
                + tier.name().toLowerCase()).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("spellbook.transcend.slots",
                used, tier.slots).withStyle(ChatFormatting.AQUA));

        if (used > 0) {
            int active = getActiveSlot(stack);
            CompoundTag slotData = getSlotData(stack, active);
            if (slotData != null) {
                String summary = String.format("§b%d§7/§b%d§7  §a%s§7/§e%s§7%s",
                        active + 1, used,
                        slotData.getString("carrier"), slotData.getString("element"),
                        slotData.getString("effect").isEmpty() ? ""
                                : "§7+§6" + slotData.getString("effect"));
                tooltip.add(Component.literal(summary));
            }
        }
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("msg.transcend.spellbook.migration")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("spellbook.transcend.tip.inscribe")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("spellbook.transcend.tip.cycle")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("spellbook.transcend.tip.scroll")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
