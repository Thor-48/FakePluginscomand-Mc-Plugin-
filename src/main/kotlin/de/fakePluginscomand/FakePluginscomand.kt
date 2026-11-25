package de.fakePluginscomand

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerCommandSendEvent
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin

class FakePluginscomand : JavaPlugin(), Listener {

    /** Data of a fake plugin (loaded from config.yml). */
    data class FakePluginInfo(
        val name: String,
        val version: String,
        val description: String,
        val website: String?,
        val authors: String,
        val isPaperPlugin: Boolean
    )

    /** Common entry used for list output (works for fake and real plugins). */
    private data class ListEntry(
        val name: String,
        val isPaper: Boolean,
        val info: Component
    )

    private val mm = MiniMessage.miniMessage()

    // ---- Values loaded from the config (filled in loadSettings()) ----
    private var adminPermission = "fakeplugins.admin"
    private var fakePlugins = listOf<FakePluginInfo>()

    private var hideOwnNamespace = true
    private var hideNamespacedCommands = true
    private var hiddenCommands = setOf<String>()

    private var listCommands = setOf<String>()
    private var realListCommands = setOf<String>()
    private var versionCommands = setOf<String>()
    private var adminsSeeRealOnList = true

    private var groupByType = true
    private var hoverInfo = true
    private var clickInfo = true

    override fun onEnable() {
        saveDefaultConfig()
        loadSettings()
        server.pluginManager.registerEvents(this, this)
        logger.info("FakePluginscomand enabled.")
    }

    /** Reads the whole config again (also used by "/fakeplugins reload"). */
    private fun loadSettings() {
        reloadConfig()
        val c = config

        adminPermission = c.getString("settings.admin-permission", "fakeplugins.admin")!!

        hideOwnNamespace = c.getBoolean("command-tree.hide-own-namespace", true)
        hideNamespacedCommands = c.getBoolean("command-tree.hide-namespaced-commands", true)
        hiddenCommands = c.getStringList("command-tree.hidden-commands")
            .map { it.lowercase() }.toSet()

        listCommands = c.getStringList("commands.plugin-list-commands")
            .map { it.lowercase() }.toSet()
        realListCommands = c.getStringList("commands.real-list-commands")
            .map { it.lowercase() }.toSet()
        versionCommands = c.getStringList("commands.version-commands")
            .map { it.lowercase() }.toSet()
        adminsSeeRealOnList = c.getBoolean("commands.admins-see-real-plugins-on-list", true)

        groupByType = c.getBoolean("display.group-by-type", true)
        hoverInfo = c.getBoolean("display.hover-info", true)
        clickInfo = c.getBoolean("display.click-info", true)

        // Read the fake plugins as a list (the order is kept)
        fakePlugins = c.getMapList("fake-plugins").mapNotNull { map ->
            val name = map["name"]?.toString()?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            FakePluginInfo(
                name = name,
                version = map["version"]?.toString() ?: "1.0.0",
                description = map["description"]?.toString() ?: "",
                website = map["website"]?.toString()?.takeIf { it.isNotBlank() },
                authors = map["authors"]?.toString() ?: "Unknown",
                isPaperPlugin = map["paper-plugin"] as? Boolean ?: false
            )
        }
    }

    // ============================================================
    // Message helpers (MiniMessage strings from the config)
    // ============================================================

    private fun msg(key: String, vararg resolvers: TagResolver): Component {
        val raw = config.getString("messages.$key", "") ?: ""
        return mm.deserialize(raw, TagResolver.resolver(*resolvers))
    }

    private fun isAdmin(player: Player) = player.hasPermission(adminPermission)

    /**
     * Handles a player without permission.
     * If "messages.no-permission" is empty, the event is NOT cancelled, so the
     * server answers with its normal "Unknown command" message. That makes the
     * admin commands look like they do not exist at all.
     */
    private fun deny(event: PlayerCommandPreprocessEvent) {
        val message = config.getString("messages.no-permission", "") ?: ""
        if (message.isBlank()) return
        event.isCancelled = true
        event.player.sendMessage(msg("no-permission"))
    }

    // ============================================================
    // Command tree filtering
    // ============================================================
    //
    // This is the core of the protection against client-side scanners.
    // Normal players do not receive plugin-related commands or namespaces
    // in the command tree their client sees. Players with the admin
    // permission get everything as usual.

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onCommandSend(event: PlayerCommandSendEvent) {
        if (isAdmin(event.player)) return

        val ownNamespace = name.lowercase()

        event.commands.removeIf { command ->
            val lower = command.lowercase()
            when {
                // Own namespace (e.g. "fakepluginscomand:...")
                hideOwnNamespace && lower.startsWith("$ownNamespace:") -> true
                // Every namespaced command (e.g. "essentials:home", "luckperms:lp").
                // These entries would give a scanner direct hints about installed plugins.
                hideNamespacedCommands && lower.contains(":") -> true
                // The default discovery commands (plugins, version, about, ...)
                lower in hiddenCommands -> true
                else -> false
            }
        }
    }

    // ============================================================
    // Command interception (/pl, /plugins, /rpl, ...)
    // ============================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        val player = event.player

        val commandLine = event.message.removePrefix("/").trim()
        val command = commandLine
            .split("\\s+".toRegex())
            .firstOrNull()
            ?.lowercase()
            ?: return

