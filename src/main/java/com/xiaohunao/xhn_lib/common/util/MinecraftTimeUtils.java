package com.xiaohunao.xhn_lib.common.util;


import com.xiaohunao.xhn_lib.common.network.bidir.TimeSyncPayload;
import com.xiaohunao.xhn_lib.common.util.data.MinecraftTimeNode;
import com.xiaohunao.xhn_lib.common.util.data.MoonPhase;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 时间工具类，提供便捷的时间操作方法
 */
public class MinecraftTimeUtils {

    public static final long MC_DAY_TICKS = 24000L;
    public static final long MOON_CYCLE_TICKS = MC_DAY_TICKS * 8L;

    public static final UniversalRangeChecker MC_TIME_CHECKER = new UniversalRangeChecker(
            Optional.empty(),     // 不限制范围
            Optional.empty(),     // 不限制范围
            Optional.of(0L),      // 循环最小值
            Optional.of(MC_DAY_TICKS - 1),  // 循环最大值 (23999)
            true                  // 默认匹配所有时间
    );


    /**
     * 标准化时间到0-23999范围内
     */
    public static long normalizeTime(long ticks) {
        return MC_TIME_CHECKER.normalizeValue(ticks, 0L, MC_DAY_TICKS - 1);
    }

    /**
     * 获取指定时间的月相数字 (0-7)
     */
    public static int getMoonPhaseNumber(long worldTime) {
        return (int)(worldTime / 24000L % 8L + 8L) % 8;
    }


    /**
     * 获取指定时间的月相
     */
    public static MoonPhase getMoonPhase(long worldTime) {
        int phase = (int) ((worldTime / MC_DAY_TICKS) % 8L);
        return MoonPhase.fromPhase(phase);
    }

    /**
     * 计算到下一个满月的时间
     */
    public static long getNextFullMoonTime(long currentWorldTime) {
        int currentMoonPhase = getMoonPhaseNumber(currentWorldTime);
        long cyclesToNextFullMoon = (8 - currentMoonPhase) % 8;

        // 如果已经是满月，则计算下一个满月
        if (cyclesToNextFullMoon == 0) {
            cyclesToNextFullMoon = 8;
        }

        // 计算基准时间（当前天数的开始）
        long currentDayStart = (currentWorldTime / MC_DAY_TICKS) * MC_DAY_TICKS;

        // 计算下一个满月的时间（日落结束时）
        return currentDayStart + cyclesToNextFullMoon * MC_DAY_TICKS + MinecraftTimeNode.SUNSET_END.getTicks();
    }

    /**
     * 计算到指定月相的时间
     */
    public static long getNextMoonPhaseTime(long currentWorldTime, MoonPhase targetMoonPhase) {
        int currentMoonPhase = getMoonPhaseNumber(currentWorldTime);
        int targetPhase = targetMoonPhase.getPhase();
        long cyclesToTarget = (targetPhase - currentMoonPhase + 8) % 8;

        // 如果已经是目标月相，则计算下一个周期的该月相
        if (cyclesToTarget == 0) {
            cyclesToTarget = 8;
        }

        long currentDayStart = (currentWorldTime / MC_DAY_TICKS) * MC_DAY_TICKS;

        // 统一使用日落结束时间作为月相观测时间
        return currentDayStart + cyclesToTarget * MC_DAY_TICKS + MinecraftTimeNode.SUNSET_END.getTicks();
    }

    /**
     * 跳转到下一个满月
     */
    public static void jumpToNextFullMoon(Level level) {
        long currentTime = level.getDayTime();
        long nextFullMoonTime = getNextFullMoonTime(currentTime);
        setTime(level, nextFullMoonTime);
    }

    /**
     * 跳转到指定月相
     */
    public static void jumpToNextMoonPhase(Level level, MoonPhase moonPhase) {
        long currentTime = level.getDayTime();
        long nextMoonPhaseTime = getNextMoonPhaseTime(currentTime, moonPhase);
        setTime(level, nextMoonPhaseTime);
    }

    /**
     * 获取距离下一个满月的天数
     */
    public static long getDaysUntilNextFullMoon(long currentWorldTime) {
        long nextFullMoon = getNextFullMoonTime(currentWorldTime);
        return (nextFullMoon - currentWorldTime) / MC_DAY_TICKS;
    }

