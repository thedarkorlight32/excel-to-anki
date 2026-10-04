package example.genanki

class IdGenerator(start: Long) {
    private var current = start

    fun next(): Long {
        val out = current
        current += 1
        return out
    }
}
