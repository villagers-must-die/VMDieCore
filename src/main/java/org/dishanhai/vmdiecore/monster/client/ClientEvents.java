package org.dishanhai.vmdiecore.monster.client;

import java.util.*;
import org.dishanhai.vmdiecore.monster.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

public final class ClientEvents {
    private static final Map<UUID,LivingEntity> PROXIES=new HashMap<>();
    private static final EquipmentSlot[] SPIDER_SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND};
    private static final float[][] SPIDER_EQUIPMENT_POSITIONS={{0,1.02F,-0.48F},{0,1.0F,0.05F},{-0.30F,0.75F,0.42F},{0.30F,0.75F,0.42F},{-0.72F,0.55F,-0.35F},{0.72F,0.55F,-0.35F}};
    public static void clear(){PROXIES.clear();}
    private static LivingEntity proxy(Player p,Race r){
        LivingEntity proxy=PROXIES.get(p.getUUID());
        if(proxy==null || proxy.getType()!=r.model || proxy.level()!=p.level()){
            proxy=r.model.create(p.level());if(proxy!=null)PROXIES.put(p.getUUID(),proxy);
        }
        return proxy;
    }
    @SubscribeEvent public void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();ClientState.tick();
        while(ClientEntrypoint.ARCHIVE.consumeClick())if(mc.player!=null)mc.setScreen(new ArchiveScreen());
    }
    @SubscribeEvent public void logout(ClientPlayerNetworkEvent.LoggingOut e){ClientState.clear();}
    @SubscribeEvent public void playerRender(RenderPlayerEvent.Pre e){
        Player p=e.getEntity();Race r=Race.of(p);if(r==null || r.model==null || p.isSpectator() || p.getPersistentData().getBoolean("mp_preview"))return;
        var mc=Minecraft.getInstance();
        LivingEntity proxy=proxy(p,r);if(proxy==null)return;
        float partial=e.getPartialTick();
        proxy.tickCount=p.tickCount;proxy.setPos(p.position());proxy.xo=p.xo;proxy.yo=p.yo;proxy.zo=p.zo;
        proxy.setYRot(p.getYRot());proxy.yRotO=p.yRotO;proxy.setXRot(p.getXRot());proxy.xRotO=p.xRotO;
        proxy.yBodyRot=p.yBodyRot;proxy.yBodyRotO=p.yBodyRotO;proxy.yHeadRot=p.yHeadRot;proxy.yHeadRotO=p.yHeadRotO;
        proxy.attackAnim=p.attackAnim;proxy.oAttackAnim=p.oAttackAnim;proxy.hurtTime=p.hurtTime;proxy.deathTime=p.deathTime;
        proxy.setInvisible(p.isInvisible());proxy.setPose(p.getPose());proxy.setOnGround(p.onGround());
        proxy.setSharedFlagOnFire(p.isOnFire());
        proxy.setShiftKeyDown(p.isShiftKeyDown());if(proxy instanceof Mob mob)mob.setAggressive(p.isUsingItem());
        proxy.setDeltaMovement(p.getDeltaMovement());proxy.walkAnimation.setSpeed(p.walkAnimation.speed());
        ((org.dishanhai.vmdiecore.monster.mixin.WalkAnimationAccessor)proxy.walkAnimation).mp$oldSpeed(p.walkAnimation.speed(0));
        ((org.dishanhai.vmdiecore.monster.mixin.WalkAnimationAccessor)proxy.walkAnimation).mp$position(p.walkAnimation.position());
        proxy.swinging=p.swinging;proxy.swingingArm=p.swingingArm;
        for(EquipmentSlot slot:EquipmentSlot.values())if(slot.getType()!=EquipmentSlot.Type.ANIMAL_ARMOR)proxy.setItemSlot(slot,p.getItemBySlot(slot));
        if(p.isUsingItem())proxy.startUsingItem(p.getUsedItemHand());else proxy.stopUsingItem();
        e.setCanceled(true);
        @SuppressWarnings("unchecked") EntityRenderer<LivingEntity> renderer=(EntityRenderer<LivingEntity>)(EntityRenderer<?>)mc.getEntityRenderDispatcher().getRenderer(proxy);
        renderer.render(proxy,e.getEntity().getYRot(),partial,e.getPoseStack(),e.getMultiBufferSource(),e.getPackedLight());
        if(r==Race.ARTHROPOD)renderSpiderEquipment(p,e);
        var names=(org.dishanhai.vmdiecore.monster.mixin.EntityRendererInvoker)e.getRenderer();
        if(names.mp$showName(p))names.mp$nameTag(p,p.getDisplayName(),e.getPoseStack(),e.getMultiBufferSource(),e.getPackedLight(),partial);
    }
    // SpiderRenderer has no humanoid armor or held-item layers. Show the actual
    // equipped item models on its body so other players can identify each slot.
    private static void renderSpiderEquipment(Player player,RenderPlayerEvent.Pre event){
        var pose=event.getPoseStack();
        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180-net.minecraft.util.Mth.rotLerp(event.getPartialTick(),player.yBodyRotO,player.yBodyRot)));
        for(int i=0;i<SPIDER_SLOTS.length;i++){
            var stack=player.getItemBySlot(SPIDER_SLOTS[i]);if(stack.isEmpty())continue;
            pose.pushPose();pose.translate(SPIDER_EQUIPMENT_POSITIONS[i][0],SPIDER_EQUIPMENT_POSITIONS[i][1],SPIDER_EQUIPMENT_POSITIONS[i][2]);pose.scale(0.55F,0.55F,0.55F);
            Minecraft.getInstance().getItemRenderer().renderStatic(player,stack,net.minecraft.world.item.ItemDisplayContext.FIXED,false,pose,
                event.getMultiBufferSource(),player.level(),event.getPackedLight(),
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,player.getId()+i);
            pose.popPose();
        }
        pose.popPose();
    }
    @SubscribeEvent public void hunger(RenderGuiLayerEvent.Pre e){
        if(e.getName().equals(net.minecraft.resources.ResourceLocation.withDefaultNamespace("food_level")) && Minecraft.getInstance().player!=null && Race.of(Minecraft.getInstance().player)==Race.SKELETON)e.setCanceled(true);
    }
    @SubscribeEvent public void arm(RenderArmEvent e){
        Race race=Race.of(e.getPlayer());if(race==null || race.model==null)return;
        if(race==Race.ARTHROPOD){e.setCanceled(true);return;}
        var mc=Minecraft.getInstance();var proxy=proxy(e.getPlayer(),race);if(proxy==null)return;
        var renderer=mc.getEntityRenderDispatcher().getRenderer(proxy);
        if(renderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer<?,?> living && living.getModel() instanceof net.minecraft.client.model.HumanoidModel<?> model){
            var part=e.getArm()==HumanoidArm.RIGHT?model.rightArm:model.leftArm;
            var saved=part.storePose();part.xRot=0;part.yRot=0;part.zRot=0;
            e.getPoseStack().pushPose();if(race==Race.ENDERMAN)e.getPoseStack().translate(0,0.875,0);
            var buffer=e.getMultiBufferSource().getBuffer(net.minecraft.client.renderer.RenderType.entitySolid(texture(race)));
            part.render(e.getPoseStack(),buffer,e.getPackedLight(),net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            e.getPoseStack().popPose();part.loadPose(saved);e.setCanceled(true);
        }
    }
    private static net.minecraft.resources.ResourceLocation texture(Race r){
        return net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/entity/"+switch(r){case ZOMBIE->"zombie/zombie.png";case SKELETON->"skeleton/skeleton.png";default->"enderman/enderman.png";});
    }
}