    /**
     * 检查当前是否为指定的月相
     */
    public static boolean isMoonPhaseActive(Level level, MoonPhase... activeMoonPhases) {
        MoonPhase currentPhase = getMoonPhase(level.getDayTime());
        for (MoonPhase phase : activeMoonPhases) {
            if (currentPhase == phase) {
                return true;
            }
        }
        return false;
    }



    /**
     * 计算两个时间点之间的tick差值（考虑跨天情况）
     */
    public static long getTimeDifference(long startTicks, long endTicks) {
        long start = normalizeTime(startTicks);
        long end = normalizeTime(endTicks);

        if (end >= start) {
            return end - start;
        } else {
            return MC_DAY_TICKS - start + end;
        }
    }

    /**
     * 检查当前时间是否在指定范围内（支持跨天）
     */
    public static boolean isTimeInRange(long currentTicks, long startTicks, long endTicks) {
        UniversalRangeChecker rangeChecker = new UniversalRangeChecker(
                Optional.of(startTicks),
                Optional.of(endTicks),
                Optional.of(0L),
                Optional.of(MC_DAY_TICKS - 1),
                false
        );
        return rangeChecker.matches(currentTicks);
    }


    /**
     * 是否为白天 (06:00 - 18:00)
     */
    public static boolean isDaytime(long ticks) {
        return isTimeInRange(ticks,
                MinecraftTimeNode.DAY_START.getTicks(),
                MinecraftTimeNode.SUNSET.getTicks());
    }

    /**
     * 是否为夜晚 (18:00 - 06:00)
     */
    public static boolean isNighttime(long ticks) {
        return !isDaytime(ticks);
    }

    /**
     * 是否为黎明 (00:00 - 06:27)
     */
    public static boolean isDawn(long ticks) {
        return isTimeInRange(ticks,
                MinecraftTimeNode.DAY_START.getTicks(),
                MinecraftTimeNode.SUNRISE_END.getTicks());
    }

    /**
     * 是否为上午 (06:27 - 12:00)
     */
    public static boolean isMorning(long ticks) {
        return isTimeInRange(ticks,
                MinecraftTimeNode.SUNRISE_END.getTicks(),
                MinecraftTimeNode.NOON.getTicks());
    }

    /**
     * 是否为正午附近 (11:30 - 12:30)
     */
    public static boolean isNoon(long ticks) {
        return isTimeInRange(ticks,
                MinecraftTimeNode.NOON.getTicks() - 500,
                MinecraftTimeNode.NOON.getTicks() + 500);
    }

    /**
     * 是否为下午 (12:00 - 18:00)
     */
    public static boolean isAfternoon(long ticks) {
        return isTimeInRange(ticks,
                MinecraftTimeNode.NOON.getTicks(),
                MinecraftTimeNode.SUNSET.getTicks());
    }

    /**
     * 是否为黄昏 (18:00 - 19:00)
     */
    public static boolean isDusk(long ticks) {
        return isTimeInRange(ticks,
                MinecraftTimeNode.SUNSET.getTicks(),
                MinecraftTimeNode.CMD_NIGHT.getTicks());
    }

    /**
     * 是否为夜晚 (19:00 - 00:00)
     */
    public static boolean isEvening(long ticks) {
        return isTimeInRange(ticks,
                MinecraftTimeNode.CMD_NIGHT.getTicks(),
                MinecraftTimeNode.MIDNIGHT.getTicks());
    }

    /**
     * 是否为深夜 (00:00 - 06:00)
     */
    public static boolean isDeepNight(long ticks) {
        return isTimeInRange(ticks,
                MinecraftTimeNode.MIDNIGHT.getTicks(),
                MC_DAY_TICKS + MinecraftTimeNode.DAY_START.getTicks());
    }

    /**
     * 是否可以睡觉（晴天）
     */
    public static boolean canSleepClearWeather(long ticks) {
        return isTimeInRange(ticks,
                MinecraftTimeNode.SLEEP_START_CLEAR.getTicks(),
                MinecraftTimeNode.SLEEP_END_CLEAR.getTicks());
    }

    /**
     * 是否可以睡觉（雨天）
     */
    public static boolean canSleepRainWeather(long ticks) {
        return isTimeInRange(ticks,
                MinecraftTimeNode.SLEEP_START_RAIN.getTicks(),
                MinecraftTimeNode.SLEEP_END_RAIN.getTicks());
    }

