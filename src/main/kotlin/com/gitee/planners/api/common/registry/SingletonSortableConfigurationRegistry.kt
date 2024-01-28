package com.gitee.planners.api.common.registry

import java.util.Collections

abstract class SingletonSortableConfigurationRegistry<T>(path: String) : SingletonConfigurationRegistry<T>(path) where T : Unique, T : Sortable {


    override fun onLoaded() {
        super.onLoaded()
        val sortableList = ArrayList<T>(this.getValues())
        sortableList.sortWith { o1, o2 -> o1.priority.compareTo(o2.priority) }
        this.table.clear()
        sortableList.forEach {
            this[it.id] = it
        }
    }

    fun indexOf(data: T): Int {
        return getValues().indexOf(data)
    }

}
