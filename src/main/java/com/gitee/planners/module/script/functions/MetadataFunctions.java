package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.PlayerTemplateAPI;
import com.gitee.planners.api.common.entity.ProxyBukkitEntity;
import com.gitee.planners.api.common.metadata.EntityMetadataManager;
import com.gitee.planners.api.common.metadata.Metadata;
import com.gitee.planners.api.common.metadata.MetadataContainer;
import com.gitee.planners.api.common.metadata.MetadataHelperKt;
import com.gitee.planners.api.common.metadata.MetadataTypeToken;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Metadata 元数据操作函数
 * <pre>{@code
 * // JS: hasMeta("key", entity)
 * // JS: getMeta("key", entity)
 * // JS: setMeta("key", value, entity)
 * // JS: setMetaTimeout("key", value, ticks, entity)
 * // JS: removeMeta("key", entity)
 * }</pre>
 */
public final class MetadataFunctions {

    private MetadataFunctions() {}

    public static void register() {
        // hasMeta(key, entity)
        GlobalFunctions.register("hasMeta", args -> {
            String key = ScriptArgs.getString(args, 0);
            if (key == null) return null;
            Object entityArg = ScriptArgs.get(args, 1);
            if (!(entityArg instanceof Entity)) return null;
            return getContainer((Entity) entityArg).get(key) != null;
        });

        // getMeta(key, entity)
        GlobalFunctions.register("getMeta", args -> {
            String key = ScriptArgs.getString(args, 0);
            if (key == null) return null;
            Object entityArg = ScriptArgs.get(args, 1);
            if (!(entityArg instanceof Entity)) return null;
            Metadata token = getContainer((Entity) entityArg).get(key);
            return token != null ? token.any() : null;
        });

        // setMeta(key, value, entity)
        GlobalFunctions.register("setMeta", args -> {
            String key = ScriptArgs.getString(args, 0);
            if (key == null) return null;
            Object value = ScriptArgs.get(args, 1);
            if (value == null) return null;
            Object entityArg = ScriptArgs.get(args, 2);
            if (!(entityArg instanceof Entity)) return null;
            getContainer((Entity) entityArg).set(key, MetadataHelperKt.metadataValue(value, -1L));
            return null;
        });

        // setMetaTimeout(key, value, ticks, entity)
        GlobalFunctions.register("setMetaTimeout", args -> {
            String key = ScriptArgs.getString(args, 0);
            if (key == null) return null;
            Object value = ScriptArgs.get(args, 1);
            if (value == null) return null;
            long timeout = ScriptArgs.getLong(args, 2);
            Object entityArg = ScriptArgs.get(args, 3);
            if (!(entityArg instanceof Entity)) return null;
            getContainer((Entity) entityArg).set(key, MetadataHelperKt.metadataValue(value, timeout));
            return null;
        });

        // removeMeta(key, entity)
        GlobalFunctions.register("removeMeta", args -> {
            String key = ScriptArgs.getString(args, 0);
            if (key == null) return null;
            Object entityArg = ScriptArgs.get(args, 1);
            if (!(entityArg instanceof Entity)) return null;
            getContainer((Entity) entityArg).set(key, new MetadataTypeToken.Void());
            return null;
        });
    }

    private static MetadataContainer getContainer(Entity entity) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            return PlayerTemplateAPI.INSTANCE.getOrNull(player.getUniqueId());
        }
        return EntityMetadataManager.INSTANCE.get(new ProxyBukkitEntity(entity));
    }
}