    /**
     * 敌对生物是否会生成（晴天）
     */
    public static boolean isHostileSpawnTimeClear(long ticks) {
        return isTimeInRange(ticks,
                MinecraftTimeNode.HOSTILE_SPAWN_CLEAR.getTicks(),
                MinecraftTimeNode.HOSTILE_STOP_CLEAR.getTicks());
    }

    /**
     * 敌对生物是否会生成（雨天）
     */
    public static boolean isHostileSpawnTimeRain(long ticks) {
        return isTimeInRange(ticks,
                MinecraftTimeNode.HOSTILE_SPAWN_RAIN.getTicks(),
                MinecraftTimeNode.CLOCK_DAWN.getTicks());
    }

    /**
     * 亡灵生物是否会燃烧
     */
    public static boolean isUndeadBurnTime(long ticks) {
        return isTimeInRange(ticks,
                MinecraftTimeNode.SLEEP_END_CLEAR.getTicks(),
                MinecraftTimeNode.SLEEP_START_CLEAR.getTicks());
    }

    /**
     * 村民是否在工作时间
     */
    public static boolean isVillagerWorkTime(long ticks) {
        return isTimeInRange(ticks,
                MinecraftTimeNode.VILLAGER_WORK_START.getTicks(),
                MinecraftTimeNode.VILLAGER_SOCIAL_START.getTicks());
    }

    /**
     * 村民是否在社交时间
     */
    public static boolean isVillagerSocialTime(long ticks) {
        return isTimeInRange(ticks,
                MinecraftTimeNode.VILLAGER_SOCIAL_START.getTicks(),
                MinecraftTimeNode.SUNSET.getTicks());
    }

    /**
     * 获取当前时间最接近的时间节点
     */
    public static MinecraftTimeNode getNearestTimeNode(long ticks) {
        long normalizedTicks = normalizeTime(ticks);
        MinecraftTimeNode nearest = null;
        long minDistance = Long.MAX_VALUE;

        for (MinecraftTimeNode node : MinecraftTimeNode.values()) {
            long distance = Math.min(
                    Math.abs(normalizedTicks - node.getTicks()),
                    MC_DAY_TICKS - Math.abs(normalizedTicks - node.getTicks())
            );

            if (distance < minDistance) {
                minDistance = distance;
                nearest = node;
            }
        }

        return nearest;
    }

    /**
     * 获取下一个指定类型的时间节点
     */
    public static MinecraftTimeNode getNextNodeByCategory(long currentTicks, MinecraftTimeNode.TimeCategory category) {
        long normalizedTicks = normalizeTime(currentTicks);
        MinecraftTimeNode nextNode = null;
        long minDistance = Long.MAX_VALUE;

        for (MinecraftTimeNode node : MinecraftTimeNode.values()) {
            if (node.hasCategory(category)) {
                long distance;
                if (node.getTicks() > normalizedTicks) {
                    distance = node.getTicks() - normalizedTicks;
                } else {
                    distance = MC_DAY_TICKS - normalizedTicks + node.getTicks();
                }

                if (distance < minDistance) {
                    minDistance = distance;
                    nextNode = node;
                }
            }
        }

        return nextNode;
    }

    /**
     * 获取指定类型的所有时间节点
     */
    public static List<MinecraftTimeNode> getNodesByCategory(MinecraftTimeNode.TimeCategory category) {
        return Arrays.stream(MinecraftTimeNode.values())
                .filter(node -> node.hasCategory(category))
                .sorted(Comparator.comparing(MinecraftTimeNode::getTicks))
                .collect(Collectors.toList());
    }

    /**
     * 获取当前时间段内的所有时间节点
     */
    public static List<MinecraftTimeNode> getNodesInTimeRange(long startTicks, long endTicks) {
        return Arrays.stream(MinecraftTimeNode.values())
                .filter(node -> isTimeInRange(node.getTicks(), startTicks, endTicks))
                .sorted(Comparator.comparing(MinecraftTimeNode::getTicks))
                .collect(Collectors.toList());
    }


    /**
     * 跳转到下一个指定的时间节点
     */
    public static long getNextTimeNodeTime(long currentWorldTime, MinecraftTimeNode timeNode) {
        long timeOfDay = normalizeTime(currentWorldTime);
        long currentDayStart = (currentWorldTime / MC_DAY_TICKS) * MC_DAY_TICKS;
        long targetTicks = timeNode.getTicks();

        if (timeOfDay < targetTicks) {
            // 如果当前时间在今天的目标时间之前
            return currentDayStart + targetTicks;
        } else {
            // 如果当前时间已过今天的目标时间，跳到明天的目标时间
            return currentDayStart + MC_DAY_TICKS + targetTicks;
        }
    }

