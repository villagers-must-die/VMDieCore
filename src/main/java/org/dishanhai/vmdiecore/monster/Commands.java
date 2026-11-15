package org.dishanhai.vmdiecore.monster;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import static net.minecraft.commands.Commands.*;

public final class Commands {
    @SubscribeEvent public void register(RegisterCommandsEvent e){
        e.getDispatcher().register(literal("monster").executes(c->{var p=c.getSource().getPlayerOrException();PacketDistributor.sendToPlayer(p,Network.packet(p,true));return 1;})
            .then(literal("status").executes(c->{
                var p=c.getSource().getPlayerOrException();var eyes=net.minecraft.core.BlockPos.containing(p.getX(),p.getEyeY(),p.getZ());var race=Race.of(p);
                String status="种族="+(race==null?"未选择":race.title)+"；模式="+p.gameMode.getGameModeForPlayer().getName()
                    +"；白天="+p.level().isDay()+"；头顶露天="+p.level().canSeeSky(eyes)
                    +"；头部空置="+p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty()
                    +"；日照暴露="+RaceEvents.isExposedToSun(p)+"；着火时长="+p.getRemainingFireTicks()
                    +"；飞行许可="+p.mayFly()+"；正在飞行="+p.getAbilities().flying;
                c.getSource().sendSuccess(()->Component.literal(status),false);return 1;
            }))
            .then(literal("race").requires(s->s.hasPermission(2)).then(argument("player",EntityArgument.player())
                .then(argument("race",StringArgumentType.word()).suggests((c,b)->{for(Race r:Race.values())b.suggest(r.id());b.suggest("reset");return b.buildFuture();})
                    .executes(c->{var p=EntityArgument.getPlayer(c,"player");String id=StringArgumentType.getString(c,"race");Race r=Race.parse(id);
                        if("reset".equals(id)){p.getPersistentData().remove("mp_race");p.getPersistentData().remove("mp_change_after");RaceEvents.applyAttributes(p);p.setHealth(Math.min(p.getHealth(),p.getMaxHealth()));p.refreshDimensions();Network.sync(p);PacketDistributor.sendToPlayer(p,Network.packet(p,true));return 1;}
                        if(r==null){c.getSource().sendFailure(Component.literal("未知种族"));return 0;}return RaceEvents.choose(p,r,true)?1:0;
                    })))));
    }
}
