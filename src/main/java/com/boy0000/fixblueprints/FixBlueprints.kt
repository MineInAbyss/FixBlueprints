package com.boy0000.fixblueprints

import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class FixBlueprints : JavaPlugin() {
    override fun onEnable() {
        // Plugin startup logic
        saveDefaultConfig()
        fixBluePrints()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    private val blueprintDir = File(this.dataFolder.parent, "/ModelEngine/blueprints/")
    private val assetTemplateFolder = File(this.dataFolder, "/assets/")
    private fun fixBluePrints() {
        if (!blueprintDir.exists() || !assetTemplateFolder.exists()) return
        scanDirectory(blueprintDir)
    }

    // Recursive function to read all files down until it gets a BBModel file
    private fun scanDirectory(blueprintDir: File) {
        blueprintDir.listFiles()?.forEach blueprint@{ blueprintFile ->
            if (blueprintFile.name.endsWith(".bbmodel")) {
                readBBModel(blueprintFile)
            } else scanDirectory(blueprintFile)
        }
    }

    private fun readBBModel(blueprintFile: File) {
        var blueprint = blueprintFile.readText()
        /*if (blueprintFile.parent.substringAfterLast("/").endsWith("blocks")) {
            blueprint = blueprint
                .replace(
                    "\\\\assets\\\\mineinabyss\\\\textures\\\\block\\\\modelengine\\\\",
                    "\\\\assets\\\\mineinabyss\\\\textures\\\\blocks\\\\"
                )
                .replace(
                    "\\\\assets\\\\minecraft\\\\textures\\\\block\\\\",
                    "\\\\assets\\\\mineinabyss\\\\textures\\\\blocks\\\\"
                )
                .replace(
                    "\\\\assets\\\\minecraft\\\\textures\\\\creatures\\\\",
                    "\\\\assets\\\\mineinabyss\\\\textures\\\\blocks\\\\"
                )

        }
        if (blueprintFile.parent.substringAfterLast("/").endsWith("mobs")) {
            blueprint = blueprint.replace(
                "\\\\assets\\\\minecraft\\\\textures\\\\creatures\\\\",
                "\\\\assets\\\\mineinabyss\\\\textures\\\\mobs\\\\"
            )
        }
        if (blueprintFile.parent.substringAfterLast("/").endsWith("npc")) {
            blueprint = blueprint.replace(
                "\\\\assets\\\\minecraft\\\\textures\\\\characters\\\\",
                "\\\\assets\\\\mineinabyss\\\\textures\\\\characters\\\\"
            )
        }*/

        val paths = blueprint.split("\"path\":\"").asSequence().drop(1)
            .map { it.substringBefore("\",\"id\"").replace("\\\\", "/") }
            .toList().filter { it.endsWith(".png") }.filter { "assets/mineinabyss" !in it }.toList()
        // If empty or not in a resourcepack structured folder, skip
        if (paths.isEmpty() || paths.none { "assets/" in it }) return
        val namespaces = paths.map { it.substringAfter("assets/").substringBefore("/") }
        val folders = paths.mapIndexed { i, p ->
            p.substringAfter("assets/${namespaces[i]}/textures/").substringBeforeLast("/")
        }
        val textures = paths.map { it.substringAfterLast("/") }.filter { it.endsWith(".png") && "/" !in it }
        blueprintFile.writeBBModel(paths, namespaces, folders, textures)
    }

    private fun File.writeBBModel(
        paths: List<String>,
        namespaces: List<String>,
        folders: List<String>,
        textures: List<String>
    ) {
        var blueprint = this.readText()

        paths.forEachIndexed { index, path ->
            val newPath = "\"path\":\"${path}\","
            val oldPath = blueprint.substringAfter(newPath).substringBefore("\",\"id\":").replace("\\\\", "/")
            val oldTexture = oldPath.substringAfter("\"name\":\"").substringBefore("\",\"folder\":")
            val oldFolder = oldPath.substringAfter("\"name\":\"$oldTexture\",\"folder\":\"").substringBefore("\",\"namespace\":")
            val oldNamespace = oldPath.substringAfter("\"folder\":\"$oldFolder\",\"namespace\":\"").substringBefore("\",\"id\":").takeIf { it.isNotBlank() } ?: "minecraft"
            val replacement = oldPath
                .replace(oldNamespace, namespaces[index])
                .replace(oldFolder, folders[index])
                .replace(oldTexture, textures[index])
            if (nameWithoutExtension == "ashimite") {
                //println("oldPath: $oldPath")
                //println("newPath: $newPath")
                println(oldNamespace)
                println(oldFolder)
                //println(oldTexture)
            }
            blueprint = blueprint.replace(oldPath, replacement)
            //this.writeText(blueprint)
        }
    }
}
