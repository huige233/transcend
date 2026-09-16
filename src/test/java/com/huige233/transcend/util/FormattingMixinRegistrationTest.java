package com.huige233.transcend.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 检查格式码相关 Mixin 的开发启动、打包注册及客户端隔离配置。 */
class FormattingMixinRegistrationTest {
    @Test
    void developmentRunsRegisterMixinConfig() throws Exception {
        String build = Files.readString(Path.of("build.gradle"));
        assertTrue(build.contains("config \"${mod_id}.mixins.json\""),
                "Development runs must register the mixin config, not just its refmap");
    }

    @Test
    void packagedJarRegistersMixinConfigInManifest() throws Exception {
        String build = Files.readString(Path.of("build.gradle"));
        assertTrue(build.contains("\"MixinConfigs\": \"${mod_id}.mixins.json\""),
                "Forge 1.20.1 discovers mixin configs through the JAR manifest");
        String metadata = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));
        assertFalse(metadata.contains("MixinConfigs="),
                "MixinConfigs is not a Forge mods.toml registration key");
    }

    @Test
    void formattingMixinsAreRegisteredOnCorrectSides() throws Exception {
        JsonObject config = JsonParser.parseString(Files.readString(
                Path.of("src/main/resources/transcend.mixins.json"))).getAsJsonObject();
        var common = config.getAsJsonArray("mixins").asList();
        var client = config.getAsJsonArray("client").asList();
        for (String name : new String[]{"SharedConstantsMixin", "ServerGamePacketListenerImplMixin", "SignBlockEntityMixin"}) {
            assertTrue(common.stream().anyMatch(entry -> name.equals(entry.getAsString())), name);
        }
        for (String name : new String[]{"ChatScreenMixin", "ChatComponentMixin"}) {
            assertTrue(client.stream().anyMatch(entry -> name.equals(entry.getAsString())), name);
            assertFalse(common.stream().anyMatch(entry -> name.equals(entry.getAsString())), name);
        }
    }
}
