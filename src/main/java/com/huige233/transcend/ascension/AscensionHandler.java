package com.huige233.transcend.ascension;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.ModDamageTypes;
import com.huige233.transcend.ascension.tree.PassiveEffect;
import com.huige233.transcend.ascension.tree.TreeRegistry;
import com.huige233.transcend.world.nexus.NexusWorldPenalty;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AscensionHandler {

    private static final String UNDYING_COOLDOWN_KEY = "transcend_undying_cd";
    private static final String IMMORTALITY_SAVE_READY_KEY = "transcend_vow_immortality_save_ready";
    public static final long IMMORTALITY_SAVE_COOLDOWN_TICKS = 6000L;
    private static final Set<String> RESONANCE_REACTION_CATEGORIES = Set.of(
            "five_elements_generation", "five_elements_overcoming");

    private static final UUID UUID_HP    = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000001");
    private static final UUID UUID_SPEED = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000002");
    private static final UUID UUID_SPELL_POWER   = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000003");
    private static final UUID UUID_SPELL_RESIST  = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000004");
    private static final UUID UUID_MAX_MANA      = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000005");
    private static final UUID UUID_MANA_REGEN    = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000006");
    private static final UUID UUID_CRIT_CHANCE   = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000007");
    private static final UUID UUID_CDR           = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000008");
    private static final UUID UUID_DEVIATION_MAJOR = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000009");
    private static final UUID UUID_DEVIATION_MANA = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000010");
    private static final UUID UUID_REALM_DAMAGE = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000011");

    private static final long XP_KILL_NORMAL = 5;
    private static final long XP_KILL_BOSS   = 500;
    public  static final long XP_RITUAL      = 1000;

    private static long computeSoulGain(LivingEntity target, boolean isBoss) {
        if (isBoss) return 200L;
        float maxHp = target.getMaxHealth();
        if (maxHp >= 100f) return 50L;
        if (maxHp >= 30f) return 5L;
        return 1L;
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        LivingEntity target = event.getEntity();
        boolean isBoss = target.getMaxHealth() >= 200f;

        PlayerAscensionData data = AscensionCapability.get(player);

        if (target instanceof Enemy) {
            data.addCultivationXP(CultivationProgression.killXp(target.getMaxHealth(), isBoss));
        }

        data.addKill(isBoss);

        com.huige233.transcend.ascension.resource.ClassResourceHandler.onKill(player, isBoss);

        if (isBoss) {
            AscensionVow tv = data.getActiveTertiaryVow();
            if (tv != null && !data.isVowLiberated(tv.getId())) {
                switch (tv.getId()) {
                    case "vow_of_destruction" -> {

                        float hpRatio = player.getHealth() / player.getMaxHealth();
                        if (hpRatio < 0.30f) {
                            tryLiberateVowByTrial(player, data, tv.getId());
                        }
                    }
                    case "vow_of_solitude" -> {

                        if (target instanceof com.huige233.transcend.entity.boss.VoidWeaver) {
                            if (!data.isVowSolitudeUsedAoE()) {
                                tryLiberateVowByTrial(player, data, tv.getId());
                            }
                            data.resetVowSolitudeForBoss();
                        }
                    }
                }
            }
        }

        if (isBoss) {
            tryLiberateVowForStage(player, data, 4);
        }

        long soulGain = computeSoulGain(target, isBoss);
        data.addSoulEnergy(soulGain);

        long baseXp = isBoss ? XP_KILL_BOSS : XP_KILL_NORMAL;
        List<PassiveEffect> passives = TreeRegistry.getInstance()
                .getActivePassives(data.getUnlockedNodes(), data.getMageClass());
        AscensionStatBlock stats = data.buildTotalStats();
        float xpMultiplier = PassiveEffect.aggregateKillXpMultiplier(stats.xpGainMult, passives);
        long xp = (long) (baseXp * xpMultiplier);

        boolean leveledUp = data.addInsightXP(xp);

        if (leveledUp) {
            int lv = data.getInsightLevel();
            player.displayClientMessage(
                    Component.translatable("msg.transcend.ascension_level_up", lv)
                            .withStyle(ChatFormatting.GOLD), true);
            applyPersistentStats(player, data);
        }

        for (PassiveEffect effect : passives) {
            effect.onKill(player, target, isBoss);
        }

        if (data.getMageClass() == MageClass.ABYSSWALKER) {
            player.getFoodData().eat(1, 0.1f);
        }

        syncToClient(player, data);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {

        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && !(event.getEntity() instanceof Player)) {
            handlePlayerAttacks(event, attacker, event.getEntity());
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PlayerAscensionData data = AscensionCapability.get(player);
        List<PassiveEffect> passives = TreeRegistry.getInstance()
                .getActivePassives(data.getUnlockedNodes(), data.getMageClass());
        AscensionStatBlock stats = data.buildTotalStats();

        float dodgeChance = PassiveEffect.aggregateDodgeChance(stats.dodgeChance, passives);
        if (dodgeChance > 0f && player.getRandom().nextFloat() < dodgeChance) {
            event.setCanceled(true);
            return;
        }

        if (data.hasSelectedClass()) {
            switch (data.getMageClass()) {
                case EARTHSHAPER -> event.setAmount(event.getAmount() * 0.97f);
                case STORMCALLER -> {
                    if (event.getSource().getEntity() instanceof LivingEntity atk) {
                        atk.hurt(player.damageSources().magic(), event.getAmount() * 0.01f);
                    }
                }
                default -> {}
            }
        }

        float statReduction = stats.getEffectiveDamageReductionPercent();
        if (statReduction > 0f) {
            event.setAmount(event.getAmount() * (1.0f - statReduction));
        }
        if (stats.damageReductionFlat > 0) {
            event.setAmount(Math.max(0, event.getAmount() - stats.damageReductionFlat));
        }
        float passiveReduction = PassiveEffect.aggregateDamageReduction(passives);
        if (passiveReduction > 0f) {
            event.setAmount(event.getAmount() * (1.0F - passiveReduction));
        }

        if (isLiberatedVow(data, "vow_of_immortality")) {
            event.setAmount(applyImmortalityDamageReduction(event.getAmount()));
        }

        for (PassiveEffect effect : passives) {
            if (event.isCanceled()) break;
            if (effect instanceof PassiveEffect.Dodge
                    || effect instanceof PassiveEffect.DamageReduction
                    || effect instanceof PassiveEffect.Undying) continue;
            effect.onHurt(event, player, data.getRitualTier());
        }

        if (!event.isCanceled()) applyAggregatedUndying(event, player, passives);

        if (!event.isCanceled()) {
            float sedimentReduction = com.huige233.transcend.ascension.resource.ClassResourceHandler
                    .getEarthshaperDamageReduction(player);
            event.setAmount(event.getAmount() * (1.0F - sedimentReduction));
        }

        if (!event.isCanceled()) {
            com.huige233.transcend.ascension.resource.ClassResourceHandler.onTakeDamage(player, event.getAmount());
        }

        if (!event.isCanceled() && event.getAmount() > 0) {
            AscensionVow tv = data.getActiveTertiaryVow();
            if (tv != null && "vow_of_immortality".equals(tv.getId()) && !data.isVowLiberated(tv.getId())) {
                data.addVowImmortalityDamage(event.getAmount());
                if (data.getVowImmortalityDamageEndured() >= 50000L) {
                    tryLiberateVowByTrial(player, data, tv.getId());
                }
            }

            AscensionVow tvRes = data.getActiveTertiaryVow();
            if (tvRes != null && "vow_of_resonance".equals(tvRes.getId()) && !data.isVowLiberated(tvRes.getId())) {
                data.updateVowResonanceLastCombatTick(player.level().getGameTime());
            }
        }
    }

    private static void handlePlayerAttacks(LivingHurtEvent event, ServerPlayer attacker, LivingEntity target) {
        PlayerAscensionData data = AscensionCapability.get(attacker);
        List<PassiveEffect> passives = TreeRegistry.getInstance()
                .getActivePassives(data.getUnlockedNodes(), data.getMageClass());
        if (target.getMaxHealth() > 0.0F) {
            float healthFraction = target.getHealth() / target.getMaxHealth();
            float executeBonus = PassiveEffect.strongestExecuteBonus(passives, healthFraction);
            if (executeBonus > 0.0F) event.setAmount(event.getAmount() * (1.0F + executeBonus));
        }
        for (PassiveEffect effect : passives) {
            if (event.isCanceled()) break;
            if (effect instanceof PassiveEffect.ExecuteThreshold) continue;
            effect.onAttack(event, attacker, target);
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || event.getEntity() instanceof Player || event.getAmount() <= 0.0F) return;

        AscensionStatBlock stats = AscensionCapability.get(attacker).buildTotalStats();
        float healRate = PassiveEffect.aggregateDamageHealingRate(stats.lifesteal, stats.spellVamp,
                event.getSource().is(ModDamageTypes.ELEMENTAL_SPELL));
        if (healRate > 0.0F) attacker.heal(event.getAmount() * healRate);

        com.huige233.transcend.ascension.resource.ClassResourceHandler
                .onDealDamage(attacker, event.getAmount());
    }

    private static void applyAggregatedUndying(LivingHurtEvent event, ServerPlayer player,
                                                List<PassiveEffect> passives) {
        if (player.getHealth() - event.getAmount() > 0.0F) return;
        PassiveEffect.UndyingPolicy policy = PassiveEffect.aggregateUndying(passives);
        if (!policy.isPresent()) return;

        long now = player.level().getGameTime();
        long lastTrigger = player.getPersistentData().getLong(UNDYING_COOLDOWN_KEY);
        if (now - lastTrigger < policy.cooldown()
                || player.getRandom().nextFloat() >= policy.chance()) return;

        event.setAmount(Math.max(0.0F, player.getHealth() - 1.0F));
        player.getPersistentData().putLong(UNDYING_COOLDOWN_KEY, now);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.REGENERATION, 100, 1));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.ABSORPTION, 200, 1));
    }

    private static boolean applyImmortalitySave(LivingHurtEvent event, ServerPlayer player,
                                                PlayerAscensionData data) {
        if (!isLiberatedVow(data, "vow_of_immortality")
                || player.getHealth() - event.getAmount() > 0.0F) return false;
        long now = player.level().getGameTime();
        if (now < player.getPersistentData().getLong(IMMORTALITY_SAVE_READY_KEY)) return false;

        event.setAmount(Math.max(0.0F, player.getHealth() - 1.0F));
        player.getPersistentData().putLong(IMMORTALITY_SAVE_READY_KEY,
                now + IMMORTALITY_SAVE_COOLDOWN_TICKS);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.REGENERATION, 100, 1));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.ABSORPTION, 200, 1));
        return true;
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void onImmortalityDeathSave(LivingHurtEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        applyImmortalitySave(event, player, AscensionCapability.get(player));
    }

    private static boolean isLiberatedVow(PlayerAscensionData data, String vowId) {
        AscensionVow vow = data.getActiveTertiaryVow();
        return vow != null && vowId.equals(vow.getId()) && data.isVowLiberated(vowId);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PlayerAscensionData data = AscensionCapability.get(sp);

            AscensionVow tv = data.getActiveTertiaryVow();
            if (tv != null && "vow_of_immortality".equals(tv.getId()) && !data.isVowLiberated(tv.getId())) {
                data.resetVowImmortalityDamage();
                sp.displayClientMessage(
                        Component.translatable("msg.transcend.vow_immortality_reset")
                                .withStyle(ChatFormatting.RED), true);
            }
            applyPersistentStats(sp, data);
        }
    }

    public static void applyPersistentStats(ServerPlayer player, PlayerAscensionData data) {
        AscensionStatBlock stats = data.buildTotalStats();

        AttributeInstance maxHpAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHpAttr != null) {
            maxHpAttr.removeModifier(UUID_HP);
            if (stats.bonusMaxHealth > 0) {
                maxHpAttr.addPermanentModifier(new AttributeModifier(
                        UUID_HP,
                        "transcend_ascension_hp",
                        stats.bonusMaxHealth,
                        AttributeModifier.Operation.ADDITION));
            }
        }

        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(UUID_SPEED);
            if (stats.moveSpeedBonus > 0) {
                speedAttr.addPermanentModifier(new AttributeModifier(
                        UUID_SPEED,
                        "transcend_ascension_speed",
                        stats.moveSpeedBonus,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }

        applyTranscendAttribute(player,
                com.huige233.transcend.TranscendAttributes.SPELL_POWER.get(),
                UUID_SPELL_POWER, "transcend_ascension_spell_power",
                stats.spellPowerBonus, AttributeModifier.Operation.ADDITION);

        applyTranscendAttribute(player,
                com.huige233.transcend.TranscendAttributes.SPELL_RESIST.get(),
                UUID_SPELL_RESIST, "transcend_ascension_spell_resist",
                stats.incomingSpellDamageReduction, AttributeModifier.Operation.ADDITION);

        applyTranscendAttribute(player,
                com.huige233.transcend.TranscendAttributes.MAX_MANA.get(),
                UUID_MAX_MANA, "transcend_ascension_max_mana",
                stats.bonusManaCapacity, AttributeModifier.Operation.ADDITION);

        applyTranscendAttribute(player,
                com.huige233.transcend.TranscendAttributes.MANA_REGEN.get(),
                UUID_MANA_REGEN, "transcend_ascension_mana_regen",
                stats.manaRegenBonus, AttributeModifier.Operation.ADDITION);

        applyTranscendAttribute(player,
                com.huige233.transcend.TranscendAttributes.CRIT_CHANCE.get(),
                UUID_CRIT_CHANCE, "transcend_ascension_crit_chance",
                stats.critChance, AttributeModifier.Operation.ADDITION);

        applyTranscendAttribute(player,
                com.huige233.transcend.TranscendAttributes.COOLDOWN_REDUCTION.get(),
                UUID_CDR, "transcend_ascension_cdr",
                stats.cooldownReduction, AttributeModifier.Operation.ADDITION);

        applyTranscendAttribute(player, Attributes.ATTACK_DAMAGE, UUID_REALM_DAMAGE,
                "transcend_cultivation_realm_damage",
                Math.max(0, data.getCultivationRealm().getRank() - 1) * 0.02D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);

        applyDeviationModifier(player, Attributes.MAX_HEALTH, UUID_DEVIATION_MAJOR, -0.20D,
                data.hasCultivationDeviation());
        applyDeviationModifier(player, Attributes.MOVEMENT_SPEED, UUID_DEVIATION_MAJOR, -0.20D,
                data.hasCultivationDeviation());
        applyDeviationModifier(player, Attributes.ATTACK_DAMAGE, UUID_DEVIATION_MAJOR, -0.20D,
                data.hasCultivationDeviation());
        applyDeviationModifier(player, Attributes.ATTACK_SPEED, UUID_DEVIATION_MAJOR, -0.20D,
                data.hasCultivationDeviation());
        applyDeviationModifier(player, Attributes.ARMOR, UUID_DEVIATION_MAJOR, -0.20D,
                data.hasCultivationDeviation());
        applyDeviationModifier(player, Attributes.ARMOR_TOUGHNESS, UUID_DEVIATION_MAJOR, -0.20D,
                data.hasCultivationDeviation());
        applyDeviationModifier(player, com.huige233.transcend.TranscendAttributes.SPELL_POWER.get(),
                UUID_DEVIATION_MAJOR, -0.20D, data.hasCultivationDeviation());
        applyDeviationModifier(player, com.huige233.transcend.TranscendAttributes.SPELL_RESIST.get(),
                UUID_DEVIATION_MAJOR, -0.20D, data.hasCultivationDeviation());
        applyDeviationModifier(player, com.huige233.transcend.TranscendAttributes.MANA_REGEN.get(),
                UUID_DEVIATION_MAJOR, -0.20D, data.hasCultivationDeviation());
        applyDeviationModifier(player, com.huige233.transcend.TranscendAttributes.CRIT_CHANCE.get(),
                UUID_DEVIATION_MAJOR, -0.20D, data.hasCultivationDeviation());
        applyDeviationModifier(player, com.huige233.transcend.TranscendAttributes.COOLDOWN_REDUCTION.get(),
                UUID_DEVIATION_MAJOR, -0.20D, data.hasCultivationDeviation());
        applyDeviationModifier(player, com.huige233.transcend.TranscendAttributes.MAX_MANA.get(),
                UUID_DEVIATION_MANA, -0.30D, data.hasCultivationDeviation());

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void applyTranscendAttribute(ServerPlayer player,
                                                net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                                UUID uuid, String name, double value,
                                                AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        inst.removeModifier(uuid);
        if (value > 0) {
            inst.addPermanentModifier(new AttributeModifier(uuid, name, value, op));
        }
    }

    private static void applyDeviationModifier(ServerPlayer player,
                                               net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                               UUID uuid, double amount, boolean active) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(uuid);
        if (active) {
            instance.addPermanentModifier(new AttributeModifier(uuid, "transcend_cultivation_deviation",
                    amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    public static boolean tryCompleteRitual(ServerPlayer player,
                                            PlayerAscensionData data,
                                            AscensionRitual ritual) {
        boolean ok = data.tryCompleteRitual(ritual);
        if (!ok) return false;

        player.sendSystemMessage(
                Component.translatable("msg.transcend.ritual_complete",
                        ritual.getDisplayName())
                        .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(
                ritual.getRewardText());

        applyPersistentStats(player, data);
        syncToClient(player, data);

        applyRitualEffects(player, ritual);

        player.level().broadcastEntityEvent(player, (byte) 35);

        int unlockedStage = ritual.stageIndex + 1;
        if (unlockedStage >= 1 && unlockedStage <= 4 && !data.hasVowForStage(unlockedStage)) {
            player.sendSystemMessage(Component.translatable("msg.transcend.vow_hint", unlockedStage)
                    .withStyle(ChatFormatting.AQUA));
        }

        for (int s = 1; s <= Math.min(ritual.stageIndex, 2); s++) {
            tryLiberateVowForStage(player, data, s);
        }
        return true;
    }

    public static void tryLiberateVowForStage(ServerPlayer player, PlayerAscensionData data, int stage) {
        String vowId = data.getVowForStage(stage);
        if (vowId == null || vowId.isEmpty()) return;
        if (data.isVowLiberated(vowId)) return;
        data.liberateVow(vowId);

        AscensionVow vow = VowRegistry.get(vowId);
        Component vowName = (vow != null)
                ? Component.translatable(vow.getTranslationKey())
                : Component.literal(vowId);
        player.sendSystemMessage(
                Component.translatable("msg.transcend.vow_liberated", vowName)
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
        player.level().playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
        applyPersistentStats(player, data);
        syncToClient(player, data);
    }

    private static void tryLiberateVowByTrial(ServerPlayer player, PlayerAscensionData data, String vowId) {
        if (data.isVowLiberated(vowId)) return;
        data.liberateVow(vowId);
        AscensionVow vow = VowRegistry.get(vowId);
        Component vowName = (vow != null)
                ? Component.translatable(vow.getTranslationKey())
                : Component.literal(vowId);
        player.sendSystemMessage(
                Component.translatable("msg.transcend.vow_trial_complete", vowName)
                        .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(
                Component.translatable("msg.transcend.vow_liberated", vowName)
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
        player.level().playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
        if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    30, 0.5, 0.5, 0.5, 0.2);
        }
        applyPersistentStats(player, data);
        syncToClient(player, data);
    }

    public static void notifyVowResonanceReaction(net.minecraft.world.entity.LivingEntity caster, String reactionName) {
        if (!(caster instanceof ServerPlayer sp)) return;
        PlayerAscensionData data = AscensionCapability.get(sp);
        AscensionVow tv = data.getActiveTertiaryVow();
        if (tv == null || !"vow_of_resonance".equals(tv.getId()) || data.isVowLiberated(tv.getId())) return;
        if (!RESONANCE_REACTION_CATEGORIES.contains(reactionName)) return;
        data.addVowResonanceCombatReaction(reactionName);
        data.updateVowResonanceLastCombatTick(sp.level().getGameTime());
        if (hasBothResonanceReactionCategories(data.getVowResonanceCombatReactions())) {
            tryLiberateVowByTrial(sp, data, tv.getId());
        }
    }

    public static void recordCast(Player player) {
        if (player.level().isClientSide) return;
        AscensionCapability.ifPresent(player, data -> {
            data.addCast();

            AscensionVow tv = data.getActiveTertiaryVow();
            if (tv != null && "vow_of_resonance".equals(tv.getId()) && !data.isVowLiberated(tv.getId())) {
                data.updateVowResonanceLastCombatTick(player.level().getGameTime());
            }

            long casts = data.getTotalCasts();
            if (casts == 100 || casts == 500 || casts == 1000 || casts == 5000) {
                if (player instanceof ServerPlayer sp) syncToClient(sp, data);
            }
        });
    }

    public static void syncToClient(ServerPlayer player, PlayerAscensionData data) {
        com.huige233.transcend.handle.NetworkHandler.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new com.huige233.transcend.network.S2CAscensionSync(data));
    }

    public static boolean tryFreeCast(ServerPlayer player) {
        if (player == null) return false;
        PlayerAscensionData data = AscensionCapability.get(player);
        List<PassiveEffect> passives = TreeRegistry.getInstance()
                .getActivePassives(data.getUnlockedNodes(), data.getMageClass());
        float chance = PassiveEffect.aggregateFreeCastChance(passives);
        return chance > 0.0F && player.getRandom().nextFloat() < chance;
    }

    public static float getCritChance(net.minecraft.world.entity.player.Player player) {
        if (player == null) return 0f;
        return Math.min(0.80f,
                (float) player.getAttributeValue(com.huige233.transcend.TranscendAttributes.CRIT_CHANCE.get()));
    }

    public static float getCDR(net.minecraft.world.entity.player.Player player) {
        if (player == null) return 0f;
        return Math.min(0.75f,
                (float) player.getAttributeValue(com.huige233.transcend.TranscendAttributes.COOLDOWN_REDUCTION.get()));
    }

    private static final String REGEN_ACCUM_TAG = "transcend_mana_regen_accum";

    public static double computeEnvironmentManaMultiplier(ServerPlayer sp) {
        double mult = 1.0;
        net.minecraft.world.level.Level level = sp.level();
        net.minecraft.core.BlockPos pos = sp.blockPosition();

        int y = pos.getY();
        if (y < 0)        mult *= 1.6;
        else if (y < 40)  mult *= 1.3;
        else if (y > 100) mult *= 1.2;

        boolean canSeeSky = level.canSeeSky(pos);
        if (canSeeSky) {
            boolean day = level.isDay();
            mult *= day ? 1.2 : 1.1;
        }

        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim = level.dimension();
        if (dim == net.minecraft.world.level.Level.NETHER) mult *= 0.7;
        else if (dim == net.minecraft.world.level.Level.END) mult *= 1.4;

        if (level.isThundering())     mult *= 1.3;
        else if (level.isRaining())   mult *= 0.8;

        var biomeHolder = level.getBiome(pos);
        String biomeName = biomeHolder.unwrapKey()
                .map(k -> k.location().getPath())
                .orElse("");
        if (biomeName.contains("mushroom") || biomeName.contains("ancient")
                || biomeName.contains("deep_dark") || biomeName.contains("lush")) {
            mult *= 1.5;
        } else if (biomeName.contains("ocean") || biomeName.contains("river")
                || biomeName.contains("swamp")) {
            mult *= 1.2;
        } else if (biomeName.contains("forest") || biomeName.contains("jungle")
                || biomeName.contains("taiga")) {
            mult *= 1.1;
        } else if (biomeName.contains("desert") || biomeName.contains("badlands")
                || biomeName.contains("savanna")) {
            mult *= 0.7;
        }

        return Math.max(0.3, Math.min(mult, 3.0));
    }

    private static final java.util.Map<UUID, Integer> LAST_SYNCED_INNATE = new java.util.concurrent.ConcurrentHashMap<>();

    private static final long FORCE_SYNC_INTERVAL_TICKS = 40L;
    private static final java.util.Map<UUID, Long> LAST_SYNC_TICK = new java.util.concurrent.ConcurrentHashMap<>();

    private static final java.util.Map<UUID, Float> ABSORB_ACCUM = new java.util.concurrent.ConcurrentHashMap<>();

    private static final java.util.Map<UUID, Float> ABSORB_LAST_SEC = new java.util.concurrent.ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer sp)) return;

        PlayerAscensionData data = AscensionCapability.get(sp);
        UUID uuid = sp.getUUID();
        long now = sp.level().getGameTime();

        if (data.isAuraGuardEnabled() && now % 20L == 0L) {
            int upkeep = 2;
        if (!com.huige233.transcend.spell.MagicCrystalHelper.consumeMana(sp, upkeep)) {
                data.setAuraGuardEnabled(false);
                syncToClient(sp, data);
            }
        }

        double regenPerSec = sp.getAttributeValue(com.huige233.transcend.TranscendAttributes.MANA_REGEN.get());
        double intrinsicDelta = (regenPerSec > 0) ? regenPerSec / 20.0 : 0.0;
        net.minecraft.nbt.CompoundTag pdata = sp.getPersistentData();
        double storedFraction = pdata.getDouble(REGEN_ACCUM_TAG);
        int currentInnate = com.huige233.transcend.spell.MagicCrystalHelper.getInnateMana(sp);
        int maxInnate = com.huige233.transcend.spell.MagicCrystalHelper.getInnateMaxMana(sp);
        int effectiveMax = (int) (maxInnate * NexusWorldPenalty.getManaCapMultiplier(sp));
        if (currentInnate > effectiveMax) {
        com.huige233.transcend.spell.MagicCrystalHelper.setInnateMana(sp, effectiveMax);
            currentInnate = effectiveMax;
        }
        double intrinsicAccepted = Math.min(intrinsicDelta,
                Math.max(0.0, effectiveMax - currentInnate - storedFraction));

        int stage = data.getRitualTier();
        int ascLv = data.getInsightLevel();

        double absorbPerSec = NexusWorldPenalty.isAuraAbsorptionDisabled(sp)
                ? 0.0
                : Math.min(8.0, stage + ascLv * 0.4);
        float actualAbsorbed = 0f;
        if (absorbPerSec > 0) {
            double absorbPerTick = absorbPerSec / 20.0;
            float acceptable = environmentalManaAcceptance(effectiveMax - currentInnate,
                    storedFraction, intrinsicAccepted, absorbPerTick);
            if (acceptable > 0.0F) {
                com.huige233.transcend.world.mana.ChunkManaSavedData chunkData =
                        com.huige233.transcend.world.mana.ChunkManaSavedData.get(
                                (net.minecraft.server.level.ServerLevel) sp.level());
                actualAbsorbed = chunkData.consumeMana(sp.chunkPosition(), acceptable);
            }
        }

        ABSORB_ACCUM.merge(uuid, actualAbsorbed, Float::sum);

        if (now % 20 == 0) {
            ABSORB_LAST_SEC.put(uuid, ABSORB_ACCUM.getOrDefault(uuid, 0f));
            ABSORB_ACCUM.put(uuid, 0f);
        }

        double accumDelta = intrinsicAccepted + actualAbsorbed;
        if (accumDelta > 0) {
            double accum = storedFraction + accumDelta;
            int wholeManas = Math.min((int) accum, Math.max(0, effectiveMax - currentInnate));
            pdata.putDouble(REGEN_ACCUM_TAG, accum - wholeManas);

            if (wholeManas > 0) {
            com.huige233.transcend.spell.MagicCrystalHelper.setInnateMana(sp,
                        currentInnate + wholeManas);
            }
        }

        int innateNow = com.huige233.transcend.spell.MagicCrystalHelper.getInnateMana(sp);
        float absorbHud = ABSORB_LAST_SEC.getOrDefault(uuid, 0f);
        Integer lastSent = LAST_SYNCED_INNATE.get(uuid);
        long lastSync = LAST_SYNC_TICK.getOrDefault(uuid, -FORCE_SYNC_INTERVAL_TICKS);
        boolean valueChanged = (lastSent == null || lastSent != innateNow);
        boolean forcedRefresh = (now - lastSync) >= FORCE_SYNC_INTERVAL_TICKS;
        if (valueChanged || forcedRefresh) {
            com.huige233.transcend.handle.NetworkHandler.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                    new com.huige233.transcend.network.S2CInnateManaSync(innateNow, absorbHud));
            LAST_SYNCED_INNATE.put(uuid, innateNow);
            LAST_SYNC_TICK.put(uuid, now);
        }

        AscensionVow activeTv = data.getActiveTertiaryVow();
        if (activeTv != null && !data.isVowLiberated(activeTv.getId())) {
            switch (activeTv.getId()) {
                case "vow_of_greed" -> {

        int curMana = com.huige233.transcend.spell.MagicCrystalHelper.getInnateMana(sp);
        int maxMana = com.huige233.transcend.spell.MagicCrystalHelper.getInnateMaxMana(sp);
                    if (curMana >= maxMana && maxMana > 0) {
                        data.tickVowGreedFull();
                        if (data.getVowGreedFullManaTicks() >= 1200) {
                            tryLiberateVowByTrial(sp, data, activeTv.getId());
                        }
                    } else {
                        data.resetVowGreedFullManaTicks();
                    }
                }
                case "vow_of_resonance" -> {

                    long lastCombat = data.getVowResonanceLastCombatTick();
                    if (lastCombat > 0 && (now - lastCombat) > 200L) {
                        data.resetVowResonanceCombat();
                    }
                }
            }
        }

    }

    static float environmentalManaAcceptance(int freeWholeMana, double storedFraction,
                                             double intrinsicDelta, double requested) {
        if (freeWholeMana <= 0 || requested <= 0.0 || !Double.isFinite(requested)) return 0.0F;
        double capacity = freeWholeMana - Math.max(0.0, storedFraction) - Math.max(0.0, intrinsicDelta);
        return (float) Math.max(0.0, Math.min(requested, capacity));
    }

    static float applyImmortalityDamageReduction(float incomingDamage) {
        return Math.max(0.0F, incomingDamage) * 0.75F;
    }

    static boolean hasBothResonanceReactionCategories(Set<String> reactions) {
        return reactions != null && reactions.containsAll(RESONANCE_REACTION_CATEGORIES);
    }

    public static void applyVowEffects(ServerPlayer player, PlayerAscensionData data) {
        if (player == null || data == null) return;

        for (int stage = 1; stage <= 4; stage++) {
            String vowId = data.getVowForStage(stage);
            if (vowId == null || vowId.isEmpty()) continue;

            AscensionVow vow = VowRegistry.get(vowId);
            if (vow == null) continue;

        }
    }

    private static void applyRitualEffects(ServerPlayer player, AscensionRitual ritual) {
        switch (ritual) {
            case AWAKENING -> {
                player.heal(player.getMaxHealth());
                player.getFoodData().eat(20, 20f);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.GLOWING, 200, 0));
            }
            case TEMPERING -> {
                player.heal(player.getMaxHealth());
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 600, 1));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.REGENERATION, 400, 2));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 600, 1));
            }
            case PURIFICATION -> {
                player.heal(player.getMaxHealth());
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 1200, 2));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.REGENERATION, 600, 3));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 1200, 0));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.NIGHT_VISION, 1200, 0));
                for (net.minecraft.world.effect.MobEffectInstance effect : new java.util.ArrayList<>(player.getActiveEffects())) {
                    if (!effect.getEffect().isBeneficial()) player.removeEffect(effect.getEffect());
                }
            }
            case TRANSCENDENCE -> {
                player.heal(player.getMaxHealth());
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 2400, 3));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.REGENERATION, 1200, 4));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.ABSORPTION, 2400, 4));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.HERO_OF_THE_VILLAGE, 6000, 0));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 2400, 2));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 2400, 1));
                for (net.minecraft.world.effect.MobEffectInstance effect : new java.util.ArrayList<>(player.getActiveEffects())) {
                    if (!effect.getEffect().isBeneficial()) player.removeEffect(effect.getEffect());
                }
                player.level().broadcastEntityEvent(player, (byte) 35);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHealAscension(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerAscensionData data = AscensionCapability.get(player);
        AscensionStatBlock stats = data.buildTotalStats();

        if (event.getAmount() > 0) {
            AscensionVow tv = data.getActiveTertiaryVow();
            if (tv != null && "vow_of_devotion".equals(tv.getId()) && !data.isVowLiberated(tv.getId())) {
                data.addVowDevotionHealing(event.getAmount());
                if (data.getVowDevotionHealingAccum() >= 10000L) {
                    tryLiberateVowByTrial(player, data, tv.getId());
                }
            }
        }

        float amount = event.getAmount();
        if (!Float.isFinite(amount) || amount <= 0.0F) {
            event.setAmount(0.0F);
            return;
        }
        float mult = data.getVowHealingMult() * (1.0f + stats.healingReceivedBonus);

        if (stats.naturalRegenBonus > 0
                && event.getAmount() > 0
                && event.getAmount() <= 2.0f
                && player.getFoodData().getFoodLevel() >= 18) {
            mult *= (1.0f + stats.naturalRegenBonus);
        }

        float adjusted = amount * mult;
        event.setAmount(Float.isFinite(adjusted) ? Math.max(0.0F, adjusted) : 0.0F);
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void onDeathSave(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerAscensionData data = AscensionCapability.get(player);
        AscensionStatBlock stats = data.buildTotalStats();
        if (stats.deathSaveEnabled <= 0) return;
        if (data.getRitualTier() < 4 || data.getInsightLevel() < 10) return;

        long now = player.level().getGameTime();
        long cooldownTicks = 6000L;
        if (now - data.getLastDeathSaveAt() < cooldownTicks) return;

        if (player.getHealth() - event.getAmount() > 0) return;

        float capped = Math.max(0f, player.getHealth() - 1.0f);
        event.setAmount(capped);
        data.setLastDeathSaveAt(now);
        com.huige233.transcend.ascension.AscensionHandler.syncToClient(player, data);

        player.displayClientMessage(
                Component.translatable("msg.transcend.death_save.triggered")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        player.level().playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.TOTEM_USE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    40, 0.5, 0.5, 0.5, 0.5);
        }
    }

    @SubscribeEvent
    public static void onPlayerTickR74Food(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        PlayerAscensionData data = AscensionCapability.get(player);
        AscensionStatBlock stats = data.buildTotalStats();
        if (stats.foodConsumptionReduction <= 0) return;

        try {
            net.minecraft.world.food.FoodData fd = player.getFoodData();
            java.lang.reflect.Field f = net.minecraft.world.food.FoodData.class.getDeclaredField("exhaustionLevel");
            f.setAccessible(true);
            float currExh = f.getFloat(fd);
            Float prevExh = EXHAUSTION_TRACKING.get(player.getUUID());

            if (prevExh != null && currExh > prevExh) {
                float delta = currExh - prevExh;
                float refund = delta * stats.foodConsumptionReduction;
                float newExh = Math.max(0f, currExh - refund);
                f.setFloat(fd, newExh);
                EXHAUSTION_TRACKING.put(player.getUUID(), newExh);
            } else {
                EXHAUSTION_TRACKING.put(player.getUUID(), currExh);
            }
        } catch (Exception ignored) {

        }
    }

    private static final java.util.Map<java.util.UUID, Float> EXHAUSTION_TRACKING = new java.util.concurrent.ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLivingFall(net.minecraftforge.event.entity.living.LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerAscensionData data = AscensionCapability.get(player);
        AscensionStatBlock stats = data.buildTotalStats();
        if (stats.fallDamageReduction <= 0) return;

        event.setDamageMultiplier(event.getDamageMultiplier() * (1.0f - stats.fallDamageReduction));
    }

    @SubscribeEvent
    public static void onMobEffectAdded(net.minecraftforge.event.entity.living.MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        net.minecraft.world.effect.MobEffectInstance inst = event.getEffectInstance();
        if (inst.getEffect().getCategory() != net.minecraft.world.effect.MobEffectCategory.HARMFUL) return;

        PlayerAscensionData data = AscensionCapability.get(player);
        AscensionStatBlock stats = data.buildTotalStats();
        if (stats.controlResistance <= 0) return;

        int oldDuration = inst.getDuration();
        int newDuration = (int) (oldDuration * (1.0f - stats.controlResistance));
        if (newDuration < 20) newDuration = 20;
        if (newDuration >= oldDuration) return;

        try {
            java.lang.reflect.Field f = net.minecraft.world.effect.MobEffectInstance.class.getDeclaredField("duration");
            f.setAccessible(true);
            f.setInt(inst, newDuration);
        } catch (Exception ignored) {

        }
    }

    @SubscribeEvent
    public static void onSoulMarkRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered()) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        PlayerAscensionData data = AscensionCapability.get(sp);
        if (data.getSoulMarks().isEmpty()) return;

        net.minecraft.resources.ResourceLocation dimKey = sp.level().dimension().location();
        var nearest = data.findNearestSoulMark(dimKey, sp.blockPosition());
        if (nearest == null) return;

        net.minecraft.core.BlockPos teleportPos = nearest.pos();
        net.minecraft.world.level.block.entity.BlockEntity be = sp.level().getBlockEntity(teleportPos);

        if (!(be instanceof com.huige233.transcend.block.ascension.AscensionAnchorBlockEntity anchorBe)) {
            data.removeSoulMark(dimKey, teleportPos);
            syncToClient(sp, data);
            return;
        }

        if (!sp.getUUID().equals(anchorBe.getSoulMarkOwner())) {
            data.removeSoulMark(dimKey, teleportPos);
            syncToClient(sp, data);
            return;
        }

        sp.teleportTo(teleportPos.getX() + 0.5, teleportPos.getY() + 1.0, teleportPos.getZ() + 0.5);
        sp.displayClientMessage(
                Component.translatable("msg.transcend.soul_mark.respawned")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        sp.level().playSound(null, teleportPos,
                net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.8F);
        if (sp.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                    teleportPos.getX() + 0.5, teleportPos.getY() + 1.5, teleportPos.getZ() + 0.5,
                    40, 0.5, 1.0, 0.5, 0.3);
        }
    }

    @SubscribeEvent
    public static void onSoulMarkRangeBuff(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 60 != 0) return;

        PlayerAscensionData data = AscensionCapability.get(sp);
        if (data.getSoulMarks().isEmpty()) return;

        net.minecraft.resources.ResourceLocation dimKey = sp.level().dimension().location();
        var nearest = data.findNearestSoulMark(dimKey, sp.blockPosition());
        if (nearest == null) return;

        net.minecraft.world.level.block.entity.BlockEntity be = sp.level().getBlockEntity(nearest.pos());
        if (!(be instanceof com.huige233.transcend.block.ascension.AscensionAnchorBlockEntity anchorBe)) return;
        if (!sp.getUUID().equals(anchorBe.getSoulMarkOwner())) return;

        double distSq = nearest.pos().distSqr(sp.blockPosition());
        if (distSq > 100.0 * 100.0) return;

        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.REGENERATION, 80, 0, true, false, true));
        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 80, 0, true, false, true));
    }
}
