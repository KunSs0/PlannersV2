import com.gitee.planners.util.unboxJavaToKotlin
import com.google.gson.Gson
import com.test.MathHelper

object Test {

    @JvmStatic
    fun main(args: Array<String>) {
        println(unboxJavaToKotlin(10.0f::class.java))
    }

}
