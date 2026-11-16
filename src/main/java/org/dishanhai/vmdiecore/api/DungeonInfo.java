package org.dishanhai.vmdiecore.api;

import net.minecraft.resources.ResourceLocation;

public record DungeonInfo(int id, ResourceLocation theme, String themeName, int level, String levelName,
                          boolean hasExit, int visits) {
}
