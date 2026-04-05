# ❄️ SBM: Snowballmatch

> [!IMPORTANT]
> This project is still a **Work in Progress**. This is the first fully usable version, but refinement is ongoing. (See *Building Progress* at the bottom).

> 💡 **Note:** This Project is being developed for **Flavortown** by **HackClub**.

## 📖 What is it About?

The **Snowballmatch Plugin (SBM)** is inspired by a classic game played in school. Two teams fight in rounds using tactics and snowballs until only one player (or team) remains. 

It is specifically designed for **hosted competitions**, meaning it’s built to be managed by a live host or moderator rather than running on a 24/7 automated loop. This makes it the perfect tool for organized community events and tournaments.


## 📖 Quick Start Guide

### 📋 Prerequisites

* **Docker** and **Docker Compose** must be installed on your system.
* The **"Essential" folder** containing the worlds and plugins must be ready.

### Phase 1: Environment Setup (Docker)
1.  **Modify Configuration:** Open the `docker-compose.yml` file in the root directory.
2.  **Security Check:** Locate the environment variables for your database and RCON. Replace the default passwords with secure, unique strings.
3.  **Deployment:** Open your terminal in the project folder and run:
    ```bash
    docker-compose up -d
    ```
    *This command starts the Minecraft server and the PostgreSQL database in the background.*

### Phase 2: Manual File Installation
1.  **Download Essentials:** Get the "Essential" folder containing the worlds and required plugins.
2.  **World Setup:** Move the folders `SBM-Lobby` and `SBM-Arena` directly into your Minecraft server's root directory.
3.  **Plugin Setup:** Place the `SBM-Plugin.jar` and `ProtocolLib.jar` into the `/plugins` folder.

### Phase 3: Server Configuration
1.  **Define Primary World:** Open `server.properties` and update the following line:
    ```properties
    level-name=SBM-Lobby
    ```
    *(Note: The SBM-Plugin will handle loading the `SBM-Arena` world automatically.)*
2.  **Initial Boot:** Restart the Minecraft server to initialize the plugin and generate the configuration files.

### Phase 4: Localization and Settings
* **Main Settings:** Edit `/plugins/SBM-Plugin/config.yml` to adjust core features.
* **Translations:** Navigate to the `/lang/` folder. Use `lang_en.yml` (English) or `lang_de.yml` (German) to modify in-game messages.

---

### 💻 Hardware Requirements

| Component | Minimum | Recommended |
| :--- | :--- | :--- |
| **RAM (Memory)** | 4 GB | 8 GB |
| **CPU** | 2 Cores (Modern) | 4 Cores (High Clock Speed) |
| **Disk Type** | HDD | SSD (Crucial for World Loading) |

* **Memory Usage:** Minecraft and the PostgreSQL database share resources. 8 GB of RAM allows the operating system and the server to run without swapping data to the disk.
* **Storage Speed:** An SSD is highly recommended. It significantly reduces "stuttering" when players join or move between the Lobby and the Arena.

## ⌨️ Commands

### ⏱️ Round Management

| Syntax | Description |
| :--- | :--- |
| `/game <start\|pause\|resume>` | Controls the flow of the match. |
| `/game reset` | Resets the round to its state before the start. |
| `/game gate [<team>]` | Locks or unlocks the team gates. |

### ❄️ Game Events & Server

| Syntax | Description |
| :--- | :--- |
| `/game bonus-snowball <team> [<amount>]` | Manually drops snowballs via the team's dropper. |
| `/game <open\|close>` | Opens or closes the server for players. |
| `/game open timer set <YYYY/MM/DD> <hh:mm:ss>` | Schedules a planned opening time. |
| `/game open timer <start\|stop>` | Controls the countdown for the planned opening. |

### 👥 Team & Map Management

| Syntax | Description |
| :--- | :--- |
| `/team add <player> [<team>]` | Adds a player to a team (auto-assigns if empty). |
| `/team remove <player>` | Removes a player from their current team. |
| `/map load <map>` | Loads a specific arena map. |
| `/map update` | Refreshes the build area behind the arena. |

### 📢 Alerts & Navigation

* **Global Alerts:** `/alert <normal\|important> <message...>`
* **Private Alerts:** `/alert private <receiver> <message...>`
* **Fast Travel:** `/hub`, `/lobby`, `/spawn`, `/game-server`, `/event-server`

## Hotbars

### Default Items

![Default Items](/pics/Default%20Inv.png)

These items are available in every hotbar:

1. **Language** Opens a menu to change your preferred language.

2. **Toggle Snow** Since high particle counts can cause lag, you can use this to turn the snow effect off.

---

### Lobby Hotbar

![Pic Lobby Hotbar](/pics/Lobby%20Inv.png)

1. **Game Server Join** This item either displays when the game server is joinable or acts as a direct button to join.

2. **Parkour** Teleports you to the start of the parkour course.

---

### Game Server Hotbar

![GameServerHotbarPic](/pics/Gameserver.png)

1. **Participate** Click this to be automatically assigned to a team.

2. **Overlooks** Opens a menu to pick one of three viewpoints and teleports you there.

3. **Spyglass** A standard spyglass. Use it to observe other players.

4. **Lobby** Teleports you back to the lobby.


---

## ❤️ About the Development

I first started this project in 2023, but back then, the code was disorganized and I didn't have the time to finish it. Thanks to HackClub's **Flavortown**, I rediscovered it. 

I initially planned to build a web dashboard, but the old groundwork needed a complete rework. I ended up scaling the arena, adding new features, and implementing a multi-language system (moving from German to English).

With the (personal) deadline approaching, the custom web app isn't quite ready. However, since all data is in **Redis**, I created a functional demo interface using **Tooljet**. In the future, I plan to release both a lightweight version and a fully integrated dashboard version.

## 🤖 Usage of AI
AI was used as a support tool for research, server management, and brainstorming. No code was written directly by AI, though some logic was inspired by AI suggestions. It served as a technical collaborator, not a writer.

---

## 🏗️ Building Progress
* [✅] Core Snowball Mechanics
* [✅] Redis Integration
* [✅] Multi-language Support (EN/DE)
* [ __ ] Custom Web Dashboard (Standalone Version)
* [ __ ] 100% Edge-Case Covering
* [ __ ] Security Audit for On-Server Dashboard