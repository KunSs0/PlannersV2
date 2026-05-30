package com.gitee.planners.core.ui

import com.gitee.planners.api.KeyBindingAPI
import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.ui.BaseUI.Companion.setIcon
import org.bukkit.entity.Player
import taboolib.common.platform.function.submitAsync
import taboolib.common.platform.function.warning
import taboolib.library.configuration.ConfigurationSection
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

                setIcon(submitIcon.get(), submitIcon.get().icon) {
                    // 满级后不可升级
                    if (skill.level >= skill.immutable.maxLevel) {
                        player.sendLang("skill-upgrade-failed")
                        return@setIcon
                    }
                    // 升级条件由技能树 upgrade() 校验
                    process()
                }

                onBuild { player, inventory ->
                    setDecorateIcon(decorateIcon.get(), inventory)
                }

            }

        }
    }

}
