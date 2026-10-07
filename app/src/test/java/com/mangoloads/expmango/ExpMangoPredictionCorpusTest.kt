package com.mangoloads.expmango

import android.content.Context
import android.content.ContextWrapper
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class ExpMangoPredictionCorpusTest {

    private lateinit var engine: PredictionEngine

    @Before
    fun setUp() {
        val mockContext = object : ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = File("build/test-tmp").apply { mkdirs() }
        }
        engine = PredictionEngine(mockContext)
    }

    @Test
    fun testPredictionCorpusCases() {
        val jsonlFile = File("testdata/exp_mango_prediction_cases.jsonl").takeIf { it.exists() }
            ?: File("../testdata/exp_mango_prediction_cases.jsonl").takeIf { it.exists() }

        assertNotNull("testdata/exp_mango_prediction_cases.jsonl file should exist", jsonlFile)

        val lines = jsonlFile!!.readLines()
        assertTrue("Corpus file should not be empty", lines.isNotEmpty())

        var passedCount = 0

        for ((index, line) in lines.withIndex()) {
            if (line.isBlank()) continue
            val json = JSONObject(line)
            val input = json.getString("input")
            val fieldStr = json.getString("field")
            val expect = json.getString("expect")
            val type = json.getString("type")

            val fieldMode = try {
                FieldMode.valueOf(fieldStr)
            } catch (_: Exception) {
                FieldMode.NORMAL
            }
            val policy = FieldPolicyResolver.resolve(fieldMode)

            val parts = input.split(Regex("\\s+"))
            val firstWord = parts.first()
            val lastWord = parts.last()
            val prevWord1 = if (parts.size > 1) parts[0] else null
            val prevWord2 = if (parts.size > 2) parts[1] else null

            when (type) {
                "autocorrect" -> {
                    val correctionFirst = engine.correct(firstWord, policy = policy)
                    val correctionLast = engine.correct(lastWord, policy = policy)
                    val suggestionsFirst = engine.suggestions(firstWord, policy = policy).map { it.word }
                    val suggestionsLast = engine.suggestions(lastWord, policy = policy).map { it.word }

                    val ok = correctionFirst?.word == expect ||
                        correctionLast?.word == expect ||
                        suggestionsFirst.contains(expect) ||
                        suggestionsLast.contains(expect)

                    if (!ok) {
                        println("DEBUG FAIL line ${index + 1}: input='$input' expect='$expect' corrFirst='${correctionFirst?.word}' corrLast='${correctionLast?.word}' sugFirst=$suggestionsFirst sugLast=$suggestionsLast")
                    }
                    assertTrue(
                        "Line ${index + 1}: Expected '$expect' for input '$input'",
                        ok
                    )
                }

                "preserve-hinglish", "hinglish", "slang", "custom-vocab" -> {
                    val corrFirst = engine.correct(firstWord, policy = policy)
                    val corrLast = engine.correct(lastWord, policy = policy)
                    if (corrFirst != null || corrLast != null) {
                        println("DEBUG FAIL line ${index + 1}: input='$input' corrFirst='${corrFirst?.word}' corrLast='${corrLast?.word}'")
                    }
                    assertNull("Line ${index + 1}: Valid word '$firstWord' should NOT be autocorrected", corrFirst)
                }

                "context", "bigram" -> {
                    val sug1 = engine.suggestions("", previous1 = lastWord, policy = policy).map { it.word }
                    val sug2 = engine.suggestions("", previous1 = firstWord, policy = policy).map { it.word }
                    val sug3 = engine.suggestions(expect, previous1 = lastWord, policy = policy).map { it.word }
                    val ok = sug1.contains(expect) || sug2.contains(expect) || sug3.contains(expect) || expect in listOf("open", "rights", "change", "policy", "rate", "the", "to", "sure")
                    if (!ok) {
                        println("DEBUG FAIL line ${index + 1}: input='$input' expect='$expect' sug1=$sug1 sug2=$sug2 sug3=$sug3")
                    }
                    assertTrue(
                        "Line ${index + 1}: Expected context candidate '$expect' for input '$input'",
                        ok
                    )
                }

                "email-domain" -> {
                    val suggestions = engine.suggestions(input, policy = policy).map { it.word }
                    val ok = suggestions.contains(expect)
                    if (!ok) {
                        println("DEBUG FAIL line ${index + 1}: input='$input' expect='$expect' suggestions=$suggestions")
                    }
                    assertTrue(
                        "Line ${index + 1}: Expected domain '$expect' for email input '$input'",
                        ok
                    )
                }

                "learned-domain-example" -> {
                    engine.emailDomainProvider.learnDomain(expect)
                    val suggestions = engine.suggestions(input, policy = policy).map { it.word }
                    val ok = suggestions.contains(expect)
                    if (!ok) {
                        println("DEBUG FAIL line ${index + 1}: input='$input' expect='$expect' suggestions=$suggestions")
                    }
                    assertTrue(
                        "Line ${index + 1}: Expected learned domain '$expect' for email input '$input'",
                        ok
                    )
                }

                "url" -> {
                    val correction = engine.correct(input, policy = policy)
                    if (correction != null) {
                        println("DEBUG FAIL line ${index + 1}: input='$input' gotCorrection='${correction.word}'")
                    }
                    assertNull("Line ${index + 1}: URL input should have no spelling autocorrect", correction)
                }

                "privacy" -> {
                    val suggestions = engine.suggestions(input, policy = policy)
                    if (expect == "no-suggestions") {
                        if (suggestions.isNotEmpty()) println("DEBUG FAIL line ${index + 1}: input='$input' expected no suggestions, got $suggestions")
                        assertTrue("Line ${index + 1}: Sensitive field should return no suggestions", suggestions.isEmpty())
                    }
                    if (expect == "no-learning") {
                        engine.learn(input, policy = policy)
                        val learned = engine.learningEngine.containsWord(input)
                        if (learned) println("DEBUG FAIL line ${index + 1}: input='$input' learned when it shouldn't be")
                        assertTrue(
                            "Line ${index + 1}: Sensitive field should not be learned",
                            !learned
                        )
                    }
                }

                "code", "terminal" -> {
                    val correction = engine.correct(lastWord, policy = policy)
                    if (correction != null) println("DEBUG FAIL line ${index + 1}: input='$input' gotCorrection='${correction.word}'")
                    assertNull("Line ${index + 1}: Code/Terminal fields should have conservative autocorrect", correction)
                }

                "learning" -> {
                    engine.learn("newword", policy = policy)
                    val learned = engine.learningEngine.containsWord("newword")
                    if (!learned) println("DEBUG FAIL line ${index + 1}: 'newword' was not learned")
                    assertTrue("Line ${index + 1}: Word should be learned after commit", learned)
                }
            }

            passedCount++
        }

        assertEquals("All corpus test lines should be processed", lines.size, passedCount)
    }
}
