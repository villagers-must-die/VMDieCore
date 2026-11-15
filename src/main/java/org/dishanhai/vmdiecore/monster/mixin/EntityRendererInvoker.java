package org.dishanhai.vmdiecore.monster.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityRenderer.class)
public interface EntityRendererInvoker {
    @Invoker("shouldShowName") boolean mp$showName(Entity entity);
    @Invoker("renderNameTag") void mp$nameTag(Entity entity,Component text,PoseStack poses,MultiBufferSource buffer,int light,float partial);
}
