# ❄️ SBM: Snowballmatch

> [!IMPORTANT]
> This project is still a **Work in Progress**. This is the first fully usable version, but refinement is ongoing. (See *Building Progress* at the bottom).

> 💡 **Note:** This Project is being developed for **Flavortown** by **HackClub**.

## 📖 What is it About?

The **Snowballmatch Plugin (SBM)** is inspired by a classic game played in school. Two teams fight in rounds using tactics and snowballs until only one player (or team) remains. 

It is specifically designed for **hosted competitions**, meaning it’s built to be managed by a live host or moderator rather than running on a 24/7 automated loop. This makes it the perfect tool for organized community events and tournaments.

## 🚀 Quick Setup

The easiest way to get SBM running is via Docker.

1.  Locate the `docker-compose.yml` file in the project root.
2.  Run the following command in your terminal:
    ```bash
    docker-compose up -d
    ```
3.  Everything should be set up automatically.
4.  **Important:** Please replace the default passwords in the file before deploying to ensure your server's security.

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

---

## 🛠️ Technical & Fair Play Rules

### Disconnections
* **Proxy Hearts:** If a player leaves, their hearts are transferred to a placeholder so the team's total heart count remains accurate.
* **Rejoining:** Players reclaim their hearts upon rejoining.
* **Host Authority:** If a player is gone too long, the Host may **disqualify** them. Matches may be paused or restarted if team balance becomes unfair.

### Disqualification
* **Cheats:** Use of external cheat systems or prohibited clients is strictly forbidden.
* **Bugs:** All bugs must be reported. Exploiting bugs for an advantage results in disqualification.
* **Decisions:** Hosts have the final say on all disqualifications based on the "spirit of the game."

---

## ❤️ About the Development
I first started this project in 2023, but back then, the code was disorganized and I didn't have the time to finish it. Thanks to HackClub's **Flavortown**, I rediscovered it. 

I initially planned to build a web dashboard, but the old groundwork needed a complete rework. I ended up scaling the arena, adding new features, and implementing a multi-language system (moving from German to English).

With the deadline approaching, the custom web app isn't quite ready. However, since all data is in **Redis**, I created a functional demo interface using **Tooljet**. In the future, I plan to release both a lightweight version and a fully integrated dashboard version.

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