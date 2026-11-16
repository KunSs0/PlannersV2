package com.gitee.planners.api.common.util

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.util.BoundingBox

/**
 * 选区查找器基类。
 *
 * 统一负责“遍历候选 → 读取真实碰撞箱 → 形状判定 → 遮挡判定”，子类只实现形状与取样范围。
 * 遮挡是所有选区共用的系统规则：被实心方块挡住的目标一律不入选，只有配置白名单里的方块
 * （例如栅栏）不遮挡。
 */
abstract class NearestEntityFinder(val origin: Location, var samples: List<Entity>) {

    /**
     * 直接给定候选实体的构造入口。
     *
     * @param origin 选区原点。
     * @param sampling 候选实体采样器。
     */
    constructor(origin: Location, sampling: SynchronousSampling<List<Entity>>) : this(origin, sampling.get())

    /**
     * 执行筛选。
     *
     * @return 落在选区内且未被遮挡的实体。
     */
    fun request(): List<Entity> {
        val result = ArrayList<Entity>(samples.size)
        if (samples.isEmpty()) {
            return result
        }
        val rayOrigin = occlusionOrigin()
        val world = rayOrigin.world
        for (entity in samples) {
            val box = getBoundingBox(entity)
            if (!touches(box)) {
                continue
            }
            if (!SelectionOcclusion.isClear(world, rayOrigin, box)) {
                continue
            }
            result.add(entity)
        }
        return result
    }

    /**
     * 取覆盖整个选区的世界轴对齐包围盒。
     *
     * 用于就近取样：先用包围盒向世界要候选实体，再做形状与遮挡判定，避免把大范围内的
     * 掉落物、箭矢等非生物对象全部装进列表。
     *
     * @return 取样包围盒。
     */
    abstract fun samplingBounds(): BoundingBox

    /**
     * 判断碰撞箱是否落在选区形状内。
     *
     * @param box 实体碰撞箱。
     * @return 落在形状内时返回 true。
     */
    protected abstract fun touches(box: BoundingBox): Boolean

    /**
     * 取遮挡射线的起点。
     *
     * 默认取选区原点；需要把起点抬到选区判定高度的子类（例如矩形光束）自行覆写。
     *
     * @return 射线起点。
     */
    protected open fun occlusionOrigin(): Location {
        return origin
    }

    /**
     * 读取实体当前的真实碰撞箱。
     *
     * 使用 Bukkit AABB 而不是用工位与宽高拼装：只有真实 AABB 才包含姿态（潜行、游泳、爬行）、
     * 载具偏移与非常规体型。不缓存结果：实体会移动、被击退或改变尺寸，跨查询复用旧包围盒
     * 会让选区使用过期坐标。
     *
     * @param entity 待读取实体。
     * @return 实体碰撞箱。
     */
    fun getBoundingBox(entity: Entity): BoundingBox {
        return entity.boundingBox
    }
}
