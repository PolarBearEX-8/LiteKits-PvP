# LiteKits PvP

LiteKits PvP is a lightweight Spigot 1.8.8 PvP kit plugin with editable player kits, soup healing, combat tagging, block decay, and simple world rules.

## Features

- Join, leave, set spawn, and reload commands through `/spvp`, `/souppvp`, or `/kitpvp`.
- Player kit loading with `/kit <number>`.
- In-game kit editor with `/editkit <number>`.
- Configurable instant soup healing.
- Combat tagging with combat-log handling.
- Ender pearl cooldown.
- Kill, death, K/D, and killstreak stats.
- Sidebar scoreboard with configurable lines.
- Player-placed block decay with break animation.
- Liquid and generated block decay for water, lava, obsidian, cobblestone, and stone caused by player actions.
- Block protection so LiteKits players can only break blocks caused by player actions.
- Fire spread and burn protection in the LiteKits spawn world.
- Optional world rules for disabling night, weather, and natural monster spawns.

## Commands

| Command | Description |
| --- | --- |
| `/spvp join` | Join LiteKits. |
| `/spvp leave` | Leave LiteKits. |
| `/spvp setspawn` | Set the LiteKits spawn. |
| `/spvp reload` | Reload config, lang, editkit, and world rules. |
| `/souppvp <subcommand>` | Alias for `/spvp`. |
| `/kitpvp <subcommand>` | Alias for `/spvp`. |
| `/kit <number>` | Load a saved kit. |
| `/editkit <number>` | Open the kit editor. |

`/spvp reload`, `/souppvp reload`, and `/kitpvp reload` can be run from console.

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `litekits.admin` | OP | Allows `/spvp setspawn` and `/spvp reload`. |
| `litekits.kit` | Everyone | Allows `/kit` and `/editkit`. |
| `souppvp.admin` | OP | Legacy admin permission. |
| `souppvp.kit` | Everyone | Legacy kit permission. |

## Configuration

The generated `config.yml` exposes feature toggles and tuning values:

```yml
features:
  kits: true
  editkit: true
  instant-soup: true
  scoreboard: true
  stats: true
  death-messages: true
  enderpearl-cooldown: true
  combat: true
  combat-log: true
  block-decay: true
  liquid-decay: true
  generated-block-decay: true
  block-break-protection: true
  fire-protection: true

combat:
  cooldown-seconds: 20
  block-leave-command: true
  block-kit-command: true
  block-editkit-command: true

enderpearl:
  cooldown-seconds: 15

soup:
  heal-health: 6.0
  add-food: 6
  add-saturation: 7.2
  play-sound: true
  return-bowl: true
```

World rules accept world names, inline lists, or `-ALL`:

```yml
disable-night: [world]
disable-weather: [world, arena]
disable-world-mob-spawn: -ALL
```

`disable-world-mob-spawn` only blocks natural/world-generated monsters. Spawn eggs, spawners, commands, and plugin-spawned mobs still work.

## Edit Kit Configuration

`editkit.yml` is generated as an editable example. Delete the file or leave it empty to regenerate the example. If the file already contains settings, LiteKits will not restore deleted categories or items.

Categories support:

- `enabled`
- `hotbar-slot`
- `order`
- `item`
- `name`
- `items`

Items support material names, numeric IDs, damage/data values, custom names, lore, and enchantments.

## Building

Requirements:

- Java 8 or newer
- Gradle 8.x

Build:

```bash
gradle build
```

The plugin jar is created in `build/libs/`.

GitHub Actions is included and will compile the plugin on pushes and pull requests.

## License

LiteKits PvP is provided under the LiteKits PvP Perimeter License, based on the PolyForm Perimeter License 1.0.0. See [LICENSE](LICENSE).
