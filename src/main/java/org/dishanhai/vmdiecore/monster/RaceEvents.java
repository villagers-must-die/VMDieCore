package org.dishanhai.vmdiecore.monster;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RaceEvents {
    public static boolean choose(ServerPlayer p,Race r,boolean force){
        Race old=Race.of(p);
        if(old==r)return true;
        if(!force && old!=null){
            if(!MonsterPerspective.RESELECT.get()){message(p,"monsterperspective.message.race_0");return false;}
            long remain=p.getPersistentData().getLong("mp_change_after")-p.serverLevel().getGameTime();
            if(remain>0){message(p,"monsterperspective.message.race_1",(remain+19)/20);return false;}
        }
        if(!force && (!p.isAlive() || p.isPassenger()))return false;
        // Check the future body before committing, so changing under a low ceiling cannot trap the player.
        float w=r==Race.ARTHROPOD?1.4F:0.6F,h=r==Race.ENDERMAN?3F:r==Race.ARTHROPOD?1F:1.8F;
        if(!p.level().noCollision(p,EntityDimensions.scalable(w,h).makeBoundingBox(p.position()).deflate(0.01))){
            message(p,"monsterperspective.message.race_2");return false;
        }
        float fraction=p.getHealth()/p.getMaxHealth();
        p.getPersistentData().putString("mp_race",r.id());
        p.getPersistentData().putLong("mp_change_after",p.serverLevel().getGameTime()+20L*MonsterPerspective.CHANGE_COOLDOWN.get());
        applyAttributes(p);
        p.setHealth(Math.max(1,p.getMaxHealth()*fraction));
        if(old!=null && old.burns() && !r.burns())p.clearFire();
        p.refreshDimensions(); Network.sync(p);
        p.containerMenu.slotsChanged(p.getInventory());
        message(p,"monsterperspective.message.race_3",Component.translatable(r.title));
        return true;
    }
    public static void message(Player p,String key,Object... args){p.displayClientMessage(Component.translatable(key,args),true);}
    private static void modifier(Player p,net.minecraft.core.Holder<Attribute> attribute,String key,double amount,AttributeModifier.Operation op){
        AttributeInstance a=p.getAttribute(attribute); if(a==null)return;
        var id=MonsterPerspective.id(key); a.removeModifier(id);
        if(amount!=0)a.addTransientModifier(new AttributeModifier(id,amount,op));
    }
    public static void applyAttributes(Player p){
        Race r=Race.of(p);
        modifier(p,Attributes.MAX_HEALTH,"race_health",r==Race.SKELETON?-0.2:r==Race.ENDERMAN?1:0,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modifier(p,Attributes.ARMOR,"race_armor",r==Race.ZOMBIE?2:0,AttributeModifier.Operation.ADD_VALUE);
        modifier(p,Attributes.MOVEMENT_SPEED,"race_speed",r==Race.SKELETON?0.2:0,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent e){
        if(e.getEntity() instanceof ServerPlayer p){
            applyAttributes(p);p.refreshDimensions();
            for(ServerPlayer other:p.getServer().getPlayerList().getPlayers())PacketDistributor.sendToPlayer(p,Network.packet(other,false));
            Network.sync(p);if(Race.of(p)==null)PacketDistributor.sendToPlayer(p,Network.packet(p,true));
        }
    }
    @SubscribeEvent public void track(PlayerEvent.StartTracking e){if(e.getEntity() instanceof ServerPlayer p && e.getTarget() instanceof ServerPlayer t)PacketDistributor.sendToPlayer(p,Network.packet(t,false));}
    @SubscribeEvent public void clone(PlayerEvent.Clone e){
        var old=e.getOriginal().getPersistentData();
        for(String key:old.getAllKeys())if(key.startsWith("mp_"))e.getEntity().getPersistentData().put(key,old.get(key).copy());
    }
    @SubscribeEvent public void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p){applyAttributes(p);p.refreshDimensions();Network.sync(p);}}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){p.refreshDimensions();Network.sync(p);}}
    public static boolean isExposedToSun(Player p){
        BlockPos eyes=BlockPos.containing(p.getX(),p.getEyeY(),p.getZ());
        return p.level().dimensionType().hasSkyLight() && p.level().isDay()
            && p.level().canSeeSky(eyes) && !p.level().isRainingAt(eyes)
            && !p.isInWaterOrBubble() && !p.isInPowderSnow;
    }
    @SubscribeEvent public void tick(PlayerTickEvent.Post e){
        Player p=e.getEntity();Race r=Race.of(p);if(r==null || !p.isAlive())return;
        // Fluid movement must be predicted by the local client as well as simulated on the server.
        // Exempt flight-capable players before takeoff as well: forcing them down while the
        // double-jump is being processed can land them again and cancel creative flight.
        if(!p.isSpectator() && !p.isCreative() && !p.mayFly() && !p.getAbilities().flying && !p.isPassenger() && p.isInWater()){
            if(r==Race.ZOMBIE)p.setDeltaMovement(p.getDeltaMovement().x,Math.min(p.getDeltaMovement().y,-0.08),p.getDeltaMovement().z);
            if(r==Race.SKELETON){p.setSwimming(false);p.setDeltaMovement(p.getDeltaMovement().x,0.08,p.getDeltaMovement().z);}
        }
        if(r==Race.ARTHROPOD && p.horizontalCollision && !p.isShiftKeyDown() && !p.mayFly() && !p.getAbilities().flying && !p.isSpectator()){
            p.setDeltaMovement(p.getDeltaMovement().x,0.16,p.getDeltaMovement().z);p.fallDistance=0;
        }
        if(p.level().isClientSide)return;
        if(r==Race.SKELETON){p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(5);p.getFoodData().setExhaustion(0);}
        if(p.isSpectator())return;
        if(r.burns() && p.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && isExposedToSun(p)){
            // Do not reset a running timer: baseTick damages on multiples of 20. Resetting
            // it to 160 every tick would turn ordinary fire into 20 hits per second.
            if(p.getRemainingFireTicks()<=0)p.igniteForSeconds(8);
            // Player clamps creative fire to one tick. Publish the visual flag after ignition,
            // instead of waiting for the following baseTick, which may already extinguish it.
            // Creative damage immunity is retained; survival uses the normal fire damage path.
            p.setSharedFlagOnFire(p.getRemainingFireTicks()>0);
        }
        if(!p.isCreative() && p.tickCount%20==0){
            if(r==Race.ENDERMAN && p.isInWaterRainOrBubble())p.hurt(p.damageSources().drown(),1);
        }
    }
    @SubscribeEvent public void incoming(LivingIncomingDamageEvent e){
        if(!(e.getEntity() instanceof Player p))return; Race r=Race.of(p);if(r==null)return;
        if(r==Race.VAMPIRE && e.getSource().is(DamageTypes.FALL)){e.setCanceled(true);return;}
        if(e.getSource().getEntity() instanceof LivingEntity attacker && e.getSource().getDirectEntity()==attacker){
            var key=r.undead()?Enchantments.SMITE:r==Race.ARTHROPOD?Enchantments.BANE_OF_ARTHROPODS:null;
            if(key!=null){int level=EnchantmentHelper.getItemEnchantmentLevel(p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key),attacker.getMainHandItem());
                if(level>0){e.setAmount(e.getAmount()+2.5F*level);if(r==Race.ARTHROPOD)p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,20+attacker.getRandom().nextInt(10*level+1),3));}}
        }
    }
    @SubscribeEvent public void damaged(LivingDamageEvent.Post e){
        if(e.getNewDamage()<=0 || e.getEntity().level().isClientSide)return;
        if(e.getSource().getEntity() instanceof Player attacker && Race.of(attacker)==Race.ARTHROPOD
            && e.getSource().getDirectEntity()==attacker && e.getEntity()!=attacker)
            e.getEntity().addEffect(new MobEffectInstance(MobEffects.POISON,100,0),attacker);
        if(!(e.getEntity() instanceof ServerPlayer p) || Race.of(p)!=Race.VAMPIRE || !p.isAlive())return;
        long now=p.serverLevel().getGameTime();if(now<p.getPersistentData().getLong("mp_bats_after"))return;
        p.getPersistentData().putLong("mp_bats_after",now+200);
        Bat first=null;
        for(int i=0;i<4;i++){
            Bat bat=EntityType.BAT.create(p.level());if(bat==null)continue;
            bat.moveTo(p.getX()+(i%2==0?0.7:-0.7),p.getY()+1,p.getZ()+(i<2?0.7:-0.7),0,0);bat.setResting(false);
            bat.getPersistentData().putUUID("mp_bat_owner",p.getUUID());bat.getPersistentData().putLong("mp_bat_expire",now+120);
            p.level().addFreshEntity(bat);if(first==null)first=bat;
        }
        if(first!=null)for(Mob mob:p.level().getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(16)))
            if(mob.getTarget()==p || mob==e.getSource().getEntity())mob.setTarget(first);
    }
    @SubscribeEvent public void target(LivingChangeTargetEvent e){
        if(!(e.getNewAboutToBeSetTarget() instanceof Player p) || Race.of(p)!=Race.VAMPIRE)return;
        var bats=p.level().getEntitiesOfClass(Bat.class,p.getBoundingBox().inflate(16),b->b.isAlive() && b.getPersistentData().hasUUID("mp_bat_owner") && b.getPersistentData().getUUID("mp_bat_owner").equals(p.getUUID()));
        if(!bats.isEmpty())e.setNewAboutToBeSetTarget(bats.getFirst());
    }
    @SubscribeEvent public void entityTick(EntityTickEvent.Post e){
        Entity ent=e.getEntity();if(!ent.level().isClientSide && ent.getPersistentData().contains("mp_bat_expire") && ent.level().getGameTime()>=ent.getPersistentData().getLong("mp_bat_expire"))ent.discard();
    }
    @SubscribeEvent public void foodStart(LivingEntityUseItemEvent.Start e){
        if(e.getEntity() instanceof Player p && e.getItem().is(Items.MILK_BUCKET))p.getPersistentData().putBoolean("mp_milk_full",p.getHealth()>=p.getMaxHealth());
    }
    @SubscribeEvent public void foodEffect(MobEffectEvent.Applicable e){
        if(!(e.getEntity() instanceof Player p) || !p.isUsingItem())return;
        Race r=Race.of(p);
        if(r==Race.ZOMBIE && p.getUseItem().is(Items.ROTTEN_FLESH) && e.getEffectInstance().is(MobEffects.HUNGER))e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        if(r==Race.ARTHROPOD && p.getUseItem().is(Items.SPIDER_EYE) && e.getEffectInstance().is(MobEffects.POISON))e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
    @SubscribeEvent public void finish(LivingEntityUseItemEvent.Finish e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;Race r=Race.of(p);
        if(r==Race.ZOMBIE && (e.getItem().is(Items.GOLDEN_APPLE)||e.getItem().is(Items.ENCHANTED_GOLDEN_APPLE)) && p.hasEffect(MobEffects.WEAKNESS)){
            p.getPersistentData().putBoolean("mp_cure_death",true);p.kill();p.getPersistentData().remove("mp_cure_death");
        }
        if(r==Race.SKELETON && e.getItem().is(Items.MILK_BUCKET)){
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION,100,1));p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,2400,0));
        }
        if(Race.hasTech(p,Race.SKELETON) && e.getItem().is(Items.MILK_BUCKET) && p.getPersistentData().getBoolean("mp_milk_full"))give(p,new ItemStack(Items.BONE));
    }
    public static void give(Player p,ItemStack stack){if(!p.getInventory().add(stack))p.drop(stack,false);}
}
