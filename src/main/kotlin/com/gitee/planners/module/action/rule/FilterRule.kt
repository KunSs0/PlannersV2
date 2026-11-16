package com.gitee.planners.module.action.rule

import com.gitee.planners.module.action.ActionArguments
import com.gitee.planners.module.action.ActionScope
import com.gitee.planners.module.action.ActionTarget
import com.gitee.planners.module.action.query.QueryScope

/**
 * 过滤规则。
 *
 * 规则是「逐元素判据」的唯一定义位置：只判断单个目标是否保留，不排序、不截断、
 * 不合并、不改变元素顺序，也不产生任何副作用。需要改容器形态的能力请继续使用目标器。
 *
 * 规则同时拿到执行上下文与候选元素：施法者侧的数据（技能变量、队伍、标记）从
 * [scope] 读取，被判定对象只从 [target] 读取，避免「上下文主体」与「候选对象」互相污染。
 */
interface FilterRule {

    /**
     * 规则主键，形如 `health.damaged`，小写且按域分组。
     */
    val id: String

    /**
     * 允许出现的上下文，缺省为技能。
     */
    val scope: ActionScope
        get() {
            return ActionScope.SKILL
        }

    /**
     * 加载期参数校验。
     *
     * 只校验本规则自己的参数；未知键由实现自行拒绝，报错路径指回原配置行。
     *
     * @param arguments 规则参数，已剔除外层 `r`。
     */
    fun validate(arguments: ActionArguments) {
    }

    /**
     * 加载期编译。
     *
     * 需要缓存解析结果时在此返回，运行期每次求值都会原样传回 [test]；
     * 不需要缓存时返回 null。
     *
     * @param arguments 规则参数，已剔除外层 `r`。
     * @return 编译结果。
     */
    fun compile(arguments: ActionArguments): Any? {
        return null
    }

    /**
     * 判断单个目标是否保留。
     *
     * @param scope 当前执行上下文，读取施法者侧数据。
     * @param target 候选目标。
     * @param arguments 规则参数，已剔除外层 `r`。
     * @param compiled [compile] 的返回值。
     * @return 保留时返回 true。
     */
    fun test(scope: QueryScope, target: ActionTarget, arguments: ActionArguments, compiled: Any?): Boolean
}
