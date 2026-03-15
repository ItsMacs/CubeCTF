# CubeCTF

## Overview
This is a simple CTF (Capture the Flag) plugin, for CubeCraft's take home project.
It manages a simple and lightweight, but fun, CTF gamemode.

## Build
To build the plugin, run `gradle build`.
To run a local test server with Paperweight, run `gradle runServer`

The plugin uses Java 21 and Gradle 9.0.0.
Tested on: `1.21.11`.

The plugin has a `config.yml` and a `lang.yml` file, both are created after the first launch in the `plugins/CubeCTF` folder.

## Commands
| Command | Description | Permission |
|---|---|---|
| `/ctf join <team> [otherPlayer]` | Join a team. Admins can specify another player to force-assign. | None / `ctf.admin` for `[otherPlayer]` |
| `/ctf leave [otherPlayer]` | Leave your current team. Admins can force another player to leave. | None / `ctf.admin` for `[otherPlayer]` |
| `/ctf start` | Force-starts the game. Only works from lobby state. | `ctf.admin` |
| `/ctf stop` | Stops the game. Only works while in progress. | `ctf.admin` |
| `/ctf setflag <team>` | Sets the flag location for a team to your current position. | `ctf.admin` |
| `/ctf score <team> <score>` | Sets the score for a team. Can be negative; exceeding the win threshold triggers an instant win. | `ctf.admin` |
*Note: AI was used to beautify this command layout*
