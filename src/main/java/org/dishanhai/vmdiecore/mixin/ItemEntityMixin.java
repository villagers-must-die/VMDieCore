package org.dishanhai.vmdiecore.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.dishanhai.vmdiecore.item.StackLimits;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ItemEntity.class)
abstract class ItemEntityMixin {
    @ModifyConstant(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V",
            constant = @Constant(intValue = 64))
    private static int vmdiecore$extendMergeLimit(int original, ItemEntity destination, ItemStack stack) {
        return Math.max(original, StackLimits.getLimit(stack, original));
    }
}
