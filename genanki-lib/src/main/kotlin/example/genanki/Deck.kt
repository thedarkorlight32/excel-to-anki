package example.genanki

import com.fasterxml.jackson.databind.node.ObjectNode
import java.sql.Connection

class Deck(
    val deckId: Long,
    val name: String,
    private val description: String = ""
) {
    private val notes = mutableListOf<Note>()
    private val models = linkedMapOf<Long, Model>()

    fun addNote(note: Note) {
        notes += note
    }

    private fun addModel(model: Model) {
        models[model.modelId] = model
    }

    private fun toJson(): Map<String, Any> = mapOf(
        "collapsed" to false,
        "conf" to 1,
        "desc" to description,
        "dyn" to 0,
        "extendNew" to 0,
        "extendRev" to 50,
        "id" to deckId,
        "lrnToday" to listOf(163, 2),
        "mod" to 1425278051,
        "name" to name,
        "newToday" to listOf(163, 2),
        "revToday" to listOf(163, 0),
        "timeToday" to listOf(163, 23598),
        "usn" to -1
    )

    fun writeToDb(connection: Connection, timestampSeconds: Long, idGen: IdGenerator) {
        val decksJson = connection.createStatement().use { st ->
            st.executeQuery("SELECT decks FROM col LIMIT 1").use { rs ->
                check(rs.next()) { "col table is empty" }
                rs.getString(1)
            }
        }
        val decksNode = jsonMapper.readTree(decksJson) as ObjectNode
        decksNode.set<ObjectNode>(deckId.toString(), jsonMapper.valueToTree(toJson()))
        connection.prepareStatement("UPDATE col SET decks = ?").use { stmt ->
            stmt.setString(1, decksNode.toString())
            stmt.executeUpdate()
        }

        notes.forEach { addModel(it.model) }

        val modelsJson = connection.createStatement().use { st ->
            st.executeQuery("SELECT models FROM col LIMIT 1").use { rs ->
                check(rs.next()) { "col table is empty" }
                rs.getString(1)
            }
        }
        val modelsNode = jsonMapper.readTree(modelsJson) as ObjectNode
        models.values.forEach { model ->
            modelsNode.set<ObjectNode>(model.modelId.toString(), jsonMapper.valueToTree(model.toJson(timestampSeconds, deckId)))
        }
        connection.prepareStatement("UPDATE col SET models = ?").use { stmt ->
            stmt.setString(1, modelsNode.toString())
            stmt.executeUpdate()
        }

        notes.forEach { it.writeToDb(connection, timestampSeconds, deckId, idGen) }
    }
}
