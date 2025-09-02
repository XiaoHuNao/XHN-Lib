package com.xiaohunao.xhn_lib.common.event;

import com.xiaohunao.xhn_lib.api.register.PostRegisterResult;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class FlexibleRegisterEvent<T> extends Event  {
    private final ResourceLocation location;
    private final T value;
    private final MappedRegistry<T> registry;

    public FlexibleRegisterEvent(ResourceLocation location, T value, MappedRegistry<T> registry) {
        this.location = location;
        this.value = value;
        this.registry = registry;
    }

    /**
     * 获取资源位置
     */
    public ResourceLocation getLocation() {
        return location;
    }

    /**
     * 获取注册的值
     */
    public T getValue() {
        return value;
    }

    /**
     * 获取注册表
     */
    public MappedRegistry<T> getRegistry() {
        return registry;
    }

    /**
     * 注册后事件，在资源注册完成后触发
     *
     * @param <T> 资源类型
     */
    public static class After<T> extends FlexibleRegisterEvent<T> {
        public After(ResourceLocation location, T value, MappedRegistry<T> registry) {
            super(location,value,registry);
        }
    }

    /**
     * 注册前事件，允许外部在注册前再次修改资源
     *
     * @param <T> 资源类型
     */
    public static class Before<T> extends FlexibleRegisterEvent<T> implements ICancellableEvent {
        private boolean modified = false;
        private final PostRegisterResult<T> originalResult;
        private PostRegisterResult<T> result;

        public Before(ResourceLocation location, T value, PostRegisterResult<T> originalResult, MappedRegistry<T> registry) {
            super(location,value,registry);
            this.originalResult = originalResult;
            this.result = originalResult;
        }

        /**
         * 获取原始处理结果
         */
        public PostRegisterResult<T> getOriginalResult() {
            return originalResult;
        }

        /**
         * 设置修改后的结果
         */
        public void setResult(PostRegisterResult<T> result) {
            this.result = result;
            this.modified = true;
        }

        /**
         * 获取当前结果
         */
        public PostRegisterResult<T> getResult() {
            return result;
        }

        /**
         * 检查是否被修改过
         */
        public boolean isModified() {
            return modified;
        }

        /**
         * 保持原样
         */
        public void keep() {
            setResult(PostRegisterResult.keep());
        }

        /**
         * 修改值
         */
        public void modify(T newValue) {
            setResult(PostRegisterResult.modify(newValue));
        }

        /**
         * 删除资源
         */
        public void remove() {
            setResult(PostRegisterResult.remove());
        }
    }
}
