package org.dishanhai.vmdiecore.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record DungeonHandle(int id, ResourceLocation theme, int tier, ResourceKey<Level> dimension,
                            BlockPos anchor, long seed) {
}
