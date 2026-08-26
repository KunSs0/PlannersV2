/**
 * Script Engine - Planners API
 *
 * 统一命名空间: PlannersJs
 *
 * 设计约定:
 * - 只向全局暴露 PlannersJs，避免 getSkill / cast 等通用方法名冲突。
 * - 回调第一个参数始终为 Java 原事件对象，第二个参数为普通 JS 快照。
 * - 需要修改事件字段时直接操作第一个 Java 事件对象，例如 event.setAmount(20)。
 * - 所有 Java 原对象方法均保留，快照方法用于日志、RPC、UI 数据源和 JSON.stringify。
 */
(function () {
    var root = typeof globalThis !== "undefined" ? globalThis : this;

    var PlannersAPI = Java.type("com.gitee.planners.api.PlannersAPI").INSTANCE;
    var PlayerTemplateAPI = Java.type("com.gitee.planners.api.PlayerTemplateAPI").INSTANCE;
    var BackpackAPI = Java.type("com.gitee.planners.api.BackpackAPI").INSTANCE;
    var KeyBindingAPI = Java.type("com.gitee.planners.api.KeyBindingAPI").INSTANCE;
    var Registries = Java.type("com.gitee.planners.api.Registries").INSTANCE;
    var ProxyTarget = Java.type("com.gitee.planners.api.job.target.ProxyTarget");
    var ProxyTargetContainer = Java.type("com.gitee.planners.api.job.target.ProxyTargetContainer");
    var ProxyBukkitEntity = Java.type("com.gitee.planners.api.job.target.ProxyTarget$BukkitEntity");
    var EntityStateManager = Java.type("com.gitee.planners.core.skill.entity.state.EntityStateManager").INSTANCE;
    var MagicPointProvider = Java.type("com.gitee.planners.core.player.magic.MagicPointProvider").Companion;
    var Cooler = Java.type("com.gitee.planners.core.skill.cooler.Cooler").Companion;
    var MetadataHelper = Java.type("com.gitee.planners.api.common.metadata.MetadataHelperKt");
    var DamageCause = Java.type("com.gitee.planners.api.damage.DamageCause");
    var ProxyDamage = Java.type("com.gitee.planners.api.damage.ProxyDamage");
    var Utils = root.ScriptEngineUtils;
    if (typeof Utils !== "object" || Utils == null) {
        throw new Error("PlannersJs requires api.utils.js to be loaded before planners.api.js");
    }
    var requireFunction = Utils.requireFunction;
    var toArray = Utils.toArray;
    var mapToObject = Utils.mapToObject;
    var jsObjectToJavaMap = Utils.jsObjectToJavaMap;
    var enumName = Utils.enumName;
    var callIfPresent = Utils.callIfPresent;
    var futureThen = Utils.futureThen;
    var toScriptObject = Utils.requireFunction("__toScriptObject");

    /**
     * 将已加载的 Bukkit 玩家或 PlayerTemplate 解析为玩家档案。
     *
     * 直接传入 PlayerTemplate 时不再查询加载状态；传入 Bukkit Player 时，
     * 只有 Planners 档案已加载才返回对应 PlayerTemplate。
     *
     * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate|null} templateOrPlayer 玩家、玩家档案或 null。
     * @returns {com.gitee.planners.core.player.PlayerTemplate|null} 已加载的档案；参数为空或玩家档案未加载时返回 null。
     */
    function getLoadedTemplateOrNull(templateOrPlayer) {
        if (templateOrPlayer == null) {
            return null;
        }
        if (typeof templateOrPlayer.getOnlinePlayer === "function") {
            return templateOrPlayer;
        }
        if (!PlayerTemplateAPI.getPlannersLoaded(templateOrPlayer)) {
            return null;
        }
        return PlayerTemplateAPI.getPlannersTemplate(templateOrPlayer);
    }

    /**
     * 获取玩家的转职线聚合对象。
     *
     * PlayerRouter 归属整条转职线，保存当前 PlayerRoute、根到当前阶段的
     * 路径、全线已学习技能和共享 SP；它不是单个 Job 阶段。
     *
     * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate|null} templateOrPlayer 玩家、玩家档案或 null。
     * @returns {com.gitee.planners.core.player.PlayerRouter|null} 已选择职业的转职线；档案未加载或尚未选择职业时返回 null。
     */
    function getRouterOrNull(templateOrPlayer) {
        var template = getLoadedTemplateOrNull(templateOrPlayer);
        if (template == null) {
            return null;
        }
        return template.getPlayerRouter();
    }

    /**
     * 将冷却 API 的技能参数解析为 PlayerSkill。
     *
     * 字符串 ID 只会从该玩家已注册的技能中解析，不能传 ImmutableSkill 的
     * 配置 ID 代替 PlayerSkill。直接传对象时，调用方必须传入 PlayerSkill。
     *
     * @param {org.bukkit.entity.Player|null} player Bukkit 玩家。
     * @param {com.gitee.planners.core.player.PlayerSkill|string|null} skillOrId 玩家技能实例或其 ID。
     * @returns {com.gitee.planners.core.player.PlayerSkill|null} 可用于冷却读写的玩家技能；玩家未加载、技能未注册或参数为空时返回 null。
     */
    function resolveCooldownSkill(player, skillOrId) {
        if (player == null || skillOrId == null) {
            return null;
        }
        if (typeof skillOrId !== "string") {
            return skillOrId;
        }
        var template = getLoadedTemplateOrNull(player);
        if (template == null) {
            return null;
        }
        return template.getRegisteredSkillOrNull(String(skillOrId));
    }

    /**
     * 将元数据目标解析为 Planners MetadataContainer。
     *
     * 已实现 getMetadata/setMetadata 的对象直接使用；Bukkit Entity 会包装为
     * ProxyTarget.BukkitEntity；其余 Player 参数会解析为已加载 PlayerTemplate。
     *
     * @param {org.bukkit.entity.Entity|org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate|com.gitee.planners.api.common.metadata.MetadataContainer|null} target 元数据持有者。
     * @returns {com.gitee.planners.api.common.metadata.MetadataContainer|null} 元数据容器；目标为空或玩家档案未加载时返回 null。
     */
    function resolveMetadataContainer(target) {
        if (target == null) {
            return null;
        }
        if (typeof target.getMetadata === "function" && typeof target.setMetadata === "function") {
            return target;
        }
        if (typeof target.getUniqueId === "function" && typeof target.getWorld === "function") {
            return new ProxyBukkitEntity(target);
        }
        return getLoadedTemplateOrNull(target);
    }

    /**
     * Planners 公开事件全限定类名表。
     *
     * 嵌套 Kotlin/Java 类在 JVM 中使用 `$` 分隔，例如 PlayerSkillCastEvent$Pre。
     *
     * @type {Object<string, string>}
     */
    var EVENTS = {
        /** 数据库实现初始化事件。 */
        DATABASE_INIT: "com.gitee.planners.api.event.DatabaseInitEvent",
        /** 插件 reload 前事件。 */
        PLUGIN_RELOAD_PRE: "com.gitee.planners.api.event.PluginReloadEvents$Pre",
        /** 插件 reload 后事件。 */
        PLUGIN_RELOAD_POST: "com.gitee.planners.api.event.PluginReloadEvents$Post",
        /** 代理客户端按键抬起事件。 */
        CLIENT_KEY_UP: "com.gitee.planners.api.event.ProxyClientKeyEvents$Up",
        /** 代理客户端按键按下事件。 */
        CLIENT_KEY_DOWN: "com.gitee.planners.api.event.ProxyClientKeyEvents$Down",

        /** 连招输入命中事件。 */
        COMBINED_PRESS_IN: "com.gitee.planners.api.event.action.CombinedEvent$PressIn",
        /** 连招开始事件。 */
        COMBINED_BEGIN: "com.gitee.planners.api.event.action.CombinedEvent$Begin",
        /** 连招关闭事件。 */
        COMBINED_CLOSE: "com.gitee.planners.api.event.action.CombinedEvent$Close",

        /** 实体应用模型事件。 */
        ENTITY_MODEL_APPLY: "com.gitee.planners.api.event.entity.EntityModelApplyEvent",
        /** 状态附加到实体前事件，可取消。 */
        ENTITY_STATE_ATTACH_PRE: "com.gitee.planners.api.event.entity.EntityStateEvent$Attach$Pre",
        /** 状态附加到实体后事件。 */
        ENTITY_STATE_ATTACH_POST: "com.gitee.planners.api.event.entity.EntityStateEvent$Attach$Post",
        /** 状态从实体移除前事件，可取消。 */
        ENTITY_STATE_DETACH_PRE: "com.gitee.planners.api.event.entity.EntityStateEvent$Detach$Pre",
        /** 状态从实体移除后事件。 */
        ENTITY_STATE_DETACH_POST: "com.gitee.planners.api.event.entity.EntityStateEvent$Detach$Post",
        /** 状态首层挂载前事件，可取消。 */
        ENTITY_STATE_MOUNT_PRE: "com.gitee.planners.api.event.entity.EntityStateEvent$Mount$Pre",
        /** 状态首层挂载后事件。 */
        ENTITY_STATE_MOUNT_POST: "com.gitee.planners.api.event.entity.EntityStateEvent$Mount$Post",
        /** 状态全部关闭前事件，可取消。 */
        ENTITY_STATE_CLOSE_PRE: "com.gitee.planners.api.event.entity.EntityStateEvent$Close$Pre",
        /** 状态全部关闭后事件。 */
        ENTITY_STATE_CLOSE_POST: "com.gitee.planners.api.event.entity.EntityStateEvent$Close$Post",
        /** 状态自然到期事件。 */
        ENTITY_STATE_END: "com.gitee.planners.api.event.entity.EntityStateEvent$End",

        /** 背包装备技能事件。 */
        BACKPACK_EQUIP: "com.gitee.planners.api.event.player.BackpackEquipEvent$Equip",
        /** 技能装配成功事件。 */
        BACKPACK_EQUIP_POST: "com.gitee.planners.api.event.player.BackpackEquipEvent$Post",
        /** 背包卸下技能事件。 */
        BACKPACK_UNEQUIP: "com.gitee.planners.api.event.player.BackpackEquipEvent$Unequip",
        /** 背包页面切换前事件，可取消。 */
        BACKPACK_PAGE_SWITCH_PRE: "com.gitee.planners.api.event.player.BackpackPageSwitchEvent$Pre",
        /** 背包页面切换后事件。 */
        BACKPACK_PAGE_SWITCH_POST: "com.gitee.planners.api.event.player.BackpackPageSwitchEvent$Post",
        /** 玩家伤害实体事件。 */
        PLAYER_DAMAGE_ENTITY: "com.gitee.planners.api.event.player.PlayerDamageEntityEvent",
        /** 玩家经验增加事件。 */
        PLAYER_EXPERIENCE_INCREMENT: "com.gitee.planners.api.event.player.PlayerExperienceEvent$Increment",
        /** 玩家经验减少事件。 */
        PLAYER_EXPERIENCE_DECREMENT: "com.gitee.planners.api.event.player.PlayerExperienceEvent$Decrement",
        /** 玩家经验设置事件。 */
        PLAYER_EXPERIENCE_SET: "com.gitee.planners.api.event.player.PlayerExperienceEvent$Set",
        /** 玩家经验更新完成事件。 */
        PLAYER_EXPERIENCE_UPDATED: "com.gitee.planners.api.event.player.PlayerExperienceEvent$Updated",
        /** 玩家等级变化事件。 */
        PLAYER_LEVEL_CHANGE: "com.gitee.planners.api.event.player.PlayerLevelChangeEvent",
        /** 玩家魔法点增加事件。 */
        PLAYER_MAGIC_POINT_INCREASE: "com.gitee.planners.api.event.player.PlayerMagicPointEvent$Increase",
        /** 玩家魔法点减少事件。 */
        PLAYER_MAGIC_POINT_DECREASE: "com.gitee.planners.api.event.player.PlayerMagicPointEvent$Decrease",
        /** 玩家魔法点设置事件。 */
        PLAYER_MAGIC_POINT_SET: "com.gitee.planners.api.event.player.PlayerMagicPointEvent$Set",
        /** 玩家档案加载完成事件。 */
        PLAYER_PROFILE_LOADED: "com.gitee.planners.api.event.player.PlayerProfileLoadedEvent",
        /** 玩家路线设置前事件，可取消。 */
        PLAYER_SET_ROUTE_PRE: "com.gitee.planners.api.event.player.PlayerSetRouteEvent$Pre",
        /** 玩家路线设置后事件。 */
        PLAYER_SET_ROUTE_POST: "com.gitee.planners.api.event.player.PlayerSetRouteEvent$Post",
        /** 技能输入检查事件，可取消。 */
        PLAYER_SKILL_CAST_CHECK: "com.gitee.planners.api.event.player.PlayerSkillCastEvent$Check",
        /** 技能释放前事件，可取消。 */
        PLAYER_SKILL_CAST_PRE: "com.gitee.planners.api.event.player.PlayerSkillCastEvent$Pre",
        /** 技能释放后事件。 */
        PLAYER_SKILL_CAST_POST: "com.gitee.planners.api.event.player.PlayerSkillCastEvent$Post",
        /** 技能冷却设置事件。 */
        PLAYER_SKILL_COOLDOWN_SET: "com.gitee.planners.api.event.player.PlayerSkillCooldownEvent$Set",
        /** 玩家技能等级变化事件。 */
        PLAYER_SKILL_LEVEL_CHANGE: "com.gitee.planners.api.event.player.PlayerSkillEvent$LevelChange",
        /** 玩家首次学习技能成功事件。 */
        PLAYER_SKILL_LEARN: "com.gitee.planners.api.event.player.PlayerSkillEvent$Learn",
        /** 技能目标捕获事件。 */
        TARGET_CAPTURED: "com.gitee.planners.api.event.player.TargetCapturedEvent",

        /** 脚本自定义触发事件。 */
        SCRIPT_CUSTOM_TRIGGER: "com.gitee.planners.api.event.script.ScriptCustomTriggerEvent"
    };

    /**
     * 将 Bukkit Location 转为普通 JS 快照。
     *
     * @param {org.bukkit.Location|null} location Bukkit 位置。
     * @returns {Object|null} 位置快照，包含 world/x/y/z/yaw/pitch。
     */
    function toLocationData(location) {
        if (location == null) {
            return null;
        }
        var world = location.getWorld();
        var worldName = null;
        if (world != null) {
            worldName = world.getName();
        }
        return {
            world: worldName,
            x: Number(location.getX()),
            y: Number(location.getY()),
            z: Number(location.getZ()),
            yaw: Number(location.getYaw()),
            pitch: Number(location.getPitch())
        };
    }

    /**
     * 将 Bukkit Entity 转为普通 JS 快照。
     *
     * @param {org.bukkit.entity.Entity|null} entity Bukkit 实体。
     * @returns {Object|null} 实体快照，包含 name/uuid/type/world/location/valid。
     */
    function toEntityData(entity) {
        if (entity == null) {
            return null;
        }
        var location = null;
        if (typeof entity.getLocation === "function") {
            location = entity.getLocation();
        }
        return {
            name: callIfPresent(entity, "getName"),
            uuid: String(entity.getUniqueId()),
            type: enumName(entity.getType()),
            world: entity.getWorld().getName(),
            location: toLocationData(location),
            valid: entity.isValid()
        };
    }

    /**
     * 将 Bukkit Player 转为普通 JS 快照。
     *
     * @param {org.bukkit.entity.Player|null} player Bukkit 玩家。
     * @returns {Object|null} 玩家快照，额外包含 displayName/online。
     */
    function toPlayerData(player) {
        if (player == null) {
            return null;
        }
        var data = toEntityData(player);
        data.name = player.getName();
        data.displayName = player.getDisplayName();
        data.online = player.isOnline();
        return data;
    }

    /**
     * 将实现 Unique 的 Java 对象转为 ID 快照。
     *
     * @param {com.gitee.planners.api.common.Unique|null} unique Unique 对象。
     * @returns {Object|null} 形如 { id: string } 的快照。
     */
    function toUniqueData(unique) {
        if (unique == null) {
            return null;
        }
        return {
            id: unique.getId ? unique.getId() : String(unique)
        };
    }

    /**
     * 将 Planners State 转为普通 JS 快照。
     *
     * @param {com.gitee.planners.core.config.State|null} state 状态定义。
     * @returns {Object|null} 状态快照。
     */
    function toStateData(state) {
        if (state == null) {
            return null;
        }
        return {
            id: state.getId(),
            name: state.getName(),
            priority: Number(state.getPriority()),
            maxLayer: Number(state.getMaxLayer()),
            isStatic: state.isStatic(),
            action: state.getAction()
        };
    }

    /**
     * 将 ImmutableSkill 转为普通 JS 快照。
     *
     * @param {com.gitee.planners.core.config.ImmutableSkill|null} skill 技能配置对象。
     * @returns {Object|null} 技能配置快照。
     */
    function toImmutableSkillData(skill) {
        if (skill == null) {
            return null;
        }
        return {
            id: skill.getId(),
            name: skill.getName(),
            categories: toArray(skill.getCategories()),
            async: skill.getAsync(),
            startedLevel: Number(skill.getStartedLevel()),
            maxLevel: Number(skill.getMaxLevel()),
            attributes: mapToObject(skill.getAttributes()),
            variableIds: Object.keys(mapToObject(skill.getVariables())),
            hookIds: Object.keys(mapToObject(skill.getHooks()))
        };
    }

    /**
     * 将 PlayerSkill 转为普通 JS 快照。
     *
     * @param {com.gitee.planners.core.player.PlayerSkill|null} skill 玩家技能实例。
     * @returns {Object|null} 玩家技能快照。
     */
    function toPlayerSkillData(skill) {
        if (skill == null) {
            return null;
        }
        return {
            index: Number(skill.getIndex()),
            id: skill.getId(),
            name: skill.getName(),
            level: Number(skill.getLevel()),
            equipped: skill.getEquipped(),
            backpackPage: skill.getBackpackPage(),
            backpackSlot: skill.getBackpackSlot(),
            immutable: toImmutableSkillData(skill.getImmutable()),
            variableIds: Object.keys(mapToObject(skill.getVariables()))
        };
    }

    /**
     * 将 ImmutableJob 转为普通 JS 快照。
     *
     * @param {com.gitee.planners.core.config.ImmutableJob|null} job 职业配置对象。
     * @returns {Object|null} 职业快照。
     */
    function toJobData(job) {
        if (job == null) {
            return null;
        }
        return {
            id: job.getId(),
            name: job.getName(),
            attributes: mapToObject(job.getAttributes()),
            skillIds: toArray(job.getImmutableSkillValues()).map(function (skill) {
                return skill.getId();
            }),
            variableIds: Object.keys(mapToObject(job.getVariables()))
        };
    }

    /**
     * 将 ImmutableRoute 转为普通 JS 快照。
     *
     * @param {com.gitee.planners.core.config.ImmutableRoute|null} route 路线配置对象。
     * @returns {Object|null} 路线配置快照。
     */
    function toImmutableRouteData(route) {
        if (route == null) {
            return null;
        }
        return {
            routerId: route.getRouterId(),
            id: route.getId(),
            skillTreeIds: toArray(route.getSkillTreeIds()),
            branchIds: toArray(route.getBranches()).map(function (branch) {
                return branch.getId();
            }),
            job: toJobData(route.getJob())
        };
    }

    /**
     * 将 PlayerRoute 转为普通 JS 快照。
     *
     * @param {com.gitee.planners.core.player.PlayerRoute|null} route 玩家路线实例。
     * @returns {Object|null} 玩家路线快照。
     */
    function toPlayerRouteData(route) {
        if (route == null) {
            return null;
        }
        return {
            bindingId: Number(route.getBindingId()),
            routerId: route.getRouterId(),
            parentId: Number(route.getParentId()),
            jobId: route.getJobId(),
            name: route.getName(),
            job: toJobData(route.getJob()),
            skillTreeIds: toArray(route.getSkillTrees()).map(function (tree) {
                return tree.getId();
            }),
            registeredSkills: mapToObject(route.getRegisteredSkill(), toPlayerSkillData)
        };
    }

    /**
     * 将 PlayerRouter 转为普通 JS 快照。
     *
     * @param {com.gitee.planners.core.player.PlayerRouter|null} router 玩家路由等级实例。
     * @returns {Object|null} 玩家路由等级快照。
     */
    function toPlayerRouterData(router) {
        if (router == null) {
            return null;
        }
        var routeLine = toArray(router.getRouteLine());
        return {
            bindingId: Number(router.getBindingId()),
            userId: Number(router.getUserId()),
            routerId: router.getRouterId(),
            level: Number(router.getLevel()),
            experience: Number(router.getExperience()),
            minLevel: Number(router.getMinLevel()),
            maxLevel: Number(router.getMaxLevel()),
            currentRouteId: Number(router.getCurrentRouteId()),
            currentRoute: toPlayerRouteData(router.getCurrentRoute()),
            routeLine: routeLine.map(toPlayerRouteData),
            routes: toArray(router.getRoutes()).map(toPlayerRouteData),
            skillPointsCurrent: Number(router.getSkillPointsCurrent()),
            skillPointsUsed: Number(router.getSkillPointsUsed()),
            effectiveSkills: mapToObject(router.getEffectiveSkills(), toPlayerSkillData)
        };
    }

    /**
     * 将 PlayerTemplate 转为普通 JS 快照。
     *
     * @param {com.gitee.planners.core.player.PlayerTemplate|null} template 玩家档案。
     * @returns {Object|null} 玩家档案快照。
     */
    function toTemplateData(template) {
        if (template == null) {
            return null;
        }
        var playerRouter = template.getPlayerRouter();
        return {
            id: Number(template.getId()),
            player: toPlayerData(template.getOnlinePlayer()),
            level: Number(template.getLevel()),
            experience: Number(template.getExperience()),
            experienceMax: Number(template.getExperienceMax()),
            playerRouter: toPlayerRouterData(playerRouter)
        };
    }

    /**
     * 将 BackpackSlot 转为普通 JS 快照。
     *
     * @param {com.gitee.planners.core.config.BackpackSlot|null} slot 背包槽位配置。
     * @returns {Object|null} 背包槽位快照。
     */
    function toBackpackSlotData(slot) {
        if (slot == null) {
            return null;
        }
        return {
            id: slot.getId(),
            key: slot.getKey(),
            categories: toArray(slot.getCategories())
        };
    }

    /**
     * 将 BackpackPage 的槽位转为有序快照列表。
     *
     * @param {com.gitee.planners.core.config.BackpackPage|null} page 背包页面配置。
     * @returns {Object[]} 槽位快照列表。
     */
    function toBackpackSlotListData(page) {
        if (page == null) {
            return [];
        }
        var result = [];
        var iterator = page.getSlots().values().iterator();
        while (iterator.hasNext()) {
            result.push(toBackpackSlotData(iterator.next()));
        }
        return result;
    }

    /**
     * 将 BackpackPage 转为普通 JS 快照。
     *
     * @param {com.gitee.planners.core.config.BackpackPage|null} page 背包页面配置。
     * @returns {Object|null} 背包页面快照。
     */
    function toBackpackPageData(page) {
        if (page == null) {
            return null;
        }
        return {
            id: page.getId(),
            name: page.getName(),
            slots: mapToObject(page.getSlots(), toBackpackSlotData)
        };
    }

    /**
     * 将 BackpackConfig 转为普通 JS 快照。
     *
     * @param {com.gitee.planners.core.config.BackpackConfig|null} config 背包配置。
     * @returns {Object|null} 背包配置快照。
     */
    function toBackpackConfigData(config) {
        if (config == null) {
            return null;
        }
        return {
            defaultPage: config.getDefaultPage(),
            firstPageId: config.getFirstPageId(),
            pages: mapToObject(config.getPages(), toBackpackPageData)
        };
    }

    /**
     * 将 ProxyTarget 转为普通 JS 快照。
     *
     * @param {com.gitee.planners.api.job.target.ProxyTarget|null} target Planners 代理目标。
     * @returns {Object|null} 代理目标快照。
     */
    function toProxyTargetData(target) {
        if (target == null) {
            return null;
        }
        var instance = target.getInstance();
        var data = {
            type: target.getClass().getName(),
            instanceClass: instance == null ? null : instance.getClass().getName()
        };
        if (typeof target.getName === "function") {
            data.name = target.getName();
        }
        if (typeof target.getUniqueId === "function") {
            data.uuid = String(target.getUniqueId());
        }
        if (typeof target.getEntityType === "function") {
            data.entityType = enumName(target.getEntityType());
        }
        if (typeof target.getBukkitLocation === "function") {
            data.location = toLocationData(target.getBukkitLocation());
        }
        if (instance != null && typeof instance.getUniqueId === "function") {
            data.entity = toEntityData(instance);
        }
        return data;
    }

    /**
     * 将 ProxyTargetContainer 转为普通 JS 快照数组。
     *
     * @param {com.gitee.planners.api.job.target.ProxyTargetContainer|null} container 代理目标容器。
     * @returns {Object[]} 代理目标快照数组。
     */
    function toProxyTargetContainerData(container) {
        var targets = toArray(container);
        var result = [];
        for (var i = 0; i < targets.length; i++) {
            result.push(toProxyTargetData(targets[i]));
        }
        return result;
    }

    /**
     * 将 DamageCause 转为普通 JS 快照。
     *
     * @param {com.gitee.planners.api.damage.DamageCause|null} cause 伤害来源。
     * @returns {Object|null} 伤害来源快照。
     */
    function toDamageCauseData(cause) {
        if (cause == null) {
            return null;
        }
        return {
            name: cause.getName(),
            type: cause.getClass().getName()
        };
    }

    /**
     * 将 ProxyDamage 转为普通 JS 快照。
     *
     * @param {com.gitee.planners.api.damage.ProxyDamage|null} proxyDamage 伤害代理对象。
     * @returns {Object|null} 伤害代理快照。
     */
    function toProxyDamageData(proxyDamage) {
        if (proxyDamage == null) {
            return null;
        }
        return {
            source: toEntityData(proxyDamage.getSource()),
            target: toEntityData(proxyDamage.getTarget()),
            baseDamage: Number(proxyDamage.getBaseDamage()),
            finalDamage: Number(proxyDamage.getFinalDamage()),
            cancelled: proxyDamage.isCancelled(),
            cause: toDamageCauseData(proxyDamage.getCause()),
            metadata: mapToObject(proxyDamage.getMetadata())
        };
    }

    /**
     * 将 Planners / Bukkit 事件转为普通 JS 快照。
     *
     * 转换器会按事件实际可用 getter 提取字段，因此不同事件会拥有不同字段。
     * 需要修改事件时请使用原始 Java 事件对象，而不是修改快照。
     *
     * @param {taboolib.platform.type.BukkitProxyEvent|null} event 原始事件对象。
     * @returns {Object|null} 事件快照。
     */
    function toEventData(event) {
        if (event == null) {
            return null;
        }
        var data = {
            event: event.getClass().getName(),
            cancelled: typeof event.isCancelled === "function" ? event.isCancelled() : false
        };

        var player = callIfPresent(event, "getPlayer");
        if (player == null) {
            player = callIfPresent(event, "getSender");
        }
        if (player != null && typeof player.getUniqueId === "function") {
            data.player = toPlayerData(player);
        }

        var template = callIfPresent(event, "getTemplate");
        if (template != null) {
            data.template = toTemplateData(template);
            data.player = toPlayerData(template.getOnlinePlayer());
        }

        var skill = callIfPresent(event, "getSkill");
        if (skill != null) {
            if (typeof skill.getImmutable === "function") {
                data.skill = toPlayerSkillData(skill);
            } else {
                data.skill = toUniqueData(skill);
            }
        }

        var immutable = callIfPresent(event, "getImmutable");
        if (immutable != null) {
            data.immutable = toImmutableSkillData(immutable);
        }

        var route = callIfPresent(event, "getRoute");
        if (route != null) {
            if (typeof route.getRegisteredSkill === "function") {
                data.route = toPlayerRouteData(route);
            } else {
                data.route = toImmutableRouteData(route);
            }
        }

        var entity = callIfPresent(event, "getEntity");
        if (entity != null) {
            if (typeof entity.getInstance === "function") {
                data.entity = toProxyTargetData(entity);
            } else {
                data.entity = toEntityData(entity);
            }
        }

        var state = callIfPresent(event, "getState");
        if (state != null) {
            data.state = toStateData(state);
        }

        var container = callIfPresent(event, "getContainer");
        if (container != null) {
            data.container = toProxyTargetContainerData(container);
        }

        var combined = callIfPresent(event, "getCombined");
        if (combined != null) {
            data.combined = String(combined);
        }

        var action = callIfPresent(event, "getAction");
        if (action != null) {
            data.action = String(action);
        }

        var cause = callIfPresent(event, "getCause");
        if (cause != null) {
            if (typeof cause.getName === "function") {
                data.cause = cause.getName();
            } else {
                data.cause = enumName(cause);
            }
        }

        var proxyDamage = callIfPresent(event, "getProxyDamage");
        if (proxyDamage != null) {
            data.proxyDamage = toProxyDamageData(proxyDamage);
        }

        data.model = callIfPresent(event, "getModel");
        data.key = callIfPresent(event, "getKey");
        data.name = callIfPresent(event, "getName");
        data.type = callIfPresent(event, "getType");
        data.page = callIfPresent(event, "getPage");
        data.slot = callIfPresent(event, "getSlot");
        data.fromPage = callIfPresent(event, "getFromPage");
        data.toPage = callIfPresent(event, "getToPage");
        data.fromLevel = callIfPresent(event, "getForm");
        data.toLevel = callIfPresent(event, "getTo");
        data.amount = callIfPresent(event, "getAmount");
        data.value = callIfPresent(event, "getValue");
        data.ticks = callIfPresent(event, "getTicks");
        data.damage = callIfPresent(event, "getDamage");
        data.bukkitCause = enumName(callIfPresent(event, "getBukkitCause"));
        data.database = callIfPresent(event, "getInstance");

        return data;
    }

    /**
     * 注册 Planners 事件监听器。
     *
     * @param {string} eventName 事件全限定类名，推荐使用 PlannersJs.events 常量。
     * @param {function(Object, Object): void} callback 回调，第一个参数是原始 Java 事件，第二个参数是事件快照。
     * @throws {Error} callback 不是函数或 ScriptEngine 未提供 on 绑定时抛出。
     */
    function listen(eventName, callback) {
        if (typeof callback !== "function") {
            throw new Error("PlannersJs.listen requires a function callback: " + eventName);
        }
        requireFunction("on")(eventName, function (event) {
            callback(event, toEventData(event));
        });
    }

    /**
     * 注册带 Bukkit 优先级的 Planners 事件监听器。
     *
     * @param {string} eventName 事件全限定类名，推荐使用 PlannersJs.events 常量。
     * @param {EventPriority|string} priority Bukkit 事件优先级。
     * @param {function(Object, Object): void} callback 回调，第一个参数是原始 Java 事件，第二个参数是事件快照。
     * @throws {Error} callback 不是函数或 ScriptEngine 未提供 onPriority 绑定时抛出。
     */
    function listenPriority(eventName, priority, callback) {
        if (typeof callback !== "function") {
            throw new Error("PlannersJs.listenPriority requires a function callback: " + eventName);
        }
        requireFunction("onPriority")(eventName, priority, function (event) {
            callback(event, toEventData(event));
        });
    }

    /**
     * 根据短名获取 Planners registry。
     *
     * 可用短名: job、skill、skillTree、router、currency、level、keybinding、state。
     *
     * @param {string} name registry 短名。
     * @returns {com.gitee.planners.util.builtin.Builtin} registry 原对象。
     * @throws {Error} registry 短名未知时抛出。
     */
    function registryOf(name) {
        if (name === "job") {
            return Registries.getJOB();
        }
        if (name === "skill") {
            return Registries.getSKILL();
        }
        if (name === "skillTree") {
            return Registries.getSKILL_TREE();
        }
        if (name === "router") {
            return Registries.getROUTER();
        }
        if (name === "currency") {
            return Registries.getCURRENCY();
        }
        if (name === "level") {
            return Registries.getLEVEL();
        }
        if (name === "keybinding") {
            return Registries.getKEYBINDING();
        }
        if (name === "state") {
            return Registries.getSTATE();
        }
        throw new Error("Unknown Planners registry: " + name);
    }

    /**
     * 获取 Planners 状态配置。
     *
     * @param {string} stateId 状态 ID。
     * @returns {com.gitee.planners.core.config.State|null} 状态不存在时返回 null。
     */
    function stateSpec(stateId) {
        if (stateId == null) {
            return null;
        }
        return Registries.getSTATE().getOrNull(String(stateId));
    }

    /**
     * 将 Bukkit 实体转换为 Planners 实体目标。
     *
     * @param {org.bukkit.entity.Entity|com.gitee.planners.api.job.target.ProxyTarget.BukkitEntity|null} entity 实体或代理目标。
     * @returns {com.gitee.planners.api.job.target.ProxyTarget.BukkitEntity|null} Planners 实体目标。
     */
    function stateTarget(entity) {
        if (entity == null) {
            return null;
        }
        if (typeof entity.getMetadata === "function" && typeof entity.getBukkitLocation === "function") {
            return entity;
        }
        return new ProxyBukkitEntity(entity);
    }

    /**
     * Planners 脚本 API 根命名空间。
     *
     * @namespace PlannersJs
     *
     * @example
     * PlannersJs.onSkillCastPre(function (event, data) {
     *     logger.info(data.player.name + " casts " + data.skill.id);
     * });
     */
    root.PlannersJs = {
        /**
         * Planners 公开事件全限定类名表。
         *
         * @type {Object<string, string>}
         */
        events: EVENTS,

        /**
         * 注册事件监听器。
         *
         * @param {string} eventName 事件全限定类名。
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         */
        listen: listen,

        /**
         * 注册带优先级的事件监听器。
         *
         * @param {string} eventName 事件全限定类名。
         * @param {EventPriority|string} priority Bukkit 事件优先级。
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         */
        listenPriority: listenPriority,

        /**
         * listen 的短别名。
         *
         * @param {string} eventName 事件全限定类名。
         * @param {function(Object, Object): void} callback 回调。
         */
        on: listen,

        /**
         * listenPriority 的短别名。
         *
         * @param {string} eventName 事件全限定类名。
         * @param {EventPriority|string} priority Bukkit 事件优先级。
         * @param {function(Object, Object): void} callback 回调。
         */
        onPriority: listenPriority,

        /**
         * 监听插件 reload 前事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onReloadPre: function (callback) { listen(EVENTS.PLUGIN_RELOAD_PRE, callback); },
        /**
         * 监听插件 reload 后事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onReloadPost: function (callback) { listen(EVENTS.PLUGIN_RELOAD_POST, callback); },
        /**
         * 监听客户端按键抬起事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onClientKeyUp: function (callback) { listen(EVENTS.CLIENT_KEY_UP, callback); },
        /**
         * 监听客户端按键按下事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onClientKeyDown: function (callback) { listen(EVENTS.CLIENT_KEY_DOWN, callback); },
        /**
         * 监听连招输入命中事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onCombinedPressIn: function (callback) { listen(EVENTS.COMBINED_PRESS_IN, callback); },
        /**
         * 监听连招开始事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onCombinedBegin: function (callback) { listen(EVENTS.COMBINED_BEGIN, callback); },
        /**
         * 监听连招关闭事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onCombinedClose: function (callback) { listen(EVENTS.COMBINED_CLOSE, callback); },
        /**
         * 监听实体模型应用事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onEntityModelApply: function (callback) { listen(EVENTS.ENTITY_MODEL_APPLY, callback); },
        /**
         * 监听状态附加前事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onEntityStateAttachPre: function (callback) { listen(EVENTS.ENTITY_STATE_ATTACH_PRE, callback); },
        /**
         * 监听状态附加后事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onEntityStateAttachPost: function (callback) { listen(EVENTS.ENTITY_STATE_ATTACH_POST, callback); },
        /**
         * 监听状态移除前事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onEntityStateDetachPre: function (callback) { listen(EVENTS.ENTITY_STATE_DETACH_PRE, callback); },
        /**
         * 监听状态移除后事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onEntityStateDetachPost: function (callback) { listen(EVENTS.ENTITY_STATE_DETACH_POST, callback); },
        /**
         * 监听状态首次挂载前事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onEntityStateMountPre: function (callback) { listen(EVENTS.ENTITY_STATE_MOUNT_PRE, callback); },
        /**
         * 监听状态首次挂载后事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onEntityStateMountPost: function (callback) { listen(EVENTS.ENTITY_STATE_MOUNT_POST, callback); },
        /**
         * 监听状态完全关闭前事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onEntityStateClosePre: function (callback) { listen(EVENTS.ENTITY_STATE_CLOSE_PRE, callback); },
        /**
         * 监听状态完全关闭后事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onEntityStateClosePost: function (callback) { listen(EVENTS.ENTITY_STATE_CLOSE_POST, callback); },
        /**
         * 监听状态自然到期事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onEntityStateEnd: function (callback) { listen(EVENTS.ENTITY_STATE_END, callback); },
        /**
         * 监听背包装备技能事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onBackpackEquip: function (callback) { listen(EVENTS.BACKPACK_EQUIP, callback); },
        /**
         * 监听技能装配成功事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onBackpackEquipPost: function (callback) { listen(EVENTS.BACKPACK_EQUIP_POST, callback); },
        /**
         * 监听背包卸下技能事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onBackpackUnequip: function (callback) { listen(EVENTS.BACKPACK_UNEQUIP, callback); },
        /**
         * 监听背包页面切换前事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onBackpackPageSwitchPre: function (callback) { listen(EVENTS.BACKPACK_PAGE_SWITCH_PRE, callback); },
        /**
         * 监听背包页面切换后事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onBackpackPageSwitchPost: function (callback) { listen(EVENTS.BACKPACK_PAGE_SWITCH_POST, callback); },
        /**
         * 监听玩家伤害实体事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onPlayerDamageEntity: function (callback) { listen(EVENTS.PLAYER_DAMAGE_ENTITY, callback); },
        /**
         * 监听玩家经验增加事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onPlayerExperienceIncrement: function (callback) { listen(EVENTS.PLAYER_EXPERIENCE_INCREMENT, callback); },
        /**
         * 监听玩家经验减少事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onPlayerExperienceDecrement: function (callback) { listen(EVENTS.PLAYER_EXPERIENCE_DECREMENT, callback); },
        /**
         * 监听玩家经验设置事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onPlayerExperienceSet: function (callback) { listen(EVENTS.PLAYER_EXPERIENCE_SET, callback); },
        /**
         * 监听玩家经验更新完成事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onPlayerExperienceUpdated: function (callback) { listen(EVENTS.PLAYER_EXPERIENCE_UPDATED, callback); },
        /**
         * 监听玩家等级变化事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onPlayerLevelChange: function (callback) { listen(EVENTS.PLAYER_LEVEL_CHANGE, callback); },
        /**
         * 监听玩家魔法点增加事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onPlayerMagicPointIncrease: function (callback) { listen(EVENTS.PLAYER_MAGIC_POINT_INCREASE, callback); },
        /**
         * 监听玩家魔法点减少事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onPlayerMagicPointDecrease: function (callback) { listen(EVENTS.PLAYER_MAGIC_POINT_DECREASE, callback); },
        /**
         * 监听玩家魔法点设置事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onPlayerMagicPointSet: function (callback) { listen(EVENTS.PLAYER_MAGIC_POINT_SET, callback); },
        /**
         * 监听玩家档案加载完成事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onPlayerProfileLoaded: function (callback) { listen(EVENTS.PLAYER_PROFILE_LOADED, callback); },
        /**
         * 监听玩家路线设置前事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onPlayerSetRoutePre: function (callback) { listen(EVENTS.PLAYER_SET_ROUTE_PRE, callback); },
        /**
         * 监听玩家路线设置后事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onPlayerSetRoutePost: function (callback) { listen(EVENTS.PLAYER_SET_ROUTE_POST, callback); },
        /**
         * 监听技能输入检查事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onSkillCastCheck: function (callback) { listen(EVENTS.PLAYER_SKILL_CAST_CHECK, callback); },
        /**
         * 监听技能释放前事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onSkillCastPre: function (callback) { listen(EVENTS.PLAYER_SKILL_CAST_PRE, callback); },
        /**
         * 监听技能释放后事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onSkillCastPost: function (callback) { listen(EVENTS.PLAYER_SKILL_CAST_POST, callback); },
        /**
         * 监听技能冷却设置事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onSkillCooldownSet: function (callback) { listen(EVENTS.PLAYER_SKILL_COOLDOWN_SET, callback); },
        /**
         * 监听玩家技能等级变化事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onSkillLevelChange: function (callback) { listen(EVENTS.PLAYER_SKILL_LEVEL_CHANGE, callback); },
        /**
         * 监听玩家首次学习技能成功事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onSkillLearn: function (callback) { listen(EVENTS.PLAYER_SKILL_LEARN, callback); },
        /**
         * 监听目标捕获事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onTargetCaptured: function (callback) { listen(EVENTS.TARGET_CAPTURED, callback); },
        /**
         * 监听脚本自定义触发事件。
         *
         * @param {function(Object, Object): void} callback 回调，第一个参数是原始事件，第二个参数是事件快照。
         * @returns {void}
         */
        onScriptCustomTrigger: function (callback) { listen(EVENTS.SCRIPT_CUSTOM_TRIGGER, callback); },

        /**
         * Java 对象到普通 JS 数据的转换工具。
         *
         * @namespace PlannersJs.convert
         */
        convert: {
            /** @param {java.util.Collection|Array|null} value Java 集合或 JS 数组。 @returns {Array} JS 数组。 */
            toArray: toArray,
            /** @param {java.util.Map|null} value Java Map。 @param {function(*, string): *} [mapper] 值转换器。 @returns {Object} JS 对象。 */
            mapToObject: mapToObject,
            /** @param {Object|null} value JS 对象。 @returns {java.util.LinkedHashMap} Java Map。 */
            jsObjectToJavaMap: jsObjectToJavaMap,
            /** @param {org.bukkit.entity.Player|null} value Bukkit 玩家。 @returns {Object|null} 玩家快照。 */
            player: toPlayerData,
            /** @param {org.bukkit.entity.Entity|null} value Bukkit 实体。 @returns {Object|null} 实体快照。 */
            entity: toEntityData,
            /** @param {org.bukkit.Location|null} value Bukkit 位置。 @returns {Object|null} 位置快照。 */
            location: toLocationData,
            /** @param {com.gitee.planners.core.player.PlayerTemplate|null} value 玩家档案。 @returns {Object|null} 档案快照。 */
            template: toTemplateData,
            /** @param {com.gitee.planners.core.player.PlayerRoute|null} value 玩家路线。 @returns {Object|null} 玩家路线快照。 */
            playerRoute: toPlayerRouteData,
            /** @param {com.gitee.planners.core.player.PlayerRouter|null} value 玩家路由等级。 @returns {Object|null} 玩家路由等级快照。 */
            playerRouter: toPlayerRouterData,
            /** @param {com.gitee.planners.core.player.PlayerSkill|null} value 玩家技能。 @returns {Object|null} 玩家技能快照。 */
            playerSkill: toPlayerSkillData,
            /** @param {com.gitee.planners.core.config.ImmutableSkill|null} value 技能配置。 @returns {Object|null} 技能配置快照。 */
            immutableSkill: toImmutableSkillData,
            /** @param {com.gitee.planners.core.config.ImmutableRoute|null} value 路线配置。 @returns {Object|null} 路线配置快照。 */
            immutableRoute: toImmutableRouteData,
            /** @param {com.gitee.planners.core.config.ImmutableJob|null} value 职业配置。 @returns {Object|null} 职业快照。 */
            job: toJobData,
            /** @param {com.gitee.planners.core.config.BackpackConfig|null} value 背包配置。 @returns {Object|null} 背包配置快照。 */
            backpackConfig: toBackpackConfigData,
            /** @param {com.gitee.planners.core.config.BackpackPage|null} value 背包页面。 @returns {Object|null} 背包页面快照。 */
            backpackPage: toBackpackPageData,
            /** @param {com.gitee.planners.core.config.BackpackSlot|null} value 背包槽位。 @returns {Object|null} 背包槽位快照。 */
            backpackSlot: toBackpackSlotData,
            /** @param {com.gitee.planners.api.job.target.ProxyTarget|null} value 代理目标。 @returns {Object|null} 代理目标快照。 */
            proxyTarget: toProxyTargetData,
            /** @param {com.gitee.planners.api.job.target.ProxyTargetContainer|null} value 代理目标容器。 @returns {Object[]} 代理目标快照数组。 */
            proxyTargetContainer: toProxyTargetContainerData,
            /** @param {com.gitee.planners.api.damage.DamageCause|null} value 伤害来源。 @returns {Object|null} 伤害来源快照。 */
            damageCause: toDamageCauseData,
            /** @param {com.gitee.planners.api.damage.ProxyDamage|null} value 伤害代理。 @returns {Object|null} 伤害代理快照。 */
            proxyDamage: toProxyDamageData,
            /** @param {com.gitee.planners.core.config.State|null} value 状态定义。 @returns {Object|null} 状态快照。 */
            state: toStateData,
            /** @param {Object|null} value 原始事件。 @returns {Object|null} 事件快照。 */
            event: toEventData
        },

        /**
         * Planners registry 读取工具。
         *
         * @namespace PlannersJs.registry
         */
        registry: {
            /**
             * 获取 registry 原对象。
             *
             * @param {string} name registry 短名。
             * @returns {com.gitee.planners.util.builtin.Builtin} registry 原对象。
             */
            raw: registryOf,
            /**
             * 获取 registry 的所有键。
             *
             * @param {string} name registry 短名。
             * @returns {string[]} 键列表。
             */
            keys: function (name) {
                return toArray(registryOf(name).keys());
            },
            /**
             * 获取 registry 的所有值。
             *
             * @param {string} name registry 短名。
             * @returns {Array} Java 对象列表。
             */
            values: function (name) {
                return toArray(registryOf(name).values());
            },
            /**
             * 从 registry 按 ID 获取 Java 原对象。
             *
             * @param {string} name registry 短名。
             * @param {string} id 对象 ID。
             * @returns {*|null} registry 对象；不存在返回 null。
             */
            get: function (name, id) {
                return registryOf(name).getOrNull(String(id));
            },
            /**
             * 判断 registry 是否包含指定 ID。
             *
             * @param {string} name registry 短名。
             * @param {string} id 对象 ID。
             * @returns {boolean} 存在返回 true。
             */
            has: function (name, id) {
                return registryOf(name).containsKey(String(id));
            },
            /**
             * 获取 registry 元素数量。
             *
             * @param {string} name registry 短名。
             * @returns {number} 元素数量。
             */
            size: function (name) {
                return Number(registryOf(name).size());
            },
            /**
             * 按 ID 获取 ImmutableSkill 原对象。
             *
             * @param {string} id 技能 ID。
             * @returns {com.gitee.planners.core.config.ImmutableSkill|null} 技能配置；不存在返回 null。
             */
            skill: function (id) {
                return Registries.getSKILL().getOrNull(String(id));
            },
            /**
             * 按 ID 获取技能配置快照。
             *
             * @param {string} id 技能 ID。
             * @returns {Object|null} 技能配置快照。
             */
            skillData: function (id) {
                return toImmutableSkillData(Registries.getSKILL().getOrNull(String(id)));
            },
            /**
             * 按 ID 获取 ImmutableJob 原对象。
             *
             * @param {string} id 职业 ID。
             * @returns {com.gitee.planners.core.config.ImmutableJob|null} 职业配置；不存在返回 null。
             */
            job: function (id) {
                return Registries.getJOB().getOrNull(String(id));
            },
            /**
             * 按 ID 获取职业快照。
             *
             * @param {string} id 职业 ID。
             * @returns {Object|null} 职业快照。
             */
            jobData: function (id) {
                return toJobData(Registries.getJOB().getOrNull(String(id)));
            },
            /**
             * 按 ID 获取 ImmutableRouter 原对象。
             *
             * @param {string} id 路由 ID。
             * @returns {com.gitee.planners.core.config.ImmutableRouter|null} 路由配置；不存在返回 null。
             */
            router: function (id) {
                return Registries.getROUTER().getOrNull(String(id));
            },
            /**
             * 按 ID 获取 State 原对象。
             *
             * @param {string} id 状态 ID。
             * @returns {com.gitee.planners.core.config.State|null} 状态定义；不存在返回 null。
             */
            state: function (id) {
                return Registries.getSTATE().getOrNull(String(id));
            },
            /**
             * 按 ID 获取状态快照。
             *
             * @param {string} id 状态 ID。
             * @returns {Object|null} 状态快照。
             */
            stateData: function (id) {
                return toStateData(Registries.getSTATE().getOrNull(String(id)));
            },
            /**
             * 获取 BackpackConfig 原对象。
             *
             * @returns {com.gitee.planners.core.config.BackpackConfig} 背包配置。
             */
            backpack: function () {
                return Registries.getBACKPACK();
            },
            /**
             * 获取背包配置快照。
             *
             * @returns {Object|null} 背包配置快照。
             */
            backpackData: function () {
                return toBackpackConfigData(Registries.getBACKPACK());
            }
        },

        /**
         * 实体状态查询与修改工具。
         *
         * @namespace PlannersJs.state
         */
        state: {
            /**
             * 按 ID 获取状态定义。
             *
             * @param {string} stateId 状态 ID。
             * @returns {com.gitee.planners.core.config.State|null} 状态定义；不存在返回 null。
             */
            getSpec: stateSpec,
            /**
             * 将 Bukkit 实体转换为 Planners 实体目标。
             *
             * @param {org.bukkit.entity.Entity|com.gitee.planners.api.job.target.ProxyTarget.BukkitEntity|null} entity 实体或代理目标。
             * @returns {com.gitee.planners.api.job.target.ProxyTarget.BukkitEntity|null} Planners 实体目标。
             */
            asTarget: stateTarget,
            /**
             * 判断实体是否拥有指定状态。
             *
             * @param {org.bukkit.entity.Entity|com.gitee.planners.api.job.target.ProxyTarget.BukkitEntity} entity 实体或代理目标。
             * @param {string} stateId 状态 ID。
             * @returns {boolean} 拥有状态时返回 true。
             */
            has: function (entity, stateId) {
                var state = stateSpec(stateId);
                var target = stateTarget(entity);
                if (state == null || target == null) {
                    return false;
                }
                return EntityStateManager.has(target, state);
            },
            /**
             * 获取实体指定状态的当前层数。
             *
             * @param {org.bukkit.entity.Entity|com.gitee.planners.api.job.target.ProxyTarget.BukkitEntity} entity 实体或代理目标。
             * @param {string} stateId 状态 ID。
             * @returns {number} 当前有效层数；状态不存在、已失效或参数无效时返回 0。
             */
            getLayer: function (entity, stateId) {
                var state = stateSpec(stateId);
                var target = stateTarget(entity);
                if (state == null || target == null) {
                    return 0;
                }
                return Number(EntityStateManager.getLayer(target, state));
            },
            /**
            * 判断实体上的指定状态是否已经过期。
             *
             * @param {org.bukkit.entity.Entity|com.gitee.planners.api.job.target.ProxyTarget.BukkitEntity} entity 实体或代理目标。
             * @param {string} stateId 状态 ID。
             * @returns {boolean} 状态不存在、参数无效或已经过期时返回 true。
             */
            isExpired: function (entity, stateId) {
                var state = stateSpec(stateId);
                var target = stateTarget(entity);
                if (state == null || target == null) {
                    return true;
                }
                return EntityStateManager.isExpired(target, state);
            },
            /**
             * 给实体附加一层状态。
             *
             * @param {org.bukkit.entity.Entity|com.gitee.planners.api.job.target.ProxyTarget.BukkitEntity} entity 实体或代理目标。
             * @param {string} stateId 状态 ID。
             * @param {number} durationTicks 持续 tick，必须大于 0。
             * @param {boolean} [refreshDuration=true] 已有状态时是否刷新持续时间。
             * @returns {boolean} 成功附加、叠层或刷新状态时返回 true。
             */
            attach: function (entity, stateId, durationTicks, refreshDuration) {
                var state = stateSpec(stateId);
                var target = stateTarget(entity);
                if (state == null || target == null) {
                    return false;
                }
                var refresh = refreshDuration;
                if (refresh === undefined || refresh === null) {
                    refresh = true;
                }
                return EntityStateManager.attach(target, state, Number(durationTicks), refresh === true);
            },
            /**
             * 从实体移除指定层数的状态。
             *
             * @param {org.bukkit.entity.Entity|com.gitee.planners.api.job.target.ProxyTarget.BukkitEntity} entity 实体或代理目标。
             * @param {string} stateId 状态 ID。
             * @param {number} [layer=1] 移除层数；传 999 表示清空。
             * @returns {boolean} 成功移除至少一个状态层时返回 true。
             */
            detach: function (entity, stateId, layer) {
                var state = stateSpec(stateId);
                var target = stateTarget(entity);
                if (state == null || target == null) {
                    return false;
                }
                var amount = layer;
                if (amount === undefined || amount === null) {
                    amount = 1;
                }
                return EntityStateManager.detach(target, state, Number(amount));
            },
            /**
             * 完整移除实体上的指定状态。
             *
             * @param {org.bukkit.entity.Entity|com.gitee.planners.api.job.target.ProxyTarget.BukkitEntity} entity 实体或代理目标。
             * @param {string} stateId 状态 ID。
             * @returns {boolean} 已调用状态管理器时返回 true。
             */
            remove: function (entity, stateId) {
                var state = stateSpec(stateId);
                var target = stateTarget(entity);
                if (state == null || target == null) {
                    return false;
                }
                EntityStateManager.remove(target, state);
                return true;
            }
        },

        /**
         * 玩家档案、等级、经验和魔法点工具。
         *
         * @namespace PlannersJs.profile
         */
        profile: {
            /**
             * 判断玩家档案是否已经加载。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @returns {boolean} 已加载返回 true。
             */
            isLoaded: function (player) {
                return PlayerTemplateAPI.getPlannersLoaded(player);
            },
            /**
             * 获取玩家档案原对象。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @returns {com.gitee.planners.core.player.PlayerTemplate} 玩家档案。
             */
            get: function (player) {
                return PlayerTemplateAPI.getPlannersTemplate(player);
            },
            /**
             * 获取玩家档案快照。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @returns {Object|null} 玩家档案快照。
             */
            data: function (player) {
                return toTemplateData(PlayerTemplateAPI.getPlannersTemplate(player));
            },
            /**
             * 获取当前魔法值。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @returns {number} 当前魔法值；档案未加载时返回 0。
             */
            magicPoint: function (player) {
                if (!PlayerTemplateAPI.getPlannersLoaded(player)) {
                    return 0;
                }
                return Number(MagicPointProvider.getINSTANCE().getPoint(player));
            },
            /**
             * 获取当前魔法值上限。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @returns {number} 当前魔法值上限；档案未加载时返回 0。
             */
            magicPointMax: function (player) {
                if (!PlayerTemplateAPI.getPlannersLoaded(player)) {
                    return 0;
                }
                return Number(MagicPointProvider.getINSTANCE().getPointInUpperLimit(player));
            },
            /**
             * 获取玩家的完整转职线聚合对象。
             *
             * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
             * @returns {com.gitee.planners.core.player.PlayerRouter|null} 未选择职业时返回 null。
             */
            router: function (templateOrPlayer) {
                return getRouterOrNull(templateOrPlayer);
            },
            /**
             * 获取当前 PlayerRoute 对应的 Job 配置。
             *
             * PlayerRoute 仅代表转职线中的一个 Job 阶段；需要整条转职线、
             * 共享 SP 或已继承技能时，应调用 profile.router()。
             *
             * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
             * @returns {com.gitee.planners.core.config.ImmutableJob|null} 当前 Job；档案未加载或尚未选择职业时返回 null。
             */
            job: function (templateOrPlayer) {
                var playerRouter = getRouterOrNull(templateOrPlayer);
                if (playerRouter == null) {
                    return [];
                }
                return playerRouter.getCurrentJob();
            },
            /**
             * 获取当前 Job 阶段。
             *
             * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
             * @returns {com.gitee.planners.core.player.PlayerRoute|null} 未选择职业时返回 null。
             */
            currentRoute: function (templateOrPlayer) {
                var playerRouter = getRouterOrNull(templateOrPlayer);
                if (playerRouter == null) {
                    return null;
                }
                return playerRouter.getCurrentRoute();
            },
            /**
             * 获取当前转职线的所有 Job 阶段，顺序为根 Job 到当前 Job。
             *
             * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
             * @returns {Object[]} PlayerRoute 快照列表。
             */
            routeLineData: function (templateOrPlayer) {
                var playerRouter = getRouterOrNull(templateOrPlayer);
                if (playerRouter == null) {
                    return [];
                }
                return toArray(playerRouter.getRouteLine()).map(toPlayerRouteData);
            },
            /**
             * 设置玩家路线。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {com.gitee.planners.core.config.ImmutableRoute} route 路线配置原对象。
             * @returns {java.util.concurrent.CompletableFuture} 异步设置结果。
             */
            setRoute: function (player, route) {
                return PlayerTemplateAPI.setPlayerRoute(player, route);
            },
            /**
             * 增加玩家魔法点。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {number} amount 增加数量。
             */
            addMagicPoint: function (player, amount) {
                PlayerTemplateAPI.addMagicPoint(player, Number(amount));
            },
            /**
             * 减少玩家魔法点。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {number} amount 减少数量。
             */
            takeMagicPoint: function (player, amount) {
                PlayerTemplateAPI.takeMagicPoint(player, Number(amount));
            },
            /**
             * 设置玩家魔法点。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {number} value 目标魔法点。
             */
            setMagicPoint: function (player, value) {
                PlayerTemplateAPI.setMagicPoint(player, Number(value));
            },
            /**
             * 重置玩家魔法点到上限。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             */
            resetMagicPoint: function (player) {
                PlayerTemplateAPI.resetMagicPoint(player);
            },
            /**
             * 增加玩家路线等级。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {number} amount 增加等级。
             */
            addLevel: function (player, amount) {
                PlayerTemplateAPI.addLevel(player, Number(amount));
            },
            /**
             * 设置玩家路线等级。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {number} level 目标等级。
             */
            setLevel: function (player, level) {
                PlayerTemplateAPI.setLevel(player, Number(level));
            },
            /**
             * 增加玩家经验。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {number} amount 增加经验。
             */
            addExperience: function (player, amount) {
                PlayerTemplateAPI.addExperience(player, Number(amount));
            },
            /**
             * 减少玩家经验。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {number} amount 减少经验。
             */
            takeExperience: function (player, amount) {
                PlayerTemplateAPI.takeExperience(player, Number(amount));
            },
            /**
             * 设置玩家经验。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {number} value 目标经验。
             */
            setExperience: function (player, value) {
                PlayerTemplateAPI.setExperience(player, Number(value));
            },
            /**
             * 设置玩家技能等级。
             *
             * @param {com.gitee.planners.core.player.PlayerTemplate} template 玩家档案。
             * @param {com.gitee.planners.core.player.PlayerSkill} skill 玩家技能实例。
             * @param {number} level 目标等级。
             */
            setSkillLevel: function (template, skill, level) {
                PlayerTemplateAPI.setSkillLevel(template, skill, Number(level));
            }
        },

        /**
         * 玩家路线动态读取工具。
         *
         * @namespace PlannersJs.route
         */
        route: {
            /**
             * 获取玩家路线的节点状态读取对象。
             *
             * @param {com.gitee.planners.core.player.PlayerRoute} route 玩家路线。
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @returns {Object} 可读取节点等级、可激活状态和提示的动态对象。
             */
            getPlayerRoute: function (route, player) {
                if (route == null || player == null) {
                    return null;
                }
                return toScriptObject(route.getPlayerRoute(player));
            }
        },

        /**
         * 技能背包页面、槽位和装备工具。
         *
         * @namespace PlannersJs.backpack
         */
        backpack: {
            /**
             * 获取玩家当前背包页面 ID。
             *
             * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
             * @returns {string} 当前页面 ID。
             */
            currentPage: function (templateOrPlayer) {
                return BackpackAPI.getCurrentPage(resolveTemplate(templateOrPlayer));
            },
            /**
             * 设置玩家当前背包页面。
             *
             * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
             * @param {string} page 页面 ID。
             */
            setCurrentPage: function (templateOrPlayer, page) {
                BackpackAPI.setCurrentPage(resolveTemplate(templateOrPlayer), String(page));
            },
            /**
             * 将技能装备到指定页面槽位。
             *
             * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
             * @param {com.gitee.planners.core.player.PlayerSkill} skill 玩家技能实例。
             * @param {string} page 页面 ID。
             * @param {string} slot 槽位 ID。
             * @returns {boolean} 分类和槽位均允许时返回 true。
             */
            equipSkill: function (templateOrPlayer, skill, page, slot) {
                var template = resolveTemplate(templateOrPlayer);
                var pageId = String(page);
                var slotId = String(slot);
                if (!BackpackAPI.canEquipSkill(template, skill, pageId, slotId)) {
                    return false;
                }
                BackpackAPI.equipSkill(template, skill, pageId, slotId);
                return true;
            },
            /**
             * 检查技能是否可以装备到指定页面槽位。
             *
             * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
             * @param {com.gitee.planners.core.player.PlayerSkill} skill 玩家技能实例。
             * @param {string} page 页面 ID。
             * @param {string} slot 槽位 ID。
             * @returns {boolean} 页面、槽位和分类均匹配时返回 true。
             */
            canEquipSkill: function (templateOrPlayer, skill, page, slot) {
                return BackpackAPI.canEquipSkill(resolveTemplate(templateOrPlayer), skill, String(page), String(slot));
            },
            /**
             * 卸下指定玩家技能。
             *
             * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
             * @param {com.gitee.planners.core.player.PlayerSkill} skill 玩家技能实例。
             */
            unequipSkill: function (templateOrPlayer, skill) {
                BackpackAPI.unequipSkill(resolveTemplate(templateOrPlayer), skill);
            },
            /**
             * 根据按键 ID 获取当前页面对应技能原对象。
             *
             * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
             * @param {string} keyId 按键 ID。
             * @returns {com.gitee.planners.core.player.PlayerSkill|null} 玩家技能实例；未装备返回 null。
             */
            skillByKey: function (templateOrPlayer, keyId) {
                return BackpackAPI.getSkillByKey(resolveTemplate(templateOrPlayer), String(keyId));
            },
            /**
             * 根据按键 ID 获取当前页面对应技能快照。
             *
             * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
             * @param {string} keyId 按键 ID。
             * @returns {Object|null} 玩家技能快照。
             */
            skillByKeyData: function (templateOrPlayer, keyId) {
                return toPlayerSkillData(BackpackAPI.getSkillByKey(resolveTemplate(templateOrPlayer), String(keyId)));
            },
            /**
             * 获取按键对应的 hotbar 索引。
             *
             * @param {string} keyId 按键 ID。
             * @returns {number} hotbar 索引；未找到时按 Planners API 返回值。
             */
            hotbarIndex: function (keyId) {
                return Number(BackpackAPI.getHotbarIndex(String(keyId)));
            },
            /**
             * 获取背包页面配置原对象。
             *
             * @param {string} pageId 页面 ID。
             * @returns {com.gitee.planners.core.config.BackpackPage|null} 页面配置；不存在返回 null。
             */
            page: function (pageId) {
                return Registries.getBACKPACK().getPage(String(pageId));
            },
            /**
             * 获取背包页面配置快照。
             *
             * @param {string} pageId 页面 ID。
             * @returns {Object|null} 页面快照。
             */
            pageData: function (pageId) {
                return toBackpackPageData(Registries.getBACKPACK().getPage(String(pageId)));
            },
            /**
             * 获取指定页面的槽位快照列表，供客户端读取布局和分类限制。
             *
             * @param {string} pageId 页面 ID。
             * @returns {Object[]} 槽位快照列表。
             */
            slots: function (pageId) {
                return toBackpackSlotListData(Registries.getBACKPACK().getPage(String(pageId)));
            },
            /**
             * 获取完整背包配置快照。
             *
             * @returns {Object|null} 背包配置快照。
             */
            data: function () {
                return toBackpackConfigData(Registries.getBACKPACK());
            }
        },

        /**
         * 技能释放、变量、回调和图标工具。
         *
         * @namespace PlannersJs.skill
         */
        skill: {
            /**
             * 释放玩家技能，会走 PlannersAPI 的冷却和条件流程。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {com.gitee.planners.core.player.PlayerSkill} playerSkill 玩家技能实例。
             * @returns {com.gitee.planners.core.skill.ExecutableResult} 释放结果。
             */
            cast: function (player, playerSkill) {
                return PlannersAPI.cast(player, playerSkill);
            },
            /**
             * 释放 ImmutableSkill，不记录玩家技能冷却流程。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {com.gitee.planners.core.config.ImmutableSkill} immutableSkill 技能配置。
             * @param {number} level 技能等级。
             * @returns {java.util.concurrent.CompletableFuture} 脚本执行 Future。
             */
            castImmutable: function (player, immutableSkill, level) {
                return PlannersAPI.cast(player, immutableSkill, Number(level));
            },
            /**
             * 获取玩家已注册技能原对象。
             *
             * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
             * @param {string} skillId 技能 ID。
             * @returns {com.gitee.planners.core.player.PlayerSkill|null} 玩家技能；未注册返回 null。
             */
            playerSkill: function (templateOrPlayer, skillId) {
                return resolveTemplate(templateOrPlayer).getRegisteredSkillOrNull(String(skillId));
            },
            /**
             * 获取玩家已注册技能快照。
             *
             * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
             * @param {string} skillId 技能 ID。
             * @returns {Object|null} 玩家技能快照。
             */
            playerSkillData: function (templateOrPlayer, skillId) {
                return toPlayerSkillData(resolveTemplate(templateOrPlayer).getRegisteredSkillOrNull(String(skillId)));
            },
            /**
             * 获取或创建玩家技能实例。
             *
             * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
             * @param {string} skillId 技能 ID。
             * @returns {java.util.concurrent.CompletableFuture} 完成后值为 PlayerSkill。
             */
            getOrCreatePlayerSkill: function (templateOrPlayer, skillId) {
                return resolveTemplate(templateOrPlayer).getSkill(String(skillId));
            },
            /**
             * 计算技能变量值。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {com.gitee.planners.core.config.ImmutableSkill} skill 技能配置。
             * @param {com.gitee.planners.api.job.Variable|string} variableOrId 变量对象或变量 ID。
             * @returns {java.util.concurrent.CompletableFuture} 完成后值为变量计算结果。
             */
            variableValue: function (player, skill, variableOrId) {
                return PlannersAPI.getVariableValue(player, skill, variableOrId);
            },
            /**
             * 创建技能脚本执行选项。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {com.gitee.planners.core.config.ImmutableSkill|com.gitee.planners.core.player.PlayerSkill} skill 技能配置或玩家技能。
             * @param {number} [level] 可选等级；传入时使用指定等级重载。
             * @returns {com.gitee.planners.module.script.ScriptOptions} 脚本选项。
             */
            newOptions: function (player, skill, level) {
                if (level == null) {
                    return PlannersAPI.newOptions(player, skill);
                }
                return PlannersAPI.newOptions(player, skill, Number(level));
            },
            /**
             * 执行技能 action 中声明的回调函数。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {com.gitee.planners.core.player.PlayerSkill} playerSkill 玩家技能实例。
             * @param {string} method 回调函数名。
             * @param {Object} variables 注入脚本上下文的变量。
             * @param {Object} payload 传给回调函数的负载。
             * @returns {boolean} 函数存在并完成调用时返回 true。
             */
            executeCallback: function (player, playerSkill, method, variables, payload) {
                return PlannersAPI.executeSkillCallback(
                    player,
                    playerSkill,
                    String(method),
                    jsObjectToJavaMap(variables),
                    jsObjectToJavaMap(payload)
                );
            },
            /**
             * 创建技能图标格式化器。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {com.gitee.planners.core.player.PlayerSkill} playerSkill 玩家技能实例。
             * @param {number} [level] 可选展示等级。
             * @returns {com.gitee.planners.core.skill.formatter.IconFormatter} 图标格式化器。
             */
            iconFormatter: function (player, playerSkill, level) {
                if (level == null) {
                    return KeyBindingAPI.createIconFormatter(player, playerSkill);
                }
                return KeyBindingAPI.createIconFormatter(player, playerSkill, Number(level));
            },
            /**
             * 获取当前 PlayerRoute 绑定的技能树。
             *
             * 技能树只控制当前 Job 的技能学习与升级条件。父 Job 技能通过
             * PlayerRouter.effectiveSkills 继承，不会重复出现在子 Job 技能树中。
             *
             * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
             * @returns {Array<com.gitee.planners.core.config.ImmutableSkillTree>} 当前 Job 的技能树；档案未加载或未选择职业时返回空数组。
             */
            tree: function (templateOrPlayer) {
                var playerRouter = getRouterOrNull(templateOrPlayer);
                if (playerRouter == null) {
                    return null;
                }
                return toArray(playerRouter.getCurrentRoute().getSkillTrees());
            },
            /**
             * 获取当前转职线的可用共享 SP。
             *
             * SP 归属 PlayerRouter，而非单个 PlayerRoute，因此转职后父、子
             * Job 共同使用同一份点数。
             *
             * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
             * @returns {number} 当前可用 SP；档案未加载或尚未选择职业时返回 0。
             */
            points: function (templateOrPlayer) {
                var playerRouter = getRouterOrNull(templateOrPlayer);
                if (playerRouter == null) {
                    return 0;
                }
                return Number(playerRouter.getSkillPointsCurrent());
            },
            /**
             * 增加当前转职线的共享 SP。
             *
             * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
             * @param {number} amount 增加的 SP 数量。
             * @returns {boolean} 已写入 PlayerRouter 时返回 true；档案未加载或尚未选择职业时返回 false。
             */
            addPoints: function (templateOrPlayer, amount) {
                var playerRouter = getRouterOrNull(templateOrPlayer);
                if (playerRouter == null) {
                    return false;
                }
                playerRouter.addSkillPoints(Number(amount));
                return true;
            },
            /**
             * 扣除当前转职线的共享 SP。
             *
             * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
             * @param {number} amount 扣除的 SP 数量。
             * @returns {boolean} 扣除成功时返回 true；档案未加载、未选择职业或可用 SP 不足时返回 false。
             */
            takePoints: function (templateOrPlayer, amount) {
                var playerRouter = getRouterOrNull(templateOrPlayer);
                if (playerRouter == null) {
                    return false;
                }
                return playerRouter.takeSkillPoints(Number(amount));
            }
        },

        /**
         * 玩家技能冷却读写工具。
         *
         * 所有方法仅接受 PlayerSkill，或能解析为该玩家已注册 PlayerSkill 的
         * 字符串 ID；无法解析的技能不会读写冷却。
         *
         * @namespace PlannersJs.cooldown
         */
        cooldown: {
            /**
             * 获取玩家技能的剩余冷却 tick。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {com.gitee.planners.core.player.PlayerSkill|string} skillOrId 玩家技能实例或已注册的技能 ID。
             * @returns {number} 剩余冷却 tick；技能不存在、档案未加载或冷却已结束时返回 0。
             */
            get: function (player, skillOrId) {
                var skill = resolveCooldownSkill(player, skillOrId);
                if (skill == null) {
                    return 0;
                }
                var remaining = Cooler.getINSTANCE().get(player, skill);
                if (remaining < 0) {
                    return 0;
                }
                return Number(remaining);
            },
            /**
             * 设置玩家技能的冷却时长。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {com.gitee.planners.core.player.PlayerSkill|string} skillOrId 玩家技能实例或已注册的技能 ID。
             * @param {number} ticks 冷却时长，单位为 tick。
             * @returns {boolean} 找到 PlayerSkill 并写入冷却时返回 true；否则返回 false。
             */
            set: function (player, skillOrId, ticks) {
                var skill = resolveCooldownSkill(player, skillOrId);
                if (skill == null) {
                    return false;
                }
                Cooler.getINSTANCE().set(player, skill, Number(ticks));
                return true;
            },
            /**
             * 将玩家技能的冷却重置为 0 tick。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {com.gitee.planners.core.player.PlayerSkill|string} skillOrId 玩家技能实例或已注册的技能 ID。
             * @returns {boolean} 找到 PlayerSkill 并完成重置时返回 true；否则返回 false。
             */
            reset: function (player, skillOrId) {
                return this.set(player, skillOrId, 0);
            },
            /**
             * 检查玩家技能是否仍在冷却中。
             *
             * @param {org.bukkit.entity.Player} player Bukkit 玩家。
             * @param {com.gitee.planners.core.player.PlayerSkill|string} skillOrId 玩家技能实例或已注册的技能 ID。
             * @returns {boolean} 剩余冷却大于 0 tick 时返回 true；技能不存在或档案未加载时返回 false。
             */
            has: function (player, skillOrId) {
                return this.get(player, skillOrId) > 0;
            }
        },

        /**
         * Planners 元数据读写工具。
         *
         * 可传 Bukkit Entity、PlayerTemplate 或实现 MetadataContainer 的对象。
         * Bukkit Player 仅在 Planners 档案已加载时可作为 PlayerTemplate 使用。
         *
         * @namespace PlannersJs.metadata
         */
        metadata: {
            /**
             * 获取元数据原对象。
             *
             * @param {org.bukkit.entity.Entity|org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate|com.gitee.planners.api.common.metadata.MetadataContainer} target 元数据持有者。
             * @param {string} key 元数据键。
             * @returns {com.gitee.planners.api.common.metadata.Metadata|null} 元数据对象；目标不可用或键不存在时返回 null。
             */
            get: function (target, key) {
                var container = resolveMetadataContainer(target);
                if (container == null) {
                    return null;
                }
                return container.getMetadata(String(key));
            },
            /**
             * 获取元数据保存的原始值。
             *
             * @param {org.bukkit.entity.Entity|org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate|com.gitee.planners.api.common.metadata.MetadataContainer} target 元数据持有者。
             * @param {string} key 元数据键。
             * @returns {*} 元数据值；目标不可用或键不存在时返回 null。
             */
            value: function (target, key) {
                var metadata = this.get(target, key);
                if (metadata == null) {
                    return null;
                }
                return metadata.any();
            },
            /**
             * 检查指定元数据键是否存在。
             *
             * @param {org.bukkit.entity.Entity|org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate|com.gitee.planners.api.common.metadata.MetadataContainer} target 元数据持有者。
             * @param {string} key 元数据键。
             * @returns {boolean} 键存在时返回 true；目标不可用或键不存在时返回 false。
             */
            has: function (target, key) {
                return this.get(target, key) != null;
            },
            /**
             * 设置永久元数据。
             *
             * @param {org.bukkit.entity.Entity|org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate|com.gitee.planners.api.common.metadata.MetadataContainer} target 元数据持有者。
             * @param {string} key 元数据键。
             * @param {*} value 要保存的值。
             * @returns {boolean} 写入成功时返回 true；目标不可用时返回 false。
             */
            set: function (target, key, value) {
                var container = resolveMetadataContainer(target);
                if (container == null) {
                    return false;
                }
                container.setMetadata(String(key), MetadataHelper.metadataValue(value, -1));
                return true;
            },
            /**
             * 设置会自动过期的元数据。
             *
             * @param {org.bukkit.entity.Entity|org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate|com.gitee.planners.api.common.metadata.MetadataContainer} target 元数据持有者。
             * @param {string} key 元数据键。
             * @param {*} value 要保存的值。
             * @param {number} timeoutMs 过期时长，单位为毫秒。
             * @returns {boolean} 写入成功时返回 true；目标不可用时返回 false。
             */
            setTimeout: function (target, key, value, timeoutMs) {
                var container = resolveMetadataContainer(target);
                if (container == null) {
                    return false;
                }
                container.setMetadata(String(key), MetadataHelper.metadataValue(value, Number(timeoutMs)));
                return true;
            },
            /**
             * 将键写为 Void metadata，语义上移除指定元数据。
             *
             * @param {org.bukkit.entity.Entity|org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate|com.gitee.planners.api.common.metadata.MetadataContainer} target 元数据持有者。
             * @param {string} key 元数据键。
             * @returns {boolean} 写入 Void metadata 成功时返回 true；目标不可用时返回 false。
             */
            remove: function (target, key) {
                var container = resolveMetadataContainer(target);
                if (container == null) {
                    return false;
                }
                container.setMetadata(String(key), MetadataHelper.metadataValue(null, -1));
                return true;
            }
        },

        /**
         * Planners ProxyTarget 构造和转换工具。
         *
         * @namespace PlannersJs.target
         */
        target: {
            /**
             * 将 Bukkit Entity、Player、Location、Block、CommandSender 等对象包装为 ProxyTarget。
             *
             * @param {*} value 可被 ProxyTarget.of 支持的对象。
             * @returns {com.gitee.planners.api.job.target.ProxyTarget} 代理目标。
             */
            of: function (value) {
                return ProxyTarget.Companion.of(value);
            },
            /**
             * 将多个对象包装为 ProxyTargetContainer。
             *
             * @param {...*} values 可被 ProxyTarget.of 支持的对象列表。
             * @returns {com.gitee.planners.api.job.target.ProxyTargetContainer} 代理目标容器。
             */
            containerOf: function () {
                var container = new ProxyTargetContainer();
                for (var i = 0; i < arguments.length; i++) {
                    container.add(ProxyTarget.Companion.of(arguments[i]));
                }
                return container;
            },
            /**
             * 将 ProxyTarget 转为快照。
             *
             * @param {com.gitee.planners.api.job.target.ProxyTarget|null} value 代理目标。
             * @returns {Object|null} 代理目标快照。
             */
            data: toProxyTargetData,
            /**
             * 将 ProxyTargetContainer 转为快照数组。
             *
             * @param {com.gitee.planners.api.job.target.ProxyTargetContainer|null} value 代理目标容器。
             * @returns {Object[]} 代理目标快照数组。
             */
            containerData: toProxyTargetContainerData
        },

        /**
         * Planners 伤害代理工具。
         *
         * @namespace PlannersJs.damage
         */
        damage: {
            /**
             * 根据名称获取 DamageCause。
             *
             * @param {string} name 伤害来源名称，支持 Bukkit DamageCause 和已配置自定义来源。
             * @returns {com.gitee.planners.api.damage.DamageCause} 伤害来源。
             */
            cause: function (name) {
                return DamageCause.Companion.of(String(name));
            },
            /**
             * 根据名称获取 DamageCause，未定义时返回 null。
             *
             * @param {string} name 伤害来源名称。
             * @returns {com.gitee.planners.api.damage.DamageCause|null} 伤害来源或 null。
             */
            causeOrNull: function (name) {
                return DamageCause.Companion.ofOrNull(String(name));
            },
            /**
             * 构建 ProxyDamage。
             *
             * @param {org.bukkit.entity.LivingEntity|null} source 伤害来源实体，可为空。
             * @param {org.bukkit.entity.LivingEntity} target 受击实体。
             * @param {number} amount 基础伤害。
             * @param {string} [causeName] 伤害来源名称，未传时使用 Planners 默认来源。
             * @param {Object} [metadata] 附加元数据。
             * @returns {com.gitee.planners.api.damage.ProxyDamage} 伤害代理对象。
             */
            build: function (source, target, amount, causeName, metadata) {
                var builder = ProxyDamage.Companion.builder();
                if (source != null) {
                    builder.source(source);
                }
                builder.target(target);
                builder.damage(Number(amount));
                if (causeName != null) {
                    builder.cause(String(causeName));
                }
                if (metadata != null) {
                    var keys = Object.keys(metadata);
                    for (var i = 0; i < keys.length; i++) {
                        var key = keys[i];
                        builder.metadata(key, metadata[key]);
                    }
                }
                return builder.build();
            },
            /**
             * 执行 ProxyDamage。
             *
             * @param {com.gitee.planners.api.damage.ProxyDamage} proxyDamage 伤害代理对象。
             * @returns {com.gitee.planners.api.damage.DamageResult} 执行结果。
             */
            execute: function (proxyDamage) {
                return proxyDamage.execute();
            },
            /**
             * 将 ProxyDamage 转为快照。
             *
             * @param {com.gitee.planners.api.damage.ProxyDamage|null} proxyDamage 伤害代理对象。
             * @returns {Object|null} 伤害代理快照。
             */
            data: toProxyDamageData
        },

        /**
         * Java CompletableFuture 辅助工具。
         *
         * @namespace PlannersJs.future
         */
        future: {
            /**
             * 为 CompletableFuture 注册完成回调。
             *
             * @param {java.util.concurrent.CompletableFuture} future Java Future。
             * @param {function(*): void} callback 完成回调。
             * @returns {java.util.concurrent.CompletableFuture} thenAccept 返回的 Future。
             */
            then: futureThen
        }
    };

    /**
     * 将 Player 或 PlayerTemplate 统一解析为 PlayerTemplate。
     *
     * @param {org.bukkit.entity.Player|com.gitee.planners.core.player.PlayerTemplate} templateOrPlayer 玩家或档案。
     * @returns {com.gitee.planners.core.player.PlayerTemplate} 玩家档案。
     * @throws {Error} 参数为空时抛出。
     */
    function resolveTemplate(templateOrPlayer) {
        if (templateOrPlayer == null) {
            throw new Error("PlannersJs requires player or PlayerTemplate");
        }
        if (typeof templateOrPlayer.getOnlinePlayer === "function") {
            return templateOrPlayer;
        }
        return PlayerTemplateAPI.getPlannersTemplate(templateOrPlayer);
    }
})();
