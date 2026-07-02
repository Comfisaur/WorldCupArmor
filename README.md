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
- Toggle the whole system on or off and reload the config

Colors persist in `config.yml` as `teams.<name>.<piece>: RRGGBB` and can be edited by hand.

## Building

```
mvn package
```

Requires JDK 21. The jar lands in `target/`.
