package com.mangoloads.expmango

import android.content.Context
import java.io.File
import java.util.Locale

data class Candidate(
    val word: String,
    val score: Double,
    val type: String = "prediction"
)

class PredictionEngine(context: Context) {

    private val appContext = context.applicationContext
    private val lexicon = Lexicon()
    val emailDomainProvider = EmailDomainProvider()
    val dictionaryManager = DictionaryManager(appContext)
    val learningEngine = LearningEngine(appContext)

    init {
        loadAssets()
        loadCustomDictionary()
    }

    fun suggestions(
        input: String,
        previous1: String? = null,
        previous2: String? = null,
        policy: FieldPolicy = FieldPolicyResolver.resolve(FieldMode.NORMAL),
        limit: Int = 3
    ): List<Candidate> {
        if (policy.isPrivate && (policy.mode == FieldMode.PASSWORD || policy.mode == FieldMode.PIN || policy.mode == FieldMode.OTP)) {
            return emptyList()
        }

        if (policy.showDomainChips && input.contains('@')) {
            val domains = emailDomainProvider.getDomainSuggestions(input, limit)
            return domains.map { Candidate(it, 100.0, "email-domain") }
        }

        val token = input.trim().lowercase(Locale.ROOT)

        if (token.isBlank()) {
            return getContextNextWords(previous1, previous2, limit)
        }

        val candidates = mutableMapOf<String, Candidate>()

        val prefixMatches = lexicon.prefix(token, 24)
        prefixMatches.forEach { word ->
            val score = scoreCandidate(word, token, previous1, previous2, policy)
            candidates[word] = Candidate(word, score, "prefix")
        }

        if (policy.canAutocorrect) {
            TypoEngine.exactTypos[token]?.let { fixed ->
                val score = 100.0
                candidates[fixed] = Candidate(fixed, score, "autocorrect")
            }
        }

        if (candidates.size < limit && policy.canAutocorrect && token.length >= 3 && !lexicon.contains(token)) {
            val fuzzy = lexicon.fuzzyCandidates(token)
            fuzzy.forEach { word ->
                if (!candidates.containsKey(word)) {
                    val score = scoreCandidate(word, token, previous1, previous2, policy)
                    candidates[word] = Candidate(word, score, "typo")
                }
            }
        }

        return candidates.values
            .sortedByDescending { it.score }
            .take(limit)
    }

    fun correct(
        typed: String,
        previous1: String? = null,
        previous2: String? = null,
        policy: FieldPolicy = FieldPolicyResolver.resolve(FieldMode.NORMAL)
    ): Candidate? {
        val original = typed.trim()
        if (original.isBlank()) return null

        if (!policy.canAutocorrect || policy.mode in setOf(
                FieldMode.URI,
                FieldMode.EMAIL,
                FieldMode.CODE,
                FieldMode.TERMINAL,
                FieldMode.PASSWORD,
                FieldMode.PIN,
                FieldMode.OTP
            )
        ) {
            return null
        }

        val lower = original.lowercase(Locale.ROOT)

        if (lexicon.contains(lower) || learningEngine.containsWord(lower)) {
            return null
        }

        TypoEngine.exactTypos[lower]?.let { fixed ->
            return Candidate(fixed, 100.0, "exact-typo")
        }

        if (lower.length < 3) return null

        val candidates = lexicon.fuzzyCandidates(lower)
        var bestCandidate: String? = null
        var minDistance = 3.0

        for (cand in candidates) {
            val dist = TypoEngine.editDistance(lower, cand)
            if (dist < minDistance) {
                minDistance = dist
                bestCandidate = cand
            }
        }

        return bestCandidate?.let {
            Candidate(it, 80.0 - minDistance * 10, "fuzzy-autocorrect")
        }
    }

    fun glide(
        raw: String,
        previous1: String? = null,
        policy: FieldPolicy = FieldPolicyResolver.resolve(FieldMode.NORMAL)
    ): Candidate? {
        val seq = raw.lowercase(Locale.ROOT)
        if (seq.length < 3) return null

        return lexicon.fuzzyCandidates(seq)
            .map { word ->
                val dist = TypoEngine.editDistance(seq, word)
                val freq = lexicon.frequency(word)
                Candidate(word, 85.0 - dist * 10.0 + minOf(freq, 25), "glide")
            }
            .maxByOrNull { it.score }
            ?.takeIf { it.score >= 48.0 }
    }

