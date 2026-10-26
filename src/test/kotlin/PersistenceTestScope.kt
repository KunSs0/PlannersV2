import com.gitee.planners.core.database.AsyncDatabaseQueue
import com.gitee.planners.core.database.ProfilePersistence

/**
 * 隔离每个测试的队列与调度边界，生产流程仍调用真实 ProfilePersistence。
 * @param schedule 可选的测试调度器；默认只记录后台任务。
 */
class PersistenceTestScope(schedule: ((Runnable) -> Unit)? = null) : AutoCloseable {
    val tasks = ArrayList<Runnable>()
    val failures = ArrayList<Throwable>()
    private val field = ProfilePersistence::class.java.getDeclaredField("queue")
    private val previous = LinkedHashMap<java.lang.reflect.Field, Any?>()
    private val queue: AsyncDatabaseQueue

    init {
        field.isAccessible = true
        queue = field.get(ProfilePersistence) as AsyncDatabaseQueue
        val scheduler: (Runnable) -> Unit
        if (schedule == null) {
            scheduler = { task -> tasks.add(task) }
        } else {
            scheduler = schedule
        }
        // 每个 fixture 使用独立队列，丢弃的模拟调度任务不能污染其他用例。
        val replacement = AsyncDatabaseQueue(scheduler, { _, failure -> failures.add(failure) })
        for (member in AsyncDatabaseQueue::class.java.declaredFields) {
            if (!java.lang.reflect.Modifier.isStatic(member.modifiers)) {
                member.isAccessible = true
                previous[member] = member.get(queue)
                member.set(queue, member.get(replacement))
            }
        }
    }

    /** 恢复此前队列，不执行尚未启动的数据库任务。 */
    override fun close() {
        for ((member, value) in previous) {
            member.set(queue, value)
        }
    }
}
