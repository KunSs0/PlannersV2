

object Test {

    @JvmStatic
    fun main(args: Array<String>) {

        val finder = NearestEntityFinder(Vector(0, 0, 0))
        val vectors = finder.request(listOf(Vector(0.0, 1.81, 0.0)))
        println(vectors)
    }


}
