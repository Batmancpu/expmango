package com.mangoloads.expmango

import android.content.Context
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

data class LearnedWordStats(
    val word: String,
    @Volatile var frequency: Int = 1,
    @Volatile var lastUsedTime: Long = System.currentTimeMillis()
)

class LearningEngine(context: Context) {

    private val appContext = context.applicationContext
    private val filesDir: File = try {
        appContext.filesDir ?: File("build/test-tmp").apply { mkdirs() }
    } catch (_: Exception) {
        File("build/test-tmp").apply { mkdirs() }
    }

    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    private val userWords = ConcurrentHashMap<String, LearnedWordStats>()
    private val bigrams = ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>()
    private val trigrams = ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>>()

    private val userWordsFile = File(filesDir, "mango_user_words.tsv")
    private val bigramFile = File(filesDir, "mango_bigrams.tsv")
    private val trigramFile = File(filesDir, "mango_trigrams.tsv")

    private var saveFuture: ScheduledFuture<*>? = null

    @Volatile
    private var dirty = false

    init {
        executor.execute { loadUserData() }
    }

    fun learnWord(word: String, policy: FieldPolicy) {
        if (!policy.canLearn || policy.isPrivate) return
        val clean = word.trim().lowercase(Locale.ROOT)
        if (clean.length < 2 || clean.any { it.isWhitespace() } || clean.any { it.isDigit() }) return

        val stats = userWords.computeIfAbsent(clean) { LearnedWordStats(clean) }
        stats.frequency++
        stats.lastUsedTime = System.currentTimeMillis()

        dirty = true
        scheduleSave()
    }

    fun learnBigram(prev: String, current: String, policy: FieldPolicy) {
        if (!policy.canLearn || policy.isPrivate) return
        val w1 = prev.trim().lowercase(Locale.ROOT)
        val w2 = current.trim().lowercase(Locale.ROOT)
        if (w1.isBlank() || w2.isBlank() || w1.length < 2 || w2.length < 2) return

        val map = bigrams.computeIfAbsent(w1) { ConcurrentHashMap() }
        map.compute(w2) { _, count -> (count ?: 0) + 1 }

        dirty = true
        scheduleSave()
    }

    fun learnTrigram(prev2: String, prev1: String, current: String, policy: FieldPolicy) {
        if (!policy.canLearn || policy.isPrivate) return
        val w1 = prev2.trim().lowercase(Locale.ROOT)
        val w2 = prev1.trim().lowercase(Locale.ROOT)
        val w3 = current.trim().lowercase(Locale.ROOT)
        if (w1.isBlank() || w2.isBlank() || w3.isBlank()) return

        val outerMap = trigrams.computeIfAbsent(w1) { ConcurrentHashMap() }
        val innerMap = outerMap.computeIfAbsent(w2) { ConcurrentHashMap() }
        innerMap.compute(w3) { _, count -> (count ?: 0) + 1 }

        dirty = true
        scheduleSave()
    }

    fun getBigramNext(prev: String, limit: Int = 3): List<Pair<String, Int>> {
        val key = prev.trim().lowercase(Locale.ROOT)
        val map = bigrams[key] ?: return emptyList()
        return map.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }

    fun getTrigramNext(prev2: String, prev1: String, limit: Int = 3): List<Pair<String, Int>> {
        val w1 = prev2.trim().lowercase(Locale.ROOT)
        val w2 = prev1.trim().lowercase(Locale.ROOT)
        val map = trigrams[w1]?.get(w2) ?: return emptyList()
        return map.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }

    fun containsWord(word: String): Boolean =
        userWords.containsKey(word.lowercase(Locale.ROOT))

    fun getWordFrequency(word: String): Int =
        userWords[word.lowercase(Locale.ROOT)]?.frequency ?: 0

    fun resetLearnedData() {
        executor.execute {
            userWords.clear()
            bigrams.clear()
            trigrams.clear()
            userWordsFile.delete()
            bigramFile.delete()
            trigramFile.delete()
            dirty = false
        }
    }

    fun shutdown() {
        saveNow()
        executor.shutdown()
    }

    private fun scheduleSave() {
        saveFuture?.cancel(false)
        saveFuture = executor.schedule({ saveNow() }, 1000, TimeUnit.MILLISECONDS)
    }

    @Synchronized
    private fun saveNow() {
        if (!dirty) return
        try {
            userWordsFile.printWriter().use { out ->
                userWords.values.sortedByDescending { it.frequency }.take(10000).forEach { stats ->
                    out.println("${stats.word}\t${stats.frequency}\t${stats.lastUsedTime}")
                }
            }

            bigramFile.printWriter().use { out ->
                bigrams.forEach { (prev, map) ->
                    map.forEach { (next, count) ->
                        out.println("$prev\t$next\t$count")
                    }
                }
            }

            trigramFile.printWriter().use { out ->
                trigrams.forEach { (p1, map1) ->
                    map1.forEach { (p2, map2) ->
                        map2.forEach { (p3, count) ->
                            out.println("$p1\t$p2\t$p3\t$count")
                        }
                    }
                }
            }

            dirty = false
        } catch (_: Exception) {
        }
    }

    private fun loadUserData() {
        try {
            if (userWordsFile.exists()) {
                userWordsFile.forEachLine { line ->
                    val parts = line.split('\t')
                    if (parts.size >= 2) {
                        val word = parts[0]
                        val freq = parts[1].toIntOrNull() ?: 1
                        val time = parts.getOrNull(2)?.toLongOrNull() ?: System.currentTimeMillis()
                        userWords[word] = LearnedWordStats(word, freq, time)
                    }
                }
            }

            if (bigramFile.exists()) {
                bigramFile.forEachLine { line ->
                    val parts = line.split('\t')
                    if (parts.size >= 3) {
                        val map = bigrams.computeIfAbsent(parts[0]) { ConcurrentHashMap() }
                        map[parts[1]] = parts[2].toIntOrNull() ?: 1
                    }
                }
            }

            if (trigramFile.exists()) {
                trigramFile.forEachLine { line ->
                    val parts = line.split('\t')
                    if (parts.size >= 4) {
                        val map1 = trigrams.computeIfAbsent(parts[0]) { ConcurrentHashMap() }
                        val map2 = map1.computeIfAbsent(parts[1]) { ConcurrentHashMap() }
                        map2[parts[2]] = parts[3].toIntOrNull() ?: 1
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
}
