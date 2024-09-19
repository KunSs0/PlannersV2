import com.gitee.planners.util.math.createIdentityMatrix
import com.gitee.planners.util.math.rotate
import com.gitee.planners.util.math.transform
import org.ejml.simple.SimpleMatrix

/**
 * 矩形体的顶点示意图：
 *
 *       y
 *       |
 *       |____ x
 *      /
 *     z
 * 顶点编号：
 *
 *        4--------5
 *       /|       /|
 *      0--------1 |
 *      | |      | |
 *      | 7------|-6
 *      |/       |/
 *      3--------2
 *
 * 顶点坐标：
 *
 *  前上右 0: ( width/2,  height/2,  depth/2)
 *  前上左 1: (-width/2,  height/2,  depth/2)
 *  前下右 2: ( width/2, -height/2,  depth/2)
 *  前下左 3: (-width/2, -height/2,  depth/2)
 *  后上右 4: ( width/2,  height/2, -depth/2)
 *  后上左 5: (-width/2,  height/2, -depth/2)
 *  后下右 6: ( width/2, -height/2, -depth/2)
 *  后下左 7: (-width/2, -height/2, -depth/2)
 * */
class ShapeBlock(val width: Double, val height: Double, val depth: Double) {

    /**
     *
     */
    // 计算八个顶点
    val mapped = arrayOf(
        Vector(width / 2, height / 2, depth / 2),
        Vector(-width / 2, height / 2, depth / 2),
        Vector(width / 2, -height / 2, depth / 2),
        Vector(-width / 2, -height / 2, depth / 2),
        Vector(width / 2, height / 2, -depth / 2),
        Vector(-width / 2, height / 2, -depth / 2),
        Vector(width / 2, -height / 2, -depth / 2),
        Vector(-width / 2, -height / 2, -depth / 2)
    )

    fun build(yaw: Float): Map<Type, Vector> {
        // 绑定视角
        val matrix = createIdentityMatrix().rotate(yaw.toDouble(), 1)
        // 计算旋转后的八个顶点
        val map = Type.values().associateWith {
            val build = it.build(mapped[it.index], Vector(width, height, depth), 0.0)

            transform(matrix,build.x, build.y, build.z, 1.0)
        }

        return map
    }
    /**
     * Transform a vector using the matrix
     */
    fun transform(matrix: SimpleMatrix, x: Double, y: Double, z: Double, w: Double): Vector {
        val simpleMatrix = SimpleMatrix(4, 1, true, doubleArrayOf(x, y, z, w))
        val result = matrix.mult(simpleMatrix)
        return Vector(result[0, 0], result[1, 0], result[2, 0])
    }
}


enum class Type(val index: Int) {
    /**
     * 后 下左
     * 从原点向左、向下、向后各移动 inflation 的距离。
     */
    BOTTOM_LEFT_BACK(7) {
        override fun build(origin: Vector, size: Vector, inflation: Double): Vector {
            return Vector(
                origin.x - inflation, origin.y - inflation, origin.z - inflation
            )
        }
    },

    /**
     * 后 下右
     * 从原点向左、向下移动 inflation 的距离，向后移动 vertexSize.z 加上 inflation 的距离。
     */
    BOTTOM_RIGHT_BACK(6) {
        override fun build(origin: Vector, size: Vector, inflation: Double): Vector {
            return Vector(
                origin.x - inflation, origin.y - inflation, origin.z + size.z + inflation
            )
        }
    },

    /**
     * 前 下左
     * 从原点向右移动 vertexSize.x 加上 inflation 的距离，向下移动 inflation 的距离，向后移动 inflation 的距离。
     */
    BOTTOM_LEFT_FRONT(3) {
        override fun build(origin: Vector, size: Vector, inflation: Double): Vector {
            return Vector(
                origin.x + size.x + inflation, origin.y - inflation, origin.z - inflation
            )
        }
    },
    //
    /**
     * 前 下右
     * 从原点向右移动 vertexSize.x 加上 inflation 的距离，向下移动 inflation 的距离，向后移动 vertexSize.z 加上 inflation 的距离。
     */
    BOTTOM_RIGHT_FRONT(2) {
        override fun build(origin: Vector, size: Vector, inflation: Double): Vector {
            return Vector(
                origin.x + size.x + inflation, origin.y - inflation, origin.z + size.z + inflation
            )
        }
    },

    /**
     * 后 上左
     * 从原点向左、向后移动 inflation 的距离，向上移动 vertexSize.y 加上 inflation 的距离。
     */
    TOP_LEFT_BACK(5) {
        override fun build(origin: Vector, size: Vector, inflation: Double): Vector {
            return Vector(
                origin.x - inflation, origin.y + size.y + inflation, origin.z - inflation
            )
        }
    },

    /**
     * 后 上右
     * 从原点向左移动 inflation 的距离，向上移动 vertexSize.y 加上 inflation 的距离，向后移动 vertexSize.z 加上 inflation 的距离。
     */
    TOP_RIGHT_BACK(4) {
        override fun build(origin: Vector, size: Vector, inflation: Double): Vector {
            return Vector(
                origin.x - inflation, origin.y + size.y + inflation, origin.z + size.z + inflation
            )
        }
    },

    /**
     * 前 上左
     * 从原点向右移动 vertexSize.x 加上 inflation 的距离，向上移动 vertexSize.y 加上 inflation 的距离，向后移动 inflation 的距离。
     */
    TOP_LEFT_FRONT(1) {
        override fun build(origin: Vector, size: Vector, inflation: Double): Vector {
            return Vector(
                origin.x + size.x + inflation, origin.y + size.y + inflation, origin.z - inflation
            )
        }
    },

    /**
     * 前 上右
     * 从原点向右移动 vertexSize.x 加上 inflation 的距离，向上移动 vertexSize.y 加上 inflation 的距离，向后移动 vertexSize.z 加上 inflation 的距离。
     */
    TOP_RIGHT_FRONT(0) {
        override fun build(origin: Vector, size: Vector, inflation: Double): Vector {
            return Vector(
                origin.x + size.x + inflation, origin.y + size.y + inflation, origin.z + size.z + inflation
            )
        }
    };

    abstract fun build(origin: Vector, size: Vector, inflation: Double): Vector
}
