package com.mangoloads.expmango

import java.util.Locale

object TypoEngine {

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

    private val neighborKeys = mapOf(
        'q' to listOf('w', 'a', 's'),
        'w' to listOf('q', 'e', 'a', 's', 'd'),
        'e' to listOf('w', 'r', 's', 'd', 'f'),
        'r' to listOf('e', 't', 'd', 'f', 'g'),
        't' to listOf('r', 'y', 'f', 'g', 'h'),
        'y' to listOf('t', 'u', 'g', 'h', 'j'),
        'u' to listOf('y', 'i', 'h', 'j', 'k'),
        'i' to listOf('u', 'o', 'j', 'k', 'l'),
        'o' to listOf('i', 'p', 'k', 'l'),
        'p' to listOf('o', 'l'),
        'a' to listOf('q', 'w', 's', 'z'),
        's' to listOf('a', 'w', 'e', 'd', 'z', 'x'),
        'd' to listOf('s', 'e', 'r', 'f', 'x', 'c'),
        'f' to listOf('d', 'r', 't', 'g', 'c', 'v'),
        'g' to listOf('f', 't', 'y', 'h', 'v', 'b'),
        'h' to listOf('g', 'y', 'u', 'j', 'b', 'n'),
        'j' to listOf('h', 'u', 'i', 'k', 'n', 'm'),
        'k' to listOf('j', 'i', 'o', 'l', 'm'),
        'l' to listOf('k', 'o', 'p'),
        'z' to listOf('a', 's', 'x'),
        'x' to listOf('z', 's', 'd', 'c'),
        'c' to listOf('x', 'd', 'f', 'v'),
        'v' to listOf('c', 'f', 'g', 'b'),
        'b' to listOf('v', 'g', 'h', 'n'),
        'n' to listOf('b', 'h', 'j', 'm'),
        'm' to listOf('n', 'j', 'k')
    )

    fun isNeighbor(c1: Char, c2: Char): Boolean {
        val l1 = c1.lowercaseChar()
        val l2 = c2.lowercaseChar()
        if (l1 == l2) return true
        return neighborKeys[l1]?.contains(l2) == true
    }

    /**
     * Calculates Damerau-Levenshtein edit distance considering neighbor keys.
     */
    fun editDistance(a: String, b: String): Double {
        val s1 = a.lowercase(Locale.ROOT)
        val s2 = b.lowercase(Locale.ROOT)
        val m = s1.length
        val n = s2.length

        if (s1 == s2) return 0.0
        if (kotlin.math.abs(m - n) > 3) return 10.0

        val d = Array(m + 1) { DoubleArray(n + 1) }

        for (i in 0..m) d[i][0] = i.toDouble()
        for (j in 0..n) d[0][j] = j.toDouble()

        for (i in 1..m) {
            for (j in 1..n) {
                val char1 = s1[i - 1]
                val char2 = s2[j - 1]

                val subCost = when {
                    char1 == char2 -> 0.0
                    isNeighbor(char1, char2) -> 0.6 // Lower penalty for key neighbors
                    else -> 1.0
                }

                var min = minOf(
                    d[i - 1][j] + 1.0,      // deletion
                    d[i][j - 1] + 1.0,      // insertion
                    d[i - 1][j - 1] + subCost // substitution
                )

                // Transposition
                if (i > 1 && j > 1 && s1[i - 1] == s2[j - 2] && s1[i - 2] == s2[j - 1]) {
                    min = minOf(min, d[i - 2][j - 2] + 0.8)
                }

                d[i][j] = min
            }
        }
        return d[m][n]
    }
}
