package com.mangoloads.expmango

import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

data class WordStats(
    val word: String,
    @Volatile var frequency: Int = 1
)

class PrefixTrie {
    private class Node {
        val children = HashMap<Char, Node>()
        val words = ArrayList<String>()
    }

    private val root = Node()

    @Synchronized
    fun add(word: String) {
        var node = root
        for (c in word) {
            node = node.children.getOrPut(c) { Node() }
            if (node.words.size < 32 && !node.words.contains(word)) node.words.add(word)
        }
    }

    @Synchronized
    fun prefix(prefix: String, limit: Int): List<String> {
        var node = root
        for (c in prefix) {
            node = node.children[c] ?: return emptyList()
        }
        return node.words.take(limit)
    }
}

class Lexicon {
    private val words = ConcurrentHashMap<String, WordStats>()
    private val buckets = ConcurrentHashMap<String, MutableList<String>>()
    private val trie = PrefixTrie()

    fun add(word: String, frequency: Int = 1) {
        val w = word.trim().lowercase(Locale.ROOT)
        if (w.length < 2 || w.any { it.isWhitespace() }) return
        val existing = words[w]
        if (existing != null) {
            existing.frequency += frequency.coerceAtLeast(1)
            return
        }

        words[w] = WordStats(w, frequency.coerceAtLeast(1))
        buckets.computeIfAbsent(bucketKey(w)) {
            Collections.synchronizedList(ArrayList())
        }.add(w)
        trie.add(w)
    }

    fun contains(word: String): Boolean =
        words.containsKey(word.lowercase(Locale.ROOT))

    fun frequency(word: String): Int =
        words[word.lowercase(Locale.ROOT)]?.frequency ?: 0

    fun prefix(prefix: String, limit: Int): List<String> =
        trie.prefix(prefix.lowercase(Locale.ROOT), limit)

    fun fuzzyCandidates(word: String): List<String> {
        val w = word.lowercase(Locale.ROOT)
        val first = w.firstOrNull() ?: return emptyList()
        val result = ArrayList<String>(48)

        for (length in maxOf(2, w.length - 2)..w.length + 2) {
            val bucket = buckets[bucketKey(first, length)] ?: continue
            synchronized(bucket) {
                for (candidate in bucket) {
                    if (abs(candidate.length - w.length) > 2) continue
                    if (boundedDistance(w, candidate, 2) <= 2) {
                        result.add(candidate)
                        if (result.size >= 48) return result
                    }
                }
            }
        }
        return result
    }

    fun allWords(): Set<String> = words.keys

    private fun bucketKey(word: String): String =
        bucketKey(word.firstOrNull() ?: '_', word.length)

    private fun bucketKey(first: Char, length: Int): String =
        first.toString() + ":" + length

    private fun boundedDistance(a: String, b: String, max: Int): Int {
        if (abs(a.length - b.length) > max) return max + 1
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)

        for (i in a.indices) {
            cur[0] = i + 1
            var rowMin = cur[0]

            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                cur[j + 1] = minOf(
                    cur[j] + 1,
                    prev[j + 1] + 1,
                    prev[j] + cost
                )
                rowMin = minOf(rowMin, cur[j + 1])
            }

            if (rowMin > max) return max + 1
            val tmp = prev
            prev = cur
            cur = tmp
        }

        return prev[b.length]
    }
}
