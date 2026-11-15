package org.dishanhai.vmdiecore.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.dishanhai.vmdiecore.item.StackLimits;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Container.class)
interface ContainerMixin {
    @ModifyExpressionValue(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;getMaxStackSize()I"))
    private int vmdiecore$extendContainerLimit(int original, ItemStack stack) {
        return StackLimits.extendContainerLimit(original, stack);
    }
}
