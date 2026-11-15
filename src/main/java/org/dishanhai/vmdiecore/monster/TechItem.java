package org.dishanhai.vmdiecore.monster;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class TechItem extends Item {
    public final String kind;
    public TechItem(Properties p,String kind){super(p);this.kind=kind;}
    public static CompoundTag data(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static void data(ItemStack s,CompoundTag n){s.set(DataComponents.CUSTOM_DATA,CustomData.of(n));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player user,InteractionHand hand){
        ItemStack s=user.getItemInHand(hand);
        if(!Race.hasTech(user,Race.ENDERMAN)){RaceEvents.message(user,"需要传送技术：末影人或血族。");return InteractionResultHolder.fail(s);}
        if(!(user instanceof ServerPlayer p))return InteractionResultHolder.success(s);
        if(p.getCooldowns().isOnCooldown(this))return InteractionResultHolder.fail(s);
        CompoundTag n=data(s);
        if(kind.equals("scroll")){
            if(!n.contains("dimension")){
                n.putString("dimension",p.level().dimension().location().toString());n.putDouble("x",p.getX());n.putDouble("y",p.getY());n.putDouble("z",p.getZ());n.putFloat("yaw",p.getYRot());n.putFloat("pitch",p.getXRot());data(s,n);
                RaceEvents.message(p,"归途已铭刻，再次使用返回。");
            }else{
                ResourceLocation id=ResourceLocation.tryParse(n.getString("dimension"));
                ServerLevel target=id==null?null:p.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,id));
                if(target==null){RaceEvents.message(p,"标记所在维度不可用。");return InteractionResultHolder.fail(s);}
                Vec3 pos=new Vec3(n.getDouble("x"),n.getDouble("y"),n.getDouble("z"));
                target.getChunkAt(net.minecraft.core.BlockPos.containing(pos));
                if(!safe(target,p,pos)){RaceEvents.message(p,"归途已被阻挡，请清理标记位置。");return InteractionResultHolder.fail(s);}
                p.teleportTo(target,pos.x,pos.y,pos.z,n.getFloat("yaw"),n.getFloat("pitch"));s.shrink(1);sound(p);
            }
        }else if(kind.equals("binding")){
            if(!n.hasUUID("target")){RaceEvents.message(p,"先右键一个生物来绑定。");return InteractionResultHolder.fail(s);}
            LivingEntity target=null;
            // getEntity(UUID) only inspects loaded entities, and never force-loads an untrusted target chunk.
            for(ServerLevel world:p.getServer().getAllLevels())if(world.getEntity(n.getUUID("target")) instanceof LivingEntity t){target=t;break;}
            if(target==null || !target.isAlive() || target.isRemoved()){
                s.shrink(1);RaceEvents.message(p,"灵魂失联，绑定珍珠已破碎。");return InteractionResultHolder.consume(s);
            }
            ServerLevel a=p.serverLevel(),b=(ServerLevel)target.level();Vec3 from=p.position(),to=target.position();
            if(target.isPassenger() || target.isVehicle() || p.isPassenger() || !safe(b,p,to,target) || !safe(a,target,from,p)){
                RaceEvents.message(p,"目标或落点无法安全交换。");return InteractionResultHolder.fail(s);
            }
            float yaw=p.getYRot(),pitch=p.getXRot();
            // Move the non-player first; a denied dimension transfer must not move the player.
            if(a!=b){
                Entity moved=target.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(a,from,target.getDeltaMovement(),target.getYRot(),target.getXRot(),net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
                if(moved==null){RaceEvents.message(p,"目标拒绝跨维度传送。");return InteractionResultHolder.fail(s);}
            }else target.teleportTo(from.x,from.y,from.z);
            p.teleportTo(b,to.x,to.y,to.z,yaw,pitch);sound(p);
        }else{
            ThrownEnderpearl pearl=new ThrownEnderpearl(level,p);pearl.setItem(new ItemStack(Items.ENDER_PEARL));
            pearl.shootFromRotation(p,p.getXRot(),p.getYRot(),0,1.5F,1);level.addFreshEntity(pearl);
            level.playSound(null,p.blockPosition(),SoundEvents.ENDER_PEARL_THROW,SoundSource.PLAYERS,0.5F,1);
        }
        p.getCooldowns().addCooldown(this,20);return InteractionResultHolder.consume(s);
    }
    @Override public InteractionResult interactLivingEntity(ItemStack s,Player p,LivingEntity target,InteractionHand hand){
        if(!kind.equals("binding"))return InteractionResult.PASS;
        if(!Race.hasTech(p,Race.ENDERMAN))return InteractionResult.FAIL;
        if(target==p)return InteractionResult.FAIL;
        if(!p.level().isClientSide){CompoundTag n=data(s);n.putUUID("target",target.getUUID());n.putString("target_name",target.getName().getString());data(s,n);RaceEvents.message(p,"已绑定："+target.getName().getString());}
        return InteractionResult.sidedSuccess(p.level().isClientSide);
    }
    public static boolean safe(ServerLevel l,LivingEntity e,Vec3 pos){
        return safe(l,e,pos,e);
    }
    public static boolean safe(ServerLevel l,LivingEntity e,Vec3 pos,Entity occupant){
        var box=e.getDimensions(e.getPose()).makeBoundingBox(pos).deflate(0.01);
        return pos.y>=l.getMinBuildHeight() && pos.y+e.getBbHeight()<l.getMaxBuildHeight() && l.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(pos))
            && l.noBlockCollision(e,box) && l.getEntityCollisions(occupant,box).isEmpty();
    }
    private static void sound(Player p){p.level().playSound(null,p.blockPosition(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1,1);}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag flags){
        lines.add(Component.literal("传送技术 · 末影人 / 血族").withColor(0xBD9AEF));CompoundTag n=data(s);
        if(kind.equals("scroll"))lines.add(Component.literal(n.contains("dimension")?"已标记："+n.getString("dimension")+" ["+(int)n.getDouble("x")+", "+(int)n.getDouble("y")+", "+(int)n.getDouble("z")+"]":"首次使用标记，第二次返回并消耗").withColor(0xAAB4C8));
        if(kind.equals("binding"))lines.add(Component.literal(n.hasUUID("target")?"绑定："+n.getString("target_name"):"右键生物绑定；空中右键交换").withColor(0xAAB4C8));
        if(kind.equals("infinite"))lines.add(Component.literal("可重复投掷 · 冷却 1 秒").withColor(0xAAB4C8));
    }
}
