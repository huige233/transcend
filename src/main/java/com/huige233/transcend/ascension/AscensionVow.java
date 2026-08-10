package com.huige233.transcend.ascension;

/** 飞升誓言定义类。 */
public class AscensionVow {
    private final String id;
    private final int stage;
    private final String benefitKey;
    private final String costKey;
    private final String trialKey;

    private final float spellDamageBonus;
    private final float circleUpkeepMult;
    private final float manaCostMult;
    private final float healingMult;
    private final int healthAdd;
    private final float critChanceAdd;
    private final float critMultMin;
    private final float cdrAdd;
    private final float cooldownMult;
    private final float moveSpeedMult;
    private final float manaCapAdd;
    private final float reactionBonus;
    private final float manaRegenBonus;

    private AscensionVow(Builder b) {
        this.id = b.id;
        this.stage = b.stage;
        this.benefitKey = "vow.transcend." + b.id + ".benefit";
        this.costKey = "vow.transcend." + b.id + ".cost";
        this.trialKey = "vow.transcend." + b.id + ".trial";
        this.spellDamageBonus = b.spellDamageBonus;
        this.circleUpkeepMult = b.circleUpkeepMult;
        this.manaCostMult = b.manaCostMult;
        this.healingMult = b.healingMult;
        this.healthAdd = b.healthAdd;
        this.critChanceAdd = b.critChanceAdd;
        this.critMultMin = b.critMultMin;
        this.cdrAdd = b.cdrAdd;
        this.cooldownMult = b.cooldownMult;
        this.moveSpeedMult = b.moveSpeedMult;
        this.manaCapAdd = b.manaCapAdd;
        this.reactionBonus = b.reactionBonus;
        this.manaRegenBonus = b.manaRegenBonus;
    }

    public static Builder builder(String id, int stage) {
        return new Builder(id, stage);
    }

    public String getId() { return id; }
    public int getStage() { return stage; }
    public String getTranslationKey() { return "vow.transcend." + id; }
    public String getBenefitKey() { return benefitKey; }
    public String getCostKey() { return costKey; }
    public String getTrialKey() { return trialKey; }

    public float getSpellDamageBonus() { return spellDamageBonus; }
    public float getCircleUpkeepMult() { return circleUpkeepMult; }
    public float getManaCostMult() { return manaCostMult; }
    public float getHealingMult() { return healingMult; }
    public int getHealthAdd() { return healthAdd; }
    public float getCritChanceAdd() { return critChanceAdd; }
    public float getCritMultMin() { return critMultMin; }
    public float getCdrAdd() { return cdrAdd; }
    public float getCooldownMult() { return cooldownMult; }
    public float getMoveSpeedMult() { return moveSpeedMult; }
    public float getManaCapAdd() { return manaCapAdd; }
    public float getReactionBonus() { return reactionBonus; }
    public float getManaRegenBonus() { return manaRegenBonus; }

    private static float additivePhase(float v, boolean liberated) {
        return liberated ? Math.max(0f, v) : Math.min(0f, v);
    }

    private static float multLowGoodPhase(float v, boolean liberated) {
        return liberated ? Math.min(1.0f, v) : Math.max(1.0f, v);
    }

    private static float multHighGoodPhase(float v, boolean liberated) {
        return liberated ? Math.max(1.0f, v) : Math.min(1.0f, v);
    }

    public float spellDamageBonus(boolean liberated)  { return additivePhase(spellDamageBonus, liberated); }
    public int   healthAdd(boolean liberated)         { return (int) additivePhase(healthAdd, liberated); }
    public float manaCapAdd(boolean liberated)        { return additivePhase(manaCapAdd, liberated); }
    public float critChanceAdd(boolean liberated)     { return additivePhase(critChanceAdd, liberated); }
    public float cdrAdd(boolean liberated)            { return additivePhase(cdrAdd, liberated); }
    public float cooldownMult(boolean liberated)      { return multLowGoodPhase(cooldownMult, liberated); }
    public float reactionBonus(boolean liberated)     { return additivePhase(reactionBonus, liberated); }
    public float manaRegenBonus(boolean liberated)   { return additivePhase(manaRegenBonus, liberated); }

    public float critMultMin(boolean liberated)       { return liberated ? critMultMin : 0f; }
    public float manaCostMult(boolean liberated)      { return multLowGoodPhase(manaCostMult, liberated); }
    public float circleUpkeepMult(boolean liberated)  { return multLowGoodPhase(circleUpkeepMult, liberated); }
    public float healingMult(boolean liberated)       { return multHighGoodPhase(healingMult, liberated); }
    public float moveSpeedMult(boolean liberated)     { return multHighGoodPhase(moveSpeedMult, liberated); }

    public static class Builder {
        private final String id;
        private final int stage;

        private float spellDamageBonus = 0f;
        private float circleUpkeepMult = 1.0f;
        private float manaCostMult = 1.0f;
        private float healingMult = 1.0f;
        private int healthAdd = 0;
        private float critChanceAdd = 0f;
        private float critMultMin = 0f;
        private float cdrAdd = 0f;
        private float cooldownMult = 1.0f;
        private float moveSpeedMult = 1.0f;
        private float manaCapAdd = 0f;
        private float reactionBonus = 0f;
        private float manaRegenBonus = 0f;

        public Builder(String id, int stage) {
            this.id = id;
            this.stage = stage;
        }

        public Builder spellDamage(float v) { this.spellDamageBonus = v; return this; }
        public Builder circleUpkeep(float v) { this.circleUpkeepMult = v; return this; }
        public Builder manaCost(float v) { this.manaCostMult = v; return this; }
        public Builder healing(float v) { this.healingMult = v; return this; }
        public Builder health(int v) { this.healthAdd = v; return this; }
        public Builder critChance(float v) { this.critChanceAdd = v; return this; }
        public Builder critMult(float v) { this.critMultMin = v; return this; }
        public Builder cdr(float v) { this.cdrAdd = v; return this; }
        public Builder cooldown(float v) { this.cooldownMult = v; return this; }
        public Builder moveSpeed(float v) { this.moveSpeedMult = v; return this; }
        public Builder manaCap(float v) { this.manaCapAdd = v; return this; }
        public Builder reaction(float v) { this.reactionBonus = v; return this; }
        public Builder manaRegen(float v) { this.manaRegenBonus = v; return this; }

        public AscensionVow build() {
            return new AscensionVow(this);
        }
    }
}
