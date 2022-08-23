package team.redrock.rain.dtsgen

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.jar.JarFile

/**
 * team.redrock.rain.dtsgen.DtsGen
 * dts-gen
 *
 * @author 寒雨
 * @since 2022/8/22 19:23
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        error("empty arguments")
    }
    val file = File(args[0])
    if (!file.exists()) {
        error("file not found")
    }
    val fileOutputDir = File(file.parent, "${file.nameWithoutExtension}-types")
    fileOutputDir.mkdirs()
    JarFile(file).use { jarFile ->
        for (jarEntry in jarFile.entries()) {
            jarFile.getInputStream(jarEntry).use {
                runCatching {
                    val path = jarEntry.name
                    // 是类文件
                    if (path.endsWith(".class")) {
                        val reader = ClassReader(it)
                        val classNode = ClassNode()
                        reader.accept(classNode, 0)
                        val generator = DeclareTypeGenerator(classNode)
                        generator.generate(File(fileOutputDir, classNode.name.replace("/", ".") + ".d.ts"))
                    }
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }
    }
    val indexDts = fileOutputDir.resolve("index.d.ts")
    indexDts.createNewFile()
    fileOutputDir.listFiles()?.forEach {
        if (it.name.endsWith(".d.ts") && it.name != "index.d.ts") {
            indexDts.appendText("/// <reference path=\"./${it.name}\" />\n")
        }
    }
}