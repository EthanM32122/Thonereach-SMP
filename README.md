# Starlit-SMP

The original Thonereach SMP project, renamed to Starlit-SMP. This keeps the original Java plugin gameplay code and commands.

## Features in this source
- Cash economy, player payments, and shard balances
- Player stats, kills, deaths, and balance leaderboard
- Spawn, homes, named warps, random teleport, and teleport requests
- Private messages and player ignore
- AFK shard rewards
- Item selling and shop price information
- Sidebar scoreboard

## Build
Requires Java 8 and Maven. Run:

```sh
mvn clean package
```

The plugin JAR is generated at `target/StarlitSMP.jar`.

## Server compatibility
The source and Maven dependency target **Spigot/Paper 1.12.2**. The old Dockerfile used an EaglerX 1.8 image; that image is not a matching 1.12.2 runtime. Use a 1.12.2 server build with an Eaglercraft-compatible WebSocket layer before deploying this plugin.
