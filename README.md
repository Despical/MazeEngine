![Maze Engine with 30 themed presets, arrow guides and seeded generation](assets/mazeengine-cover.png)

# Maze Engine

[![CI](https://github.com/Despical/MazeEngine/actions/workflows/build.yml/badge.svg)](https://github.com/Despical/MazeEngine/actions/workflows/build.yml)
![Java](https://img.shields.io/badge/Java-25-007396.svg)
![Gradle](https://img.shields.io/badge/Gradle-9.8.0-079ec0?logo=gradle&logoColor=white)
![Paper](https://img.shields.io/badge/Paper-1.21.11%2B-62b47a)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

Maze Engine creates persistent, deterministic Minecraft mazes with themed blocks, private previews and a route guide.

[Hub](https://hub.despical.dev/plugins/maze-engine) · [SpigotMC](https://www.spigotmc.org/resources/maze-engine-30-themes-private-previews-route-guides.139500/) · [BuiltByBit](https://builtbybit.com/resources/maze-engine-themed-maze-generator.130660/) · [Modrinth](https://modrinth.com/plugin/maze-engine)

Read the [documentation](https://docs.despical.dev/maze-engine/) for setup instructions, configuration details and examples.

---

## Features

- Seeded PERFECT and BRAIDED generation with configurable complexity and geometry.
- Thirty bundled themes, weighted block palettes, optional roofs, lights and decorations.
- Private block-display previews with a build button and no terrain changes.
- A private arrow guide that follows the shortest route to the exit.
- Tick-budgeted world operations, chunk tickets and configurable generation limits.
- Saved layouts, ownership, safe teleport points and interrupted-operation recovery.
- SAFE, REPLACE and CLEAR placement, plus optional original-terrain snapshots.
- Clickable chat panels, permission-aware commands and configurable messages.
- Optional [WorldEdit](https://worldedit.enginehub.org/en/latest/), [FastAsyncWorldEdit](https://intellectualsites.gitbook.io/fastasyncworldedit), [WorldGuard](https://worldguard.enginehub.org/en/latest/) and [PlaceholderAPI](https://wiki.placeholderapi.com/) integrations.
- A public API registered with Bukkit's service manager.

---

## Installation

Maze Engine supports [Paper](https://papermc.io/downloads/paper) and compatible forks such as [Purpur](https://purpurmc.org/download/purpur) for Minecraft **1.21.11 and later**. Run the server with **[Java 25](https://adoptium.net/temurin/releases/?version=25)**. Folia is not supported.

1. Download the latest plugin JAR from [Hub](https://hub.despical.dev/plugins/maze-engine) or [SpigotMC](https://www.spigotmc.org/resources/maze-engine-30-themes-private-previews-route-guides.139500/).
2. Stop the server and place `maze-engine-<version>.jar` in its `plugins/` directory.
3. Install any optional integrations you want to use, choosing builds compatible with your server version.
4. Start the server with Java 25 and check the console for a successful Maze Engine startup.
5. Run `/maze help` as an operator or with the relevant [permissions](https://docs.despical.dev/maze-engine/permissions/).

The [BuiltByBit](https://builtbybit.com/resources/maze-engine-themed-maze-generator.130660/) and [Modrinth](https://modrinth.com/plugin/maze-engine) pages are linked above; downloads there will be available once those pages are published. Building from source is optional; see [Build from Source](#build-from-source) below.

Missing settings and the 30 bundled presets are installed under `plugins/MazeEngine/`; existing files are preserved. Saved maze records and optional terrain snapshots use the same data directory.

[WorldEdit](https://worldedit.enginehub.org/en/latest/) or [FastAsyncWorldEdit](https://intellectualsites.gitbook.io/fastasyncworldedit) is required for selections and terrain snapshots. [WorldGuard](https://worldguard.enginehub.org/en/latest/) checks region permissions during preflight and block writes when installed. [PlaceholderAPI](https://wiki.placeholderapi.com/) is optional.

---

## Getting started

```text
/maze presets
/maze preview garden 15 15 --preset Hedge --seed 42
/maze create garden
/maze tp garden
/maze solve garden
```

Move outside the preview region before building. A matching `/maze create <name>` uses the active preview's exact settings. You can also click its Create button.

Dimensions describe logical cells. Physical size depends on path width and wall thickness. The default origin is three blocks east and south of the player, with the floor one block below their feet.

---

## Commands

`/maze` and `/mazeengine` share the same command tree. `/maze help` and `/maze help concepts` provide paginated in-game reference panels.

| Command | Purpose |
| --- | --- |
| `/maze create <name> [width depth] [options]` | Generate a maze or build a matching preview. |
| `/maze preview <name> [width depth] [options]` | Preview the maze privately. |
| `/maze preview stop` | Close the active preview. |
| `/maze presets [page]` | Browse themes and click to preview. |
| `/maze list [page]` | Browse saved mazes. |
| `/maze info <name>` | Inspect geometry, seed, status and management buttons. |
| `/maze tp <name>` | Teleport to a safe saved point or the entrance. |
| `/maze solve <name> [stop]` | Open or close the route guide. |
| `/maze setspawn <name> [reset]` | Save your location or restore the entrance destination. |
| `/maze regenerate <name> [--seed <value> / --new-seed / --repair]` | Rebuild the maze or repair its non-air blocks. |
| `/maze delete <name> [--restore / --clear] [--confirm]` | Confirm deletion; restore a snapshot or clear the region. |
| `/maze cancel <name>` | Cancel the current operation. |
| `/maze reload` | Validate and reload configuration atomically. |

Creation and preview options:

| Option | Purpose |
| --- | --- |
| `--preset <name>` | Choose a preset; defaults to `default`. |
| `--seed <number>` | Reuse a signed 64-bit generation seed. |
| `--complexity <value>` | Complexity from 0.0 to 1.0. |
| `--path-width <number>` | Passage width from 1 to 16 blocks. |
| `--wall-thickness <number>` | Wall thickness from 1 to 8 blocks. |
| `--wall-height <number>` | Wall height from 2 to 64 blocks. |
| `--mode <CLEAR / SAFE / REPLACE>` | Choose terrain placement rules. |
| `--snapshot` | Save original terrain through WorldEdit before building. |
| `--selection` | Fit a centered maze inside your WorldEdit cuboid. |
| `--world <name> --x <x> --y <y> --z <z>` | Explicit origin; required from console. |

SAFE allows replaceable floor blocks and requires air above them. REPLACE checks occupied blocks against `placement.replaceable`. CLEAR overwrites the planned region. All modes check world bounds, overlap and player occupancy before mutation.

---

## Permissions

`mazeengine.admin` grants all command permissions, `mazeengine.manage.others` and `mazeengine.bypass`. Permissions default to operators.

Each command requires `mazeengine.use` and its matching permission, such as `mazeengine.create`, `mazeengine.preview` or `mazeengine.solve`. Building a preview requires `mazeengine.create`; repairing requires `mazeengine.regenerate`.

`mazeengine.manage.others` allows managing another player's maze. `mazeengine.bypass` allows player block edits in completed protected mazes. Active world operations remain protected.

---

## Configuration

- `config.yml`: performance budgets, limits, terrain protection, mob spawning, progress bars and visual settings.
- `messages.yml`: MiniMessage panels, buttons, hover text, progress labels and action bars.
- `presets/*.yml`: geometry, generation settings, palettes and decorations for each theme.

Every global setting is explained in the bundled configuration. Set `visuals.guide.block` to a colored block such as `minecraft:red_concrete` to recolor arrows. Preview and guide lifetimes, arrow size, display brightness and boss bar appearance are configurable. Defaults preserve the standard appearance.

Reload validates the complete configuration before applying it. Existing mazes retain their saved preset and topology; preset edits affect new mazes. Display lighting, view range and guide geometry update on reload. Visual lifetimes are captured when sessions start.

Mob spawning is blocked above maze footprints by default, independently of terrain protection. Existing mobs are not removed. Snapshots retain original terrain through regeneration; `delete --restore` requires the saved schematic.

---

## Public API

Import `dev.despical.mazeengine.api.MazeEngineApi` and obtain it from Bukkit:

```java
MazeEngineApi api = Bukkit.getServicesManager().load(MazeEngineApi.class);
if (api != null) {
    api.mazes().all().forEach(maze -> getLogger().info(maze.id().value()));
}
```

Call the API from the server thread. Immutable snapshots can be retained afterward. Operation handles expose progress, cancellation and completion. `MazeOperationCompletedEvent` and `MazeOperationFailedEvent` report final outcomes; `OperationKind` identifies creation, regeneration, repair or deletion.

Binary and source API JARs are included in the build. Generate API Javadocs separately as described below.

---

## PlaceholderAPI

Install [PlaceholderAPI](https://wiki.placeholderapi.com/) to use these placeholders. Maze Engine registers its expansion automatically; no eCloud download is needed.

| Placeholder | Value |
| --- | --- |
| `%mazeengine_count%` | Number of saved mazes. |
| `%mazeengine_preset_count%` | Number of available presets. |
| `%mazeengine_active_operations%` | Number of active maze operations. |
| `%mazeengine_owned_count%` | Number of mazes owned by the player. |
| `%mazeengine_current_<field>%` | Field from the maze the player is currently inside. |
| `%mazeengine_maze_<name>_<field>%` | Field from a named maze, for example `%mazeengine_maze_garden_theme%`. |

Replace `<field>` with one of the suffixes below to choose the value you want to display.

| Field | Value |
| --- | --- |
| `name` | Saved maze name. |
| `theme` | Preset display name. |
| `difficulty` | Easy, Medium or Hard. |
| `seed` | Generation seed. |
| `status` | Saved operation status. |
| `cells_width`, `cells_depth` | Logical grid dimensions. |
| `path_width`, `wall_thickness`, `wall_height` | Geometry in blocks. |
| `roof` | Whether the maze has a roof. |
| `route_length` | Number of cell transitions on the shortest solution route. |
| `dead_ends` | Number of dead ends. |
| `progress` | Operation progress from 0 to 100. |
| `snapshot` | Whether an original-terrain snapshot is saved. |

Current-maze and named-maze fields return empty text when no matching maze exists. Values refresh every 10 server ticks.

---

## Build from Source

Install [Java 25](https://adoptium.net/temurin/releases/?version=25) and [Git](https://git-scm.com/downloads).

1. Clone the repository with `git clone https://github.com/Despical/MazeEngine.git`.
2. Enter the project folder with `cd MazeEngine`.
3. Build the plugin:

   ```bash
   ./gradlew clean build
   ```

   On Windows, use `gradlew.bat clean build`.

The build runs unit tests and creates the plugin, API and source JARs under `build/libs/`. Install `maze-engine-<version>.jar` on your server; the `-api`, `-sources` and Javadoc JARs are for developers and are not the server plugin.

API Javadocs are optional: run `./gradlew apiJavadoc` (Windows: `gradlew.bat apiJavadoc`) to generate them under `build/docs/api/`. Use `javadocJar apiJavadocJar` with the wrapper if you also want documentation JARs.

---

## Security and contributing

Report vulnerabilities privately as described in [SECURITY.md](SECURITY.md). Follow [CONTRIBUTING.md](CONTRIBUTING.md) and the [Code of Conduct](CODE_OF_CONDUCT.md).

Use four spaces, focused classes, English documentation and the existing GPL license/class Javadoc headers. Preserve gameplay and saved maze formats unless a change explicitly requires otherwise.

---

## License

Maze Engine is licensed under [GPL-3.0-or-later](LICENSE).
