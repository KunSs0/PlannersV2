object Test {

    @JvmStatic
    fun main(args: Array<String>) {
        val block = ShapeBlock(3.0, 3.0, 3.0)

        block.build(0.0f).forEach { (type, vector) ->
            println("$type -> $vector")
        }
    }


}
