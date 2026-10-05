package com.mangoloads.expmango

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(0xFF0A0A0B.toInt())
        }

        val title = TextView(this).apply {
            text = "Experimental Mango"
            textSize = 28f
            setTextColor(0xFFF5F5F7.toInt())
            gravity = Gravity.CENTER
        }

        val info = TextView(this).apply {
            text = "Fast local-first Hinglish keyboard.\n\n" +
                "Typing never waits for a model or disk write. " +
                "Vocabulary and phrase learning are persisted asynchronously. " +
                "Password and sensitive fields disable learning and autocorrect."
            textSize = 16f
            setTextColor(0xFFB8B8BE.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }

        val button = Button(this).apply {
            text = "Open keyboard settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        }

        root.addView(title, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(info, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(button, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        setContentView(root)
    }
}
