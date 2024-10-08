package eu.pb4.styledplayerlist.command;


import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.styledplayerlist.GenericModInfo;
import eu.pb4.styledplayerlist.Permissions;
import eu.pb4.styledplayerlist.access.PlayerListViewerHolder;
import eu.pb4.styledplayerlist.config.ConfigManager;
import eu.pb4.styledplayerlist.config.PlayerListStyle;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.Locale;

import static net.minecraft.server.command.CommandManager.literal;

@EventBusSubscriber
public class Commands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
            dispatcher.register(
                    literal("styledplayerlist")
                            .requires(Permissions.require("styledplayerlist.main", true))
                            .executes(Commands::about)
                            .then(literal("switch")
                                    .requires(Permissions.require("styledplayerlist.switch", true))
                                    .then(switchArgument("style")
                                            .executes(Commands::switchStyle)
                                    )
                            )

                            .then(literal("switchothers")
                                    .requires(Permissions.require("styledplayerlist.switch.others", 2))
                                    .then(CommandManager.argument("targets", EntityArgumentType.players())
                                            .then(switchArgument("style")
                                                    .executes(Commands::switchStyleOthers)
                                            )
                                    )
                            )

                            .then(literal("reload")
                                    .requires(Permissions.require("styledplayerlist.reload", 3))
                                    .executes(Commands::reloadConfig)
                            )
            );

            dispatcher.register(
                    literal("plstyle")
                            .requires(Permissions.require("styledplayerlist.switch", true))
                            .then(switchArgument("style")
                                    .executes(Commands::switchStyle)
                            )
            );
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        if (ConfigManager.loadConfig()) {
            context.getSource().sendFeedback(() -> Text.literal("Reloaded config!"), false);
        } else {
            context.getSource().sendError(Text.literal("Error accrued while reloading config!").formatted(Formatting.RED));
        }
        for (var player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
            ((PlayerListViewerHolder) player.networkHandler).styledPlayerList$reloadStyle();
            ((PlayerListViewerHolder) player.networkHandler).styledPlayerList$setupRightText();
        }

        return 1;
    }

    private static int about(CommandContext<ServerCommandSource> context) {
        for (var text : (context.getSource().getEntity() instanceof ServerPlayerEntity ? GenericModInfo.getAboutFull() : GenericModInfo.getAboutConsole())) {
            context.getSource().sendFeedback(() -> text, false);
        };

        return 1;
    }

    public static int switchStyleOthers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String styleId = context.getArgument("style", String.class);
        Collection<ServerPlayerEntity> target = EntityArgumentType.getPlayers(context, "targets");

        if (!ConfigManager.styleExist(styleId)) {
            source.sendFeedback(() -> ConfigManager.getConfig().unknownStyleMessage, false);
            return 0;
        }

        PlayerListStyle style = ConfigManager.getStyle(styleId);

        for (ServerPlayerEntity player : target) {
            ((PlayerListViewerHolder) player.networkHandler).styledPlayerList$setStyle(styleId);
        }

        source.sendFeedback(() -> Text.literal("Changed player list style of targets to " + style.name), false);


        return 2;
    }

    private static int switchStyle(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            ServerCommandSource source = context.getSource();
            String styleId = context.getArgument("style", String.class);

            if (!ConfigManager.styleExist(styleId)) {
                source.sendFeedback(() -> ConfigManager.getConfig().unknownStyleMessage, false);
                return 0;
            }

            PlayerListStyle style = ConfigManager.getStyle(styleId);
            ServerPlayerEntity player = source.getPlayer();

            if (player != null && player instanceof ServerPlayerEntity) {
                if (style.hasPermission(player)) {
                    ((PlayerListViewerHolder) player.networkHandler).styledPlayerList$setStyle(styleId);

                    source.sendFeedback(() -> ConfigManager.getConfig().getSwitchMessage(player, style.name), false);
                    return 1;
                } else {
                    source.sendFeedback(() -> ConfigManager.getConfig().permissionMessage, false);
                }
            } else {
                source.sendFeedback(() -> Text.literal("Only players can use this command!"), false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static RequiredArgumentBuilder<ServerCommandSource, String> switchArgument(String name) {
        return CommandManager.argument(name, StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

                    for (PlayerListStyle style : ConfigManager.getStyles()) {
                        if (style.id.contains(remaining) && style.hasPermission(ctx.getSource())) {
                            builder.suggest(style.id);
                        }
                    }

                    return builder.buildFuture();
                });
    }


}
