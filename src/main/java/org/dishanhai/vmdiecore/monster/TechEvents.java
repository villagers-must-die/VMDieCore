package org.dishanhai.vmdiecore.monster;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class TechEvents {
    @SubscribeEvent public void brewing(net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent e){
        var b=e.getBuilder();
        var item=MonsterPerspective.VENOM.get();
        b.addMix(net.minecraft.world.item.alchemy.Potions.WATER,item,net.minecraft.world.item.alchemy.Potions.WEAKNESS);
        b.addMix(net.minecraft.world.item.alchemy.Potions.NIGHT_VISION,item,net.minecraft.world.item.alchemy.Potions.INVISIBILITY);
        b.addMix(net.minecraft.world.item.alchemy.Potions.LONG_NIGHT_VISION,item,net.minecraft.world.item.alchemy.Potions.LONG_INVISIBILITY);
        b.addMix(net.minecraft.world.item.alchemy.Potions.HEALING,item,net.minecraft.world.item.alchemy.Potions.HARMING);
        b.addMix(net.minecraft.world.item.alchemy.Potions.STRONG_HEALING,item,net.minecraft.world.item.alchemy.Potions.STRONG_HARMING);
        b.addMix(net.minecraft.world.item.alchemy.Potions.POISON,item,net.minecraft.world.item.alchemy.Potions.HARMING);
        b.addMix(net.minecraft.world.item.alchemy.Potions.LONG_POISON,item,net.minecraft.world.item.alchemy.Potions.HARMING);
        b.addMix(net.minecraft.world.item.alchemy.Potions.STRONG_POISON,item,net.minecraft.world.item.alchemy.Potions.STRONG_HARMING);
        b.addMix(net.minecraft.world.item.alchemy.Potions.SWIFTNESS,item,net.minecraft.world.item.alchemy.Potions.SLOWNESS);
        b.addMix(net.minecraft.world.item.alchemy.Potions.LONG_SWIFTNESS,item,net.minecraft.world.item.alchemy.Potions.LONG_SLOWNESS);
        b.addMix(net.minecraft.world.item.alchemy.Potions.LEAPING,item,net.minecraft.world.item.alchemy.Potions.SLOWNESS);
        b.addMix(net.minecraft.world.item.alchemy.Potions.LONG_LEAPING,item,net.minecraft.world.item.alchemy.Potions.LONG_SLOWNESS);
    }
    @SubscribeEvent public void rightClick(PlayerInteractEvent.RightClickItem e){
        Player p=e.getEntity();ItemStack stack=e.getItemStack();
        // Main-hand empty interaction can arrive before the off-hand item; handle each only once.
        if(stack.is(Items.GLASS_BOTTLE) && Race.hasTech(p,Race.ARTHROPOD)){
            if(!p.level().isClientSide && !p.getCooldowns().isOnCooldown(Items.GLASS_BOTTLE)){
                stack.shrink(1);RaceEvents.give(p,MonsterPerspective.VENOM.toStack());p.getCooldowns().addCooldown(Items.GLASS_BOTTLE,10);
            }
            e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
        }else if(e.getHand()==InteractionHand.OFF_HAND && Race.hasTech(p,Race.ENDERMAN) && isKnife(stack)){
            if(!p.level().isClientSide && !p.getCooldowns().isOnCooldown(stack.getItem())){
                if(p.getHealth()<=2){RaceEvents.message(p,"生命不足，无法割取珍珠。");}
                else if(p.hurt(p.damageSources().generic(),2)){RaceEvents.give(p,new ItemStack(Items.ENDER_PEARL));p.getCooldowns().addCooldown(stack.getItem(),20);}
            }
            e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
    public static boolean isKnife(ItemStack s){return s.is(ItemTags.SWORDS) || s.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c","tools/knife")))
        || net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().contains("knife");}
    @SubscribeEvent public void kill(LivingDeathEvent e){
        if(e.getEntity().level().isClientSide)return;
        if(Integration.tryRevive(e))return;
        LivingEntity victim=e.getEntity();
        if(!(e.getSource().getEntity() instanceof Player p) || !Race.hasTech(p,Race.SKELETON))return;
        if(!(victim instanceof Villager || victim instanceof AbstractIllager || victim instanceof Pig || victim instanceof Cow || victim instanceof Sheep))return;
        for(int i=0;i<p.getInventory().getContainerSize();i++){
            ItemStack s=p.getInventory().getItem(i);if(s.is(MonsterPerspective.SOUL_BOTTLE)){
                s.shrink(1);RaceEvents.give(p,MonsterPerspective.FULL_SOUL.toStack());RaceEvents.message(p,"已捕获灵魂。");break;
            }
        }
    }
    public static Race required(ItemStack stack){
        var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(id.getNamespace().equals("enderio") && (id.getPath().equals("travel_anchor") || id.getPath().equals("staff_of_travelling")))return Race.ENDERMAN;
        if(id.getNamespace().equals(MonsterPerspective.ID)){
            String path=id.getPath();if(path.startsWith("silk_") || path.equals("venom_bottle"))return Race.ARTHROPOD;
            if(path.equals("flesh_part"))return Race.ZOMBIE;
            if(path.contains("soul") || path.equals("bone_part"))return Race.SKELETON;
            if(path.contains("pearl") || path.equals("teleport_scroll"))return Race.ENDERMAN;
        }
        return Integration.materialTech(stack);
    }
}
