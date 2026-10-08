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

package dev.despical.mazeengine.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and renders the plugin's configured MiniMessage templates.
 * <p>
 * Loading checks message value types, validates MiniMessage syntax, requires
 * the essential keys, and fills additional keys from bundled defaults. The
 * published template map is copied so later YAML edits do not alter the active
 * messages until a successful reload.
 * <p>
 * Rendering substitutes plain arguments as unparsed placeholders and accepts
 * explicit Adventure components for rich actions. Named colors are normalized
 * to the shared palette, and panel-capable messages delegate contextual button
 * selection and layout to MessagePanels.
 *
 * @author Despical
 * <p>
 * Created at 05.10.2026
 */
public record Messages(Map<String, String> values) {

    public static Messages load(Path file) throws Exception {
        var yaml = new YamlConfiguration();
        yaml.load(file.toFile());

        var defaults = new YamlConfiguration();
        try (var input = Messages.class.getResourceAsStream("/messages.yml")) {
            if (input == null) {
                throw new IllegalStateException("Bundled messages are missing.");
            }

            defaults.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        }

        Map<String, String> templates = new HashMap<>();

        for (String key : yaml.getKeys(false)) {
            if (!yaml.isString(key)) {
                throw new IllegalArgumentException("Message must be text: " + key);
            }

            String text = yaml.getString(key);
            MiniMessage.miniMessage().deserialize(text);

            templates.put(key, text);
        }

        for (String key : List.of("prefix", "line", "queued", "completed", "deleted", "cancelled", "teleported",
            "reloaded", "empty", "error", "progress", "list-entry", "info")) {

            if (!templates.containsKey(key)) {
                throw new IllegalArgumentException("Missing message: " + key);
            }
        }

        for (String key : defaults.getKeys(false)) {
            templates.putIfAbsent(key, defaults.getString(key));
        }

        return new Messages(Map.copyOf(templates));
    }

    public Component format(String key, String... args) {
        return rich(key, Map.of(), args);
    }

    public Component rich(String key, Map<String, Component> components, String... args) {
        var builder = TagResolver.builder();
        for (int index = 0; index < args.length; index += 2) {
            builder.resolver(Placeholder.unparsed(args[index], args[index + 1]));
        }

        components.forEach((name, value) -> builder.resolver(Placeholder.component(name, value)));

        String template = values.getOrDefault(key, key);
        Map<String, String> colors = Map.ofEntries(Map.entry("black", "#181B26"), Map.entry("dark_gray", "#586174"),
            Map.entry("gray", "#A8B3C7"), Map.entry("white", "#E6EDF7"), Map.entry("green", "#8BD5CA"),
            Map.entry("dark_green", "#6BAF91"), Map.entry("yellow", "#EED49F"), Map.entry("gold", "#F5A97F"),
            Map.entry("red", "#FF5555"), Map.entry("dark_red", "#AA0000"), Map.entry("aqua", "#91D7E3"),
            Map.entry("dark_aqua", "#7AB9C4"), Map.entry("blue", "#8AADF4"), Map.entry("dark_blue", "#7283C7"),
            Map.entry("light_purple", "#C6A0F6"), Map.entry("dark_purple", "#A381D2"));

        for (var color : colors.entrySet()) {
            template = template.replace("<" + color.getKey() + ">", "<" + color.getValue() + ">")
                .replace("</" + color.getKey() + ">", "</" + color.getValue() + ">");
        }

        Component rendered = MiniMessage.miniMessage().deserialize(template, builder.build());
        return key.endsWith("-title") ? rendered : regular(rendered);
    }

    private static Component regular(Component component) {
        return component.decoration(TextDecoration.BOLD, false)
            .children(component.children().stream().map(Messages::regular).toList());
    }

    public Component button(String labelKey, String command, String hoverKey, String color, String... args) {
        return format(labelKey, args).color(TextColor.fromHexString(color)).clickEvent(ClickEvent.runCommand(command))
            .hoverEvent(HoverEvent.showText(format(hoverKey, args)));
    }

    public void sendRich(CommandSender sender, String key, Map<String, Component> components, String... args) {
        if (values.containsKey(key + "-title") && values.containsKey(key + "-body")) {
            new MessagePanels(this).send(sender, key, components, args);
            return;
        }

        sender.sendMessage(format("prefix").append(rich(key, components, args)));
        sender.sendMessage(Component.empty());
    }

    public void send(CommandSender sender, String key, String... args) {
        sendRich(sender, key, Map.of(), args);
    }
}
