# The Watchers (Forge 1.20.1)

Invisible haunted zones are hidden inside ordinary terrain. They have no border and ignore biomes.
Stay inside one and, after 10 seconds, a track starts, fog rolls in and the first watcher appears
20-30 blocks away, staring at you. Another arrives every 20 seconds. The fifth appears right in front of
you, and a second later you die. Leave the zone and it all fades away.

## Build the mod

You need **Java 17** (a newer JDK is fine to run Gradle; the build downloads Java 17 for you if needed)
and an internet connection the first time.

```
cd watchers_mod
./gradlew build          # Windows: gradlew.bat build
```

The finished mod is `build/libs/watchers-1.0.0.jar`. The first build downloads Minecraft and Forge and
can take a few minutes.

To try it straight from the project: `./gradlew runClient`.

## Install

Put the jar in the `mods` folder of **Minecraft 1.20.1 with Forge 47.x**. On a server it must be installed
on the server **and** on every player's game, because it adds an entity, a sound and a custom network message.

## Try it quickly (operators)

```
/watchers nearest        where the closest haunted zone is
/watchers tp             teleport to the centre of it
/watchers info           are you inside a zone, how much exposure you have, how many watchers
/watchers skip 80        jump forward 80 seconds (only works inside a zone)
/watchers reset          clear the haunting
```

Creative and spectator players are never haunted (see the config), so test in survival.

## What happens, in order

| Time inside a zone | What happens |
|---|---|
| 0 - 10s | Nothing. |
| 10s | The track starts, fog and darkness begin, watcher #1 appears. |
| 30s / 50s / 70s | Watchers #2, #3, #4 appear. |
| 90s | Watcher #5 appears about 4 blocks in front of you and your view turns to its face. |
| 91s | You die. A totem of undying cannot save you. Death message: "<name> was watched to death". |

Leaving the zone: the track and fog fade out, and the time you have built up drains at twice the speed it
built up, so stepping over the edge for a moment does not reset everything. Watchers that no longer match
your time leave when you are not looking at them.

The track is 98 seconds long, so it ends right around when you die.

## Things worth knowing

- Watchers are personal. Only the player they belong to can see them, so two players in the same zone each
  have their own watchers.
- Watchers cannot be hurt, hit, pushed or targeted. `/kill @e[type=watchers:watcher]` removes them.
- They are never saved to disk.
- Zones are the same every time for a given world seed. Nothing is stored. No zone is active within 300 blocks
  of the world spawn.
- Works on Peaceful too.

## Settings

`config/watchers-common.toml` (gameplay; on a server this is the server's copy) lets you change: which
dimensions have zones, zone size, how common they are, the time before the first clue, the time between
watchers, the number of watchers, the delay before death, how fast exposure drains, how far watchers
appear, and whether creative players are exempt.

`config/watchers-client.toml` (each player's own game) lets you change: fog distance (default 45 blocks, kept
above the watcher spawn distance so you can still see them), how dark the screen gets, fade times, track
volume, and whether vanilla music is silenced.

## Swapping the art and sound

- Face: `src/main/resources/assets/watchers/textures/entity/watcher.png` is 2048x2048. The face sits in a 128x224 area of it.
  `art/watcher_face.png` is that face on its own.
- Track: `src/main/resources/assets/watchers/sounds/clue_track.ogg` (must be Ogg Vorbis).
- Model: `src/main/java/com/example/watchers/client/WatcherModel.java`.
