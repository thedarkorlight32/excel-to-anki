package example.exceltoanki

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URLEncoder

/**
 * Single interface for converting text to speech. Implementations may split text,
 * perform retries, rate-limiting, or call external SDKs.
 */
interface TextToSpeech {
    fun synthesize(text: String, lang: String): ByteArray
}

/**
 * Google Translate TTS implementation.
 */
class GoogleTranslateTtsFetcher : TextToSpeech {
    override fun synthesize(text: String, lang: String): ByteArray {
        val chunks = splitToChunks(text, 200)
        val out = ByteArrayOutputStream()
        for ((idx, chunk) in chunks.withIndex()) {
            val bytes = fetchChunk(chunk, lang)
            out.write(bytes)
            println("  fetched chunk ${idx + 1}/${chunks.size} (${chunk.length} chars)")
        }
        return out.toByteArray()
    }

    private fun splitToChunks(text: String, maxLen: Int): List<String> {
        if (text.length <= maxLen) return listOf(text)
        val words = text.split(Regex("\\s+"))
        val chunks = mutableListOf<String>()
        var current = StringBuilder()
        for (w in words) {
            if (current.isEmpty()) {
                current.append(w)
            } else if (current.length + 1 + w.length <= maxLen) {
                current.append(' ').append(w)
            } else {
                chunks.add(current.toString())
                current = StringBuilder(w)
            }
        }
        if (current.isNotEmpty()) chunks.add(current.toString())
        return chunks
    }

    private fun fetchChunk(textChunk: String, lang: String): ByteArray {
        val base = "https://translate.google.com/translate_tts"
        val q = URLEncoder.encode(textChunk, "UTF-8")
        val url = "$base?ie=UTF-8&q=$q&tl=${URLEncoder.encode(lang, "UTF-8")}&client=tw-ob"
        val conn = java.net.URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0 Safari/537.36")
        conn.setRequestProperty("Referer", "https://translate.google.com/")
        conn.connectTimeout = 20000
        conn.readTimeout = 20000

        val responseCode = conn.responseCode
        if (responseCode != 200) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "(no body)"
            throw RuntimeException("TTS request failed (HTTP $responseCode): $err")
        }

        conn.inputStream.use { ins ->
            val baos = ByteArrayOutputStream()
            val buf = ByteArray(8 * 1024)
            var read: Int
            while (ins.read(buf).also { read = it } != -1) {
                baos.write(buf, 0, read)
            }
            return baos.toByteArray()
        }
    }
}
