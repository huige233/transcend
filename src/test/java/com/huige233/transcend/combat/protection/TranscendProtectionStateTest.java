package com.huige233.transcend.combat.protection;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 验证装备保护与临时无敌独立生效、终结标记优先以及无敌窗口到期后的保护失效。 */
class TranscendProtectionStateTest {
 @Test void equipmentAndWindowAreIndependent(){
  var s=new TranscendProtectionState(true,false,10);
  assertTrue(s.permanentEquipmentProtection()); assertTrue(s.temporaryProtection());
  assertTrue(s.protectedNow());
 }
 @Test void terminalMarkWins(){
  var s=new TranscendProtectionState(true,true,300);
  assertTrue(s.equipmentEligible()); assertTrue(s.terminalMarkWins());
  assertFalse(s.permanentEquipmentProtection()); assertFalse(s.temporaryProtection()); assertFalse(s.protectedNow());
 }
 @Test void expiredWindowDoesNotProtect(){
  var s=new TranscendProtectionState(false,false,0);
  assertFalse(s.protectedNow());
 }
}
