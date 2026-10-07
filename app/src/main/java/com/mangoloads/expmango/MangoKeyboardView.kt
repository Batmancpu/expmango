package com.mangoloads.expmango

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

class MangoKeyboardView(context: Context) : View(context) {

    private data class Key(
        val label: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    )

    private val density = resources.displayMetrics.density

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF111114.toInt() }
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF242428.toInt() }
    private val keyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3C3C44.toInt() }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFB24A.toInt() }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF4F4F7.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val suggestionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val lockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFA99BFF.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val toolbarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFA5A5AE.toInt()
        textAlign = Paint.Align.CENTER
    }

    private val keys = ArrayList<Key>()
    private var suggestions = emptyList<String>()
    private var isIncognito = false
    private var pressedIndex = -1
    private var downX = 0f
    private var downY = 0f
    private var moved = false
    private var isSpaceDrag = false
    private var spaceDragLastX = 0f

    private val glide = StringBuilder()
    private val service = context as? MangoImeService

    init {
        setBackgroundColor(bg.color)
    }

    fun setSuggestions(words: List<String>, isPrivate: Boolean = false) {
        suggestions = words
        isIncognito = isPrivate
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(bg.color)
        rebuildKeys()

        val toolbarH = 36f * density
        val suggestionH = 42f * density

        // Draw Toolbar
        toolbarPaint.textSize = 16f * density
        canvas.drawText("😊", width * 0.15f, toolbarH * 0.65f, toolbarPaint)
        canvas.drawText("🌐", width * 0.38f, toolbarH * 0.65f, toolbarPaint)
        canvas.drawText("📋", width * 0.62f, toolbarH * 0.65f, toolbarPaint)
        canvas.drawText("⚙️", width * 0.85f, toolbarH * 0.65f, toolbarPaint)

        // Draw Suggestion Strip
        val suggestionY = toolbarH
        if (isIncognito) {
            lockPaint.textSize = 12f * density
            canvas.drawText("🔒 Incognito", 42f * density, suggestionY + suggestionH * 0.65f, lockPaint)
        }

        textPaint.textSize = 15f * density
        for (i in suggestions.indices.take(3)) {
            val x = width * (i + 0.5f) / 3f
            canvas.drawText(
                suggestions[i],
                x,
                suggestionY + suggestionH * 0.67f,
                suggestionPaint
            )
        }

        // Draw Keys
        for ((i, key) in keys.withIndex()) {
            val isPressed = (i == pressedIndex)
            val paint = when {
                isPressed -> keyPressedPaint
                key.label == "ENTER" -> accentPaint
                else -> keyPaint
            }

            val rect = RectF(
                key.x + 3 * density,
                key.y + 3 * density,
                key.x + key.width - 3 * density,
                key.y + key.height - 3 * density
            )

            canvas.drawRoundRect(
                rect,
                12 * density, // 12dp squircle corner radius
                12 * density,
                paint
            )

            textPaint.color = if (key.label == "ENTER") 0xFF111114.toInt() else 0xFFF4F4F7.toInt()
            textPaint.textSize = when {
                key.label == "SPACE" -> 13f * density
                key.label.length > 4 -> 12f * density
                else -> 18f * density
            }

            canvas.drawText(
                displayLabel(key.label),
                key.x + key.width / 2f,
                key.y + key.height * 0.64f,
                textPaint
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                moved = false
                isSpaceDrag = false
                glide.clear()
                pressedIndex = findKey(event.x, event.y)

                try {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                } catch (_: Exception) {}

                if (pressedIndex >= 0) {
                    val label = keys[pressedIndex].label
                    if (label == "SPACE") {
                        isSpaceDrag = true
                        spaceDragLastX = event.x
                    } else if (label.length == 1 && label[0].isLetter()) {
                        glide.append(label.lowercase())
                    }
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (hypot(dx, dy) > 24f * density) {
                    moved = true
                }

                if (isSpaceDrag) {
                    val dragDx = event.x - spaceDragLastX
                    if (abs(dragDx) > 16f * density) {
                        val ic = service?.currentInputConnection
                        if (ic != null) {
                            if (dragDx > 0) {
                                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_RIGHT))
                                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DPAD_RIGHT))
                            } else {
                                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_LEFT))
                                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DPAD_LEFT))
                            }
                        }
                        spaceDragLastX = event.x
                    }
                } else if (moved) {
                    val idx = findKey(event.x, event.y)
                    if (idx >= 0) {
                        val label = keys[idx].label
                        if (label.length == 1 && label[0].isLetter()) {
                            val lower = label.lowercase()
                            if (glide.lastOrNull()?.toString() != lower) {
                                glide.append(lower)
                            }
                        }
                    }
                }

                return true
            }

            MotionEvent.ACTION_UP -> {
                val idx = findKey(event.x, event.y)
                val wasGlide = moved && glide.length >= 3 && !isSpaceDrag

                pressedIndex = -1
                isSpaceDrag = false
                invalidate()

                if (wasGlide) {
                    service?.onGlide(glide.toString())
                } else if (!moved) {
                    handleTap(idx, event.x, event.y)
                }

                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                pressedIndex = -1
                isSpaceDrag = false
                invalidate()
                return true
            }
        }

        return true
    }

    private fun handleTap(index: Int, x: Float, y: Float) {
        val toolbarH = 36f * density
        val suggestionH = 42f * density

        // Handle Toolbar Taps
        if (y < toolbarH) {
            val section = (x / (width / 4f)).toInt().coerceIn(0, 3)
            when (section) {
                0 -> service?.currentInputConnection?.commitText("😊", 1)
                1 -> service?.onSpace()
                2 -> service?.onSpace()
                3 -> {
                    val intent = android.content.Intent(context, MainActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            }
            return
        }

        // Handle Suggestion Strip Taps
        if (y < toolbarH + suggestionH) {
            val col = (x / (width / 3f)).toInt().coerceIn(0, 2)
            if (col in suggestions.indices) {
                service?.onSuggestion(suggestions[col])
                return
            }
        }

        if (index < 0) return

        when (val label = keys[index].label) {
            "SHIFT" -> service?.toggleShift()
            "⌫" -> service?.onBackspace()
            "SPACE" -> service?.onSpace()
            "ENTER" -> service?.onEnter()
            "?123" -> Unit
            else -> if (label.length == 1 && label[0].isLetter()) {
                service?.onCharacter(label[0])
            }
        }
    }

    private fun findKey(x: Float, y: Float): Int =
        keys.indexOfFirst {
            x >= it.x &&
                x <= it.x + it.width &&
                y >= it.y &&
                y <= it.y + it.height
        }

    private fun rebuildKeys() {
        keys.clear()

        val toolbarH = 36f * density
        val suggestionH = 42f * density
        val startY = toolbarH + suggestionH
        val rowH = max(48f * density, (height - startY) / 4f)

        val rows = listOf(
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
            listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "⌫"),
            listOf("?123", ",", "SPACE", ".", "ENTER")
        )

        rows.forEachIndexed { rowIndex, row ->
            val y = startY + rowIndex * rowH
            val totalWeight = row.sumOf { weight(it).toDouble() }.toFloat()
            var x = 0f

            row.forEach { label ->
                val w = width * weight(label) / totalWeight
                keys.add(
                    Key(
                        label = label,
                        x = x,
                        y = y,
                        width = w,
                        height = rowH
                    )
                )
                x += w
            }
        }
    }

    private fun weight(label: String): Float = when (label) {
        "SHIFT", "⌫", "?123" -> 1.35f
        "SPACE" -> 3.2f
        "ENTER" -> 1.55f
        else -> 1f
    }

    private fun displayLabel(label: String): String {
        if (label.length != 1 || !label[0].isLetter()) return label
        return if (service?.currentShifted() == true) {
            label.uppercase()
        } else {
            label.lowercase()
        }
    }
}
