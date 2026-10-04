package example.genanki

data class ModelField(
    val name: String,
    val font: String = "Liberation Sans",
    val media: List<String> = emptyList(),
    val rtl: Boolean = false,
    val size: Int = 20,
    val sticky: Boolean = false
)

data class ModelTemplate(
    val name: String,
    val qfmt: String,
    val afmt: String,
    val bafmt: String = "",
    val bqfmt: String = "",
    val bfont: String = "",
    val bsize: Int = 0,
    val did: Long? = null
)

class Model(
    val modelId: Long,
    val name: String,
    val fields: List<ModelField>,
    val templates: List<ModelTemplate>,
    val css: String = "",
    val modelType: Int = FRONT_BACK,
    val latexPre: String = DEFAULT_LATEX_PRE,
    val latexPost: String = DEFAULT_LATEX_POST,
    val sortFieldIndex: Int = 0
) {
    companion object {
        const val FRONT_BACK = 0
        const val CLOZE = 1
        const val DEFAULT_LATEX_PRE = "\\documentclass[12pt]{article}\n\\special{papersize=3in,5in}\n\\usepackage[utf8]{inputenc}\n\\usepackage{amssymb,amsmath}\n\\pagestyle{empty}\n\\setlength{\\parindent}{0in}\n\\begin{document}\n"
        const val DEFAULT_LATEX_POST = "\\end{document}"
    }

    val req: List<List<Any>> by lazy { computeReq() }

    fun toJson(timestampSeconds: Long, deckId: Long): Map<String, Any?> {
        val jsonTemplates = templates.mapIndexed { ord, t ->
            mapOf(
                "name" to t.name,
                "qfmt" to t.qfmt,
                "afmt" to t.afmt,
                "ord" to ord,
                "bafmt" to t.bafmt,
                "bqfmt" to t.bqfmt,
                "bfont" to t.bfont,
                "bsize" to t.bsize,
                "did" to t.did
            )
        }
        val jsonFields = fields.mapIndexed { ord, f ->
            mapOf(
                "name" to f.name,
                "ord" to ord,
                "font" to f.font,
                "media" to f.media,
                "rtl" to f.rtl,
                "size" to f.size,
                "sticky" to f.sticky
            )
        }

        return mapOf(
            "css" to css,
            "did" to deckId,
            "flds" to jsonFields,
            "id" to modelId.toString(),
            "latexPost" to latexPost,
            "latexPre" to latexPre,
            "latexsvg" to false,
            "mod" to timestampSeconds,
            "name" to name,
            "req" to req,
            "sortf" to sortFieldIndex,
            "tags" to emptyList<String>(),
            "tmpls" to jsonTemplates,
            "type" to modelType,
            "usn" to -1,
            "vers" to emptyList<String>()
        )
    }

    private fun computeReq(): List<List<Any>> {
        val sentinel = "SeNtInEl"
        val fieldNames = fields.map { it.name }
        val out = mutableListOf<List<Any>>()

        templates.forEachIndexed { tmplOrd, tmpl ->
            val requiredAll = mutableListOf<Int>()
            fieldNames.forEachIndexed { fieldOrd, fieldName ->
                val values = fieldNames.associateWith { sentinel }.toMutableMap()
                values[fieldName] = ""
                val rendered = renderMustache(tmpl.qfmt, values)
                if (!rendered.contains(sentinel)) {
                    requiredAll += fieldOrd
                }
            }

            if (requiredAll.isNotEmpty()) {
                out += listOf(tmplOrd, "all", requiredAll)
            } else {
                val requiredAny = mutableListOf<Int>()
                fieldNames.forEachIndexed { fieldOrd, fieldName ->
                    val values = fieldNames.associateWith { "" }.toMutableMap()
                    values[fieldName] = sentinel
                    val rendered = renderMustache(tmpl.qfmt, values)
                    if (rendered.contains(sentinel)) {
                        requiredAny += fieldOrd
                    }
                }
                require(requiredAny.isNotEmpty()) { "Could not compute required fields for template qfmt=${tmpl.qfmt}" }
                out += listOf(tmplOrd, "any", requiredAny)
            }
        }

        return out
    }

    private fun renderMustache(template: String, values: Map<String, String>): String {
        var out = template

        val sectionRegex = Regex("\\{\\{([#^])\\s*([A-Za-z0-9_:-]+)\\s*}}(.*?)\\{\\{/\\s*\\2\\s*}}", setOf(RegexOption.DOT_MATCHES_ALL))
        while (true) {
            val m = sectionRegex.find(out) ?: break
            val sectionType = m.groupValues[1]
            val key = m.groupValues[2]
            val content = m.groupValues[3]
            val value = values[key].orEmpty()
            val replaceWith = when (sectionType) {
                "#" -> if (value.isNotEmpty()) content else ""
                "^" -> if (value.isEmpty()) content else ""
                else -> ""
            }
            out = out.replaceRange(m.range, replaceWith)
        }

        val varRegex = Regex("\\{\\{\\s*([A-Za-z0-9_:-]+)\\s*}}")
        out = varRegex.replace(out) { mr -> values[mr.groupValues[1]].orEmpty() }
        return out
    }
}
