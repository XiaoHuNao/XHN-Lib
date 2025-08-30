package com.xiaohunao.xhn_lib.common.codec;

import com.mojang.serialization.MapCodec;

public interface ICodec<T> {
    MapCodec<? extends T> codec();
}
