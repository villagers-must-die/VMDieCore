package org.dishanhai.vmdiecore.monster.mixin;

import org.dishanhai.vmdiecore.monster.MonsterPerspective;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Ingredient.class)
public abstract class IngredientMixin {
    @Shadow private ItemStack[] itemStacks;
    @Inject(method="getItems",at=@At("RETURN"),cancellable=true)
    private void mp$venomAlternatives(CallbackInfoReturnable<ItemStack[]> ci){
        ItemStack[] values=ci.getReturnValue();boolean fermented=false;
        for(ItemStack s:values){if(s.is(Items.FERMENTED_SPIDER_EYE))fermented=true;if(MonsterPerspective.VENOM.isBound() && s.is(MonsterPerspective.VENOM))return;}
        if(fermented && MonsterPerspective.VENOM.isBound()){
            itemStacks=java.util.Arrays.copyOf(values,values.length+1);itemStacks[values.length]=MonsterPerspective.VENOM.toStack();ci.setReturnValue(itemStacks);
        }
    }
    @Inject(method="test(Lnet/minecraft/world/item/ItemStack;)Z",at=@At("HEAD"),cancellable=true)
    private void mp$venom(ItemStack stack,CallbackInfoReturnable<Boolean> ci){
        if(stack!=null && stack.is(MonsterPerspective.VENOM) && ((Ingredient)(Object)this).test(new ItemStack(Items.FERMENTED_SPIDER_EYE)))ci.setReturnValue(true);
    }
}
