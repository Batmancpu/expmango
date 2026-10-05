package com.mangoloads.expmango

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
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
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0A0A0B.toInt()
    }
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF19191C.toInt()
    }
    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF303035.toInt()
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF5F5F7.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val suggestionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE7E7EA.toInt()
        textAlign = Paint.Align.CENTER
    }

    private val keys = ArrayList<Key>()
    private var suggestions = emptyList<String>()
    private var pressedIndex = -1
    private var downX = 0f
    private var downY = 0f
    private var moved = false
    private val glide = StringBuilder()
    private val service = context as? MangoImeService

    init {
        setBackgroundColor(bg.color)
    }

    fun setSuggestions(words: List<String>) {
        suggestions = words
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(bg.color)
        rebuildKeys()

        val suggestionH = 46f * density

        textPaint.textSize = 15f * density
        for (i in suggestions.indices.take(3)) {
            val x = width * (i + 0.5f) / 3f
            canvas.drawText(
                suggestions[i],
                x,
                suggestionH * 0.67f,
                suggestionPaint
            )
        }

        for ((i, key) in keys.withIndex()) {
            val paint = if (i == pressedIndex) pressedPaint else keyPaint
            val rect = RectF(
                key.x + 2 * density,
                key.y + 2 * density,
                key.x + key.width - 2 * density,
                key.y + key.height - 2 * density
            )
            canvas.drawRoundRect(
                rect,
                10 * density,
                10 * density,
                paint
            )

            textPaint.textSize = when {
                key.label == "SPACE" -> 14f * density
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
                glide.clear()
                pressedIndex = findKey(event.x, event.y)
                if (pressedIndex >= 0) {
                    val label = keys[pressedIndex].label
                    if (label.length == 1 && label[0].isLetter()) {
                        glide.append(label.lowercase())
                    }
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (hypot(event.x - downX, event.y - downY) > 30f * density) {
                    moved = true
                }

                if (moved) {
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
                val wasGlide = moved && glide.length >= 3

                pressedIndex = -1
                invalidate()

                if (wasGlide) {
                    service?.onGlide(glide.toString())
                } else {
                    handleTap(idx)
                }

                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                pressedIndex = -1
                invalidate()
                return true
            }
        }

        return true
    }

    private fun handleTap(index: Int) {
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

        val suggestionH = 46f * density
        val rowH = max(46f * density, (height - suggestionH) / 4f)

        val rows = listOf(
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
            listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "⌫"),
            listOf("?123", ",", "SPACE", ".", "ENTER")
        )

        rows.forEachIndexed { rowIndex, row ->
            val y = suggestionH + rowIndex * rowH
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
