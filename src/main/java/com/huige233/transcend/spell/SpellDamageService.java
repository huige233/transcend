package com.huige233.transcend.spell;

import com.huige233.transcend.ModDamageTypes;
import com.huige233.transcend.TranscendAttributes;
import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.ElementMastery;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.util.EntityCompatUtil;
import com.huige233.transcend.world.nexus.NexusWorldPenalty;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntSupplier;

public final class SpellDamageService {
    public static final String CANONICAL_MARK_TAG = "transcend_canonical_spell_mark";
    public static final String CURSE_TAG = "transcend_canonical_spell_curse";
    private static final SpellElement[] FIVE_ELEMENTS = {
            SpellElement.METAL, SpellElement.WOOD, SpellElement.WATER,
            SpellElement.FIRE, SpellElement.EARTH
    };

    public record Result(boolean landed, float attemptedDamage, float actualHealthDamage,
                         SpellElement resolvedElement) {
        private static Result rejected(SpellElement element) {
            return new Result(false, 0.0F, 0.0F, element);
        }
    }

    private SpellDamageService() {}

    interface DealRuntime {
        float applyResistance(LivingEntity target, @Nullable LivingEntity caster,
                              SpellElement resolved, int spellTier, float rawDamage, boolean chaos);

        boolean hurt(ServerLevel level, @Nullable LivingEntity caster, @Nullable Entity directSource,
                     LivingEntity target, float damage);

        void applyEffect(LivingEntity target, @Nullable LivingEntity caster, SpellElement resolved);

        void spawnHitFlash(ServerLevel level, LivingEntity target, SpellElement resolved);
    }

    static SpellElement resolveElement(SpellElement sourceElement, boolean entropyActive, int fiveElementIndex) {
        if (sourceElement == null) throw new IllegalArgumentException("sourceElement must not be null");
        if (fiveElementIndex < 0 || fiveElementIndex >= FIVE_ELEMENTS.length) {
            throw new IllegalArgumentException("fiveElementIndex must be between 0 and 4");
        }
        SpellElement canonical = sourceElement.canonical();
        return canonical == SpellElement.CHAOS || entropyActive
                ? FIVE_ELEMENTS[fiveElementIndex]
                : canonical;
    }

    static SpellElement selectElement(SpellElement sourceElement, boolean entropyActive,
                                      IntSupplier fiveElementSelector) {
        if (sourceElement == null) throw new IllegalArgumentException("sourceElement must not be null");
        boolean randomize = sourceElement.canonical() == SpellElement.CHAOS || entropyActive;
        if (!randomize) return resolveElement(sourceElement, false, 0);
        if (fiveElementSelector == null) {
            throw new IllegalArgumentException("fiveElementSelector must not be null");
        }
        return resolveElement(sourceElement, entropyActive, fiveElementSelector.getAsInt());
    }

    public static Result deal(ServerLevel level, @Nullable LivingEntity caster, @Nullable Entity directSource,
                              LivingEntity target, SpellElement sourceElement, int spellTier,
                              float rawDamage, boolean applyReactions) {
        boolean entropyActive = caster instanceof ServerPlayer serverPlayer
                && NexusWorldPenalty.isReactionScrambled(serverPlayer);
        return deal(level, caster, directSource, target, sourceElement, spellTier, rawDamage,
                applyReactions, entropyActive, () -> level.getRandom().nextInt(FIVE_ELEMENTS.length),
                RealDealRuntime.INSTANCE);
    }

