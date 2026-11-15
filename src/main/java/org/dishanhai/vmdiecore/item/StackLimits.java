package org.dishanhai.vmdiecore.item;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class StackLimits {
    public static final int MAX_COUNT = 9_999;
    private static final Map<ResourceLocation, Integer> LIMITS = new ConcurrentHashMap<>();

    private StackLimits() {}

    public static void setLimit(ResourceLocation itemId, int limit) {
        if (limit < 1 || limit > MAX_COUNT) {
            throw new IllegalArgumentException("Stack limit for " + itemId + " must be between 1 and " + MAX_COUNT);
        }
        LIMITS.put(itemId, limit);
    }

    public static int getLimit(ItemStack stack, int original) {
        if (LIMITS.isEmpty() || stack.isEmpty()) {
            return original;
        }

        var key = BuiltInRegistries.ITEM.getResourceKey(stack.getItem()).orElse(null);
        var limit = key == null ? null : LIMITS.get(key.location());
        if (limit == null || stack.has(DataComponents.MAX_DAMAGE)
                || stack.getComponentsPatch().get(DataComponents.MAX_STACK_SIZE) != null) {
            return original;
        }
        return limit;
    }

    public static int extendContainerLimit(int containerLimit, ItemStack stack) {
        // Only lift the generic cap for stacks that opt in to a higher limit.
        return containerLimit == Item.ABSOLUTE_MAX_STACK_SIZE
                ? Math.max(containerLimit, getLimit(stack, containerLimit))
                : containerLimit;
    }
}
