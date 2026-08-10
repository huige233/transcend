package com.huige233.transcend.ascension;

import java.util.List;

import com.huige233.transcend.ascension.TrialObjective.ObjectiveType;

/** 试炼目标注册表。 */
public class TrialRegistry {

    public static final List<TrialObjective> AWAKENING_TRIALS = List.of(
        new TrialObjective("awakening_discover", ObjectiveType.DISCOVER_COMPONENTS, 3, null),
        new TrialObjective("awakening_circle", ObjectiveType.STABILIZE_CIRCLE, 300, null),
        new TrialObjective("awakening_cast", ObjectiveType.CAST_COUNT, 30, null),
        new TrialObjective("awakening_crystals", ObjectiveType.ITEM_REQUIREMENT, 8, "transcend:magic_crystal")
    );

    public static final List<TrialObjective> TEMPERING_TRIALS = List.of(
        new TrialObjective("tempering_scroll", ObjectiveType.CRAFT_SCROLL, 1, null),
        new TrialObjective("tempering_dual_circle", ObjectiveType.MULTI_CIRCLE, 2, null),
        new TrialObjective("tempering_warden", ObjectiveType.BOSS_KILL, 1, "transcend:elemental_warden"),
        new TrialObjective("tempering_arena", ObjectiveType.ARENA_WAVE, 5, null)
    );

    public static final List<TrialObjective> PURIFICATION_TRIALS = List.of(
        new TrialObjective("purify_restore", ObjectiveType.RESTORE_TABLET, 1, null),
        new TrialObjective("purify_weaver", ObjectiveType.BOSS_KILL, 1, "transcend:void_weaver"),
        new TrialObjective("purify_nexus", ObjectiveType.NEXUS_ACTION, 1, null),
        new TrialObjective("purify_throughput", ObjectiveType.MANA_THROUGHPUT, 50, null)
    );

    public static final List<TrialObjective> TRANSCENDENCE_TRIALS = List.of(
        new TrialObjective("transcend_avatar", ObjectiveType.BOSS_KILL, 1, "transcend:transcendence_avatar"),
        new TrialObjective("transcend_five_circles", ObjectiveType.MULTI_CIRCLE, 5, null),
        new TrialObjective("transcend_master_scroll", ObjectiveType.CRAFT_SCROLL, 1, "master"),
        new TrialObjective("transcend_kills", ObjectiveType.KILL_COUNT, 800, null)
    );

    public static List<TrialObjective> getTrialsForStage(int targetStage) {
        return switch (targetStage) {
            case 1 -> AWAKENING_TRIALS;
            case 2 -> TEMPERING_TRIALS;
            case 3 -> PURIFICATION_TRIALS;
            case 4 -> TRANSCENDENCE_TRIALS;
            default -> List.of();
        };
    }

    public static boolean allTrialsMet(int targetStage, PlayerAscensionData data) {
        return getTrialsForStage(targetStage).stream().allMatch(t -> t.isMet(data));
    }
}
