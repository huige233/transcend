package com.huige233.transcend.circle.scroll;

import java.util.HashMap;
import java.util.Map;

/**
 * 古法咒卷效果注册表。将咒卷类型字符串映射到对应的 ScrollEffect 实例。
 */
public class ScrollEffectRegistry {

    private static final Map<String, ScrollEffect> EFFECTS = new HashMap<>();

    static {
        EFFECTS.put("solar_judgement", new SolarJudgementEffect());
        EFFECTS.put("leyline_eruption", new LeylineEruptionEffect());
        EFFECTS.put("chronal_stillness", new ChronalStillnessEffect());
        EFFECTS.put("sovereign_aegis", new SovereignAegisEffect());
        EFFECTS.put("thousand_league_return", new ThousandLeagueReturnEffect());
        EFFECTS.put("void_exile_mandate", new VoidExileMandateEffect());
        EFFECTS.put("storm_king_writ", new StormKingWritEffect());
        EFFECTS.put("worldmender_edict", new WorldmenderEdictEffect());
        EFFECTS.put("eclipse_veil", new EclipseVeilEffect());
        EFFECTS.put("avatar_fall", new AvatarFallEffect());
        EFFECTS.put("unbroken_arsenal", new UnbrokenArsenalEffect());
        EFFECTS.put("ordered_vault", new OrderedVaultEffect());
        EFFECTS.put("oreblood_revelation", new OrebloodRevelationEffect());
        EFFECTS.put("paper_legion", new PaperLegionEffect());
        EFFECTS.put("unremembered_fog", new UnrememberedFogEffect());
        EFFECTS.put("inverted_heaven", new InvertedHeavenEffect());
        EFFECTS.put("eighteenfold_dragon", new EighteenfoldDragonEffect());
        EFFECTS.put("leyline_resync", new LeylineResyncEffect());
        EFFECTS.put("forbidden_hollow_quarry", new ForbiddenHollowQuarryEffect());
        EFFECTS.put("forbidden_black_sun", new ForbiddenBlackSunEffect());
    }

    private ScrollEffectRegistry() {}

    /** 按咒卷类型获取效果，未注册返回 null */
    public static ScrollEffect get(String scrollType) {
        return EFFECTS.get(scrollType);
    }
}
