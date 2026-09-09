# LR2oraja Subscreen 1.0 — source

Production Java source and the build tools are in `Subscreen/source`. The Windows runtime is distributed separately.

Run in PowerShell:

```powershell
& .\Subscreen\source\Build.ps1 -GameDirectory 'D:\beatoraja0.8.8-jre-win64'
```

Requires the compatible installed game, its bundled Java runtime, and `beatoraja.jar`. The included ECJ compiler targets Java 17. The build writes `Subscreen/lr2touch.jar`; copy that JAR into the Windows package. No game files are modified by this build.

`AudioHooks.java` adds temporary in-process menu-audio gain and input/state hooks through the Java agent. It does not alter files on disk. `AudioControl.java` keeps saved volume settings unchanged. Backgrounds use an optional local/PATH FFmpeg decoder; conversion is not included.

Validation covered installed-class bytecode verification, UI rendering, gameplay option guards, profile/skin fixtures, idle fade/restoration, bounded audio tracking and FFmpeg playback pause/resume. Live-game performance and physical touch are not established by these checks.

Addon GPL-3.0-or-later; ASM BSD-3-Clause. ECJ 3.39.0 is EPL-2.0: see `Subscreen/source/compiler-license.html`; corresponding source: https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.39.0/ecj-3.39.0-sources.jar . ASM corresponding source is included. Game dependencies are not bundled.
