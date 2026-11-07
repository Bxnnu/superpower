# Superpowers (Fabric, Minecraft Java 1.21.1)

Pick a power and use it with keybinds. Powers are saved per player with the world.

## Play
Install **Fabric Loader** and **Fabric API** for 1.21.1, then put the built jar in your `mods` folder.

| Key / command | What it does |
|---|---|
| `G` | Open the power menu |
| `R` | Fire your active power (Laser Eyes, Fireball) |
| `/power <name>` | Choose a power by name (speed, jump, flight, laser, fireball, strength, none) |
| Flight | Double-tap jump to fly like creative mode, much faster |
| Super Jump | Sneak in the air to glide |
| Super Strength | Land from 4+ blocks up for a ground slam |

Keys can be changed under Options > Controls > Superpowers.
Fireballs don't break blocks by default. Set `FIREBALL_BREAKS_BLOCKS = true` in `Superpowers.java` to change that.

## Build
Requires JDK 21.

    ./gradlew build        (Windows: gradlew.bat build)

The mod jar is `build/libs/superpowers-1.0.0.jar` (not the `-sources` jar).
If Gradle can't find a version, open `gradle.properties` and copy the current 1.21.1 values from https://fabricmc.net/develop

To run a test game: `./gradlew runClient`
