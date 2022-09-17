package com.boy0000.fixblueprints

import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class FixBlueprints : JavaPlugin() {
    override fun onEnable() {
        // Plugin startup logic
        fixBluePrints()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    private val blueprintDir = File(this.dataFolder.parent, "/ModelEngine/blueprints/")
    private fun fixBluePrints() {
        if (blueprintDir.exists())
            scanDirectory(blueprintDir)
    }

    // Recursive function to read all files down until it gets a BBModel file
    private fun scanDirectory(blueprintDir: File) {
        blueprintDir.listFiles()?.forEach blueprint@{ blueprintFile ->
            if (blueprintFile.name.endsWith(".bbmodel")) {
                // Quick fix all mob paths and npc paths
                //blueprintFile.correctInitialTexturePaths()
                blueprintFile.readBBModel()
            } else scanDirectory(blueprintFile)
        }
    }

    private fun File.readBBModel() {
        var blueprint = readText().replace("\\\\", "/")
        val paths = blueprint.split("{\"path\":\"").drop(1).map { it.substringBefore(",\"id\"") }

        paths.forEachIndexed { i, s ->
            // Might reuse texture so make sure id is correct just in case
            if (s.substringAfter("\"id\":\"").substringBefore("\"").toIntOrNull() != i) return@forEachIndexed

            val texturePath = s.substringBefore("\",\"name\"")
            val newNamespace =
                texturePath.substringAfter("\"namespace\":\"").substringBefore("\"").substringAfter("assets/")
                    .substringBefore("/textures")
            val newTexture = texturePath.substringAfter("\"name\":\"").substringBefore("\"").substringAfterLast("/")
                .takeIf { it.endsWith(".png") } ?: return@forEachIndexed
            val newPath = texturePath.substringAfter("\"folder\":\"").substringBefore("\"")
                .substringAfter("assets/$newNamespace/textures/").substringBefore("/$newTexture")
            val template =
                "{\"path\",\"$texturePath\",\"name\":\"$newTexture\",\"folder\":\"$newPath\",\"namespace\":\"$newNamespace\""
            blueprint = blueprint
                .replace(",\"relative_path\":\"$texturePath", ",\"relative_path\":\"$texturePath")
                .replace("{\"path\":\"$s", template)
                .replace("\"saved\":false", "\"saved\":true")
        }
        writeText(blueprint)
    }

    private fun File.correctInitialTexturePaths() {
        val blueprint = readText().replace("\\\\", "/")
        val parent = parent.replace("\\\\", "/")
        val assets = "assets/minecraft/textures/"
        when {
            parent.endsWith("mobs") ->
                writeText(blueprint.replace("$assets/creatures", "assets/mineinabyss/textures/mobs"))
            parent.endsWith("npcs") -> {
                writeText(blueprint.replace("$assets/characters", "assets/mineinabyss/textures/characters"))
                writeText(blueprint.replace("$assets/creatures", "assets/mineinabyss/textures/characters"))
            }
            parent.endsWith("relics") -> {
                writeText(blueprint.replace("$assets/items", "assets/mineinabyss/textures/relics"))
                writeText(blueprint.replace("$assets/relics", "assets/mineinabyss/textures/relics"))

            }
            parent.endsWith("blocks") -> {
                writeText(blueprint.replace("$assets/block", "assets/mineinabyss/textures/blocks"))
                writeText(blueprint.replace("$assets/block/modelengine", "assets/mineinabyss/textures/blocks"))
            }
        }
    }
}
