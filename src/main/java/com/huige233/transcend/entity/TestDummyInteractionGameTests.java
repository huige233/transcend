package com.huige233.transcend.entity;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.init.ModEntities;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** 验证假人装备交换与界面操作遵守同一所有权规则。 */
@GameTestHolder(Transcend.MODID)
@PrefixGameTestTemplate(false)
public final class TestDummyInteractionGameTests {
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void onlyOwnerCanSwapEquipment(GameTestHelper helper) {
        var dummy = new TestDummy(ModEntities.TEST_DUMMY.get(), helper.getLevel());
        var owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "dummy-owner"));
        var visitor = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "dummy-visitor"));
        helper.assertTrue(dummy.bindToCreator(owner), "owner bound");
        dummy.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        visitor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_HELMET));
        helper.assertTrue(dummy.mobInteract(visitor, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
                "visitor equipment exchange is denied");
        helper.assertTrue(dummy.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET)
                && visitor.getMainHandItem().is(Items.IRON_HELMET), "denied swap preserves both inventories");
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_HELMET));
        helper.assertTrue(dummy.mobInteract(owner, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME,
                "owner can still equip dummy");
        helper.assertTrue(dummy.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)
                && owner.getMainHandItem().is(Items.DIAMOND_HELMET), "owner receives previous equipment");
        visitor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        visitor.setShiftKeyDown(true);
        helper.assertTrue(dummy.mobInteract(visitor, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
                "visitor cannot open settings");
        helper.assertTrue(dummy.mobInteract(visitor, InteractionHand.OFF_HAND) == InteractionResult.PASS,
                "offhand cannot open duplicate settings");
        helper.succeed();
    }
}
