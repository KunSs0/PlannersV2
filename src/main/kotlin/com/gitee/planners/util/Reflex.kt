package com.gitee.planners.util

import taboolib.library.reflex.ClassField
import taboolib.library.reflex.ReflexClass
import java.lang.reflect.Field

object Reflex {


    fun getFieldsWithSuperclass(clazz: Class<*>): List<ClassField> {
        val fields = mutableListOf<ClassField>()
        var temp: Class<*>? = clazz
        while (temp != null) {
            fields += ReflexClass.of(temp).structure.fields
            temp = temp.superclass
        }
        return fields
    }

}
