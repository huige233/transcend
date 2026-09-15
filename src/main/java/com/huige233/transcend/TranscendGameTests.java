package com.huige233.transcend;

import com.huige233.transcend.combat.attack.AttackLevel;
import com.huige233.transcend.combat.attack.AttackProfile;
import com.huige233.transcend.items.tech.ParticleGun;
import com.huige233.transcend.init.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;


/** 在无界面的服务端游戏测试中检查粒子枪默认提示和各攻击等级的伤害标签。 */
@net.minecraftforge.gametest.GameTestHolder(Transcend.MODID)
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class TranscendGameTests {
    private TranscendGameTests() {}

    
    @GameTest(template = "empty", timeoutTicks = 10)
    public static void particleGunDefaultTooltip(GameTestHelper helper) {
        List<Component> tooltip = new ArrayList<>();
        try {
            ModItems.particle_gun.get().appendHoverText(new ItemStack(ModItems.particle_gun.get()),
                    helper.getLevel(), tooltip, net.minecraft.world.item.TooltipFlag.NORMAL);
            if (tooltip.isEmpty()) throw new AssertionError("particle gun tooltip was empty");
            helper.succeed();
        } catch (Throwable failure) {
            helper.fail("default particle-gun tooltip threw: " + failure);
        }
    }

    
    @GameTest(template = "empty", timeoutTicks = 10)
    public static void attackLevelDamageTags(GameTestHelper helper) {
        try {
            for (AttackLevel level : AttackLevel.values()) {
                if (level == AttackLevel.HARD_DELETE || level == AttackLevel.WORLD_PURGE) continue;
                DamageSource source = TranscendDamage.attack(helper.getLevel(),
                        AttackProfile.of(level, 1.0F, null));
                AttackProfile expected = AttackProfile.of(level, 1.0F, null);
                check(source.is(ModDamageTypes.attack(level)), level + " registry key");
                check(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR) == expected.bypassesArmor(), level + " armor tag");
                check(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_RESISTANCE) == expected.bypassesResistance(), level + " resistance tag");
                check(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ENCHANTMENTS) == expected.bypassesEnchantments(), level + " enchantment tag");
                check(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) == expected.bypassesInvulnerability(), level + " invulnerability tag");
            }
            helper.succeed();
        } catch (Throwable failure) {
            helper.fail("attack damage tags mismatch: " + failure);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
