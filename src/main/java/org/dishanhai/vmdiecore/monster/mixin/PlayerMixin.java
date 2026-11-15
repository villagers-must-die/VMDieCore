package org.dishanhai.vmdiecore.monster.mixin;

import org.dishanhai.vmdiecore.monster.Race;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method="getDefaultDimensions",at=@At("HEAD"),cancellable=true)
    private void mp$dimensions(Pose pose,CallbackInfoReturnable<EntityDimensions> ci){
        Race r=Race.of((Player)(Object)this);
        if(r==Race.ENDERMAN && pose!=Pose.SLEEPING) ci.setReturnValue(EntityDimensions.scalable(0.6F,3F).withEyeHeight(2.65F));
        if(r==Race.ARTHROPOD && pose!=Pose.SLEEPING) ci.setReturnValue(EntityDimensions.scalable(1.4F,1F).withEyeHeight(0.65F));
    }
    @Inject(method="isSwimming",at=@At("HEAD"),cancellable=true)
    private void mp$swim(CallbackInfoReturnable<Boolean> ci){
        if(Race.of((Player)(Object)this)==Race.SKELETON) ci.setReturnValue(false);
    }
    @Inject(method="canEat",at=@At("HEAD"),cancellable=true)
    private void mp$eat(boolean always,CallbackInfoReturnable<Boolean> ci){
        if(Race.of((Player)(Object)this)==Race.SKELETON)ci.setReturnValue(true);
    }
}
