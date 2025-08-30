package com.xiaohunao.xhn_lib.api.data.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.xiaohunao.xhn_lib.common.event.FlexibleRegisterEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.xiaohunao.xhn_lib.api.register.FlexibleRegisterManager;
import com.xiaohunao.xhn_lib.api.register.PostRegisterAction;
import com.xiaohunao.xhn_lib.api.register.PostRegisterResult;
import com.xiaohunao.xhn_lib.common.serialization.IDynamicSerializer;
import com.xiaohunao.xhn_lib.common.util.RegistryUtils;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 抽象的动态资源加载器，用于从JSON文件加载和注册资源
 *
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

        RegistryUtils.safeRegistryOperation(registry, mappedRegistry -> {
            Set<ResourceLocation> currentResourceLocations = new HashSet<>(resources.keySet());

            removeObsoleteValues(mappedRegistry, previousValues, currentResourceLocations);
            loadNewValues(mappedRegistry, resources);
            processAllRegisteredValues(mappedRegistry);
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
     * 注册后钩子 - 处理所有已注册项（动态+静态）
     *
     * @param mappedRegistry 注册表
     */
    public void processAllRegisteredValues(MappedRegistry<T> mappedRegistry) {
        List<PostRegisterAction<T>> actionsToProcess = new ArrayList<>();
        for (Map.Entry<ResourceKey<T>, T> entry : mappedRegistry.entrySet()) {
            ResourceLocation location = entry.getKey().location();
            T value = entry.getValue();

            PostRegisterResult<T> result = onBeforeRegister(location, value);
            PostRegisterResult<T> eventResult = triggerBeforeRegisterEvent(location, value, result);
            PostRegisterResult<T> finalResult = eventResult != null ? eventResult : result;

            if (finalResult.getAction() != PostRegisterResult.Action.KEEP) {
                actionsToProcess.add(new PostRegisterAction<>(location, value, finalResult));
            }
        }

        for (PostRegisterAction<T> action : actionsToProcess) {
            ResourceLocation location = action.location();
            PostRegisterResult<T> result = action.result();

            switch (result.getAction()) {
                case MODIFY:
                    // 修改值
                    T modifiedValue = result.getValue();
                    if (modifiedValue != null) {
                        RegistryUtils.unregisterFromRegistry(mappedRegistry, location);
                        RegistryUtils.register(mappedRegistry, location, modifiedValue);
                        if (loadedValues.containsKey(location)) {
                            loadedValues.put(location, modifiedValue);
                        }
                        LOGGER.debug("Modified resource {}: {}", location, modifiedValue);
                    }
                    break;
                case REMOVE:
                    // 删除注册项
                    if (RegistryUtils.unregisterFromRegistry(mappedRegistry, location)) {
                        loadedValues.remove(location);
                        removedValues.add(location);
                        onValueRemoved(location);
                        LOGGER.info("Removed resource {} by post-register hook", location);
                    }
                    break;
                default:
                    break;
            }
        }

        mappedRegistry.entrySet().forEach(entry -> {
            ResourceLocation resLoc = entry.getKey().location();
            T resValue = entry.getValue();
            onAfterRegister(resLoc, resValue);
            FlexibleRegisterEvent.After<T> event = new FlexibleRegisterEvent.After<>(resLoc, resValue, mappedRegistry);
            NeoForge.EVENT_BUS.post(event);
        });
    }

    /**
     * 触发注册前事件，允许外部再次修改资源
     *
     * @param location 资源位置
     * @param value 原始值
     * @param originalResult 原始的处理结果
     * @return 事件修改后的结果，如果没有修改则返回null
     */
    protected PostRegisterResult<T> triggerBeforeRegisterEvent(ResourceLocation location, T value, PostRegisterResult<T> originalResult) {
        FlexibleRegisterEvent.Before<T> event = new FlexibleRegisterEvent.Before<>(location, value, originalResult, registry);
        NeoForge.EVENT_BUS.post(event);

        if (event.isModified()) {
            return event.getResult();
        }
        return null;
    }

    /**
     * 注册前钩子 - 允许在注册前最终修改一次资源,这个方法一般只会触发一次FMLCommonSetupEvent 返回null将取消注册该资源
     *
     * @param location 资源位置
     * @param value 原始值
     * @return 修改后的值，或null表示取消注册
     */
    protected PostRegisterResult<T> onBeforeRegister(ResourceLocation location, T value) {
        return PostRegisterResult.keep(); // 默认实现：保持原样
    }

    /**
     * 注册前钩子 - 允许在注册完成后,这个方法一般只会触发一次FMLCommonSetupEvent
     *
     * @param location 资源位置
     * @param value 注册值
     */
    protected void onAfterRegister(ResourceLocation location, T value) {
        // 默认实现：不做任何操作
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
