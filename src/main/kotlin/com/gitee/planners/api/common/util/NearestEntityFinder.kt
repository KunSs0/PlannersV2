package com.gitee.planners.api.common.util

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.util.BoundingBox

/**
 * 选区查找器基类。
 *
 * 统一提供实体碰撞箱读取与取样范围计算，子类只负责“碰撞箱是否落在选区形状内”。
 * 所有子类都必须按实体真实 AABB 判定，不能只看实体位置点。
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
     * @return 落在选区内的实体。
     */
    abstract fun request(): List<Entity>

    /**
     * 取覆盖整个选区的世界轴对齐包围盒。
     *
     * 用于就近取样：先用包围盒向世界要候选实体，再做形状判定，避免把大范围内的
     * 掉落物、箭矢等非生物对象全部装进列表。
     *
     * @return 取样包围盒。
     */
    abstract fun samplingBounds(): BoundingBox

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
