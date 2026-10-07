package com.mangoloads.expmango

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var dictionaryManager: DictionaryManager
    private lateinit var learningEngine: LearningEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dictionaryManager = DictionaryManager(this)
        learningEngine = LearningEngine(this)

        val rootScroll = ScrollView(this).apply {
            setBackgroundColor(0xFF111114.toInt())
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 48)
        }

        // Header
        val titleView = TextView(this).apply {
            text = "EXP Mango"
            textSize = 28f
            setTextColor(0xFFFFB24A.toInt()) // EXP Mango Accent
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val subtitleView = TextView(this).apply {
            text = "Fast, Local-First, Offline English & Hinglish Keyboard"
            textSize = 14f
            setTextColor(0xFFA5A5AE.toInt())
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 8, 0, 32)
        }

        // IME Enable Button
        val enableImeButton = Button(this).apply {
            text = "⚙️ Enable Keyboard Settings"
            setBackgroundColor(0xFF242428.toInt())
            setTextColor(0xFFF4F4F7.toInt())
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        }

        // Dictionary Manager Section
        val dictHeader = createSectionHeader("📚 Custom Dictionary Manager")
        val wordInput = EditText(this).apply {
            hint = "Add new word or Hinglish slang..."
            setHintTextColor(0xFF888888.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF242428.toInt())
            setPadding(24, 24, 24, 24)
        }

        val categorySpinner = Spinner(this).apply {
            val categories = WordCategory.values().map { it.displayName }
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, categories)
            setBackgroundColor(0xFF242428.toInt())
        }

        val addWordButton = Button(this).apply {
            text = "+ Add Word"
            setBackgroundColor(0xFFFF9F2D.toInt())
            setTextColor(0xFF111114.toInt())
            setOnClickListener {
                val typed = wordInput.text.toString().trim()
                if (typed.isNotBlank()) {
                    val selectedCategory = WordCategory.values()[categorySpinner.selectedItemPosition]
                    dictionaryManager.addWord(typed, selectedCategory)
                    wordInput.text.clear()
                    Toast.makeText(this@MainActivity, "Added '$typed' to ${selectedCategory.displayName}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Privacy Section
        val privacyHeader = createSectionHeader("🔒 Privacy & Incognito")
        val privacyText = TextView(this).apply {
            text = "• 100% Offline — No Internet permission\n" +
                "• No cloud AI, analytics, or remote tracking\n" +
                "• Password, PIN, and OTP fields automatically disable learning\n" +
                "• Per-App forced Incognito support"
            textSize = 13f
            setTextColor(0xFFA5A5AE.toInt())
            setPadding(0, 8, 0, 16)
        }

        val resetDataButton = Button(this).apply {
            text = "🗑️ Reset Learned Data"
            setBackgroundColor(0xFF3C3C44.toInt())
            setTextColor(0xFFFF6B6B.toInt())
            setOnClickListener {
                learningEngine.resetLearnedData()
                dictionaryManager.reset()
                Toast.makeText(this@MainActivity, "Learned data and custom dictionary reset", Toast.LENGTH_SHORT).show()
            }
        }

        // About & Licenses Section
        val aboutHeader = createSectionHeader("ℹ️ About & Open Source Licenses")
        val aboutText = TextView(this).apply {
            text = "EXP Mango Keyboard v1.0.0\n" +
                "Application ID: com.mangoloads.expmango\n\n" +
                "EXP Mango is built on permissively licensed open-source components.\n" +
                "Upstream Foundation: WM Keyboard (MIT License, Wasi Master).\n" +
                "All typing intelligence, Hinglish ranking, and prediction engine belong to EXP Mango."
            textSize = 12f
            setTextColor(0xFFA5A5AE.toInt())
            setPadding(0, 8, 0, 32)
        }

        container.addView(titleView)
        container.addView(subtitleView)
        container.addView(enableImeButton)
        container.addView(dictHeader)
        container.addView(wordInput)
        container.addView(categorySpinner)
        container.addView(addWordButton)
        container.addView(privacyHeader)
        container.addView(privacyText)
        container.addView(resetDataButton)
        container.addView(aboutHeader)
        container.addView(aboutText)

        rootScroll.addView(container)
        setContentView(rootScroll)
    }

    private fun createSectionHeader(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 18f
            setTextColor(0xFFF4F4F7.toInt())
            setPadding(0, 32, 0, 12)
        }
    }
}
