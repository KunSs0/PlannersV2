package com.gitee.planners.core.skilltree

class SkillTreeNodeProviderRegistry private constructor() {

    companion object {

        private val providers = LinkedHashMap<String, SkillTreeNodeProvider>()

        init {
            register(SkillTreeSkillNodeProvider())
            register(AttributeSkillTreeNodeProvider())
        }

        fun register(provider: SkillTreeNodeProvider) {
            val previous = providers.putIfAbsent(provider.id, provider)
            if (previous != null) {
                error("技能树节点 Provider 已注册: ${provider.id}")
            }
        }

        fun getOrNull(id: String): SkillTreeNodeProvider? {
            return providers[id]
        }
    }
}
