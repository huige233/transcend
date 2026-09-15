package com.huige233.transcend.items.tech;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 检查枪械和护盾模块具有独立纹理与模型，并验证基础模型的属性覆盖数量及纹理引用。 */
class ModuleVisualResourcesTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/transcend");

    private static JsonObject json(Path path) throws Exception {
        try (var reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static String textureHash(Path path) throws Exception {
        var image = ImageIO.read(path.toFile());
        assertNotNull(image, path.toString());
        assertEquals(16, image.getWidth(), path.toString());
        assertEquals(16, image.getHeight(), path.toString());
        var pixels = new int[256];
        image.getRGB(0, 0, 16, 16, pixels, 0, 16);
        return java.util.Arrays.toString(pixels);
    }

    @Test
    void everyObtainableModuleHasDistinctTextureAndModel() throws Exception {
        Set<String> hashes = new HashSet<>();
        Set<String> models = new HashSet<>();
        for (String id : List.of("ammo_std", "ammo_scatter", "ammo_piercing", "ammo_explosive",
                "barrel_std", "barrel_long", "barrel_accel", "muzzle_silencer", "muzzle_scope", "sirius")) {
            assertTrue(hashes.add(textureHash(ASSETS.resolve("textures/item/" + id + ".png"))), id);
            Path model = ASSETS.resolve("models/item/gun_module/" + id + ".json");
            assertTrue(Files.isRegularFile(model), model.toString());
            assertTrue(models.add(Files.readString(model)), id);
        }
        for (String id : List.of("basic", "reinforced", "efficient", "t5_mechanical")) {
            assertTrue(hashes.add(textureHash(ASSETS.resolve("textures/item/shield_module_" + id + ".png"))), id);
            Path model = ASSETS.resolve("models/item/shield_module/" + id + ".json");
            assertTrue(Files.isRegularFile(model), model.toString());
            assertTrue(models.add(Files.readString(model)), id);
        }
    }

    @Test
    void baseModelsContainOrderedPredicateOverrides() throws Exception {
        JsonObject gun = json(ASSETS.resolve("models/item/gun_module.json"));
        assertEquals(10, gun.getAsJsonArray("overrides").size());
        JsonObject shield = json(ASSETS.resolve("models/item/shield_module.json"));
        assertEquals(4, shield.getAsJsonArray("overrides").size());
        assertEquals("transcend:item/gun_module", gun.getAsJsonObject("textures").get("layer0").getAsString());
        assertEquals("transcend:item/shield_module", shield.getAsJsonObject("textures").get("layer0").getAsString());
    }
}
