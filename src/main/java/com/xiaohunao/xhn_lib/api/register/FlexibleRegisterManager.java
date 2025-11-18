package com.xiaohunao.xhn_lib.api.register;


import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.spongepowered.include.com.google.common.collect.HashMultimap;

import com.xiaohunao.xhn_lib.api.data.loader.BaseDynamicLoader;
import com.xiaohunao.xhn_lib.api.register.register.FlexibleRegister;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public class FlexibleRegisterManager {
    public static final FlexibleRegisterManager INSTANCE = new FlexibleRegisterManager();

    private final Map<ResourceKey<? extends Registry<?>>, Map<String, FlexibleRegister<?>>> registryMap = new ConcurrentHashMap<>();
    private final HashMultimap<BaseDynamicLoader<?>,FlexibleRegister<?>> dynamicLoaders = HashMultimap.create();
    private final Map<MappedRegistry<?>,BaseDynamicLoader<?>> dynamicLoadersMap = new ConcurrentHashMap<>();


    private FlexibleRegisterManager() {
    }

    /**
     * 获取指定注册表的所有FlexibleRegister
     */
    public Collection<Map.Entry<String, FlexibleRegister<?>>> getFlexibleRegisters(ResourceKey<? extends Registry<?>> resourceKey) {
        Map<String, FlexibleRegister<?>> modRegisters = registryMap.get(resourceKey);
        return modRegisters != null ? modRegisters.entrySet() : Collections.emptyList();
    }

    /**
     * 添加一个FlexibleRegister到管理器
     */
    public void addRegister(FlexibleRegister<?> register) {
        ResourceKey<? extends Registry<?>> registryKey = register.getRegistryKey();
        String modId = register.getModId();

        registryMap.computeIfAbsent(registryKey, resourceKey -> new HashMap<>())
                .put(modId, register);

        if (register.supportsDynamicRegistration()) {
            BaseDynamicLoader<?> loader = register.getDynamicManager();
            if (loader != null) {
                dynamicLoaders.put(loader, register);
                dynamicLoadersMap.put(loader.getRegistry(), loader);
            }
        }
    }

    /**
     * 获取特定模组和注册表的FlexibleRegister
     */
    @SuppressWarnings("unchecked")
    public <T> FlexibleRegister<T> getFlexibleRegister(String modId, ResourceKey<? extends Registry<?>> registryKey) {
        Map<String, FlexibleRegister<?>> modRegisters = registryMap.get(registryKey);
        if (modRegisters == null) {
            return null;
        }
        return (FlexibleRegister<T>) modRegisters.get(modId);
    }

    /**
     * 检查是否包含指定注册表
     */
    public boolean containsRegistry(ResourceKey<? extends Registry<?>> registryKey) {
        return registryMap.containsKey(registryKey);
    }


    public Collection<FlexibleRegister<?>> getFlexibleRegister(BaseDynamicLoader<?> loader) {
        return dynamicLoaders.get(loader);
    }

    /**
     * 检查是否包含指定模组的指定注册表
     */
    public boolean containsModRegistry(String modId, ResourceKey<? extends Registry<?>> registryKey) {
        Map<String, FlexibleRegister<?>> modRegisters = registryMap.get(registryKey);
        return modRegisters != null && modRegisters.containsKey(modId);
    }

    public void registerAllDynamicLoaders() {
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> {
            // 注册所有动态加载器
            dynamicLoaders.keySet().forEach(event::addListener);
        });
    }

    /**
     * 处理所有静态已注册项
     * 这个方法应该在 FMLCommonSetupEvent 中被调用，确保所有注册项都能被处理
     */
    public void processAllRegisteredValues() {
        for (Map<String, FlexibleRegister<?>> modRegisters : registryMap.values()) {
            for (FlexibleRegister<?> register : modRegisters.values()) {
                processStaticRegistryValues(register);
            }
        }
    }


    /**
     * 处理只支持静态注册的注册项
     */
    @SuppressWarnings("unchecked")
    private void processStaticRegistryValues(FlexibleRegister<?> register) {
        try {
            // 获取注册表
            Registry<?> registry = BuiltInRegistries.REGISTRY.get(register.getRegistryKey().location());
            if (registry instanceof MappedRegistry<?> mappedRegistry) {
                BaseDynamicLoader<?> dynamicLoader;

                if (dynamicLoadersMap.containsKey(mappedRegistry)){
                    dynamicLoader = dynamicLoadersMap.get(mappedRegistry);
                }else {
                    dynamicLoader = new BaseDynamicLoader<>("temp", (Registry<Object>) mappedRegistry, null) {
                        @Override
                        protected void apply(java.util.@NotNull Map<net.minecraft.resources.ResourceLocation, com.google.gson.JsonElement> resources,
                                             net.minecraft.server.packs.resources.@NotNull ResourceManager resourceManager,
                                             net.minecraft.util.profiling.@NotNull ProfilerFiller profiler) {
                        }
                    };
                }

                // 调用 processAllRegisteredValues 处理所有已注册项
                processAllRegisteredValues(dynamicLoader, mappedRegistry);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(FlexibleRegisterManager.class)
                .error("Error processing static registry values for {}: {}", register.getRegistryKey(), e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void processAllRegisteredValues(BaseDynamicLoader<T> loader, MappedRegistry<?> registry) {
        loader.processAllRegisteredValues((MappedRegistry<T>) registry);
    }
}
