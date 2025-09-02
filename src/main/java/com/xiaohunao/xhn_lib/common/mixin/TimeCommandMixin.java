package com.xiaohunao.xhn_lib.common.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.xiaohunao.xhn_lib.XHN_Lib;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TimeCommand.class)
public class TimeCommandMixin {

    @Inject(method = "register", at = @At("TAIL"), remap = false)
    private static void onRegister(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo ci) {
        dispatcher.register(
                Commands.literal("time")
                        .requires(p_139076_ -> p_139076_.hasPermission(2))
                        .then(
                                Commands.literal("setGameTime")
                                        .then(
                                                Commands.argument("gametime", TimeArgument.time())
                                                        .executes(context -> setGameTime(context.getSource(), IntegerArgumentType.getInteger(context, "gametime")))
                                        )
                        )
        );
    }

    /**
     * 设置游戏时间（GameTime）
     */
    private static int setGameTime(CommandSourceStack source, int gameTime) {
        for (ServerLevel serverlevel : source.getServer().getAllLevels()) {
            serverlevel.serverLevelData.setGameTime(gameTime);
        }

        source.sendSuccess(() -> Component.translatable(XHN_Lib.asDescriptionId("commands.time.set_game_time"), gameTime), true);
        return (int) (source.getLevel().getGameTime() % 2147483647L);
    }
}
