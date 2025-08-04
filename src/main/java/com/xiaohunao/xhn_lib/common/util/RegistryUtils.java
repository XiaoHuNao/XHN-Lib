package com.xiaohunao.xhn_lib.common.util;

import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 提供注册表操作的工具类，包括动态注册和注销功能
 */
public class RegistryUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryUtils.class);

    /**
     * 从注册表中注销一个条目
     *
     * @param registry 目标注册表
     * @param id 要注销的资源ID
     * @param <T> 注册表条目类型
     * @return 是否成功注销
     */
    public static <T> boolean unregisterFromRegistry(MappedRegistry<T> registry, ResourceLocation id) {
        try {
            T value = registry.get(id);
            if (value == null) {
                LOGGER.warn("Attempting to unregister non-existent entry {}: {}", registry.key().location(), id);
                return false;
            }

            ResourceKey<T> resourceKey = ResourceKey.create(registry.key(), id);

            boolean success = true;
            success &= removeFromMap(registry, "byKey", resourceKey);
            success &= removeFromMap(registry, "byLocation", id);
            success &= removeFromMap(registry, "byValue", value);
            success &= removeFromMap(registry, "toId", value);
            success &= removeFromMap(registry, "registrationInfos", resourceKey);
            success &= removeFromList(registry, "byId", resourceKey);

            return success;
        } catch (Exception e) {
            LOGGER.error("Error removing entry from registry {}: {}", registry.key().location(), id, e);
            return false;
        }
    }

    /**
     * 从注册表的指定映射字段中移除一个键值对
     */
    private static <T> boolean removeFromMap(MappedRegistry<T> registry, String fieldName, Object key) {
        try {
            Field field = getAccessibleField(MappedRegistry.class, fieldName);
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) field.get(registry);
            Object removed = map.remove(key);

            if (removed != null) {
                LOGGER.debug("Successfully removed {} from {}: {}", registry.key().location(), fieldName, key);
                return true;
            } else {
                LOGGER.debug("Failed to remove {} from {}: {}", registry.key().location(), fieldName, key);
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Error removing {} from {}: {}", registry.key().location(), fieldName, key, e);
            return false;
        }
    }

    /**
     * 从注册表的指定列表字段中移除一个条目
     */
    private static <T> boolean removeFromList(MappedRegistry<T> registry, String fieldName, ResourceKey<T> resourceKey) {
        try {
            Field field = getAccessibleField(MappedRegistry.class, fieldName);
            Object listObj = field.get(registry);

            if (!(listObj instanceof List)) {
                LOGGER.warn("{} field is not a List, cannot remove {} reference: {}", fieldName, registry.key().location(), resourceKey);
                return false;
            }

            List<?> list = (List<?>) listObj;

            boolean removed = list.removeIf(item -> {
                if (item instanceof Holder.Reference) {
                    Holder.Reference<?> ref = (Holder.Reference<?>) item;
                    try {
                        return ref.key().equals(resourceKey);
                    } catch (Exception e) {
                        return false;
                    }
                }
                return false;
            });

            if (removed) {
                LOGGER.debug("Successfully removed {} reference from {} list: {}", registry.key().location(), fieldName, resourceKey);
                return true;
            }
            LOGGER.debug("Could not find {} reference in {}: {}", registry.key().location(), fieldName, resourceKey);
            return false;
        } catch (Exception e) {
            LOGGER.error("Error removing {} from {} list: {}", registry.key().location(), fieldName, resourceKey, e);
            return false;
        }
    }

    /**
     * 获取可访问的类字段
     */
    private static Field getAccessibleField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    /**
     * 安全地解冻注册表，执行操作后再冻结
     *
     * @param registry 要操作的注册表
     * @param action 要执行的操作
     * @param <T> 注册表条目类型
     * @return 操作是否成功
     */
    public static <T> boolean safeRegistryOperation(MappedRegistry<T> registry, Consumer<MappedRegistry<T>> action) {
        registry.unfreeze();
        try {
            action.accept(registry);
            return true;
        } catch (Exception e) {
            LOGGER.error("Error performing registry operation on {}", registry.key().location(), e);
            return false;
        } finally {
            registry.freeze();
        }
    }

    /**
     * 向注册表注册一个条目
     *
     * @param registry 目标注册表
     * @param id 资源ID
     * @param value 要注册的值
     * @param <T> 注册表条目类型
     * @return 注册的值
     */
    public static <T> T register(Registry<T> registry, ResourceLocation id, T value) {
        return Registry.register(registry, id, value);
    }
}