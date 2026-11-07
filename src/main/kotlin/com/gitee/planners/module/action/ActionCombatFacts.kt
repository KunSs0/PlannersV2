package com.gitee.planners.module.action

/**
 * 战斗侧事实读取入口。
 *
 * `target.anchored` 需要知道「施法者是不是这个怪的当前仇恨目标」，该判定依赖战斗插件，
 * 由宿主通过 [installAnchored] 注入；未安装时一律返回 false。
 *
 * 与属性读取器同理：Planners 只声明需要什么事实，不引用战斗插件的类。
 */
object ActionCombatFacts {

    /**
     * 仇恨锚定判定：参数为目标与施法者。
     */
    fun interface AnchoredReader {

        /**
         * 判断施法者是否为目标的当前仇恨目标。
         *
         * @param target 目标实体。
         * @param sender 施法者。
         * @return 施法者是当前仇恨目标时返回 true。
         */
        fun isAnchored(target: Any?, sender: Any?): Boolean
    }

    /**
     * 连招播放变量读取器：参数为回调载荷与变量名。
     */
    fun interface PlaybackReader {

        /**
         * 读取播放变量。
         *
         * @param payload 回调载荷。
         * @param key 变量名。
         * @return 变量值；没有播放上下文时返回 null。
         */
        fun read(payload: Map<String, Any?>, key: String): Any?
    }

    private val defaultPlaybackReader = PlaybackReader { _, _ -> null }

    @Volatile
    private var playbackReader: PlaybackReader = defaultPlaybackReader

    /**
     * 当前生效的播放变量读取器，供作用域直接引用。
     *
     * @return 读取器函数。
     */
    @JvmStatic
    val playbackReaderFunction: (Map<String, Any?>, String) -> Any? = { payload, key ->
        playbackReader.read(payload, key)
    }

    /**
     * 安装连招播放变量读取器。
     *
     * @param value 读取器实现。
     */
    fun installPlayback(value: PlaybackReader) {
        playbackReader = value
    }

    private val defaultReader = AnchoredReader { _, _ -> false }

    @Volatile
    private var reader: AnchoredReader = defaultReader

    /**
     * 当前生效的读取器，供作用域直接引用。
     *
     * @return 读取器函数。
     */
    @JvmStatic
    val anchoredReader: (Any?, Any?) -> Boolean = { target, sender ->
        reader.isAnchored(target, sender)
    }

    /**
     * 安装仇恨锚定读取器。
     *
     * @param value 读取器实现。
     */
    fun installAnchored(value: AnchoredReader) {
        reader = value
    }

    /**
     * 恢复默认读取器。
     */
    fun reset() {
        reader = defaultReader
        playbackReader = defaultPlaybackReader
    }
}
