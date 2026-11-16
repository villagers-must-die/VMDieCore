package org.dishanhai.vmdiecore.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.dishanhai.vmdiecore.api.DungeonRule;

final class RuleJson {

    private RuleJson() {
    }

    static DungeonRule parse(ResourceLocation id, JsonObject json) {
        return new DungeonRule(id, JsonSupport.boolOr(json, "require_exit_point", true));
    }
}
