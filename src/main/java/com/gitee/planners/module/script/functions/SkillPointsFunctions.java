package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.PlayerTemplateAPI;
import com.gitee.planners.core.player.PlayerRouter;
import com.gitee.planners.core.player.PlayerTemplate;
import com.gitee.planners.core.skill.SkillPointsManager;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;
import org.bukkit.entity.Player;

/**
 * 技能点系统函数
 * <pre>{@code
 * // JS: availablePoints()  或  availablePoints(player)
 * // JS: takePoints(n)      或  takePoints(n, player)
 * }</pre>
 */
public final class SkillPointsFunctions {

    private SkillPointsFunctions() {}

    public static void register() {
        // availablePoints() 或 availablePoints(player)
        GlobalFunctions.register("availablePoints", args -> {
            Player player = ScriptArgs.getPlayer(args, 0);
            if (player == null) return 0;
            PlayerTemplate template = PlayerTemplateAPI.INSTANCE.getPlannersTemplate(player);
            PlayerRouter router = template.getPlayerRouter();
            return router != null ? SkillPointsManager.INSTANCE.getAvailable(router) : 0;
        });

        // takePoints(amount) 或 takePoints(amount, player)
        GlobalFunctions.register("takePoints", args -> {
            int amount = ScriptArgs.getInt(args, 0);
            Player player = ScriptArgs.getPlayer(args, 1);
            if (player == null) return false;
            PlayerTemplate template = PlayerTemplateAPI.INSTANCE.getPlannersTemplate(player);
            PlayerRouter router = template.getPlayerRouter();
            return router != null && SkillPointsManager.INSTANCE.takePoints(router, amount);
        });
    }
}
