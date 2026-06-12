# InfoPlayer

Administrative player panel for Minecraft 1.21.1 and NeoForge 21.1.233.

## Features

- `/infoplayer` opens the panel for administrators with permission level 2 or higher.
- Shows online and previously seen players.
- Displays player skin, name, status, experience level, total experience, play time and advancement progress.
- Opens a detailed profile with health, food, game mode, dimension, coordinates and activity dates.
- Stores the latest offline snapshot in `<world>/infoplayer_players.json`.
- Checks permissions on the server for both the command and network requests.
- Includes Russian and English localization.

## Installation

Install the same mod JAR on the NeoForge 1.21.1 server and every administrator's client. Restart the server, grant the administrator operator permission level 2 or higher, then run:

```text
/infoplayer
```

## Build

Java 21 is required.

```powershell
.\gradlew.bat build
```

The finished JAR is written to `build/libs/infoplayer-1.0.0.jar`.

## Data

InfoPlayer begins collecting player history after it is installed. It cannot reconstruct exact last-seen times from before installation.

## License

All Rights Reserved. See [LICENSE](LICENSE).
