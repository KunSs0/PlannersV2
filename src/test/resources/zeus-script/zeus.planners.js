/**
 * Zeus 专用 Planners 数据 API。
 *
 * 该文件只定义 Zeus 职业界面使用的 JSON 投影，不扩展通用 PlannersJs API，
 * 也不持有玩家快照或处理任何跨端 RPC。
 */
(function () {

if (typeof ZeusJs !== "object" || ZeusJs == null) {
    throw new Error("ZeusJs must be loaded before zeus.planners.js");
}

var DynamicSkillIcon = Java.type("com.gitee.planners.core.skill.formatter.DynamicSkillIcon").Companion;
var SkillTreeNodeEffectService = Java.type("com.gitee.planners.core.skilltree.SkillTreeNodeEffectService").INSTANCE;
var Bukkit = Java.type("org.bukkit.Bukkit");
var SNAPSHOT_TICK_CACHE = {};

function toImmutableSkillData(skill, player, template, context) {
    var level = SkillTreeNodeEffectService.getSkillLevel(template, String(skill.getId()));
    var skillId = String(skill.getId());
    // 技能展示数据只随配置与等级变化，走注册表配置缓存；请求内索引仍写入 context 供节点组装引用
    var data = PlannersJs.registry.cached(
        "skillDisplay",
        skillId + ":" + String(level),
        function () {
            var renderedIcon = DynamicSkillIcon.render(player, skill, Number(level));
            return {
                id: skillId,
                name: String(skill.getName()),
                startedLevel: Number(skill.getStartedLevel()),
                maxLevel: Number(skill.getMaxLevel()),
                categories: PlannersJs.convert.toArray(skill.getCategories()),
                iconItemId: skill.getIconItemId(),
                displayIconName: renderedIcon.getName(),
                displayIconLore: PlannersJs.convert.toArray(renderedIcon.getLore())
            };
        }
    );
    context.skillDataById[skillId] = data;
    return data;
}

function toRequirementData(requirement) {
    return {
        nodeId: String(requirement.getNodeId()),
        minLevel: Number(requirement.getMinLevel())
    };
}

function getTreeStructure(tree) {
    return PlannersJs.registry.cached(
        "tree",
        String(tree.getId()),
        function () {
            var graph = tree.getGraph();
            var nodes = [];
            var nodeIterator = tree.getNodes().values().iterator();
            while (nodeIterator.hasNext()) {
                var node = nodeIterator.next();
                var nodeId = String(node.getId());
                var position = node.getPosition();
                var requirements = graph.get(nodeId);
                var requirementData = [];
                if (requirements != null) {
                    var requirementValues = PlannersJs.convert.toArray(requirements);
                    for (var requirementIndex = 0; requirementIndex < requirementValues.length; requirementIndex++) {
                        requirementData.push(toRequirementData(requirementValues[requirementIndex]));
                    }
                }
                var type = String(node.getType()).toLowerCase();
                var definition = {
                    id: nodeId,
                    type: type,
                    x: Number(position.getX()),
                    y: Number(position.getY()),
                    maxLevel: Number(node.getMaxLevel()),
                    requirements: requirementData
                };
                if (type === "skill") {
                    var immutableSkill = PlannersJs.registry.get("skill", node.getSkillId());
                    if (immutableSkill == null) {
                        throw new Error("Skill tree node references an unknown skill: " + String(node.getSkillId()));
                    }
                    definition.immutableSkill = immutableSkill;
                    definition.skillId = String(node.getSkillId());
                } else if (type === "attribute") {
                    definition.provider = {
                        id: String(node.getProviderId()),
                        values: PlannersJs.convert.mapToObject(node.getProviderValues())
                    };
                } else {
                    throw new Error("Unknown skill tree node type: " + type);
                }
                nodes.push(definition);
            }
            return {
                nodes: nodes
            };
        }
    );
}

function toTreeNodeData(treeId, definition, level, canAdvance, hints, context) {
    var nodeId = definition.id;
    var data = {
        id: nodeId,
        type: definition.type,
        position: {
            x: definition.x,
            y: definition.y
        },
        level: level,
        maxLevel: definition.maxLevel,
        requirements: definition.requirements,
        canAdvance: canAdvance,
        hints: hints
    };
    if (data.type === "skill") {
        var skillData = context.skillDataById[definition.skillId];
        if (skillData == null) {
            throw new Error("Missing immutable skill data for skill tree node: " + treeId + "/" + nodeId);
        }
        data.skill = skillData;
    } else if (data.type === "attribute") {
        data.provider = definition.provider;
    } else {
        throw new Error("Unknown skill tree node type: " + data.type);
    }
    return data;
}

function toPlayerTreeData(route, tree, context) {
    var nodes = [];
    var structure = getTreeStructure(tree);
    var treeId = String(tree.getId());
    var nodeCount = route.getNodeCount(treeId);
    if (nodeCount !== structure.nodes.length) {
        throw new Error("Skill tree runtime node count mismatch: " + treeId);
    }
    for (var nodeIndex = 0; nodeIndex < structure.nodes.length; nodeIndex++) {
        var definition = structure.nodes[nodeIndex];
        var nodeId = route.getNodeIdAt(treeId, nodeIndex);
        nodes.push(toTreeNodeData(
            treeId,
            definition,
            Number(route.getNodeLevel(treeId, nodeId)),
            route.canAdvanceNode(treeId, nodeId),
            route.getNodeHints(treeId, nodeId),
            context
        ));
    }
    var treeData = {
        id: String(tree.getId()),
        name: String(tree.getName()),
        type: String(tree.getType()).toLowerCase(),
        nodes: nodes
    };
    return treeData;
}

function toImmutableRouterData(router, player, template, context, includedSkillIds) {
    var structure = getRouterStructure(router);
    var jobs = [];
    for (var structureIndex = 0; structureIndex < structure.jobs.length; structureIndex++) {
        var structureJob = structure.jobs[structureIndex];
        var route = structureJob.route;
        var skills = [];
        var immutableSkills = structureJob.skills;
        for (var i = 0; i < immutableSkills.length; i++) {
            var skillId = String(immutableSkills[i].getId());
            if (includedSkillIds != null && includedSkillIds[skillId] !== true) {
                continue;
            }
            skills.push(toImmutableSkillData(immutableSkills[i], player, template, context));
        }
        jobs.push({
            id: structureJob.id,
            name: structureJob.name,
            branchIds: structureJob.branchIds,
            skillTreeIds: structureJob.skillTreeIds,
            iconItemId: structureJob.iconItemId,
            display: structureJob.display,
            skills: skills
        });
    }
    return {
        routerId: structure.routerId,
        name: structure.name,
        originateJobId: structure.originateJobId,
        jobs: jobs
    };
}

function getRouterStructure(router) {
    return PlannersJs.registry.cached(
        "router",
        String(router.getId()),
        function () {
            var jobs = [];
            var routeIterator = router.getRoutes().values().iterator();
            while (routeIterator.hasNext()) {
                var route = routeIterator.next();
                var job = route.getJob();
                var branchIds = [];
                var branches = PlannersJs.convert.toArray(route.getBranches());
                for (var branchIndex = 0; branchIndex < branches.length; branchIndex++) {
                    branchIds.push(String(branches[branchIndex].getId()));
                }
                jobs.push({
                    route: route,
                    id: String(route.getId()),
                    name: String(job.getName()),
                    branchIds: branchIds,
                    skillTreeIds: PlannersJs.convert.toArray(route.getSkillTreeIds()),
                    iconItemId: route.getIconItemId(),
                    display: {
                        icon: {
                            name: job.getDisplayIconName(),
                            lore: PlannersJs.convert.toArray(job.getDisplayIconLore())
                        }
                    },
                    skills: PlannersJs.convert.toArray(job.getImmutableSkillValues())
                });
            }
            var originate = router.getOriginate();
            return {
                routerId: String(router.getId()),
                name: String(router.getName()),
                originateJobId: originate == null ? null : String(originate.getId()),
                jobs: jobs
            };
        }
    );
}

function snapshotCacheKey(player, snapshotType) {
    return String(player.getUniqueId()) + ":" + snapshotType;
}

function getSnapshotFromCurrentTick(player, snapshotType) {
    var key = snapshotCacheKey(player, snapshotType);
    var cached = SNAPSHOT_TICK_CACHE[key];
    if (cached == null) {
        return null;
    }
    if (cached.tick !== Bukkit.getCurrentTick()) {
        delete SNAPSHOT_TICK_CACHE[key];
        return null;
    }
    return cached.data;
}

function saveSnapshotForCurrentTick(player, snapshotType, data) {
    SNAPSHOT_TICK_CACHE[snapshotCacheKey(player, snapshotType)] = {
        tick: Bukkit.getCurrentTick(),
        data: data
    };
}

function clearPlayerSnapshotCache(player) {
    var playerId = String(player.getUniqueId());
    delete SNAPSHOT_TICK_CACHE[playerId + ":job_skill"];
    delete SNAPSHOT_TICK_CACHE[playerId + ":skill_tree"];
}

function toPlayerJobData(route, player, template, context, includeTrees, includedSkillIds) {
    // 注册技能表只随 bindingId 变化，走注册表缓存；reload 后上下文重建自然失效。
    // 缓存的是全量只读表；includedSkillIds 过滤在其上按需构建。
    var allSkills = PlannersJs.registry.cached(
        "registeredSkills",
        String(route.getBindingId()),
        function () {
            var result = {};
            var skillIterator = route.getRegisteredSkill().entrySet().iterator();
            while (skillIterator.hasNext()) {
                var entry = skillIterator.next();
                result[String(entry.getKey())] = Number(entry.getValue().getLevel());
            }
            return result;
        }
    );
    var skills = {};
    for (var cachedSkillId in allSkills) {
        if (includedSkillIds == null || includedSkillIds[cachedSkillId] === true) {
            skills[cachedSkillId] = {
                level: allSkills[cachedSkillId]
            };
        }
    }
    var trees = [];
    if (includeTrees) {
        var treeView = PlannersJs.route.getPlayerRoute(route, player);
        var treeValues = PlannersJs.convert.toArray(route.getSkillTrees());
        for (var i = 0; i < treeValues.length; i++) {
            trees.push(toPlayerTreeData(treeView, treeValues[i], context));
        }
    }
    return {
        bindingId: Number(route.getBindingId()),
        parentId: Number(route.getParentId()),
        id: String(route.getJobId()),
        skills: skills,
        trees: trees
    };
}

function toKeyName(keyId) {
    var keyBinding = PlannersJs.registry.get("keybinding", keyId);
    if (keyBinding == null) {
        throw new Error("Unknown Planners keybinding: " + keyId);
    }
    return String(keyBinding.getName());
}

function getBackpackStructure() {
    var backpack = PlannersJs.registry.backpack();
    return PlannersJs.registry.cached(
        "backpack",
        "default",
        function () {
            var pages = [];
            var pageIterator = backpack.getPages().values().iterator();
            while (pageIterator.hasNext()) {
                var page = pageIterator.next();
                var slots = [];
                var slotIterator = page.getSlots().values().iterator();
                while (slotIterator.hasNext()) {
                    var slot = slotIterator.next();
                    var keyId = String(slot.getKey());
                    slots.push({
                        id: String(slot.getId()),
                        key: keyId,
                        keyName: toKeyName(keyId),
                        categories: PlannersJs.convert.toArray(slot.getCategories())
                    });
                }
                pages.push({
                    id: String(page.getId()),
                    name: String(page.getName()),
                    slots: slots
                });
            }
            return {
                pages: pages
            };
        }
    );
}

function collectPlayerBackpackData(template) {
    var structure = getBackpackStructure();
    var pages = [];
    var equippedSkillIds = {};
    var skillLevels = {};
    for (var pageIndex = 0; pageIndex < structure.pages.length; pageIndex++) {
        var page = structure.pages[pageIndex];
        var slots = [];
        var equippedBySlot = template.getEquippedSkillsForPage(page.id);
        for (var slotIndex = 0; slotIndex < page.slots.length; slotIndex++) {
            var slot = page.slots[slotIndex];
            var equippedSkill = equippedBySlot.get(slot.id);
            var skillId = equippedSkill == null ? null : String(equippedSkill.getId());
            if (skillId != null) {
                equippedSkillIds[skillId] = true;
                skillLevels[skillId] = Number(equippedSkill.getLevel());
            }
            slots.push({
                id: slot.id,
                key: slot.key,
                keyName: slot.keyName,
                categories: slot.categories,
                skillId: skillId
            });
        }
        pages.push({
            id: page.id,
            name: page.name,
            slots: slots
        });
    }
    return {
        pages: pages,
        equippedSkillIds: equippedSkillIds,
        skillLevels: skillLevels
    };
}

function toJobSkillPlayerData(player, template, playerRouter, backpackData) {
    var currentJobId = String(playerRouter.getCurrentRoute().getJobId());
    var currentBackpackPageId = String(PlannersJs.backpack.currentPage(template));
    return {
        level: Number(playerRouter.getLevel()),
        currentJobId: currentJobId,
        currentBackpackPageId: currentBackpackPageId,
        skillLevels: backpackData.skillLevels,
        backpack: backpackData.pages
    };
}

function toSkillTreePlayerData(player, template, playerRouter, context, backpackData) {
    var jobs = [];
    var routeLine = PlannersJs.convert.toArray(playerRouter.getRouteLine());
    for (var routeIndex = 0; routeIndex < routeLine.length; routeIndex++) {
        jobs.push(toPlayerJobData(routeLine[routeIndex], player, template, context, true, null));
    }
    var currentJobId = String(playerRouter.getCurrentRoute().getJobId());
    var currentBackpackPageId = String(PlannersJs.backpack.currentPage(template));
    return {
        currentJobId: currentJobId,
        currentBackpackPageId: currentBackpackPageId,
        jobs: jobs,
        skillLevels: backpackData.skillLevels,
        backpack: backpackData.pages
    };
}

function toPlayerRouterData(player, template, playerRouter, context, includeTrees, includedSkillIds, backpackPages) {
    var jobs = [];
    var routeLine = PlannersJs.convert.toArray(playerRouter.getRouteLine());
    for (var i = 0; i < routeLine.length; i++) {
        jobs.push(toPlayerJobData(routeLine[i], player, template, context, includeTrees, includedSkillIds));
    }
    var experienceMax = Number(playerRouter.getExperienceMax(player));
    var currentJobId = String(playerRouter.getCurrentRoute().getJobId());
    var currentBackpackPageId = String(PlannersJs.backpack.currentPage(template));
    return {
        bindingId: Number(playerRouter.getBindingId()),
        routerId: String(playerRouter.getRouterId()),
        level: Number(playerRouter.getLevel()),
        experience: Number(playerRouter.getExperience()),
        experienceMax: experienceMax,
        minLevel: Number(playerRouter.getMinLevel()),
        maxLevel: Number(playerRouter.getMaxLevel()),
        skillPointsCurrent: Number(playerRouter.getSkillPointsCurrent()),
        skillPointsUsed: Number(playerRouter.getSkillPointsUsed()),
        currentRouteId: Number(playerRouter.getCurrentRouteId()),
        currentJobId: currentJobId,
        jobs: jobs,
        currentBackpackPageId: currentBackpackPageId,
        backpack: backpackPages
    };
}

ZeusJs.planners = {
    jobSkillSnapshot: function (player, template, playerRouter) {
        var context = {
            skillDataCache: {},
            skillDataById: {}
        };
        var backpackData = collectPlayerBackpackData(template);
        var immutable = toImmutableRouterData(
            playerRouter.getRouter(),
            player,
            template,
            context,
            backpackData.equippedSkillIds
        );
        var playerData = toJobSkillPlayerData(player, template, playerRouter, backpackData);
        return {
            immutable: immutable,
            player: playerData
        };
    },
    skillTreeSnapshot: function (player, template, playerRouter) {
        var context = {
            skillDataCache: {},
            skillDataById: {}
        };
        var backpackData = collectPlayerBackpackData(template);
        var immutable = toImmutableRouterData(playerRouter.getRouter(), player, template, context, null);
        var playerData = toSkillTreePlayerData(player, template, playerRouter, context, backpackData);
        return {
            immutable: immutable,
            player: playerData
        };
    },
    getJobSkillSnapshot: function (player, template, playerRouter) {
        var cached = getSnapshotFromCurrentTick(player, "job_skill");
        if (cached != null) {
            return {
                data: cached,
                cacheHit: true
            };
        }
        var data = this.jobSkillSnapshot(player, template, playerRouter);
        saveSnapshotForCurrentTick(player, "job_skill", data);
        return {
            data: data,
            cacheHit: false
        };
    },
    getSkillTreeSnapshot: function (player, template, playerRouter) {
        var cached = getSnapshotFromCurrentTick(player, "skill_tree");
        if (cached != null) {
            return {
                data: cached,
                cacheHit: true
            };
        }
        var data = this.skillTreeSnapshot(player, template, playerRouter);
        saveSnapshotForCurrentTick(player, "skill_tree", data);
        return {
            data: data,
            cacheHit: false
        };
    },
    clearSnapshotCache: function (player) {
        clearPlayerSnapshotCache(player);
    }
};

})();
