package com.huige233.transcend.combat.protection;


/** 保存装备资格、终结标记与复活保护时长，并计算终结标记优先的保护结果。 */
public record TranscendProtectionState(boolean equipmentEligible, boolean marked, int reviveTicks) {
    public boolean permanentEquipmentProtection() { return equipmentEligible && !marked; }
    public boolean temporaryProtection() { return reviveTicks > 0 && !marked; }
    public boolean protectedNow() { return permanentEquipmentProtection() || temporaryProtection(); }
    public boolean terminalMarkWins() { return marked; }
}
