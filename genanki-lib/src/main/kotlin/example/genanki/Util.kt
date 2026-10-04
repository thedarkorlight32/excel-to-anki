package example.genanki

import java.security.MessageDigest

private val base91Table = listOf(
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
    't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
    'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4',
    '5', '6', '7', '8', '9', '!', '#', '$', '%', '&', '(', ')', '*', '+', ',', '-', '.', '/', ':',
    ';', '<', '=', '>', '?', '@', '[', ']', '^', '_', '`', '{', '|', '}', '~'
)

fun guidFor(vararg values: String): String {
    val hashStr = values.joinToString("__")
    val digest = MessageDigest.getInstance("SHA-256").digest(hashStr.toByteArray(Charsets.UTF_8))
    var hashInt = 0L
    for (b in digest.copyOfRange(0, 8)) {
        hashInt = (hashInt shl 8) + (b.toInt() and 0xFF)
    }
    if (hashInt == 0L) return ""

    val out = StringBuilder()
    val base = base91Table.size.toLong()
    while (hashInt > 0) {
        out.append(base91Table[(hashInt % base).toInt()])
        hashInt /= base
    }
    return out.reverse().toString()
}
