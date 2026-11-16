package org.dishanhai.vmdiecore.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

public sealed interface GeneratorSettings {

    record Empty() implements GeneratorSettings {
    }

    record Flat(List<Layer> layers, ResourceLocation biome) implements GeneratorSettings {
        public Flat {
            layers = List.copyOf(layers);
        }
    }

    record Layer(int height, ResourceLocation block) {
    }

    /**
     * biomeSource 写字符串按预设表查，写对象按 worldgen/biome_source 的 codec 解析。
     */
    record Noise(ResourceLocation settings, JsonElement biomeSource) implements GeneratorSettings {
        public Noise {
            Objects.requireNonNull(settings, "settings");
            Objects.requireNonNull(biomeSource, "biomeSource");
        }

        public Noise(ResourceLocation settings, ResourceLocation biomeSource) {
            this(settings, new JsonPrimitive(Objects.requireNonNull(biomeSource, "biomeSource").toString()));
        }
    }
}
