package org.dishanhai.vmdiecore.monster.test;

import org.dishanhai.vmdiecore.monster.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;
import java.util.List;

@GameTestHolder(MonsterPerspective.ID)
@PrefixGameTestTemplate(false)
public final class MonsterGameTests {
    @GameTest(template="empty") public static void migrationKeepsReleasedRegistryAndPlayerData(GameTestHelper h){
        h.assertTrue(net.neoforged.fml.ModList.get().isLoaded("vmdiecore"),"Core must own the migrated module");
        h.assertTrue(!net.neoforged.fml.ModList.get().isLoaded("monsterperspective"),"Standalone mod must not be loaded alongside core");
        h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(MonsterPerspective.FLESH.get()).equals(MonsterPerspective.id("flesh_part")),"Released item namespace retained");
        h.assertTrue(Network.Sync.TYPE.id().getNamespace().equals("monsterperspective"),"Released payload namespace retained");
        var original=serverPlayer(h,Race.SKELETON);original.getPersistentData().putLong("mp_change_after",12345);
        var clone=serverPlayer(h,Race.ZOMBIE);
        new RaceEvents().clone(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(clone,original,true));
        RaceEvents.applyAttributes(clone);
        h.assertTrue(Race.of(clone)==Race.SKELETON && Math.abs(clone.getMaxHealth()-16)<0.01,"Legacy player race survives cloning");
        h.assertTrue(clone.getPersistentData().getLong("mp_change_after")==12345,"Legacy reselection cooldown retained");h.succeed();
    }
    @GameTest(template="empty") public static void coreStackingRoundTripsMigratedTechnology(GameTestHelper h){
        var id=MonsterPerspective.id("flesh_part");
        org.dishanhai.vmdiecore.item.StackLimits.setLimit(id,512);
        try{
            var stack=MonsterPerspective.FLESH.toStack(300);
            h.assertTrue(stack.getMaxStackSize()==512,"Core stacking applies to migrated technology");
            var encoded=stack.save(h.getLevel().registryAccess());
            var decoded=ItemStack.parse(h.getLevel().registryAccess(),encoded).orElseThrow();
            h.assertTrue(decoded.is(MonsterPerspective.FLESH) && decoded.getCount()==300,"Large legacy-namespace technology stack saves and loads");
            var p=serverPlayer(h,Race.ZOMBIE);p.getInventory().add(decoded);
            h.assertTrue(p.getInventory().countItem(MonsterPerspective.FLESH.get())==300,"Inventory accepts the entire enlarged technology stack");
        }finally{org.dishanhai.vmdiecore.item.StackLimits.setLimit(id,64);}
        h.succeed();
    }
    @GameTest(template="empty") public static void coreTechnologyQueriesRespectAllRaces(GameTestHelper h){
        for(Race race:Race.values()){
            var p=serverPlayer(h,race);
            h.assertTrue(org.dishanhai.vmdiecore.compat.kubejs.TechnologyBinding.getRace(p).equals(race.id()),"Script query reads migrated race data");
            for(Race tech:List.of(Race.ZOMBIE,Race.SKELETON,Race.ENDERMAN,Race.ARTHROPOD)){
                h.assertTrue(org.dishanhai.vmdiecore.compat.kubejs.TechnologyBinding.hasTech(p,tech.id())==(race==tech||race==Race.VAMPIRE),"Script technology gate: "+race+" -> "+tech);
            }
        }
        h.succeed();
    }
    private static Player player(GameTestHelper h,Race race){
        Player p=h.makeMockPlayer(GameType.SURVIVAL);p.getPersistentData().putString("mp_race",race.id());RaceEvents.applyAttributes(p);p.refreshDimensions();p.setHealth(p.getMaxHealth());return p;
    }
    @GameTest(template="empty") public static void healthAndArmorDoNotStack(GameTestHelper h){
        Player p=player(h,Race.ZOMBIE);RaceEvents.applyAttributes(p);h.assertTrue(p.getArmorValue()==2,"Zombie armor must be exactly +2");
        p.getPersistentData().putString("mp_race","skeleton");RaceEvents.applyAttributes(p);RaceEvents.applyAttributes(p);
        h.assertTrue(Math.abs(p.getMaxHealth()-16)<0.01,"Skeleton health must be 16");h.assertTrue(p.getArmorValue()==0,"Previous armor removed");
        p.getPersistentData().putString("mp_race","enderman");RaceEvents.applyAttributes(p);h.assertTrue(p.getMaxHealth()==40,"Enderman health must be 40");h.succeed();
    }
    @GameTest(template="empty") public static void bodyAndEyeHeights(GameTestHelper h){
        Player p=player(h,Race.ENDERMAN);h.assertTrue(p.getDimensions(Pose.STANDING).height()==3,"Enderman 3 block body");
        h.assertTrue(p.getDimensions(Pose.CROUCHING).height()==3,"Enderman cannot bypass ceiling by crouching");
        h.assertTrue(p.getEyeHeight()>2.6,"Enderman eye level");
        p.getPersistentData().putString("mp_race","arthropod");p.refreshDimensions();h.assertTrue(p.getBbHeight()==1,"Spider 1 block body");h.assertTrue(p.getBbWidth()==1.4F,"Spider width");h.succeed();
    }
    @GameTest(template="empty") public static void undeadPotionsReverse(GameTestHelper h){
        Player p=player(h,Race.SKELETON);p.setHealth(10);
        MobEffects.HARM.value().applyInstantenousEffect(null,null,p,0,1);h.assertTrue(p.getHealth()>10,"Harming must heal undead");
        float before=p.getHealth();p.invulnerableTime=0;
        MobEffects.HEAL.value().applyInstantenousEffect(null,null,p,0,1);h.assertTrue(p.getHealth()<before,"Healing must damage undead");h.succeed();
    }
    @GameTest(template="empty") public static void skeletonCanEatWithoutHunger(GameTestHelper h){
        Player p=player(h,Race.SKELETON);p.getFoodData().setFoodLevel(20);h.assertTrue(p.canEat(false),"Skeleton can eat at full hidden food level");p.setSwimming(true);h.assertTrue(!p.isSwimming(),"Skeleton cannot swim");h.succeed();
    }
    @GameTest(template="empty") public static void wallClimbAndWaterSensitivity(GameTestHelper h){
        Player p=player(h,Race.ARTHROPOD);p.horizontalCollision=true;h.assertTrue(p.onClimbable(),"Spider can climb a wall");
        p.getPersistentData().putString("mp_race","enderman");h.assertTrue(p.isSensitiveToWater(),"Enderman reacts to water bottles");h.succeed();
    }
    @GameTest(template="empty") public static void vampireBodyAndFallImmunity(GameTestHelper h){
        Player p=player(h,Race.VAMPIRE);h.assertTrue(p.isInvisible(),"Naked vampire invisible");h.assertTrue(!p.hasEffect(MobEffects.INVISIBILITY),"No particle-emitting potion effect");
        p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.LEATHER_HELMET));h.assertTrue(!p.isInvisible(),"Equipment reveals vampire");
        p.hurt(p.damageSources().fall(),100);h.assertTrue(p.getHealth()==20,"Fall damage must be canceled");h.succeed();
    }
    @GameTest(template="empty") public static void vampireSharesOnlyTechnology(GameTestHelper h){
        Player p=player(h,Race.VAMPIRE);for(Race r:Race.values())h.assertTrue(Race.hasTech(p,r),"Vampire must have every technology");
        h.assertTrue(!p.isInvertedHealAndHarm(),"Vampire must not inherit zombie potion weakness");h.assertTrue(p.getBbHeight()==1.8F,"Vampire keeps player body");h.succeed();
    }
    @GameTest(template="empty") public static void venomReplacesFermentedEye(GameTestHelper h){
        h.assertTrue(Ingredient.of(Items.FERMENTED_SPIDER_EYE).test(MonsterPerspective.VENOM.toStack()),"Venom must match fermented spider eye recipes");
        var shapeless=new ShapelessRecipe("venom_test",CraftingBookCategory.MISC,new ItemStack(Items.DIAMOND),net.minecraft.core.NonNullList.of(Ingredient.EMPTY,Ingredient.of(Items.FERMENTED_SPIDER_EYE),Ingredient.of(Items.PAPER)));
        h.assertTrue(shapeless.matches(CraftingInput.of(2,1,List.of(MonsterPerspective.VENOM.toStack(),new ItemStack(Items.PAPER))),h.getLevel()),"Optimized shapeless ingredient matching accepts venom");
        h.assertTrue(!Ingredient.of(Items.SPIDER_EYE).test(MonsterPerspective.VENOM.toStack()),"Venom must not replace raw eye");h.succeed();
    }
    @GameTest(template="empty") public static void silkPreservesAndRejectsConflicts(GameTestHelper h){
        SilkRecipe recipe=new SilkRecipe(CraftingBookCategory.EQUIPMENT);
        ItemStack pick=new ItemStack(Items.DIAMOND_PICKAXE);
        var lookup=h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        pick.enchant(lookup.getOrThrow(Enchantments.UNBREAKING),2);
        ItemStack silk=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(MonsterPerspective.id("silk_fortune")));
        ItemStack result=recipe.assemble(CraftingInput.of(2,1,List.of(pick,silk)),h.getLevel().registryAccess());
        h.assertTrue(!result.isEmpty(),"Applicable silk must enchant");h.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(lookup.getOrThrow(Enchantments.UNBREAKING),result)==2,"Existing enchantment retained");
        h.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(lookup.getOrThrow(Enchantments.FORTUNE),result)==2,"Fortune II applied");
        pick.enchant(lookup.getOrThrow(Enchantments.SILK_TOUCH),1);
        h.assertTrue(recipe.assemble(CraftingInput.of(2,1,List.of(pick,silk)),h.getLevel().registryAccess()).isEmpty(),"Silk touch and fortune conflict rejected");h.succeed();
    }
    @GameTest(template="empty") public static void craftingGateChecksActualPlayer(GameTestHelper h){
        var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ArchiveTest"));var menu=p.inventoryMenu;
        menu.getSlot(1).set(new ItemStack(Items.ROTTEN_FLESH));menu.getSlot(2).set(new ItemStack(Items.SLIME_BALL));menu.slotsChanged(p.getInventory());
        h.assertTrue(menu.getSlot(0).getItem().isEmpty(),"Unawakened player cannot craft race technology");
        p.getPersistentData().putString("mp_race","zombie");menu.slotsChanged(p.getInventory());
        h.assertTrue(menu.getSlot(0).getItem().is(MonsterPerspective.FLESH),"Zombie can craft flesh parts");
        p.getPersistentData().putString("mp_race","skeleton");menu.slotsChanged(p.getInventory());
        h.assertTrue(menu.getSlot(0).getItem().isEmpty(),"Changing race immediately locks old recipe");
        p.getInventory().clearContent();h.succeed();
    }
    private static net.minecraft.server.level.ServerPlayer serverPlayer(GameTestHelper h,Race race){
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ArchiveTest");
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),connection,p,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
        };
        p.getPersistentData().putString("mp_race",race.id());RaceEvents.applyAttributes(p);p.setHealth(p.getMaxHealth());
        p.refreshDimensions();
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));p.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);return p;
    }
    @GameTest(template="empty") public static void milkGivesGoldenAppleAndBones(GameTestHelper h){
        var p=serverPlayer(h,Race.SKELETON);p.getPersistentData().putBoolean("mp_milk_full",true);
        new RaceEvents().finish(new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish(p,new ItemStack(Items.MILK_BUCKET),0,new ItemStack(Items.BUCKET)));
        h.assertTrue(p.hasEffect(MobEffects.REGENERATION)&&p.hasEffect(MobEffects.ABSORPTION),"Milk grants golden apple effects");
        h.assertTrue(p.getInventory().countItem(Items.BONE)==1,"Full health milk gives one bone");h.succeed();
    }
    @GameTest(template="empty") public static void smiteHurtsUndead(GameTestHelper h){
        Player attacker=player(h,Race.VAMPIRE),target=player(h,Race.ZOMBIE);
        var enchant=h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SMITE);
        ItemStack sword=new ItemStack(Items.IRON_SWORD);sword.enchant(enchant,3);attacker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sword);
        target.hurt(target.damageSources().playerAttack(attacker),2);h.assertTrue(target.getHealth()<13,"Smite III adds 7.5 damage before armor");h.succeed();
    }
    @GameTest(template="empty") public static void teleportScrollMarksAndReturns(GameTestHelper h){
        var p=serverPlayer(h,Race.ENDERMAN);var scroll=MonsterPerspective.SCROLL.toStack();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,scroll);
        var origin=p.position();MonsterPerspective.SCROLL.get().use(h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(TechItem.data(scroll).contains("dimension"),"First use marks dimension and position");
        p.setPos(origin.add(2,0,0));p.getCooldowns().removeCooldown(scroll.getItem());
        MonsterPerspective.SCROLL.get().use(h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(p.position().distanceTo(origin)<0.1,"Second use returns to marked position");h.assertTrue(scroll.isEmpty(),"Scroll consumed after successful return");h.succeed();
    }
    @GameTest(template="empty") public static void boundPearlBreaksForMissingEntity(GameTestHelper h){
        var p=serverPlayer(h,Race.ENDERMAN);var pearl=MonsterPerspective.BOUND.toStack();var n=new net.minecraft.nbt.CompoundTag();n.putUUID("target",java.util.UUID.randomUUID());TechItem.data(pearl,n);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,pearl);MonsterPerspective.BOUND.get().use(h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(pearl.isEmpty(),"Missing/unloaded target breaks binding pearl");h.succeed();
    }
    private static dev.xkmc.modulargolems.content.entity.common.AbstractGolemEntity<?,?> golem(GameTestHelper h,String material,Player owner){
        var type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("modulargolems","humanoid_golem"));
        var g=(dev.xkmc.modulargolems.content.entity.common.AbstractGolemEntity<?,?>)type.create(h.getLevel());
        var part=(dev.xkmc.modulargolems.content.item.golem.GolemPart<?,?>)net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("modulargolems","humanoid_golem_body"));
        var mat=part.parseMaterial(MonsterPerspective.id(material));
        g.onCreate(new java.util.ArrayList<>(List.of(mat,mat,mat)),dev.xkmc.modulargolems.content.item.data.GolemUpgrade.EMPTY,owner.getUUID());
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));g.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);h.getLevel().addFreshEntity(g);return g;
    }
    @GameTest(template="empty") public static void boneGolemRevivesWithCooldown(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("modulargolems")){h.succeed();return;}
        var p=serverPlayer(h,Race.SKELETON);var g=golem(h,"bone",p);g.setHealth(1);g.hurt(g.damageSources().genericKill(),10000);
        h.assertTrue(g.isAlive()&&g.getHealth()==g.getMaxHealth(),"Native bone golem survives lethal hit");
        h.assertTrue(g.getPersistentData().getLong("mp_bone_after")>h.getLevel().getGameTime(),"Revival starts cooldown");
        g.invulnerableTime=0;g.hurt(g.damageSources().genericKill(),10000);h.assertTrue(g.isDeadOrDying(),"Second lethal hit during cooldown kills golem");h.succeed();
    }
    @GameTest(template="empty") public static void soulCharmTicksAndUnequipsThroughCurios(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("curios")){h.succeed();return;}
        var p=serverPlayer(h,Race.SKELETON);p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));
        var inventory=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow();
        h.assertTrue(inventory.getStacksHandler("charm").isPresent(),"Migrated charm slot is assigned to players");
        double base=p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        inventory.setEquippedCurio("charm",0,MonsterPerspective.FULL_SOUL.toStack());curioPlayerTick(p);
        h.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED)-base*1.5)<0.00001,"Curios applies the registered soul speed modifier");
        inventory.setEquippedCurio("charm",0,ItemStack.EMPTY);curioPlayerTick(p);
        h.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED)-base)<0.00001,"Unequipping removes the speed modifier");h.succeed();
    }
    private static void curioPlayerTick(net.minecraft.server.level.ServerPlayer p){
        for(int i=0;i<2;i++){
            p.tickCount++;p.tick();p.doTick();
            // This unattached fixture has no ServerLevel.tickNonPassenger to dispatch the entity event.
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(p));
        }
    }
    @GameTest(template="empty") public static void coreLoadsNativeGolemMaterialsFromItsOwnModNamespace(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("modulargolems")){h.succeed();return;}
        try{
            var type=Class.forName("dev.xkmc.modulargolems.content.config.GolemMaterialConfig");
            var config=type.getMethod("get").invoke(null);
            var ingredient=type.getMethod("getCraftIngredient",net.minecraft.resources.ResourceLocation.class);
            h.assertTrue(((Ingredient)ingredient.invoke(config,MonsterPerspective.id("bone"))).test(MonsterPerspective.BONE.toStack()),"Soul bone ingredient must load through core's resource namespace");
            h.assertTrue(((Ingredient)ingredient.invoke(config,MonsterPerspective.id("flesh"))).test(MonsterPerspective.FLESH.toStack()),"Flesh ingredient must load through core's resource namespace");
        }catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
        h.succeed();
    }
    @GameTest(template="empty") public static void fleshGolemResurrectsOwner(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("modulargolems")){h.succeed();return;}
        var p=serverPlayer(h,Race.ZOMBIE);var g=golem(h,"flesh",p);var location=g.position();p.getInventory().add(new ItemStack(Items.DIAMOND));
        p.hurt(p.damageSources().genericKill(),10000);
        h.assertTrue(p.isAlive()&&p.getHealth()==p.getMaxHealth(),"Flesh golem resurrects owner");h.assertTrue(g.isRemoved(),"Flesh golem consumed");
        h.assertTrue(p.position().distanceTo(location)<0.1,"Owner returns at golem location");h.assertTrue(p.getInventory().countItem(Items.DIAMOND)==1,"Inventory retained");h.succeed();
    }
    @GameTest(template="empty") public static void soulCaptureFillsOneBottle(GameTestHelper h){
        var p=serverPlayer(h,Race.SKELETON);p.getInventory().add(MonsterPerspective.SOUL_BOTTLE.toStack(2));var pig=net.minecraft.world.entity.EntityType.PIG.create(h.getLevel());
        new TechEvents().kill(new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(pig,p.damageSources().playerAttack(p)));
        h.assertTrue(p.getInventory().countItem(MonsterPerspective.FULL_SOUL.get())==1,"One soul filled");h.assertTrue(p.getInventory().countItem(MonsterPerspective.SOUL_BOTTLE.get())==1,"One empty bottle consumed");h.succeed();
    }
    @GameTest(template="empty") public static void boundPearlSwapsLoadedEntity(GameTestHelper h){
        var p=serverPlayer(h,Race.ENDERMAN);var pig=EntityType.PIG.create(h.getLevel());var origin=p.position();var target=origin.add(2,0,0);pig.setPos(target);h.getLevel().addFreshEntity(pig);
        var pearl=MonsterPerspective.BOUND.toStack();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,pearl);
        MonsterPerspective.BOUND.get().interactLivingEntity(pearl,p,pig,net.minecraft.world.InteractionHand.MAIN_HAND);
        MonsterPerspective.BOUND.get().use(h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(p.position().distanceTo(target)<0.1,"Player moves to target");h.assertTrue(pig.position().distanceTo(origin)<0.1,"Target moves to player");h.assertTrue(!pearl.isEmpty(),"Successful exchange keeps bound pearl");h.succeed();
    }
    @GameTest(template="empty") public static void sunRespectsHelmet(GameTestHelper h){
        var p=player(h,Race.ZOMBIE);var pos=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));p.setPos(pos.getX(),pos.getY(),pos.getZ());p.tickCount=20;h.getLevel().setDayTime(6000);h.getLevel().setWeatherParameters(100,0,false,false);
        new RaceEvents().tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));h.assertTrue(p.isOnFire(),"Direct sunlight burns unprotected zombie");
        p.clearFire();p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));new RaceEvents().tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));h.assertTrue(!p.isOnFire(),"Any head equipment prevents sunlight ignition");h.succeed();
    }
    @GameTest(template="empty") public static void nativeGolemPartGateUsesMaterial(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("modulargolems")){h.succeed();return;}
        var p=serverPlayer(h,Race.ZOMBIE);var menu=p.inventoryMenu;
        var part=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("modulargolems","humanoid_golem_body"));
        menu.getSlot(1).set(new ItemStack(part));menu.getSlot(2).set(MonsterPerspective.FLESH.toStack());menu.slotsChanged(p.getInventory());
        h.assertTrue(TechEvents.required(menu.getSlot(0).getItem())==Race.ZOMBIE,"Native part gets flesh material");
        p.getPersistentData().putString("mp_race","skeleton");menu.slotsChanged(p.getInventory());h.assertTrue(menu.getSlot(0).getItem().isEmpty(),"Native material conversion respects race gate");h.succeed();
    }
    private static Player modePlayer(GameTestHelper h,Race race,GameType mode){
        var p=h.makeMockPlayer(mode);mode.updatePlayerAbilities(p.getAbilities());
        p.getPersistentData().putString("mp_race",race.id());RaceEvents.applyAttributes(p);p.refreshDimensions();p.setHealth(p.getMaxHealth());
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));p.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);p.setNoGravity(true);
        return p;
    }
    private static void clearNoon(GameTestHelper h){
        h.getLevel().setDayTime(6000);h.getLevel().setWeatherParameters(1000,0,false,false);h.getLevel().setRainLevel(0);h.getLevel().setThunderLevel(0);h.getLevel().updateSkyBrightness();
        h.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION).set(false,h.getLevel().getServer());
        h.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_FIRE_DAMAGE).set(true,h.getLevel().getServer());
    }
    private static void tickPlayer(Player p,int ticks){
        // ServerLevel advances this counter before calling Entity.tick. The mock is not in its entity list.
        for(int i=0;i<ticks;i++){p.tickCount++;p.tick();}
    }
    @GameTest(template="empty") public static void sunlightBurnsThreeRacesThroughRealPlayerTicks(GameTestHelper h){
        clearNoon(h);
        for(Race race:List.of(Race.ZOMBIE,Race.SKELETON,Race.VAMPIRE)){
            Player p=modePlayer(h,race,GameType.SURVIVAL);float before=p.getHealth();
            h.assertTrue(RaceEvents.isExposedToSun(p),"Fixture must be in direct sunlight");
            tickPlayer(p,65);
            h.assertTrue(p.isOnFire(),race+" must stay on fire through normal Player.tick event dispatch");
            h.assertTrue(p.getHealth()<before,race+" must take real survival fire damage");
            h.assertTrue(before-p.getHealth()<=5,race+" fire damage must use vanilla once-per-second cadence");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void creativeSunFireHasNetworkVisualWithoutDamage(GameTestHelper h){
        clearNoon(h);
        for(Race race:List.of(Race.ZOMBIE,Race.SKELETON,Race.VAMPIRE)){
            Player p=modePlayer(h,race,GameType.CREATIVE);p.getAbilities().flying=true;
            tickPlayer(p,25);
            h.assertTrue(p.isOnFire(),race+" creative ignition must remain visible");
            var data=p.getEntityData().getNonDefaultValues();
            h.assertTrue(data!=null && data.stream().anyMatch(v->v.id()==0 && v.value() instanceof Byte flags && (flags&1)!=0),"Client shared fire flag must be present in synced entity data");
            h.assertTrue(p.getHealth()==p.getMaxHealth(),"Creative fire must retain damage immunity");
            h.assertTrue(p.getAbilities().flying,"Sunlight must not revoke flight");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void sunlightRespectsHeadAndNight(GameTestHelper h){
        clearNoon(h);
        for(Race race:List.of(Race.ZOMBIE,Race.SKELETON,Race.VAMPIRE)){
            Player p=modePlayer(h,race,GameType.SURVIVAL);p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));p.tick();
            h.assertTrue(!p.isOnFire(),"Head equipment prevents solar ignition for "+race);
            p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
            h.getLevel().setDayTime(18000);h.getLevel().updateSkyBrightness();p.tick();h.assertTrue(!p.isOnFire(),"Night must not ignite player");clearNoon(h);
        }
        Player other=modePlayer(h,Race.ENDERMAN,GameType.SURVIVAL);other.tick();h.assertTrue(!other.isOnFire(),"Enderman must not inherit solar weakness");
        Player spectator=modePlayer(h,Race.ZOMBIE,GameType.SPECTATOR);spectator.tick();h.assertTrue(!spectator.isOnFire(),"Spectator remains unaffected");
        h.succeed();
    }
    @GameTest(template="empty") public static void solidRoofPreventsSunlightAfterLightingUpdates(GameTestHelper h){
        clearNoon(h);h.setBlock(2,6,2,net.minecraft.world.level.block.Blocks.STONE);
        h.succeedWhen(()->{
            for(Race race:List.of(Race.ZOMBIE,Race.SKELETON,Race.VAMPIRE)){
                Player p=modePlayer(h,race,GameType.SURVIVAL);
                h.assertTrue(!h.getLevel().canSeeSky(net.minecraft.core.BlockPos.containing(p.getX(),p.getEyeY(),p.getZ())),"Roof fixture must finish skylight updates");
                p.tick();h.assertTrue(!p.isOnFire(),"Solid roof prevents solar ignition for "+race);
            }
        });
    }
    @GameTest(template="empty") public static void sunlightDoesNotIgniteInWaterOrRain(GameTestHelper h){
        clearNoon(h);waterPool(h);Player wet=modePlayer(h,Race.ZOMBIE,GameType.SURVIVAL);wet.baseTick();
        h.assertTrue(wet.isInWater(),"Water fixture must update actual fluid state");wet.tick();h.assertTrue(!wet.isOnFire(),"Water prevents solar ignition");
        Player rain=modePlayer(h,Race.VAMPIRE,GameType.SURVIVAL);rain.setPos(rain.getX()+8,rain.getY()+4,rain.getZ());
        h.getLevel().setWeatherParameters(0,1000,true,false);h.getLevel().setRainLevel(1);h.getLevel().updateSkyBrightness();rain.tick();
        h.assertTrue(!rain.isOnFire(),"Rain prevents solar ignition");clearNoon(h);h.succeed();
    }
    @GameTest(template="empty") public static void milkRequiresFullHealthAtStartForSkeletonAndVampire(GameTestHelper h){
        for(Race race:List.of(Race.SKELETON,Race.VAMPIRE))for(boolean full:List.of(false,true)){
            var p=serverPlayer(h,race);p.getInventory().clearContent();if(!full)p.setHealth(p.getMaxHealth()-2);
            var events=new RaceEvents();var milk=new ItemStack(Items.MILK_BUCKET);
            events.foodStart(new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start(p,milk,32));
            events.finish(new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish(p,milk,0,new ItemStack(Items.BUCKET)));
            h.assertTrue(p.getInventory().countItem(Items.BONE)==(full?1:0),"Only starting milk at full health yields bone: "+race+" full="+full);
        }
        h.succeed();
    }
    private static void waterPool(GameTestHelper h){
        for(int x=1;x<=3;x++)for(int z=1;z<=3;z++)for(int y=1;y<=4;y++)h.setBlock(x,y,z,net.minecraft.world.level.block.Blocks.WATER);
    }
    @GameTest(template="empty") public static void buoyancyAffectsSurvivalButPreservesCreativeTakeoff(GameTestHelper h){
        waterPool(h);
        for(Race race:List.of(Race.ZOMBIE,Race.SKELETON)){
            Player survival=modePlayer(h,race,GameType.SURVIVAL);survival.baseTick();h.assertTrue(survival.isInWater(),"Fixture must be submerged");
            survival.setDeltaMovement(0.1,0.25,0.15);new RaceEvents().tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(survival));
            h.assertTrue(race==Race.ZOMBIE?survival.getDeltaMovement().y<0:survival.getDeltaMovement().y>0,"Race buoyancy applies in survival");
            for(boolean flying:List.of(false,true)){
                Player creative=modePlayer(h,race,GameType.CREATIVE);creative.getAbilities().flying=flying;creative.baseTick();h.assertTrue(creative.isInWater(),"Creative fixture must be submerged");
                var velocity=new net.minecraft.world.phys.Vec3(0.1,0.25,0.15);creative.setDeltaMovement(velocity);new RaceEvents().tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(creative));
                h.assertTrue(creative.getDeltaMovement().equals(velocity),"Creative takeoff and flying velocity must not be overwritten");h.assertTrue(creative.getAbilities().flying==flying,"Flight state must not change");
            }
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void moddedFlightPermissionAlsoBypassesForcedSinking(GameTestHelper h){
        waterPool(h);Player p=modePlayer(h,Race.ZOMBIE,GameType.SURVIVAL);
        p.getAttribute(net.neoforged.neoforge.common.NeoForgeMod.CREATIVE_FLIGHT).setBaseValue(1);
        p.baseTick();h.assertTrue(p.isInWater()&&p.mayFly(),"Modded flight permission fixture");
        p.setDeltaMovement(0,0.3,0);new RaceEvents().tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
        h.assertTrue(p.getDeltaMovement().y==0.3,"Other mods' creative flight must be preserved");h.succeed();
    }
    @GameTest(template="empty") public static void uselessModDoesNotExtinguishOrdinaryPlayers(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("useless_mod")){h.succeed();return;}
        clearNoon(h);
        var p=serverPlayer(h,Race.ZOMBIE);p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);
        // Exercise an ordinary vanilla player, without a solar weakness refreshing the timer.
        p.getPersistentData().remove("mp_race");RaceEvents.applyAttributes(p);
        // A real ServerPlayer has a 60-tick spawn protection; expire it before measuring fire damage.
        for(int i=0;i<65;i++){p.tickCount++;p.tick();p.doTick();}
        p.getFoodData().setFoodLevel(10);p.getFoodData().setSaturation(0);p.igniteForSeconds(8);float before=p.getHealth();
        try{
            var update=Class.forName("com.sorrowmist.useless.event.EventHandler").getMethod("updateBeefInvulnerability",Player.class,boolean.class);
            update.invoke(null,p,false);
        }catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
        h.assertTrue(p.getRemainingFireTicks()>0,"Useless Mod must not extinguish a player without an immunity item");
        for(int i=0;i<25;i++){p.tickCount++;p.tick();p.doTick();}
        h.assertTrue(p.getRemainingFireTicks()>0,"Ordinary fire must survive real Useless Mod ticks; remaining="+p.getRemainingFireTicks()+", water="+p.isInWaterOrBubble());
        h.assertTrue(p.getHealth()<before,"Ordinary fire must deal damage; health="+p.getHealth()+", before="+before+", fireDamage="+p.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_FIRE_DAMAGE)+", immune="+p.isInvulnerableTo(p.damageSources().onFire())+", abilityInvulnerable="+p.getAbilities().invulnerable+", absorption="+p.getAbsorptionAmount());h.succeed();
    }
    @GameTest(template="empty") public static void uselessImmunityReleaseStillExtinguishesExactlyOnce(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("useless_mod")){h.succeed();return;}
        Player p=modePlayer(h,Race.ENDERMAN,GameType.SURVIVAL);
        try{
            Class<?> ownership=Class.forName("com.sorrowmist.useless.event.BeefInvulnerabilityOwnership");
            var get=ownership.getDeclaredMethod("getOrCreate",Player.class);get.setAccessible(true);Object tag=get.invoke(null,p);
            var migrated=ownership.getDeclaredMethod("markMigrationComplete",net.minecraft.nbt.CompoundTag.class);migrated.setAccessible(true);migrated.invoke(null,tag);
            var claim=ownership.getDeclaredMethod("claim",net.minecraft.nbt.CompoundTag.class,boolean.class);claim.setAccessible(true);claim.invoke(null,tag,false);
            p.setInvulnerable(true);p.igniteForSeconds(8);
            var update=Class.forName("com.sorrowmist.useless.event.EventHandler").getMethod("updateBeefInvulnerability",Player.class,boolean.class);
            update.invoke(null,p,false);
            h.assertTrue(p.getRemainingFireTicks()==0 && !p.isInvulnerable(),"Actually releasing owned immunity retains the original cleanup");
            p.igniteForSeconds(8);update.invoke(null,p,false);
            h.assertTrue(p.getRemainingFireTicks()>0,"Later ordinary fire must not be repeatedly extinguished");
        }catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
        h.succeed();
    }
}
