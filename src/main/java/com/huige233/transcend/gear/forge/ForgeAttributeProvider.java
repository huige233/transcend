package com.huige233.transcend.gear.forge;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.gear.GearCategory;
import com.huige233.transcend.gear.GearForgeData;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
/** 锻造属性提供者。 */
public class ForgeAttributeProvider {

    private static final UUID UUID_SHARPNESS = UUID.fromString("a5e09f01-1111-4901-aaaa-000000000001");
    private static final UUID UUID_WARD      = UUID.fromString("a5e09f01-2222-4902-bbbb-000000000002");
    private static final UUID UUID_SWIFTNESS = UUID.fromString("a5e09f01-3333-4903-cccc-000000000003");
    private static final UUID UUID_FOCUS     = UUID.fromString("a5e09f01-4444-4904-dddd-000000000004");

    public static final double SHARPNESS_PER_SOCKET = 0.05;
    public static final double WARD_PER_SOCKET      = 0.5;
    public static final double SWIFTNESS_PER_SOCKET = 0.02;
    public static final double FOCUS_PER_SOCKET     = 1.0;

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        if (!GearForgeData.isInPipeline(stack)) return;

        EquipmentSlot slot = event.getSlotType();
        GearCategory category = GearCategory.classify(stack);

        boolean isWeaponMainhand = (category == GearCategory.WEAPON && slot == EquipmentSlot.MAINHAND);

        boolean isArmorNaturalSlot = false;
        if (category == GearCategory.ARMOR && stack.getItem() instanceof ArmorItem armorItem) {
            isArmorNaturalSlot = (armorItem.getEquipmentSlot() == slot);
        }

        boolean contributesMovement = isWeaponMainhand || isArmorNaturalSlot;

        int sharpness = 0, ward = 0, swiftness = 0, focus = 0;
        for (GearForgeData.ResonanceSocket socket : GearForgeData.getSockets(stack)) {
            String cid = socket.crystalId();
            if (ResonanceKind.SHARPNESS.id.equals(cid)) sharpness++;
            else if (ResonanceKind.WARD.id.equals(cid)) ward++;
            else if (ResonanceKind.SWIFTNESS.id.equals(cid)) swiftness++;
            else if (ResonanceKind.FOCUS.id.equals(cid)) focus++;
        }

        if (isWeaponMainhand && sharpness > 0) {
            addMod(event, Attributes.ATTACK_DAMAGE, UUID_SHARPNESS,
                    "transcend.forge.sharpness",
                    sharpness * SHARPNESS_PER_SOCKET,
                    AttributeModifier.Operation.MULTIPLY_BASE);
        }
        if (isArmorNaturalSlot && ward > 0) {
            addMod(event, Attributes.ARMOR, UUID_WARD,
                    "transcend.forge.ward",
                    ward * WARD_PER_SOCKET,
                    AttributeModifier.Operation.ADDITION);
        }
        if (contributesMovement && swiftness > 0) {
            addMod(event, Attributes.MOVEMENT_SPEED, UUID_SWIFTNESS,
                    "transcend.forge.swiftness",
                    swiftness * SWIFTNESS_PER_SOCKET,
                    AttributeModifier.Operation.MULTIPLY_BASE);
        }
        if (isArmorNaturalSlot && focus > 0) {
            addMod(event, Attributes.MAX_HEALTH, UUID_FOCUS,
                    "transcend.forge.focus",
                    focus * FOCUS_PER_SOCKET,
                    AttributeModifier.Operation.ADDITION);
        }
    }

    private static void addMod(ItemAttributeModifierEvent event, Attribute attr, UUID uuid,
                                String name, double amount, AttributeModifier.Operation op) {
        if (amount == 0) return;
        event.addModifier(attr, new AttributeModifier(uuid, name, amount, op));
    }
}
