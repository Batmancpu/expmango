package com.mangoloads.expmango

import android.content.Context
import android.content.ContextWrapper
import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class FieldPolicyAndPrivacyTest {

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
    fun testFieldPolicyResolutions() {
        val normalInfo = EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }
        val normalPolicy = FieldPolicyResolver.fromEditorInfo(normalInfo)
        assertEquals(FieldMode.NORMAL, normalPolicy.mode)
        assertTrue(normalPolicy.canPredict)
        assertTrue(normalPolicy.canAutocorrect)
        assertTrue(normalPolicy.canLearn)
        assertFalse(normalPolicy.isPrivate)

        val passInfo = EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val passPolicy = FieldPolicyResolver.fromEditorInfo(passInfo)
        assertEquals(FieldMode.PASSWORD, passPolicy.mode)
        assertFalse(passPolicy.canPredict)
        assertFalse(passPolicy.canAutocorrect)
        assertFalse(passPolicy.canLearn)
        assertTrue(passPolicy.isPrivate)

        val pinInfo = EditorInfo().apply { inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD }
        val pinPolicy = FieldPolicyResolver.fromEditorInfo(pinInfo)
        assertEquals(FieldMode.PIN, pinPolicy.mode)
        assertFalse(pinPolicy.canPredict)
        assertFalse(pinPolicy.canAutocorrect)
        assertFalse(pinPolicy.canLearn)
        assertTrue(pinPolicy.isPrivate)

        val emailInfo = EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        val emailPolicy = FieldPolicyResolver.fromEditorInfo(emailInfo)
        assertEquals(FieldMode.EMAIL, emailPolicy.mode)
        assertTrue(emailPolicy.canPredict)
        assertFalse(emailPolicy.canAutocorrect) // Suppress ordinary spelling autocorrect
        assertTrue(emailPolicy.showDomainChips)

        val uriInfo = EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI }
        val uriPolicy = FieldPolicyResolver.fromEditorInfo(uriInfo)
        assertEquals(FieldMode.URI, uriPolicy.mode)
        assertFalse(uriPolicy.canAutocorrect)

        val termInfo = EditorInfo().apply { packageName = "com.termux" }
        val termPolicy = FieldPolicyResolver.fromEditorInfo(termInfo)
        assertEquals(FieldMode.TERMINAL, termPolicy.mode)
        assertFalse(termPolicy.canAutocorrect)
        assertFalse(termPolicy.canLearn)
    }

    @Test
    fun testForcedPerAppIncognito() {
        val forcedApps = setOf("com.example.secretapp")
        val info = EditorInfo().apply {
            packageName = "com.example.secretapp"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        val policy = FieldPolicyResolver.fromEditorInfo(info, forcedApps)
        assertEquals(FieldMode.INCOGNITO, policy.mode)
        assertTrue(policy.isPrivate)
        assertFalse(policy.canLearn)
    }

    @Test
    fun testEmailDomainProvider() {
        val provider = EmailDomainProvider()
        val defaultSuggestions = provider.getDomainSuggestions("user@")
        assertTrue("Default suggestions should contain gmail.com", defaultSuggestions.contains("gmail.com"))

        val filteredSuggestions = provider.getDomainSuggestions("user@out")
        assertTrue("Filtered suggestions should contain outlook.com", filteredSuggestions.contains("outlook.com"))

        provider.learnDomain("mycompany.org")
        val learnedSuggestions = provider.getDomainSuggestions("user@mycomp")
        assertTrue("Learned domain should be returned", learnedSuggestions.contains("mycompany.org"))
    }

    @Test
    fun testIncognitoPreventsLearning() {
        val incognitoPolicy = FieldPolicyResolver.resolve(FieldMode.INCOGNITO)
        engine.learn("secretword123", policy = incognitoPolicy)
        assertFalse("Secret word should not be learned in Incognito", engine.learningEngine.containsWord("secretword123"))
    }
}
