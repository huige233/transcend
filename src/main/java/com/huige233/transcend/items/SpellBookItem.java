package com.huige233.transcend.items;

import com.huige233.transcend.client.magic.MagicCrystalHelper;
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

/**
 * Round 27: Spellbook Loadout — IS&S 风格法术配装载体。
 *
 * <p>5 阶法术书，存 N 个法术配置（不是 ItemStack，是配置 NBT 数据）。
 * <pre>
 *   APPRENTICE  - 3 slots,  Rarity.UNCOMMON
 *   ADEPT       - 5 slots,  Rarity.RARE
 *   MASTER      - 7 slots,  Rarity.RARE
 *   ARCHON      - 9 slots,  Rarity.EPIC
 *   TRANSCENDENT- 12 slots, Rarity.EPIC
 * </pre>
 *
 * <p>交互：
 * <ul>
 *   <li>右键（无副手 scroll） → 施放当前 active slot 的法术</li>
 *   <li>右键（副手持 spell_scroll） → 将 scroll 数据写入下一空 slot，消耗 scroll</li>
 *   <li>潜行 + 右键 → 切换 active slot（循环）</li>
 * </ul>
 *
 * <p>NBT 结构：
 * <pre>
 *   "slots"         ListTag of CompoundTag { carrier, element, effect, power, cooldown }
 *   "active_slot"   int 0..tier.slots-1
 * </pre>
 *
 * <p>所有 slot 法术消耗 mana 但**不消耗 slot**（可重复施放）。Mana 成本比 SpellScrollItem 低 50%（书的优势）。
 */
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

    // ─── NBT helpers ───

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

    /**
     * R78: 滚轮切换槽位（公开方法，供 C2SSpellBookSlotChange 调用）。
     * 在 USED slots 范围内循环（不是 tier.slots — 避免空 slot 跳过）。
     *
     * @param delta -1 = 向前一个；+1 = 向后一个；其它值会按符号取 ±1
     * @return 切换后的新 slot 索引；book 为空时返回 -1（无变化）
     */
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

    /** 从 scroll NBT 拷贝 5 字段到 book slot。返回写入的 slot 索引；满 → -1。 */
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

    // ─── Use logic ───

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.pass(stack);
        }

        // Path A: 副手持 spell_scroll → 刻入
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
            // 消耗 scroll
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

        // Round 43 Path A2: 副手持 spell glyph → 注入当前激活 slot 的 augment list
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
            // 计算同 augment 已有层数
            int existing = 0;
            for (int o : currentAugs) if (o == aug.ordinal()) existing++;
            if (existing >= aug.maxStack) {
                player.displayClientMessage(
                        Component.translatable("glyph.transcend.max_reached", aug.maxStack)
                                .withStyle(ChatFormatting.YELLOW), true);
                return InteractionResultHolder.fail(stack);
            }
            // 写入新层
            int[] updated = new int[currentAugs.length + 1];
            System.arraycopy(currentAugs, 0, updated, 0, currentAugs.length);
            updated[currentAugs.length] = aug.ordinal();
            slotData.putIntArray("augments", updated);
            // 持久化
            net.minecraft.nbt.ListTag list = stack.getOrCreateTag().getList("slots", net.minecraft.nbt.Tag.TAG_COMPOUND);
            list.set(active, slotData);
            stack.getOrCreateTag().put("slots", list);
            // 消耗 1 个 glyph
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

        // Path B: 潜行 + 右键 → 循环 active slot
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

        // Path C: 普通右键 → 施放 active slot 法术
        return castActiveSlot(serverLevel, player, stack);
    }

    private InteractionResultHolder<ItemStack> castActiveSlot(ServerLevel serverLevel,
                                                               Player player, ItemStack stack) {
        int active = getActiveSlot(stack);
        CompoundTag slotData = getSlotData(stack, active);
        if (slotData == null) {
            player.displayClientMessage(
                    Component.translatable("spellbook.transcend.empty_slot")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        SpellCarrier carrier = SpellCarrier.getById(slotData.getString("carrier"));
        SpellElement element = SpellElement.getById(slotData.getString("element"));
        SpellEffect effect = slotData.getString("effect").isEmpty() ? null
                : SpellEffect.getById(slotData.getString("effect"));
        float power = slotData.contains("base_power") ? slotData.getFloat("base_power") : 1.0F;

        // Round 43: 解析 augments → 计算修正倍率
        int[] augOrdinals = slotData.contains("augments") ? slotData.getIntArray("augments") : new int[0];
        int amplifyStacks = 0, dampenStacks = 0, quickfireStacks = 0, splitStacks = 0;
        int pierceStacks = 0, chainStacks = 0, extendStacks = 0, homingStacks = 0;
        for (int o : augOrdinals) {
            com.huige233.transcend.spell.SpellAugment a = com.huige233.transcend.spell.SpellAugment.byOrdinal(o);
            switch (a) {
                case AMPLIFY -> amplifyStacks++;
                case DAMPEN -> dampenStacks++;
                case QUICKFIRE -> quickfireStacks++;
                case SPLIT -> splitStacks++;
                case PIERCE -> pierceStacks++;
                case CHAIN -> chainStacks++;
                case EXTEND -> extendStacks++;
                case HOMING -> homingStacks++;
            }
        }
        // Amplify: 伤害 +25% / 层
        float augPower = power * (1.0F + 0.25F * amplifyStacks);
        // Mana cost 修正：amplify +20%/层 × dampen 0.7 × quickfire 1.3
        float manaCostMult = (1.0F + 0.20F * amplifyStacks);
        if (dampenStacks > 0) manaCostMult *= 0.7F;
        if (quickfireStacks > 0) manaCostMult *= 1.3F;
        // Cooldown 修正：amplify +5%/层 × dampen 1.4 × quickfire 0.5
        float cdMult = (1.0F + 0.05F * amplifyStacks);
        if (dampenStacks > 0) cdMult *= 1.4F;
        if (quickfireStacks > 0) cdMult *= 0.5F;

        // Mana cost: element + effect cost × power × multiplier
        int baseCost = element.getManaCost();
        if (effect != null) baseCost += effect.getExtraManaCost();
        int manaCost = (int) Math.max(1, baseCost * power * manaCostMult);
        if (!MagicCrystalHelper.hasEnoughMana(player, manaCost)) {
            player.displayClientMessage(
                    Component.translatable("scroll.transcend.not_enough_crystals", manaCost)
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        MagicCrystalHelper.consumeMana(player, manaCost);

        // 投射类直接 spawn SpellProjectile
        boolean isProjectile = (carrier == SpellCarrier.ORB || carrier == SpellCarrier.ARROW
                || carrier == SpellCarrier.VORTEX || carrier == SpellCarrier.TRAP);
        if (isProjectile) {
            float speed = Math.max(0.5F, carrier.projectileSpeed * 0.15F);
            // shotCount: MULTISHOT effect → 3, OR split augment → 3
            int shotCount = (effect == SpellEffect.MULTISHOT || splitStacks > 0) ? 3 : 1;
            for (int shot = 0; shot < shotCount; shot++) {
                SpellProjectile proj = new SpellProjectile(serverLevel, player, carrier, element,
                        effect == SpellEffect.MULTISHOT ? null : effect, augPower);
                // Round 46: 4 个 augment 注入（pierce/chain/extend/homing）
                proj.setAugments(pierceStacks, chainStacks, extendStacks, homingStacks);
                float yawOffset = (shotCount > 1) ? (shot - 1) * 7.5F : 0;
                proj.shootFromRotation(player, player.getXRot(),
                        player.getYRot() + yawOffset, 0.0F, speed, 1.0F);
                serverLevel.addFreshEntity(proj);
            }
        }

        serverLevel.playSound(null, player.blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7F, 1.4F);
        // 冷却：tier 阶梯 × augment 修正
        int cd = Math.max(5, (int)((35 - tier.slots * 2) * cdMult));
        player.getCooldowns().addCooldown(this, cd);

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getUsedSlots(stack) >= tier.slots;
    }

    // ─── Tooltip ───

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
        tooltip.add(Component.translatable("spellbook.transcend.tip.cast")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("spellbook.transcend.tip.inscribe")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("spellbook.transcend.tip.cycle")
                .withStyle(ChatFormatting.GRAY));
        // R78: wheel-scroll switching
        tooltip.add(Component.translatable("spellbook.transcend.tip.scroll")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
