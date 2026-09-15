package com.mega.uom.event.eventhandler.client;

import com.mega.uom.ModSource;
import com.mega.uom.auto.AutoRegisterManager;
import com.mega.uom.client.CustomParticleRenderHandler;
import com.mega.uom.client.baked.loader.CosmicModelLoader;
import com.mega.uom.client.baked.loader.SeparateTransformsCosmicModelLoader;
import com.mega.uom.client.baked.loader.VanillaCosmicModelLoader;
import com.mega.uom.client.component.LoreHelper;
import com.mega.uom.client.render.DeferredWorldEffectRenderer;
import com.mega.uom.client.render.MegaRenderBuffers;
import com.mega.uom.client.render.MyGuiGraphics;
import com.mega.uom.client.render.RendererUtils;
import com.mega.uom.client.render.armor.FadedDragonRenderer;
import com.mega.uom.client.render.curios.DomainOfFadeRenderer;
import com.mega.uom.client.render.shader.CullWrappedRenderLayer;
import com.mega.uom.client.render.shader.GlowRenderLayer;
import com.mega.uom.client.render.shader.MegaRenderType;
import com.mega.uom.client.render.shader.core.ModShaders;
import com.mega.uom.client.render.shader.cosmic.CosmicItemShaders;
import com.mega.uom.client.render.shader.cosmic.VanillaCosmicShaders;
import com.mega.uom.common.capability.ModCapabilities;
import com.mega.uom.common.capability.entity.runic.IRunicShieldCapability;
import com.mega.uom.common.items.SpecialItemCheck;
import com.mega.uom.common.items.armor.FadedDragonItem;
import com.mega.uom.common.items.armor.FantasyEndingArmorItem;
import com.mega.uom.common.items.combat.sword.FantasyEndingSword;
import com.mega.uom.common.items.curios.TheDomainOfFadeCurio;
import com.mega.uom.common.items.magic.spell_books.UltimateMagicSpellBook;
import com.mega.uom.common.items.tools.DreamShadowAxe;
import com.mega.uom.common.items.tools.DreamShadowPickaxe;
import com.mega.uom.common.items.tools.DreamShadowShovel;
import com.mega.uom.common.register.TargetRegister;
import com.mega.uom.compat.SafeClass;
import com.mega.uom.event.ClientProgramTickEvent;
import com.mega.uom.event.render.RenderUpdateEvent;
import com.mega.uom.event.screen.FeRenderTooltipEvent;
import com.mega.uom.util.entity.EntityViewUtils;
import com.mega.uom.util.entity.PlayerInvulnerableEntityData;
import com.mega.uom.util.itf.spell.AbstractSpellInterface;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.render.SpellBookCurioRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

