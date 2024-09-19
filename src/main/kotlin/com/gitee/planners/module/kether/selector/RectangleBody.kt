package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.Axis
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.*
import com.gitee.planners.util.math.createIdentityMatrix
import com.gitee.planners.util.math.rotate
import com.gitee.planners.util.math.transform
import com.gitee.planners.util.math.translate
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyParticle
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.info
import taboolib.common.util.Vector
import taboolib.common5.Quat
import taboolib.common5.cdouble
import taboolib.module.navigation.BoundingBox
import taboolib.module.navigation.NMS
import taboolib.module.navigation.toCommonVector
import taboolib.module.nms.nmsClass
import taboolib.platform.util.onlinePlayers

/**
 * 矩形选择器
 */
object RectangleBody : AbstractSelector("rectangle") {

    /**
     * rectangle <width> <height> <depth> [offset "<x> <y> <z>"] [debug bool]
     */
    override fun select() = KetherHelper.combinedKetherParser {
        it.group(
            actionDouble(),
            actionDouble(),
            actionDouble(),
            command("offset", then = actionVector()).option().defaultsTo(Vector()),
            command("debug", then = bool()).option().defaultsTo(false)
        ).apply(it) { width, height, depth, offset, isDebug ->
            now {
                val block = ShapeBlock(width, height, depth, offset)
                val center = (getEnvironmentContext().origin as? TargetLocation<*>)
                if (center == null) {
                    return@now
                }
                if (isDebug) {
                    block.drawTest(center.getBukkitLocation())
                }
                getTargetContainer() += block.find(center.getBukkitLocation()).map(::adaptTarget)
            }
        }
    }

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
     *  前上左 0: ( width/2,  height/2,  depth/2)
     *  前上右 1: (-width/2,  height/2,  depth/2)
     *  前下右 2: ( width/2, -height/2,  depth/2)
     *  前下左 3: (-width/2, -height/2,  depth/2)
     *  后上右 4: ( width/2,  height/2, -depth/2)
     *  后上左 5: (-width/2,  height/2, -depth/2)
     *  后下右 6: ( width/2, -height/2, -depth/2)
     *  后下左 7: (-width/2, -height/2, -depth/2)
     * */
    class ShapeBlock(val width: Double, val height: Double, val depth: Double, val offset: Vector) {

        fun build(location: Vector, yaw: Float): Map<Type, Vector> {
            // 绑定视角
            val matrix = createIdentityMatrix()
                // 移动到世界坐标
                .translate(location.x, location.y, location.z)
            // 计算原点
            val origin = Vector(-width / 2, 0.0, -depth / 2)
            // 计算旋转后的八个顶点
            return Type.values().associateWith {
                val build = it.build(origin, Vector(width, height, depth), 0.0)
                matrix
                    .rotate(-Math.toRadians(yaw.cdouble), 1)
                    .translate(offset.x, offset.y, offset.z)
                    .transform(build.x, build.y, build.z, 1.0)
            }
        }

        fun build(location: Vector, yaw: Float, point: Type): Vector {
            // 绑定视角
            val matrix = createIdentityMatrix()
                // 移动到世界坐标
                .translate(location.x, location.y, location.z)
            // 计算原点
            val origin = Vector(-width / 2, 0.0, -depth / 2)
            // 计算旋转后的八个顶点
            val build = point.build(origin, Vector(width, height, depth), 0.0)
            return matrix
                .rotate(-Math.toRadians(yaw.cdouble), 1)
                .translate(offset.x, offset.y, offset.z)
                .transform(build.x, build.y, build.z, 1.0)
        }

        fun getBoundingBox(map: Map<Type, Vector>): BoundingBox {
            val values = map.values
            val minX = values.minOf { it.x }
            val minY = values.minOf { it.y }
            val minZ = values.minOf { it.z }
            val maxX = values.maxOf { it.x }
            val maxY = values.maxOf { it.y }
            val maxZ = values.maxOf { it.z }
            return BoundingBox(minX, minY, minZ, maxX, maxY, maxZ)
        }

        fun find(location: Location): List<Entity> {
            val map = build(Vector(location.x, location.y, location.z), location.yaw)
            val boundingBox = getBoundingBox(map)
            // 找到aabb内的所有实体
            val entities = location.world!!.getNearbyEntities(
                location,
                boundingBox.getXSize(),
                boundingBox.getYSize(),
                boundingBox.getZSize()
            )
            println("find abb 'width=${boundingBox.getXSize()},height=${boundingBox.getYSize()},depth=${boundingBox.getZSize()}' = $entities")
            // 从aabb实体集里找到obb内的实体
            return entities.filter {
                val point = Vector(location.x, location.y, location.z)
                val aabb = NMS.instance.getBoundingBox(it)!!
                this.contains(point, aabb, location.yaw)
            }
        }

        fun contains(location: Vector, aabb: BoundingBox, entityYaw: Float): Boolean {
            val axisX = getAxis(Axis.X, entityYaw)
            val axisY = getAxis(Axis.Y, entityYaw)
            val axisZ = getAxis(Axis.Z, entityYaw)

            val normalAxisX = Vector(1.0, 0.0, 0.0)
            val normalAxisY = Vector(0.0, 1.0, 0.0)
            val normalAxisZ = Vector(0.0, 0.0, 1.0)

            // 生成所有轴
            val axes = arrayOf(
                // 三个OBB主轴
                axisX,
                axisY,
                axisZ,
                // 三个AABB主轴
                normalAxisX,
                normalAxisY,
                normalAxisZ,
                // 九个轴
                axisX.getCrossProduct(normalAxisX),
                axisX.getCrossProduct(normalAxisY),
                axisX.getCrossProduct(normalAxisZ),
                axisY.getCrossProduct(normalAxisX),
                axisY.getCrossProduct(normalAxisY),
                axisY.getCrossProduct(normalAxisZ),
                axisZ.getCrossProduct(normalAxisX),
                axisZ.getCrossProduct(normalAxisY),
                axisZ.getCrossProduct(normalAxisZ)
            )

            return axes.filter { it.length() > 1e-6 }.all {
                // 轴向量归一化
                val normalized = it.normalize()
                val (minA, maxA) = project(aabb, normalized)
                val (minB, maxB) = project(location, entityYaw, normalized)
                // 投影不重叠
                !(maxA < minB || maxB < minA)
            }
        }

