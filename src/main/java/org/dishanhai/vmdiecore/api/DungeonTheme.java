package org.dishanhai.vmdiecore.api;

import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record DungeonTheme(ResourceLocation id, String name, ResourceLocation icon, Map<Integer, TierDefinition> tiers) {

    public DungeonTheme {
        tiers = Map.copyOf(tiers);
    }

    /** 按等级升序返回全部等级。 */
    public List<TierDefinition> tierList() {
        return tiers.values().stream().sorted(Comparator.comparingInt(TierDefinition::level)).toList();
    }

    /** 按等级查定义。 */
    public Optional<TierDefinition> tier(int level) {
        return Optional.ofNullable(tiers.get(level));
    }

    /** 返回最小等级，空种类返回 0。 */
    public int lowestLevel() {
        return tiers.keySet().stream().min(Integer::compareTo).orElse(0);
    }
}
