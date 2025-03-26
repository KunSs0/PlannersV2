package com.gitee.planners.util.math

import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency

@RuntimeDependencies(
        RuntimeDependency(
                value = "!org.ejml:ejml-core:0.41",
                test = "!org.ejml.EjmlVersion"
        ),
        RuntimeDependency(
                value = "!org.ejml:ejml-simple:0.41",
                test = "!org.ejml.simple.SimpleBase"
        ),
        RuntimeDependency(
                value = "!org.ejml:ejml-fdense:0.41",
                test = "!org.ejml.generic.GenericMatrixOps_F32"
        ),
        RuntimeDependency(
                value = "!org.ejml:ejml-ddense:0.41",
                test = "!org.ejml.generic.GenericMatrixOps_F64"
        ),
)
object SimpleMatrixLoader
