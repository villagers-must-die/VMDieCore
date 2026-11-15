package org.dishanhai.vmdiecore.monster;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;

public final class SilkRecipe extends CustomRecipe {
    public SilkRecipe(CraftingBookCategory c){super(c);}
    private static final int[] LEVELS={2,2,1,3,3,4,3,3};
    private static int weave(ItemStack s){
        var id=BuiltInRegistries.ITEM.getKey(s.getItem());
        for(int i=0;i<MonsterPerspective.WEAVES.length;i++)if(id.equals(MonsterPerspective.id("silk_"+MonsterPerspective.WEAVES[i])))return i;
        return -1;
    }
    private ItemStack result(CraftingInput input,HolderLookup.Provider registries){
        int fabric=-1;ItemStack target=ItemStack.EMPTY;
        for(int i=0;i<input.size();i++){
            ItemStack s=input.getItem(i);if(s.isEmpty())continue;
            int w=weave(s);if(w>=0){if(fabric>=0)return ItemStack.EMPTY;fabric=w;}
            else{if(!target.isEmpty())return ItemStack.EMPTY;target=s;}
        }
        if(fabric<0 || target.isEmpty() || target.getMaxStackSize()!=1 || !target.has(DataComponents.ENCHANTMENTS))return ItemStack.EMPTY;
        var enchant=registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,net.minecraft.resources.ResourceLocation.withDefaultNamespace(MonsterPerspective.WEAVES[fabric])));
        if(!enchant.value().canEnchant(target))return ItemStack.EMPTY;
        ItemEnchantments old=EnchantmentHelper.getEnchantmentsForCrafting(target);
        if(old.getLevel(enchant)>=LEVELS[fabric])return ItemStack.EMPTY;
        for(var entry:old.keySet())if(!entry.equals(enchant) && !Enchantment.areCompatible(entry,enchant))return ItemStack.EMPTY;
        ItemStack result=target.copyWithCount(1);int index=fabric;
        EnchantmentHelper.updateEnchantments(result,mutable->mutable.upgrade(enchant,LEVELS[index]));
        return result;
    }
    @Override public boolean matches(CraftingInput i,Level l){return !result(i,l.registryAccess()).isEmpty();}
    @Override public ItemStack assemble(CraftingInput i,HolderLookup.Provider p){return result(i,p);}
    @Override public boolean canCraftInDimensions(int w,int h){return w*h>=2;}
    @Override public RecipeSerializer<?> getSerializer(){return MonsterPerspective.SILK_RECIPE.get();}
}
