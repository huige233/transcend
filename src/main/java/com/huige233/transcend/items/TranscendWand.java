package com.huige233.transcend.items;

import com.huige233.transcend.ModRarities;
import com.huige233.transcend.ascension.resource.ClassResourceHandler;
import com.huige233.transcend.spell.MagicCrystalHelper;
import com.huige233.transcend.visual.ServerVisualBroadcaster;
import com.huige233.transcend.visual.ServerVisualGeometry;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.network.S2CParticleBatchPack;
import com.huige233.transcend.spell.SpellCarrier;
import com.huige233.transcend.spell.SpellEffect;
import com.huige233.transcend.spell.SpellElement;
import com.huige233.transcend.spell.SpellAspect;
import com.huige233.transcend.spell.SpellProjectile;
import com.huige233.transcend.spell.SpellDamageMath;
import com.huige233.transcend.spell.SpellDamageService;
import com.huige233.transcend.spell.ElementReaction;
import com.huige233.transcend.spell.WandRune;
import com.huige233.transcend.spell.config.SpellConfigurationRules;
import com.huige233.transcend.util.EntityCompatUtil;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import com.huige233.transcend.world.nexus.NexusWorldPenalty;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TranscendWand extends Item {

    private static final UUID ARMOR_BREAK_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");

    private static final String TAG_WAND_SLOTS = "wand_slots";
    private static final String TAG_SELECTED_SLOT = "selected_slot";
    private static final String TAG_MAX_SLOTS = "max_slots";

    private static final String TAG_SEQ_MODE = "seq_mode";
    private static final String TAG_RECHARGE_UNTIL = "recharge_until";

    private final int maxSlots;
    private final int castInterval;
    private final int castCount;

    public TranscendWand(int maxSlots, int castInterval, int castCount) {
        super(new Properties().stacksTo(1).fireResistant());
        this.maxSlots = maxSlots;
        this.castInterval = castInterval;
        this.castCount = castCount;
        ModItems.ITEMS.add(this);
    }

    private void ensureWandNBT(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_WAND_SLOTS)) {
            ListTag slots = new ListTag();
            for (int i = 0; i < maxSlots; i++) {
                slots.add(new CompoundTag());
            }
            tag.put(TAG_WAND_SLOTS, slots);
            tag.putInt(TAG_SELECTED_SLOT, 0);
            tag.putInt(TAG_MAX_SLOTS, maxSlots);
        }
    }

    private static int getMaxSlots(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return (tag != null && tag.contains(TAG_MAX_SLOTS)) ? tag.getInt(TAG_MAX_SLOTS) : 3;
    }

    public int getCastCount() { return castCount; }

    private static int getSelectedSlot(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return (tag != null && tag.contains(TAG_SELECTED_SLOT)) ? tag.getInt(TAG_SELECTED_SLOT) : 0;
    }

    private static void setSelectedSlot(ItemStack stack, int slot) {
        stack.getOrCreateTag().putInt(TAG_SELECTED_SLOT, slot);
    }

    private static ListTag getSlotsList(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_WAND_SLOTS, Tag.TAG_LIST)) {
            return new ListTag();
        }
        return tag.getList(TAG_WAND_SLOTS, Tag.TAG_COMPOUND);
    }

    private static CompoundTag getSlotData(ItemStack stack, int slot) {
        ListTag slots = getSlotsList(stack);
        if (slot < 0 || slot >= slots.size()) return new CompoundTag();
        return slots.getCompound(slot);
    }

    private static boolean isSlotOccupied(ItemStack stack, int slot) {
        CompoundTag data = getSlotData(stack, slot);
        return data.contains("carrier");
    }

    private static void setSlotData(ItemStack stack, int slot, CompoundTag scrollData) {
        ListTag slots = getSlotsList(stack);
        if (slot >= 0 && slot < slots.size()) {
            slots.set(slot, scrollData);
            stack.getOrCreateTag().put(TAG_WAND_SLOTS, slots);
        }
    }

    private static String slotCdKey(int slot) {
        return "slot_cd_" + slot;
    }

    private static int getSlotCooldown(ItemStack stack, int slot) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 0;
        return tag.getInt(slotCdKey(slot));
    }

    private static void setSlotCooldown(ItemStack stack, int slot, int ticks) {
        stack.getOrCreateTag().putInt(slotCdKey(slot), Math.max(0, ticks));
    }

    private static boolean isSeqMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();

        return tag == null || !tag.contains(TAG_SEQ_MODE) || tag.getBoolean(TAG_SEQ_MODE);
    }

    private static int countOccupied(ItemStack stack) {
        ListTag slots = getSlotsList(stack);
        int n = 0;
        for (int i = 0; i < slots.size(); i++) {
            if (slots.getCompound(i).contains("carrier")) n++;
        }
        return n;
    }

    private static int nextOccupiedSlot(ItemStack stack, int from) {
        int maxSl = getMaxSlots(stack);
        if (maxSl <= 0) return -1;
        for (int i = 1; i <= maxSl; i++) {
            int cand = (from + i) % maxSl;
            if (isSlotOccupied(stack, cand)) return cand;
        }
        return isSlotOccupied(stack, from) ? from : -1;
    }

    private static int firstOccupiedSlot(ItemStack stack) {
        int maxSl = getMaxSlots(stack);
        for (int i = 0; i < maxSl; i++) {
            if (isSlotOccupied(stack, i)) return i;
        }
        return -1;
    }

    private static boolean isRecharging(ItemStack stack, long gameTime) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getLong(TAG_RECHARGE_UNTIL) > gameTime;
    }

    private static void setRecharge(ItemStack stack, long until) {
        stack.getOrCreateTag().putLong(TAG_RECHARGE_UNTIL, until);
    }

    private int rechargeTicks(ItemStack stack) {
        int occ = Math.max(1, countOccupied(stack));
        return Math.max(6, castInterval * 2 + occ * 2);
    }

    private static final int[] XP_THRESHOLDS = {0, 10, 25, 50, 80, 120, 170, 230, 300, 400};
    private static final float[] LEVEL_DAMAGE_MULT = {1.0F, 1.08F, 1.18F, 1.30F, 1.45F, 1.62F, 1.82F, 2.05F, 2.30F, 2.60F};

    private static final float[] LEVEL_CD_MULT = {1.0F, 0.99F, 0.97F, 0.95F, 0.93F, 0.91F, 0.89F, 0.87F, 0.84F, 0.82F};

    public static int getSpellLevel(ItemStack stack, int slot) {
        CompoundTag data = getSlotData(stack, slot);
        int lvl = data.getInt("spell_level");
        return lvl < 1 ? 1 : Math.min(lvl, 10);
    }

    public static int getSpellXp(ItemStack stack, int slot) {
        return getSlotData(stack, slot).getInt("spell_xp");
    }

    public static void addSpellXp(ItemStack stack, int slot, int amount) {
        CompoundTag data = getSlotData(stack, slot);
        int xp = data.getInt("spell_xp") + amount;
        int level = data.getInt("spell_level");
        if (level < 1) level = 1;

        while (level < 10 && xp >= XP_THRESHOLDS[level]) {
            xp -= XP_THRESHOLDS[level];
            level++;
        }
        if (level >= 10) level = 10;

        data.putInt("spell_xp", xp);
        data.putInt("spell_level", level);

        ListTag slots = getSlotsList(stack);
        if (slot >= 0 && slot < slots.size()) {
            slots.set(slot, data);
            stack.getOrCreateTag().put(TAG_WAND_SLOTS, slots);
        }
    }

    public static float getLevelDamageMult(int level) {
        int idx = Math.max(0, Math.min(level - 1, 9));
        return LEVEL_DAMAGE_MULT[idx];
    }

    public static float getLevelCdMult(int level) {
        int idx = Math.max(0, Math.min(level - 1, 9));
        return LEVEL_CD_MULT[idx];
    }

    private static int countElement(ItemStack stack, String elementId) {
        ListTag slots = getSlotsList(stack);
        int count = 0;
        for (int i = 0; i < slots.size(); i++) {
            CompoundTag s = slots.getCompound(i);
            if (s.contains("element") && s.getString("element").equals(elementId)) {
                count++;
            }
        }
        return count;
    }

    public static float getResonanceBonus(ItemStack stack, int slot) {
        CompoundTag data = getSlotData(stack, slot);
        if (!data.contains("element")) return 1.0F;
        int count = countElement(stack, data.getString("element"));
        if (count >= 4) return 1.60F;
        if (count >= 3) return 1.35F;
        if (count >= 2) return 1.15F;
        return 1.0F;
    }

    public static String getResonanceElement(ItemStack stack) {
        ListTag slots = getSlotsList(stack);
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (int i = 0; i < slots.size(); i++) {
            CompoundTag s = slots.getCompound(i);
            if (s.contains("element")) {
                String el = s.getString("element");
                counts.merge(el, 1, Integer::sum);
            }
        }
        for (var entry : counts.entrySet()) {
            if (entry.getValue() >= 3) return entry.getKey();
        }
        return null;
    }

    public static int getManaCostFromTag(CompoundTag slotTag) {
        if (slotTag == null || !slotTag.contains("element")) return 0;
        SpellCarrier carrier = SpellCarrier.getById(slotTag.getString("carrier"));
        SpellElement element = SpellElement.getById(slotTag.getString("element"));
        List<SpellEffect> effects = getConfiguredEffects(slotTag);
        return com.huige233.transcend.spell.config.SpellConfigurationRules
                .compute(carrier, element, effects).manaCost();
    }

    @Nullable
    private static List<SpellEffect> getConfiguredEffects(CompoundTag slotTag) {
        List<SpellEffect> effects = new ArrayList<>();
        if (slotTag.contains("effects", Tag.TAG_LIST)) {
            ListTag list = slotTag.getList("effects", Tag.TAG_STRING);
            for (int i = 0; i < Math.min(7, list.size()); i++) {
                SpellEffect effect = SpellEffect.getById(list.getString(i));
                if (effect == null) return null;
                effects.add(effect);
            }
        } else {
            String legacyId = slotTag.getString("effect");
            SpellEffect legacy = SpellEffect.getById(legacyId);
            if (!legacyId.isEmpty() && legacy == null) return null;
            if (legacy != null) effects.add(legacy);
        }
        return effects;
    }

    private static boolean canHitTarget(Entity entity, Entity owner) {
        return entity != null
                && entity != owner
                && entity.isAlive()
                && !EntityCompatUtil.isProtectedPlayer(entity);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public @NotNull Rarity getRarity(@NotNull ItemStack stack) {
        return ModRarities.COSMIC;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return 72000;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        ListTag slots = getSlotsList(stack);
        for (int i = 0; i < slots.size(); i++) {
            CompoundTag s = slots.getCompound(i);
            if (s.contains("carrier")) return true;
        }
        return false;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level,
                                                           @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ensureWandNBT(stack);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                ItemStack offhand = player.getOffhandItem();
                int selected = getSelectedSlot(stack);
                int maxSl = getMaxSlots(stack);

                if (offhand.getItem() instanceof SpellScrollItem) {

                    CompoundTag offhandTag = offhand.getTag();
                    if (offhandTag == null || !offhandTag.contains("carrier")
                            || offhandTag.getString("carrier").isEmpty()) {
                        player.displayClientMessage(
                                Component.translatable("msg.transcend.wand.scroll_blank")
                                        .withStyle(ChatFormatting.RED), true);
                        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
                    }
                    int targetSlot = -1;
                    if (!isSlotOccupied(stack, selected)) {
                        targetSlot = selected;
                    } else {
                        for (int i = 0; i < maxSl; i++) {
                            if (!isSlotOccupied(stack, i)) {
                                targetSlot = i;
                                break;
                            }
                        }
                    }
                    if (targetSlot == -1) {
                        player.displayClientMessage(
                                Component.translatable("msg.transcend.wand.slots_full")
                                        .withStyle(ChatFormatting.RED), true);
                    } else {
                        CompoundTag scrollTag = offhand.getOrCreateTag().copy();
                        setSlotData(stack, targetSlot, scrollTag);
                        setSelectedSlot(stack, targetSlot);
                        offhand.shrink(1);
                        level.playSound(null, player.blockPosition(),
                                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                        player.displayClientMessage(
                                Component.translatable("msg.transcend.wand.loaded_slot", targetSlot + 1)
                                        .withStyle(ChatFormatting.GREEN), true);
                    }
                } else {
                    int next = selected;
                    for (int i = 1; i <= maxSl; i++) {
                        int candidate = (selected + i) % maxSl;
                        if (isSlotOccupied(stack, candidate)) {
                            next = candidate;
                            break;
                        }
                    }
                    if (next != selected) {
                        setSelectedSlot(stack, next);
                    }
                    player.displayClientMessage(
                            Component.translatable("msg.transcend.wand.selected_slot", getSelectedSlot(stack) + 1)
                                    .withStyle(ChatFormatting.GOLD), true);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        } else {

            int selected = getSelectedSlot(stack);
            boolean hasSpell = isSlotOccupied(stack, selected);
            if (!hasSpell) {
                if (!level.isClientSide) {
                    player.displayClientMessage(
                            Component.translatable("msg.transcend.wand.no_spell")
                                    .withStyle(ChatFormatting.RED), true);
                }
                return InteractionResultHolder.fail(stack);
            }

            if (!level.isClientSide) {
                castSpell(level, player, stack);
            }

            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity,
                          @NotNull ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide) return;
        if (!(entity instanceof Player player)) return;

        int selected = getSelectedSlot(stack);
        if (!isSlotOccupied(stack, selected)) return;

        if (isSeqMode(stack) && countOccupied(stack) >= 2) {
            long now = level.getGameTime();
            if (isRecharging(stack, now)) return;
            if (getSlotCooldown(stack, selected) > 0) return;

            castSpell(level, player, stack);

            int next = nextOccupiedSlot(stack, selected);
            int first = firstOccupiedSlot(stack);
            if (next == -1) return;
            setSelectedSlot(stack, next);
            if (next == first && next != selected) {
                setRecharge(stack, now + rechargeTicks(stack));
            }
            return;
        }

        if (getSlotCooldown(stack, selected) <= 0) {
            castSpell(level, player, stack);
        }
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level,
                             @NotNull LivingEntity entity, int timeCharged) {

    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level,
                              @NotNull Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) return;
        ensureWandNBT(stack);

        CompoundTag tag = stack.getOrCreateTag();

        int maxSl = getMaxSlots(stack);
        for (int i = 0; i < maxSl; i++) {
            String key = slotCdKey(i);
            int cd = tag.getInt(key);
            if (cd > 0) {
                tag.putInt(key, cd - 1);
            }
        }
    }

    private void castSpell(Level level, Player player, ItemStack wandStack) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        castConfiguredSpell(serverLevel, player, wandStack);
    }

    private void castConfiguredSpell(ServerLevel serverLevel, Player player, ItemStack wandStack) {
        int selected = getSelectedSlot(wandStack);
        CompoundTag slotData = getSlotData(wandStack, selected);
        if (!slotData.contains("carrier")) return;

        SpellCarrier carrier = SpellCarrier.getById(slotData.getString("carrier"));
        SpellElement element = SpellElement.getById(slotData.getString("element"));
        List<SpellEffect> parsedEffects = getConfiguredEffects(slotData);
        if (carrier == null || element == null || parsedEffects == null) {
            player.displayClientMessage(Component.translatable("msg.transcend.wand.invalid_configuration")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        List<SpellEffect> effects = com.huige233.transcend.spell.config.SpellConfigurationRules
                .canonicalEffects(parsedEffects);
        com.huige233.transcend.spell.config.SpellConfigurationRules.Computation computation =
                com.huige233.transcend.spell.config.SpellConfigurationRules.compute(carrier, element, effects);

        {
            com.huige233.transcend.ascension.PlayerAscensionData _tv_data =
                    com.huige233.transcend.ascension.AscensionCapability.get(player);
            com.huige233.transcend.ascension.AscensionVow _tv = _tv_data.getActiveTertiaryVow();
            if (_tv != null && "vow_of_solitude".equals(_tv.getId()) && !_tv_data.isVowLiberated(_tv.getId())) {
                if (isAoeCarrier(carrier)) {
                    _tv_data.markVowSolitudeUsedAoE();
                    player.displayClientMessage(
                            Component.translatable("msg.transcend.vow_solitude.aoe_blocked")
                                    .withStyle(ChatFormatting.RED), true);
                    return;
                }
            }
        }
        float storedBasePower = slotData.contains("base_power") ? slotData.getFloat("base_power") : 1.0F;
        float basePower = Float.isFinite(storedBasePower)
                ? Math.max(0.0F, Math.min(1000.0F, storedBasePower)) : 1.0F;
        float storedCooldown = slotData.contains("base_cooldown") ? slotData.getFloat("base_cooldown") : 1.0F;
        float baseCooldownMult = Float.isFinite(storedCooldown)
                ? Math.max(0.1F, Math.min(10.0F, storedCooldown)) : 1.0F;

        int wandUpgrade = wandStack.getOrCreateTag().getInt("wand_upgrade_level");

        float wandSpeedBonus = 1.0F - wandUpgrade * 0.02F;
        float wandEfficiency = Math.max(0.5F, 1.0F - wandUpgrade * 0.05F);

        String runeId = wandStack.getOrCreateTag().getString("wand_rune");
        WandRune rune = WandRune.getById(runeId);

        float armorCostReduce = com.huige233.transcend.items.armor.ElementArmor.getCostReduction(player, element);

        com.huige233.transcend.ascension.PlayerAscensionData ascData =
                com.huige233.transcend.ascension.AscensionCapability.get(player);
        if (computation.manaCost() == Integer.MAX_VALUE || computation.tier() > ascData.getSpellTier()) {
            player.displayClientMessage(Component.translatable("msg.transcend.wand.invalid_configuration")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        int spellTier = computation.tier();
        if (effects.size() > ascData.getMaxSpellEffectSlots()) {
            player.displayClientMessage(Component.translatable("msg.transcend.wand.too_many_effects")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!SpellConfigurationRules.areEffectsCompatible(carrier, effects)) {
            player.displayClientMessage(Component.translatable("gui.transcend.spell_config.incompatible")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        java.util.EnumMap<SpellEffect, Integer> effectCounts = new java.util.EnumMap<>(SpellEffect.class);
        for (SpellEffect configuredEffect : effects) {
            int count = effectCounts.merge(configuredEffect, 1, Integer::sum);
            if (count > 1 && !configuredEffect.isRepeatable()) {
                player.displayClientMessage(Component.translatable("msg.transcend.wand.effect_not_repeatable")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
        }
        float masteryDiscount = ascData.getManaCostReduction(element);
        int manaCost = Math.max(1, (int)(computation.manaCost() * wandEfficiency
                * (1.0F - armorCostReduce) * (1.0F - masteryDiscount)));
        if (rune == WandRune.CONSERVATION) manaCost = Math.max(1, manaCost - 1);
        if (rune == WandRune.OVERCHARGE) manaCost = Math.max(1, (int)(manaCost * 1.2F));
        double vowAdjustedCost = manaCost * (double) ascData.getVowManaCostMult();
        manaCost = Math.max(1, (int) Math.min(Integer.MAX_VALUE,
                Double.isFinite(vowAdjustedCost) ? vowAdjustedCost : manaCost));

        SpellAspect castAspect = chunkTintAspect(element);
        ChunkManaSavedData chunkMana = ChunkManaSavedData.getIfPresent(serverLevel);

        boolean manaFree = player instanceof net.minecraft.server.level.ServerPlayer sp
                && com.huige233.transcend.ascension.AscensionHandler.tryFreeCast(sp);
        SpellCastPayment.Result payment = castConfiguredSpellPaymentBoundary(
                player, wandStack, chunkMana, player.chunkPosition(), castAspect, manaCost, manaFree);
        manaCost = payment.manaCost();
        if (!payment.paid()) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.wand.no_mana", manaCost)
                            .withStyle(ChatFormatting.RED), true);
            return;
        }
        slotData.putInt("spell_tier", spellTier);
        if (manaFree) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.wand.free_cast")
                            .withStyle(ChatFormatting.AQUA), true);
        }

        ClassResourceHandler.onCast(player, element);

        int spellLevel = getSpellLevel(wandStack, selected);
        float levelDmgMult = getLevelDamageMult(spellLevel);
        float levelCdMult = getLevelCdMult(spellLevel);

        float armorBoost = com.huige233.transcend.items.armor.ElementArmor.getElementBoost(player, element);

        float ascensionMult = ascData.getSpellDamageMultiplier(element, player);

        float damage = element.getBaseDamage() * basePower * levelDmgMult * (1.0F + armorBoost) * ascensionMult;
        if (rune == WandRune.OVERCHARGE) damage *= 1.3F;
        if (rune == WandRune.GLASS_CANNON) damage *= 1.5F;

        damage *= com.huige233.transcend.spell.config.SpellConfigurationRules.amplifyMultiplier(effects);

        boolean isProjectile = carrier == SpellCarrier.ORB || carrier == SpellCarrier.ARROW
                || carrier == SpellCarrier.VORTEX || carrier == SpellCarrier.TRAP
                || carrier == SpellCarrier.RAIN;
        if (effects.contains(SpellEffect.SHIELD)) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1, false, true));
        }
        if (isProjectile) {
            float speed = Math.max(0.5F, carrier.getProjectileSpeed() * 0.15F);

            float projBasePower = basePower * levelDmgMult * (1.0F + armorBoost);
            if (rune == WandRune.OVERCHARGE)   projBasePower *= 1.3F;
            if (rune == WandRune.GLASS_CANNON) projBasePower *= 1.5F;
            com.huige233.transcend.spell.config.SpellConfigurationRules.MultishotPlan multishot =
                    com.huige233.transcend.spell.config.SpellConfigurationRules.multishotPlan(effects);
            int shotCount = multishot.projectileCount();
            projBasePower *= multishot.powerPerProjectile();
            if (carrier == SpellCarrier.RAIN) {
                castRain(serverLevel, player, carrier, element, projBasePower, projBasePower,
                        effects, spellTier);
            } else {
                for (int shot = 0; shot < shotCount; shot++) {
                    SpellProjectile proj = new SpellProjectile(serverLevel, player, carrier, element,
                            effects, projBasePower, spellTier);
                    float yawOffset = (shotCount > 1) ? (shot - (shotCount - 1) / 2.0F) * 7.5F : 0;
                    proj.shootFromRotation(player, player.getXRot(), player.getYRot() + yawOffset, 0.0F, speed, 1.0F);
                    serverLevel.addFreshEntity(proj);
                }
            }
        } else {
            CarrierResult carrierResult = applyCarrier(serverLevel, player, carrier, element, damage,
                    effects, basePower, spellTier);

            for (SpellEffect configuredEffect : effects) {
                applyEffect(serverLevel, player, configuredEffect, element, damage, spellTier,
                        carrierResult.actualHealthDamage);
            }
        }

        addSpellXp(wandStack, selected, 1);

        com.huige233.transcend.ascension.AscensionHandler.recordCast(player);

        float ascCdr = com.huige233.transcend.ascension.AscensionHandler.getCDR(player);
        float vowCooldownMult = com.huige233.transcend.ascension.AscensionCapability.get(player)
                .getVowCooldownMult();

        float cooldown = carrier.getBaseCooldown() * baseCooldownMult
                * wandSpeedBonus * levelCdMult * (1.0f - ascCdr);
        int minimumCooldown = 4;
        if (rune == WandRune.RAPID_FIRE) {
            cooldown *= 0.6F;
            minimumCooldown = 2;
        }

        if (effects.contains(SpellEffect.AMPLIFY)) {
            cooldown *= 0.6F;
            minimumCooldown = 2;
        }
        int cooldownTicks = Math.max(minimumCooldown, (int) (cooldown * vowCooldownMult));
        if (rune == WandRune.MANA_SIPHON && player.getHealth() < player.getMaxHealth()) {
            player.heal(damage * 0.1F);
        }
        if (rune == WandRune.GLASS_CANNON) {
            player.hurt(player.damageSources().magic(), 1.0F);
        }
        setSlotCooldown(wandStack, selected, cooldownTicks);

        if (rune == WandRune.SPELL_ECHO && serverLevel.getRandom().nextFloat() < 0.25F) {
            setSlotCooldown(wandStack, selected, 0);
            wandStack.getOrCreateTag().putBoolean("transcend_echo_pending", true);
        }

        if (rune == WandRune.CHAIN_CASTER) {
            int maxSl2 = getMaxSlots(wandStack);
            for (int i = 1; i <= maxSl2; i++) {
                int candidate = (selected + i) % maxSl2;
                if (isSlotOccupied(wandStack, candidate) && candidate != selected) {
                    setSelectedSlot(wandStack, candidate);
                    break;
                }
            }
        }

        if (rune == WandRune.ELEMENTAL_MASTERY) {
            player.getPersistentData().putInt("transcend_elemental_mastery", 60);
        }

        playElementSound(serverLevel, player, element);

        {
            ChunkManaSavedData auraData = ChunkManaSavedData.get(serverLevel);
            auraData.recordCastTint(player.chunkPosition(), chunkTintAspect(element));
        }

        int maxSl = getMaxSlots(wandStack);
        player.displayClientMessage(
                Component.literal("[")
                        .withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.literal("Slot " + (selected + 1) + "/" + maxSl)
                                .withStyle(ChatFormatting.GOLD))
                        .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.translatable(carrier.getDisplayKey()).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" + ").withStyle(ChatFormatting.GRAY))
                        .append(Component.translatable(element.getDisplayKey()).withStyle(getElementColor(element))),
                true);
    }

    static SpellCastPayment.Result castConfiguredSpellPaymentBoundary(
            Player player, ItemStack wandStack, @Nullable ChunkManaSavedData chunkMana,
            net.minecraft.world.level.ChunkPos chunkPos, @Nullable SpellAspect castAspect,
            int canonicalCost, boolean ascensionFree) {
        return castConfiguredSpellPaymentBoundary(player, wandStack, chunkMana, chunkPos, castAspect,
                canonicalCost, ascensionFree, () -> MagicCrystalHelper.getInnateMaxMana(player));
    }

    static SpellCastPayment.Result castConfiguredSpellPaymentBoundary(
            Player player, ItemStack wandStack, @Nullable ChunkManaSavedData chunkMana,
            net.minecraft.world.level.ChunkPos chunkPos, @Nullable SpellAspect castAspect,
            int canonicalCost, boolean ascensionFree, java.util.function.IntSupplier innateMaxMana) {
        if (player == null || wandStack == null || chunkPos == null) {
            throw new IllegalArgumentException("cast payment boundary requires player, wand and chunk position");
        }
        float chunkMultiplier = chunkMana == null
                ? 1.0F
                : chunkMana.getManaCostMultiplier(chunkPos, castAspect);
        boolean echoFree = wandStack.getOrCreateTag().getBoolean("transcend_echo_pending");
        return SpellCastPayment.attempt(canonicalCost, chunkMultiplier, ascensionFree, echoFree,
                new SpellCastPayment.Access() {
                    @Override
                    public long availableMana() {
                        return MagicCrystalHelper.countManaLong(player);
                    }

                    @Override
                    public boolean consumeMana(int amount) {
                        return MagicCrystalHelper.consumeMana(player, amount, innateMaxMana);
                    }

                    @Override
                    public void setEchoPending(boolean pending) {
                        wandStack.getOrCreateTag().putBoolean("transcend_echo_pending", pending);
                    }
                });
    }

    private static boolean isAoeCarrier(SpellCarrier carrier) {
        return carrier == SpellCarrier.NOVA
                || carrier == SpellCarrier.VORTEX
                || carrier == SpellCarrier.RAIN
                || carrier == SpellCarrier.CHAIN;
    }

    private CarrierResult applyCarrier(ServerLevel level, Player player, SpellCarrier carrier,
                              SpellElement element, float damage, List<SpellEffect> effects,
                              float basePower, int spellTier) {
        CarrierResult result = new CarrierResult(player.position());
        return switch (carrier) {
            case ORB     -> { result.actualHealthDamage = castOrb(level, player, carrier, element, damage, spellTier); yield result; }
            case ARROW   -> { result.actualHealthDamage = castArrow(level, player, element, damage, spellTier); yield result; }
            case SLASH   -> { result.actualHealthDamage = castSlash(level, player, element, damage, spellTier); yield result; }
            case BEAM    -> { result.actualHealthDamage = castBeam(level, player, element, damage, spellTier); yield result; }
            case NOVA    -> { result.actualHealthDamage = castNova(level, player, carrier, element, damage, spellTier); yield result; }
            case DASH    -> { result.actualHealthDamage = castDash(level, player, element, damage, spellTier); yield result; }
            case CHAIN   -> { result.actualHealthDamage = castChain(level, player, element, damage, spellTier); yield result; }
            case BARRIER -> { castBarrier(level, player, element, damage); yield result; }
            case RAIN, VORTEX, TRAP -> result;
        };
    }

    private static final class CarrierResult {
        private Vec3 impactPosition;
        private final List<LivingEntity> landedTargets = new ArrayList<>();
        private float actualHealthDamage;

        private CarrierResult(Vec3 impactPosition) {
            this.impactPosition = impactPosition;
        }
    }

    private static SpellAspect chunkTintAspect(SpellElement element) {
        if (element == null) return null;
        return switch (element.canonical()) {
            case FIRE -> SpellAspect.BLAZE;
            case METAL, WATER, EARTH -> SpellAspect.FROST;
            case WOOD -> SpellAspect.VERDANT;
            case CHAOS -> SpellAspect.CHAOS;
        };
    }

    private float castOrb(ServerLevel level, Player player, SpellCarrier carrier,
                         SpellElement element, float damage, int spellTier) {
        HitResult hit = player.pick(20, 0, false);
        Vec3 pos = hit.getLocation();
        double r = carrier.getAoeRadius();

        List<S2CParticleBatchPack.ParticleEntry> entries =
                ServerVisualGeometry.circle(pos.x, pos.y + 0.1, pos.z, r, 20, 0,
                        new Vector3f(0, 1, 0));
        sendParticles(level, pos, entries, element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.8F, 15);
        spawnVanillaFlairAOE(level, pos, element, r);

        AABB area = new AABB(pos.x - r, pos.y - r, pos.z - r,
                pos.x + r, pos.y + r, pos.z + r);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> canHitTarget(e, player));
        float total = 0.0F;
        for (LivingEntity target : targets) {
            total += applyElementDamage(level, player, target, element, damage, spellTier);
        }
        return total;
    }

    private float castArrow(ServerLevel level, Player player, SpellElement element, float damage, int spellTier) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 endPos = eyePos.add(look.scale(20));

        List<S2CParticleBatchPack.ParticleEntry> entries =
                ServerVisualGeometry.line(eyePos.x, eyePos.y, eyePos.z,
                        endPos.x, endPos.y, endPos.z, 15);
        sendParticles(level, eyePos.add(endPos).scale(0.5), entries,
                element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.5F, 10);

        AABB searchBox = new AABB(eyePos, endPos).inflate(1.5);
        List<Entity> candidates = level.getEntities(player, searchBox,
                e -> canHitTarget(e, player) && (e instanceof LivingEntity));
        Entity nearest = candidates.stream()
                .min(Comparator
                        .comparingDouble(e -> e.distanceToSqr(eyePos)))
                .orElse(null);

        if (nearest instanceof LivingEntity living) {
            return applyElementDamage(level, player, living, element, damage, spellTier);
        } 
        return 0.0F;
    }

    private float castSlash(ServerLevel level, Player player, SpellElement element, float damage, int spellTier) {
        Vec3 playerPos = player.position();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 eyePos = player.getEyePosition();

        ServerVisualBroadcaster.beam(level, eyePos, eyePos.add(look),
                element.getParticleR(), element.getParticleG(), element.getParticleB(), 6, "slash");

        List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();
        double baseAngle = Math.atan2(look.z, look.x);
        for (int i = 0; i < 12; i++) {
            double angle = baseAngle - Math.toRadians(60) + Math.toRadians(120) * i / 11.0;
            double px = eyePos.x + 3.0 * Math.cos(angle);
            double pz = eyePos.z + 3.0 * Math.sin(angle);
            entries.add(new S2CParticleBatchPack.ParticleEntry(px, eyePos.y, pz, 0.0F, 0.0F, 0.0F));
        }
        sendParticles(level, eyePos, entries, element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.7F, 12);

        List<S2CParticleBatchPack.ParticleEntry> innerEntries = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double angle = baseAngle - Math.toRadians(45) + Math.toRadians(90) * i / 7.0;
            double px = eyePos.x + 2.0 * Math.cos(angle);
            double pz = eyePos.z + 2.0 * Math.sin(angle);
            innerEntries.add(new S2CParticleBatchPack.ParticleEntry(px, eyePos.y - 0.3F, pz, 0.0F, 0.0F, 0.0F));
        }
        sendParticles(level, eyePos, innerEntries, element.getParticleR() * 0.7F, element.getParticleG() * 0.7F, element.getParticleB() * 0.7F, 0.5F, 8);
        spawnVanillaFlairAOE(level, eyePos.add(look.scale(1.5)), element, 2.0);

        AABB area = player.getBoundingBox().inflate(3);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> canHitTarget(e, player));

        float total = 0.0F;
        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().subtract(playerPos).normalize();
            double dot = look.x * toTarget.x + look.z * toTarget.z;

            if (dot >= 0.5) {
                total += applyElementDamage(level, player, target, element, damage, spellTier);
            }
        }
        return total;
    }

    private float castBeam(ServerLevel level, Player player, SpellElement element, float damage, int spellTier) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 endPos = eyePos.add(look.scale(30));

        ServerVisualBroadcaster.beam(level, eyePos, endPos,
                element.getParticleR(), element.getParticleG(), element.getParticleB(), 8, "beam");

        List<S2CParticleBatchPack.ParticleEntry> entries =
                ServerVisualGeometry.line(eyePos.x, eyePos.y, eyePos.z,
                        endPos.x, endPos.y, endPos.z, 20);
        sendParticles(level, eyePos.add(endPos).scale(0.5), entries,
                element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.6F, 12);
        spawnVanillaFlairAOE(level, endPos, element, 1.0);

        for (double d = 0.5; d <= 30.0; d += 0.5) {
            Vec3 checkPos = eyePos.add(look.scale(d));
            AABB checkBox = new AABB(checkPos.x - 0.5, checkPos.y - 0.5, checkPos.z - 0.5,
                    checkPos.x + 0.5, checkPos.y + 0.5, checkPos.z + 0.5);
            List<Entity> hits = level.getEntities(player, checkBox,
                    e -> canHitTarget(e, player) && (e instanceof LivingEntity));
            Entity first = hits.stream()
                    .min(Comparator
                            .comparingDouble(e -> e.distanceToSqr(eyePos)))
                    .orElse(null);
            if (first instanceof LivingEntity living) {
                return applyElementDamage(level, player, living, element, damage, spellTier);
            } 
        }
        return 0.0F;
    }

    private float castNova(ServerLevel level, Player player, SpellCarrier carrier,
                          SpellElement element, float damage, int spellTier) {
        Vec3 pos = player.position();
        double r = carrier.getAoeRadius();

        ServerVisualBroadcaster.beam(level, pos, pos,
                element.getParticleR(), element.getParticleG(), element.getParticleB(), 10, "nova");

        List<S2CParticleBatchPack.ParticleEntry> entries =
                ServerVisualGeometry.circle(pos.x, pos.y + 0.5, pos.z, 5.0, 30, 0,
                        new Vector3f(0, 1, 0));
        sendParticles(level, pos, entries, element.getParticleR(), element.getParticleG(), element.getParticleB(), 1.0F, 18);

        List<S2CParticleBatchPack.ParticleEntry> ring2 = ServerVisualGeometry.circle(
                pos.x, pos.y + 0.5, pos.z, r * 0.6, 16, Math.PI / 6, new Vector3f(0, 1, 0));
        sendParticles(level, pos, ring2, element.getParticleR() * 0.6F, element.getParticleG() * 0.6F, element.getParticleB() * 0.6F, 0.6F, 10);
        spawnVanillaFlairAOE(level, pos, element, r);

        AABB area = player.getBoundingBox().inflate(r);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> canHitTarget(e, player));
        float total = 0.0F;
        for (LivingEntity target : targets) {
            total += applyElementDamage(level, player, target, element, damage, spellTier);
        }
        return total;
    }

    private void castBreath(ServerLevel level, Player player, SpellElement element, float damage, int spellTier) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        Vec3 coneCenter = eyePos.add(look.scale(2.5));
        AABB coneBox = new AABB(
                coneCenter.x - 2.5, coneCenter.y - 1.0, coneCenter.z - 2.5,
                coneCenter.x + 2.5, coneCenter.y + 1.0, coneCenter.z + 2.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, coneBox,
                e -> canHitTarget(e, player));
        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().subtract(player.position()).normalize();
            double dot = look.x * toTarget.x + look.z * toTarget.z;
            if (dot >= 0.5) {
                applyElementDamage(level, player, target, element, damage, spellTier);
            }
        }

        for (int i = 1; i <= 5; i++) {
            double dist = i * 1.0;
            double spread = dist * 0.4;
            Vec3 point = eyePos.add(look.scale(dist));
        List<S2CParticleBatchPack.ParticleEntry> cone = ServerVisualGeometry.circle(
                    point.x, point.y, point.z, spread, 6, i * 0.5, new Vector3f(0, 1, 0));
            sendParticles(level, point, cone, element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.5F, 6);
        }
        spawnVanillaFlairAOE(level, eyePos.add(look.scale(2.5)), element, 2.0);
    }

    private void castRain(ServerLevel level, Player player, SpellCarrier carrier,
                          SpellElement element, float damage, float basePower,
                          List<SpellEffect> effects, int spellTier) {
        HitResult hit = player.pick(20, 0, false);
        Vec3 target = hit.getLocation();
        for (int i = 0; i < 8; i++) {
            double ox = (level.getRandom().nextDouble() - 0.5) * carrier.getAoeRadius() * 2;
            double oz = (level.getRandom().nextDouble() - 0.5) * carrier.getAoeRadius() * 2;
            SpellProjectile proj = new SpellProjectile(level, player, carrier, element,
                    effects, basePower, spellTier);
            proj.setPos(target.x + ox, target.y + 12.0, target.z + oz);
            proj.setDeltaMovement(0, -0.8, 0);
            level.addFreshEntity(proj);
        }
    }

    private float castDash(ServerLevel level, Player player, SpellElement element, float damage, int spellTier) {

        if (player instanceof net.minecraft.server.level.ServerPlayer sp
                && NexusWorldPenalty.isDashBlocked(sp)) {
            sp.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("msg.transcend.nexus_binding_blocked"),
                true);
            return 0.0F;
        }
        Vec3 look = player.getLookAngle().normalize();
        Optional<Vec3> safeTarget = findSafeMovementDestination(level, player,
                player.position().add(look.scale(4.0)), 2);
        if (safeTarget.isEmpty()) return 0.0F;
        Vec3 dashTarget = safeTarget.get();

        AABB pathBox = new AABB(
                Math.min(player.getX(), dashTarget.x) - 0.5,
                player.getY() - 0.5,
                Math.min(player.getZ(), dashTarget.z) - 0.5,
                Math.max(player.getX(), dashTarget.x) + 0.5,
                player.getY() + 2.0,
                Math.max(player.getZ(), dashTarget.z) + 0.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, pathBox,
                e -> canHitTarget(e, player));
        float total = 0.0F;
        for (LivingEntity target : targets) {
            total += applyElementDamage(level, player, target, element, damage, spellTier);
        }

        Vec3 oldPos = player.position();
        player.teleportTo(dashTarget.x, dashTarget.y, dashTarget.z);

        for (int i = 0; i < 8; i++) {
            double t = i / 8.0;
            Vec3 trailPos = oldPos.add(dashTarget.subtract(oldPos).scale(t));
        List<S2CParticleBatchPack.ParticleEntry> trailRing = ServerVisualGeometry.circle(
                    trailPos.x, trailPos.y + 0.5, trailPos.z, 0.3, 6, i * 0.8, new Vector3f(0, 1, 0));
            sendParticles(level, trailPos, trailRing, element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.4F, 6);
        }
        spawnVanillaFlairAOE(level, dashTarget, element, 1.5);
        return total;
    }

    private void castGround(ServerLevel level, Player player, SpellCarrier carrier,
                            SpellElement element, float damage, int spellTier) {
        Vec3 pos = player.position();
        double r = carrier.getAoeRadius();

        List<S2CParticleBatchPack.ParticleEntry> entries =
                ServerVisualGeometry.circle(pos.x, pos.y + 0.1, pos.z, r, 24, 0,
                        new Vector3f(0, 1, 0));
        sendParticles(level, pos, entries, element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.9F, 16);

        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2 * i / 6;
            double sx = pos.x + r * 0.7 * Math.cos(angle);
            double sz = pos.z + r * 0.7 * Math.sin(angle);
        List<S2CParticleBatchPack.ParticleEntry> spike = ServerVisualGeometry.line(
                    sx, pos.y, sz, sx, pos.y + 1.5, sz, 5);
            sendParticles(level, new Vec3(sx, pos.y + 0.75, sz), spike, element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.7F, 10);
        }
        spawnVanillaFlairAOE(level, pos, element, r);

        AABB area = new AABB(pos.x - r, pos.y - 0.5, pos.z - r,
                pos.x + r, pos.y + 1.5, pos.z + r);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> canHitTarget(e, player));
        for (LivingEntity target : targets) {
            applyElementDamage(level, player, target, element, damage, spellTier);
        }
    }

    private static float getBasePowerFromWand(Player player) {
        return 1.0F;
    }

    private float castChain(ServerLevel level, Player player, SpellElement element, float damage, int spellTier) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double range = 12.0;
        AABB area = player.getBoundingBox().inflate(range);
        List<LivingEntity> all = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> canHitTarget(e, player));
        all.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));

        LivingEntity current = null;
        for (LivingEntity e : all) {
            Vec3 toE = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye).normalize();
            if (look.dot(toE) > 0.7) {
                current = e;
                break;
            }
        }
        if (current == null && !all.isEmpty()) current = all.get(0);
        if (current == null) return 0.0F;

        int bounces = 4;
        LivingEntity prev = null;
        float total = 0.0F;
        for (int i = 0; i < bounces && current != null; i++) {
            total += applyElementDamage(level, player, current, element,
                    damage * (1.0F - i * 0.15F), spellTier);
        List<S2CParticleBatchPack.ParticleEntry> line = ServerVisualGeometry.line(
                    prev != null ? prev.getX() : player.getX(),
                    prev != null ? prev.getY() + prev.getBbHeight() * 0.5 : player.getEyeY(),
                    prev != null ? prev.getZ() : player.getZ(),
                    current.getX(), current.getY() + current.getBbHeight() * 0.5, current.getZ(), 10);
            sendParticles(level, player.position(), line, element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.8F, 5);
            prev = current;
            LivingEntity finalCurrent = current;
            current = all.stream()
                    .filter(e -> e != finalCurrent && canHitTarget(e, player) && e.distanceTo(finalCurrent) < 6.0)
                    .min(Comparator.comparingDouble(e -> e.distanceTo(finalCurrent)))
                    .orElse(null);
        }
        return total;
    }

    private void castSpike(ServerLevel level, Player player, SpellElement element, float damage, int spellTier) {
        HitResult hit = player.pick(16, 0, false);
        Vec3 target = hit.getLocation();
        AABB area = new AABB(target.x - 1.5, target.y - 0.5, target.z - 1.5,
                target.x + 1.5, target.y + 3.0, target.z + 1.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> canHitTarget(e, player));
        for (LivingEntity t : targets) {
            applyElementDamage(level, player, t, element, damage, spellTier);
            t.setDeltaMovement(t.getDeltaMovement().add(0, 0.5, 0));
        }
        List<S2CParticleBatchPack.ParticleEntry> spikes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            double px = target.x + Math.cos(angle) * 1.0;
            double pz = target.z + Math.sin(angle) * 1.0;
            spikes.addAll(ServerVisualGeometry.line(px, target.y, pz, px, target.y + 2.0, pz, 6));
        }
        sendParticles(level, target, spikes, element.getParticleR(), element.getParticleG(), element.getParticleB(), 1.0F, 10);
    }

    private void castTeleport(ServerLevel level, Player player, SpellElement element, float damage, int spellTier) {
        HitResult hit = player.pick(30, 0, false);
        Optional<Vec3> safeTarget = findSafeMovementDestination(level, player, hit.getLocation(), 3);
        if (safeTarget.isEmpty()) return;
        Vec3 target = safeTarget.get();
        Vec3 oldPos = player.position();
        player.teleportTo(target.x, target.y, target.z);
        AABB area = new AABB(target.x - 3, target.y - 1, target.z - 3,
                target.x + 3, target.y + 3, target.z + 3);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> canHitTarget(e, player));
        for (LivingEntity t : targets) {
            applyElementDamage(level, player, t, element, damage, spellTier);
        }
        List<S2CParticleBatchPack.ParticleEntry> ring = ServerVisualGeometry.circle(
                target.x, target.y + 0.1, target.z, 3.0, 24, 0, new Vector3f(0, 1, 0));
        sendParticles(level, target, ring, element.getParticleR(), element.getParticleG(), element.getParticleB(), 1.0F, 8);
    }

    private static Optional<Vec3> findSafeMovementDestination(ServerLevel level, Player player,
                                                               Vec3 intended, int fallbackRadius) {
        if (intended == null || !Double.isFinite(intended.x) || !Double.isFinite(intended.y)
                || !Double.isFinite(intended.z)) return Optional.empty();
        Vec3 start = player.position().add(0.0, 0.1, 0.0);
        Vec3 end = intended.add(0.0, 0.1, 0.0);
        BlockHitResult obstruction = level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (obstruction.getType() == HitResult.Type.BLOCK) {
            Vec3 direction = end.subtract(start);
            if (direction.lengthSqr() > 1.0E-6) {
                intended = obstruction.getLocation().subtract(direction.normalize().scale(0.45))
                        .add(0.0, -0.1, 0.0);
            }
        }

        BlockPos origin = BlockPos.containing(intended);
        Vec3 playerPos = player.position();
        return SafeDestinationSearch.find(origin.getX(), origin.getY(), origin.getZ(), fallbackRadius, grid -> {
            BlockPos feet = new BlockPos(grid.x(), grid.y(), grid.z());
            BlockPos head = feet.above();
            BlockPos support = feet.below();
            if (!level.hasChunkAt(feet) || !level.hasChunkAt(head) || !level.hasChunkAt(support)
                    || !level.getWorldBorder().isWithinBounds(feet)) return false;
            if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    || !level.getBlockState(head).getCollisionShape(level, head).isEmpty()
                    || !level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)) return false;
            Vec3 destination = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
            return hasClearMovementPath(level, player, destination)
                    && level.noCollision(player, player.getBoundingBox().move(destination.subtract(playerPos)));
        }).map(grid -> new Vec3(grid.x() + 0.5, grid.y(), grid.z() + 0.5));
    }

    private static boolean hasClearMovementPath(ServerLevel level, Player player, Vec3 destination) {
        for (double height : new double[] {0.1, 1.7}) {
            Vec3 start = player.position().add(0.0, height, 0.0);
            Vec3 end = destination.add(0.0, height, 0.0);
            if (level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.BLOCK) return false;
        }
        return true;
    }

    private void castBarrier(ServerLevel level, Player player, SpellElement element, float damage) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 2));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 2));
        Vec3 pos = player.position();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 wallCenter = pos.add(look.scale(2.0));
        List<S2CParticleBatchPack.ParticleEntry> wall = new ArrayList<>();
        Vector3f right = new Vector3f((float) -look.z, 0, (float) look.x);
        for (int h = 0; h < 6; h++) {
            for (int w = -3; w <= 3; w++) {
                wall.add(new S2CParticleBatchPack.ParticleEntry(
                        wallCenter.x + right.x() * w * 0.5,
                        wallCenter.y + h * 0.5,
                        wallCenter.z + right.z() * w * 0.5));
            }
        }
        sendParticles(level, wallCenter, wall, element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.8F, 20);
    }

    private void castVortex(ServerLevel level, Player player, SpellCarrier carrier,
                            SpellElement element, float damage, int spellTier) {
        HitResult hit = player.pick(20, 0, false);
        Vec3 pos = hit.getLocation();

        for (int ring = 0; ring < 3; ring++) {
            double r = carrier.getAoeRadius() * (1.0 - ring * 0.2);
            List<com.huige233.transcend.network.S2CParticleBatchPack.ParticleEntry> entries =
                    ServerVisualGeometry.circle(
                            pos.x, pos.y + 0.2 + ring * 0.5, pos.z, r, 16, ring * 1.0,
                            new org.joml.Vector3f(0, 1, 0));
            sendParticles(level, pos, entries,
                    element.getParticleR(), element.getParticleG(), element.getParticleB(),
                    0.9F - ring * 0.2F, 14);
        }

        com.huige233.transcend.handle.SpellZoneTickHandler.spawnVortex(
                level, pos, element, damage, spellTier, player, carrier.getAoeRadius());
    }

    private void castTrap(ServerLevel level, Player player, SpellCarrier carrier,
                          SpellElement element, float damage, int spellTier) {
        HitResult hit = player.pick(20, 0, false);
        Vec3 pos = hit.getLocation();

        List<com.huige233.transcend.network.S2CParticleBatchPack.ParticleEntry> entries =
                ServerVisualGeometry.circle(
                        pos.x, pos.y + 0.05, pos.z, 1.2, 12, 0,
                        new org.joml.Vector3f(0, 1, 0));
        sendParticles(level, pos, entries,
                element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.7F, 10);

        com.huige233.transcend.handle.SpellZoneTickHandler.spawnTrap(
                level, pos, element, damage, spellTier, player);
    }

    private void castRing(ServerLevel level, Player player, SpellCarrier carrier,
                          SpellElement element, float damage, int spellTier) {
        double r = carrier.getAoeRadius();
        AABB area = player.getBoundingBox().inflate(r);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> canHitTarget(e, player));
        for (LivingEntity t : targets) {
            double dx = t.getX() - player.getX();
            double dz = t.getZ() - player.getZ();
            if (Math.sqrt(dx * dx + dz * dz) <= r) {
                applyElementDamage(level, player, t, element, damage * 0.5F, spellTier);
            }
        }
        List<S2CParticleBatchPack.ParticleEntry> ring = ServerVisualGeometry.circle(
                player.getX(), player.getY() + 0.5, player.getZ(), r, 36, 0, new Vector3f(0, 1, 0));
        sendParticles(level, player.position(), ring, element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.9F, 12);
    }

    private float applyElementDamage(ServerLevel level, Player player,
                                    LivingEntity target, SpellElement element, float damage, int spellTier) {
        damage *= ClassResourceHandler.getSpellDamageMultiplier(player, element);
        return SpellDamageService.deal(level, player, player, target, element, spellTier, damage, true)
                .actualHealthDamage();
    }

    private void applyEffect(ServerLevel level, Player player, SpellEffect effect,
                             SpellElement element, float damage, int spellTier, float landedHealthDamage) {
        switch (effect) {
            case EXPLOSION -> {
                HitResult hit = player.pick(20, 0, false);
                Vec3 pos = hit.getLocation();
                applyVisualExplosion(level, player, pos, element, damage, spellTier);
            }
            case PIERCING -> {
                Vec3 eyePos = player.getEyePosition();
                Vec3 look = player.getLookAngle();
                AABB searchBox = new AABB(eyePos, eyePos.add(look.scale(20))).inflate(1.5);
                List<LivingEntity> extras = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                        e -> canHitTarget(e, player));
                boolean skippedFirst = false;
                for (LivingEntity extra : extras) {
                    if (!skippedFirst) {
                        skippedFirst = true;
                        continue;
                    }
                    SpellDamageService.deal(level, player, player, extra, element,
                            spellTier, damage * 0.5F, true);
                }
            }
            case SPLIT -> {

            }
            case HOMING -> {

            }
            case HEALING -> {
                player.heal(damage * 0.5F);
            }
            case SHIELD -> {
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1, false, true));
            }
            case CHAIN_LIGHTNING, AMPLIFY, LIFESTEAL, MULTISHOT, SLOWFIELD, MARK -> {
                HitResult effectHit = player.pick(20, 0, false);
                Vec3 effectPos = effectHit.getLocation();
                AABB effectBox = new AABB(effectPos.x - 3, effectPos.y - 2, effectPos.z - 3,
                        effectPos.x + 3, effectPos.y + 2, effectPos.z + 3);
                List<LivingEntity> effectTargets = level.getEntitiesOfClass(LivingEntity.class, effectBox,
                        e -> canHitTarget(e, player));
                switch (effect) {
                    case CHAIN_LIGHTNING -> {
                        effectTargets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(effectPos.x, effectPos.y, effectPos.z)));
                        int chains = Math.min(3, effectTargets.size());
                        for (int i = 0; i < chains; i++) {
                            LivingEntity target = effectTargets.get(i);
                            SpellDamageService.deal(level, player, player, target, element,
                                    spellTier, damage * 0.6F, true);
                        }
                    }
                    case AMPLIFY -> {

                    }
                    case LIFESTEAL -> {
                        player.heal(Math.max(0.0F, landedHealthDamage) * 0.15F);
                    }
                    case SLOWFIELD -> {
                        for (LivingEntity t : effectTargets) {
                            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true));
                        }
                    }
                    case MARK -> {
                        for (LivingEntity t : effectTargets) {
                            t.getPersistentData().putInt(SpellDamageService.CANONICAL_MARK_TAG, 100);
                        }
                    }
                    default -> {}
                }
            }
            case ROOT -> {
                HitResult hit = player.pick(20, 0, false);
                Vec3 pos = hit.getLocation();
                AABB box = new AABB(pos.x - 1.5, pos.y - 1.5, pos.z - 1.5,
                        pos.x + 1.5, pos.y + 1.5, pos.z + 1.5);
                level.getEntitiesOfClass(LivingEntity.class, box, e -> canHitTarget(e, player))
                        .forEach(e -> {
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4, false, true));
                            Vec3 pull = pos.subtract(e.position());
                            if (pull.lengthSqr() > 0.0001) e.setDeltaMovement(e.getDeltaMovement().add(pull.normalize().scale(0.5)));
                            e.hurtMarked = true;
                        });
            }
            case BLIGHT -> {
                HitResult hit = player.pick(20, 0, false);
                Vec3 pos = hit.getLocation();
                AABB box = new AABB(pos.x - 1.5, pos.y - 1.5, pos.z - 1.5,
                        pos.x + 1.5, pos.y + 1.5, pos.z + 1.5);
                level.getEntitiesOfClass(LivingEntity.class, box, e -> canHitTarget(e, player))
                        .forEach(e -> {
                            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 2, false, true));
                            e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 2, false, true));
                            SpellDamageService.deal(level, player, player, e, element, spellTier, 1.5F, true);
                        });
            }
            case CURSE -> {
                HitResult curseHit = player.pick(10, 0, false);
                Vec3 cursePos = curseHit.getLocation();
                AABB curseBox = new AABB(cursePos.x - 1.5, cursePos.y - 1.5, cursePos.z - 1.5,
                        cursePos.x + 1.5, cursePos.y + 1.5, cursePos.z + 1.5);
                level.getEntitiesOfClass(LivingEntity.class, curseBox, e -> canHitTarget(e, player))
                        .forEach(e -> {
                            e.getPersistentData().putInt(SpellDamageService.CURSE_TAG, 160);
                            e.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                        });
            }
            case OVERLOAD -> {

                HitResult overloadHit = player.pick(10, 0, false);
                Vec3 overloadPos = overloadHit.getLocation();
                AABB overloadBox = new AABB(overloadPos.x - 1.5, overloadPos.y - 1.5, overloadPos.z - 1.5,
                        overloadPos.x + 1.5, overloadPos.y + 1.5, overloadPos.z + 1.5);
                level.getEntitiesOfClass(LivingEntity.class, overloadBox, e -> canHitTarget(e, player))
                        .forEach(e -> SpellDamageService.deal(level, player, player, e, element,
                                spellTier, damage, true));
                player.hurt(player.damageSources().magic(), damage * 0.25F);
            }
            case SHATTER -> {
                HitResult shatterHit = player.pick(10, 0, false);
                Vec3 shatterPos = shatterHit.getLocation();
                AABB shatterBox = new AABB(shatterPos.x - 1.5, shatterPos.y - 1.5, shatterPos.z - 1.5,
                        shatterPos.x + 1.5, shatterPos.y + 1.5, shatterPos.z + 1.5);
                level.getEntitiesOfClass(LivingEntity.class, shatterBox, e -> canHitTarget(e, player))
                        .forEach(e -> {
                            applyTimedArmorReduction(e);
                            if (e instanceof Player targetPlayer) {
                                targetPlayer.getCooldowns().addCooldown(net.minecraft.world.item.Items.SHIELD, 120);
                            }
                        });
            }
        }
    }

    private static void applyTimedArmorReduction(LivingEntity target) {
        var armor = target.getAttribute(Attributes.ARMOR);
        if (armor == null) return;
        armor.removeModifier(ARMOR_BREAK_UUID);
        armor.addTransientModifier(new AttributeModifier(ARMOR_BREAK_UUID, "spell_shatter", -8.0,
                AttributeModifier.Operation.ADDITION));
        target.getPersistentData().putInt("transcend_armor_break", 120);
    }

    private void applyVisualExplosion(ServerLevel level, Player player, Vec3 pos, SpellElement element,
                                      float rawDamage, int spellTier) {
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z,
                8, 1.0, 0.6, 1.0, 0.05);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 0.9F, 1.1F);
        level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(3.0),
                        e -> canHitTarget(e, player))
                .stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(pos)))
                .limit(12)
                .forEach(e -> SpellDamageService.deal(level, player, player, e, element,
                        spellTier, rawDamage * 0.55F, true));
    }

    private void playElementSound(ServerLevel level, Player player, SpellElement element) {
        BlockPos pos = player.blockPosition();
        switch (element) {
            case METAL -> level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6F, 1.3F);
            case WOOD -> level.playSound(null, pos, SoundEvents.BEEHIVE_EXIT, SoundSource.PLAYERS, 0.5F, 1.0F);
            case WATER -> level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.2F);
            case FIRE -> level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            case EARTH -> level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5F, 0.8F);
            case CHAOS -> level.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.2F, 1.5F);
        }
    }

    private void sendParticles(ServerLevel level, Vec3 center,
                               List<S2CParticleBatchPack.ParticleEntry> entries,
                               float r, float g, float b, float scale, int lifetime) {
        float radius = estimateBatchRadius(center, entries, Math.max(0.8F, scale * 2.5F));
        int shaderLifetime = Math.max(10, Math.min(28, lifetime));
        String pattern = entries.size() >= 20 ? "hexagram" : "pentagram";
        ServerVisualBroadcaster.circle(level, center.add(0.0, 0.06, 0.0), radius,
                r, g, b, shaderLifetime, 24, pattern);
        if (entries.size() >= 12) {
            ServerVisualBroadcaster.shieldRipple(level, center.add(0.0, 0.45, 0.0),
                    radius * 0.8F, r, g, b, shaderLifetime);
        }
        if (entries.size() >= 30) {
            ServerVisualBroadcaster.shockwave(level, center.add(0.0, 0.1, 0.0),
                    radius * 1.25F, r, g, b, shaderLifetime + 6);
        }
    }

    private void spawnVanillaFlairAOE(ServerLevel level, Vec3 pos, SpellElement element, double radius) {
        float rr = element.getParticleR();
        float gg = element.getParticleG();
        float bb = element.getParticleB();
        float rad = (float) Math.max(1.0, radius);
        ServerVisualBroadcaster.circle(level, pos.add(0.0, 0.08, 0.0), rad, rr, gg, bb,
                16, 24, element == SpellElement.CHAOS ? "pentagram" : "hexagram");
        ServerVisualBroadcaster.shieldRipple(level, pos.add(0.0, 0.45, 0.0),
                rad * 0.75F, rr, gg, bb, 12);
        ServerVisualBroadcaster.beam(level, pos.add(-rad * 0.6, 0.6, 0.0),
                pos.add(rad * 0.6, 0.6, 0.0), rr, gg, bb, 12, shaderTypeForElement(element));
    }

    private float estimateBatchRadius(Vec3 center, List<S2CParticleBatchPack.ParticleEntry> entries, float fallback) {
        if (entries == null || entries.isEmpty()) return fallback;
        int step = Math.max(1, entries.size() / 24);
        double acc = 0.0;
        int samples = 0;
        for (int i = 0; i < entries.size(); i += step) {
            S2CParticleBatchPack.ParticleEntry e = entries.get(i);
            double dx = e.x - center.x;
            double dy = e.y - center.y;
            double dz = e.z - center.z;
            acc += Math.sqrt(dx * dx + dy * dy + dz * dz);
            samples++;
        }
        if (samples == 0) return fallback;
        return (float) Math.max(0.8, Math.min(8.0, acc / samples));
    }

    private String shaderTypeForElement(SpellElement element) {
        return switch (element) {
            case METAL, FIRE -> "beam";
            case CHAOS -> "slash";
            default -> "nova";
        };
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        ensureWandNBT(stack);

        int maxSl = getMaxSlots(stack);
        int selected = getSelectedSlot(stack);

        String tierName;
        ChatFormatting tierColor;
        if (maxSl >= 7) {
            tierName = "Master";
            tierColor = ChatFormatting.RED;
        } else if (maxSl >= 5) {
            tierName = "Advanced";
            tierColor = ChatFormatting.DARK_PURPLE;
        } else {
            tierName = "Basic";
            tierColor = ChatFormatting.BLUE;
        }

        tooltip.add(Component.translatable("tooltip.transcend.wand.tier", tierName)
                .withStyle(tierColor));
        tooltip.add(Component.translatable("tooltip.transcend.wand.max_slots", maxSl)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.wand.cast_interval", castInterval)
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.transcend.wand.slots_header")
                .withStyle(ChatFormatting.GOLD));

        for (int i = 0; i < maxSl; i++) {
                CompoundTag slotNbt = getSlotData(stack, i);
                String prefix = (i == selected) ? ">>> " : "  ";
                ChatFormatting slotColor = (i == selected) ? ChatFormatting.YELLOW : ChatFormatting.GRAY;

                if (slotNbt.contains("carrier")) {
                    SpellCarrier carrier = SpellCarrier.getById(slotNbt.getString("carrier"));
                    SpellElement element = SpellElement.getById(slotNbt.getString("element"));
                    SpellEffect effect = SpellEffect.getById(slotNbt.getString("effect"));

                    Component slotDesc = Component.literal(prefix + (i + 1) + ": ")
                            .withStyle(slotColor)
                            .append(Component.translatable(carrier.getDisplayKey()).withStyle(ChatFormatting.GOLD))
                            .append(Component.literal(" + ").withStyle(ChatFormatting.DARK_GRAY))
                            .append(Component.translatable(element.getDisplayKey()).withStyle(getElementColor(element)));

                    if (effect != null) {
                        slotDesc = slotDesc.copy()
                                .append(Component.literal(" + ").withStyle(ChatFormatting.DARK_GRAY))
                                .append(Component.translatable(effect.getDisplayKey()).withStyle(ChatFormatting.LIGHT_PURPLE));
                    }

                    int cd = getSlotCooldown(stack, i);
                    if (cd > 0) {
                        slotDesc = slotDesc.copy()
                                .append(Component.literal(" [CD:" + cd + "]").withStyle(ChatFormatting.RED));
                    } else {
                        slotDesc = slotDesc.copy()
                                .append(Component.literal(" [Ready]").withStyle(ChatFormatting.GREEN));
                    }

                    tooltip.add(slotDesc);

                    int slotLevel = getSpellLevel(stack, i);
                    int slotXp = getSpellXp(stack, i);
                    tooltip.add(Component.literal("    Lv." + slotLevel + " [" + slotXp + " XP]")
                            .withStyle(ChatFormatting.YELLOW));
                } else {
                    tooltip.add(Component.literal(prefix + (i + 1) + ": ")
                            .withStyle(slotColor)
                            .append(Component.translatable("tooltip.transcend.wand.empty_slot")
                                    .withStyle(ChatFormatting.DARK_GRAY)));
                }
        }

        CompoundTag currentSlot = getSlotData(stack, selected);
        if (currentSlot.contains("carrier")) {
            int cost = getManaCostFromTag(currentSlot);
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("tooltip.transcend.spell_scroll.mana_cost", cost)
                    .withStyle(ChatFormatting.AQUA));
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.transcend.wand.usage")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.wand.usage_load")
                .withStyle(ChatFormatting.DARK_GRAY));

        if (countOccupied(stack) >= 2) {
            tooltip.add(Component.translatable("tooltip.transcend.wand.sequence")
                    .withStyle(ChatFormatting.DARK_AQUA));
        }

        String resonance = getResonanceElement(stack);
        if (resonance != null) {
            SpellElement resEl = SpellElement.getById(resonance);
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("tooltip.transcend.wand.resonance")
                    .withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.literal(" ").append(
                            Component.translatable(resEl.getDisplayKey()).withStyle(ChatFormatting.GOLD))));
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
