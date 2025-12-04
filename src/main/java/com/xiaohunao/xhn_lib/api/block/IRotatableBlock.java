package com.xiaohunao.xhn_lib.api.block;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * 可旋转方块接口
 * 只负责抽象旋转相关的核心逻辑，方便各个方块类复用
 */
public interface IRotatableBlock {

    // 六向
    DirectionProperty FACING_ALL = DirectionProperty.create("facing", Direction.values());
    // 水平四向
    DirectionProperty FACING_HORIZONTAL = DirectionProperty.create("subfacing", Plane.HORIZONTAL);

    /**
     * 每个实现类需要指定自己的旋转类型
     */
    RotationType getRotationType();

    /**
     * 通用的放置时朝向计算逻辑
     * 实现类在 Block 中可以这样用：
     *
     * <pre>
     *   @Override
     *   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
     *       return IRotatableBlock.super.getStateForPlacement(this, ctx);
     *   }
     * </pre>
     */
    default BlockState getStateForPlacement(Block block, BlockPlaceContext context) {
        return this.getRotationType().getHandler().getStateForPlacement(block, context);
    }

    /**
     * 通用的旋转实现（/rotate 命令、结构块等会调用）
     *
     * 实现类在 Block 中可以这样用：
     *
     * <pre>
     *   @Override
     *   public BlockState rotate(BlockState state, Rotation rot) {
     *       return IRotatableBlock.super.rotate(state, rot);
     *   }
     * </pre>
     */
    default BlockState rotate(BlockState state, Rotation rot) {
        DirectionProperty[] properties = this.getRotationType().getProperties();
        if (properties.length > 0) {
            DirectionProperty prop = properties[0];
            Direction current = state.getValue(prop);
            return state.setValue(prop, rot.rotate(current));
        }
        return state;
    }

    /**
     * 通用的镜像实现（结构块等会调用）
     *
     * 实现类在 Block 中可以这样用：
     *
     * <pre>
     *   @Override
     *   public BlockState mirror(BlockState state, Mirror mirror) {
     *       return IRotatableBlock.super.mirror(state, mirror);
     *   }
     * </pre>
     */
    default BlockState mirror(BlockState state, Mirror mirror) {
        DirectionProperty[] properties = this.getRotationType().getProperties();
        if (properties.length > 0) {
            DirectionProperty prop = properties[0];
            Direction current = state.getValue(prop);
            // 先算出镜像对应的旋转，再用 BlockState.rotate 应用
            return state.rotate(mirror.getRotation(current));
        }
        return state;
    }

    /**
     * 旋转类型枚举，抽象出“如何根据放置朝向初始化 BlockState”
     * 以及「这个旋转类型需要哪些 DirectionProperty」
     */
    enum RotationType {
        // 不旋转
        NONE((block, context) -> block.defaultBlockState()),

        // 只水平四向：subfacing = 玩家水平方向的相反方向
        FOUR_WAY((block, context) ->
                block.defaultBlockState()
                        .setValue(IRotatableBlock.FACING_HORIZONTAL,
                                context.getHorizontalDirection().getOpposite()),
                IRotatableBlock.FACING_HORIZONTAL
        ),

        // 六向：facing = 玩家最近看的方向的相反方向
        SIX_WAY((block, context) ->
                block.defaultBlockState()
                        .setValue(IRotatableBlock.FACING_ALL,
                                context.getNearestLookingDirection().getOpposite()),
                IRotatableBlock.FACING_ALL
        ),

        // 24 向示例：这里简单用最近看的方向（不取反），可按需求改
        TWENTY_FOUR_WAY((block, context) ->
                block.defaultBlockState()
                        .setValue(IRotatableBlock.FACING_ALL,
                                context.getNearestLookingDirection()),
                IRotatableBlock.FACING_ALL,
                IRotatableBlock.FACING_HORIZONTAL
        );

        private final RotationHandler handler;
        private final DirectionProperty[] properties;

        RotationType(RotationHandler handler, DirectionProperty... properties) {
            this.handler = handler;
            this.properties = properties;
        }

        public RotationHandler getHandler() {
            return this.handler;
        }

        public DirectionProperty[] getProperties() {
            return this.properties;
        }
    }

    /**
     * “根据方块和放置上下文，算出初始 BlockState” 的函数式接口
     */
    @FunctionalInterface
    interface RotationHandler {
        BlockState getStateForPlacement(Block block, BlockPlaceContext context);
    }
}