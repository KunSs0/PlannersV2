package com.gitee.planners.core.script.proxy

class ProxyStringList(private val data: List<String>) : Iterable<String> {

    override fun iterator(): Iterator<String> {
        return data.iterator()
    }
}
