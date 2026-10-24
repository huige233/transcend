package com.huige233.transcend.network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** 旧视觉系统已经移除；阻止重新引入旧渲染依赖，同时保留线协议兼容入口。 */
class VisualSourceGuardTest {
    private static final Path MAIN = Path.of("src/main/java");
    private static final Pattern LEGACY_REFERENCE = Pattern.compile("\\bShaderSpellRenderer\\b|client\\.magic\\b");
    private static final Pattern COMMENTS_AND_STRINGS = Pattern.compile(
            "(?s)/\\*.*?\\*/|//[^\\r\\n]*|\"(?:\\\\.|[^\"\\\\])*\"");

    @Test
    void removedVisualSystemHasNoRemainingCodeReferences() throws IOException {
        List<String> violations = new ArrayList<>();
        try (var files = Files.walk(MAIN)) {
            for (Path path : files.filter(file -> file.toString().endsWith(".java")).toList()) {
                if (hasLegacyReference(path)) violations.add(path.toString());
            }
        }
        assertTrue(violations.isEmpty(), String.join("\n", violations));
    }

    @Test
    void syntheticCommonReferenceIsRejected(@TempDir Path temporaryRoot) throws IOException {
        Path source = temporaryRoot.resolve("Forbidden.java");
        Files.writeString(source, "import com.huige233.transcend.client.renderer.ShaderSpellRenderer;\n"
                + "class Forbidden { void emit() { ShaderSpellRenderer.addCircle(null); } }");
        assertTrue(hasLegacyReference(source));
    }

    @Test
    void compatibilityPacketRemainsClientBoundAndDoesNotScheduleRendering() throws IOException {
        Path path = MAIN.resolve("com/huige233/transcend/network/S2CShaderEffectPack.java");
        String packet = Files.readString(path);
        String network = Files.readString(MAIN.resolve("com/huige233/transcend/handle/NetworkHandler.java"));
        assertTrue(network.contains("S2CShaderEffectPack::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT)"));
        assertTrue(packet.contains("setPacketHandled(true)"));
        assertFalse(packet.contains("enqueueWork"));
        assertFalse(hasLegacyReference(path));
    }

    private static boolean hasLegacyReference(Path source) throws IOException {
        String code = COMMENTS_AND_STRINGS.matcher(Files.readString(source)).replaceAll(" ");
        return LEGACY_REFERENCE.matcher(code).find();
    }
}
