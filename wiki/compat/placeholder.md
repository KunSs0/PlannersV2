# PlaceholderAPI

Planners 使用固定的字面量占位符协议，不执行占位符参数中的 JavaScript。

占位符格式为 `%planners_查询名[:参数...]%`。带参数的查询使用冒号分隔，因此技能、职业和状态 ID 可以包含下划线。

| 分类 | 占位符 |
| --- | --- |
| 档案 | `%planners_profile_loaded%` |
| 等级与经验 | `%planners_level%`、`%planners_level_min%`、`%planners_level_max%`、`%planners_experience%`、`%planners_experience_max%`、`%planners_experience_remaining%`、`%planners_experience_percent%` |
| 魔法 | `%planners_magic%`、`%planners_magic_max%`、`%planners_magic_percent%` |
| 路由与职业 | `%planners_router_id%`、`%planners_router_name%`、`%planners_job_id%`、`%planners_job_name%` |
| 技能点 | `%planners_skill_points%`、`%planners_skill_points_used%`、`%planners_skill_points_total%`、`%planners_skill_count%` |
| 背包 | `%planners_backpack_page%`、`%planners_backpack_page_name%` |
| 技能 | `%planners_skill_name:heavy_slash%`、`%planners_skill_level:heavy_slash%`、`%planners_skill_max_level:heavy_slash%`、`%planners_skill_learned:heavy_slash%`、`%planners_skill_equipped:heavy_slash%`、`%planners_skill_page:heavy_slash%`、`%planners_skill_slot:heavy_slash%`、`%planners_skill_cooldown:heavy_slash%` |
| 槽位 | `%planners_backpack_skill_id:0:slot0%`、`%planners_backpack_skill_name:0:slot0%`、`%planners_backpack_skill_level:0:slot0%` |
| 属性 | `%planners_attribute:STR%` |
| 状态 | `%planners_state_active:berserk%`、`%planners_state_layer:berserk%`、`%planners_state_remaining:berserk%` |

百分比统一返回 `0` 至 `100` 的整数。`skill_cooldown` 和 `state_remaining` 的单位为 tick。无玩家上下文、档案未加载、未知查询或不合法参数返回空字符串；未学习技能的 `skill_level` 返回 `0`，未生效状态的 `state_layer` 和 `state_remaining` 返回 `0`。
