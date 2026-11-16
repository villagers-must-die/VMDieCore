package org.dishanhai.vmdiecore.monster.mixin;
import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WalkAnimationState.class)
public interface WalkAnimationAccessor {
    @Accessor("speedOld") void mp$oldSpeed(float value);
    @Accessor("position") void mp$position(float value);
}
