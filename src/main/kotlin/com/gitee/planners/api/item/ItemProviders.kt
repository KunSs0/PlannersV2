package com.gitee.planners.api.item

/**
 * 物品库适配器注册表，仅用于插件启用期的注册与条件求值期的查表。
 *
 * 注册阶段在主线程串行执行，求值阶段只读取，不存在并发写入。
 */
object ItemProviders {

    private val providers = LinkedHashMap<String, ItemProvider>()

    /**
     * 注册物品库适配器。
     *
     * @param provider 物品库适配器。
     * @throws IllegalArgumentException 适配器 ID 为空或已被其他实现占用时抛出。
     */
    fun register(provider: ItemProvider) {
        val id = provider.providerId
        require(id.isNotEmpty()) { "物品库 ID 不能为空" }
        val current = providers[id]
        require(current == null || current === provider) { "物品库已注册：$id" }
        providers[id] = provider
    }

    /**
     * 按 ID 获取物品库适配器。
     *
     * @param id 物品库 ID。
     * @return 未注册时返回 null。
     */
    fun getOrNull(id: String): ItemProvider? {
        return providers[id]
    }

    /** 已注册适配器快照。 */
    fun values(): List<ItemProvider> {
        return ArrayList(providers.values)
    }
}