        fun project(location: Vector, entityYaw: Float, axis: Vector): Pair<Double, Double> {
            val vertices = Type.values().map { this.build(location, entityYaw, it) }
            val map = vertices.map { it.dot(axis) }
            return Pair(map.minOrNull()!!, map.maxOrNull()!!)
        }

        fun project(aabb: BoundingBox, axis: Vector): Pair<Double, Double> {
            // AABB的八个顶点
            val vertices = arrayOf(
                Vector(aabb.minX, aabb.minY, aabb.minZ),
                Vector(aabb.maxX, aabb.minY, aabb.minZ),
                Vector(aabb.minX, aabb.maxY, aabb.minZ),
                Vector(aabb.minX, aabb.minY, aabb.maxZ),
                Vector(aabb.maxX, aabb.maxY, aabb.minZ),
                Vector(aabb.minX, aabb.maxY, aabb.maxZ),
                Vector(aabb.maxX, aabb.minY, aabb.maxZ),
                Vector(aabb.maxX, aabb.maxY, aabb.maxZ)
            )
            // 顶点投影到轴上
            val map = vertices.map { it.dot(axis) }
            return Pair(map.minOrNull()!!, map.maxOrNull()!!)
        }

        fun getAxis(axis: Axis, entityYaw: Float): Vector {
            return when (axis) {
                Axis.X -> Vector(1.0, 0.0, 0.0)
                    .rotateAroundAxis(getAxis(Axis.Y, entityYaw), Math.toRadians(entityYaw.cdouble))

                Axis.Y -> Vector(0.0, 1.0, 0.0)

                Axis.Z -> Vector(0.0, 0.0, 1.0)
                    .rotateAroundAxis(getAxis(Axis.Y, entityYaw), Math.toRadians(entityYaw.cdouble))

                else -> error("Invalid axis: $axis")
            }
        }

        /**
         * 绘制测试例子图
         */
        fun drawTest(location: Location) {
            val map = build(Vector(location.x, location.y, location.z), location.yaw)
            // 绘制12条线
            onlinePlayers.filter { location.distance(it.location) < 50 }.forEach {
                drawLine(it, map[Type.TOP_LEFT_FRONT]!!, map[Type.TOP_RIGHT_FRONT]!!)
                drawLine(it, map[Type.TOP_LEFT_FRONT]!!, map[Type.TOP_LEFT_BACK]!!)
                drawLine(it, map[Type.TOP_RIGHT_FRONT]!!, map[Type.TOP_RIGHT_BACK]!!)
                drawLine(it, map[Type.TOP_LEFT_BACK]!!, map[Type.TOP_RIGHT_BACK]!!)

                drawLine(it, map[Type.BOTTOM_LEFT_FRONT]!!, map[Type.BOTTOM_RIGHT_FRONT]!!)
                drawLine(it, map[Type.BOTTOM_LEFT_FRONT]!!, map[Type.BOTTOM_LEFT_BACK]!!)
                drawLine(it, map[Type.BOTTOM_RIGHT_FRONT]!!, map[Type.BOTTOM_RIGHT_BACK]!!)
                drawLine(it, map[Type.BOTTOM_LEFT_BACK]!!, map[Type.BOTTOM_RIGHT_BACK]!!)

                drawLine(it, map[Type.TOP_LEFT_FRONT]!!, map[Type.BOTTOM_LEFT_FRONT]!!)
                drawLine(it, map[Type.TOP_RIGHT_FRONT]!!, map[Type.BOTTOM_RIGHT_FRONT]!!)
                drawLine(it, map[Type.TOP_LEFT_BACK]!!, map[Type.BOTTOM_LEFT_BACK]!!)
                drawLine(it, map[Type.TOP_RIGHT_BACK]!!, map[Type.BOTTOM_RIGHT_BACK]!!)
            }
        }

        /**
         * 两点之间绘制线条
         */
        fun drawLine(sender: Player, position: Vector, target: Vector) {
            val x1 = position.x
            val y1 = position.y
            val z1 = position.z
            val x2 = target.x
            val y2 = target.y
            val z2 = target.z

            // 计算两点间的距离
            val distance = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1))

            // 设定粒子数量，基于距离
            val particlesCount = (distance * 10).toInt() // 每单位距离10个粒子

            // 计算方向向量的增量
            val dx = (x2 - x1) / particlesCount
            val dy = (y2 - y1) / particlesCount
            val dz = (z2 - z1) / particlesCount

            // 循环生成粒子
            for (i in 0 until particlesCount) {
                val px = x1 + dx * i
                val py = y1 + dy * i
                val pz = z1 + dz * i

                // 在计算出的位置生成粒子，这里使用了火焰粒子，你可以根据需要更改
                ProxyParticle.FLAME.sendTo(adaptPlayer(sender), taboolib.common.util.Location(null, px, py, pz))
            }
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
         * 从原点向右移动 vertexSize.x 加上 inflation 的距离，
         * 向上移动 vertexSize.y 加上 inflation 的距离，
         * 向后移动 vertexSize.z 加上 inflation 的距离。
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

}
