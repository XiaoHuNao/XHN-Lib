package com.xiaohunao.xhn_lib.api.recipe.input;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

public interface FluidRecipeInput extends RecipeInput {

    @Override
    default ItemStack getItem(int index) {
        return ItemStack.EMPTY;
    }

    FluidStack getFluid(int index);

    @Override
    default boolean isEmpty() {
        for (int i = 0; i < size(); i++) {
            if (!getFluid(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}