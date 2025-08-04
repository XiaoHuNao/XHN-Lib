package com.xiaohunao.xhn_lib.api.data.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.xiaohunao.xhn_lib.api.register.FlexibleRegisterManager;
import com.xiaohunao.xhn_lib.common.serialization.IDynamicSerializer;
import com.xiaohunao.xhn_lib.common.util.RegistryUtils;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 抽象的动态资源加载器，用于从JSON文件加载和注册资源
 * @param <T> 要加载的资源类型
 */
public class BaseDynamicLoader<T> extends SimpleJsonResourceReloadListener {
    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseDynamicLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    protected final Map<ResourceLocation, T> loadedValues = new HashMap<>();
    protected final Set<ResourceLocation> removedValues = new HashSet<>();

    protected final String folderName;
    protected final IDynamicSerializer<T> serializer;
    protected final MappedRegistry<T> registry;

    /**
     * 创建一个新的动态加载器
     *
     * @param folderName 资源文件夹名称
     * @param registry 目标注册表
     * @param serializer 用于反序列化的序列化器
     */
    public BaseDynamicLoader(String folderName, Registry<T> registry, IDynamicSerializer<T> serializer) {
        super(GSON, folderName);
        this.folderName = folderName;
        this.registry = (MappedRegistry<T>) registry;
        this.serializer = serializer;
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> resources, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        Set<ResourceLocation> previousValues = new HashSet<>(loadedValues.keySet());
        loadedValues.clear();
        removedValues.clear();

        RegistryUtils.safeRegistryOperation(registry, mappedRegistry -> {
            Set<ResourceLocation> currentResourceLocations = new HashSet<>(resources.keySet());
            loadNewValues(mappedRegistry,resources);
            removeObsoleteValues(mappedRegistry,previousValues, currentResourceLocations);

            FlexibleRegisterManager.INSTANCE.getFlexibleRegister(this).forEach(flexibleRegister -> flexibleRegister.setEntriesChanged(true));

            LOGGER.info("{} loading complete, currently has {} entries", mappedRegistry.key().location(), loadedValues.size());
        });
    }

    /**
     * 加载新的资源值
     *
     * @param resources 资源映射
     */
    protected void loadNewValues(MappedRegistry<T> mappedRegistry, Map<ResourceLocation, JsonElement> resources) {
        resources.forEach((resourceLocation, jsonElement) -> {
            try {
                T value = serializer.read(jsonElement);
                if (value != null) {
                    if (mappedRegistry.containsKey(resourceLocation)) {
                        LOGGER.warn("Resource {} already exists in registry {}, skipping", resourceLocation, mappedRegistry.key().location());
                        return;
                    }

                    RegistryUtils.register(mappedRegistry, resourceLocation, value);
                    loadedValues.put(resourceLocation, value);
                    onValueLoaded(resourceLocation, value);
                    LOGGER.debug("Successfully loaded {}: {}", mappedRegistry.key().location(), resourceLocation);
                }
            } catch (Exception e) {
                LOGGER.error("Error loading {}: {}", mappedRegistry.key().location(), resourceLocation, e);
            }
        });
    }

    /**
     * @param location 资源位置
     * @param value 加载的值
     */
    protected void onValueLoaded(ResourceLocation location, T value) {
    }

    /**
     * 移除不再存在的资源
     *
     * @param previousValues 之前加载的资源ID集合
     * @param currentResourceLocations 当前资源ID集合
     */
    protected void removeObsoleteValues(MappedRegistry<T> mappedRegistry, Set<ResourceLocation> previousValues, Set<ResourceLocation> currentResourceLocations) {
        previousValues.stream()
                .filter(id -> !currentResourceLocations.contains(id))
                .forEach(id -> {
                    removedValues.add(id);
                    if (RegistryUtils.unregisterFromRegistry(mappedRegistry, id)) {
                        onValueRemoved(id);
                        LOGGER.info("Removed no longer existing {}: {}", mappedRegistry.key().location(), id);
                    }
                });
    }

    /**
     * @param location 被移除的资源位置
     */
    protected void onValueRemoved(ResourceLocation location) {
    }

    /**
     * 获取此加载器使用的序列化器
     *
     * @return 序列化器实例
     */
    public IDynamicSerializer<T> getSerializer() {
        return serializer;
    }
}
