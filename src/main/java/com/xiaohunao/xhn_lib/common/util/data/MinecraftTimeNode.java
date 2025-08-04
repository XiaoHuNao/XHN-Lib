package com.xiaohunao.xhn_lib.common.util.data;

/**
 * Minecraft时间系统特殊节点枚举
 * 包含一天24小时内所有重要的时间节点
 */
public enum MinecraftTimeNode {

    // ========== 黎明时段 (00:00 - 06:00) ==========
    DAY_START(0, "06:00", "一天开始，村民醒来", TimeCategory.BASIC.getValue()),
    MOON_DISAPPEARS(167, "06:10", "月亮完全消失于地平线", TimeCategory.CELESTIAL.getValue()),
    SUNRISE_END(450, "06:27", "日出结束，地平线颜色稳定", TimeCategory.LIGHTING.getValue()),

    // ========== 上午时段 (06:00 - 12:00) ==========
    CMD_DAY(1000, "07:00", "/time set day命令时间", TimeCategory.COMMAND.getValue()),
    VILLAGER_WORK_START(2000, "08:00", "村民开始工作日程", TimeCategory.VILLAGER.getValue()),
    MAX_LIGHT_REACHED(4283, "10:17", "天空光照达到最大值15", TimeCategory.LIGHTING.getValue()),
    CLOCK_NOON(5723, "11:43", "时钟指向中午", TimeCategory.CLOCK.getValue()),

    // ========== 正午时段 (12:00) ==========
    NOON(6000, "12:00", "正午，太阳最高点，/time set noon",
            TimeCategory.combine(TimeCategory.BASIC, TimeCategory.COMMAND)),

    // ========== 下午时段 (12:00 - 18:00) ==========
    VILLAGER_SOCIAL_START(9000, "15:00", "村民结束工作，开始社交", TimeCategory.VILLAGER.getValue()),
    SUNSET_START(11617, "17:37", "日落开始，地平线昏暗", TimeCategory.LIGHTING.getValue()),
    MOON_APPEARS(11834, "17:50", "月亮出现在地平线", TimeCategory.CELESTIAL.getValue()),

    // ========== 黄昏时段 (18:00 - 19:00) ==========
    SUNSET(12000, "18:00", "黄昏，村民睡觉", TimeCategory.BASIC.getValue()),
    SLEEP_START_RAIN(12010, "18:00", "雨天可开始睡觉", TimeCategory.SLEEP.getValue()),
    LIGHT_START_DECREASE(12040, "18:02", "晴天光照开始降低", TimeCategory.LIGHTING.getValue()),
    SLEEP_START_CLEAR(12544, "18:32", "晴天可开始睡觉，亡灵不燃烧", TimeCategory.SLEEP.getValue()),
    FLOWER_CLOSE_START(12600, "18:36", "闭合眼眸花开始转变", TimeCategory.NATURE.getValue()),
    CLOCK_DUSK(12610, "18:36", "时钟指向黄昏", TimeCategory.CLOCK.getValue()),
    HOSTILE_SPAWN_RAIN(12769, "18:46", "雨天敌对生物开始生成", TimeCategory.HOSTILE.getValue()),

    // ========== 夜晚时段 (19:00 - 00:00) ==========
    CMD_NIGHT(13000, "19:00", "/time set night命令时间", TimeCategory.COMMAND.getValue()),
    HOSTILE_SPAWN_CLEAR(13188, "19:11", "晴天敌对生物开始生成", TimeCategory.HOSTILE.getValue()),
    MIN_LIGHT_REACHED(13670, "19:40", "天空光照达到最低值4", TimeCategory.LIGHTING.getValue()),
    SUN_DISAPPEARS(13702, "19:42", "太阳完全消失于地平线", TimeCategory.CELESTIAL.getValue()),
    SUNSET_END(13800, "19:48", "日落结束，地平线颜色稳定", TimeCategory.LIGHTING.getValue()),
    CLOCK_MIDNIGHT(17843, "23:50", "时钟指向午夜", TimeCategory.CLOCK.getValue()),

    // ========== 午夜时段 (00:00) ==========
    MIDNIGHT(18000, "00:00", "午夜，月亮正中，/time set midnight",
            TimeCategory.combine(TimeCategory.BASIC, TimeCategory.COMMAND)),

    // ========== 深夜时段 (00:00 - 06:00) ==========
    SUNRISE_START(22200, "04:12", "日出开始，地平线变亮", TimeCategory.LIGHTING.getValue()),
    SUN_APPEARS(22300, "04:18", "太阳出现在地平线", TimeCategory.CELESTIAL.getValue()),
    LIGHT_START_INCREASE(22331, "04:19", "天空光照开始增加", TimeCategory.LIGHTING.getValue()),
    HOSTILE_STOP_CLEAR(22812, "04:48", "晴天敌对生物停止生成", TimeCategory.HOSTILE.getValue()),
    CLOCK_DAWN(23031, "05:01", "时钟指向黎明，雨天敌对生物停止生成",
            TimeCategory.combine(TimeCategory.CLOCK, TimeCategory.HOSTILE)),
    FLOWER_OPEN_START(23400, "05:24", "张开眼眸花开始转变", TimeCategory.NATURE.getValue()),
    SLEEP_END_CLEAR(23460, "05:27", "晴天不能再睡觉，亡灵开始燃烧", TimeCategory.SLEEP.getValue()),
    MAX_LIGHT_RESTORED(23961, "05:57", "晴天光照重新达到最大值15", TimeCategory.LIGHTING.getValue()),
    SLEEP_END_RAIN(23992, "05:59", "雨天不能再睡觉", TimeCategory.SLEEP.getValue());

    private final int ticks;
    private final String gameTime;
    private final String description;
    private final int categories;

    MinecraftTimeNode(int ticks, String gameTime, String description, int categories) {
        this.ticks = ticks;
        this.gameTime = gameTime;
        this.description = description;
        this.categories = categories;
    }

    
    public int getTicks() { return ticks; }
    public String getGameTime() { return gameTime; }
    public String getDescription() { return description; }
    public int getCategories() { return categories; }
    public boolean hasCategory(TimeCategory category) {
        return (categories & category.getValue()) != 0;
    }


    public enum TimeCategory {
        BASIC(1),        // 基础节点（四大时间点）
        COMMAND(2),      // 指令相关节点
        LIGHTING(4),     // 光照系统节点
        HOSTILE(8),      // 敌对生物相关节点
        SLEEP(16),       // 睡眠机制节点
        VILLAGER(32),    // 村民行为节点
        CELESTIAL(64),   // 天体运动节点（太阳月亮）
        CLOCK(128),      // 时钟指向节点
        NATURE(256);     // 自然现象节点（花朵等）

        private final int value;

        TimeCategory(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        /**
         * 组合多个分类
         */
        public static int combine(TimeCategory... categories) {
            int result = 0;
            for (TimeCategory category : categories) {
                result |= category.value;
            }
            return result;
        }
    }
}
