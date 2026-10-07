package com.mangoloads.expmango

import android.content.Context
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

enum class WordCategory(val displayName: String) {
    ENGLISH("English"),
    HINGLISH("Hinglish"),
    SLANG("Slang"),
    INDIAN_ENGLISH("Indian English"),
    UPSC("UPSC"),
    TECHNOLOGY("Technology"),
    NAMES("Names"),
    PERSONAL("Personal"),
    OTHER("Other")
}

data class CustomWord(
    val word: String,
    val category: WordCategory = WordCategory.PERSONAL,
    val frequency: Int = 100
)

class DictionaryManager(context: Context) {

    private val appContext = context.applicationContext
    private val filesDir: File = try {
        appContext.filesDir ?: File("build/test-tmp").apply { mkdirs() }
    } catch (_: Exception) {
        File("build/test-tmp").apply { mkdirs() }
    }

    private val customWordsFile = File(filesDir, "custom_dictionary.tsv")
    private val words = ConcurrentHashMap<String, CustomWord>()

    init {
        loadCustomWords()
    }

    fun addWord(word: String, category: WordCategory = WordCategory.PERSONAL, frequency: Int = 100) {
        val clean = word.trim().lowercase(Locale.ROOT)
        if (clean.isNotBlank() && clean.none { it.isWhitespace() }) {
            words[clean] = CustomWord(clean, category, frequency)
            saveCustomWords()
        }
    }

    fun addBulkWords(lines: List<String>, category: WordCategory = WordCategory.PERSONAL) {
        lines.forEach { line ->
            val clean = line.trim().lowercase(Locale.ROOT)
            if (clean.isNotBlank() && !clean.startsWith("#") && clean.none { it.isWhitespace() }) {
                words[clean] = CustomWord(clean, category, 100)
            }
        }
        saveCustomWords()
    }

    fun removeWord(word: String) {
        val clean = word.trim().lowercase(Locale.ROOT)
        if (words.remove(clean) != null) {
            saveCustomWords()
        }
    }

    fun getWords(category: WordCategory? = null): List<CustomWord> {
        val list = words.values.toList()
        return if (category == null) list else list.filter { it.category == category }
    }

    fun search(query: String): List<CustomWord> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isBlank()) return getWords()
        return words.values.filter { it.word.contains(q) }
    }

    fun reset() {
        words.clear()
        if (customWordsFile.exists()) {
            customWordsFile.delete()
        }
    }

    fun getWordCount(): Int = words.size

    fun getCategoryCounts(): Map<WordCategory, Int> {
        val counts = WordCategory.values().associateWith { 0 }.toMutableMap()
        words.values.forEach { cw ->
            counts[cw.category] = (counts[cw.category] ?: 0) + 1
        }
        return counts
    }

    private fun loadCustomWords() {
        if (!customWordsFile.exists()) return
        try {
            customWordsFile.forEachLine { line ->
                val parts = line.split('\t')
                if (parts.isNotEmpty()) {
                    val w = parts[0].trim().lowercase(Locale.ROOT)
                    val cat = parts.getOrNull(1)?.let {
                        try { WordCategory.valueOf(it) } catch (_: Exception) { WordCategory.PERSONAL }
                    } ?: WordCategory.PERSONAL
                    val freq = parts.getOrNull(2)?.toIntOrNull() ?: 100
                    if (w.isNotBlank()) {
                        words[w] = CustomWord(w, cat, freq)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun saveCustomWords() {
        try {
            customWordsFile.printWriter().use { out ->
                words.values.sortedBy { it.word }.forEach { cw ->
                    out.println("${cw.word}\t${cw.category.name}\t${cw.frequency}")
                }
            }
        } catch (_: Exception) {
        }
    }
}
