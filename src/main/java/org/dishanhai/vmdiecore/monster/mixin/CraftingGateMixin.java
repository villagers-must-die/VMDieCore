package org.dishanhai.vmdiecore.monster.mixin;

import org.dishanhai.vmdiecore.monster.*;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public abstract class CraftingGateMixin {
    @Inject(method="slotChangedCraftingGrid",at=@At("TAIL"))
    private static void mp$gate(AbstractContainerMenu menu,Level level,Player player,CraftingContainer input,ResultContainer result,RecipeHolder<?> recipe,CallbackInfo ci){
        if(level.isClientSide || !(player instanceof net.minecraft.server.level.ServerPlayer sp))return;
        Race tech=TechEvents.required(result.getItem(0));
        if(result.getRecipeUsed()!=null && result.getRecipeUsed().value() instanceof SilkRecipe)tech=Race.ARTHROPOD;
        if(tech!=null && !Race.hasTech(player,tech)){
            result.setItem(0,net.minecraft.world.item.ItemStack.EMPTY);menu.setRemoteSlot(0,net.minecraft.world.item.ItemStack.EMPTY);
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(menu.containerId,menu.incrementStateId(),0,net.minecraft.world.item.ItemStack.EMPTY));
        }
    }
}