    static Result deal(ServerLevel level, @Nullable LivingEntity caster, @Nullable Entity directSource,
                       LivingEntity target, SpellElement sourceElement, int spellTier,
                       float rawDamage, boolean applyReactions, boolean entropyActive,
                       IntSupplier fiveElementSelector, DealRuntime runtime) {
        if (level == null || target == null || sourceElement == null || !target.isAlive()
                || target == caster || target == directSource || EntityCompatUtil.isProtectedPlayer(target)
                || !Float.isFinite(rawDamage) || rawDamage <= 0.0F || runtime == null) {
            return Result.rejected(sourceElement);
        }

        boolean chaos = sourceElement.canonical() == SpellElement.CHAOS;
        SpellElement resolved = selectElement(sourceElement, entropyActive, fiveElementSelector);
        float damage = runtime.applyResistance(target, caster, resolved, spellTier, rawDamage, chaos);

        if (target.getPersistentData().getInt(CANONICAL_MARK_TAG) > 0) damage *= 1.15F;
        if (target.getPersistentData().getInt(CURSE_TAG) > 0) damage *= 1.20F;
        ElementReaction.MarkResult reaction = applyReactions
                ? ElementReaction.prospectivelyResolve(target, resolved, caster)
                : null;
        if (reaction != null) damage *= reaction.damageMultiplier();
        damage = Float.isFinite(damage) ? Math.max(0.0F, damage) : 0.0F;
        if (damage <= 0.0F) return Result.rejected(resolved);

        float healthBefore = target.getHealth();
        boolean landed = runtime.hurt(level, caster, directSource, target, damage);
        float measuredHealthDamage = landed
                ? Math.max(0.0F, healthBefore - Math.max(0.0F, target.getHealth()))
                : 0.0F;
        float actualHealthDamage = Float.isFinite(measuredHealthDamage) ? measuredHealthDamage : 0.0F;
        if (landed) {
            if (reaction != null) ElementReaction.commitLanded(target, reaction, caster);
            runtime.applyEffect(target, caster, resolved);
            runtime.spawnHitFlash(level, target, resolved);
        }
        return new Result(landed, damage, actualHealthDamage, resolved);
    }

    static float masteryResistance(ElementMastery mastery, SpellElement incoming) {
        if (mastery == null || incoming == null || mastery == ElementMastery.NONE) return 0.0F;
        if (mastery == ElementMastery.OMNI) return 0.10F;
        if (!mastery.isSpecific()) return 0.0F;
        return mastery.element == incoming ? 0.20F : 0.05F;
    }

    static void applyElementEffect(LivingEntity target, @Nullable LivingEntity caster,
                                   SpellElement element) {
        switch (element) {
            case METAL -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, true));
            case WOOD -> {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, true));
                if (caster != null) caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, true));
            }
            case WATER -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true));
                target.setTicksFrozen(target.getTicksFrozen() + 40);
            }
            case FIRE -> target.setSecondsOnFire(3);
            case EARTH -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, true));
            default -> { }
        }
    }

    private enum RealDealRuntime implements DealRuntime {
        INSTANCE;

        @Override
        public float applyResistance(LivingEntity target, @Nullable LivingEntity caster,
                                     SpellElement resolved, int spellTier, float rawDamage, boolean chaos) {
            if (!(target instanceof Player defender)) return rawDamage;
            PlayerAscensionData defenderData = AscensionCapability.get(defender);
            float resistance = (chaos ? SpellDamageMath.CHAOS_BASE_RESISTANCE : SpellDamageMath.BASE_RESISTANCE)
                    + defenderData.getElementResistanceBonus(resolved)
                    + (float) defender.getAttributeValue(TranscendAttributes.SPELL_RESIST.get())
                    + masteryResistance(defenderData.getMastery(), resolved);
            float resistanceIgnore = caster instanceof Player player
                    ? AscensionCapability.get(player).buildTotalStats().getEffectiveResistIgnore()
                    : 0.0F;
            return SpellDamageMath.calculate(new SpellDamageMath.Context(rawDamage,
                    defenderData.getSpellTier(), Math.max(1, spellTier), defenderData.isAuraGuardEnabled(),
                    resistance, resistanceIgnore)).finalDamage();
        }

        @Override
        public boolean hurt(ServerLevel level, @Nullable LivingEntity caster, @Nullable Entity directSource,
                            LivingEntity target, float damage) {
            Entity actualDirect = directSource != null ? directSource : caster;
            DamageSource tagged = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(ModDamageTypes.ELEMENTAL_SPELL), actualDirect, caster);

            return target.hurt(tagged, damage);
        }

        @Override
        public void applyEffect(LivingEntity target, @Nullable LivingEntity caster, SpellElement resolved) {
            applyElementEffect(target, caster, resolved);
        }

        @Override
        public void spawnHitFlash(ServerLevel level, LivingEntity target, SpellElement resolved) {
            ElementReaction.spawnHitFlash(level, target, resolved);
        }
    }
}
