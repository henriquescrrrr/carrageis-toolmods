package pt.henrique.toolmods.command;

import com.mojang.brigadier.Command;
import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.gui.MainMenuGui;
import pt.henrique.toolmods.gui.MyModsGui;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Registers the /mods command using Paper's Brigadier lifecycle API.
 * <p>
 * Sub-commands:
 * <ul>
 *   <li>{@code /mods} — opens the main shop GUI</li>
 *   <li>{@code /mods manage} — opens the My Mods GUI for the held item</li>
 *   <li>{@code /mods reload} — reloads config (requires toolmods.admin)</li>
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
public final class ModsCommand {

    private ModsCommand() {}

    public static void register(ToolMods plugin) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();

            commands.register(
                    Commands.literal("mods")
                            // /mods — open shop
                            .executes(ctx -> {
                                CommandSourceStack source = ctx.getSource();
                                if (!(source.getSender() instanceof Player player)) {
                                    source.getSender().sendRichMessage(
                                            plugin.getLangManager().getRaw("general.player-only"));
                                    return Command.SINGLE_SUCCESS;
                                }
                                new MainMenuGui(plugin, player).open();
                                return Command.SINGLE_SUCCESS;
                            })

                            // /mods manage — show mods on held item
                            .then(Commands.literal("manage")
                                    .executes(ctx -> {
                                        CommandSourceStack source = ctx.getSource();
                                        if (!(source.getSender() instanceof Player player)) {
                                            source.getSender().sendRichMessage(
                                                    plugin.getLangManager().getRaw("general.player-only"));
                                            return Command.SINGLE_SUCCESS;
                                        }
                                        new MyModsGui(plugin, player).open();
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )

                            // /mods reload — admin reload
                            .then(Commands.literal("reload")
                                    .requires(source ->
                                            source.getSender().hasPermission("toolmods.admin"))
                                    .executes(ctx -> {
                                        plugin.reloadPlugin();
                                        ctx.getSource().getSender().sendMessage(
                                                plugin.getLangManager().getPrefixed(
                                                        ctx.getSource().getSender() instanceof Player p ? p : null,
                                                        "general.reload-success"));
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                            .build(),
                    "Open the tool mods shop",
                    List.of("toolmods")
            );
        });
    }
}

