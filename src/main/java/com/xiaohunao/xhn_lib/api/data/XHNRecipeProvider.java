package com.xiaohunao.xhn_lib.api.data;

import com.google.common.collect.Sets;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.WithConditions;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 自定义的 RecipeProvider 基类，不包含 Advancement 功能。
 * 允许子类自定义 getName() 方法以避免重复提供者错误。
 */
public abstract class XHNRecipeProvider implements DataProvider {
    protected final PackOutput.PathProvider recipePathProvider;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public XHNRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.recipePathProvider = output.createRegistryElementsPathProvider(Registries.RECIPE);
        this.registries = registries;
    }

    @Override
    public final CompletableFuture<?> run(CachedOutput output) {
        return this.registries.thenCompose((lookupProvider) -> this.run(output, lookupProvider));
    }

    protected CompletableFuture<?> run(final CachedOutput output, final HolderLookup.Provider registries) {
        final Set<ResourceLocation> set = Sets.newHashSet();
        final List<CompletableFuture<?>> list = new ArrayList<>();

        this.buildRecipes(new RecipeOutput() {
            @Override
            public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable net.minecraft.advancements.AdvancementHolder advancement, ICondition... conditions) {
                if (!set.add(id)) {
                    throw new IllegalStateException("Duplicate recipe " + id);
                } else {
                    list.add(DataProvider.saveStable(
                            output,
                            registries,
                            Recipe.CONDITIONAL_CODEC,
                            Optional.of(new WithConditions<>(recipe, conditions)),
                            XHNRecipeProvider.this.recipePathProvider.json(id)
                    ));
                }
            }

            @Override
            public net.minecraft.advancements.Advancement.Builder advancement() {
                throw new UnsupportedOperationException("Advancements are not supported in XHNRecipeProvider");
            }
        }, registries);

        return CompletableFuture.allOf(list.toArray(new CompletableFuture[0]));
    }

    protected void buildRecipes(RecipeOutput recipeOutput, HolderLookup.Provider holderLookup) {
        this.buildRecipes(recipeOutput);
    }

    protected abstract void buildRecipes(RecipeOutput recipeOutput);

    protected static String getItemName(ItemLike itemLike) {
        return BuiltInRegistries.ITEM.getKey(itemLike.asItem()).getPath();
    }
}

