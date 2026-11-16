package org.dishanhai.vmdiecore.dungeon;

import net.minecraft.resources.ResourceLocation;
import org.dishanhai.vmdiecore.api.DungeonTheme;
import org.dishanhai.vmdiecore.api.TierDefinition;
import org.dishanhai.vmdiecore.data.DungeonDefinitions;

public record DungeonInstance(int id, ResourceLocation theme, int level, boolean hasExit, long createdAt) {

    public String themeName() {
        return DungeonDefinitions.theme(theme).map(DungeonTheme::name).orElse(theme.toString());
    }

    public String tierName() {
        return DungeonDefinitions.theme(theme)
                .flatMap(definition -> definition.tier(level))
                .map(TierDefinition::name)
                .orElse("等级 " + level);
    }

    public String label() {
        return "#" + id + " " + themeName() + " " + tierName();
    }
}
