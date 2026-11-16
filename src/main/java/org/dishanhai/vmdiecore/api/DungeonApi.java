package org.dishanhai.vmdiecore.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import org.dishanhai.vmdiecore.VMDieCore;
import org.dishanhai.vmdiecore.api.event.DungeonEnterEvent;
import org.dishanhai.vmdiecore.api.event.DungeonLeaveEvent;
import org.dishanhai.vmdiecore.data.DungeonDefinitions;
import org.dishanhai.vmdiecore.dungeon.DungeonInstance;
import org.dishanhai.vmdiecore.dungeon.DungeonPool;
import org.dishanhai.vmdiecore.dungeon.VisitData;
import org.dishanhai.vmdiecore.world.DungeonLevel;
import org.dishanhai.vmdiecore.world.OriginWorld;

import java.util.List;
import java.util.OptionalInt;

/**
 * 地牢对外接口。界面、命令、其他模组都调这里。
 */
public final class DungeonApi {

    /** 按种类与等级创建地牢，定义缺失时返回空。 */
    public static OptionalInt create(ResourceLocation theme, int level) {
        DungeonTheme definition = DungeonDefinitions.theme(theme).orElse(null);
        if (definition == null) {
            VMDieCore.LOGGER.warn("创建失败：种类 {} 未定义", theme);
            return OptionalInt.empty();
        }
        TierDefinition tier = definition.tier(level).orElse(null);
        if (tier == null) {
            VMDieCore.LOGGER.warn("创建失败：{} 没有等级 {}", theme, level);
            return OptionalInt.empty();
        }
        return DungeonPool.create(definition, tier);
    }

    /** 删除地牢并把玩家送回起源大厅。 */
    public static boolean destroy(int id) {
        return DungeonPool.destroy(id);
    }

    /** 校验规则与撤离点后把玩家传送到落脚点。 */
    public static boolean enter(ServerPlayer player, int id) {
        DungeonInstance info = DungeonPool.info(id).orElse(null);
        if (info == null) {
            VMDieCore.LOGGER.warn("进入失败：地牢 #{} 不存在", id);
            return false;
        }
        DungeonTheme definition = DungeonDefinitions.theme(info.theme()).orElse(null);
        TierDefinition tier = definition == null ? null : definition.tier(info.level()).orElse(null);
        if (tier == null) {
            VMDieCore.LOGGER.warn("进入失败：地牢 #{} 的定义已不存在", id);
            return false;
        }
        DungeonRule rule = DungeonDefinitions.rule(tier.ruleId()).orElse(null);
        if (rule != null && rule.requireExitPoint() && !tier.hasExit()) {
            VMDieCore.LOGGER.warn("进入失败：地牢 #{} 没有撤离点", id);
            return false;
        }
        DungeonLevel level = DungeonPool.level(id);
        if (level == null || player.server.getLevel(level.dimension()) != level) {
            VMDieCore.LOGGER.warn("进入失败：地牢 #{} 维度不在维度表", id);
            return false;
        }
        if (level.players().stream().noneMatch(other -> other != player)) {
            DungeonProfile profile = DungeonDefinitions.profile(tier.profileId()).orElse(null);
            if (profile != null && profile.fixedTime() >= 0L) {
                level.setDayTime(profile.fixedTime());
            }
        }
        DungeonEnterEvent event = new DungeonEnterEvent(player, id);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            VMDieCore.LOGGER.info("进入地牢 #{} 被取消", id);
            return false;
        }
        BlockPos landing = level.landing();
        level.getChunk(landing.getX() >> 4, landing.getZ() >> 4);
        player.teleportTo(level, landing.getX() + 0.5D, landing.getY(), landing.getZ() + 0.5D, 0.0F, 0.0F);
        VisitData.increment(player, id);
        return true;
    }

    /** 把玩家送回起源大厅，玩家不在任何地牢里返回 false。 */
    public static boolean leave(ServerPlayer player) {
        OptionalInt current = current(player);
        if (current.isEmpty()) {
            return false;
        }
        if (!OriginWorld.sendHome(player)) {
            return false;
        }
        NeoForge.EVENT_BUS.post(new DungeonLeaveEvent(player, current.getAsInt()));
        return true;
    }

    /** 按玩家当前维度反查地牢 id。 */
    public static OptionalInt current(ServerPlayer player) {
        return DungeonPool.idOf(player.level());
    }

    /** 列出所有地牢，不带玩家视角的进入次数。 */
    public static List<DungeonInfo> list() {
        return DungeonPool.list().stream().map(info -> toInfo(info, 0)).toList();
    }

    /** 列出所有地牢，附带该玩家的进入次数。 */
    public static List<DungeonInfo> listFor(ServerPlayer player) {
        return DungeonPool.list().stream()
                .map(info -> toInfo(info, VisitData.visits(player, info.id())))
                .toList();
    }

    private static DungeonInfo toInfo(DungeonInstance info, int visits) {
        return new DungeonInfo(info.id(), info.theme(), info.themeName(), info.level(), info.tierName(),
                info.hasExit(), visits);
    }

    private DungeonApi() {
    }
}
