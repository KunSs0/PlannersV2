package com.gitee.planners.core.ui

import com.gitee.planners.api.KeyBindingAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.job.Skill
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.ui.BaseUI.Companion.setIcon
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.function.submitAsync
import taboolib.common.platform.function.warning
import taboolib.library.configuration.ConfigurationSection

object PlayerSkillUpgradeUI : AutomationBaseUI("skill-upgrade") {

    @Option("__option__.icon-current")
    val currentIcon = simpleConfigNodeTo<ConfigurationSection,Icon> {
        Icon(this)
    }

    @Option("__option__.title")
    val previewNextIcon = simpleConfigNodeTo<ConfigurationSection,Icon> {
        Icon(this)
    }

    override fun display(player: Player): BaseUI.Display {
        throw UnsupportedOperationException("Not yet implemented")
    }

    fun open(player: Player,skill: ImmutableSkill) {
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
                open(player,template.route!!,it)
            }
        }
    }

    fun open(player: Player,skill: PlayerSkill): BaseUI {
        return open(player,player.plannersTemplate.route!!,skill)
    }

    fun open(player: Player,route: PlayerRoute,skill: PlayerSkill): BaseUI {
        val template = player.plannersTemplate
        return BaseUI.createBaseUI {
            BaseUI.chest(PlayerSkillOperatorUI) {
                setIcon(currentIcon.get(),KeyBindingAPI.createIconFormatter(player, skill).build()) {

                }

                if (skill.level < skill.immutable.maxLevel) {
                    setIcon(previewNextIcon.get(),KeyBindingAPI.createIconFormatter(player, skill,skill.level + 1).build()) {
                        // 点击后检查升级条件
                        player.sendMessage("===")
                    }
                }
            }
        }
    }


}
