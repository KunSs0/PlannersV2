package com.gitee.planners.core.action.common

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper


@KetherEditor.Document("cos <value:Number>")
@CombinationKetherParser.Used
fun cos() = KetherHelper.combinedKetherParser() {
    it.group(double()).apply(it) { value ->
        now { kotlin.math.cos(value) }
    }
}

@KetherEditor.Document("sin <value:Number>")
@CombinationKetherParser.Used
fun sin() = KetherHelper.combinedKetherParser {
    it.group(double()).apply(it) { value ->
        now { kotlin.math.sin(value) }
    }
}

@KetherEditor.Document("radians <value:Number>")
@CombinationKetherParser.Used
fun radians() = KetherHelper.combinedKetherParser {
    it.group(double()).apply(it) { value ->
        now { Math.toRadians(value) }
    }
}

@KetherEditor.Document("pow <value:Number>")
@CombinationKetherParser.Used
fun pow() = KetherHelper.combinedKetherParser {
    it.group(double(), double()).apply(it) { value,t ->
        now { Math.pow(value,t) }
    }
}

@KetherEditor.Document("tan <value:Number>")
@CombinationKetherParser.Used
fun tan() = KetherHelper.combinedKetherParser {
    it.group(double()).apply(it) { value ->
        now { kotlin.math.tan(value) }
    }
}

@KetherEditor.Document("atan <value:Number>")
@CombinationKetherParser.Used
fun atan() = KetherHelper.combinedKetherParser {
    it.group(double()).apply(it) { value ->
        now { kotlin.math.atan(value) }
    }
}

@KetherEditor.Document("abs <value:Number>")
@CombinationKetherParser.Used
fun abs() = KetherHelper.combinedKetherParser {
    it.group(double()).apply(it) { value ->
        now { kotlin.math.abs(value) }
    }
}
