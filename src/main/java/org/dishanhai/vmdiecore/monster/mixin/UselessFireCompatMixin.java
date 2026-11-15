package org.dishanhai.vmdiecore.monster.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Useless Mod 2.4.6.6 clears every unprotected player's fire on every tick. */
@Pseudo
@Mixin(targets="com.sorrowmist.useless.event.EventHandler",remap=false)
public abstract class UselessFireCompatMixin {
    @Unique private static final ThreadLocal<Boolean> mp$releasedProtection=new ThreadLocal<>();

    @Shadow(remap=false)
    private static boolean releaseBeefInvulnerability(Player player){throw new AssertionError();}

    @Redirect(method="applyBeefInvulnerability",at=@At(value="INVOKE",target="Lcom/sorrowmist/useless/event/EventHandler;releaseBeefInvulnerability(Lnet/minecraft/world/entity/player/Player;)Z",remap=false),remap=false)
    private static boolean mp$rememberRelease(Player player){
        boolean released=releaseBeefInvulnerability(player);
        mp$releasedProtection.set(released);return released;
    }

    @Redirect(method="applyBeefInvulnerability",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;clearFire()V",remap=false),remap=false)
    private static void mp$extinguishOnlyOnActualRelease(Player player){
        boolean released=Boolean.TRUE.equals(mp$releasedProtection.get());mp$releasedProtection.remove();
        if(released)player.clearFire();
    }
}
