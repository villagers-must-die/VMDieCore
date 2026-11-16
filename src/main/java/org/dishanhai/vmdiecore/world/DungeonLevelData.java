package org.dishanhai.vmdiecore.world;

import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;

/**
 * 时间独立于主世界：地牢始终保持创建时设的日落。
 */
final class DungeonLevelData extends DerivedLevelData {
    private long dayTime = DungeonLevelFactory.DAY_TIME;
    private float dayTimeFraction;
    private float dayTimePerTick;

    DungeonLevelData(WorldData worldData, ServerLevelData overworldData) {
        super(worldData, overworldData);
    }

    @Override
    public long getDayTime() {
        return dayTime;
    }

    @Override
    public void setDayTime(long time) {
        dayTime = time;
    }

    @Override
    public float getDayTimeFraction() {
        return dayTimeFraction;
    }

    @Override
    public void setDayTimeFraction(float fraction) {
        dayTimeFraction = fraction;
    }

    @Override
    public float getDayTimePerTick() {
        return dayTimePerTick;
    }

    @Override
    public void setDayTimePerTick(float perTick) {
        dayTimePerTick = perTick;
    }
}