    /**
     * 跳转到下一个指定的时间节点
     */
    public static void jumpToNextTimeNode(Level level, MinecraftTimeNode timeNode) {
        long currentTime = level.getDayTime();
        long nextTime = getNextTimeNodeTime(currentTime, timeNode);
        setTime(level, nextTime);
    }

    /**
     * 计算到达指定时间节点还需要多少ticks
     */
    public static long getTicksUntilNode(long currentTicks, MinecraftTimeNode targetNode) {
        long current = normalizeTime(currentTicks);
        long target = targetNode.getTicks();

        if (target > current) {
            return target - current;
        } else {
            return MC_DAY_TICKS - current + target;
        }
    }

    /**
     * 计算从指定时间节点过去了多少ticks
     */
    public static long getTicksSinceNode(long currentTicks, MinecraftTimeNode fromNode) {
        long current = normalizeTime(currentTicks);
        long from = fromNode.getTicks();

        if (current >= from) {
            return current - from;
        } else {
            return current + MC_DAY_TICKS - from;
        }
    }

    /**
     * 安全地添加时间（确保结果为正数）
     */
    public static long safeAddTicks(long currentTicks, long ticksToAdd) {
        if (ticksToAdd < 0) {
            // 如果要减少时间，使用减法方法
            return safeSubtractTicks(currentTicks, -ticksToAdd);
        }
        return normalizeTime(Math.max(0, currentTicks + ticksToAdd));
    }

    /**
     * 安全地减少时间（确保结果为正数）
     */
    public static long safeSubtractTicks(long currentTicks, long ticksToSubtract) {
        if (ticksToSubtract < 0) {
            // 如果要增加时间，使用加法方法
            return safeAddTicks(currentTicks, -ticksToSubtract);
        }

        long result = currentTicks - ticksToSubtract;
        // 如果结果为负数，通过循环处理
        while (result < 0) {
            result += MC_DAY_TICKS;
        }
        return normalizeTime(result);
    }

    /**
     * 广播时间给世界中的所有玩家
     */
    public static void broadcastTimeToAllPlayers(ServerLevel level, long time) {
        TimeSyncPayload payload = new TimeSyncPayload(time);
        PacketDistributor.sendToAllPlayers(payload);
    }

    /**
     * 添加指定的ticks到当前时间
     */
    public static long addTicks(Level level, long currentTicks, long ticksToAdd) {
        long newTime = safeAddTicks(currentTicks, ticksToAdd);

        if (level instanceof ServerLevel serverLevel) {
            safeSetTime(serverLevel, newTime);
            broadcastTimeToAllPlayers(serverLevel, newTime);
        } else if (level instanceof ClientLevel) {
            TimeSyncPayload payload = new TimeSyncPayload(newTime);
            PacketDistributor.sendToServer(payload);
        }

        return newTime;
    }

    /**
     * 从当前时间减去指定的ticks
     */
    public static long subtractTicks(Level level, long currentTicks, long ticksToSubtract) {
        long newTime = safeSubtractTicks(currentTicks, ticksToSubtract);

        if (level instanceof ServerLevel serverLevel) {
            safeSetTime(serverLevel, newTime);
            broadcastTimeToAllPlayers(serverLevel, newTime);
        } else if (level instanceof ClientLevel) {
            TimeSyncPayload payload = new TimeSyncPayload(newTime);
            PacketDistributor.sendToServer(payload);
        }

        return newTime;
    }

    /**
     * 设置绝对时间（支持客户端和服务端）
     */
    public static void setTime(Level level, long time) {
        if (level instanceof ServerLevel serverLevel) {
            safeSetTime(serverLevel, time);
            broadcastTimeToAllPlayers(serverLevel, time);
        } else if (level instanceof ClientLevel clientLevel) {
            clientLevel.setDayTime(time);
            TimeSyncPayload payload = new TimeSyncPayload(time);
            PacketDistributor.sendToServer(payload);
        }
    }

    /**
     *  检查serverLevelData安全的设置时间
     */
    public static void safeSetTime(ServerLevel serverLevel, long time) {
        ServerLevelData serverLevelData = serverLevel.serverLevelData;
        if (serverLevelData instanceof DerivedLevelData){
            serverLevel.getServer().overworld().setDayTime(time);
        } else {
            serverLevelData.setDayTime(time);
        }
    }




}
