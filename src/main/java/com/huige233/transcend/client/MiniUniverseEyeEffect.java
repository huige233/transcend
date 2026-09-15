package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.block.MiniUniverseGeneratorBlockEntity;
import com.huige233.transcend.client.renderer.TranscendRenderTypes;
import com.huige233.transcend.init.ModBlockEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


/** 为运行中的微型宇宙发电机渲染星空球壳、轨道行星与随能量消耗演变的中心天体。 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MiniUniverseEyeEffect implements BlockEntityRenderer<MiniUniverseGeneratorBlockEntity> {
    private static final double SHELL_RADIUS = .49;
    private static final int RING_SEGMENTS = 72;
    private static final int LATITUDE = 16;
    private static final int LONGITUDE = 32;
    private static final Vec3[][] UNIT_SPHERE = sphereMesh();
    private static final World[] WORLDS = {
            
            new World(.28, .23, .045, .22, -.18, .31, .4, .25f, .56f, .90f),
            new World(.33, .26, .055, -1.02, .34, -.23, 2.1, .72f, .45f, .24f),
            new World(.36, .29, .034, .66, 1.05, .17, 4.2, .48f, .28f, .78f),
            new World(.30, .34, .025, 1.28, -.72, -.13, 1.0, .28f, .72f, .64f),
            new World(.37, .24, .020, -.58, 2.35, .46, 3.3, .84f, .34f, .16f),
            new World(.32, .30, .030, .92, -2.05, -.19, 5.1, .22f, .76f, .82f)
    };

    public MiniUniverseEyeEffect(BlockEntityRendererProvider.Context context) {}

    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MINI_UNIVERSE_GENERATOR.get(), MiniUniverseEyeEffect::new);
    }

    @Override
    public void render(MiniUniverseGeneratorBlockEntity machine, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (machine.getLevel() == null || !machine.getBlockState().getValue(MiniUniverseGeneratorBlockEntity.ACTIVE)) return;
        double time = (machine.getLevel().getGameTime() + (double) partialTick) / 20.0;
        Vec3 center = Vec3.atLowerCornerOf(machine.getBlockPos()).add(.5, 1.61, .5);
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(center).normalize();
        boolean distant = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(center) > 32 * 32;
        poses.pushPose();
        poses.translate(.5, 1.61, .5);
        PoseStack.Pose pose = poses.last();
        
        
        VertexConsumer out = buffers.getBuffer(TranscendRenderTypes.phaseShield());
        double usage = machine.generationUsage();
        float collapse = (float) Math.max(0, (usage - .75) / .20);
        float starCollapse = usage >= .95 ? 0f : 1f;
        float solarBlackening = (float) Math.max(0, Math.min(1, (usage - .35) / .40));
        float sunVisible = 1f - solarBlackening;
        shell(out, pose, camera, time);
        
        
        if (sunVisible > 0) {
            sphere(out, pose, Vec3.ZERO, .050, 1f, .42f + .36f * sunVisible,
                    .05f, time, distant);
            sphere(out, pose, Vec3.ZERO, .064 + .022 * solarBlackening,
                    .95f * sunVisible, .18f * sunVisible, .015f, time, distant);
        }
        if (solarBlackening > 0) {
            sphere(out, pose, Vec3.ZERO, .046 + .025 * solarBlackening,
                    .008f, .006f, .014f, time, distant);
            sphere(out, pose, Vec3.ZERO, .073 + .018 * solarBlackening,
                    .16f * solarBlackening, .035f, .008f, time, distant);
        }
        if (starCollapse > 0) for (World world : WORLDS) {
            World visible = world.collapsed(collapse);
            orbit(out, pose, visible);
            Vec3 position = visible.position(time * visible.speed + visible.phase);
            sphere(out, pose, position, visible.radius, visible.red, visible.green, visible.blue, time, distant);
        }

        Vec3 right = camera.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < .001) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 up = right.cross(camera).normalize();
        
        int stars = distant ? 42 : 126;
        stars = Math.max(12, (int) (stars * starCollapse));
        for (int i = 0; i < stars; i++) {
            double u = (i + .5) / stars;
            double y = 1.0 - 2.0 * u;
            double azimuth = i * 2.399963229728653 + time * .012;
            double radial = .435 + .035 * Math.sin(i * 7.13) * Math.sin(i * 7.13);
            double horizontal = Math.sqrt(Math.max(0, 1 - y * y));
            Vec3 position = new Vec3(Math.cos(azimuth) * horizontal * radial,
                    y * radial, Math.sin(azimuth) * horizontal * radial).xRot(.45f);
            float sparkle = (float) (.38 + .16 * Math.sin(time * .7 + i));
            mote(out, pose, position, right, up, i % 13 == 0 ? .0045 : .0022,
                    .52f, .64f, .94f, sparkle);
            if (!distant && i % 5 == 0)
                mote(out, pose, position, right, up, .018, .18f, .12f, .40f, .025f);
        }
        
        
        outerGoldLines(out, pose, time);
        poses.popPose();
    }

    private static void outerGoldLines(VertexConsumer out, PoseStack.Pose pose, double time) {
        for (int ring = 0; ring < 2; ring++) {
            double tilt = ring == 0 ? -.62 : .62;
            double angularSpeed = ring == 0 ? .040 : -.032;
            for (int i = 0; i < RING_SEGMENTS; i++) {
                double a = i * Math.PI * 2 / RING_SEGMENTS + time * angularSpeed;
                double b = (i + 1) * Math.PI * 2 / RING_SEGMENTS + time * angularSpeed;
                Vec3 p = new Vec3(Math.cos(a) * .505, Math.sin(a) * .505, 0).xRot((float) tilt);
                Vec3 q = new Vec3(Math.cos(b) * .505, Math.sin(b) * .505, 0).xRot((float) tilt);
                quad(out, pose, p, q, q.scale(1.004), p.scale(1.004), .78f, .48f, .10f, .24f);
            }
        }
    }

    private static void shell(VertexConsumer out, PoseStack.Pose pose, Vec3 camera, double time) {
        for (Vec3[] face : UNIT_SPHERE) {
            for (Vec3 normal : face) {
                double facing = normal.dot(camera);
                
                float alpha = (float) (.05 + .94 * Math.max(0, Math.min(1, -facing * 6 + .5)));
                float rim = (float) Math.pow(1 - Math.abs(facing), 4);
                
                float glow = rim * (.55f + .15f * (float) Math.sin(machinePulse(time)));
                vertex(out, pose, normal.scale(SHELL_RADIUS), .006f + glow * .018f,
                        .008f + glow * .026f, .022f + glow * .070f, alpha);
            }
        }
    }

    private static double machinePulse(double time) { return time * .8; }

    private static void sphere(VertexConsumer out, PoseStack.Pose pose, Vec3 center, double radius,
                               float red, float green, float blue, double time, boolean distant) {
        Vec3 light = center.lengthSqr() < .0001 ? new Vec3(-.4, .7, .5).normalize() : center.scale(-1).normalize();
        
        for (int lat = 0; lat < LATITUDE; lat += distant ? 2 : 1) {
            for (int lon = 0; lon < LONGITUDE; lon += distant ? 2 : 1) {
                int step = distant ? 2 : 1;
                Vec3 a = UNIT_SPHERE[lat * LONGITUDE + lon][0];
                Vec3 b = UNIT_SPHERE[lat * LONGITUDE + (lon + step - 1)][1];
                Vec3 c = UNIT_SPHERE[(lat + step - 1) * LONGITUDE + (lon + step - 1)][2];
                Vec3 d = UNIT_SPHERE[(lat + step - 1) * LONGITUDE + lon][3];
                planetVertex(out, pose, a, center, radius, light, red, green, blue, time);
                planetVertex(out, pose, b, center, radius, light, red, green, blue, time);
                planetVertex(out, pose, c, center, radius, light, red, green, blue, time);
                planetVertex(out, pose, d, center, radius, light, red, green, blue, time);
            }
        }
    }

    private static void planetVertex(VertexConsumer out, PoseStack.Pose pose, Vec3 normal, Vec3 center,
                                     double radius, Vec3 light, float red, float green, float blue, double time) {
        float shade = (float) (.18 + .82 * Math.max(0, normal.dot(light)));
        float bands = (float) (.84 + .16 * Math.sin(normal.y * 28 + normal.x * 4 + time * .25));
        vertex(out, pose, center.add(normal.scale(radius)), red * shade * bands,
                green * shade * bands, blue * shade * bands, 1f);
    }

    private static void orbit(VertexConsumer out, PoseStack.Pose pose, World world) {
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double a = i * Math.PI * 2 / RING_SEGMENTS, b = (i + 1) * Math.PI * 2 / RING_SEGMENTS;
            Vec3 p = world.position(a), q = world.position(b);
            float alpha = (float) (.10 + .08 * Math.sin(a) * Math.sin(a));
            quad(out, pose, p, q, q.scale(1.006), p.scale(1.006), .28f, .34f, .55f, alpha);
        }
    }

    /** 保存微型行星的轨道、大小、颜色和运动参数，并计算轨道位置与坍缩后的轨道。 */
    private record World(double rx, double rz, double radius, double pitch, double yaw,
                         double speed, double phase, float red, float green, float blue) {
        Vec3 position(double angle) {
            double x = Math.cos(angle) * rx, z = Math.sin(angle) * rz;
            double tiltedZ = z * Math.cos(pitch);
            return new Vec3(x * Math.cos(yaw) - tiltedZ * Math.sin(yaw), z * Math.sin(pitch),
                    x * Math.sin(yaw) + tiltedZ * Math.cos(yaw));
        }
        World collapsed(float factor) {
            double scale = 1.0 - .82 * Math.max(0, Math.min(1, factor));
            return new World(rx * scale, rz * scale, radius, pitch, yaw, speed, phase, red, green, blue);
        }
    }

    private static Vec3[][] sphereMesh() {
        Vec3[][] faces = new Vec3[LATITUDE * LONGITUDE][4];
        for (int lat = 0; lat < LATITUDE; lat++) {
            double p = -Math.PI / 2 + lat * Math.PI / LATITUDE;
            double q = -Math.PI / 2 + (lat + 1) * Math.PI / LATITUDE;
            for (int lon = 0; lon < LONGITUDE; lon++) {
                double a = lon * Math.PI * 2 / LONGITUDE, b = (lon + 1) * Math.PI * 2 / LONGITUDE;
                faces[lat * LONGITUDE + lon] = new Vec3[]{point(p, a), point(p, b), point(q, b), point(q, a)};
            }
        }
        return faces;
    }

    private static Vec3 point(double pitch, double yaw) {
        return new Vec3(Math.cos(pitch) * Math.cos(yaw), Math.sin(pitch), Math.cos(pitch) * Math.sin(yaw));
    }

    private static void mote(VertexConsumer out, PoseStack.Pose pose, Vec3 center, Vec3 right, Vec3 up,
                             double radius, float red, float green, float blue, float alpha) {
        Vec3 horizontal = right.scale(radius), vertical = up.scale(radius);
        quad(out, pose, center.subtract(horizontal).subtract(vertical), center.add(horizontal).subtract(vertical),
                center.add(horizontal).add(vertical), center.subtract(horizontal).add(vertical), red, green, blue, alpha);
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             float red, float green, float blue, float alpha) {
        vertex(out, pose, a, red, green, blue, alpha);
        vertex(out, pose, b, red, green, blue, alpha);
        vertex(out, pose, c, red, green, blue, alpha);
        vertex(out, pose, d, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, Vec3 p,
                               float red, float green, float blue, float alpha) {
        out.vertex(pose.pose(), (float) p.x, (float) p.y, (float) p.z).color(red, green, blue, alpha).endVertex();
    }
}
