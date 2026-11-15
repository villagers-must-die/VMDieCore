package org.dishanhai.vmdiecore.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.dishanhai.vmdiecore.item.StackLimits;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Slot.class)
abstract class SlotMixin {
    @ModifyExpressionValue(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;getMaxStackSize()I"))
    private int vmdiecore$extendSlotLimit(int original, ItemStack stack) {
        return StackLimits.extendContainerLimit(original, stack);
    }
}
