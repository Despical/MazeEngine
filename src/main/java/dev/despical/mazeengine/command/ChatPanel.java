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

import dev.despical.mazeengine.MazeEnginePlugin;
import dev.despical.mazeengine.config.Preset;
import dev.despical.mazeengine.service.MazeService;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.map.MinecraftFont;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Provides the shared layout and interaction primitives used by chat panels.
 * <p>
 * Help, presets, maze details, and confirmations reuse its typography, page
 * navigation, theme colors, and pixel-width calculations. Adventure components
 * carry hover descriptions and command suggestions without embedding formatting
 * rules separately in every panel renderer.
 * <p>
 * Private callback buttons bind their command arguments to the issuing player
 * and a limited lifetime. Console output uses ordinary text where player-only
 * interactions are unavailable. Subclasses supply panel contents while this
 * base class keeps spacing and navigation consistent.
 *
 * @author Despical
 * <p>
 * Created at 07.10.2026
 */
class ChatPanel {

    protected final MazeEnginePlugin plugin;

    ChatPanel(MazeEnginePlugin plugin) {
        this.plugin = plugin;
    }

    protected static void checkPage(int page, int pages) {
        if (page < 1 || page > pages) {
            throw new IllegalArgumentException("Choose a page between 1 and " + pages + ".");
        }
    }

    protected void navigation(CommandSender sender, String command, int page, int pages) {
        if (pages <= 1) {
            sender.sendMessage(Component.empty());
            return;
        }

        sender.sendMessage(Component.empty());

        var messages = plugin.messages();
        Component previous = page > 1 ? messages.button("button-previous", command + (page - 1), "hover-previous", "#7ABFCC")
            : messages.format("button-previous").color(TextColor.color(0x586174));
        Component next = page < pages ? messages.button("button-next", command + (page + 1), "hover-next", "#7ABFCC")
            : messages.format("button-next").color(TextColor.color(0x586174));

        sender.sendMessage(center(messages.rich("page-navigation", Map.of("previous", previous, "next", next), "page",
            Integer.toString(page), "pages", Integer.toString(pages)), false));
        sender.sendMessage(Component.empty());
    }

    protected void header(CommandSender sender, String key, String... args) {
        separator(sender);

        sender.sendMessage(Component.empty());
        sender.sendMessage(center(plugin.messages().format(key + "-title", args), true));

        Component description = plugin.messages().format(key + "-description", args);
        if (key.equals("help")) {
            description = plugin.messages().rich("help-description",
                Map.of("link", plugin.messages().button("button-concepts", "/maze help concepts 1", "hover-concepts", "#7ABFCC")),
                args
            );
        }

        sender.sendMessage(center(description, false));
        sender.sendMessage(Component.empty());
    }

    protected void separator(CommandSender sender) {
        sender.sendMessage(center(plugin.messages().format("panel-separator"), false));
    }

    protected void hint(CommandSender sender, String key) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(plugin.messages().rich("info-hint", Map.of("text", plugin.messages().format(key))));
    }

    public static Component center(Component component, boolean bold) {
        int width = pixelWidth(PlainTextComponentSerializer.plainText().serialize(component), bold);
        return Component.text(" ".repeat(Math.max(0, (320 - width) / 8))).append(component);
    }

    protected static int pixelWidth(String text, boolean bold) {
        int width = 0;

        for (char ch : text.toCharArray()) {
            // Default client bitmap advances; Bukkit's map font differs for several chat glyphs.
            int advance;

            if (ch >= 32 && ch <= 126) {
                advance = DEFAULT_ASCII_ADVANCES.charAt(ch - 32) - '0';
            } else if (ch == '•') {
                advance = 3;
            } else if (ch == '·') {
                advance = 2;
            } else {
                var glyph = MinecraftFont.Font.getChar(ch);
                advance = (glyph == null ? 5 : glyph.getWidth()) + 1;
            }

            width += advance + (bold && ch != ' ' ? 1 : 0);
        }

        return width;
    }

    protected static final String DEFAULT_ASCII_ADVANCES = "42466662444626266666666666225656766666666466666666666666666464663666665662653666666646666664247";

    protected Component difficulty(Preset preset) {
        String key = preset.difficulty().name().toLowerCase(Locale.ROOT);
        String color = switch (preset.difficulty()) {
            case EASY -> "#91B78A";
            case MEDIUM -> "#D5B877";
            case HARD -> "#C44B5B";
        };

        return Component.text(plugin.messages().values().get("difficulty-" + key), TextColor.fromHexString(color));
    }

    protected Component presetHover(Preset preset) {
        var messages = plugin.messages();

        return messages.rich("preset-hover", Map.of("difficulty", difficulty(preset)), "name", preset.displayName(),
            "description", wrap(preset.description(), 36), "width", Integer.toString(preset.pathWidth()), "thickness",
            Integer.toString(preset.wallThickness()), "height", Integer.toString(preset.wallHeight()), "roof",
            messages.values().get(preset.roof() ? "ceiling-covered" : "ceiling-open")
        );
    }

    protected Component theme(Preset preset) {
        return Component.text(preset.displayName(), TextColor.fromHexString(preset.accent())).hoverEvent(HoverEvent.showText(presetHover(preset)));
    }

    public static String wrap(String text, int columns) {
        StringBuilder result = new StringBuilder();
        int line = 0;

        for (String word : text.split("\\s+")) {
            if (line > 0 && line + word.length() + 1 > columns) {
                result.append('\n');
                line = 0;
            } else if (line > 0) {
                result.append(' ');
                line++;
            }

            result.append(word);
            line += word.length();
        }

        return result.toString();
    }

    /**
     * Paper translates callbacks to native custom clicks on 26.3; no hidden command is sent to the
     * client.
     */
    public Component callbackButton(CommandSender owner, String label, String hover, String color, int seconds, String... command) {
        return callbackButton(owner, label, hover, color, seconds,
            sender -> new MazeCommands(plugin).execute(sender, command));
    }

    public Component callbackButton(CommandSender owner, String label, String hover, String color, int seconds, Consumer<CommandSender> action) {
        var messages = plugin.messages();

        return messages.format(label).color(TextColor.fromHexString(color)).hoverEvent(HoverEvent.showText(messages.format(hover)))
            .clickEvent(ClickEvent.callback(audience -> {
                if (!(audience instanceof CommandSender sender)) {
                    return;
                }

                if (owner instanceof Player player) {
                    if (!(sender instanceof Player clicked) || !clicked.getUniqueId().equals(player.getUniqueId())) {
                        return;
                    }
                } else if (sender != owner) {
                    return;
                }

                Runnable execute = () -> {
                    if (!plugin.isEnabled()) {
                        return;
                    }

                    if (!sender.hasPermission("mazeengine.use")) {
                        messages.send(sender, "error", "reason", "Missing permission: mazeengine.use");
                        return;
                    }

                    try {
                        action.accept(sender);
                    } catch (MazeService.UnknownMazeException error) {
                        messages.send(sender, "unknown-maze", "name", error.mazeName());
                    } catch (Exception error) {
                        messages.send(sender, "error", "reason", Objects.toString(error.getMessage(), "Please try again."));
                    }
                };

                if (Bukkit.isPrimaryThread()) {
                    execute.run();
                } else if (plugin.isEnabled()) {
                    plugin.getServer().getScheduler().runTask(plugin, execute);
                }
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(Duration.ofSeconds(seconds)).build()));
    }
}
