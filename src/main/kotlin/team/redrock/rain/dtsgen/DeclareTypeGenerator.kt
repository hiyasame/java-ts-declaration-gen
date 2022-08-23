package team.redrock.rain.dtsgen

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.io.File

/**
 * team.redrock.rain.dtsgen.DeclareTypeGenerator
 * dts-gen
 *
 * @author 寒雨
 * @since 2022/8/22 19:56
 */
class DeclareTypeGenerator(private val classNode: ClassNode) {

    private val strList = mutableListOf<String>()
    private var nextLineIdx: Int = 0
    private val typeMapper = ASMTypeMapper()
    private val methodSignatureRegex = "\\((?<params>(\\S+)?)\\)(?<result>\\S+)".toRegex()

    fun generate(file: File) {
        if (!file.exists()) {
            file.createNewFile()
            withNewRetract {
                writeNamespace {
                    writeClassDeclaration {
                        classNode.methods.forEach { writeMethod(it) }
                        classNode.fields.forEach { writeField(it) }
                    }
                }
            }
            file.writeText(strList.joinToString("\n"))
        }
    }

    /**
     * 属性声明
     *
     * @param fieldNode
     */
    private fun RetractNode.writeField(fieldNode: FieldNode) {
        writeTsIgnore()
        var content = ""
        if (fieldNode.access and Opcodes.ACC_PRIVATE != 0) {
            content += "private "
        } else if (fieldNode.access and Opcodes.ACC_PUBLIC != 0) {
            content += "public "
        }
        if (fieldNode.access and Opcodes.ACC_ABSTRACT != 0) {
            content += "abstract "
        }
        if (fieldNode.access and Opcodes.ACC_FINAL != 0) {
            content += "readonly "
        }
        if (fieldNode.access and Opcodes.ACC_STATIC != 0) {
            content += "static "
        }
        content += fieldNode.name
        val typeDesc = fieldNode.desc
        content += ": ${typeMapper.map(typeDesc)}"
        writeLine(content)
    }

    private fun RetractNode.writeConstructor(methodNode: MethodNode) {
        var content = ""
        if (methodNode.access and Opcodes.ACC_PRIVATE != 0) {
            content += "private "
        } else if (methodNode.access and Opcodes.ACC_PUBLIC != 0) {
            content += "public "
        }
        content += "constructor"
        val typeSignature = methodNode.desc
        val namedGroup = methodSignatureRegex.find(typeSignature)!!.groups as MatchNamedGroupCollection
        val params = typeMapper.splitTypes(namedGroup["params"]!!.value).map { typeMapper.map(it) }
        var paramIdx = 0
        content += if (params.isEmpty() || params[0].isEmpty()) {
            "()"
        } else {
            "(" + params.joinToString(", ") { s -> "param${paramIdx++}: $s" } + ")"
        }
        writeLine(content)
    }

    /**
     * 方法声明
     */
    private fun RetractNode.writeMethod(methodNode: MethodNode) {
        writeTsIgnore()
        if (methodNode.name == "<init>") {
            writeConstructor(methodNode)
            return
        }
        var content = ""
        if (methodNode.access and Opcodes.ACC_PRIVATE != 0) {
            content += "private "
        } else if (methodNode.access and Opcodes.ACC_PUBLIC != 0) {
            content += "public "
        }
        if (methodNode.access and Opcodes.ACC_ABSTRACT != 0) {
            content += "abstract "
        }
        if (methodNode.access and Opcodes.ACC_FINAL != 0) {
            content += "readonly "
        }
        if (methodNode.access and Opcodes.ACC_STATIC != 0) {
            content += "static "
        }
        content += methodNode.name
        val typeSignature = methodNode.desc
        val namedGroup = methodSignatureRegex.find(typeSignature)!!.groups as MatchNamedGroupCollection
        val params = typeMapper.splitTypes(namedGroup["params"]!!.value).map { typeMapper.map(it) }
        val result = typeMapper.map(namedGroup["result"]!!.value)
        var paramIdx = 0
        content += "(" + params.joinToString(", ") { "param${paramIdx++}: $it" } + "): $result"
        writeLine(content)
    }

    /**
     * 类声明
     *
     * @param nextNode
     */
    private inline fun RetractNode.writeClassDeclaration(nextNode: RetractNode.() -> Unit) {
        writeTsIgnore()
        var content = ""
        var isClass = false
        // interface or enum or class
        content += if (classNode.access and Opcodes.ACC_INTERFACE != 0) {
            "interface"
        } else if (classNode.access and Opcodes.ACC_ENUM != 0) {
            "enum"
        } else {
            isClass = true
            "class"
        }
        content += " ${classNode.name.split("/").last()}"
        // 类一定有超类，就必须跟一个extends, java/lang/Object除外
        if (isClass) {
            content += " extends "
            content += classNode.superName.replace("/", ".")
        }
        // 是否abstract
        if (classNode.access and Opcodes.ACC_ABSTRACT != 0) {
            content = "abstract $content"
        }
        // 有interfaces
        if (classNode.interfaces.isNotEmpty()) {
            content += if (classNode.access and Opcodes.ACC_INTERFACE != 0) {
                " extends ${classNode.interfaces.joinToString(", ") { it.replace("/", ".") }}"
            } else {
                " implements ${classNode.interfaces.joinToString(", ") { it.replace("/", ".") }}"
            }
        }
        writeBlock(content) {
            nextNode()
        }
    }

    /**
     * 声明包名为namespace
     *
     * @param nextNode
     */
    private inline fun RetractNode.writeNamespace(nextNode: RetractNode.() -> Unit) {
        val namespaces = classNode.name.split("/").toMutableList().apply { removeLast() }
        var currentNode = this
        namespaces.forEachIndexed { index, s ->
            var contentStr = "namespace $s"
            if (index == 0) {
                contentStr = "declare $contentStr"
            }
            currentNode.writeBlock(contentStr) {
                currentNode = this
            }
        }
        currentNode.nextNode()
    }

    private fun RetractNode.writeTsIgnore() {
        writeLine("// @ts-ignore")
    }

    private inline fun RetractNode.writeBlock(content: String, nextNode: RetractNode.() -> Unit) {
        strList.add(nextLineIdx, singleSpace.repeat(depth) + "$content {")
        strList.add(nextLineIdx + 1, singleSpace.repeat(depth) + "}")
        nextLineIdx++
        retract {
            nextNode()
        }
    }

    private fun RetractNode.writeLine(content: String) {
        strList.add(nextLineIdx, singleSpace.repeat(depth) + content)
        nextLineIdx++
    }

}