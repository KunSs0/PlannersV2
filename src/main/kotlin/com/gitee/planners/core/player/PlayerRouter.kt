package com.gitee.planners.core.player

import com.gitee.planners.api.RegistryBuiltin
import com.gitee.planners.api.job.Job
import com.gitee.planners.api.job.Route
import com.gitee.planners.api.job.Router

class PlayerRouter(val bindingId: Long, val routerId: String, val nodes: List<Node>) : Router {

    private val instance: Router
        get() = RegistryBuiltin.ROUTER.getOrNull(routerId) ?: error("Could not find router with id $routerId")

    override val id: String
        get() = instance.id

    override val name: String
        get() = instance.name

    override fun getRouteOrNull(id: String): Route? {
        throw IllegalStateException("Not implemented")
    }

    override fun getRouteByJob(job: Job): Route? {
        throw IllegalStateException("Not implemented")
    }

    /**
     * parent == -1 代表为顶节点
     */
    class Node(val parentId: Long, routeId: Long)

}
