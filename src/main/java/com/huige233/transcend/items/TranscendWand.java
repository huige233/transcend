package com.huige233.transcend.items;

import com.huige233.transcend.ModRarities;
import com.huige233.transcend.client.magic.MagicCircleGeometry;
import com.huige233.transcend.client.magic.MagicCrystalHelper;
import com.huige233.transcend.client.renderer.ShaderSpellRenderer;
import com.huige233.transcend.entity.RainbowLightning;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.network.S2CParticleBatchPack;
import com.huige233.transcend.spell.SpellCarrier;
import com.huige233.transcend.spell.SpellEffect;
import com.huige233.transcend.spell.SpellElement;
import com.huige233.transcend.spell.SpellProjectile;
import com.huige233.transcend.spell.ElementReaction;
import com.huige233.transcend.spell.WandRune;
import com.huige233.transcend.util.EntityCompatUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class TranscendWand extends Item {

    private static final UUID ACID_ARMOR_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID ARMOR_BREAK_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");

    private static final String TAG_WAND_SLOTS = "wand_slots";
    private static final String TAG_SELECTED_SLOT = "selected_slot";
    private static final String TAG_MAX_SLOTS = "max_slots";

    private final int maxSlots;
    private final int castInterval;

    public TranscendWand(int maxSlots, int castInterval) {
        super(new Properties().stacksTo(1).fireResistant());
        this.maxSlots = maxSlots;
        this.castInterval = castInterval;
        ModItems.ITEMS.add(this);
    }

    // ═══════════════════════════════════════════
    //  NBT initialization
    // ═══════════════════════════════════════════

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

    // ═══════════════════════════════════════════
    //  Slot accessors
    // ═══════════════════════════════════════════

    private static int getMaxSlots(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return (tag != null && tag.contains(TAG_MAX_SLOTS)) ? tag.getInt(TAG_MAX_SLOTS) : 3;
    }

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

    // ── Per-slot cooldown helpers ──

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

    // ── Spell XP/Level helpers ──

    private static final int[] XP_THRESHOLDS = {0, 10, 25, 50, 80, 120, 170, 230, 300, 400};
    private static final float[] LEVEL_DAMAGE_MULT = {1.0F, 1.08F, 1.18F, 1.30F, 1.45F, 1.62F, 1.82F, 2.05F, 2.30F, 2.60F};
    // v6 CD 重设计：法术等级 CD 缩减大幅收敛（0.55→0.82），不再让法术等级单独把 CD
    // 推到机关枪。机关枪手感保留给"满飞升"持有者（ascCdr 接近 0.75 时配合此值才能
    // 把多数 carrier 推到 4-tick floor）。
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

        // Check for level up
        while (level < 10 && xp >= XP_THRESHOLDS[level]) {
            xp -= XP_THRESHOLDS[level];
            level++;
        }
        if (level >= 10) level = 10;

        data.putInt("spell_xp", xp);
        data.putInt("spell_level", level);

        // Write back to wand
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

    // ── Element Resonance ──

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

    // ═══════════════════════════════════════════
    //  Static helper — mana cost from raw slot NBT
    // ═══════════════════════════════════════════

    public static int getManaCostFromTag(CompoundTag slotTag) {
        if (slotTag == null || !slotTag.contains("element")) return 0;
        SpellElement element = SpellElement.getById(slotTag.getString("element"));
        int cost = element.getManaCost();
        String effectId = slotTag.getString("effect");
        SpellEffect effect = SpellEffect.getById(effectId);
        if (effect != null) {
            cost += effect.getExtraManaCost();
        }
        return cost;
    }

    private static boolean canHitTarget(Entity entity, Entity owner) {
        return entity != null
                && entity != owner
                && entity.isAlive()
                && !EntityCompatUtil.isProtectedPlayer(entity);
    }

    // ═══════════════════════════════════════════
    //  Item property overrides
    // ═══════════════════════════════════════════

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

    // ═══════════════════════════════════════════
    //  use() — entry point for right-click
    // ═══════════════════════════════════════════

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
                    // FIX: creative 取出的空白 scroll 没有 carrier 字段，加载后 isSlotOccupied 仍 false
                    // 必须先校验 scroll 至少含 carrier，否则给出提示并拒绝
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
            // Normal right-click: start using (enables onUseTick for auto-fire)
            int selected = getSelectedSlot(stack);
            if (!isSlotOccupied(stack, selected)) {
                if (!level.isClientSide) {
                    player.displayClientMessage(
                            Component.translatable("msg.transcend.wand.no_spell")
                                    .withStyle(ChatFormatting.RED), true);
                }
                return InteractionResultHolder.fail(stack);
            }

            // Immediate first cast
            if (!level.isClientSide) {
                castSpell(level, player, stack);
            }

            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
    }

    // ═══════════════════════════════════════════
    //  onUseTick — auto-fire while held
    // ═══════════════════════════════════════════

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity,
                          @NotNull ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide) return;
        if (!(entity instanceof Player player)) return;

        int selected = getSelectedSlot(stack);
        if (!isSlotOccupied(stack, selected)) return;

        // Only fire when per-slot cooldown has reached 0
        if (getSlotCooldown(stack, selected) <= 0) {
            castSpell(level, player, stack);
        }
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level,
                             @NotNull LivingEntity entity, int timeCharged) {
        // No special action needed — auto-fire simply stops
    }

    // ═══════════════════════════════════════════
    //  inventoryTick — tick down per-slot cooldowns
    // ═══════════════════════════════════════════

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level,
                              @NotNull Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) return;
        ensureWandNBT(stack);

        int maxSl = getMaxSlots(stack);
        CompoundTag tag = stack.getOrCreateTag();
        for (int i = 0; i < maxSl; i++) {
            String key = slotCdKey(i);
            int cd = tag.getInt(key);
            if (cd > 0) {
                tag.putInt(key, cd - 1);
            }
        }
    }

    // ═══════════════════════════════════════════
    //  castSpell — the main casting routine
    // ═══════════════════════════════════════════

    private void castSpell(Level level, Player player, ItemStack wandStack) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        int selected = getSelectedSlot(wandStack);
        CompoundTag slotData = getSlotData(wandStack, selected);
        if (!slotData.contains("carrier")) return;

        // Read spell components from slot NBT
        SpellCarrier carrier = SpellCarrier.getById(slotData.getString("carrier"));
        SpellElement element = SpellElement.getById(slotData.getString("element"));
        SpellEffect effect = SpellEffect.getById(slotData.getString("effect"));
        float basePower = slotData.contains("base_power") ? slotData.getFloat("base_power") : 1.0F;
        float baseCooldownMult = slotData.contains("base_cooldown") ? slotData.getFloat("base_cooldown") : 1.0F;

        int wandUpgrade = wandStack.getOrCreateTag().getInt("wand_upgrade_level");
        // v6 CD 重设计：法杖升级带来的速度加成大幅收敛（0.06→0.02 / 级），满级 10 也只
        // 带来 20% CD 缩减。这样升级法杖不再独立把 CD 推到 4-tick floor，floor 需要靠
        // 飞升 CDR（最高 0.75）才能稳定触发 — 满足"满飞升才接近机关枪"的设计目标。
        float wandSpeedBonus = 1.0F - wandUpgrade * 0.02F;
        float wandEfficiency = Math.max(0.5F, 1.0F - wandUpgrade * 0.05F);

        String runeId = wandStack.getOrCreateTag().getString("wand_rune");
        WandRune rune = WandRune.getById(runeId);

        float armorCostReduce = com.huige233.transcend.items.armor.ElementArmor.getCostReduction(player, element);
        // 飞升专精魔力折扣
        com.huige233.transcend.ascension.PlayerAscensionData ascData =
                com.huige233.transcend.ascension.AscensionCapability.get(player);
        float masteryDiscount = ascData.getManaCostReduction(element);
        int manaCost = Math.max(1, (int)(getManaCostFromTag(slotData) * wandEfficiency
                * (1.0F - armorCostReduce) * (1.0F - masteryDiscount)));
        if (rune == WandRune.CONSERVATION) manaCost = Math.max(1, manaCost - 1);
        if (rune == WandRune.OVERCHARGE) manaCost = Math.max(1, (int)(manaCost * 1.2F));

        // ManaFreeCast passive check
        java.util.List<com.huige233.transcend.ascension.tree.PassiveEffect> passives =
                com.huige233.transcend.ascension.tree.TreeRegistry.getInstance()
                .getActivePassives(ascData.getUnlockedNodes());
        boolean manaFree = false;
        for (var p : passives) {
            if (p instanceof com.huige233.transcend.ascension.tree.PassiveEffect.ManaFreeCast mfc) {
                if (player.getRandom().nextFloat() < mfc.chance()) { manaFree = true; break; }
            }
        }
        if (manaFree) manaCost = 0;

        // ── SPELL_ECHO 上一次施法触发的"自由跟发"：本次免魔力 ─────────────
        boolean echoFree = wandStack.getOrCreateTag().getBoolean("transcend_echo_pending");
        if (echoFree) {
            wandStack.getOrCreateTag().putBoolean("transcend_echo_pending", false);
            manaCost = 0;
        }

        if (!MagicCrystalHelper.hasEnoughMana(player, manaCost)) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.wand.no_mana", manaCost)
                            .withStyle(ChatFormatting.RED), true);
            return;
        }

        // v3 ascension refactor: mana_free_cast passive — random chance to skip mana cost.
        boolean freeCast = false;
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            freeCast = com.huige233.transcend.ascension.AscensionHandler.tryFreeCast(sp);
        }

        // Consume mana (unless this cast was rolled free)
        if (!freeCast) {
            MagicCrystalHelper.consumeMana(player, manaCost);
        } else {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.wand.free_cast")
                            .withStyle(ChatFormatting.AQUA), true);
        }

        // Level & resonance multipliers
        int spellLevel = getSpellLevel(wandStack, selected);
        float levelDmgMult = getLevelDamageMult(spellLevel);
        float levelCdMult = getLevelCdMult(spellLevel);

        float armorBoost = com.huige233.transcend.items.armor.ElementArmor.getElementBoost(player, element);

        // ── 飞升乘数（法术强度 + 专精加成）────────────────────────────────
        float ascensionMult = ascData.getSpellDamageMultiplier(element, player);

        float damage = element.getBaseDamage() * basePower * levelDmgMult * (1.0F + armorBoost) * ascensionMult;
        if (rune == WandRune.OVERCHARGE) damage *= 1.3F;
        if (rune == WandRune.GLASS_CANNON) damage *= 1.5F;

        // AMPLIFY effect: +50% damage
        if (effect == SpellEffect.AMPLIFY) damage *= 1.5F;

        // Apply carrier targeting + particles
        // For projectile carriers (ORB, ARROW), spawn SpellProjectile instead of direct damage
        boolean isProjectile = (carrier == SpellCarrier.ORB || carrier == SpellCarrier.ARROW
                || carrier == SpellCarrier.VORTEX || carrier == SpellCarrier.TRAP);
        if (isProjectile) {
            float speed = Math.max(0.5F, carrier.getProjectileSpeed() * 0.15F);
            // 把 levelDmgMult / armorBoost / 符文加成 烘进传给 projectile 的 basePower。
            // SpellProjectile.onHitEntity 之后会再补 ascData.getSpellDamageMultiplier (法术强度+精通)
            // 与暴击/穿甲/处决；不能在这里再乘 ascensionMult,否则会双计。
            float projBasePower = basePower * levelDmgMult * (1.0F + armorBoost);
            if (rune == WandRune.OVERCHARGE)   projBasePower *= 1.3F;
            if (rune == WandRune.GLASS_CANNON) projBasePower *= 1.5F;
            // MULTISHOT: fire 3 projectiles in a spread
            int shotCount = (effect == SpellEffect.MULTISHOT) ? 3 : 1;
            for (int shot = 0; shot < shotCount; shot++) {
                SpellProjectile proj = new SpellProjectile(level, player, carrier, element,
                        effect == SpellEffect.MULTISHOT ? null : effect, projBasePower);
                float yawOffset = (shotCount > 1) ? (shot - 1) * 7.5F : 0;
                proj.shootFromRotation(player, player.getXRot(), player.getYRot() + yawOffset, 0.0F, speed, 1.0F);
                level.addFreshEntity(proj);
            }
        } else {
            applyCarrier(serverLevel, player, carrier, element, damage, effect, basePower);

            // Apply effect modifier (only for non-projectile carriers;
            // projectile carriers handle effects via SpellProjectile on hit)
            if (effect != null) {
                applyEffect(serverLevel, player, effect, damage);
            }
        }

        // Grant spell XP for casting
        addSpellXp(wandStack, selected, 1);

        // ── 飞升施法计数 ────────────────────────────────────────────────────
        com.huige233.transcend.ascension.AscensionHandler.recordCast(player);

        // ── 飞升CDR ─────────────────────────────────────────────────────────
        // v3 改读 attribute（包含装备 / curio / 3rd-party modifier）
        float ascCdr = com.huige233.transcend.ascension.AscensionHandler.getCDR(player);

        int cooldownTicks = Math.max(4, (int) (carrier.getBaseCooldown() * baseCooldownMult * wandSpeedBonus * levelCdMult * (1.0f - ascCdr)));
        if (rune == WandRune.RAPID_FIRE) cooldownTicks = Math.max(2, (int)(cooldownTicks * 0.6F));
        // QUICKCAST effect: -40% cooldown
        if (effect == SpellEffect.QUICKCAST) cooldownTicks = Math.max(2, (int)(cooldownTicks * 0.6F));
        if (rune == WandRune.MANA_SIPHON && player.getHealth() < player.getMaxHealth()) {
            player.heal(damage * 0.1F);
        }
        if (rune == WandRune.GLASS_CANNON) {
            player.hurt(player.damageSources().magic(), 1.0F);
        }
        setSlotCooldown(wandStack, selected, cooldownTicks);

        // SPELL_ECHO rune: 25% chance — 立刻清 CD 且下一发免魔力（真正的"回响"）
        if (rune == WandRune.SPELL_ECHO && serverLevel.getRandom().nextFloat() < 0.25F) {
            setSlotCooldown(wandStack, selected, 0);
            wandStack.getOrCreateTag().putBoolean("transcend_echo_pending", true);
        }

        // CHAIN_CASTER rune: auto-switch to next occupied slot after casting
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

        // ELEMENTAL_MASTERY rune: mark player for boosted reaction damage
        if (rune == WandRune.ELEMENTAL_MASTERY) {
            player.getPersistentData().putInt("transcend_elemental_mastery", 60);
        }

        // Play element-specific sound
        playElementSound(serverLevel, player, element);

        // Action bar cycling info
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

    // ═══════════════════════════════════════════
    //  Carrier-based targeting with particles
    // ═══════════════════════════════════════════

    private void applyCarrier(ServerLevel level, Player player, SpellCarrier carrier,
                              SpellElement element, float damage, SpellEffect effect, float basePower) {
        switch (carrier) {
            case ORB -> castOrb(level, player, carrier, element, damage);
            case ARROW -> castArrow(level, player, element, damage);
            case SLASH -> castSlash(level, player, element, damage);
            case BEAM -> castBeam(level, player, element, damage);
            case NOVA -> castNova(level, player, carrier, element, damage);
            case BREATH -> castBreath(level, player, element, damage);
            case RAIN -> castRain(level, player, carrier, element, damage, basePower, effect);
            case DASH -> castDash(level, player, element, damage);
            case GROUND -> castGround(level, player, carrier, element, damage);
            case CHAIN -> castChain(level, player, element, damage);
            case SPIKE -> castSpike(level, player, element, damage);
            case TELEPORT -> castTeleport(level, player, element, damage);
            case BARRIER -> castBarrier(level, player, element, damage);
            case SUMMON -> castSummon(level, player, element, basePower);
            case RING -> castRing(level, player, carrier, element, damage);
            default -> {}
        }
    }

    private void castOrb(ServerLevel level, Player player, SpellCarrier carrier,
                         SpellElement element, float damage) {
        HitResult hit = player.pick(20, 0, false);
        Vec3 pos = hit.getLocation();
        double r = carrier.getAoeRadius();

        // Particle: colored circle at target position
        List<S2CParticleBatchPack.ParticleEntry> entries =
                MagicCircleGeometry.buildCircle(pos.x, pos.y + 0.1, pos.z, r, 20, 0,
                        new Vector3f(0, 1, 0));
        sendParticles(level, pos, entries, element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.8F, 15);
        spawnVanillaFlairAOE(level, pos, element, r);

        // AOE damage
        AABB area = new AABB(pos.x - r, pos.y - r, pos.z - r,
                pos.x + r, pos.y + r, pos.z + r);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> canHitTarget(e, player));
        for (LivingEntity target : targets) {
            applyElementDamage(level, player, target, element, damage);
        }
    }

    private void castArrow(ServerLevel level, Player player, SpellElement element, float damage) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 endPos = eyePos.add(look.scale(20));

        // Particle: line from player to target
        List<S2CParticleBatchPack.ParticleEntry> entries =
                MagicCircleGeometry.buildLine(eyePos.x, eyePos.y, eyePos.z,
                        endPos.x, endPos.y, endPos.z, 15);
        sendParticles(level, eyePos.add(endPos).scale(0.5), entries,
                element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.5F, 10);

        // Find nearest entity along line of sight
        AABB searchBox = new AABB(eyePos, endPos).inflate(1.5);
        List<Entity> candidates = level.getEntities(player, searchBox,
                e -> canHitTarget(e, player) && (e instanceof LivingEntity || EntityCompatUtil.isGoetyObsidianMonolith(e)));
        Entity nearest = candidates.stream()
                .min(Comparator
                        .comparingInt((Entity e) -> EntityCompatUtil.isGoetyObsidianMonolith(e) ? 0 : 1)
                        .thenComparingDouble(e -> e.distanceToSqr(eyePos)))
                .orElse(null);

        if (nearest instanceof LivingEntity living) {
            applyElementDamage(level, player, living, element, damage);
        } else if (nearest != null && EntityCompatUtil.isGoetyObsidianMonolith(nearest)) {
            nearest.hurt(player.damageSources().playerAttack(player), damage);
        }
    }

    private void castSlash(ServerLevel level, Player player, SpellElement element, float damage) {
        Vec3 playerPos = player.position();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 eyePos = player.getEyePosition();

        // Shader slash arc
        com.huige233.transcend.client.renderer.ShaderSpellRenderer.addSpellEffect(
                eyePos, eyePos.add(look), element.getParticleR(), element.getParticleG(), element.getParticleB(), 6, "slash");

        // Particle: fan-shaped arc in front of player (120 degrees, 12 particles)
        List<S2CParticleBatchPack.ParticleEntry> entries = new ArrayList<>();
        double baseAngle = Math.atan2(look.z, look.x);
        for (int i = 0; i < 12; i++) {
            double angle = baseAngle - Math.toRadians(60) + Math.toRadians(120) * i / 11.0;
            double px = eyePos.x + 3.0 * Math.cos(angle);
            double pz = eyePos.z + 3.0 * Math.sin(angle);
            entries.add(new S2CParticleBatchPack.ParticleEntry(px, eyePos.y, pz, 0.0F, 0.0F, 0.0F));
        }
        sendParticles(level, eyePos, entries, element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.7F, 12);

        // Second particle layer: inner arc
        List<S2CParticleBatchPack.ParticleEntry> innerEntries = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double angle = baseAngle - Math.toRadians(45) + Math.toRadians(90) * i / 7.0;
            double px = eyePos.x + 2.0 * Math.cos(angle);
            double pz = eyePos.z + 2.0 * Math.sin(angle);
            innerEntries.add(new S2CParticleBatchPack.ParticleEntry(px, eyePos.y - 0.3F, pz, 0.0F, 0.0F, 0.0F));
        }
        sendParticles(level, eyePos, innerEntries, element.getParticleR() * 0.7F, element.getParticleG() * 0.7F, element.getParticleB() * 0.7F, 0.5F, 8);
        spawnVanillaFlairAOE(level, eyePos.add(look.scale(1.5)), element, 2.0);

        // Damage entities in 3-block 120-degree cone
        AABB area = player.getBoundingBox().inflate(3);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> canHitTarget(e, player));

        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().subtract(playerPos).normalize();
            double dot = look.x * toTarget.x + look.z * toTarget.z;
            // 120-degree cone: cos(60) = 0.5
            if (dot >= 0.5) {
                applyElementDamage(level, player, target, element, damage);
            }
        }
    }

    private void castBeam(ServerLevel level, Player player, SpellElement element, float damage) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 endPos = eyePos.add(look.scale(30));

        // Shader beam effect
        com.huige233.transcend.client.renderer.ShaderSpellRenderer.addSpellEffect(
                eyePos, endPos, element.getParticleR(), element.getParticleG(), element.getParticleB(), 8, "beam");

        // Particle: line from eyes to 30 blocks out
        List<S2CParticleBatchPack.ParticleEntry> entries =
                MagicCircleGeometry.buildLine(eyePos.x, eyePos.y, eyePos.z,
                        endPos.x, endPos.y, endPos.z, 20);
        sendParticles(level, eyePos.add(endPos).scale(0.5), entries,
                element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.6F, 12);
        spawnVanillaFlairAOE(level, endPos, element, 1.0);

        // Raycast: check in 0.5-block steps, damage first entity hit
        for (double d = 0.5; d <= 30.0; d += 0.5) {
            Vec3 checkPos = eyePos.add(look.scale(d));
            AABB checkBox = new AABB(checkPos.x - 0.5, checkPos.y - 0.5, checkPos.z - 0.5,
                    checkPos.x + 0.5, checkPos.y + 0.5, checkPos.z + 0.5);
            List<Entity> hits = level.getEntities(player, checkBox,
                    e -> canHitTarget(e, player) && (e instanceof LivingEntity || EntityCompatUtil.isGoetyObsidianMonolith(e)));
            Entity first = hits.stream()
                    .min(Comparator
                            .comparingInt((Entity e) -> EntityCompatUtil.isGoetyObsidianMonolith(e) ? 0 : 1)
                            .thenComparingDouble(e -> e.distanceToSqr(eyePos)))
                    .orElse(null);
            if (first instanceof LivingEntity living) {
                applyElementDamage(level, player, living, element, damage);
                return;
            } else if (first != null && EntityCompatUtil.isGoetyObsidianMonolith(first)) {
                first.hurt(player.damageSources().playerAttack(player), damage);
                return;
            }
        }
    }

    private void castNova(ServerLevel level, Player player, SpellCarrier carrier,
                          SpellElement element, float damage) {
        Vec3 pos = player.position();
        double r = carrier.getAoeRadius();

        // Shader expanding nova ring
        com.huige233.transcend.client.renderer.ShaderSpellRenderer.addSpellEffect(
                pos, pos, element.getParticleR(), element.getParticleG(), element.getParticleB(), 10, "nova");

        // Particle: ring expanding from player
        List<S2CParticleBatchPack.ParticleEntry> entries =
                MagicCircleGeometry.buildCircle(pos.x, pos.y + 0.5, pos.z, 5.0, 30, 0,
                        new Vector3f(0, 1, 0));
        sendParticles(level, pos, entries, element.getParticleR(), element.getParticleG(), element.getParticleB(), 1.0F, 18);

        // Expanding ring
        List<S2CParticleBatchPack.ParticleEntry> ring2 = MagicCircleGeometry.buildCircle(
                pos.x, pos.y + 0.5, pos.z, r * 0.6, 16, Math.PI / 6, new Vector3f(0, 1, 0));
        sendParticles(level, pos, ring2, element.getParticleR() * 0.6F, element.getParticleG() * 0.6F, element.getParticleB() * 0.6F, 0.6F, 10);
        spawnVanillaFlairAOE(level, pos, element, r);

        // AOE damage
        AABB area = player.getBoundingBox().inflate(r);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> canHitTarget(e, player));
        for (LivingEntity target : targets) {
            applyElementDamage(level, player, target, element, damage);
        }
    }

    private void castBreath(ServerLevel level, Player player, SpellElement element, float damage) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        // 5-block cone in front of player — collect entities in a forward AABB
        Vec3 coneCenter = eyePos.add(look.scale(2.5));
        AABB coneBox = new AABB(
                coneCenter.x - 2.5, coneCenter.y - 1.0, coneCenter.z - 2.5,
                coneCenter.x + 2.5, coneCenter.y + 1.0, coneCenter.z + 2.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, coneBox,
                e -> canHitTarget(e, player));
        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().subtract(player.position()).normalize();
            double dot = look.x * toTarget.x + look.z * toTarget.z;
            if (dot >= 0.5) { // ~120-degree cone
                applyElementDamage(level, player, target, element, damage);
            }
        }

        // Breath cone particles
        for (int i = 1; i <= 5; i++) {
            double dist = i * 1.0;
            double spread = dist * 0.4;
            Vec3 point = eyePos.add(look.scale(dist));
            List<S2CParticleBatchPack.ParticleEntry> cone = MagicCircleGeometry.buildCircle(
                    point.x, point.y, point.z, spread, 6, i * 0.5, new Vector3f(0, 1, 0));
            sendParticles(level, point, cone, element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.5F, 6);
        }
        spawnVanillaFlairAOE(level, eyePos.add(look.scale(2.5)), element, 2.0);
    }

    private void castRain(ServerLevel level, Player player, SpellCarrier carrier,
                          SpellElement element, float damage, float basePower, SpellEffect effect) {
        HitResult hit = player.pick(20, 0, false);
        Vec3 target = hit.getLocation();
        for (int i = 0; i < 8; i++) {
            double ox = (level.getRandom().nextDouble() - 0.5) * carrier.getAoeRadius() * 2;
            double oz = (level.getRandom().nextDouble() - 0.5) * carrier.getAoeRadius() * 2;
            SpellProjectile proj = new SpellProjectile(level, player, carrier, element, effect, basePower);
            proj.setPos(target.x + ox, target.y + 12.0, target.z + oz);
            proj.setDeltaMovement(0, -0.8, 0);
            level.addFreshEntity(proj);
        }
    }

    private void castDash(ServerLevel level, Player player, SpellElement element, float damage) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 dashTarget = player.position().add(look.scale(4.0));
        // Damage entities along the path
        AABB pathBox = new AABB(
                Math.min(player.getX(), dashTarget.x) - 0.5,
                player.getY() - 0.5,
                Math.min(player.getZ(), dashTarget.z) - 0.5,
                Math.max(player.getX(), dashTarget.x) + 0.5,
                player.getY() + 2.0,
                Math.max(player.getZ(), dashTarget.z) + 0.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, pathBox,
                e -> canHitTarget(e, player));
        for (LivingEntity target : targets) {
            applyElementDamage(level, player, target, element, damage);
        }
        // Teleport player forward
        Vec3 oldPos = player.position();
        player.teleportTo(dashTarget.x, player.getY(), dashTarget.z);

        // Dash trail
        for (int i = 0; i < 8; i++) {
            double t = i / 8.0;
            Vec3 trailPos = oldPos.add(dashTarget.subtract(oldPos).scale(t));
            List<S2CParticleBatchPack.ParticleEntry> trailRing = MagicCircleGeometry.buildCircle(
                    trailPos.x, trailPos.y + 0.5, trailPos.z, 0.3, 6, i * 0.8, new Vector3f(0, 1, 0));
            sendParticles(level, trailPos, trailRing, element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.4F, 6);
        }
        spawnVanillaFlairAOE(level, dashTarget, element, 1.5);
    }

    private void castGround(ServerLevel level, Player player, SpellCarrier carrier,
                            SpellElement element, float damage) {
        Vec3 pos = player.position();
        double r = carrier.getAoeRadius();

        // Particle: ring at ground level
        List<S2CParticleBatchPack.ParticleEntry> entries =
                MagicCircleGeometry.buildCircle(pos.x, pos.y + 0.1, pos.z, r, 24, 0,
                        new Vector3f(0, 1, 0));
        sendParticles(level, pos, entries, element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.9F, 16);

        // Vertical spike particles
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2 * i / 6;
            double sx = pos.x + r * 0.7 * Math.cos(angle);
            double sz = pos.z + r * 0.7 * Math.sin(angle);
            List<S2CParticleBatchPack.ParticleEntry> spike = MagicCircleGeometry.buildLine(
                    sx, pos.y, sz, sx, pos.y + 1.5, sz, 5);
            sendParticles(level, new Vec3(sx, pos.y + 0.75, sz), spike, element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.7F, 10);
        }
        spawnVanillaFlairAOE(level, pos, element, r);

        // AOE damage at feet level
        AABB area = new AABB(pos.x - r, pos.y - 0.5, pos.z - r,
                pos.x + r, pos.y + 1.5, pos.z + r);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> canHitTarget(e, player));
        for (LivingEntity target : targets) {
            applyElementDamage(level, player, target, element, damage);
        }
    }

    private static float getBasePowerFromWand(Player player) {
        return 1.0F;
    }

    private void castChain(ServerLevel level, Player player, SpellElement element, float damage) {
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
        if (current == null) return;

        int bounces = 4;
        LivingEntity prev = null;
        for (int i = 0; i < bounces && current != null; i++) {
            applyElementDamage(level, player, current, element, damage * (1.0F - i * 0.15F));
            List<S2CParticleBatchPack.ParticleEntry> line = MagicCircleGeometry.buildLine(
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
    }

    private void castSpike(ServerLevel level, Player player, SpellElement element, float damage) {
        HitResult hit = player.pick(16, 0, false);
        Vec3 target = hit.getLocation();
        AABB area = new AABB(target.x - 1.5, target.y - 0.5, target.z - 1.5,
                target.x + 1.5, target.y + 3.0, target.z + 1.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> canHitTarget(e, player));
        for (LivingEntity t : targets) {
            applyElementDamage(level, player, t, element, damage);
            t.setDeltaMovement(t.getDeltaMovement().add(0, 0.5, 0));
        }
        List<S2CParticleBatchPack.ParticleEntry> spikes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            double px = target.x + Math.cos(angle) * 1.0;
            double pz = target.z + Math.sin(angle) * 1.0;
            spikes.addAll(MagicCircleGeometry.buildLine(px, target.y, pz, px, target.y + 2.0, pz, 6));
        }
        sendParticles(level, target, spikes, element.getParticleR(), element.getParticleG(), element.getParticleB(), 1.0F, 10);
    }

    private void castTeleport(ServerLevel level, Player player, SpellElement element, float damage) {
        HitResult hit = player.pick(30, 0, false);
        Vec3 target = hit.getLocation();
        Vec3 oldPos = player.position();
        player.teleportTo(target.x, target.y, target.z);
        AABB area = new AABB(target.x - 3, target.y - 1, target.z - 3,
                target.x + 3, target.y + 3, target.z + 3);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> canHitTarget(e, player));
        for (LivingEntity t : targets) {
            applyElementDamage(level, player, t, element, damage);
        }
        List<S2CParticleBatchPack.ParticleEntry> ring = MagicCircleGeometry.buildCircle(
                target.x, target.y + 0.1, target.z, 3.0, 24, 0, new Vector3f(0, 1, 0));
        sendParticles(level, target, ring, element.getParticleR(), element.getParticleG(), element.getParticleB(), 1.0F, 8);
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

    private void castSummon(ServerLevel level, Player player, SpellElement element, float basePower) {
        HitResult hit = player.pick(10, 0, false);
        Vec3 target = hit.getLocation();

        if (basePower >= 2.0F) {
            com.huige233.transcend.entity.SpellGuardian guardian = new com.huige233.transcend.entity.SpellGuardian(
                    com.huige233.transcend.init.ModEntities.SPELL_GUARDIAN.get(), level);
            guardian.setPos(target.x, target.y, target.z);
            guardian.setOwner(player);
            level.addFreshEntity(guardian);
        }

        int wispCount = basePower >= 3.0F ? 2 : 1;
        for (int i = 0; i < wispCount; i++) {
            com.huige233.transcend.entity.SpellWisp wisp = new com.huige233.transcend.entity.SpellWisp(
                    com.huige233.transcend.init.ModEntities.SPELL_WISP.get(), level);
            double ox = (i - 0.5) * 2;
            wisp.setPos(target.x + ox, target.y + 1, target.z);
            wisp.setOwner(player);
            level.addFreshEntity(wisp);
        }
    }

    private void castRing(ServerLevel level, Player player, SpellCarrier carrier,
                          SpellElement element, float damage) {
        double r = carrier.getAoeRadius();
        AABB area = player.getBoundingBox().inflate(r);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> canHitTarget(e, player));
        for (LivingEntity t : targets) {
            double dx = t.getX() - player.getX();
            double dz = t.getZ() - player.getZ();
            if (Math.sqrt(dx * dx + dz * dz) <= r) {
                applyElementDamage(level, player, t, element, damage * 0.5F);
            }
        }
        List<S2CParticleBatchPack.ParticleEntry> ring = MagicCircleGeometry.buildCircle(
                player.getX(), player.getY() + 0.5, player.getZ(), r, 36, 0, new Vector3f(0, 1, 0));
        sendParticles(level, player.position(), ring, element.getParticleR(), element.getParticleG(), element.getParticleB(), 0.9F, 12);
    }

    // ═══════════════════════════════════════════
    //  Element-based damage + secondary effects
    // ═══════════════════════════════════════════

    private net.minecraft.world.damagesource.DamageSource getElementDamageSource(Player player,
                                                                                  LivingEntity target,
                                                                                  SpellElement element) {
        if (EntityCompatUtil.isBotaniaGaiaGuardian(target)) {
            return player.damageSources().playerAttack(player);
        }
        return switch (element) {
            case THUNDER -> player.damageSources().lightningBolt();
            case VOID, SONIC, ELDRITCH -> player.damageSources().indirectMagic(player, player);
            default -> player.damageSources().playerAttack(player);
        };
    }

    private void applyElementDamage(ServerLevel level, Player player,
                                    LivingEntity target, SpellElement element, float damage) {
        net.minecraft.world.damagesource.DamageSource damageSource = getElementDamageSource(player, target, element);
        switch (element) {
            case FIRE -> {
                target.hurt(damageSource, damage);
                target.setSecondsOnFire(4);
            }
            case ICE -> {
                target.hurt(damageSource, damage);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true));
                target.setTicksFrozen(target.getTicksFrozen() + 100);
            }
            case THUNDER -> {
                target.hurt(damageSource, damage);
                RainbowLightning bolt = new RainbowLightning(level,
                        target.getX(), target.getY(), target.getZ());
                level.addFreshEntity(bolt);
            }
            case WIND -> {
                target.hurt(damageSource, damage);
                Vec3 knockDir = target.position().subtract(player.position()).normalize();
                target.knockback(2.0F, -knockDir.x, -knockDir.z);
                target.hurtMarked = true;
            }
            case EARTH -> {
                target.hurt(damageSource, damage);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3, false, true));
            }
            case VOID -> {
                target.hurt(damageSource, damage);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1, false, true));
            }
            case HOLY -> {
                if (target instanceof Monster) {
                    target.hurt(damageSource, damage * 1.5F);
                } else if (target instanceof Player targetPlayer) {
                    targetPlayer.heal(damage);
                } else {
                    target.hurt(damageSource, damage);
                }
            }
            case BLOOD -> {
                target.hurt(damageSource, damage);
                target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 1, false, true));
                player.heal(1.0F);
            }
            case DARK -> {
                target.hurt(damageSource, damage);
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, true));
            }
            case LIGHT -> {
                float lightDmg = (target instanceof Monster) ? damage * 1.5F : damage;
                target.hurt(damageSource, lightDmg);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, true));
            }
            case POISON -> {
                target.hurt(damageSource, damage);
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1, false, true));
            }
            case TIME -> {
                target.hurt(damageSource, damage);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3, false, true));
            }
            case SPACE -> {
                target.hurt(damageSource, damage);
                double angle = level.getRandom().nextDouble() * Math.PI * 2;
                double dx = Math.cos(angle) * 5.0;
                double dz = Math.sin(angle) * 5.0;
                target.teleportTo(target.getX() + dx, target.getY(), target.getZ() + dz);
            }
            case NATURE -> {
                target.hurt(damageSource, damage);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true));
            }
            case CHAOS -> {
                target.hurt(damageSource, damage);
                int roll = level.getRandom().nextInt(7);
                switch (roll) {
                    case 0 -> target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 1, false, true));
                    case 1 -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, true));
                    case 2 -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, true));
                    case 3 -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1, false, true));
                    case 4 -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3, false, true));
                    case 5 -> {
                        double a = level.getRandom().nextDouble() * Math.PI * 2;
                        target.teleportTo(target.getX() + Math.cos(a) * 5.0, target.getY(), target.getZ() + Math.sin(a) * 5.0);
                    }
                    case 6 -> player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true));
                }
            }
            case ACID -> {
                target.hurt(damageSource, damage);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, true));
                var armorAttr = target.getAttribute(Attributes.ARMOR);
                if (armorAttr != null) {
                    armorAttr.removeModifier(ACID_ARMOR_UUID);
                    armorAttr.addTransientModifier(new AttributeModifier(
                            ACID_ARMOR_UUID,
                            "spell_acid", -4.0, AttributeModifier.Operation.ADDITION));
                }
            }
            case SONIC -> {
                target.hurt(damageSource, damage);
                if (level.getRandom().nextFloat() < 0.5F) {
                    target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 1, false, true));
                }
            }
            case ELDRITCH -> {
                target.hurt(damageSource, damage);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 2, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 1, false, true));
            }
        }
        ElementReaction.spawnHitFlash(level, target, element);
        ElementReaction.tryReaction(target, element, damage, player);
    }

    // ═══════════════════════════════════════════
    //  Effect modifiers
    // ═══════════════════════════════════════════

    private void applyEffect(ServerLevel level, Player player, SpellEffect effect, float damage) {
        switch (effect) {
            case EXPLOSION -> {
                HitResult hit = player.pick(20, 0, false);
                Vec3 pos = hit.getLocation();
                level.explode(player, pos.x, pos.y, pos.z, 2.0F,
                        Level.ExplosionInteraction.NONE);
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
                    extra.hurt(player.damageSources().playerAttack(player), damage * 0.5F);
                }
            }
            case SPLIT -> {
                // Reserved for future use
            }
            case HOMING -> {
                // Reserved for future use
            }
            case HEALING -> {
                player.heal(damage * 0.5F);
            }
            case SHIELD -> {
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1, false, true));
            }
            case CHAIN_LIGHTNING, BOUNCE, DELAYED, AMPLIFY, LIFESTEAL,
                 QUICKCAST, MULTISHOT, SLOWFIELD, GRAVITY_WELL, MARK -> {
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
                            effectTargets.get(i).hurt(player.damageSources().playerAttack(player), damage * 0.6F);
                        }
                    }
                    case AMPLIFY -> {
                        for (LivingEntity t : effectTargets) {
                            t.hurt(player.damageSources().playerAttack(player), damage * 0.5F);
                        }
                    }
                    case LIFESTEAL -> {
                        float healed = damage * 0.3F;
                        player.heal(healed);
                    }
                    case SLOWFIELD -> {
                        for (LivingEntity t : effectTargets) {
                            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true));
                        }
                    }
                    case GRAVITY_WELL -> {
                        for (LivingEntity t : effectTargets) {
                            Vec3 pull = effectPos.subtract(t.position()).normalize().scale(0.5);
                            t.setDeltaMovement(t.getDeltaMovement().add(pull));
                            t.hurtMarked = true;
                        }
                    }
                    case MARK -> {
                        for (LivingEntity t : effectTargets) {
                            t.getPersistentData().putInt("transcend_mark", 100);
                        }
                    }
                    default -> {}
                }
            }
            case ECHO -> {
                // Immediate impact echo: deal 50% damage to all entities within 2 blocks
                Vec3 eyePos = player.getEyePosition();
                HitResult echoHit = player.pick(20, 0, false);
                Vec3 echoPos = echoHit.getLocation();
                AABB echoBox = new AABB(echoPos.x - 2, echoPos.y - 2, echoPos.z - 2,
                        echoPos.x + 2, echoPos.y + 2, echoPos.z + 2);
                level.getEntitiesOfClass(LivingEntity.class, echoBox, e -> canHitTarget(e, player))
                        .forEach(e -> e.hurt(player.damageSources().playerAttack(player), damage * 0.5F));
            }
            case ARMOR_BREAK -> {
                HitResult hit = player.pick(20, 0, false);
                Vec3 pos = hit.getLocation();
                AABB box = new AABB(pos.x - 1.5, pos.y - 1.5, pos.z - 1.5,
                        pos.x + 1.5, pos.y + 1.5, pos.z + 1.5);
                level.getEntitiesOfClass(LivingEntity.class, box, e -> canHitTarget(e, player))
                        .forEach(e -> {
                            var attr = e.getAttribute(Attributes.ARMOR);
                            if (attr != null) {
                                attr.removeModifier(ARMOR_BREAK_UUID);
                                attr.addTransientModifier(new AttributeModifier(
                                        ARMOR_BREAK_UUID,
                                        "spell_armor_break", -8.0, AttributeModifier.Operation.ADDITION));
                                e.getPersistentData().putInt("transcend_armor_break", 120);
                            }
                        });
            }
            case ROOT -> {
                HitResult hit = player.pick(20, 0, false);
                Vec3 pos = hit.getLocation();
                AABB box = new AABB(pos.x - 1.5, pos.y - 1.5, pos.z - 1.5,
                        pos.x + 1.5, pos.y + 1.5, pos.z + 1.5);
                level.getEntitiesOfClass(LivingEntity.class, box, e -> canHitTarget(e, player))
                        .forEach(e -> {
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4, false, true));
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
                        });
            }
            case LINGERING -> {
                HitResult hit = player.pick(20, 0, false);
                Vec3 pos = hit.getLocation();
                AABB box = new AABB(pos.x - 2, pos.y - 2, pos.z - 2,
                        pos.x + 2, pos.y + 2, pos.z + 2);
                level.getEntitiesOfClass(LivingEntity.class, box, e -> canHitTarget(e, player))
                        .forEach(e -> e.hurt(player.damageSources().playerAttack(player), 1.5F));
            }
            case DEVOUR -> {
                // Handled per-target; nothing to do at the carrier level
            }
            case ABSORB -> {
                // 30% of damage dealt becomes absorption hearts for caster
                HitResult absorbHit = player.pick(10, 0, false);
                Vec3 absorbPos = absorbHit.getLocation();
                AABB absorbBox = new AABB(absorbPos.x - 1.5, absorbPos.y - 1.5, absorbPos.z - 1.5,
                        absorbPos.x + 1.5, absorbPos.y + 1.5, absorbPos.z + 1.5);
                level.getEntitiesOfClass(LivingEntity.class, absorbBox, e -> canHitTarget(e, player))
                        .forEach(e -> {
                            float absorb = Math.min(e.getHealth(), damage) * 0.3F;
                            player.setAbsorptionAmount(player.getAbsorptionAmount() + absorb);
                        });
            }
            case REFLECT -> {
                // Target's next attack within 5 seconds is reflected back
                HitResult reflectHit = player.pick(10, 0, false);
                Vec3 reflectPos = reflectHit.getLocation();
                AABB reflectBox = new AABB(reflectPos.x - 1.5, reflectPos.y - 1.5, reflectPos.z - 1.5,
                        reflectPos.x + 1.5, reflectPos.y + 1.5, reflectPos.z + 1.5);
                level.getEntitiesOfClass(LivingEntity.class, reflectBox, e -> canHitTarget(e, player))
                        .forEach(e -> {
                            if (e instanceof Mob mob) {
                                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 4));
                            }
                        });
            }
            case CURSE -> {
                // Amplify all damage target receives by 50% for 8 seconds via Unluck
                HitResult curseHit = player.pick(10, 0, false);
                Vec3 cursePos = curseHit.getLocation();
                AABB curseBox = new AABB(cursePos.x - 1.5, cursePos.y - 1.5, cursePos.z - 1.5,
                        cursePos.x + 1.5, cursePos.y + 1.5, cursePos.z + 1.5);
                level.getEntitiesOfClass(LivingEntity.class, curseBox, e -> canHitTarget(e, player))
                        .forEach(e -> {
                            e.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 160, 4));
                            e.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                        });
            }
            case OVERLOAD -> {
                // Deal 2x damage but also hurt caster for 25%
                HitResult overloadHit = player.pick(10, 0, false);
                Vec3 overloadPos = overloadHit.getLocation();
                AABB overloadBox = new AABB(overloadPos.x - 1.5, overloadPos.y - 1.5, overloadPos.z - 1.5,
                        overloadPos.x + 1.5, overloadPos.y + 1.5, overloadPos.z + 1.5);
                level.getEntitiesOfClass(LivingEntity.class, overloadBox, e -> canHitTarget(e, player))
                        .forEach(e -> e.hurt(player.damageSources().playerAttack(player), damage));
                player.hurt(player.damageSources().magic(), damage * 0.25F);
            }
            case WEAKEN -> {
                // Reduce target's attack damage
                HitResult weakenHit = player.pick(10, 0, false);
                Vec3 weakenPos = weakenHit.getLocation();
                AABB weakenBox = new AABB(weakenPos.x - 1.5, weakenPos.y - 1.5, weakenPos.z - 1.5,
                        weakenPos.x + 1.5, weakenPos.y + 1.5, weakenPos.z + 1.5);
                level.getEntitiesOfClass(LivingEntity.class, weakenBox, e -> canHitTarget(e, player))
                        .forEach(e -> {
                            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                            e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1));
                        });
            }
            case UNSTABLE -> {
                float roll = level.getRandom().nextFloat();
                if (roll < 0.3F) {
                    HitResult unstableHit = player.pick(10, 0, false);
                    Vec3 unstablePos = unstableHit.getLocation();
                    AABB unstableBox = new AABB(unstablePos.x - 1.5, unstablePos.y - 1.5, unstablePos.z - 1.5,
                            unstablePos.x + 1.5, unstablePos.y + 1.5, unstablePos.z + 1.5);
                    level.getEntitiesOfClass(LivingEntity.class, unstableBox, e -> canHitTarget(e, player))
                            .forEach(e -> e.hurt(player.damageSources().playerAttack(player), damage * 2.0F));
                } else if (roll > 0.8F) {
                    player.hurt(player.damageSources().magic(), damage * 0.5F);
                }
            }
            case SHATTER -> {
                // Remove ALL armor from target for 6 seconds (via attribute modifier)
                HitResult shatterHit = player.pick(10, 0, false);
                Vec3 shatterPos = shatterHit.getLocation();
                AABB shatterBox = new AABB(shatterPos.x - 1.5, shatterPos.y - 1.5, shatterPos.z - 1.5,
                        shatterPos.x + 1.5, shatterPos.y + 1.5, shatterPos.z + 1.5);
                level.getEntitiesOfClass(LivingEntity.class, shatterBox, e -> canHitTarget(e, player))
                        .forEach(e -> {
                            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 3));
                            if (e instanceof Player targetPlayer) {
                                targetPlayer.getCooldowns().addCooldown(net.minecraft.world.item.Items.SHIELD, 120);
                            }
                        });
            }
            case SUMMON_WISP -> {
                // Spawn a SpellWisp at player's look target position
                HitResult wispHit = player.pick(10, 0, false);
                Vec3 wispPos = wispHit.getLocation();
                com.huige233.transcend.entity.SpellWisp wisp = new com.huige233.transcend.entity.SpellWisp(
                        com.huige233.transcend.init.ModEntities.SPELL_WISP.get(), level);
                wisp.setPos(wispPos.x, wispPos.y, wispPos.z);
                wisp.setOwner(player);
                level.addFreshEntity(wisp);
            }
            case SUMMON_GUARDIAN -> {
                // Spawn a SpellGuardian at player's look target position
                HitResult guardianHit = player.pick(10, 0, false);
                Vec3 guardianPos = guardianHit.getLocation();
                com.huige233.transcend.entity.SpellGuardian guardian = new com.huige233.transcend.entity.SpellGuardian(
                        com.huige233.transcend.init.ModEntities.SPELL_GUARDIAN.get(), level);
                guardian.setPos(guardianPos.x, guardianPos.y, guardianPos.z);
                guardian.setOwner(player);
                level.addFreshEntity(guardian);
            }
        }
    }

    // ═══════════════════════════════════════════
    //  Sound per element
    // ═══════════════════════════════════════════

    private void playElementSound(ServerLevel level, Player player, SpellElement element) {
        BlockPos pos = player.blockPosition();
        switch (element) {
            case FIRE -> level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            case ICE -> level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
            case THUNDER -> level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6F, 1.0F);
            case WIND -> level.playSound(null, pos, SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.0F, 1.0F);
            case EARTH -> level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5F, 0.8F);
            case VOID -> level.playSound(null, pos, SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.7F, 1.0F);
            case HOLY -> level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.5F);
            case BLOOD -> level.playSound(null, pos, SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.5F, 0.8F);
            case DARK -> level.playSound(null, pos, SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.3F, 0.5F);
            case LIGHT -> level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.5F);
            case POISON -> level.playSound(null, pos, SoundEvents.WITCH_DRINK, SoundSource.PLAYERS, 0.5F, 1.0F);
            case TIME -> level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.4F, 0.8F);
            case SPACE -> level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 1.2F);
            case NATURE -> level.playSound(null, pos, SoundEvents.BEEHIVE_EXIT, SoundSource.PLAYERS, 0.4F, 1.0F);
            case CHAOS -> level.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.2F, 1.5F);
            case ACID -> level.playSound(null, pos, SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 0.5F, 0.8F);
            case SONIC -> level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.2F, 2.0F);
            case ELDRITCH -> level.playSound(null, pos, SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.3F, 0.5F);
        }
    }

    // ═══════════════════════════════════════════
    //  Particle sending
    // ═══════════════════════════════════════════

    private void sendParticles(ServerLevel level, Vec3 center,
                               List<S2CParticleBatchPack.ParticleEntry> entries,
                               float r, float g, float b, float scale, int lifetime) {
        if (preferShaderWandFx()) {
            float radius = estimateBatchRadius(center, entries, Math.max(0.8F, scale * 2.5F));
            int shaderLifetime = Math.max(10, Math.min(28, lifetime));
            String pattern = entries.size() >= 20 ? "hexagram" : "pentagram";
            ShaderSpellRenderer.addCircle(center.add(0.0, 0.06, 0.0), radius, r, g, b, shaderLifetime, 24, pattern);
            if (entries.size() >= 12) {
                ShaderSpellRenderer.addShieldRipple(center.add(0.0, 0.45, 0.0), radius * 0.8F, r, g, b, shaderLifetime);
            }
            if (entries.size() >= 30) {
                ShaderSpellRenderer.addShockwave(center.add(0.0, 0.1, 0.0), radius * 1.25F, r, g, b, shaderLifetime + 6);
            }
            return;
        }

        S2CParticleBatchPack packet = new S2CParticleBatchPack(
                entries, new Vector3f(r, g, b), scale, lifetime, true);
        NetworkHandler.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        center.x, center.y, center.z, 64, level.dimension())),
                packet);
    }

    private void spawnVanillaFlairAOE(ServerLevel level, Vec3 pos, SpellElement element, double radius) {
        if (preferShaderWandFx()) {
            float rr = element.getParticleR();
            float gg = element.getParticleG();
            float bb = element.getParticleB();
            float rad = (float) Math.max(1.0, radius);
            ShaderSpellRenderer.addCircle(pos.add(0.0, 0.08, 0.0), rad, rr, gg, bb, 16, 24,
                    element == SpellElement.VOID || element == SpellElement.CHAOS || element == SpellElement.ELDRITCH
                            ? "pentagram" : "hexagram");
            ShaderSpellRenderer.addShieldRipple(pos.add(0.0, 0.45, 0.0), rad * 0.75F, rr, gg, bb, 12);
            ShaderSpellRenderer.addSpellEffect(pos.add(-rad * 0.6, 0.6, 0.0),
                    pos.add(rad * 0.6, 0.6, 0.0),
                    rr, gg, bb, 12, shaderTypeForElement(element));
            return;
        }

        int count = 8;
        switch (element) {
            case FIRE -> level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    pos.x, pos.y + 0.5, pos.z, count, radius * 0.5, 0.3, radius * 0.5, 0.02);
            case ICE -> level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                    pos.x, pos.y + 0.5, pos.z, count, radius * 0.5, 0.5, radius * 0.5, 0.01);
            case THUNDER -> level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y + 0.5, pos.z, count * 2, radius * 0.3, 0.5, radius * 0.3, 0.1);
            case VOID, DARK, ELDRITCH -> level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                    pos.x, pos.y + 0.5, pos.z, count, radius * 0.3, 0.5, radius * 0.3, 0.5);
            case HOLY, LIGHT -> level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    pos.x, pos.y + 0.5, pos.z, count, radius * 0.3, 0.5, radius * 0.3, 0.02);
            case POISON, ACID -> level.sendParticles(net.minecraft.core.particles.ParticleTypes.ITEM_SLIME,
                    pos.x, pos.y + 0.3, pos.z, count, radius * 0.4, 0.2, radius * 0.4, 0.01);
            case BLOOD -> level.sendParticles(net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR,
                    pos.x, pos.y + 0.5, pos.z, count / 2, radius * 0.3, 0.3, radius * 0.3, 0.02);
            case WIND -> level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    pos.x, pos.y + 0.3, pos.z, count, radius * 0.5, 0.3, radius * 0.5, 0.03);
            case CHAOS -> {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                        pos.x, pos.y + 0.5, pos.z, count, radius * 0.4, 0.5, radius * 0.4, 0.05);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                        pos.x, pos.y + 0.5, pos.z, count / 2, radius * 0.3, 0.3, radius * 0.3, 0.05);
            }
            default -> {}
        }
    }

    private boolean preferShaderWandFx() {
        return true;
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
            case THUNDER, SONIC -> "beam";
            case CHAOS, VOID, DARK, ELDRITCH -> "slash";
            default -> "nova";
        };
    }

    // ═══════════════════════════════════════════
    //  Tooltip
    // ═══════════════════════════════════════════

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        ensureWandNBT(stack);

        int maxSl = getMaxSlots(stack);
        int selected = getSelectedSlot(stack);

        // Wand tier
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

        // List all slots
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

                // Cooldown status
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

        // Mana cost of current slot
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

    // ═══════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════

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