import java.awt.*;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientHandler {
    public static final float faded_dragon_layer_alpha = 0.58F;
    public static float rotationStar = 0F;
    public static boolean inventoryRender = false;
    public static ResourceLocation BAR = new ResourceLocation("fantasy_ending", "textures/entity/bossbar/base.png");
    public static Minecraft mc = Minecraft.getInstance();
    static FadedDragonRenderer renderer;
    static Vector3f randColor = new Vector3f();

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void drawScreenPre(ScreenEvent.Render.Pre e) {
        inventoryRender = true;
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void drawScreenPost(ScreenEvent.Render.Post e) {
        inventoryRender = false;
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeGuiDraw(ScreenEvent.Render.Pre pre) {
        if (FantasyEndingArmorItem.shouldNotDead(Minecraft.getInstance().player) && pre.getScreen() instanceof DeathScreen screen) {
            mc.mouseHandler.grabMouse();
            mc.getSoundManager().resume();
            pre.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void modifyItemTooltipFe(FeRenderTooltipEvent.PrePre event) {
        if (SpecialItemCheck.isFeItem(event.getItemStack())) {
            ClientHooks.tooltipFeItem(event);
            ClientHooks.isFeItemTooltipNow = true;
            return;
        }
        ClientHooks.isFeItemTooltipNow = false;
    }
    @SubscribeEvent
    public static void modifyItemTooltipDs_Sp(FeRenderTooltipEvent.Post event) {
        if (SpecialItemCheck.isDsItem(event.getItemStack()) || SpecialItemCheck.isSpItem(event.getItemStack()))
            ClientHooks.tooltipDsSpItem(event);
    }

    @SubscribeEvent
    public static void cancelItemTooltipBackground(RenderTooltipEvent.Color event) {
        if (SpecialItemCheck.isFeItem(event.getItemStack()) || SpecialItemCheck.isDsItem(event.getItemStack()) || SpecialItemCheck.isSpItem(event.getItemStack())) {
            event.setBorderStart(0);
            event.setBorderEnd(0);
            event.setBackgroundStart(0);
            event.setBackgroundEnd(0);
        }
    }

    @SubscribeEvent
    public static void modifyTooltipColor(RenderTooltipEvent.Color event) {
    }

    @SubscribeEvent
    public static void renderPlayer(RenderPlayerEvent event) {
        if (SafeClass.isRenderingShaderShadowPass()) return;
        Player player = event.getEntity();
        if (mc.player == null) return;
        if (player.isUsingItem()) {
            if (player.getUseItem().is(TargetRegister.ds_sword()) || player.getUseItem().is(AutoRegisterManager.ITEM_ARH().get(FantasyEndingSword.class))) {
                float partialTick = event.getPartialTick();
                if (DeferredWorldEffectRenderer.shouldDefer()) {
                    DeferredWorldEffectRenderer.defer(event.getPoseStack(), (matrix, bufferSource) -> renderPlayerUseItemEffect(player, partialTick, matrix, bufferSource));
                } else {
                    renderPlayerUseItemEffect(player, partialTick, event.getPoseStack(), Minecraft.getInstance().renderBuffers().bufferSource());
                }
            }
        }
    }

    private static void renderPlayerUseItemEffect(Player player, float partialTick, PoseStack matrix, MultiBufferSource.BufferSource bufferSource) {
        float size = .9F;
        float partial = 1.5F;
        matrix.pushPose();
        matrix.scale(size, size * partial, size);
        Vec3 vec3 = Vec3.directionFromRotation(0, Mth.lerp(Minecraft.getInstance().getPartialTick(), player.yHeadRotO, player.yHeadRot));
        matrix.translate(vec3.x * .5F, partial / 2F * size, vec3.z * .5F);
        matrix.mulPose(Axis.XP.rotationDegrees(90));
        matrix.mulPose(Axis.ZP.rotationDegrees(player.getYHeadRot() - 30));
        RenderType glowRenderLayer = new GlowRenderLayer(new CullWrappedRenderLayer(MegaRenderType.createSphereRenderType2(RendererUtils.beam)), null, 1, false);
        RendererUtils.renderSphere(matrix, bufferSource, size, 20, mc.getEntityRenderDispatcher().getPackedLightCoords(player, partialTick), .3f, .3f, .3f, .25f, glowRenderLayer, .7F, true);
        matrix.popPose();
    }

    @SubscribeEvent
    public static void doRunicShieldRender(RenderLivingEvent.Post<LivingEntity, ?> event) {
        IRunicShieldCapability capability;
        LivingEntity living = event.getEntity();
        if (SafeClass.isRenderingShaderShadowPass()) return;
        if (SafeClass.isYSMLoaded()) return;
        if ((capability = ModCapabilities.getCapability(living, ModCapabilities.FE_SHIELD_EC)) != null && capability.getLevel() > 0) {

            float size = .9F;
            float partial = 1.5F;
            if (FantasyEndingArmorItem.is4Armor(living)) {
                float partialHurtTime = capability.hurtTime() - mc.getPartialTick();
                if (partialHurtTime > 0F) {
                    double sizeMulti = event.getEntity().getBoundingBox().getSize();
                    float alpha = partialHurtTime / 10;
                    float partialTick = event.getPartialTick();
                    if (DeferredWorldEffectRenderer.shouldDefer()) {
                        DeferredWorldEffectRenderer.defer(event.getPoseStack(), (matrix, bufferSource) -> renderRunicShieldEffect(living, partialTick, matrix, bufferSource, size, partial, sizeMulti, alpha));
                    } else {
                        MultiBufferSource.BufferSource bufferSource = MegaRenderBuffers.getBufferSource();
                        renderRunicShieldEffect(living, partialTick, event.getPoseStack(), bufferSource, size, partial, sizeMulti, alpha);
                        bufferSource.endLastBatch();
                    }
                }
            }
        } else if ((capability = ModCapabilities.getCapability(living, ModCapabilities.MAGIC_SHIELD_EC)) != null) {
            if (capability.getLevel() > 0 && capability.tickCount() < capability.maxLifeTime()) {
                float size = .9F;
                float partial = 1.5F;
                float partialHurtTime = capability.hurtTime() - mc.getPartialTick();
                if (partialHurtTime > 0F) {
                    double sizeMulti = event.getEntity().getBoundingBox().getSize();
                    float alpha = partialHurtTime / 10;
                    float[] color = new Color(capability.getRunicColor()).getColorComponents(null);
                    float partialTick = event.getPartialTick();
                    if (DeferredWorldEffectRenderer.shouldDefer()) {
                        DeferredWorldEffectRenderer.defer(event.getPoseStack(), (matrix, bufferSource) -> renderMagicShieldEffect(living, partialTick, matrix, bufferSource, size, partial, sizeMulti, alpha, color));
                    } else {
                        MultiBufferSource.BufferSource bufferSource = MegaRenderBuffers.getBufferSource();
                        RenderType glowRenderLayer = renderMagicShieldEffect(living, partialTick, event.getPoseStack(), bufferSource, size, partial, sizeMulti, alpha, color);
                        bufferSource.endBatch(glowRenderLayer);
                    }
                }
            }
        }
        if (false)
            if (event.getRenderer().getModel() instanceof HumanoidModel<?> humanoidModel && (PlayerInvulnerableEntityData.isInvul(living) || FantasyEndingArmorItem.is4Armor(living))) {
            label0 :
            {
                if (SafeClass.isYSMLoaded() && living == mc.player)
                    break label0;
                boolean invisible = mc.player != null && living.isInvisibleTo(mc.player);
                boolean flag = living instanceof Player player && player.isSpectator();
                if (!invisible) {
                    if (flag) return;
                    float partial = event.getPartialTick();
                    boolean crouching = living.getPose() == Pose.CROUCHING;
                    if (renderer == null) renderer = new FadedDragonRenderer();
                    PoseStack stack = new PoseStack();
                    stack.mulPoseMatrix(event.getPoseStack().last().pose());
                    renderer.withScale(0.3F);
                    ClientHooks.afterLevelBeforeRenderStackModify(stack, event.getRenderer(), partial, crouching, living);
                    stack.pushPose();
                    RenderType renderType = MegaRenderType.entityTranslucent(new ResourceLocation(ModSource.MODID, "textures/item/armor/faded_dragon.png"));
                    VertexConsumer buffer = MegaRenderBuffers.getBufferSource().getBuffer(renderType);
                    renderer.prepForRender(living, new ItemStack(AutoRegisterManager.ITEM_ARH().get(FadedDragonItem.class)), EquipmentSlot.CHEST, humanoidModel);
                    renderer.renderToBuffer(stack, buffer, mc.getEntityRenderDispatcher().getPackedLightCoords(living, event.getPartialTick()), OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
                    MegaRenderBuffers.getBufferSource().endBatch(renderType);
                    stack.popPose();
                }
            }
        }
    }

    private static void renderRunicShieldEffect(LivingEntity living, float partialTick, PoseStack matrix, MultiBufferSource.BufferSource bufferSource, float size, float partial, double sizeMulti, float alpha) {
        matrix.pushPose();
        matrix.scale((float) (size * sizeMulti), (float) (size * partial * sizeMulti), (float) (size * sizeMulti));
        matrix.translate(0F, partial / 2F * size, 0F);
        matrix.mulPose(Axis.XP.rotationDegrees(90));
        matrix.mulPose(Axis.ZP.rotationDegrees(living.getYHeadRot() - 30));
        RenderType glowRenderLayer = new GlowRenderLayer(new CullWrappedRenderLayer(MegaRenderType.createSphereRenderType2(RendererUtils.beam)), null, 1, false);
        RendererUtils.renderRainbowSphere(matrix, bufferSource, size, 20, mc.getEntityRenderDispatcher().getPackedLightCoords(living, partialTick), randColor.x, randColor.y, randColor.z, alpha * 0.45F, glowRenderLayer, 1F);
        matrix.popPose();
    }

    private static RenderType renderMagicShieldEffect(LivingEntity living, float partialTick, PoseStack matrix, MultiBufferSource.BufferSource bufferSource, float size, float partial, double sizeMulti, float alpha, float[] color) {
        matrix.pushPose();
        matrix.scale((float) (size * sizeMulti), (float) (size * partial * sizeMulti), (float) (size * sizeMulti));
        matrix.translate(0F, partial / 2F * size, 0F);
        matrix.mulPose(Axis.XP.rotationDegrees(90));
        matrix.mulPose(Axis.ZP.rotationDegrees(living.getYHeadRot() - 30));
        RenderType glowRenderLayer = new GlowRenderLayer(new CullWrappedRenderLayer(MegaRenderType.createSphereRenderType2(RendererUtils.beam)), null, 1, false);
        RendererUtils.renderSphere(matrix, bufferSource, size, 20, mc.getEntityRenderDispatcher().getPackedLightCoords(living, partialTick), color[0], color[1], color[2], .25f * alpha, glowRenderLayer, 1F, true);
        RendererUtils.renderSphere(matrix, bufferSource, size * 0.95F, 20, mc.getEntityRenderDispatcher().getPackedLightCoords(living, partialTick), color[0], color[1], color[2], .15f * alpha, glowRenderLayer, 1F, true);
        matrix.popPose();
        return glowRenderLayer;
    }

    @SubscribeEvent
    public static void render3x3BlockOutline$DSP(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            ItemStack stack = mc.player.getMainHandItem();
            if (stack.is(TargetRegister.DREAM_SHADOW_METAL) && stack.getItem() instanceof DiggerItem diggerItem) {
                BlockState stateTarget = mc.level.getBlockState(event.getTarget().getBlockPos());
                BlockPos[] pos = EntityViewUtils.get3x3BlocksWithDirection(mc.player, event.getTarget().getBlockPos());
                Predicate<Block> predicate = (b) -> true;
                if (diggerItem instanceof DreamShadowPickaxe) {
                    if (stateTarget.getBlock().defaultDestroyTime() < .1F || !mc.player.isShiftKeyDown())
                        return;
                    predicate = DreamShadowPickaxe.predicate;
                } else if (diggerItem instanceof DreamShadowShovel) {
                    predicate = DreamShadowShovel.predicate;
                } else if (diggerItem instanceof DreamShadowAxe) {
                    if (stateTarget.getBlock().defaultDestroyTime() < .1F || !mc.player.isShiftKeyDown())
                        return;
                    predicate = DreamShadowAxe.predicate;
                }
                Vec3 vec3 = mc.gameRenderer.getMainCamera().getPosition();
                for (BlockPos blockPos : pos) {
                    BlockState state = mc.level.getBlockState(blockPos);
                    if (state.getBlock().defaultDestroyTime() > 0 && predicate.test(stateTarget.getBlock()))
                        renderShape(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.lines()), state.getShape(mc.level, blockPos, CollisionContext.of(mc.player)), blockPos.getX() - vec3.x, blockPos.getY() - vec3.y, blockPos.getZ() - vec3.z, 1f, 1f, 1f, .7f);
                }
            }
        }
    }

    @SubscribeEvent
    public static void renderDiggingTooltip$DSP(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        if (mc.hitResult instanceof BlockHitResult blockHitResult && mc.player != null && mc.level != null) {
            ItemStack stack = mc.player.getMainHandItem();
            if (stack.is(TargetRegister.DREAM_SHADOW_METAL) && stack.getItem() instanceof DiggerItem diggerItem) {
                BlockPos[] pos = EntityViewUtils.get3x3BlocksWithDirection(mc.player, blockHitResult.getBlockPos());
                int count = 0;
                if (diggerItem instanceof DreamShadowPickaxe) {
                    if (mc.level.getBlockState(blockHitResult.getBlockPos()).getBlock().defaultDestroyTime() < .1f || !mc.player.isShiftKeyDown())
                        return;
                    count = Math.toIntExact(Arrays.stream(pos).filter(bp -> mc.level.getBlockState(bp).getBlock().defaultDestroyTime() > 0 && DreamShadowPickaxe.predicate.test(mc.level.getBlockState(bp).getBlock())).count());

                } else if (diggerItem instanceof DreamShadowShovel) {
                    count = Math.toIntExact(Arrays.stream(pos).filter(bp -> {
                        BlockState state = mc.level.getBlockState(bp);
                        return state.getBlock().defaultDestroyTime() > 0 && DreamShadowShovel.predicate.test(mc.level.getBlockState(bp).getBlock());
                    }).count());
                } else if (diggerItem instanceof DreamShadowAxe) {
                    if (mc.level.getBlockState(blockHitResult.getBlockPos()).getBlock().defaultDestroyTime() < .1f || !mc.player.isShiftKeyDown())
                        return;
                    count = Math.toIntExact(Arrays.stream(pos).filter(bp -> mc.level.getBlockState(bp).getBlock().defaultDestroyTime() > 0 && DreamShadowAxe.predicate.test(mc.level.getBlockState(bp).getBlock())).count());
                }
                MyGuiGraphics.create().renderTooltip(mc.font, LoreHelper.get3x3Tooltip(count), mc.getWindow().getGuiScaledWidth() / 2 + 2, mc.getWindow().getGuiScaledHeight() / 2 + 12, FastColor.ARGB32.color(0, 0, 0, 0), FastColor.ARGB32.color(350, 66, 66, 66), FastColor.ARGB32.color(350, 66, 66, 66));

            }
        }
    }

    private static void renderShape(PoseStack p_109783_, VertexConsumer p_109784_, VoxelShape p_109785_, double p_109786_, double p_109787_, double p_109788_, float p_109789_, float p_109790_, float p_109791_, float p_109792_) {
        PoseStack.Pose posestack$pose = p_109783_.last();
        p_109785_.forAllEdges((p_234280_, p_234281_, p_234282_, p_234283_, p_234284_, p_234285_) -> {
            float f = (float) (p_234283_ - p_234280_);
            float f1 = (float) (p_234284_ - p_234281_);
            float f2 = (float) (p_234285_ - p_234282_);
            float f3 = Mth.sqrt(f * f + f1 * f1 + f2 * f2);
            f /= f3;
            f1 /= f3;
            f2 /= f3;
            p_109784_.vertex(posestack$pose.pose(), (float) (p_234280_ + p_109786_), (float) (p_234281_ + p_109787_), (float) (p_234282_ + p_109788_)).color(p_109789_, p_109790_, p_109791_, p_109792_).normal(posestack$pose.normal(), f, f1, f2).endVertex();
            p_109784_.vertex(posestack$pose.pose(), (float) (p_234283_ + p_109786_), (float) (p_234284_ + p_109787_), (float) (p_234285_ + p_109788_)).color(p_109789_, p_109790_, p_109791_, p_109792_).normal(posestack$pose.normal(), f, f1, f2).endVertex();
        });
    }

    @SubscribeEvent
    public static void setOriginRaritySpell(ModifySpellLevelEvent event) {
        if (event.getEntity() != null && event.getEntity().level().isClientSide) {
            if (event.getLevel() > event.getSpell().getMaxLevel())
                ((AbstractSpellInterface) event.getSpell()).uom$setClientOriginRarity(true);
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class TickEventsHandler {
        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
        }

        @SubscribeEvent
        public static void clientTick(ClientProgramTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                if (mc.level != null) {
                    ClientHooks.lastTick = ClientHooks.tick;
                    if (ClientHooks.isFeItemTooltipNow) {
                        if (ClientHooks.tick < 20) {
                            ClientHooks.tick++;
                        }
                    } else ClientHooks.tick = 0;
                }
            }
        }

        @SubscribeEvent
        public static void insertGameRender(TickEvent.RenderTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null && mc.cameraEntity != null) {

                    try {
                        CustomParticleRenderHandler.runAllTasks(mc, new PoseStack(), event.renderTickTime);
                    } catch (Throwable throwable) {
                        if (!(throwable instanceof ConcurrentModificationException))
                            throwable.printStackTrace();
                    }
                }
            }
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class FMLSetupClient {
        @SubscribeEvent
        public static void propertyOverrideRegistry(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                for (RegistryObject<Item> registryObject : AutoRegisterManager.ITEM_ARH().c2roMap.values()) {
                    if (registryObject.get() instanceof BowItem bow) {
                        ItemProperties.register(bow, new ResourceLocation("pulling"), (itemStack, clientWorld, livingEntity, i)
                                -> livingEntity != null && livingEntity.isUsingItem() && livingEntity.getUseItem() == itemStack ? 1.0F : 0.0F);
                        ItemProperties.register(bow, new ResourceLocation("pull"), (itemStack, clientWorld, livingEntity, i) -> {
                            if (livingEntity == null) {
                                return 0.0F;
                            } else {
                                return livingEntity.getUseItem() != itemStack ? 0.0F : (float) (itemStack.getUseDuration() - livingEntity.getUseItemRemainingTicks()) / 20.0F;
                            }
                        });
                    }
                }
            });
            event.enqueueWork(() -> {
                if (SafeClass.isCuriosLoaded()) {
                    CuriosRendererRegistry.register(AutoRegisterManager.ITEM_ARH().get(TheDomainOfFadeCurio.class), DomainOfFadeRenderer::new);
                    CuriosRendererRegistry.register(AutoRegisterManager.ITEM_ARH().get(UltimateMagicSpellBook.class), SpellBookCurioRenderer::new);
                }
            });
        }

    }

    @SuppressWarnings("DataFlowIssue")
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class PostEffectEvents {
        public static long time = 0L;

        @SubscribeEvent
        public static void modifyUniforms(RenderUpdateEvent event) {
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerLoaders(ModelEvent.RegisterGeometryLoaders event) {
            event.register("cosmic_mask_loader", CosmicModelLoader.INSTANCE);
            event.register("cosmic_vanilla_loader", VanillaCosmicModelLoader.INSTANCE);
            event.register("cosmic_st_loader", SeparateTransformsCosmicModelLoader.INSTANCE);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onRegisterShaders(RegisterShadersEvent event) {
            CosmicItemShaders.onRegisterShaders(event);
            VanillaCosmicShaders.onRegisterShaders(event);
            ModShaders.onRegisterShaders(event);
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class MusicEvents {
    }

}
