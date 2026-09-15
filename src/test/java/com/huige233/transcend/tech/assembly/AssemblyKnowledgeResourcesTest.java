package com.huige233.transcend.tech.assembly;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 检查七级机械知识手册的模型、独立纹理和双语翻译，并确保配方逐级升级且只产出一本。 */
class AssemblyKnowledgeResourcesTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path ASSETS = RESOURCES.resolve("assets/transcend");

    private JsonObject json(Path path) throws Exception {
        try (var reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test
    void everyTierHasModelDistinctTextureAndTranslations() throws Exception {
        JsonObject english = json(ASSETS.resolve("lang/en_us.json"));
        JsonObject chinese = json(ASSETS.resolve("lang/zh_cn.json"));
        Set<String> textures = new HashSet<>();
        for (int tier = 1; tier <= 7; tier++) {
            String id = "mechanical_knowledge_t" + tier;
            JsonObject model = json(ASSETS.resolve("models/item/" + id + ".json"));
            assertEquals("minecraft:item/generated", model.get("parent").getAsString());
            assertEquals("transcend:item/" + id, model.getAsJsonObject("textures").get("layer0").getAsString());
            Path texture = ASSETS.resolve("textures/item/" + id + ".png");
            var image = ImageIO.read(texture.toFile());
            assertNotNull(image);
            assertEquals(32, image.getWidth());
            assertEquals(32, image.getHeight());
            assertTrue(image.getColorModel().hasAlpha());
            assertTrue(textures.add(java.util.Base64.getEncoder().encodeToString(Files.readAllBytes(texture))));
            assertTrue(english.has("item.transcend." + id));
            assertTrue(chinese.has("item.transcend." + id));
        }
    }

    @Test
    void recipesAreAcyclicAndProduceExactlyOneManual() throws Exception {
        for (int tier = 1; tier <= 7; tier++) {
            String id = "mechanical_knowledge_t" + tier;
            JsonObject recipe = json(RESOURCES.resolve("data/transcend/recipes/" + id + ".json"));
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
            JsonObject result = recipe.getAsJsonObject("result");
            assertEquals("transcend:" + id, result.get("item").getAsString());
            assertEquals(1, result.has("count") ? result.get("count").getAsInt() : 1);
            assertEquals(tier == 1 ? "minecraft:book" : "transcend:mechanical_knowledge_t" + (tier - 1),
                    recipe.getAsJsonObject("key").getAsJsonObject("B").get("item").getAsString());
            for (var ingredient : recipe.getAsJsonObject("key").entrySet()) {
                String item = ingredient.getValue().getAsJsonObject().get("item").getAsString();
                if (item.startsWith("transcend:mechanical_knowledge_t")) {
                    int inputTier = Integer.parseInt(item.substring(item.length() - 1));
                    assertTrue(inputTier >= 1 && inputTier < tier);
                } else {
                    assertTrue(item.startsWith("minecraft:"));
                }
            }
        }
    }
}
