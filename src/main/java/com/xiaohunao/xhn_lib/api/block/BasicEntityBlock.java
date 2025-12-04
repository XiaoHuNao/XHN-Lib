package com.xiaohunao.xhn_lib.api.block;

import com.xiaohunao.xhn_lib.api.block.entity.BasicBlockEntity;
import com.xiaohunao.xhn_lib.api.block.entity.ITickableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class BasicEntityBlock<T extends BasicBlockEntity> extends BasicBlock implements EntityBlock {
    public BasicEntityBlock(Properties properties) {
        super(properties);
    }

    @Override
    public abstract @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState);

    @Nullable
    @SuppressWarnings("unchecked")
    public <R extends BlockEntity> BlockEntityTicker<R> getTicker(Level level, BlockState blockState, BlockEntityType<R> blockEntityType) {
        return (level1, blockPos, state, blockEntity) -> {
            if (blockEntity instanceof ITickableBlockEntity) {
                ITickableBlockEntity<R> tickable = (ITickableBlockEntity<R>) blockEntity;
                if (level1.isClientSide()) {
                    tickable.clientTick(level1, blockPos, state, blockEntity);
                } else {
                    tickable.serverTick(level1, blockPos, state, blockEntity);
                }
            }
        };
    }
}
