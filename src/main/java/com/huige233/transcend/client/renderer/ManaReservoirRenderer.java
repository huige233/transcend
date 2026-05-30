package com.huige233.transcend.client.renderer;

import com.huige233.transcend.block.mana.ManaReservoirBlockEntity;
import com.huige233.transcend.init.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * R81: 魔力储液池方块实体渲染器 — 池面漂浮魔力水晶视觉。
 *
 * <p>玩家请求："魔力水晶可以做一个效果在上面浮动"
 *
 * <p>显示规则（基于 manaStored / capacity 比例）：
 * <ul>
 *   <li>fill = 0%  → 1 颗水晶（idle 装饰，让空池子也有"生气"）</li>
 *   <li>fill 0..33%  → 1 颗</li>
 *   <li>fill 33..66% → 2 颗</li>
 *   <li>fill 66..100%→ 3 颗</li>
 * </ul>
 *
 * <p>该 BER 同时为 {@code mana_reservoir} 和 {@code greater_mana_reservoir} 服务
 * （两者共用同一个 BE 类）。
 */
public class ManaReservoirRenderer implements BlockEntityRenderer<ManaReservoirBlockEntity> {

    public ManaReservoirRenderer(BlockEntityRendererProvider.Context ctx) {
        // 复用 ItemRenderer 即可，无 baked model 需要
    }

    @Override
    public void render(ManaReservoirBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) return;

        long gameTime = level.getGameTime();
        float time = gameTime + partialTick;

        // 根据填充率决定显示数量
        int stored = be.getManaStorage().getManaStored();
        int capacity = be.getCapacity();
        float fillRatio = capacity > 0 ? (float) stored / capacity : 0f;
        int displayCount;
        if (fillRatio >= 0.66f)      displayCount = 3;
        else if (fillRatio >= 0.33f) displayCount = 2;
        else                         displayCount = 1;  // 空也至少 1 颗装饰

        // 高度跟随填充率：fill=0 时 y=0.78（贴近液面），fill=1 时 y=0.85
        // greater 模型液面 y=12/16=0.75；小池液面 y=11/16≈0.6875
        // 用 0.78 起点 + 0.07 浮动余量适应两个尺寸
        float baseY = 0.78f + fillRatio * 0.04f;

        ItemStack crystalStack = new ItemStack(ModItems.magic_crystal.get());
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();

        for (int i = 0; i < displayCount; i++) {
            poseStack.pushPose();

            // 公转：N 颗均分 360° + 时间偏移
            float baseAngle = displayCount > 1
                    ? i * (float) (Math.PI * 2.0 / displayCount)
                    : 0f;
            float orbitSpin = time * 0.02f;
            float angle = baseAngle + orbitSpin;
            float radius = displayCount > 1 ? 0.24f : 0f;

            float xOff = (float) Math.cos(angle) * radius;
            float zOff = (float) Math.sin(angle) * radius;
            // Y bob：每颗水晶相位错开
            float bob = (float) Math.sin((time + i * 30) * 0.04) * 0.04f;

            poseStack.translate(0.5 + xOff, baseY + bob, 0.5 + zOff);
            poseStack.scale(0.28f, 0.28f, 0.28f);
            // 自转
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.5f));

            renderer.renderStatic(crystalStack, ItemDisplayContext.GROUND,
                    packedLight, packedOverlay, poseStack, buffer, level, 0);

            poseStack.popPose();
        }
    }
}
