package org.dishanhai.vmdiecore.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

final class JsonSupport {

    private JsonSupport() {
    }

    static ResourceLocation id(JsonObject json, String key, ResourceLocation fallback) {
        JsonElement element = json.get(key);
        return element == null ? fallback : ResourceLocation.parse(element.getAsString());
    }

    static ResourceLocation id(JsonObject json, String key) {
        return ResourceLocation.parse(required(json, key).getAsString());
    }

    static JsonElement required(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null) throw new IllegalArgumentException("缺少字段 " + key);
        return element;
    }

    static int intOr(JsonObject json, String key, int fallback) {
        JsonElement element = json.get(key);
        return element == null ? fallback : element.getAsInt();
    }

    static long longOr(JsonObject json, String key, long fallback) {
        JsonElement element = json.get(key);
        return element == null ? fallback : element.getAsLong();
    }

    static boolean boolOr(JsonObject json, String key, boolean fallback) {
        JsonElement element = json.get(key);
        return element == null ? fallback : element.getAsBoolean();
    }

    static String stringOr(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        return element == null ? fallback : element.getAsString();
    }
}
