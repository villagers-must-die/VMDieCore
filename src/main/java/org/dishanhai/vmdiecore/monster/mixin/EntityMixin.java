package org.dishanhai.vmdiecore.monster.mixin;

import org.dishanhai.vmdiecore.monster.Race;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method="isInvisible",at=@At("HEAD"),cancellable=true)
    private void mp$unclothed(CallbackInfoReturnable<Boolean> ci){
        if((Object)this instanceof Player p && Race.of(p)==Race.VAMPIRE && !p.getPersistentData().getBoolean("mp_preview")){
            if(!p.getMainHandItem().isEmpty() || !p.getOffhandItem().isEmpty())return;
            for(ItemStack stack:p.getArmorSlots())if(!stack.isEmpty())return;
            ci.setReturnValue(true);
        }
    }
}
