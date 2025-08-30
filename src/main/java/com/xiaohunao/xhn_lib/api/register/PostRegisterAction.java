package com.xiaohunao.xhn_lib.api.register;

import net.minecraft.resources.ResourceLocation;

/**
 * 注册后处理操作
 */
public record PostRegisterAction<T>(ResourceLocation location, T value, PostRegisterResult<T> result) {
}