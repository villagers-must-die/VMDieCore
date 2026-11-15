package org.dishanhai.vmdiecore.compat.kubejs;

import net.minecraft.resources.ResourceLocation;
import org.dishanhai.vmdiecore.item.StackLimits;

public final class StackingBinding {
    private StackingBinding() {}

    public static void setLimit(ResourceLocation itemId, int limit) {
        StackLimits.setLimit(itemId, limit);
    }
}
