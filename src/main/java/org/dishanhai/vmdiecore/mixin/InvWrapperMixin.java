package org.dishanhai.vmdiecore.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.dishanhai.vmdiecore.item.StackLimits;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InvWrapper.class)
abstract class InvWrapperMixin {
    @ModifyExpressionValue(method = "insertItem", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/items/wrapper/InvWrapper;getSlotLimit(I)I"))
    private int vmdiecore$extendInsertionLimit(int original, int slot, ItemStack stack) {
        return StackLimits.extendContainerLimit(original, stack);
    }
}
