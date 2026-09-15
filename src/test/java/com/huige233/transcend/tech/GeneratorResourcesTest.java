package com.huige233.transcend.tech;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 检查三种发电机的方块状态、物品模型和独立纹理，并防止数据生成器将机器替换为平面物品模型。 */
class GeneratorResourcesTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/transcend");
    private static final List<String> GENERATORS = List.of(
            "fire_generator", "wind_generator", "creative_generator");

    private JsonObject json(Path path) throws Exception {
        try (var reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test
    void generatorsUseTheirOwnBlockTexturesInWorldAndInventory() throws Exception {
        for (String id : GENERATORS) {
            String blockModel = "transcend:block/" + id;
            JsonObject model = json(ASSETS.resolve("models/block/" + id + ".json"));
            assertEquals("minecraft:block/cube_all", model.get("parent").getAsString(), id);
            assertEquals(blockModel, model.getAsJsonObject("textures").get("all").getAsString(), id);

            JsonObject item = json(ASSETS.resolve("models/item/" + id + ".json"));
            assertEquals(blockModel, item.get("parent").getAsString(), id);
            assertFalse(item.has("textures"), id + " must inherit its block texture");

            JsonObject blockstate = json(ASSETS.resolve("blockstates/" + id + ".json"));
            assertEquals(blockModel, blockstate.getAsJsonObject("variants")
                    .getAsJsonObject("").get("model").getAsString(), id);
        }
    }

    @Test
    void generatorTexturesAreReadableAndVisuallyDistinct() throws Exception {
        Set<String> pixels = new HashSet<>();
        for (String id : GENERATORS) {
            Path texture = ASSETS.resolve("textures/block/" + id + ".png");
            assertTrue(Files.isRegularFile(texture), texture.toString());
            var image = ImageIO.read(texture.toFile());
            assertNotNull(image, texture.toString());
            assertEquals(16, image.getWidth(), id);
            assertEquals(16, image.getHeight(), id);
            assertTrue(pixels.add(Arrays.toString(image.getRGB(0, 0, 16, 16, null, 0, 16))),
                    id + " must not reuse another generator's pixels");
        }
    }

    @Test
    void itemDatagenPreservesGeneratorBlockParents() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/huige233/transcend/init/ModItemModelGen.java"));
        for (String id : GENERATORS) {
            assertTrue(source.contains("withExistingParent(\"" + id
                    + "\", modLoc(\"block/" + id + "\"))"), id);
            assertFalse(source.contains("itemGenerateModel(ModItems." + id + ".get()"),
                    id + " datagen must not replace the block parent with a flat item model");
        }
    }
}
