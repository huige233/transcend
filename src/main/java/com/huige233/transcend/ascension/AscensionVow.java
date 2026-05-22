package com.huige233.transcend.ascension;

/**
 * 飞升誓约 — 提供增益但同时带来代价的永久性选择。
 * 每个飞升阶段可选择一个誓约，通过洗点药水可重置。
 */
public class AscensionVow {
    private final String id;
    private final int stage;          // 可选择此誓约的飞升阶段 (1-4)
    private final String benefitKey;  // 增益翻译键
    private final String costKey;     // 代价翻译键

    // Stat modifiers (values are percentages or flat amounts, 0 = no effect)
    private final float spellDamageBonus;
    private final float circleUpkeepMult;   // 1.0 = no change, 0.9 = -10%
    private final int circleLimitAdd;
    private final float manaCostMult;       // 1.0 = no change, 1.1 = +10%
    private final float healingMult;        // 1.0 = no change, 0.8 = -20%
    private final int healthAdd;
    private final float critChanceAdd;
    private final float critMultMin;
    private final float cdrAdd;
    private final float moveSpeedMult;
    private final float manaCapAdd;
    private final float summonBonus;
    private final float reactionBonus;

    private AscensionVow(Builder b) {
        this.id = b.id;
        this.stage = b.stage;
        this.benefitKey = "vow.transcend." + b.id + ".benefit";
        this.costKey = "vow.transcend." + b.id + ".cost";
        this.spellDamageBonus = b.spellDamageBonus;
        this.circleUpkeepMult = b.circleUpkeepMult;
        this.circleLimitAdd = b.circleLimitAdd;
        this.manaCostMult = b.manaCostMult;
        this.healingMult = b.healingMult;
        this.healthAdd = b.healthAdd;
        this.critChanceAdd = b.critChanceAdd;
        this.critMultMin = b.critMultMin;
        this.cdrAdd = b.cdrAdd;
        this.moveSpeedMult = b.moveSpeedMult;
        this.manaCapAdd = b.manaCapAdd;
        this.summonBonus = b.summonBonus;
        this.reactionBonus = b.reactionBonus;
    }

    public static Builder builder(String id, int stage) {
        return new Builder(id, stage);
    }

    // ===== Getters =====
    public String getId() { return id; }
    public int getStage() { return stage; }
    public String getTranslationKey() { return "vow.transcend." + id; }
    public String getBenefitKey() { return benefitKey; }
    public String getCostKey() { return costKey; }

    public float getSpellDamageBonus() { return spellDamageBonus; }
    public float getCircleUpkeepMult() { return circleUpkeepMult; }
    public int getCircleLimitAdd() { return circleLimitAdd; }
    public float getManaCostMult() { return manaCostMult; }
    public float getHealingMult() { return healingMult; }
    public int getHealthAdd() { return healthAdd; }
    public float getCritChanceAdd() { return critChanceAdd; }
    public float getCritMultMin() { return critMultMin; }
    public float getCdrAdd() { return cdrAdd; }
    public float getMoveSpeedMult() { return moveSpeedMult; }
    public float getManaCapAdd() { return manaCapAdd; }
    public float getSummonBonus() { return summonBonus; }
    public float getReactionBonus() { return reactionBonus; }

    /**
     * 誓约构建器 — 仅设置需要修改的字段，其余使用中性默认值。
     */
    public static class Builder {
        private final String id;
        private final int stage;
        // Neutral defaults: 0 for additive, 1.0 for multiplicative
        private float spellDamageBonus = 0f;
        private float circleUpkeepMult = 1.0f;
        private int circleLimitAdd = 0;
        private float manaCostMult = 1.0f;
        private float healingMult = 1.0f;
        private int healthAdd = 0;
        private float critChanceAdd = 0f;
        private float critMultMin = 0f;
        private float cdrAdd = 0f;
        private float moveSpeedMult = 1.0f;
        private float manaCapAdd = 0f;
        private float summonBonus = 0f;
        private float reactionBonus = 0f;

        public Builder(String id, int stage) {
            this.id = id;
            this.stage = stage;
        }

        public Builder spellDamage(float v) { this.spellDamageBonus = v; return this; }
        public Builder circleUpkeep(float v) { this.circleUpkeepMult = v; return this; }
        public Builder circleLimit(int v) { this.circleLimitAdd = v; return this; }
        public Builder manaCost(float v) { this.manaCostMult = v; return this; }
        public Builder healing(float v) { this.healingMult = v; return this; }
        public Builder health(int v) { this.healthAdd = v; return this; }
        public Builder critChance(float v) { this.critChanceAdd = v; return this; }
        public Builder critMult(float v) { this.critMultMin = v; return this; }
        public Builder cdr(float v) { this.cdrAdd = v; return this; }
        public Builder moveSpeed(float v) { this.moveSpeedMult = v; return this; }
        public Builder manaCap(float v) { this.manaCapAdd = v; return this; }
        public Builder summon(float v) { this.summonBonus = v; return this; }
        public Builder reaction(float v) { this.reactionBonus = v; return this; }

        public AscensionVow build() {
            return new AscensionVow(this);
        }
    }
}
