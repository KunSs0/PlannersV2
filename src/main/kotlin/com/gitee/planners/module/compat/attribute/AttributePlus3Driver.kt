package com.gitee.planners.module.compat.attribute

import com.gitee.planners.api.common.task.SimpleUniqueTask
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.TargetBukkitEntity
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.serverct.ersha.api.AttributeAPI
import taboolib.common.platform.function.warning
import taboolib.common5.cdouble
import taboolib.common5.clong

object AttributePlus3Driver : AttributeDriver {


    override fun authorizeEnable(): Boolean {
        val plugin = Bukkit.getPluginManager().getPlugin("AttributePlus")
        return plugin != null && plugin.description.version.split(".")[0] == "3"
    }

    override fun set(target: Target<*>, id: String, source: List<String>, timeout: Int) {
        if (target !is TargetBukkitEntity) {
            warning("Target type must be TargetBukkitEntity.")
            return
        }
        val entity = target.instance as LivingEntity
        val original = AttributeAPI.getAttrData(entity)
        val data = AttributeAPI.getAttributeSource(source)
        //用持久化，能通过ap指令直接删
        AttributeAPI.addPersistentSourceAttribute(original,id,source,timeout.cdouble)
//        // 设置超时任务,如果timeout==-1 代表永不超时(关服就没了)
//        if (timeout != -1) {
//            val uniqueId = "${entity.uniqueId}.$id"
//            SimpleUniqueTask.create(uniqueId, timeout.clong, true) {
//                remove(target, id)
//            }
//        }
    }

    override fun remove(target: Target<*>, id: String) {
        if (target !is TargetBukkitEntity) {
            warning("Target type must be TargetBukkitEntity.")
            return
        }
        val entity = target.instance as LivingEntity
        val original = AttributeAPI.getAttrData(entity)
        AttributeAPI.takeSourceAttribute(original, id)
    }


}
