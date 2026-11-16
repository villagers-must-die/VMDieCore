package org.dishanhai.vmdiecore.api;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record TierDefinition(int level, String name, List<ResourceLocation> pools, boolean hasExit,
                             ResourceLocation profileId, ResourceLocation ruleId) {

    public TierDefinition {
        pools = List.copyOf(pools);
    }

    /** 返回首个结构池，拼装时优先尝试它。 */
    public ResourceLocation firstPool() {
        return pools.getFirst();
    }
}
