package org.dishanhai.vmdiecore.api;

import net.minecraft.resources.ResourceLocation;

public record DungeonRule(ResourceLocation id, boolean requireExitPoint) {
}
