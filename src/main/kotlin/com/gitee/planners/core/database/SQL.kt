package com.gitee.planners.core.database

import taboolib.module.database.Action
import taboolib.module.database.ActionInsert
import taboolib.module.database.Table
import java.sql.Connection
import java.sql.PreparedStatement
import javax.sql.DataSource

fun Table<*, *>.nullableInsert(dataSource: DataSource, func: ActionNullableInsert.() -> Unit) {
    workspace(dataSource) {
        val build = ActionNullableInsert(this@nullableInsert.name).also(func).build()
        this.executeUpdate(build.query, build)
    }.run()
}

class ActionNullableInsert(val table: String) : Action {

    /** 该行为执行完毕后的回调 */
    private var finallyCallback: (PreparedStatement.(Connection) -> Unit)? = null


    private val keys = mutableListOf<String>()

    /** 插入值 */
    private var values = ArrayList<Any>()

    fun set(name: String, value: Any?) {
        if (value != null) {
            keys += name
            values += value
        }
    }

    fun build(): ActionInsert {

        return ActionInsert(table, keys.toTypedArray()).also {
            it.values(values)
            it.onFinally {
                this@ActionNullableInsert.callFinally(this, it)
            }
        }
    }

    override val query: String
        get() = TODO("Not yet implemented")

    override fun onFinally(onFinally: PreparedStatement.(Connection) -> Unit) {
        this.finallyCallback = onFinally
    }

    override fun callFinally(preparedStatement: PreparedStatement, connection: Connection) {
        this.finallyCallback?.invoke(preparedStatement, connection)
    }


}
