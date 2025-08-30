package com.xiaohunao.xhn_lib.api.register.register;

import com.mojang.serialization.MapCodec;
import com.xiaohunao.xhn_lib.api.register.holder.FlexibleHolder;
import com.xiaohunao.xhn_lib.common.codec.ICodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 专门用于注册MapCodec的灵活注册器 提供类型安全的MapCodec注册功能
 *
 * @param <T> 实现ICodec接口的类型
 */
public class MapCodecFlexibleRegister<T extends ICodec<T>> extends FlexibleRegister<MapCodec<? extends T>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapCodecFlexibleRegister.class);

    /**
     * 使用ResourceKey构造，不支持动态注册
     */
    protected MapCodecFlexibleRegister(ResourceKey<? extends Registry<MapCodec<? extends T>>> registry, String modId) {
        super(registry, modId);
    }

    /**
     * 使用Registry构造，不支持动态注册
     */
    protected MapCodecFlexibleRegister(Registry<MapCodec<? extends T>> registry, String modId) {
        super(registry, modId);
    }

    /**
     * 创建MapCodecFlexibleRegister的静态工厂方法
     */
    public static <T extends ICodec<T>> MapCodecFlexibleRegister<T> createMapCodec(ResourceKey<? extends Registry<MapCodec<? extends T>>> registry, String modId) {
        return new MapCodecFlexibleRegister<>(registry, modId);
    }

    /**
     * 创建MapCodecFlexibleRegister的静态工厂方法
     */
    public static <T extends ICodec<T>> MapCodecFlexibleRegister<T> createMapCodec(Registry<MapCodec<? extends T>> registry, String modId) {
        return new MapCodecFlexibleRegister<>(registry, modId);
    }

    /**
     * 注册单个MapCodec
     *
     * @param name 注册名称
     * @param codec 要注册的MapCodec
     * @return 当前实例，支持链式调用
     */
    public MapCodecFlexibleRegister<T> addMapCodec(String name, MapCodec<? extends T> codec) {
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(codec, "MapCodec cannot be null");

        try {
            registerStatic(name, () -> codec);
        } catch (Exception e) {
            LOGGER.error("Failed to register MapCodec: {} -> {}", name, codec.getClass().getSimpleName(), e);
        }

        return this;
    }

    /**
     * 批量注册MapCodec，使用名称和MapCodec的交替数组
     *
     * @param codecPairs 交替的名称和MapCodec数组
     * @return 当前实例，支持链式调用
     */
    public MapCodecFlexibleRegister<T> addMapCodec(Object... codecPairs) {
        if (codecPairs == null || codecPairs.length == 0) {
            throw new IllegalArgumentException("MapCodec pairs cannot be null or empty");
        }
        if (codecPairs.length % 2 != 0) {
            throw new IllegalArgumentException("MapCodec pairs must be in pairs (name, codec, name, codec, ...)");
        }

        for (int i = 0; i < codecPairs.length; i += 2) {
            Object nameObj = codecPairs[i];
            Object codecObj = codecPairs[i + 1];

            if (!(nameObj instanceof String)) {
                LOGGER.error("Invalid name type at index {}: expected String, got {}", i, nameObj.getClass().getSimpleName());
                continue;
            }

            if (!(codecObj instanceof MapCodec)) {
                LOGGER.error("Invalid MapCodec type at index {}: expected MapCodec, got {}", i + 1, codecObj.getClass().getSimpleName());
                continue;
            }

            @SuppressWarnings("unchecked")
            MapCodec<? extends T> codec = (MapCodec<? extends T>) codecObj;
            String name = (String) nameObj;

            addMapCodec(name, codec);
        }

        return this;
    }
}
