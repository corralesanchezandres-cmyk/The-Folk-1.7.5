package com.thefolk;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Comandos de prueba (requieren permisos de operador):
 *  /thefolk spawn <1-4>, /thefolk thing, /thefolk mode <normal|error|coming>, /thefolk status
 */
@Mod.EventBusSubscriber(modid = TheFolkMod.MOD_ID)
public class FolkCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("thefolk")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("phase", IntegerArgumentType.integer(1, 4))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            int phase = IntegerArgumentType.getInteger(ctx, "phase");
                                            boolean ok = FolkDirector.trySpawn(player, phase, true);
                                            msg(ctx, ok ? "The Folk (fase " + phase + ") ha aparecido."
                                                    : "No se encontro un lugar para que aparezca.");
                                            return ok ? 1 : 0;
                                        })))
                        .then(Commands.literal("thing")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    boolean ok = FolkDirector.trySpawnThing(player, true);
                                    msg(ctx, ok ? "The Thing ha aparecido (20 segundos)."
                                            : "No se encontro un lugar para que aparezca.");
                                    return ok ? 1 : 0;
                                }))
                        .then(Commands.literal("mode")
                                .then(Commands.literal("normal")
                                        .executes(ctx -> setMode(ctx, FolkDirector.MODE_NORMAL)))
                                .then(Commands.literal("error")
                                        .executes(ctx -> setMode(ctx, FolkDirector.MODE_ERROR)))
                                .then(Commands.literal("coming")
                                        .executes(ctx -> setMode(ctx, FolkDirector.MODE_COMING))))
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    ServerLevel level = ctx.getSource().getLevel();
                                    int mode = FolkDirector.currentMode(level);
                                    long day = FolkDirector.currentDay(level);
                                    int phase = FolkDirector.phaseOfDay(day, mode);
                                    int dayInCycle = FolkDirector.dayInCycle(day, mode);
                                    msg(ctx, "Modo: " + FolkDirector.modeName(mode) + " | dia " + day
                                            + " del mundo | dia " + dayInCycle + " del ciclo | fase " + phase);
                                    return 1;
                                })));
    }

    private static void msg(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
    }

    private static int setMode(CommandContext<CommandSourceStack> ctx, int mode) {
        FolkDirector.setMode(ctx.getSource().getLevel(), mode);
        msg(ctx, "Modo: " + FolkDirector.modeName(mode));
        return 1;
    }
}
