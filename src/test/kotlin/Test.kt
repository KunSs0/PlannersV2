import com.google.gson.Gson

object Test {

    private val gson = Gson()

    @JvmStatic
    fun main(args: Array<String>) {
        val json = gson.toJson(10)
        println(json)
    }

}
