package org.dishanhai.vmdiecore.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.ItemStack;
import org.dishanhai.vmdiecore.item.StackLimits;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemStack.class)
abstract class ItemStackMixin {
    @ModifyReturnValue(method = "getMaxStackSize", at = @At("RETURN"))
    private int vmdiecore$getStackLimit(int original) {
        return StackLimits.getLimit((ItemStack) (Object) this, original);
    }

    // The persisted count must also round-trip existing stacks after a script changes.
    @ModifyArg(method = "lambda$static$3", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/ExtraCodecs;intRange(II)Lcom/mojang/serialization/Codec;"), index = 1)
    private static int vmdiecore$extendSavedCount(int original) {
        return StackLimits.MAX_COUNT;
    }
}
