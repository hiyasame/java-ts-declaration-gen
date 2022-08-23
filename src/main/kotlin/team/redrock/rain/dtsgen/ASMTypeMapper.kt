package team.redrock.rain.dtsgen

/**
 * team.redrock.rain.dtsgen.ASMTypeMapper
 * dts-gen
 *
 * @author 寒雨
 * @since 2022/8/22 23:31
 */
class ASMTypeMapper {

    private val signatureMap = mapOf(
        // 匹配数字基础类型，全部转换为number
        "^([IDFJSCB])$".toRegex() to "number /** {0} **/",
        // 转换void
        "^V$".toRegex() to "void",
        // 转换布尔值
        "^Z$".toRegex() to "boolean",
    )
    private val typeSplitRegex = "(\\[)?(?<!L(\\S+)?)[IDFJSCB]|(?<=;)[IDFJSCB]|L(\\S+);".toRegex()
    private val objSignatureRegex = "L(\\S+);".toRegex()
    private val genericRegex = "<(\\S+)>".toRegex()

    // TODO: 转换常用函数式接口为lambda表达式, 最好能支持Kotlin的FunctionN
    private val typeMap = mapOf(
        "java\\.lang\\.(Byte|Float|Double|Integer|Short|Long)".toRegex() to "number /** {0} **/",
        "java\\.lang\\.Object".toRegex() to "any",
        "java\\.util\\.(List|Set|Collection)".toRegex() to "Array",
        "java\\.lang\\.Throwable".toRegex() to "Error",
        "(java\\.lang\\.String)$".toRegex() to "string"
    )

    /**
     * 转换单个JVM类型字符串为Java的类型表示方式
     *
     * @param str
     * @return
     */
    fun map(str: String): String {
        // 递归处理泛型
        var currentStr = genericRegex.replace(str) {
            "<${map(it.groups[0]!!.value)}>"
        }
        // 处理基础类型
        currentStr = signatureMap.toList().fold(currentStr) { s, (regex, replacement) ->
            regex.replace(s) {
                if (it.groups.isNotEmpty())
                    replacement.replace("{0}", it.groups[0]!!.value)
                else
                    replacement
            }
        }
        // 处理引用类型
        currentStr = currentStr.replace(objSignatureRegex) { it.groups[1]!!.value.replace("/", ".") }
            .run {
                typeMap.toList().fold(this) { s, (regex, replacement) ->
                    regex.replace(s) {
                        if (it.groups.isNotEmpty())
                            replacement.replace("{0}", it.groups[0]!!.value)
                        else
                            replacement
                    }
                }
            }
        // 处理数组
        if (currentStr.startsWith("[")) {
            currentStr = "Array<" + currentStr.removePrefix("[") + ">"
        }
        return currentStr
    }

    /**
     * 分割JVM类型
     *
     * @param str
     * @return
     */
    fun splitTypes(str: String): List<String> {
        val list = mutableListOf<String>()
        val m = typeSplitRegex.toPattern().matcher(str)
        while (m.find()) {
            list.add(m.group())
        }
        return list.flatMap { it.tryModifyType() }
    }

    private fun String.tryModifyType(): List<String> {
        val list = split(";")
            .toMutableList()
        // 最后一个是否为空字符串
        var lastFlag = false
        if (list.last().isEmpty()) {
            lastFlag = true
            list.removeLast()
        }
        list.forEachIndexed { index, _ ->
            if (index != list.size - 1 || lastFlag) {
                list[index] += ";"
            }
        }
        if (list.size == 1) {
            return list
        }
        return list.flatMap { splitTypes(it) }
    }
}