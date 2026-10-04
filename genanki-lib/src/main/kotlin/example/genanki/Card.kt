package example.genanki

import java.sql.Connection

class Card(private val ord: Int, private val suspend: Boolean = false) {
    fun writeToDb(connection: Connection, timestampSeconds: Long, deckId: Long, noteId: Long, idGen: IdGenerator, due: Int = 0) {
        val queue = if (suspend) -1 else 0
        connection.prepareStatement("INSERT INTO cards VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);").use { stmt ->
            stmt.setLong(1, idGen.next())
            stmt.setLong(2, noteId)
            stmt.setLong(3, deckId)
            stmt.setInt(4, ord)
            stmt.setLong(5, timestampSeconds)
            stmt.setInt(6, -1)
            stmt.setInt(7, 0)
            stmt.setInt(8, queue)
            stmt.setInt(9, due)
            stmt.setInt(10, 0)
            stmt.setInt(11, 0)
            stmt.setInt(12, 0)
            stmt.setInt(13, 0)
            stmt.setInt(14, 0)
            stmt.setInt(15, 0)
            stmt.setInt(16, 0)
            stmt.setInt(17, 0)
            stmt.setString(18, "")
            stmt.executeUpdate()
        }
    }
}
