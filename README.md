# WorldCupArmor

Paper plugin for Minecraft 1.21.11 that shows netherite armor as team colored leather armor to everyone except the wearer. The real armor stays netherite on the server, so stats, durability and protection are untouched. Only the visual sent to other clients is swapped, letting everyone run the same kit while teams stay recognisable at a glance.

## Requirements

- Paper 1.21.11
- ProtocolLib (latest dev build from ci.dmulloy2.net)

## Usage

Teams are the vanilla scoreboard teams (`/team add <name>`, `/team join <name> <player>`).

Run `/worldcuparmor` (aliases `/wca`, `/armorgui`, permission `worldcuparmor.admin`) to open the admin GUI:

- Pick a team, then dye the helmet, chestplate, leggings and boots individually or all at once
- Choose from the 16 dye colors or enter an exact hex code in chat
- Shift click a piece (or the all pieces button) to pick an armor trim pattern and material
- Click the banner to give the team a country flag, shown as a sprite next to player names
- Toggle the whole system on or off and reload the config

Players in spectator mode see everyone on a team with a glowing outline. The outline uses the scoreboard team color (`/team modify <name> color <color>`), and nobody outside spectator mode sees it.

Colors persist in `config.yml` as `teams.<name>.<piece>: RRGGBB` and can be edited by hand. A piece with a trim is stored as a section with `color`, `trim-pattern` and `trim-material` keys instead. A team's country is stored as `teams.<name>.flag`.

## Country flags

Flags come from the CartCup resource pack in `resourcepack/cartcup-flags.zip`, which adds every flag to the `minecraft:gui` atlas as sprite `cartcup:flag/<country>` (the game only stitches its built in atlases, so the pack cannot define its own). Players need the pack installed (or server resource pack) and a 1.21.9+ client to see the sprites; without it they see a placeholder.

Picking a country in the GUI sets the scoreboard team prefix, so the flag shows in name tags, the tab list and chat. The prefix is built from the MiniMessage template `flag-prefix` in `config.yml` (default `<flag> `), where `<flag>` inserts the team's flag sprite and `<flag:name>` inserts any specific flag. The plugin keeps the prefix applied automatically: it is set when you pick the flag, on startup and reload, and a background task restores it if the team gets recreated or another plugin overwrites it.

Because these are regular scoreboard teams and the prefix is a normal text component, the flags keep working with vanilla mechanics and even without the plugin. The equivalent vanilla command is:

```
/team modify Brazil prefix [{object:"atlas",atlas:"minecraft:gui",sprite:"cartcup:flag/brazil"},{text:" "}]
```

## Building

```
mvn package
```

Requires JDK 21. The jar lands in `target/`.
