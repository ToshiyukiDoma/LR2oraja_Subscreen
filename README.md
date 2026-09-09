# LR2oraja Subscreen
A plugin that makes sense if you have a "Lightning Model" setup on BMS/IIDX station.

Only tested with: https://github.com/seraxis/lr2oraja-endlessdream
I cannot guarantee if it will work with other LR2oraja forks or variants.

## Samples:

<img width="400" alt="01-home" src="https://github.com/user-attachments/assets/80838d54-6cf5-48e7-a5d5-940536dfe992" />
<img width="400" alt="11-concentration" src="https://github.com/user-attachments/assets/777ef7bb-7b6d-49f0-95a3-4ba55d5cbec9" />
<img width="400" alt="10-chart-loading" src="https://github.com/user-attachments/assets/1c3e006c-8184-47ca-a337-e093572f7551" />
<img width="400" alt="09-skins" src="https://github.com/user-attachments/assets/050bba2a-65bf-412c-bb7d-da30fc29dde4" />
<img width="400" alt="08-style" src="https://github.com/user-attachments/assets/d8c5111e-e361-4cda-b1d8-a6bd2d5376d2" />
<img width="400" alt="07-profiles" src="https://github.com/user-attachments/assets/7a2adf52-8fce-4793-83cb-e9d1c6b88e42" />
<img width="400" alt="05-play-settings" src="https://github.com/user-attachments/assets/dd9908df-ae69-4529-a8f5-1b30c5ce23b1" />
<img width="400" alt="04-navigation" src="https://github.com/user-attachments/assets/f3a4bc84-57cd-45de-ad65-6ce02a5abcc8" />

## Build Instructions

Production Java source and the build tools are in `Subscreen/source`. The Windows runtime is distributed separately.

Run in PowerShell:

```powershell
& .\Subscreen\source\Build.ps1 -GameDirectory 'D:\beatoraja0.8.8-jre-win64'
```

Requires the compatible installed game, its bundled Java runtime, and `beatoraja.jar`. The included ECJ compiler targets Java 17. The build writes `Subscreen/lr2touch.jar`; copy that JAR into the Windows package. No game files are modified by this build.

`AudioHooks.java` adds temporary in-process menu-audio gain and input/state hooks through the Java agent. It does not alter files on disk. `AudioControl.java` keeps saved volume settings unchanged. Backgrounds use an optional local/PATH FFmpeg decoder; conversion is not included.

Validation covered installed-class bytecode verification, UI rendering, gameplay option guards, profile/skin fixtures, idle fade/restoration, bounded audio tracking and FFmpeg playback pause/resume. Live-game performance and physical touch are not established by these checks.

Addon GPL-3.0-or-later; ASM BSD-3-Clause. ECJ 3.39.0 is EPL-2.0: see `Subscreen/source/compiler-license.html`; corresponding source: https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.39.0/ecj-3.39.0-sources.jar . ASM corresponding source is included. Game dependencies are not bundled.
