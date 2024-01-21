
class NearestEntityFinder(val origin: Vector) {

    fun request(vectors: List<Vector>): List<Vector> {
        val boundingBoxes = vectors.map { entity ->
            val boundingBoxWidth = 0.6
            val boundingBoxHeight = 1.8
            entity to BoundingBox(
                entity.x - boundingBoxWidth, entity.y - boundingBoxHeight,
                entity.z - boundingBoxWidth, entity.x + boundingBoxWidth,
                entity.y + boundingBoxHeight, entity.z + boundingBoxWidth,
            )
        }

        return boundingBoxes.filter { it.second.contains(origin) }.map { it.first }
    }


}