    fun learn(
        word: String,
        previous1: String? = null,
        previous2: String? = null,
        policy: FieldPolicy = FieldPolicyResolver.resolve(FieldMode.NORMAL)
    ) {
        if (!policy.canLearn || policy.isPrivate) return
        val clean = word.trim().lowercase(Locale.ROOT)
        if (clean.isBlank()) return

        lexicon.add(clean)
        learningEngine.learnWord(clean, policy)

        val p1 = previous1?.trim()?.lowercase(Locale.ROOT)
        val p2 = previous2?.trim()?.lowercase(Locale.ROOT)

        if (!p1.isNullOrEmpty()) {
            learningEngine.learnBigram(p1, clean, policy)
        }
        if (!p1.isNullOrEmpty() && !p2.isNullOrEmpty()) {
            learningEngine.learnTrigram(p2, p1, clean, policy)
        }
    }

    fun shutdown() {
        learningEngine.shutdown()
    }

    private fun getContextNextWords(
        previous1: String?,
        previous2: String?,
        limit: Int
    ): List<Candidate> {
        val candidates = mutableListOf<Candidate>()

        val p1 = previous1?.trim()?.lowercase(Locale.ROOT)
        val p2 = previous2?.trim()?.lowercase(Locale.ROOT)

        if (!p1.isNullOrEmpty() && !p2.isNullOrEmpty()) {
            val triResults = learningEngine.getTrigramNext(p2, p1, limit)
            triResults.forEach { (word, count) ->
                candidates.add(Candidate(word, 90.0 + count, "trigram"))
            }
        }

        if (candidates.size < limit && !p1.isNullOrEmpty()) {
            val biResults = learningEngine.getBigramNext(p1, limit - candidates.size)
            biResults.forEach { (word, count) ->
                if (candidates.none { it.word == word }) {
                    candidates.add(Candidate(word, 70.0 + count, "bigram"))
                }
            }
        }

        if (candidates.size < limit && !p1.isNullOrEmpty()) {
            val fallbacks = when (p1) {
                "going" -> listOf("to", "home", "back")
                "not" -> listOf("sure", "yet", "ready")
                "is" -> listOf("the", "a", "not")
                "rights" -> listOf("and", "in", "for")
                "fundamental" -> listOf("rights")
                "polity" -> listOf("upsc", "and")
                "open" -> listOf("ai")
                "climate" -> listOf("change")
                "monetary" -> listOf("policy")
                "repo" -> listOf("rate")
                else -> emptyList()
            }
            fallbacks.forEach { fb ->
                if (candidates.none { it.word == fb }) {
                    candidates.add(Candidate(fb, 50.0, "context-fallback"))
                }
            }
        }

        return candidates.take(limit)
    }

    private fun scoreCandidate(
        word: String,
        prefix: String,
        previous1: String?,
        previous2: String?,
        policy: FieldPolicy
    ): Double {
        var score = 50.0

        val lenDiff = word.length - prefix.length
        score -= lenDiff * 2.0

        val globalFreq = lexicon.frequency(word)
        val personalFreq = learningEngine.getWordFrequency(word)
        score += minOf(globalFreq, 30) * 0.5
        score += minOf(personalFreq, 50) * 1.0

        val p1 = previous1?.trim()?.lowercase(Locale.ROOT)
        if (!p1.isNullOrEmpty()) {
            val biMap = learningEngine.getBigramNext(p1, 10).toMap()
            val biCount = biMap[word] ?: 0
            score += minOf(biCount * 5, 25)
        }

        return score
    }

    private fun loadAssets() {
        loadAssetFile("dictionary.txt")
        loadAssetFile("hinglish.txt")
    }

    private fun loadAssetFile(filename: String) {
        try {
            appContext.assets.open(filename).bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim().lowercase(Locale.ROOT)
                    if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                        val parts = trimmed.split(Regex("\\s+"))
                        val word = parts[0]
                        val freq = parts.getOrNull(1)?.toIntOrNull() ?: 1
                        lexicon.add(word, freq)
                    }
                }
            }
        } catch (_: Exception) {
            val file = File("app/src/main/assets/$filename").takeIf { it.exists() }
                ?: File("src/main/assets/$filename").takeIf { it.exists() }
            file?.forEachLine { line ->
                val trimmed = line.trim().lowercase(Locale.ROOT)
                if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                    val parts = trimmed.split(Regex("\\s+"))
                    val word = parts[0]
                    val freq = parts.getOrNull(1)?.toIntOrNull() ?: 1
                    lexicon.add(word, freq)
                }
            }
        }
    }

    private fun loadCustomDictionary() {
        val custom = dictionaryManager.getWords()
        custom.forEach { cw ->
            lexicon.add(cw.word, cw.frequency)
        }
    }
}
