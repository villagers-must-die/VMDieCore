package org.dishanhai.vmdiecore.monster;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

public final class Integration {
    public static void setup(FMLCommonSetupEvent e){
        e.enqueueWork(()->{if(ModList.get().isLoaded("curios"))CuriosBridge.register();});
    }
    public static Race materialTech(ItemStack s){return ModList.get().isLoaded("modulargolems")?GolemsBridge.materialTech(s):null;}
    public static boolean tryRevive(LivingDeathEvent e){return ModList.get().isLoaded("modulargolems") && GolemsBridge.revive(e);}

    private static final class GolemsBridge {
        static Race materialTech(ItemStack s){
            var mat=dev.xkmc.modulargolems.content.item.golem.GolemPart.getMaterial(s).orElse(null);
            return MonsterPerspective.id("flesh").equals(mat)?Race.ZOMBIE:MonsterPerspective.id("bone").equals(mat)?Race.SKELETON:null;
        }
        static boolean has(dev.xkmc.modulargolems.content.entity.common.AbstractGolemEntity<?,?> g,String material){
            return g.getMaterials().stream().anyMatch(m->m.id().equals(MonsterPerspective.id(material)));
        }
        static boolean revive(LivingDeathEvent e){
            LivingEntity entity=e.getEntity();long now=entity.level().getGameTime();
            if(entity instanceof dev.xkmc.modulargolems.content.entity.common.AbstractGolemEntity<?,?> g
                && has(g,"bone") && now>=g.getPersistentData().getLong("mp_bone_after")){
                e.setCanceled(true);g.setHealth(g.getMaxHealth());g.clearFire();g.invulnerableTime=40;
                g.getPersistentData().putLong("mp_bone_after",now+20L*MonsterPerspective.BONE_COOLDOWN.get());
                return true;
            }
            if(!(entity instanceof ServerPlayer p) || !Race.hasTech(p,Race.ZOMBIE) || p.getPersistentData().getBoolean("mp_cure_death"))return false;
            for(var level:p.getServer().getAllLevels()){
                for(var ent:level.getAllEntities()){
                    if(ent instanceof dev.xkmc.modulargolems.content.entity.common.AbstractGolemEntity<?,?> g
                        && g.isAlive() && p.getUUID().equals(g.getOwnerUUID()) && has(g,"flesh") && TechItem.safe(level,p,g.position(),g)){
                        var pos=g.position();g.discard();e.setCanceled(true);p.setHealth(p.getMaxHealth());p.clearFire();p.fallDistance=0;p.invulnerableTime=60;
                        p.teleportTo(level,pos.x,pos.y,pos.z,p.getYRot(),p.getXRot());RaceEvents.message(p,"monsterperspective.message.revive");return true;
                    }
                }
            }
            return false;
        }
    }
    public static final class CuriosBridge {
        static void register(){
            top.theillusivec4.curios.api.CuriosApi.registerCurio(MonsterPerspective.FULL_SOUL.get(),new top.theillusivec4.curios.api.type.capability.ICurioItem(){
                @Override public void curioTick(top.theillusivec4.curios.api.SlotContext context,ItemStack s){
                    if(context.entity() instanceof Player p && !p.level().isClientSide){
                        var a=p.getAttribute(Attributes.MOVEMENT_SPEED);var id=MonsterPerspective.id("soul_speed");
                        if(a!=null && Race.hasTech(p,Race.SKELETON) && !a.hasModifier(id))a.addTransientModifier(new AttributeModifier(id,0.5,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                        if(a!=null && !Race.hasTech(p,Race.SKELETON))a.removeModifier(id);
                    }
                }
                @Override public void onUnequip(top.theillusivec4.curios.api.SlotContext context,ItemStack next,ItemStack prev){
                    var a=context.entity().getAttribute(Attributes.MOVEMENT_SPEED);if(a!=null)a.removeModifier(MonsterPerspective.id("soul_speed"));
                }
            });
        }
    }
}
