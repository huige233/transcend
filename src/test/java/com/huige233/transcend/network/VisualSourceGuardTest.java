package com.huige233.transcend.network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 按运行侧分类核对客户端视觉代码引用清单，检测服务端入口的直接引用并检查获准数据包的客户端隔离。 */
class VisualSourceGuardTest {
    private static final Path MAIN = Path.of("src/main/java");
    private static final Pattern INVENTORY_REFERENCE = Pattern.compile("ShaderSpellRenderer|client\\.magic");
    private static final Pattern DIRECT_CLIENT_REFERENCE = Pattern.compile(
            "(?:com\\.huige233\\.transcend\\.client\\.(?:renderer\\.ShaderSpellRenderer|magic(?:\\.[A-Za-z_$][\\w$]*)?)|\\bShaderSpellRenderer\\b)");
    private static final Pattern COMMENTS_AND_STRINGS = Pattern.compile(
            "(?s)/\\*.*?\\*/|//[^\\r\\n]*|\"(?:\\\\.|[^\"\\\\])*\"");

    private static final Map<String, InventoryEntry> EXPECTED = Map.ofEntries(
            item("circle/scroll/ScrollVisualHelper.java", 1, SideSafety.SERVER_ENTERED),
            item("client/circle/OreRevealClientState.java", 1, SideSafety.CLIENT_ONLY),
            item("client/magic/MagicCrystalHelper.java", 1, SideSafety.CLIENT_ONLY),
            item("client/renderer/ShaderSpellRenderer.java", 1, SideSafety.CLIENT_ONLY),
            item("network/S2CShaderEffectPack.java", 9, SideSafety.APPROVED_CLIENT_HANDLER)
    );

    @Test
    void inventoryAccountsForEveryReferenceAndClassification() throws IOException {
        Map<String, Integer> actual = new TreeMap<>();
        try (var files = Files.walk(MAIN)) {
            for (Path path : files.filter(file -> file.toString().endsWith(".java")).toList()) {
                int count = count(INVENTORY_REFERENCE, Files.readString(path));
                if (count > 0) actual.put(relative(path), count);
            }
        }

        Map<String, Integer> expectedCounts = new TreeMap<>();
        EXPECTED.forEach((path, inventory) -> expectedCounts.put(path, inventory.matchCount()));
        assertEquals(expectedCounts, actual, "Update the classified allowlist for every added/removed match");
        assertEquals(5, actual.size());
        assertEquals(13, actual.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(3, EXPECTED.values().stream().filter(e -> e.sideSafety() == SideSafety.CLIENT_ONLY).count());
        assertEquals(1, EXPECTED.values().stream().filter(e -> e.sideSafety() == SideSafety.APPROVED_CLIENT_HANDLER).count());
        assertEquals(1, EXPECTED.values().stream().filter(e -> e.sideSafety() == SideSafety.SERVER_ENTERED).count());
    }

    @Test
    void syntheticCommonReferenceIsRejected(@TempDir Path temporaryRoot) throws IOException {
        Path source = temporaryRoot.resolve("com/example/Forbidden.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "package com.example;\n"
                + "import com.huige233.transcend.client.renderer.ShaderSpellRenderer;\n"
                + "class Forbidden { void emit() { ShaderSpellRenderer.addCircle(null, 1, 1, 1, 1, 1, 1, null); } }\n");

        List<String> violations = directViolations(source, SideSafety.SERVER_ENTERED);
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("Forbidden.java"));
    }

    @Test
    void approvedClientPacketHandlerPassesTheGuard() throws IOException {
        Path handler = MAIN.resolve("com/huige233/transcend/network/S2CShaderEffectPack.java");
        String packetSource = Files.readString(handler);
        String codeOnly = COMMENTS_AND_STRINGS.matcher(packetSource).replaceAll(" ");
        String networkSource = Files.readString(MAIN.resolve(
                "com/huige233/transcend/handle/NetworkHandler.java"));
        int clientGate = packetSource.indexOf("DistExecutor.unsafeRunWhenOn(Dist.CLIENT");
        int rendererDispatch = packetSource.indexOf("case CIRCLE -> ShaderSpellRenderer.addCircle");

        assertTrue(DIRECT_CLIENT_REFERENCE.matcher(codeOnly).find(), "Handler must be an explicit allowlist entry");
        assertTrue(clientGate >= 0 && rendererDispatch > clientGate,
                "Renderer dispatch must remain inside the Dist.CLIENT gate");
        assertTrue(networkSource.contains("S2CShaderEffectPack::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT)"),
                "Approved handler must remain registered only PLAY_TO_CLIENT");
        assertTrue(directViolations(handler, SideSafety.APPROVED_CLIENT_HANDLER).isEmpty());
    }

    @Test
    void serverEnteredSourcesDoNotDirectlyReferenceClientVisualCode() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Map.Entry<String, InventoryEntry> item : EXPECTED.entrySet()) {
            Path source = MAIN.resolve("com/huige233/transcend").resolve(item.getKey());
            violations.addAll(directViolations(source, item.getValue().sideSafety()));
        }
        assertTrue(violations.isEmpty(), "Todos 11/12 must migrate these server-entered references:\n"
                + String.join("\n", violations));
    }

    private static List<String> directViolations(Path source, SideSafety sideSafety) throws IOException {
        if (sideSafety != SideSafety.SERVER_ENTERED) return List.of();
        String codeOnly = COMMENTS_AND_STRINGS.matcher(Files.readString(source)).replaceAll(" ");
        if (!DIRECT_CLIENT_REFERENCE.matcher(codeOnly).find()) return List.of();
        return List.of(source.toString());
    }

    private static int count(Pattern pattern, String source) {
        int matches = 0;
        var matcher = pattern.matcher(source);
        while (matcher.find()) matches++;
        return matches;
    }

    private static String relative(Path path) {
        return MAIN.resolve("com/huige233/transcend").relativize(path).toString().replace('\\', '/');
    }

    private static Map.Entry<String, InventoryEntry> item(String path, int count, SideSafety sideSafety) {
        return entry(path, new InventoryEntry(count, sideSafety));
    }

    /** 记录单个源码文件应包含的视觉引用数量及运行侧安全分类。 */
    private record InventoryEntry(int matchCount, SideSafety sideSafety) {}

    /** 区分纯客户端源码、服务端可进入源码和获准客户端包处理器，以选择引用检查规则。 */
    private enum SideSafety {
        CLIENT_ONLY,
        SERVER_ENTERED,
        APPROVED_CLIENT_HANDLER
    }
}
