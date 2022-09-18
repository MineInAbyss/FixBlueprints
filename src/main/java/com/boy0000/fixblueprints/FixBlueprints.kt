package com.boy0000.fixblueprints

import com.ticxo.modelengine.api.ModelEngineAPI
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class FixBluePrintCommands : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (label == "fixblueprints")
            fixBlueprints.fixBluePrints()
        return true
    }
}

val fixBlueprints: FixBlueprints by lazy { Bukkit.getPluginManager().getPlugin("FixBlueprints") as FixBlueprints }
val modelengine: ModelEngineAPI by lazy { Bukkit.getPluginManager().getPlugin("ModelEngine") as ModelEngineAPI }

class FixBlueprints : JavaPlugin() {
    override fun onEnable() {
        // Plugin startup logic
        getCommand("fixblueprints")?.setExecutor(FixBluePrintCommands())
        //Don't run every startup as its very unnecessary
        //fixBluePrints()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    private val blueprintDir = File(this.dataFolder.parent, "/ModelEngine/blueprints/")
    private val prefix = "<red>[FixBlueprints] "
    private fun log(msg: String) {
        Bukkit.getConsoleSender().sendRichMessage(prefix + msg)
    }

    fun fixBluePrints() {
        if (blueprintDir.exists()) {
            log("<green>Scanning directories...")
            scanDirectory(blueprintDir)
            if (!modelengine.isEnabled) return
            modelengine.generator.importModels()
            scanJSON(blueprintDir)
            log("<green>Finished fixing blueprints!")
        }
    }

    // Recursive function to read all files down until it gets a BBModel file
    private fun scanDirectory(blueprintDir: File) {
        blueprintDir.listFiles()?.forEach blueprint@{ blueprintFile ->
            if (blueprintFile.name.endsWith(".bbmodel")) {
                log("<green>Found BBModel file: <gold>${blueprintFile.name}")
                // Quick fix all mob paths and npc paths
                blueprintFile.correctInitialTexturePaths()
                blueprintFile.readBBModel()
            } else scanDirectory(blueprintFile)
        }
    }

    private fun scanJSON(blueprintDir: File) {
        blueprintDir.listFiles()?.forEach blueprint@{ blueprintFile ->
            if (blueprintFile.name.endsWith(".bbmodel"))
                blueprintFile.readJSON()
            else scanJSON(blueprintFile)
        }
    }

    private fun File.readBBModel() {
        var blueprint = readText().replace("\\\\", "/")
        val paths = blueprint.split("{\"path\":\"").drop(1).map { it.substringBefore(",\"id\"") }

        paths.forEach { s ->
            val texturePath = s.substringBefore("\",\"name\"")
            val (newNamespace, newFolder, newTexture) =
                texturePath.getNewProperties().takeIf { it.toList().joinToString("").isNotBlank() } ?: return@forEach
            val template =
                "{\"path\":\"$texturePath\",\"name\":\"$newTexture\",\"folder\":\"$newFolder\",\"namespace\":\"$newNamespace\""

            blueprint = blueprint
                .replace(",\"relative_path\":\"$s", ",\"relative_path\":\"$texturePath")
                .replace("{\"path\":\"$s", template)
                .replace("\"saved\":false", "\"saved\":true")
        }
        writeText(blueprint)
        readJSON()
        log("<green>Fixed <italic>$name\n")
    }

    private fun File.correctInitialTexturePaths() {
        var blueprint = readText().replace("\\\\", "/")
        val parent = parent.replace("\\\\", "/")
        val assets = "assets/minecraft/textures"
        when {
            parent.endsWith("mobs") -> {
                log("<yellow>Correcting initial texture paths...")
                blueprint = blueprint.replace("$assets/creatures", "assets/mineinabyss/textures/mobs")
            }

            parent.endsWith("npcs") -> {
                log("<yellow>Correcting initial texture paths...")
                blueprint = blueprint
                    .replace("$assets/characters", "assets/mineinabyss/textures/characters")
                    .replace("$assets/creatures", "assets/mineinabyss/textures/characters")
            }

            parent.endsWith("relics") -> {
                log("<yellow>Correcting initial texture paths...")
                blueprint = blueprint
                    .replace("$assets/items", "assets/mineinabyss/textures/relics")
                    .replace("$assets/relics", "assets/mineinabyss/textures/relics")

            }

            parent.endsWith("blocks") -> {
                log("<yellow>Correcting initial texture paths...")
                blueprint = blueprint
                    .replace("$assets/block", "assets/mineinabyss/textures/blocks")
                    .replace("$assets/block/modelengine", "assets/mineinabyss/textures/blocks")
            }
        }
        writeText(blueprint)
    }

    // Fixes instances where the texture is in assets/namespace/textures/file.png which makes json malformatted
    private fun File.readJSON() {
        readText().replace("\\\\", "/").split("{\"path\":\"")
            .drop(1).map { it.substringBefore(",\"id\"") }
            .forEach { s ->
                val texturePath = s.substringBefore("\",\"name\"")
                val (namespace, folder, texture) =
                    texturePath.getNewProperties().takeIf { it.toList().joinToString("").isNotBlank() }
                        ?: return@forEach

                val jsonDir = File(
                    dataFolder.parent,
                    "/ModelEngine/resource pack/assets/${modelengine.generator.namespace}/models/$nameWithoutExtension"
                )
                if (!jsonDir.exists() || folder.isNotBlank() || texture.isBlank() || namespace.isBlank()) return@forEach

                jsonDir.listFiles()?.filter { it.name.endsWith(".json") }?.forEach { jsonFile ->
                    log("<green>Fixing JSON file: <gold>${jsonFile.name}</gold> for <gold>$name")
                    jsonFile.writeText(
                        jsonFile.readText()
                            .replace("$namespace:/$nameWithoutExtension", "$namespace:$nameWithoutExtension")
                    )
                }
            }
    }

    private fun String.getNewProperties(): Triple<String, String, String> {
        val newNamespace = substringAfter("assets/").substringBefore("/textures")
        val newTexture = substringAfterLast("/").takeIf { it.endsWith(".png") } ?: return Triple("", "", "")
        val newPath = substringAfter("assets/$newNamespace/textures/").substringBefore("/$newTexture")
            .takeIf { !it.endsWith(".png") } ?: ""
        return Triple(newNamespace, newPath, newTexture)
    }
}
