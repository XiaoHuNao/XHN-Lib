package com.xiaohunao.xhn_lib.api.register.register;

import com.google.gson.JsonElement;
import com.xiaohunao.xhn_lib.api.data.loader.BaseDynamicLoader;
import com.xiaohunao.xhn_lib.api.register.FlexibleRegisterManager;
import com.xiaohunao.xhn_lib.api.register.holder.FlexibleHolder;
import com.xiaohunao.xhn_lib.common.serialization.IDynamicSerializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.registries.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 灵活的注册工具，支持静态注册和动态注册
 * <p>
 * 静态注册：在游戏启动时通过代码注册，类似于普通的内容注册
 * 动态注册：在游戏运行时通过资源包或代码动态注册，可以在不重启游戏的情况下更新内容
 * </p>
 *
 * @param <T> 要注册的内容类型
 */
public class FlexibleRegister<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlexibleRegister.class);

    // 存储动态注册的条目，键为名称，值为Holder
    private final Map<String, FlexibleHolder<T, ? extends T>> dynamicEntries = Collections.synchronizedMap(new HashMap<>());
    // 存储静态注册的条目，键为名称，值为Holder
    private final Map<String, FlexibleHolder<T, ? extends T>> staticEntries = Collections.synchronizedMap(new HashMap<>());

    // 模组ID
    private final String modId;
    // 注册表键
    protected final ResourceKey<? extends Registry<T>> registryKey;
    // 用于动态内容加载的管理器
    protected final BaseDynamicLoader<T> dynamicManager;
    // 是否支持动态注册
    private final boolean supportsDynamic;
    // 缓存所有条目
    private Collection<FlexibleHolder<T, ? extends T>> cachedAllEntries = null;


    @Nullable
    private Registry<T> customRegistry;
    @Nullable
    private FlexibleRegisterHolder<T> registryHolder;

    // 状态跟踪
    private boolean registeredEventBus = false;
    private boolean seenRegisterEvent = false;
    private boolean seenNewRegistryEvent = false;
    private boolean entriesChanged = true;


    public static <T> FlexibleRegister<T> create(Registry<T> registry, String modId, @Nullable BaseDynamicLoader<T> dynamicManager){
        return create(registry.key(),modId,dynamicManager);
    }

    public static <T> FlexibleRegister<T> create(Registry<T> registry, String modId){
        return create(registry.key(), modId, null);
    }

    public static <T> FlexibleRegister<T> create(ResourceKey<? extends Registry<T>> registry, String modId){
        return new FlexibleRegister<>(registry,modId,null);
    }

    public static <T> FlexibleRegister<T> create(ResourceKey<? extends Registry<T>> registry, String modId, @Nullable BaseDynamicLoader<T> dynamicManager){
        return new FlexibleRegister<>(registry,modId,dynamicManager);
    }

    protected FlexibleRegister(Registry<T> registry, String modId, @Nullable BaseDynamicLoader<T> dynamicManager ) {
        this(registry.key(), modId, dynamicManager);
    }

    protected FlexibleRegister(ResourceKey<? extends Registry<T>> registry, String modId, @Nullable BaseDynamicLoader<T> dynamicManager ) {
        this.registryKey = Objects.requireNonNull(registry);
        this.modId = Objects.requireNonNull(modId);
        this.dynamicManager = dynamicManager;
        this.supportsDynamic = dynamicManager != null;

        FlexibleRegisterManager.INSTANCE.addRegister(this);
    }

    protected FlexibleRegister(ResourceKey<? extends Registry<T>> registry, String modId) {
        this(registry, modId, null);
    }

    protected FlexibleRegister(Registry<T> registry, String modId) {
        this(registry, modId, null);
    }



    public void register(IEventBus eventBus) {
        if (this.registeredEventBus)
            throw new IllegalStateException("Cannot register FlexibleRegister to more than one event bus.");
        this.registeredEventBus = true;

        eventBus.addListener(this::addEntries);
        eventBus.addListener(this::addRegistry);
    }


    private void addRegistry(NewRegistryEvent event) {
        this.seenNewRegistryEvent = true;
        if (this.customRegistry != null) {
            event.register(this.customRegistry);
        }
    }

    /**
     * 处理RegisterEvent事件，注册静态条目
     */
    private void addEntries(RegisterEvent event) {
        if (!event.getRegistryKey().equals(this.registryKey)) {
            return;
        }
        
        LOGGER.debug("Processing RegisterEvent for {} entries from mod {}", this.registryKey, this.modId);
        this.seenRegisterEvent = true;
        

        for (Map.Entry<String, FlexibleHolder<T, ? extends T>> entry : staticEntries.entrySet()) {
            FlexibleHolder<T, ? extends T> holder = entry.getValue();
            ResourceLocation entryId = holder.getKey().location();
            
            event.register(this.registryKey, entryId, holder::get);
            LOGGER.debug("Registered static entry: {}", entryId);
        }
    }

    /**
     * 处理AddReloadListenerEvent事件，注册动态加载器
     */
    public void onAddReloadListener(AddReloadListenerEvent event) {
        if (this.dynamicManager != null && !event.getListeners().contains(this.dynamicManager)) {
            event.addListener(this.dynamicManager);
            LOGGER.debug("Added dynamic loader for registry: {}", this.registryKey);
        }
    }

    public FlexibleRegister<T> makeSimpleRegistry() {
        makeRegistry(this.registryKey.location(), RegistryBuilder::create);
        return this;
    }

    public Registry<T> makeRegistry() {
        return makeRegistry(this.registryKey.location(), RegistryBuilder::create);
    }

    public Registry<T> makeRegistry(final Consumer<RegistryBuilder<T>> consumer) {
        return makeRegistry(this.registryKey.location(), consumer);
    }

    private Registry<T> makeRegistry(final ResourceLocation registryName, final Consumer<RegistryBuilder<T>> consumer) {
        if (registryName == null)
            throw new IllegalStateException("Cannot create a registry without specifying a registry name");
        if (BuiltInRegistries.REGISTRY.containsKey(registryName) || this.customRegistry != null)
            throw new IllegalStateException("Cannot create a registry that already exists - " + this.registryKey);
        if (this.seenNewRegistryEvent)
            throw new IllegalStateException("Cannot create a registry after NewRegistryEvent was fired");

        RegistryBuilder<T> registryBuilder = new RegistryBuilder<>(this.registryKey);
        consumer.accept(registryBuilder);
        this.customRegistry = registryBuilder.create();
        this.registryHolder = new FlexibleRegisterHolder<>(this.registryKey);
        this.registryHolder.registry = this.customRegistry;
        return this.customRegistry;
    }

    public Supplier<Registry<T>> getSupplierRegistry() {
        if (this.registryHolder == null)
            this.registryHolder = new FlexibleRegisterHolder<>(this.registryKey);

        return this.registryHolder;
    }

    public Registry<T> getRegistry() {
        Registry<T> registry = (Registry<T>) BuiltInRegistries.REGISTRY.get(registryKey.location());
        if (registry == null) {
            throw new IllegalArgumentException("No registry with key " + registryKey);
        }
        return registry;
    }

    /**
     * 静态注册一个条目
     *
     * @param name 条目名称
     * @param supplier 条目供应器
     * @return 注册的DeferredHolder
     */
    public <I extends T> FlexibleHolder<T, I> registerStatic(String name, Supplier<I> supplier) {
        if (seenRegisterEvent) {
            throw new IllegalStateException("Cannot register new entries to FlexibleRegister after RegisterEvent has been fired.");
        }

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(modId, name);
        FlexibleHolder<T, I> flexibleHolder = FlexibleHolder.createStatic(this.registryKey, id, supplier);
        staticEntries.put(name, flexibleHolder);

        entriesChanged = true;

        return flexibleHolder;
    }

    public <I extends T> FlexibleHolder<T, I> registerDynamic(String name) {
        if (!supportsDynamic) {
            throw new IllegalStateException("This registry is not configured to support dynamic registration: " + this.registryKey);
        }

        if (dynamicManager == null) {
            throw new IllegalStateException("Attempted dynamic registration without a dynamic loader: " + this.registryKey);
        }

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(modId, name);

        FlexibleHolder<T, I> holder = FlexibleHolder.createDynamic(this.registryKey, id);
        dynamicEntries.put(name, holder);

        entriesChanged = true;
        
        LOGGER.info("Prepared dynamic content placeholder: {}", id);
        
        return holder;
    }


    /**
     * 获取类型化的动态内容加载管理器
     */
    public BaseDynamicLoader<T> getDynamicManager() {
        return dynamicManager;
    }

    public T tryDecodeDynamicValue(JsonElement jsonElement) {
        try {
            IDynamicSerializer<T> serializer = getDynamicManager().getSerializer();
            return serializer.read(jsonElement);
        } catch (Exception e) {
            LOGGER.error("Failed to decode dynamic value: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取所有动态注册的条目
     */
    public Collection<FlexibleHolder<T, ? extends T>> getDynamicEntries() {
        return Collections.unmodifiableCollection(dynamicEntries.values());
    }

    /**
     * 根据名称获取动态注册的条目
     */
    public Optional<FlexibleHolder<T, ? extends T>> getDynamicEntry(String name) {
        return Optional.ofNullable(dynamicEntries.get(name));
    }

    /**
     * 根据谓词过滤动态条目
     */
    public Collection<FlexibleHolder<T, ? extends T>> filterDynamicEntries(Predicate<FlexibleHolder<T, ? extends T>> predicate) {
        return dynamicEntries.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有静态注册的条目
     */
    public Collection<FlexibleHolder<T, ? extends T>> getStaticEntries() {
        return Collections.unmodifiableCollection(staticEntries.values());
    }

    /**
     * 根据名称获取静态注册的条目
     */
    public Optional<FlexibleHolder<T, ? extends T>> getStaticEntry(String name) {
        return Optional.ofNullable(staticEntries.get(name));
    }

    /**
     * 获取所有注册的条目（静态+动态）
     */
    public Collection<FlexibleHolder<T, ? extends T>> getAllEntries() {
        if (entriesChanged || cachedAllEntries == null) {
            List<FlexibleHolder<T, ? extends T>> allEntries = new ArrayList<>(staticEntries.size() + dynamicEntries.size());
            allEntries.addAll(staticEntries.values());
            allEntries.addAll(dynamicEntries.values());
            cachedAllEntries = Collections.unmodifiableCollection(allEntries);
            entriesChanged = false;
        }
        return cachedAllEntries;
    }

    /**
     * 检查是否支持动态注册
     */
    public boolean supportsDynamicRegistration() {
        return supportsDynamic;
    }

    /**
     * 检查指定名称的条目是否为动态注册
     */
    public boolean isDynamicallyRegistered(String name) {
        return dynamicEntries.containsKey(name);
    }

    /**
     * 获取模组ID
     */
    public String getModId() {
        return modId;
    }

    /**
     * 获取注册表键
     */
    public ResourceKey<? extends Registry<T>> getRegistryKey() {
        return registryKey;
    }

    public void cleanup() {
        dynamicEntries.clear();
        staticEntries.clear();
    }

    public boolean isEntriesChanged() {
        return entriesChanged;
    }

    public void setEntriesChanged(boolean entriesChanged) {
        this.entriesChanged = entriesChanged;
    }

    private static class FlexibleRegisterHolder<V> implements Supplier<Registry<V>> {
        private final ResourceKey<? extends Registry<V>> registryKey;
        private Registry<V> registry = null;

        private FlexibleRegisterHolder(ResourceKey<? extends Registry<V>> registryKey) {
            this.registryKey = registryKey;
        }

        @SuppressWarnings("unchecked")
        @Override
        public @Nullable Registry<V> get() {
            if (this.registry == null)
                this.registry = (Registry<V>) BuiltInRegistries.REGISTRY.get(this.registryKey.location());

            return this.registry;
        }
    }
}
