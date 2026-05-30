package com.gitee.planners.module.script.bridge;

import com.gitee.planners.api.PlayerTemplateAPI;
import com.gitee.planners.core.player.PlayerRoute;
import com.gitee.planners.core.player.PlayerTemplate;
import com.gitee.planners.core.skill.SkillPointsManager;
import org.bukkit.entity.Player;

/**
 * 注入 JS 的 player 桥接对象。
 * <p>
 * 通过 {@code ScriptOptions.set("player", bridge)} 注入，
 * JS 中可访问 {@code player.level}、{@code player.availablePoints()} 等属性和方法。
 * <p>
 * Nashorn 通过 JavaBean 命名约定访问（getXxx → player.xxx），
 * GraalJS 通过 HostAccess.ALL 访问。
 */
public final class PlayerBridge {

    private final Player player;
    private PlayerTemplate template;
    private PlayerRoute route;

    public PlayerBridge(Player player) {
        this.player = player;
    }

    // ---- 属性（Nashorn: getXxx() → player.xxx） ----

    /** 玩家等级 */
    public int getLevel() {
        return template().getLevel();
    }

    // ---- 查询方法 ----

    /** 当前可用技能点 */
    public int availablePoints() {
        PlayerRoute route = route();
        return route != null ? SkillPointsManager.INSTANCE.getAvailable(route) : 0;
    }

    /** 已学习技能等级（未学习返回 0） */
    public int getSkillLevel(String skillId) {
        PlayerRoute route = route();
        if (route == null) return 0;
        var skill = route.getSkillOrNull(skillId);
        return skill != null ? skill.getLevel() : 0;
    }

    /** 当前职业 ID */
    public String getJob() {
        PlayerRoute route = route();
        return route != null ? route.getJob().getId() : "";
    }

    // ---- 消耗方法 ----

    /** 扣除技能点 */
    public boolean takePoints(int amount) {
        PlayerRoute route = route();
        return route != null && SkillPointsManager.INSTANCE.takePoints(route, amount);
    }

    // ---- 占位方法（后续对接经济/背包） ----

    /** 金币余额（占位） */
    public long currency() {
        throw new UnsupportedOperationException("currency not implemented yet");
    }

    /** 扣除金币（占位） */
    public boolean takeCurrency(int amount) {
        throw new UnsupportedOperationException("takeCurrency not implemented yet");
    }

    /** 背包持有物品（占位） */
    public boolean hasItem(String itemId, int count) {
        throw new UnsupportedOperationException("hasItem not implemented yet");
    }

    /** 扣除物品（占位） */
    public boolean takeItem(String itemId, int count) {
        throw new UnsupportedOperationException("takeItem not implemented yet");
    }

    // ---- 内部 ----

    private PlayerTemplate template() {
        if (template == null) {
            template = PlayerTemplateAPI.INSTANCE.getPlannersTemplate(player);
        }
        return template;
    }

    private PlayerRoute route() {
        if (route == null) {
            PlayerTemplate t = template();
            route = t != null ? t.getRoute() : null;
        }
        return route;
    }
}
