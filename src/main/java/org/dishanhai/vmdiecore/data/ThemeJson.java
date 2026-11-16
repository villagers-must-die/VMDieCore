package org.dishanhai.vmdiecore.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.dishanhai.vmdiecore.VMDieCore;
import org.dishanhai.vmdiecore.api.DungeonTheme;
import org.dishanhai.vmdiecore.api.TierDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ThemeJson {

    private ThemeJson() {
    }

    static DungeonTheme parse(ResourceLocation id, JsonObject json) {
        String name = JsonSupport.stringOr(json, "name", id.getPath());
        ResourceLocation icon = JsonSupport.id(json, "icon", ResourceLocation.withDefaultNamespace("stone"));
        JsonArray array = JsonSupport.required(json, "tiers").getAsJsonArray();
        Map<Integer, TierDefinition> tiers = new LinkedHashMap<>();
        for (JsonElement element : array) {
            TierDefinition tier = parseTier(element.getAsJsonObject());
            if (tiers.putIfAbsent(tier.level(), tier) != null) {
                VMDieCore.LOGGER.warn("地牢 {} 的等级 {} 重复，后续跳过", id, tier.level());
            }
        }
        if (tiers.isEmpty()) throw new IllegalArgumentException("tiers 不能为空");
        return new DungeonTheme(id, name, icon, tiers);
    }

    private static TierDefinition parseTier(JsonObject json) {
        int level = JsonSupport.required(json, "level").getAsInt();
        if (level < 1) throw new IllegalArgumentException("等级必须大于 0");
        String name = JsonSupport.stringOr(json, "name", "第 " + level + " 级");
        JsonElement poolElement = json.has("pools") ? json.get("pools") : JsonSupport.required(json, "pool");
        List<ResourceLocation> pools = parsePools(poolElement);
        boolean hasExit = JsonSupport.boolOr(json, "has_exit", true);
        ResourceLocation profile = JsonSupport.id(json, "profile");
        ResourceLocation rule = JsonSupport.id(json, "rule");
        return new TierDefinition(level, name, pools, hasExit, profile, rule);
    }

    private static List<ResourceLocation> parsePools(JsonElement element) {
        List<ResourceLocation> pools = new ArrayList<>();
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                pools.add(ResourceLocation.parse(entry.getAsString()));
            }
        } else {
            pools.add(ResourceLocation.parse(element.getAsString()));
        }
        if (pools.isEmpty()) throw new IllegalArgumentException("pool 不能为空");
        return pools;
    }
}
