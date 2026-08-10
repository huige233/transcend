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

/** 魔力水库方块实体渲染器。 */
public class ManaReservoirRenderer implements BlockEntityRenderer<ManaReservoirBlockEntity> {

    public ManaReservoirRenderer(BlockEntityRendererProvider.Context ctx) {

    }

    @Override
    public void render(ManaReservoirBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) return;

        long gameTime = level.getGameTime();
        float time = gameTime + partialTick;

        int stored = be.getManaStorage().getManaStored();
        int capacity = be.getCapacity();
        float fillRatio = capacity > 0 ? (float) stored / capacity : 0f;
        int displayCount;
        if (fillRatio >= 0.66f)      displayCount = 3;
        else if (fillRatio >= 0.33f) displayCount = 2;
        else                         displayCount = 1;

        float baseY = 0.78f + fillRatio * 0.04f;

        ItemStack crystalStack = new ItemStack(ModItems.magic_crystal.get());
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();

        for (int i = 0; i < displayCount; i++) {
            poseStack.pushPose();

            float baseAngle = displayCount > 1
                    ? i * (float) (Math.PI * 2.0 / displayCount)
                    : 0f;
            float orbitSpin = time * 0.02f;
            float angle = baseAngle + orbitSpin;
            float radius = displayCount > 1 ? 0.24f : 0f;

            float xOff = (float) Math.cos(angle) * radius;
            float zOff = (float) Math.sin(angle) * radius;

            float bob = (float) Math.sin((time + i * 30) * 0.04) * 0.04f;

            poseStack.translate(0.5 + xOff, baseY + bob, 0.5 + zOff);
            poseStack.scale(0.28f, 0.28f, 0.28f);

            poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.5f));

            renderer.renderStatic(crystalStack, ItemDisplayContext.GROUND,
                    packedLight, packedOverlay, poseStack, buffer, level, 0);

            poseStack.popPose();
        }
    }
}
