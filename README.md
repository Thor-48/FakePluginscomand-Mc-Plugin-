# FakePluginscomand

A lightweight Minecraft plugin for **1.21+** that hides your server's real plugins and protects against command-tree scanners by replacing `/pl` and `/plugins` with a customizable fake plugin list featuring interactive hover and click details.

[![Modrinth](https://img.shields.io/badge/Modrinth-Profile-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/user/Why_Luca)
[![Discord](https://img.shields.io/badge/Discord-Join%20Server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.com/invite/D8DtJF2Jye)
[![Ko-fi](https://img.shields.io/badge/Ko--fi-Support-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/why_luca)

---

## Features
* **Customizable Fake Plugin List:** Configure unlimited fake plugins in `config.yml` with custom names, versions, descriptions, websites, and authors.
* **Paper & Bukkit Grouping:** Optionally separate the fake plugin output into **Paper Plugins** and **Bukkit Plugins** lists.
* **Interactive Hover & Click Info:** Players can hover over or click plugin names in chat (or run `/fakeplugininfo <plugin>`) to view detailed plugin info.
* **Command-Tree Scanner Protection:** Hides the plugin's own namespace (`fakepluginscomand:...`), all namespaced commands (`essentials:home`, `luckperms:lp`, etc.), and configurable commands like `/plugins`, `/pl`, `/version`, `/ver`, and `/about` from unauthorized players.
* **Stealth Command Handling:** Intercepts commands via events so no extra commands appear in the command tree or tab-completion.
* **Full MiniMessage Support:** Customize all messages, headers, separators, and info lines using Adventure MiniMessage formatting.

## Commands

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/pl`, `/plugins`, `/bukkit:pl`, `/bukkit:plugins` | Displays the fake plugin list to regular players (admins see the real list by default). | None *(Admin bypass: `fakeplugins.admin`)* |
| `/fakeplugininfo <plugin>` | Shows detailed info for a configured fake plugin. | None *(Configurable in `config.yml`)* |
| `/rpl`, `/realplugins` | Always displays the real server plugins. | `fakeplugins.admin` |
| `/realplugininfo <plugin>` | Displays real plugin information. | `fakeplugins.admin` |
| `/fakeplugins reload` | Reloads the `config.yml` configuration. | `fakeplugins.admin` |

## Permissions

| Permission | Description | Default |
| :--- | :--- | :--- |
| `fakeplugins.admin` | Allows viewing the real plugin list, real plugin info, hidden command-tree entries, and reloading the configuration. | `op` |

## Installation
1. Download the latest `.jar` file from [Modrinth](https://modrinth.com/user/Why_Luca).
2. Place the file into your server's `plugins` folder (requires **Paper 1.21+**).
3. Restart your server to generate the default `config.yml`.
4. Customize your fake plugins and messages in `plugins/FakePluginscomand/config.yml` and run `/fakeplugins reload`.

## Links & Support
* 🌐 **Modrinth:** [modrinth.com/user/Why_Luca](https://modrinth.com/user/Why_Luca)
* 💬 **Discord:** [Join the Discord Server](https://discord.com/invite/D8DtJF2Jye)
* ☕ **Support:** [Buy me a coffee on Ko-fi](https://ko-fi.com/why_luca)

## License
This project is licensed under the **FakePluginscomand License** – see the `LICENSE.md` file in this repository for details.

*Copyright © 2026 Thor_48, Why_Luca, and Luca The Fox*
