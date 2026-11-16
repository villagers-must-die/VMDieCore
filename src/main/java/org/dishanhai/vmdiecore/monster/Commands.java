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
                var status=Component.translatable("monsterperspective.command.status",
                    race==null?Component.translatable("monsterperspective.command.unselected"):Component.translatable(race.title),
                    p.gameMode.getGameModeForPlayer().getName(),p.level().isDay(),p.level().canSeeSky(eyes),
                    p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty(),RaceEvents.isExposedToSun(p),
                    p.getRemainingFireTicks(),p.mayFly(),p.getAbilities().flying);
                c.getSource().sendSuccess(()->status,false);return 1;
            }))
            .then(literal("race").requires(s->s.hasPermission(2)).then(argument("player",EntityArgument.player())
                .then(argument("race",StringArgumentType.word()).suggests((c,b)->{for(Race r:Race.values())b.suggest(r.id());b.suggest("reset");return b.buildFuture();})
                    .executes(c->{var p=EntityArgument.getPlayer(c,"player");String id=StringArgumentType.getString(c,"race");Race r=Race.parse(id);
                        if("reset".equals(id)){p.getPersistentData().remove("mp_race");p.getPersistentData().remove("mp_change_after");RaceEvents.applyAttributes(p);p.setHealth(Math.min(p.getHealth(),p.getMaxHealth()));p.refreshDimensions();Network.sync(p);PacketDistributor.sendToPlayer(p,Network.packet(p,true));return 1;}
                        if(r==null){c.getSource().sendFailure(Component.translatable("monsterperspective.command.unknown_race"));return 0;}return RaceEvents.choose(p,r,true)?1:0;
                    })))));
    }
}
