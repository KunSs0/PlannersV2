package com.gitee.planners.module.action

/**
 * 内置动作、条件、目标器与触发器的安装入口。
 *
 * 本类只做登记；具体实现按类别分文件维护，避免单文件过大。
 */
object ActionBuiltins {

    /**
     * 安装全部内置条目。
     */
    fun install() {
        installActions()
        installTargeters()
        installTriggers()
        installConditions()
    }

    /** 登记动作。 */
    private fun installActions() {
        ActionRegistry.register(BuiltinActions.Message)
        ActionRegistry.register(BuiltinActions.Command)
        ActionRegistry.register(BuiltinActions.Log)
        ActionRegistry.register(BuiltinActions.Call)
        ActionRegistry.register(BuiltinActions.Delay)
        ActionRegistry.register(BuiltinActions.TargetFinderSet)
        ActionRegistry.register(FinderSetAction)
        ActionRegistry.register(TargetCountAction)
        ActionRegistry.register(PlannersActions.Cooldown)
        ActionRegistry.register(PlannersActions.Experience)
        ActionRegistry.register(PlannersActions.StateAttach)
        ActionRegistry.register(PlannersActions.StateDetach)
        ActionRegistry.register(PlannersActions.MetaSet)
        ActionRegistry.register(PlannersActions.MetaGet)
        ActionRegistry.register(PlannersActions.MetaClear)
        ActionRegistry.register(PlannersCooldownActions.Arm)
        ActionRegistry.register(PlannersCurrencyActions.Currency)
        ActionRegistry.register(PlannersEffectActions.Spawn)
        ActionRegistry.register(TimerStartAction)
        ActionRegistry.register(AreaStartAction)
        ActionRegistry.register(AreaStopAction)
        ActionRegistry.register(TimerStopAction)
    }

    /** 登记目标器。 */
    private fun installTargeters() {
        ActionRegistry.register(BuiltinTargeters.Sender)
        ActionRegistry.register(BuiltinTargeters.Wounded)
        ActionRegistry.register(UuidTargets)
        ActionRegistry.register(ForwardTargeter)
        ActionRegistry.register(BuiltinTargeters.SenderLocation)
        ActionRegistry.register(BuiltinTargeters.Current)
        ActionRegistry.register(BuiltinTargeters.Range)
        ActionRegistry.register(BuiltinTargeters.Sector)
        ActionRegistry.register(BuiltinTargeters.Rect)
        ActionRegistry.register(BuiltinTargeters.Source)
        ActionRegistry.register(BuiltinTargeters.TargetFinder)
        ActionRegistry.register(BuiltinTargeters.Crosshair)
        ActionRegistry.register(BuiltinTargeters.Enemy)
        ActionRegistry.register(BuiltinTargeters.Ally)
        ActionRegistry.register(BuiltinTargeters.Type)
        ActionRegistry.register(BuiltinTargeters.ExcludeType)
        ActionRegistry.register(BuiltinTargeters.Name)
        ActionRegistry.register(BuiltinTargeters.World)
        ActionRegistry.register(BuiltinTargeters.IncludeSelf)
        ActionRegistry.register(BuiltinTargeters.Alive)
        ActionRegistry.register(BuiltinTargeters.Sort)
        ActionRegistry.register(BuiltinTargeters.Reverse)
        ActionRegistry.register(BuiltinTargeters.Shuffle)
        ActionRegistry.register(BuiltinTargeters.Limit)
        ActionRegistry.register(BuiltinTargeters.OnlinePlayers)
        ActionRegistry.register(BuiltinTargeters.Attacker)
        ActionRegistry.register(BuiltinTargeters.Defender)
    }

    /** 登记触发器。 */
    private fun installTriggers() {
        ActionRegistry.register(BuiltinTriggers.Timer)
        ActionRegistry.register(ActionTriggers.SkillCast)
        ActionRegistry.register(ActionTriggers.SkillCooldown)
        ActionRegistry.register(ActionTriggers.SkillLearn)
        ActionRegistry.register(ActionTriggers.SkillLevel)
        ActionRegistry.register(ActionTriggers.SkillNode)
        ActionRegistry.register(ActionTriggers.Level)
        ActionRegistry.register(ActionTriggers.Experience)
        ActionRegistry.register(ActionTriggers.MagicPoint)
        ActionRegistry.register(ActionTriggers.TargetCaptured)
        ActionRegistry.register(ActionTriggers.PlayerDamageEntity)
        ActionRegistry.register(ActionTriggers.RouteCleared)
        ActionRegistry.register(ActionTriggers.Quit)
    }

    /** 登记条件。 */
    private fun installConditions() {
        ActionRegistry.register(BuiltinConditions.Expression)
        ActionRegistry.register(BuiltinConditions.Chance)
        ActionRegistry.register(BuiltinOnceCondition)
        ActionRegistry.register(PlannersConditions.Node)
        ActionRegistry.register(PlannersConditions.Meta)
        ActionRegistry.register(PlannersConditions.Level)
        ActionRegistry.register(PlannersConditions.Job)
        ActionRegistry.register(PlannersConditions.Wounded)
        ActionRegistry.register(AreaContainsCondition)
        ActionRegistry.register(PlannersCooldownConditions.Ready)
        ActionRegistry.register(PlannersCurrencyActions.CurrencyCondition)
    }
}
