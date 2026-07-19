package com.huige233.transcend.entity.boss;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossVisualMigrationTest {
    private static final Path MAIN = Path.of("src/main/java/com/huige233/transcend");
    private static final List<String> SCOPED_CLASSES = List.of(
            "com/huige233/transcend/entity/SpellPillar.class",
            "com/huige233/transcend/entity/SpellGuardian.class",
            "com/huige233/transcend/entity/SpellWisp.class",
            "com/huige233/transcend/entity/nexus/NexusGuardian.class",
            "com/huige233/transcend/entity/nexus/NexusSentinel.class",
            "com/huige233/transcend/entity/boss/AbstractTranscendBoss.class",
            "com/huige233/transcend/entity/boss/PhaseDrivenBossBase.class",
            "com/huige233/transcend/entity/boss/ElementalWarden.class",
            "com/huige233/transcend/entity/boss/VoidWeaver.class",
            "com/huige233/transcend/entity/boss/TranscendenceAvatar.class",
            "com/huige233/transcend/entity/boss/BossParticleModel.class",
            "com/huige233/transcend/entity/boss/AvatarMeteorEffect.class",
            "com/huige233/transcend/entity/boss/AvatarMeteorEffectManager.class"
    );

    @Test
    void representativeEffectOrderAndStateAreCharacterizedBeforeMigration() throws IOException {
        String boss = source("entity/boss/AbstractTranscendBoss.java");
        assertInOrder(section(boss, "protected void setPhase", "public boolean hurt"),
                "for (int ring = 0; ring < 3; ++ring)",
                "12.0f + (float)(phaseNum * 2)",
                "this.getY() + 4.5",
                "sp.knockback");
        assertInOrder(section(boss, "protected void fireBeamSweep", "protected void groundSlam"),
                "30, \"beam\"", "for (int i = -3; i <= 3; ++i)", "12, \"slash\"");
        assertInOrder(section(boss, "protected void groundSlam", "protected DamageSource"),
                "40, 32, \"hexagram\"", "getEntitiesOfClass", "radius * 1.15", "20");

        String voidWeaver = source("entity/boss/VoidWeaver.java");
        assertInOrder(section(voidWeaver, "private void teleportNearTarget", "private void novaBlast"),
                "2.0F, 0.15F, 0.0F, 0.2F, 60, 22, \"pentagram\"",
                "this.bossTeleportTo",
                "2.4F, 0.25F, 0.03F, 0.35F, 80, 24, \"hexagram\"",
                "2.2F, 0.25F, 0.03F, 0.35F, 18",
                "2.8F, 0.25F, 0.03F, 0.35F, 16");

        String pillar = source("entity/SpellPillar.java");
        assertInOrder(section(pillar, "public void die", "public boolean isPushable"),
                "2.8F", "2.1F", "14, 24, \"hexagram\"", "center.add(0.0, 2.6, 0.0)",
                "12, \"beam\"", "GLASS_BREAK");

        String avatar = source("entity/boss/TranscendenceAvatar.java");
        assertInOrder(section(avatar, "private void summonMeteorCataclysm", "private void voidPrison"),
                "powerMult = (empowered ? 2.2F : 1.8F) * spellPower",
                "radiusMult = empowered ? 1.45F : 1.25F",
                "specialLevel = empowered ? 4 : 3",
                "lifetime = empowered ? 135 : 120",
                "WITHER_SPAWN");
    }

    @Test
    void scopedCompiledClassesDoNotLinkClientVisualCode() throws IOException {
        ClassLoader loader = BossVisualMigrationTest.class.getClassLoader();
        for (String classFile : SCOPED_CLASSES) {
            try (InputStream input = loader.getResourceAsStream(classFile)) {
                assertTrue(input != null, "Missing compiled class: " + classFile);
                String constantPool = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
                assertFalse(constantPool.contains("com/huige233/transcend/client/"),
                        classFile + " links a client class from common/server bytecode");
            }
        }
    }

    @Test
    void scopedCommonClassesLoadWithClientPackagesBlocked() throws ClassNotFoundException {
        Set<String> scoped = Set.of(
                "com.huige233.transcend.entity.SpellPillar",
                "com.huige233.transcend.entity.SpellGuardian",
                "com.huige233.transcend.entity.SpellWisp",
                "com.huige233.transcend.entity.nexus.NexusGuardian",
                "com.huige233.transcend.entity.nexus.NexusSentinel",
                "com.huige233.transcend.entity.boss.AbstractTranscendBoss",
                "com.huige233.transcend.entity.boss.PhaseDrivenBossBase",
                "com.huige233.transcend.entity.boss.ElementalWarden",
                "com.huige233.transcend.entity.boss.VoidWeaver",
                "com.huige233.transcend.entity.boss.TranscendenceAvatar",
                "com.huige233.transcend.entity.boss.BossParticleModel",
                "com.huige233.transcend.entity.boss.AvatarMeteorEffect",
                "com.huige233.transcend.entity.boss.AvatarMeteorEffectManager"
        );
        ClassLoader parent = BossVisualMigrationTest.class.getClassLoader();
        ClassLoader dedicatedSide = new ClassLoader(parent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("com.huige233.transcend.client.") || name.startsWith("net.minecraft.client.")) {
                    throw new ClassNotFoundException("Client package blocked by dedicated-side probe: " + name);
                }
                if (!scoped.contains(name)) return super.loadClass(name, resolve);
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        String resource = name.replace('.', '/') + ".class";
                        try (InputStream input = parent.getResourceAsStream(resource)) {
                            if (input == null) throw new ClassNotFoundException(resource);
                            byte[] bytes = input.readAllBytes();
                            loaded = defineClass(name, bytes, 0, bytes.length);
                        } catch (IOException error) {
                            throw new ClassNotFoundException(name, error);
                        }
                    }
                    if (resolve) resolveClass(loaded);
                    return loaded;
                }
            }
        };

        for (String name : scoped) Class.forName(name, false, dedicatedSide);
        System.out.println("TASK12_DEDICATED_CLASSLOAD=" + scoped.stream().sorted().toList());
    }

    @Test
    void meteorManagerClearsLevelReferencesWhenServerStops() throws ReflectiveOperationException {
        AvatarMeteorEffectManager.onServerStopped(null);
        AvatarMeteorEffectManager.add(new AvatarMeteorEffect(
                null, net.minecraft.world.phys.Vec3.ZERO, 1.0F, 1.0F, 0, 20, java.util.UUID.randomUUID()));
        assertFalse(collectionField("PENDING_EFFECTS").isEmpty());

        AvatarMeteorEffectManager.onServerStopped(null);

        assertTrue(collectionField("PENDING_EFFECTS").isEmpty());
        assertTrue(collectionField("ACTIVE_EFFECTS").isEmpty());
    }

    @Test
    void adjacentEntityVisualsDoNotConsumeAuthoritativeRandomState() throws IOException {
        String wisp = source("entity/SpellWisp.java");
        String visualSection = section(wisp, "if (this.level() instanceof ServerLevel sl)",
                "if (!this.level().isClientSide && owner != null)");
        assertTrue(wisp.contains("private final RandomSource visualRandom"));
        assertTrue(visualSection.contains("visualRandom.nextDouble()"));
        assertFalse(visualSection.contains("this.random"));
    }

    private static String source(String relative) throws IOException {
        return Files.readString(MAIN.resolve(relative));
    }

    private static String section(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from + start.length());
        assertTrue(from >= 0 && to > from, "Missing source section " + start + " -> " + end);
        return source.substring(from, to);
    }

    private static void assertInOrder(String source, String... tokens) {
        int cursor = -1;
        for (String token : tokens) {
            int next = source.indexOf(token, cursor + 1);
            assertTrue(next > cursor, "Missing or reordered token: " + token);
            cursor = next;
        }
    }

    private static Collection<?> collectionField(String name) throws ReflectiveOperationException {
        Field field = AvatarMeteorEffectManager.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Collection<?>) field.get(null);
    }
}
