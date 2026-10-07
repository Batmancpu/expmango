package com.mangoloads.expmango

import android.content.Context
import android.content.ContextWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class LearningAndDictionaryTest {

    private lateinit var dictionaryManager: DictionaryManager
    private lateinit var learningEngine: LearningEngine

    @Before
    fun setUp() {
        val mockContext = object : ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = File("build/test-tmp").apply { mkdirs() }
        }
        dictionaryManager = DictionaryManager(mockContext)
        learningEngine = LearningEngine(mockContext)
    }

    @Test
    fun testDictionaryManagerCrudAndCategories() {
        dictionaryManager.reset()

        dictionaryManager.addWord("polity", WordCategory.UPSC, 200)
        dictionaryManager.addWord("khana", WordCategory.HINGLISH, 150)
        dictionaryManager.addBulkWords(listOf("react", "kotlin", "gradle"), WordCategory.TECHNOLOGY)

        val upscWords = dictionaryManager.getWords(WordCategory.UPSC)
        assertEquals(1, upscWords.size)
        assertEquals("polity", upscWords.first().word)

        val techWords = dictionaryManager.getWords(WordCategory.TECHNOLOGY)
        assertEquals(3, techWords.size)

        val searchResults = dictionaryManager.search("kot")
        assertEquals(1, searchResults.size)
        assertEquals("kotlin", searchResults.first().word)

        val counts = dictionaryManager.getCategoryCounts()
        assertEquals(1, counts[WordCategory.UPSC])
        assertEquals(3, counts[WordCategory.TECHNOLOGY])

        dictionaryManager.removeWord("react")
        assertEquals(2, dictionaryManager.getWords(WordCategory.TECHNOLOGY).size)

        dictionaryManager.reset()
        assertEquals(0, dictionaryManager.getWordCount())
    }

    @Test
    fun testLearningEngineBigramsAndTrigrams() {
        val policy = FieldPolicyResolver.resolve(FieldMode.NORMAL)

        learningEngine.learnWord("mangoloads", policy)
        assertTrue(learningEngine.containsWord("mangoloads"))

        learningEngine.learnBigram("mango", "loads", policy)
        val bigramNext = learningEngine.getBigramNext("mango")
        assertEquals(1, bigramNext.size)
        assertEquals("loads", bigramNext.first().first)

        learningEngine.learnTrigram("i", "am", "going", policy)
        val trigramNext = learningEngine.getTrigramNext("i", "am")
        assertEquals(1, trigramNext.size)
        assertEquals("going", trigramNext.first().first)

        learningEngine.resetLearnedData()
    }
}
