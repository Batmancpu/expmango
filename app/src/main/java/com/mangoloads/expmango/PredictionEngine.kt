package com.mangoloads.expmango

import android.content.Context
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class Prediction(
    val word: String,
    val score: Int,
    val reason: String
)

class PredictionEngine(context: Context) {
    private val appContext = context.applicationContext
    private val lexicon = Lexicon()
    private val bigrams =
        ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>()

    private val executor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor()

    private var saveFuture: ScheduledFuture<*>? = null

    @Volatile
    private var dirty = false

    private val userFile = File(appContext.filesDir, "mango_words.tsv")
    private val bigramFile = File(appContext.filesDir, "mango_bigrams.tsv")

    init {
        loadAsset("dictionary.txt")
        loadAsset("hinglish.txt")
        loadUserData()
    }

    fun suggestions(prefix: String, previous: String?, limit: Int = 3): List<Prediction> {
        val p = prefix.lowercase(Locale.ROOT)
        if (p.isBlank()) return nextWord(previous, limit)

        return lexicon.prefix(p, 24)
            .map { word ->
                var score = 100 - (word.length - p.length) * 3
                score += minOf(lexicon.frequency(word), 30)
                val context = previous?.let {
                    bigrams[it.lowercase(Locale.ROOT)]?.get(word) ?: 0
                } ?: 0
                score += minOf(context * 5, 25)
                Prediction(word, score, "prefix")
            }
            .sortedByDescending { it.score }
            .take(limit)
    }

    fun nextWord(previous: String?, limit: Int = 3): List<Prediction> {
        val key = previous?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (key.isBlank()) return emptyList()

        return (bigrams[key] ?: return emptyList())
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { Prediction(it.key, it.value, "learned phrase") }
    }

    fun correct(word: String, previous: String?): Prediction? {
        val original = word.trim()
        if (original.length < 3) return null
        if (original.any { !it.isLetter() }) return null
        if (original.all { it.isUpperCase() }) return null
        if (lexicon.contains(original)) return null

        val lower = original.lowercase(Locale.ROOT)

        val exactTypos = mapOf(
            "teh" to "the",
            "hte" to "the",
            "adn" to "and",
            "nad" to "and",
            "taht" to "that",
            "thier" to "their",
            "wierd" to "weird",
            "becuase" to "because",
            "beacuse" to "because",
            "recieve" to "receive",
            "seperate" to "separate",
            "definately" to "definitely",
            "occured" to "occurred",
            "goign" to "going",
            "woudl" to "would",
            "dont" to "don't",
            "cant" to "can't",
            "wont" to "won't",
            "im" to "I'm",
            "ive" to "I've"
        )

        exactTypos[lower]?.let {
            return Prediction(it, 100, "known typo")
        }

        val candidates = lexicon.fuzzyCandidates(lower)
        if (candidates.isEmpty()) return null

        val best = candidates.maxByOrNull { candidate ->
            val d = distance(lower, candidate)
            val freq = lexicon.frequency(candidate)
            val context = previous?.let {
                bigrams[it.lowercase(Locale.ROOT)]?.get(candidate) ?: 0
            } ?: 0
            40 - d * 12 + minOf(freq, 40) + minOf(context * 4, 24)
        } ?: return null

        val d = distance(lower, best)
        val contextBoost = previous?.let {
            (bigrams[it.lowercase(Locale.ROOT)]?.get(best) ?: 0) * 4
        } ?: 0
        val score = 70 + contextBoost + minOf(lexicon.frequency(best), 20) - d * 12

        return if (score >= 58) Prediction(best, score, "fast fuzzy") else null
    }

    fun glide(raw: String, previous: String?): Prediction? {
        val seq = raw.lowercase(Locale.ROOT)
        if (seq.length < 3) return null

        return lexicon.fuzzyCandidates(seq)
            .map { word ->
                val d = distance(seq, word)
                val freq = lexicon.frequency(word)
                val context = previous?.let {
                    bigrams[it.lowercase(Locale.ROOT)]?.get(word) ?: 0
                } ?: 0
                Prediction(
                    word,
                    85 - d * 10 + minOf(freq, 25) + minOf(context * 5, 20),
                    "glide"
                )
            }
            .maxByOrNull { it.score }
            ?.takeIf { it.score >= 48 }
    }

    fun learn(word: String, previous: String?) {
        val clean = word.trim().lowercase(Locale.ROOT)
        if (clean.length < 2 || clean.any { it.isWhitespace() }) return

        lexicon.add(clean)

        previous?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }?.let { prev ->
            val map = bigrams.computeIfAbsent(prev) {
                ConcurrentHashMap()
            }
            map.compute(clean) { _, value -> (value ?: 0) + 1 }
        }

        dirty = true
        scheduleSave()
    }

    fun shutdown() {
        saveNow()
        executor.shutdownNow()
    }

    private fun scheduleSave() {
        saveFuture?.cancel(false)
        saveFuture = executor.schedule({
            saveNow()
        }, 900, TimeUnit.MILLISECONDS)
    }

    private fun saveNow() {
        if (!dirty) return

        try {
            userFile.printWriter().use { out ->
                for (word in lexicon.allWords().sorted()) {
                    out.println(word + "\t" + lexicon.frequency(word))
                }
            }

            bigramFile.printWriter().use { out ->
                for ((prev, map) in bigrams) {
                    for ((next, count) in map) {
                        out.println(prev + "\t" + next + "\t" + count)
                    }
                }
            }

            dirty = false
        } catch (_: Exception) {
            // Never let persistence affect typing.
        }
    }

    private fun loadAsset(name: String) {
        try {
            appContext.assets.open(name).bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val word = line.trim().lowercase(Locale.ROOT)
                    if (word.isNotEmpty() && !word.startsWith("#")) lexicon.add(word)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun loadUserData() {
        try {
            if (userFile.exists()) {
                userFile.forEachLine { line ->
                    val parts = line.split('\t')
                    if (parts.size >= 2) {
                        lexicon.add(parts[0], parts[1].toIntOrNull() ?: 1)
                    }
                }
            }

            if (bigramFile.exists()) {
                bigramFile.forEachLine { line ->
                    val parts = line.split('\t')
                    if (parts.size >= 3) {
                        val map = bigrams.computeIfAbsent(parts[0]) {
                            ConcurrentHashMap()
                        }
                        map[parts[1]] = parts[2].toIntOrNull() ?: 1
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun distance(a: String, b: String): Int {
        if (a == b) return 0
        if (abs(a.length - b.length) > 3) return 4

        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)

        for (i in a.indices) {
            cur[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                cur[j + 1] = minOf(
                    cur[j] + 1,
                    prev[j + 1] + 1,
                    prev[j] + cost
                )
            }
            val tmp = prev
            prev = cur
            cur = tmp
        }
        return prev[b.length]
    }
}
