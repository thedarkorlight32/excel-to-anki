package example.genanki

import java.sql.Connection

class Note(
    val model: Model,
    val fields: List<String>,
    sortField: String? = null,
    tags: List<String> = emptyList(),
    guid: String? = null,
    private val due: Int = 0
) {
    private val validatedTags = tags.toMutableList().also {
        it.forEach { tag -> require(' ' !in tag) { "Tag \"$tag\" contains a space; this is not allowed." } }
    }

    private val explicitGuid = guid
    private val explicitSortField = sortField

    private val cards: List<Card> by lazy {
        when (model.modelType) {
            Model.FRONT_BACK -> frontBackCards()
            else -> error("Only FRONT_BACK modelType is supported in this Kotlin rewrite.")
        }
    }

    private val resolvedSortField: String
        get() = explicitSortField ?: fields[model.sortFieldIndex]

    private val resolvedGuid: String
        get() = explicitGuid ?: guidFor(*fields.toTypedArray())

    fun writeToDb(connection: Connection, timestampSeconds: Long, deckId: Long, idGen: IdGenerator) {
        require(model.fields.size == fields.size) {
            "Number of fields in Model does not match number of fields in Note: model has ${model.fields.size} fields, note has ${fields.size}."
        }

        connection.prepareStatement("INSERT INTO notes VALUES(?,?,?,?,?,?,?,?,?,?,?);").use { stmt ->
            stmt.setLong(1, idGen.next())
            stmt.setString(2, resolvedGuid)
            stmt.setLong(3, model.modelId)
            stmt.setLong(4, timestampSeconds)
            stmt.setInt(5, -1)
            stmt.setString(6, formatTags())
            stmt.setString(7, formatFields())
            stmt.setString(8, resolvedSortField)
            stmt.setInt(9, 0)
            stmt.setInt(10, 0)
            stmt.setString(11, "")
            stmt.executeUpdate()
        }

        val noteId = connection.createStatement().use { st ->
            st.executeQuery("SELECT last_insert_rowid();").use { rs ->
                check(rs.next()) { "Could not obtain inserted note ID." }
                rs.getLong(1)
            }
        }

        cards.forEach { it.writeToDb(connection, timestampSeconds, deckId, noteId, idGen, due) }
    }

    private fun frontBackCards(): List<Card> {
        val out = mutableListOf<Card>()
        for (entry in model.req) {
            val cardOrd = entry[0] as Int
            val mode = entry[1] as String
            @Suppress("UNCHECKED_CAST")
            val requiredOrdinals = entry[2] as List<Int>
            val values = requiredOrdinals.map { fields[it] }
            val include = when (mode) {
                "all" -> values.all { it.isNotEmpty() }
                "any" -> values.any { it.isNotEmpty() }
                else -> false
            }
            if (include) out += Card(cardOrd)
        }
        return out
    }

    private fun formatFields(): String = fields.joinToString("\u001f")

    private fun formatTags(): String = " ${validatedTags.joinToString(" ")} "
}
