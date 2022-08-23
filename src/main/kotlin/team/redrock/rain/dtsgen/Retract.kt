package team.redrock.rain.dtsgen

/**
 * team.redrock.rain.dtsgen.Retract
 * dts-gen
 *
 * @author 寒雨
 * @since 2022/8/22 20:29
 */
class RetractNode(val depth: Int, val singleSpace: String) {
    inline fun retract(scope: RetractNode.() -> Unit) {
        RetractNode(depth + 1, singleSpace).scope()
    }
}

inline fun withNewRetract(scope: RetractNode.() -> Unit) {
    RetractNode(0, "    ").scope()
}