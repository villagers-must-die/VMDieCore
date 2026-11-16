package org.dishanhai.vmdiecore.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.dishanhai.vmdiecore.api.DungeonProfile;
import org.dishanhai.vmdiecore.api.GeneratorSettings;

import java.util.ArrayList;
import java.util.List;

final class ProfileJson {

    private ProfileJson() {
    }

    static DungeonProfile parse(ResourceLocation id, JsonObject json) {
        ResourceLocation dimensionType = JsonSupport.id(json, "dimension_type");
        GeneratorSettings generator = generator(JsonSupport.required(json, "generator").getAsJsonObject());
        long fixedTime = JsonSupport.longOr(json, "fixed_time", 13000L);
        DungeonProfile.Anchor anchor = anchor(json);
        DungeonProfile.Jigsaw jigsaw = jigsaw(json);
        int clearRadius = JsonSupport.intOr(json, "clear_radius", 48);
        DungeonProfile.Landing landing = landing(json);
        return new DungeonProfile(id, dimensionType, generator, fixedTime, anchor, jigsaw, clearRadius, landing);
    }

    private static DungeonProfile.Anchor anchor(JsonObject json) {
        if (!json.has("anchor")) return new DungeonProfile.Anchor(32, -44, -26);
        JsonObject anchor = json.getAsJsonObject("anchor");
        return new DungeonProfile.Anchor(
                JsonSupport.intOr(anchor, "radius", 32),
                JsonSupport.intOr(anchor, "min_y", -44),
                JsonSupport.intOr(anchor, "max_y", -26));
    }

    private static DungeonProfile.Jigsaw jigsaw(JsonObject json) {
        if (!json.has("jigsaw")) return new DungeonProfile.Jigsaw(20, 8, false);
        JsonObject jigsaw = json.getAsJsonObject("jigsaw");
        return new DungeonProfile.Jigsaw(
                JsonSupport.intOr(jigsaw, "depth", 20),
                JsonSupport.intOr(jigsaw, "landing_offset", 8),
                JsonSupport.boolOr(jigsaw, "keep_jigsaws", false));
    }

    private static DungeonProfile.Landing landing(JsonObject json) {
        if (!json.has("landing")) return new DungeonProfile.Landing(3, ResourceLocation.withDefaultNamespace("stone"));
        JsonObject landing = json.getAsJsonObject("landing");
        return new DungeonProfile.Landing(
                JsonSupport.intOr(landing, "size", 3),
                JsonSupport.id(landing, "floor", ResourceLocation.withDefaultNamespace("stone")));
    }

    private static GeneratorSettings generator(JsonObject json) {
        String type = JsonSupport.required(json, "type").getAsString();
        return switch (type) {
            case "empty" -> new GeneratorSettings.Empty();
            case "flat" -> new GeneratorSettings.Flat(layers(json), JsonSupport.id(json, "biome", ResourceLocation.withDefaultNamespace("plains")));
            case "noise" -> new GeneratorSettings.Noise(
                    JsonSupport.id(json, "settings"),
                    JsonSupport.required(json, "biome_source"));
            default -> throw new IllegalArgumentException("未知生成器类型 " + type);
        };
    }

    private static List<GeneratorSettings.Layer> layers(JsonObject json) {
        JsonArray array = JsonSupport.required(json, "layers").getAsJsonArray();
        List<GeneratorSettings.Layer> layers = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject layer = element.getAsJsonObject();
            layers.add(new GeneratorSettings.Layer(
                    JsonSupport.required(layer, "height").getAsInt(),
                    JsonSupport.id(layer, "block")));
        }
        if (layers.isEmpty()) throw new IllegalArgumentException("layers 不能为空");
        return layers;
    }
}
