/**
 * Script Engine - Common API Utils
 *
 * 统一命名空间: ScriptEngineUtils
 *
 * 设计约定:
 * - 本文件只放跨 API 可复用的小型工具，不放具体插件业务转换逻辑。
 * - 不声明裸全局工具函数，避免与 prelude.js 或其他插件脚本冲突。
 * - 依赖本文件的 API 库应显式读取 ScriptEngineUtils，缺失时直接抛出错误。
 */
(function () {
    var root = typeof globalThis !== "undefined" ? globalThis : this;
    var LinkedHashMap = Java.type("java.util.LinkedHashMap");

    /**
     * 读取 ScriptEngine 注入的全局函数。
     *
     * @param {string} name 全局函数名。
     * @returns {function} 已注入的全局函数。
     * @throws {Error} 当绑定不存在或不是函数时抛出。
     */
    function requireFunction(name) {
        var value = root[name];
        if (typeof value !== "function") {
            throw new Error("ScriptEngineUtils requires global function: " + name);
        }
        return value;
    }


    /**
     * 将 Java Collection / Iterable 风格对象转换为 JS 数组。
     *
     * @param {java.util.Collection|Array|null} javaCollection Java 集合、JS 数组或 null。
     * @returns {Array} JS 数组；参数为空时返回空数组。
     */
    function toArray(javaCollection) {
        if (javaCollection == null) {
            return [];
        }
        if (Array.isArray(javaCollection)) {
            return javaCollection;
        }
        var result = [];
        var it = javaCollection.iterator();
        while (it.hasNext()) {
            result.push(it.next());
        }
        return result;
    }

    /**
     * 将 Java Map 转换为普通 JS 对象。
     *
     * @param {java.util.Map|null} javaMap Java Map；为空时返回空对象。
     * @param {function(*, string): *} [mapper] 可选值转换器，参数为 value 和 key。
     * @returns {Object} 普通 JS 对象。
     */
    function mapToObject(javaMap, mapper) {
        var result = {};
        if (javaMap == null) {
            return result;
        }
        var it = javaMap.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            var key = String(entry.getKey());
            var value = entry.getValue();
            if (typeof mapper === "function") {
                result[key] = mapper(value, key);
            } else {
                result[key] = value;
            }
        }
        return result;
    }

    /**
     * 将普通 JS 对象转换为 java.util.LinkedHashMap。
     *
     * @param {Object|null} object JS 对象；为空时返回空 Map。
     * @returns {java.util.LinkedHashMap} Java Map。
     */
    function jsObjectToJavaMap(object) {
        var map = new LinkedHashMap();
        if (object == null) {
            return map;
        }
        var keys = Object.keys(object);
        for (var i = 0; i < keys.length; i++) {
            var key = keys[i];
            map.put(key, object[key]);
        }
        return map;
    }

    /**
     * 获取 Java 枚举或普通对象的名称字符串。
     *
     * @param {*|null} value Java enum、字符串或任意对象。
     * @returns {string|null} 枚举 name() 或 String(value)；空值返回 null。
     */
    function enumName(value) {
        if (value == null) {
            return null;
        }
        if (typeof value.name === "function") {
            return value.name();
        }
        return String(value);
    }

    /**
     * 如果目标对象存在指定无参方法，则调用并返回结果。
     *
     * @param {*|null} target Java 或 JS 对象。
     * @param {string} methodName 无参方法名。
     * @returns {*|null} 调用结果；对象或方法不存在时返回 null。
     */
    function callIfPresent(target, methodName) {
        if (target != null && typeof target[methodName] === "function") {
            return target[methodName]();
        }
        return null;
    }

    /**
     * 为 Java CompletableFuture 注册完成回调。
     *
     * @param {java.util.concurrent.CompletableFuture} future Java Future。
     * @param {function(*): void} callback 完成回调。
     * @returns {java.util.concurrent.CompletableFuture} thenAccept 返回的 Future。
     * @throws {Error} callback 不是函数时抛出。
     */
    function futureThen(future, callback) {
        if (typeof callback !== "function") {
            throw new Error("ScriptEngineUtils.futureThen requires a function callback");
        }
        return future.thenAccept(function (value) {
            callback(value);
        });
    }

    /**
     * 跨 API 复用工具命名空间。
     *
     * @namespace ScriptEngineUtils
     */
    root.ScriptEngineUtils = {
        requireFunction: requireFunction,
        toArray: toArray,
        mapToObject: mapToObject,
        jsObjectToJavaMap: jsObjectToJavaMap,
        enumName: enumName,
        callIfPresent: callIfPresent,
        futureThen: futureThen
    };
})();
