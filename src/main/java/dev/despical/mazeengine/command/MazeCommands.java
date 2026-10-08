/*
 * MazeEngine - Deterministic maze generation for Minecraft.
 * Copyright (C) 2026  Berke Akçen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.despical.mazeengine.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;

import dev.despical.mazeengine.MazeEnginePlugin;
import dev.despical.mazeengine.service.MazeService;

import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Receives ACF command invocations and routes them through common validation.
 * <p>
 * Annotated subcommands declare action permissions and prepend their action
 * name before entering the shared execution path. The default handler also
 * uses that path, providing consistent behavior for unknown or empty input.
 * <p>
 * Execution normalizes the action, checks the supported command list, parses
 * options, and delegates to MazeCommandDispatcher. Existing-name and
 * unknown-name failures receive dedicated message panels; other exceptions
 * are rendered through the configured error template.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
@CommandAlias("__mazeengine")
@CommandPermission("mazeengine.use")
public final class MazeCommands extends BaseCommand {

    private final MazeEnginePlugin plugin;
    private static final List<String> ACTIONS = List.of("help", "create", "list", "info", "tp", "delete", "regenerate",
        "cancel", "presets", "reload", "preview", "solve", "setspawn", "build", "confirm", "dismiss");

    public MazeCommands(MazeEnginePlugin plugin) {
        this.plugin = plugin;
    }

    @Default
    @CatchUnknown
    public void defaultCommand(CommandSender sender, String[] args) {
        execute(sender, args);
    }

    @Subcommand("create")
    @CommandPermission("mazeengine.create")
    public void createCommand(CommandSender sender, String[] args) {
        route(sender, "create", args);
    }

    @Subcommand("list")
    @CommandPermission("mazeengine.list")
    public void listCommand(CommandSender sender, String[] args) {
        route(sender, "list", args);
    }

    @Subcommand("info")
    @CommandPermission("mazeengine.info")
    public void infoCommand(CommandSender sender, String[] args) {
        route(sender, "info", args);
    }

    @Subcommand("tp")
    @CommandPermission("mazeengine.tp")
    public void tpCommand(CommandSender sender, String[] args) {
        route(sender, "tp", args);
    }

    @Subcommand("delete")
    @CommandPermission("mazeengine.delete")
    public void deleteCommand(CommandSender sender, String[] args) {
        route(sender, "delete", args);
    }

    @Subcommand("regenerate")
    @CommandPermission("mazeengine.regenerate")
    public void regenerateCommand(CommandSender sender, String[] args) {
        route(sender, "regenerate", args);
    }

    @Subcommand("cancel")
    @CommandPermission("mazeengine.cancel")
    public void cancelCommand(CommandSender sender, String[] args) {
        route(sender, "cancel", args);
    }

    @Subcommand("presets")
    @CommandPermission("mazeengine.presets")
    public void presetsCommand(CommandSender sender, String[] args) {
        route(sender, "presets", args);
    }

    @Subcommand("reload")
    @CommandPermission("mazeengine.reload")
    public void reloadCommand(CommandSender sender, String[] args) {
        route(sender, "reload", args);
    }

    @Subcommand("preview")
    @CommandPermission("mazeengine.preview")
    public void previewCommand(CommandSender sender, String[] args) {
        route(sender, "preview", args);
    }

    @Subcommand("solve")
    @CommandPermission("mazeengine.solve")
    public void solveCommand(CommandSender sender, String[] args) {
        route(sender, "solve", args);
    }

    @Subcommand("setspawn")
    @CommandPermission("mazeengine.setspawn")
    public void setspawnCommand(CommandSender sender, String[] args) {
        route(sender, "setspawn", args);
    }

    @Subcommand("build")
    @CommandPermission("mazeengine.create")
    public void buildCommand(CommandSender sender, String[] args) {
        route(sender, "build", args);
    }

    @Subcommand("confirm")
    public void confirmCommand(CommandSender sender, String[] args) {
        route(sender, "confirm", args);
    }

    @Subcommand("dismiss")
    public void dismissCommand(CommandSender sender, String[] args) {
        route(sender, "dismiss", args);
    }

    @Subcommand("help")
    public void helpCommand(CommandSender sender, String[] args) {
        route(sender, "help", args);
    }

    private void route(CommandSender sender, String action, String[] args) {
        String[] full = new String[args.length + 1];
        full[0] = action;

        System.arraycopy(args, 0, full, 1, args.length);
        execute(sender, full);
    }

    public void execute(CommandSender sender, String[] args) {
        String action = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);

        try {
            if (!ACTIONS.contains(action)) {
                throw new IllegalArgumentException("Unknown command. Use /maze help.");
            }

            String permission = action.equals("build") ? "create" : action;

            if (!List.of("help", "confirm", "dismiss").contains(action) && !sender.hasPermission("mazeengine." + permission)) {
                throw new IllegalArgumentException("Missing permission: mazeengine." + permission);
            }

            CommandOptions options = CommandOptions.parse(Arrays.copyOfRange(args, Math.min(1, args.length), args.length));
            new MazeCommandDispatcher(plugin).execute(action, sender, options);
        } catch (MazeService.MazeExistsException exception) {
            plugin.messages().send(sender, "maze-exists", "name", exception.mazeName());
        } catch (MazeService.UnknownMazeException exception) {
            plugin.messages().send(sender, "unknown-maze", "name", exception.mazeName());
        } catch (Exception exception) {
            plugin.messages().send(sender, "error", "reason",
                Objects.toString(exception.getMessage(), exception.getClass().getSimpleName()));
        }
    }
}
