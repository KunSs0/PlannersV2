package com.gitee.planners.core.database

/** 后台持久化使用的不可变字段快照；身份句柄只在数据库消费者内解析。 */
object ProfileSnapshot {
    /** @property identity 目标职业线身份，其余字段均是提交时的值。 */
    data class Router(
        val identity: RecordIdentity, val user: Long, val router: String,
        val level: Int, val experience: Int, val currentRoute: RecordIdentity?,
        val points: Int, val used: Int
    )

    /** 职业阶段插入快照，父阶段只引用身份，不持有在线对象。 */
    data class Route(
        val identity: RecordIdentity, val user: Long, val router: String,
        val parent: RecordIdentity?, val job: String
    )

    /** 技能创建、等级和装备保存快照。 */
    data class Skill(
        val identity: RecordIdentity, val skill: String, val level: Int,
        val equipped: Boolean, val page: String?, val slot: String?
    )

    /** 节点学习后最终等级及其记录身份。 */
    data class Node(val identity: RecordIdentity, val tree: String, val node: String, val level: Int)

    /** 元数据已在主线程序列化，后台不再读取可变 Metadata。 */
    data class Metadata(val user: Long, val key: String, val type: String?, val token: String?, val expires: Long)

    /** 清空时捕获的旧记录身份，禁止后台重新遍历在线职业。 */
    data class Deletion(val router: RecordIdentity, val routes: List<RecordIdentity>)
}
