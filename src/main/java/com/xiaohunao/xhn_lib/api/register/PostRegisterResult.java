package com.xiaohunao.xhn_lib.api.register;

/**
 * 注册后处理结果
 */
public  class PostRegisterResult<T> {
    private final Action action;
    private final T value;

    private PostRegisterResult(Action action, T value) {
        this.action = action;
        this.value = value;
    }

    public static <T> PostRegisterResult<T> keep() {
        return new PostRegisterResult<>(Action.KEEP, null);
    }

    public static <T> PostRegisterResult<T> modify(T newValue) {
        return new PostRegisterResult<>(Action.MODIFY, newValue);
    }

    public static <T> PostRegisterResult<T> remove() {
        return new PostRegisterResult<>(Action.REMOVE, null);
    }

    public Action getAction() {
        return action;
    }

    public T getValue() {
        return value;
    }

    public enum Action {
        KEEP,    // 保持原样
        MODIFY,  // 修改值
        REMOVE   // 删除
    }
}