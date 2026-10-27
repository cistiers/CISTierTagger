English | [Русский](readme_ru.md)

## CISTierTagger
The official tier display mod for [CISTiers](https://cistiers.com)  

### About
CISTierTagger is a mod designed to display a player’s tier in various game modes based on the [cistiers.com](https://cistiers.com) ranking within the game.

<p align="center"><img src="docs/assets/cistiertagger.png" width="100%"></p>

### Configuration
Customize the display by opening the mod’s configuration via `ModMenu` or by using the `/cistier` command.

<p align="center"><img src="docs/assets/config.png" width="100%"></p>
<p align="center">
  <img src="docs/assets/config_gamemodes.png" width="49%">
  <img src="docs/assets/config_priority.png" width="49%">
</p>

### Leaderboard & Profiles
The mod also allows you to search for players on the leaderboard using the search field in the `/cistier` search menu or to view a specific player’s profile using the command `/cistier <nickname>`.

<p align="center">
  <img src="docs/assets/search.png" width="49%">
  <img src="docs/assets/profile.png" width="49%">
</p>

### Features

- Tier badge in the nametag, tab list and chat, each toggled independently.
- Shown tier picked by a gamemode priority order, or the player’s highest tier across modes.
- Toggle switches for each game mode from the [cistiers.com](https://cistiers.com) ranking.
- Search screen with preview - `/cistier <nickname>` opens the full profile (rank, points, levels, history).
- Servers can hide the badges over a plugin message - see [restriction.md](docs/restriction.md).

### Build

All supported Minecraft versions share one branch through [Stonecutter](https://stonecutter.kikugie.dev/).
Targets and dependency versions are defined in [stonecutter.properties.toml](stonecutter.properties.toml).

Build all versions:
```sh
./gradlew clean build
```

Build one version or several selected versions:
```sh
./gradlew :26.2:build
./gradlew :1.20.1:build :26.2:build
```

Run a client for a specific version:
```sh
./gradlew :26.2:runClient
```

Run clients for multiple versions in parallel:
```sh
./gradlew :1.20.1:runClient :26.2:runClient --parallel
```

Built JARs are in `versions/<minecraft>/build/libs/`, client data is in `runs/<minecraft>/`.

### License
This project is distributed under the [GPL-3.0 license](license).
