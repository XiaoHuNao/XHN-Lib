package com.xiaohunao.xhn_lib.common.util.data;

public enum MoonPhase {
    FULL_MOON(0, "满月", "Full Moon", 1.0),
    WANING_GIBBOUS(1, "亏凸月", "Waning Gibbous", 0.75),
    LAST_QUARTER(2, "下弦月", "Last Quarter", 0.5),
    WANING_CRESCENT(3, "残月", "Waning Crescent", 0.25),
    NEW_MOON(4, "新月", "New Moon", 0.0),
    WAXING_CRESCENT(5, "峨眉月", "Waxing Crescent", 0.25),
    FIRST_QUARTER(6, "上弦月", "First Quarter", 0.5),
    WAXING_GIBBOUS(7, "盈凸月", "Waxing Gibbous", 0.75);

    private final int phase;
    private final String chineseName;
    private final String englishName;
    private final double intensity;

    MoonPhase(int phase, String chineseName, String englishName, double intensity) {
        this.phase = phase;
        this.chineseName = chineseName;
        this.englishName = englishName;
        this.intensity = intensity;
    }

    public int getPhase() {
        return phase;
    }

    public String getChineseName() {
        return chineseName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public double getIntensity() {
        return intensity;
    }

    /**
     * 根据数字获取月相
     */
    public static MoonPhase fromPhase(int phase) {
        for (MoonPhase moonPhase : values()) {
            if (moonPhase.phase == phase) {
                return moonPhase;
            }
        }
        return FULL_MOON; // 默认返回满月
    }

    /**
     * 检查是否为满月
     */
    public boolean isFullMoon() {
        return this == FULL_MOON;
    }

    /**
     * 检查是否为新月
     */
    public boolean isNewMoon() {
        return this == NEW_MOON;
    }

    /**
     * 检查是否为上弦月或下弦月
     */
    public boolean isQuarterMoon() {
        return this == FIRST_QUARTER || this == LAST_QUARTER;
    }

    /**
     * 检查是否为盈月（月亮在变大）
     */
    public boolean isWaxing() {
        return phase >= 5 && phase <= 7;
    }

    /**
     * 检查是否为亏月（月亮在变小）
     */
    public boolean isWaning() {
        return phase >= 1 && phase <= 3;
    }
}