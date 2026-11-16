package org.dishanhai.vmdiecore.api;

import net.minecraft.resources.ResourceLocation;

public record DungeonProfile(ResourceLocation id, ResourceLocation dimensionType, GeneratorSettings generator,
                             long fixedTime, Anchor anchor, Jigsaw jigsaw, int clearRadius, Landing landing) {

    public record Anchor(int radius, int minY, int maxY) {
    }

    public record Jigsaw(int depth, int landingOffset, boolean keepJigsaws) {
    }

    public record Landing(int size, ResourceLocation floor) {
    }
}
