package org.dishanhai.vmdiecore.monster.mixin;

import org.dishanhai.vmdiecore.monster.Race;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method="isInvertedHealAndHarm",at=@At("HEAD"),cancellable=true)
    private void mp$undead(CallbackInfoReturnable<Boolean> ci){
        if((Object)this instanceof Player p){Race r=Race.of(p); if(r!=null && r.undead())ci.setReturnValue(true);}
    }
    @Inject(method="onClimbable",at=@At("HEAD"),cancellable=true)
    private void mp$climb(CallbackInfoReturnable<Boolean> ci){
        if((Object)this instanceof Player p && Race.of(p)==Race.ARTHROPOD && p.horizontalCollision)ci.setReturnValue(true);
    }
    @Inject(method="isSensitiveToWater",at=@At("HEAD"),cancellable=true)
    private void mp$water(CallbackInfoReturnable<Boolean> ci){
        if((Object)this instanceof Player p && Race.of(p)==Race.ENDERMAN)ci.setReturnValue(true);
    }
}
