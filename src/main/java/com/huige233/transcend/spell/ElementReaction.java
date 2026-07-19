package com.huige233.transcend.spell;

import com.huige233.transcend.visual.ServerVisualBroadcaster;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class ElementReaction {

    private static final String TAG_PREFIX = "transcend_element_";
    private static final String MARK_ELEMENT_TAG = "transcend_element_mark";
    private static final String MARK_TICKS_TAG = "transcend_element_mark_ticks";
    private static final int MARK_DURATION = 100;

    public record MarkResult(SpellElement mark, float damageMultiplier, String vowReaction) {}

    public static void markElement(LivingEntity target, SpellElement element) {
        SpellElement canonical = element.canonical();
        clearLegacyMarks(target);
        if (canonical == SpellElement.CHAOS) {
            target.getPersistentData().remove(MARK_ELEMENT_TAG);
            target.getPersistentData().remove(MARK_TICKS_TAG);
            return;
        }
        target.getPersistentData().putString(MARK_ELEMENT_TAG, canonical.id);
        target.getPersistentData().putInt(MARK_TICKS_TAG, MARK_DURATION);
    }

    public static void tryReaction(LivingEntity target, SpellElement incoming, int spellTier,
                                   float damage, LivingEntity caster) {
        if (!(target.level() instanceof ServerLevel level)) return;
        SpellDamageService.deal(level, caster, caster, target, incoming, spellTier, damage, true);
    }

    public static MarkResult prospectivelyResolve(LivingEntity target, SpellElement incoming,
                                                  LivingEntity caster) {
        float reactionBonus = 0.0F;
        if (caster instanceof Player player) {
            reactionBonus = com.huige233.transcend.ascension.AscensionCapability.get(player)
                    .buildTotalStats().reactionBonus;
        }
        return resolveMark(getLatestMark(target), incoming, reactionBonus);
    }

    public static void commitLanded(LivingEntity target, MarkResult result, LivingEntity caster) {
        if (result.mark() == null) {
            target.getPersistentData().remove(MARK_ELEMENT_TAG);
            target.getPersistentData().remove(MARK_TICKS_TAG);
        } else {
            markElement(target, result.mark());
        }
        if (result.vowReaction() != null) {
            com.huige233.transcend.ascension.AscensionHandler
                    .notifyVowResonanceReaction(caster, result.vowReaction());
        }
    }

    public static MarkResult resolveMark(SpellElement previous, SpellElement incoming, float reactionBonus) {
        SpellElement next = incoming == null ? null : incoming.canonical();
        if (next == SpellElement.CHAOS) return new MarkResult(previous, 1.0F, null);
        if (previous == null) return new MarkResult(next, 1.0F, null);
        SpellElement old = previous.canonical();
        if (generates(old, next)) {
            return new MarkResult(next, SpellDamageMath.reactionGenerationMultiplier(reactionBonus),
                    "five_elements_generation");
        }
        if (overcomes(old, next)) {
            return new MarkResult(next, 0.70F, "five_elements_overcoming");
        }
        return new MarkResult(next, 1.0F, null);
    }

    private static SpellElement getLatestMark(LivingEntity target) {
        if (target.getPersistentData().getInt(MARK_TICKS_TAG) <= 0) return null;
        String id = target.getPersistentData().getString(MARK_ELEMENT_TAG);
        if (id.isEmpty()) return null;
        SpellElement mark = SpellElement.getById(id);
        return mark == null || mark.canonical() == SpellElement.CHAOS ? null : mark.canonical();
    }

    private static void clearLegacyMarks(LivingEntity target) {
        for (SpellElement element : SpellElement.values()) {
            target.getPersistentData().remove(TAG_PREFIX + element.id);
        }
    }

    public static boolean generates(SpellElement source, SpellElement result) {
        return switch (source.canonical()) {
            case METAL -> result.canonical() == SpellElement.WATER;
            case WATER -> result.canonical() == SpellElement.WOOD;
            case WOOD -> result.canonical() == SpellElement.FIRE;
            case FIRE -> result.canonical() == SpellElement.EARTH;
            case EARTH -> result.canonical() == SpellElement.METAL;
            default -> false;
        };
    }

    public static boolean overcomes(SpellElement source, SpellElement result) {
        return switch (source.canonical()) {
            case METAL -> result.canonical() == SpellElement.WOOD;
            case WOOD -> result.canonical() == SpellElement.EARTH;
            case EARTH -> result.canonical() == SpellElement.WATER;
            case WATER -> result.canonical() == SpellElement.FIRE;
            case FIRE -> result.canonical() == SpellElement.METAL;
            default -> false;
        };
    }

    public static void tickMarks(LivingEntity entity) {
        clearLegacyMarks(entity);
        int remaining = entity.getPersistentData().getInt(MARK_TICKS_TAG);
        SpellElement latestMark = getLatestMark(entity);
        if (remaining > 0 && latestMark != null) {
            entity.getPersistentData().putInt(MARK_TICKS_TAG, remaining - 1);
        } else {
            entity.getPersistentData().remove(MARK_ELEMENT_TAG);
            entity.getPersistentData().remove(MARK_TICKS_TAG);
        }

        if (remaining > 0 && latestMark != null && entity.level() instanceof ServerLevel sl
                && entity.tickCount % 5 == 0) {
            spawnElementAura(sl, entity, latestMark);
        }

        tickCanonicalDamageTag(entity, SpellDamageService.CANONICAL_MARK_TAG);
        tickCanonicalDamageTag(entity, SpellDamageService.CURSE_TAG);

        int mastery = entity.getPersistentData().getInt("transcend_elemental_mastery");
        if (mastery > 0) {
            entity.getPersistentData().putInt("transcend_elemental_mastery", mastery - 1);
        } else if (mastery == 0 && entity.getPersistentData().contains("transcend_elemental_mastery")) {
            entity.getPersistentData().remove("transcend_elemental_mastery");
        }
    }

    private static void tickCanonicalDamageTag(LivingEntity entity, String tag) {
        int remaining = entity.getPersistentData().getInt(tag);
        if (remaining > 0) entity.getPersistentData().putInt(tag, remaining - 1);
        else if (entity.getPersistentData().contains(tag)) entity.getPersistentData().remove(tag);
    }

    private static void spawnElementAura(ServerLevel level, LivingEntity entity, SpellElement element) {
        if (entity.tickCount % 10 != 0) return;
        Vec3 center = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ());
        ServerVisualBroadcaster.shieldRipple(level, center, 1.2F + entity.getBbWidth() * 0.6F,
                element.getParticleR(), element.getParticleG(), element.getParticleB(), 12);
    }

    public static void spawnHitFlash(ServerLevel level, LivingEntity target, SpellElement element) {
        Vec3 center = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
        ServerVisualBroadcaster.shockwave(level, center, 2.2F,
                Math.min(1.0F, element.getParticleR() * 1.25F),
                Math.min(1.0F, element.getParticleG() * 1.25F),
                Math.min(1.0F, element.getParticleB() * 1.25F), 14);
        ServerVisualBroadcaster.shieldRipple(level, center, 1.5F,
                element.getParticleR(), element.getParticleG(), element.getParticleB(), 12);
    }

}
