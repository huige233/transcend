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

/**
 * R90: 造物之道 attribute 词条提供器 — 将 4 类 resonance socket 映射到 vanilla AttributeModifier。
 *
 * <p>玩家请求："我希望每一种强化方式都可以带来一条完全独立的增幅词条 / 一部分词条可以作为attribute"
 *
 * <h2>映射表</h2>
 * <pre>
 *   socket kind         attribute              operation       amount/socket
 *   ─────────────────────────────────────────────────────────────────────
 *   SHARPNESS (锋锐)    ATTACK_DAMAGE          MULTIPLY_BASE   +5% / socket   (武器: MAINHAND)
 *   WARD       (护佑)    ARMOR                  ADDITION        +0.5 / socket  (护甲: 装备槽)
 *   SWIFTNESS (敏疾)    MOVEMENT_SPEED         MULTIPLY_BASE   +2% / socket   (武器+护甲)
 *   FOCUS      (凝神)    MAX_HEALTH             ADDITION        +1.0 / socket  (护甲: 装备槽)
 * </pre>
 *
 * <h2>设计要点</h2>
 * <ul>
 *   <li><b>解耦</b>：迁移的 4 类 socket 已从 {@link ForgeBattleHandler} 移除，避免双重生效</li>
 *   <li><b>稳定 UUID</b>：每类 modifier 一个固定 UUID，amount 由 socket 数量决定 — 单次 addModifier 等价于 vanilla 装备的 ATTACK_DAMAGE_UUID 模式</li>
 *   <li><b>MULTIPLY_BASE</b>（SHARPNESS）保持比例：剑 base 7 × 5% × 4 = +1.4；裸拳 base 1 × 5% × 4 = +0.2 — 不会因 base 不同失衡</li>
 *   <li><b>ADDITION</b>（WARD, FOCUS）符合 vanilla 装备属性的默认 op（钻石护甲 +8 ARMOR 也是 ADDITION）</li>
 *   <li><b>非破坏性</b>：base item 的原始 modifiers 仍保留 — 我们只 add，不删除</li>
 * </ul>
 *
 * <h2>R35 数值约束</h2>
 * <p>相比 R87 hook 数值，本实现严格"不超出，可以少"：
 * <ul>
 *   <li>SHARPNESS：hook 原为 ×(1+0.05N) 作用于 final damage（叠乘 enchant）；attribute 仅作用于 base attack damage（不叠乘 enchant）— <b>实际效果略降</b></li>
 *   <li>WARD：hook 原为 +3%/socket 减伤（直接消减伤害）；attribute +0.5 ARMOR/socket 走 vanilla armor formula（受 toughness/穿甲影响）— <b>实际效果不超</b></li>
 *   <li>SWIFTNESS：hook 原为 SWIFTNESS_COOLDOWN_PER_SOCKET 占位未接入；本轮首次实施，+2% 移速/socket</li>
 *   <li>FOCUS：hook 原为 FOCUS_MANA_PER_SOCKET 占位未接入；本轮首次实施，+1 HP/socket（护甲）</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeAttributeProvider {

    // ── 稳定 UUID（每类 modifier 一个；amount 由 socket count 决定）─────
    private static final UUID UUID_SHARPNESS = UUID.fromString("a5e09f01-1111-4901-aaaa-000000000001");
    private static final UUID UUID_WARD      = UUID.fromString("a5e09f01-2222-4902-bbbb-000000000002");
    private static final UUID UUID_SWIFTNESS = UUID.fromString("a5e09f01-3333-4903-cccc-000000000003");
    private static final UUID UUID_FOCUS     = UUID.fromString("a5e09f01-4444-4904-dddd-000000000004");

    // ── 每 socket 的数值（与原 hook 等价或略降）──────────────────────
    public static final double SHARPNESS_PER_SOCKET = 0.05; // MULTIPLY_BASE
    public static final double WARD_PER_SOCKET      = 0.5;  // ADDITION
    public static final double SWIFTNESS_PER_SOCKET = 0.02; // MULTIPLY_BASE
    public static final double FOCUS_PER_SOCKET     = 1.0;  // ADDITION

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        if (!GearForgeData.isInPipeline(stack)) return;

        EquipmentSlot slot = event.getSlotType();
        GearCategory category = GearCategory.classify(stack);

        // 武器只在 MAINHAND 槽贡献 ATTACK_DAMAGE
        boolean isWeaponMainhand = (category == GearCategory.WEAPON && slot == EquipmentSlot.MAINHAND);

        // 护甲只在其自然槽（getEquipmentSlot）贡献 ARMOR/MAX_HEALTH
        boolean isArmorNaturalSlot = false;
        if (category == GearCategory.ARMOR && stack.getItem() instanceof ArmorItem armorItem) {
            isArmorNaturalSlot = (armorItem.getEquipmentSlot() == slot);
        }

        // 移速：武器 MAINHAND + 护甲在自然槽都贡献（玩家穿戴/手持时生效）
        boolean contributesMovement = isWeaponMainhand || isArmorNaturalSlot;

        // ── 统计 4 类 socket 数量 ─────────────────────────────────────
        int sharpness = 0, ward = 0, swiftness = 0, focus = 0;
        for (GearForgeData.ResonanceSocket socket : GearForgeData.getSockets(stack)) {
            String cid = socket.crystalId();
            if (ResonanceKind.SHARPNESS.id.equals(cid)) sharpness++;
            else if (ResonanceKind.WARD.id.equals(cid)) ward++;
            else if (ResonanceKind.SWIFTNESS.id.equals(cid)) swiftness++;
            else if (ResonanceKind.FOCUS.id.equals(cid)) focus++;
        }

        // ── 注入 modifier ────────────────────────────────────────────
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
