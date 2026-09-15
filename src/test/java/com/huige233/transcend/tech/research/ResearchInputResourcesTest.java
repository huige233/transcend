package com.huige233.transcend.tech.research;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** 检查空白研究组件和研究辅助单元的创造栏入口、模型、纹理及合成配方。 */
class ResearchInputResourcesTest {
    @Test void bothConsumablesHaveCreativeEntriesModelsTexturesAndRecipes() throws Exception {
        Path root = Path.of("src/main");
        String tab = Files.readString(root.resolve("java/com/huige233/transcend/TranscendTab.java"));
        String datagen = Files.readString(root.resolve("java/com/huige233/transcend/init/ModItemModelGen.java"));
        for (String id : new String[]{"blank_research_component", "research_assist_unit"}) {
            assertTrue(tab.contains("pOutput.accept(ModItems." + id + ".get())"), id);
            assertTrue(datagen.contains("itemGenerateModel(ModItems." + id + ".get()"), id);
            Path assets = root.resolve("resources/assets/transcend");
            var model = JsonParser.parseString(Files.readString(assets.resolve("models/item/" + id + ".json"))).getAsJsonObject();
            assertEquals("transcend:item/" + id, model.getAsJsonObject("textures").get("layer0").getAsString());
            var image = ImageIO.read(assets.resolve("textures/item/" + id + ".png").toFile());
            assertNotNull(image);
            assertEquals(16, image.getWidth());
            assertEquals(16, image.getHeight());
            var recipe = JsonParser.parseString(Files.readString(root.resolve("resources/data/transcend/recipes/" + id + ".json"))).getAsJsonObject();
            assertEquals("transcend:" + id, recipe.getAsJsonObject("result").get("item").getAsString());
        }
    }
}
