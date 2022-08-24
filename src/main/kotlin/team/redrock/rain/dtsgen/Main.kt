package team.redrock.rain.dtsgen

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InnerClassNode
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
    val npmProjectDir = File(file.parent, "${file.nameWithoutExtension}-types")
    val fileOutputDir = npmProjectDir.resolve("dist")
    fileOutputDir.mkdirs()
    val classNodes = mutableListOf<ClassNode>()
    val innerClasses = mutableSetOf<Pair<ClassNode, InnerClassNode>>()
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
                        classNodes.add(classNode)
                    }
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }
    }
    classNodes.forEach { classNode ->
        innerClasses.addAll(classNode.innerClasses.mapNotNull { s ->
            classNodes.find { it.name == s.name }?.to(s)
        })
    }
    classNodes.removeAll(innerClasses.map { it.first })
    // 生成外部类
    classNodes.forEach { classNode ->
        val generator = DeclareTypeGenerator(classNode)
        generator.generate(File(fileOutputDir, classNode.name.replace("/", ".") + ".d.ts"))
    }
    // 生成内部类
    innerClasses.forEach { (classNode, innerClassNode) ->
        val generator = DeclareTypeGenerator(classNode, true, innerClassNode)
        generator.generate(fileOutputDir.resolve(
            classNode.name.replace("/", ".")
                .replace("$", ".") + ".d.ts"
        ))
    }
    val indexDts = fileOutputDir.resolve("index.d.ts")
    indexDts.createNewFile()
    fileOutputDir.listFiles()?.forEach {
        if (it.name.endsWith(".d.ts") && it.name != "index.d.ts") {
            indexDts.appendText("/// <reference path=\"./${it.name}\" />\n")
        }
    }
    // 生成package.json
    val packageJson = npmProjectDir.resolve("package.json")
    packageJson.createNewFile()
    packageJson.writeText("""
        {
            "name": "@rhino-types/${file.nameWithoutExtension}",
            "version": "0.0.1",
            "description": "java types of ${file.nameWithoutExtension}",
            "author": "coldrain-moro",
            "license": "MIT",
            "types": "dist/index.d.ts"
        }
    """.trimIndent())
}