        when {
            // /pl, /plugins, bukkit:pl, ... (configurable)
            command in listCommands -> {
                event.isCancelled = true
                if (adminsSeeRealOnList && isAdmin(player)) {
                    sendList(player, realEntries())
                } else {
                    sendList(player, fakeEntries())
                }
            }

            // /rpl, /realplugins (admins only)
            command in realListCommands -> {
                if (!isAdmin(player)) return deny(event)
                event.isCancelled = true
                sendList(player, realEntries())
            }

            // /version <plugin> (also /ver, /about, bukkit:version, ...)
            // This replaces the plugin lookup of the normal version command, so
            // clicking a plugin in the list shows a normal looking "/version <plugin>"
            // in the confirmation window instead of a custom command.
            command in versionCommands -> {
                // Admins use the normal server command untouched (real plugin info)
                if (isAdmin(player)) return

                val pluginName = commandLine.substringAfter(" ", "").trim()
                // Without an argument the normal version output (server version) is used
                if (pluginName.isEmpty()) return

                // The normal command never runs for players when a plugin name is given
                event.isCancelled = true

                val plugin = fakePlugins.firstOrNull {
                    it.name.equals(pluginName, ignoreCase = true)
                }
                if (plugin == null) {
                    player.sendMessage(msg("plugin-not-found"))
                } else {
                    player.sendMessage(buildFakeInfo(plugin))
                }
            }

            // /fakeplugins reload (admins only)
            // Handled here instead of registering a real command, so nothing
            // new shows up in the command tree or in tab completion.
            command == "fakeplugins" -> {
                if (!isAdmin(player)) return deny(event)
                event.isCancelled = true

                if (commandLine.split("\\s+".toRegex()).getOrNull(1).equals("reload", true)) {
                    loadSettings()
                    player.sendMessage(msg("reloaded"))
                } else {
                    player.sendMessage(msg("usage-reload"))
                }
            }
        }
    }

    // ============================================================
    // List output
    // ============================================================

    private fun fakeEntries(): List<ListEntry> = fakePlugins.map {
        ListEntry(it.name, it.isPaperPlugin, buildFakeInfo(it))
    }

    private fun realEntries(): List<ListEntry> =
        Bukkit.getPluginManager().plugins
            .filter { it.isEnabled }
            .map {
                ListEntry(it.name, isPaperPlugin(it), buildRealInfo(it))
            }

    /**
     * Roughly detects whether a plugin is a Paper plugin (paper-plugin.yml).
     * This is a heuristic based on the class loader name and is only used
     * for the admin view of the real plugins.
     */
    private fun isPaperPlugin(plugin: Plugin): Boolean =
        plugin.javaClass.classLoader?.javaClass?.name
            ?.contains("PaperPluginClassLoader", ignoreCase = true) == true

    private fun sendList(player: Player, entries: List<ListEntry>) {
        player.sendMessage(
            msg("list-header", Placeholder.unparsed("count", entries.size.toString()))
        )

        if (!groupByType) {
            // One single list without the Paper/Bukkit split
            sendListLine(player, entries)
            return
        }

        val paper = entries.filter { it.isPaper }
        val bukkit = entries.filter { !it.isPaper }

        player.sendMessage(
            msg("list-header-paper", Placeholder.unparsed("count", paper.size.toString()))
        )
        if (paper.isNotEmpty()) sendListLine(player, paper)

        player.sendMessage(
            msg("list-header-bukkit", Placeholder.unparsed("count", bukkit.size.toString()))
        )
        if (bukkit.isNotEmpty()) sendListLine(player, bukkit)
    }

    private fun sendListLine(player: Player, entries: List<ListEntry>) {
        var line = Component.text().append(msg("list-prefix"))

        entries.forEachIndexed { index, entry ->
            var part = msg("list-plugin", Placeholder.unparsed("name", entry.name))

            if (hoverInfo) part = part.hoverEvent(HoverEvent.showText(entry.info))
            if (clickInfo) part = part.clickEvent(ClickEvent.runCommand("/version ${entry.name}"))

            line = line.append(part)
            if (index < entries.lastIndex) line = line.append(msg("list-separator"))
        }

        player.sendMessage(line.build())
    }

    // ============================================================
    // Info output (hover / click)
    // ============================================================

    private fun buildInfo(
        pluginName: String,
        version: String,
        description: String,
        website: String?,
        authors: String
    ): Component {
        val resolvers = arrayOf(
            Placeholder.unparsed("name", pluginName),
            Placeholder.unparsed("version", version),
            Placeholder.unparsed("description", description),
            Placeholder.unparsed("website", website ?: ""),
            Placeholder.unparsed("authors", authors)
        )

        var result = msg("info-title", *resolvers)
            .append(Component.newline())
            .append(msg("info-description", *resolvers))

        if (!website.isNullOrBlank()) {
            result = result.append(Component.newline()).append(msg("info-website", *resolvers))
        }
        return result.append(Component.newline()).append(msg("info-authors", *resolvers))
    }

    private fun buildFakeInfo(p: FakePluginInfo): Component =
        buildInfo(p.name, p.version, p.description, p.website, p.authors)

    private fun buildRealInfo(plugin: Plugin): Component {
        val meta = plugin.pluginMeta
        val authors = meta.authors.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Unknown"
        return buildInfo(
            plugin.name,
            meta.version,
            meta.description ?: "No description available.",
            meta.website,
            authors
        )
    }
}