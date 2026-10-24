# Maid Buddy – Minecraft 26.3 (Fabric)

Ein kleiner, schwebender Pixel-Begleiter mit deinem Skin, der dir überallhin folgt.
**Rein clientseitig:** Es wird keine Entity erzeugt und nichts an den Server gesendet.
Nur du siehst den Begleiter, auch auf Multiplayer-Servern.

## Funktionen
- 3D-Mini-Modell (ca. 0,85 Blöcke gross) mit deinem 64×64-Skin inklusive Überzug-Ebenen
- Schwebt rechts hinter dir, mit sanftem Auf und Ab
- Weiche, federnde Verfolgung beim Laufen, Sprinten, Springen und Fliegen
- Holt auf, wenn er zurückfällt; ab 16 Blöcken teleportiert er sich zu dir
- Weicht Wänden aus (wechselt die Seite oder geht über deinen Kopf)
- Schaut dich an; Arme und Beine bewegen sich passend zur Bewegung
- Passt sich der Umgebungshelligkeit an (mit leichtem Eigenleuchten)
- In Ego- und Third-Person-Ansicht sichtbar
- Kein Angriff, kein Item-Aufheben, keine Kollision, keine Gameplay-Änderung

## Steuerung
- **B**: Begleiter ein-/ausblenden (änderbar unter Optionen → Steuerung → Verschiedenes)

## Installation
1. Fabric Loader 0.19.5 (oder neuer) für Minecraft 26.3 installieren
2. Fabric API für 26.3 in den `mods`-Ordner legen
3. `maidbuddy-1.0.0.jar` in den `mods`-Ordner legen

## Versionen
| Komponente | Version |
|---|---|
| Minecraft | 26.3 |
| Java | 25 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.164.0+26.3 |
| Fabric Loom | 1.18-SNAPSHOT (Plugin `net.fabricmc.fabric-loom`) |
| Gradle | 9.7.1 |

## Bauen

### Variante A: Automatisch mit GitHub
1. Kostenloses Konto auf github.com anlegen → **New repository** → beliebiger Name.
2. **„uploading an existing file“** anklicken und den *Inhalt* dieses Ordners hochladen
   (auch den versteckten Ordner `.github`; am einfachsten mit GitHub Desktop).
3. Im Reiter **Actions** startet „Maid Buddy bauen“ automatisch.
4. Nach ca. 3–5 Minuten: Lauf anklicken → unten unter **Artifacts** `maidbuddy-jar` herunterladen.
5. Die ZIP entpacken → `maidbuddy-1.0.0.jar` in den `mods`-Ordner.

Falls der Lauf rot wird: den Lauf öffnen, „Mod kompilieren“ aufklappen und die
Fehlermeldung kopieren.

### Variante B: Lokal
JDK 25 und Gradle 9.7.1 installieren, dann im Projektordner:
```
gradle build
```
Die JAR liegt danach in `build/libs/`.
In IntelliJ IDEA: Projekt öffnen, als Gradle-JVM JDK 25 wählen und den Gradle-Task `build` starten.

## Anpassen
Abstand, Höhe und Teleport-Distanz stehen oben in `BuddyState.java`,
die Grösse (`SCALE`) in `BuddyRenderer.java`.
Den Skin austauschen: `src/main/resources/assets/maidbuddy/textures/entity/maid_buddy.png`
(64×64-Skin im Slim-Layout).
