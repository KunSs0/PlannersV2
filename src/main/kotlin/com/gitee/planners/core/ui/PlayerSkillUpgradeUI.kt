package com.gitee.planners.core.ui

import com.gitee.planners.api.KeyBindingAPI
import com.gitee.planners.api.PlannersAPI
import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.api.template.ProfileOperator
import com.gitee.planners.api.template.ProfileOperatorImpl
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.ui.BaseUI.Companion.setIcon
import com.gitee.planners.module.currency.Currencies
import com.gitee.planners.module.kether.context.ImmutableSkillContext
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.function.submitAsync
import taboolib.common.platform.function.warning
import taboolib.common.util.replaceWithOrder
import taboolib.common5.cdouble
import taboolib.common5.cint
import taboolib.library.configuration.ConfigurationSection
import taboolib.platform.util.buildItem
import taboolib.platform.util.sendLang

object PlayerSkillUpgradeUI : AutomationBaseUI("skill-upgrade.yml") {

    @Option("__option__.icon-current")
    val currentIcon = simpleConfigNodeTo<ConfigurationSection, Icon> {
        Icon(this)
    }

    @Option("__option__.icon-preview-next")
    val previewNextIcon = simpleConfigNodeTo<ConfigurationSection, Icon> {
        Icon(this)
    }

    @Option("__option__.icon-submit")
    val submitIcon = simpleConfigNodeTo<ConfigurationSection, Icon> {
        Icon(this)
    }

    @Option("__option__.icon-submit.format.condition")
    val submitIconConditionFormatter = simpleConfigNodeTo<Any, String> {
        this.toString()
    }

    override fun display(player: Player): BaseUI.Display {
        throw UnsupportedOperationException("Not yet implemented")
    }

    fun open(player: Player, skill: ImmutableSkill) {
        val template = player.plannersTemplate
        if (template.route == null) {
            warning("Player ${player.name} route not found")
            return
        }

        if (!template.route!!.hasImmutableSkill(skill.id)) {
            warning("Skill ${skill.id} not found in route ${template.route!!.name}")
            return
        }
        submitAsync {
            template.getSkill(skill).thenAccept {
                open(player, template.route!!, it)
            }.exceptionally {
                it.printStackTrace()
                null
            }
        }
    }

    fun open(player: Player, skill: PlayerSkill) {
        open(player, player.plannersTemplate.route!!, skill)
    }

    fun open(player: Player, route: PlayerRoute, skill: PlayerSkill) {
        createUI(player, route, skill).openTo(player)
    }

    fun createUI(player: Player, route: PlayerRoute, skill: PlayerSkill): BaseUI {
        val template = player.plannersTemplate

        fun process() {

            PlayerTemplateAPI.setSkillLevel(template, skill, skill.level + 1)
            player.sendLang("skill-upgrade-success", skill.level)
            open(player, route, skill)

        }

        return BaseUI.createBaseUI {
            BaseUI.chest(PlayerSkillOperatorUI) {
                setIcon(currentIcon.get(), KeyBindingAPI.createIconFormatter(player, skill).build()) {

                }

                if (skill.level < skill.immutable.maxLevel) {
                    setIcon(
                        previewNextIcon.get(),
                        KeyBindingAPI.createIconFormatter(player, skill, skill.level + 1).build()
                    ) {
                        // 点击后检查升级条件
                        player.sendMessage("===")
                    }
                }

                setIcon(submitIcon.get(), buildSubmitIcon(player, skill)) {
                    // 满级后不可升级
                    if (skill.level >= skill.immutable.maxLevel) {
                        player.sendLang("skill-upgrade-failed")
                        return@setIcon
                    }
                    // 点击后检查升级条件
                    val condition = skill.immutable.getConditionAsUpgrade(skill.level)
                    // 如果没有条件直接升级
                    // 如果满足条件直接升级
                    if (condition == null) {
                        process()
                        return@setIcon
                    }
                    if (checkCondition(player, skill, condition)) {
                        executeConditionOnCallback(player, skill, condition)
                        process()
                    }
                    // 不满足条件
                    else {
                        player.sendLang("skill-upgrade-failed")
                    }

                }

                onBuild { player, inventory ->
                    setDecorateIcon(decorateIcon.get(), inventory)
                }

            }

        }
    }

    /**
     * 处理升级条件后的回调
     *
     * @param player 玩家
     * @param skill 技能
     * @param condition 条件
     */
    fun executeConditionOnCallback(player: Player, skill: PlayerSkill, condition: ImmutableSkill.IndexedUpgrade) {
        val ctx = ImmutableSkillContext(adaptTarget(player), skill.immutable, skill.level)
        condition.args.forEach {
            val currency = Currencies.getInstance(it.key)
            if (currency == null) {
                warning("Unsupported currency ${it.key}")
                return@forEach
            }
            val data = it.value.get(ctx.optionsBuilder()) { it.cdouble }
            currency.take(player, data)
        }
    }

    /**
     * 检查升级条件
     *
     * @param player 玩家
     * @param skill 技能
     * @param condition 条件
     *
     * @return 是否满足条件
     */
    fun checkCondition(player: Player, skill: PlayerSkill, condition: ImmutableSkill.IndexedUpgrade): Boolean {
        val context = ImmutableSkillContext(adaptTarget(player), skill.immutable, skill.level)

        return condition.args.all { (node, amount) ->
            val currency = Currencies.getInstance(node)
            if (currency == null) {
                warning("Unsupported currency $node")
                return@all false
            }
            val data = amount.get(context.optionsBuilder()) { it.cint }
            currency.get(player) >= data
        }
    }

    fun buildSubmitIcon(player: Player, skill: PlayerSkill): ItemStack {
        val condition = skill.immutable.getConditionAsUpgrade(skill.level)
        if (condition == null) {
            return submitIcon.get().icon
        }
        val context = ImmutableSkillContext(adaptTarget(player), skill.immutable, skill.level)
        return buildItem(submitIcon.get().icon) {
            val newList = mutableListOf<String>()
            // 渲染${condition} 为 条件列表
            this.lore.forEach formatLine@{ row ->
                if (row.contains("\${condition}")) {
                    condition.args.forEach { (node, amount) ->
                        val currency = Currencies.getInstance(node)
                        if (currency == null) {
                            newList += "Unsupported currency $node"
                            return@formatLine
                        }
                        val data = amount.get(context.optionsBuilder()) { it.cint }
                        // 渲染变量
                        newList += submitIconConditionFormatter.get()
                            .replaceWithOrder(currency.name, currency.get(player), data)
                    }
                } else {
                    newList.add(row)
                }
            }
            // 装载新的lore
            this.lore.clear()
            this.lore.addAll(newList)
            colored()
        }
    }


}
