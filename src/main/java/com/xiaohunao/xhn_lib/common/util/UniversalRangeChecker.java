package com.xiaohunao.xhn_lib.common.util;

import java.util.Optional;

/**
 * 通用范围检查器 - 支持明确的循环边界范围
 */
public class UniversalRangeChecker {
    protected final Optional<Long> min;
    protected final Optional<Long> max;
    protected final Optional<Long> cycleMin; // 循环范围的最小边界
    protected final Optional<Long> cycleMax; // 循环范围的最大边界
    protected final boolean defaultResult;
    
    public UniversalRangeChecker(Optional<Long> min, Optional<Long> max, 
                               Optional<Long> cycleMin, Optional<Long> cycleMax, 
                               boolean defaultResult) {
        this.min = min;
        this.max = max;
        this.cycleMin = cycleMin;
        this.cycleMax = cycleMax;
        this.defaultResult = defaultResult;
        
        // 验证循环边界的有效性
        if (cycleMin.isPresent() != cycleMax.isPresent()) {
            throw new IllegalArgumentException("Both cycleMin and cycleMax must be present or both absent");
        }
        
        if (cycleMin.isPresent() && cycleMax.isPresent() && cycleMin.get() >= cycleMax.get()) {
            throw new IllegalArgumentException("cycleMin must be less than cycleMax");
        }
    }
    
    /**
     * 检查值是否在范围内
     * @param value 要检查的值
     * @return 是否在范围内
     */
    public boolean matches(long value) {
        Long minVal = min.orElse(null);
        Long maxVal = max.orElse(null);
        
        // 如果都为空，返回默认值
        if (minVal == null && maxVal == null) {
            return defaultResult;
        }
        
        // 如果使用循环边界，标准化数值
        if (isCyclic()) {
            long cycleMinVal = cycleMin.get();
            long cycleMaxVal = cycleMax.get();
            
            value = normalizeValue(value, cycleMinVal, cycleMaxVal);
            if (minVal != null) minVal = normalizeValue(minVal, cycleMinVal, cycleMaxVal);
            if (maxVal != null) maxVal = normalizeValue(maxVal, cycleMinVal, cycleMaxVal);
        }
        
        if (minVal != null && maxVal != null) {
            if (minVal.equals(maxVal)) {
                // 特殊情况：min == max，只有这一个值匹配
                return value == minVal;
            } else if (isCyclic() && minVal > maxVal) {
                // 循环范围（跨边界）
                return value >= minVal || value <= maxVal;
            } else {
                // 线性范围或正常循环范围
                return value >= minVal && value <= maxVal;
            }
        }
        
        // 只有单边界的情况
        if (minVal != null) {
            return value >= minVal;
        }
        
        if (maxVal != null) {
            return value <= maxVal;
        }
        
        return defaultResult;
    }
    
    /**
     * 将值标准化到循环范围内
     * @param value 原始值
     * @param cycleMin 循环范围最小值
     * @param cycleMax 循环范围最大值
     * @return 标准化后的值
     */
    public long normalizeValue(long value, long cycleMin, long cycleMax) {
        long rangeSize = cycleMax - cycleMin + 1;
        
        if (value >= cycleMin && value <= cycleMax) {
            return value; // 已在范围内
        }
        
        // 计算相对于cycleMin的偏移
        long offset = value - cycleMin;
        long normalizedOffset = offset % rangeSize;
        
        // 处理负数情况
        if (normalizedOffset < 0) {
            normalizedOffset += rangeSize;
        }
        
        return cycleMin + normalizedOffset;
    }
    
    /**
     * 检查是否为循环范围
     */
    public boolean isCyclic() {
        return cycleMin.isPresent() && cycleMax.isPresent();
    }
    
    /**
     * 获取循环边界
     */
    public Optional<Long> getCycleMin() {
        return cycleMin;
    }
    
    public Optional<Long> getCycleMax() {
        return cycleMax;
    }
    
    /**
     * 获取范围描述
     */
    public String getDescription() {
        Long minVal = min.orElse(null);
        Long maxVal = max.orElse(null);
        String rangeStr;
        
        if (minVal == null && maxVal == null) {
            rangeStr = defaultResult ? "unlimited" : "none";
        } else if (minVal != null && maxVal != null) {
            rangeStr = String.format("[%d, %d]", minVal, maxVal);
        } else if (minVal != null) {
            rangeStr = String.format("[%d, +∞)", minVal);
        } else {
            rangeStr = String.format("(-∞, %d]", maxVal);
        }
        
        if (isCyclic()) {
            rangeStr += String.format(" (cyclic in [%d, %d])", cycleMin.get(), cycleMax.get());
        } else {
            rangeStr += " (linear)";
        }
        
        return rangeStr;
    }
}