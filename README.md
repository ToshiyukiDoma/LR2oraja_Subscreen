# LR2oraja Subscreen

A companion app for **lr2oraja** providing a dedicated touchscreen display for song search, live game controls, profile management, and custom visuals without modifying core game files[cite: 1].

<img width="400" alt="home" src="https://github.com/user-attachments/assets/2efebb0e-a355-4b76-a53a-86cb887c835d" />
<img width="400" alt="gameplay-settings" src="https://github.com/user-attachments/assets/6d6ab1e0-5d8b-42df-b4e4-aaa6701f922f" />
<img width="400" alt="song-info-long" src="https://github.com/user-attachments/assets/c5622708-8dd3-47e6-8652-d539044b6ca7" />
<img width="400" alt="keyboard-large" src="https://github.com/user-attachments/assets/387b3cff-8369-4f13-8cc6-f9347cb6aa2e" />



## Quick Start

1. Extract this package into your `beatoraja` folder beside `beatoraja.exe`[cite: 1].
2. Run **`Launch LR2Touch.cmd`**, then launch the game as usual[cite: 1].
3. Open **Style** on the subscreen to assign it to your desired display[cite: 1].

---

## Core Features

* **Touch Keyboard & Search:** Integrated Japanese input supporting English, Kana, Romaji, and offline word-based Kanji conversion[cite: 1].
* **Live Game Controls:** Quick navigation, immediate Escape/Back triggers, and Master/Keysound/BGM volume sliders[cite: 1].
* **Play & Modifiers:** Real-time modifier toggles and fine-tuned sliders (±1/±10) for note duration with automatic green-number conversion[cite: 1].
* **Profile & IR Management:** Easily add, rename, or switch player profiles with full IR login support and real-time streaming text export (`current-profile.txt`)[cite: 1].
* **Skin Customization:** Draft and apply skin category options, sound sets, and custom settings directly from the song selection screen[cite: 1].
* **Style & Backgrounds:** Per-profile themes, image backgrounds, HEX color customization, and integrated video optimization[cite: 1].
* **Active Match Info:** Displays centered song metadata, scrolling titles, and loading progress during play while automatically pausing background media[cite: 1].

---

## Personalization & Folders

Place your custom assets inside the `Subscreen\` directory[cite: 1]:

* `message.txt` – UTF-8 home screen status messages (one per line, chosen randomly)[cite: 1].
* `backgrounds\` – Custom PNG, JPG, BMP, or static GIF background images[cite: 1].
* `backgrounds\import\` – Raw videos to be converted into optimized 1080p/30fps H.264 clips via **Style → Background Gallery**[cite: 1].

---

## Recovery & Uninstallation

* **Backups:** Automatic backups are created in `Subscreen\backups` whenever profile or skin changes are applied[cite: 1].
* **Uninstallation:** Close the game, remove the launcher scripts and the `Subscreen` folder, then launch `beatoraja.exe` normally[cite: 1]. Previously saved profile and game settings will remain intact[cite: 1].

---

## Source & Licenses

* **Build:** Source code and tests are located in `Subscreen\source`[cite: 1]. Run `Build.ps1 -GameDirectory '<path-to-beatoraja>'` to recompile[cite: 1].
* **Licensing:** Distributed under GPL-3.0-or-later[cite: 1]. Includes components under GPL-2.0-or-later (SKK-JISYO.L)[cite: 1], EPL-2.0 (ECJ 3.39.0)[cite: 1], and FFmpeg[cite: 1].
