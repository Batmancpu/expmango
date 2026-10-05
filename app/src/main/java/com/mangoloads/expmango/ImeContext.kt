package com.mangoloads.expmango

import android.text.InputType
import android.view.inputmethod.EditorInfo

enum class InputPolicy {
    NORMAL,
    NO_AUTOCORRECT,
    SENSITIVE
}

data class ImeContext(
    val packageName: String,
    val policy: InputPolicy
) {
    val canLearn: Boolean
        get() = policy == InputPolicy.NORMAL

    val canCorrect: Boolean
        get() = policy == InputPolicy.NORMAL

    companion object {
        fun from(info: EditorInfo?): ImeContext {
            val pkg = info?.packageName.orEmpty()
            val type = info?.inputType ?: 0
            val variation = type and InputType.TYPE_MASK_VARIATION
            val klass = type and InputType.TYPE_MASK_CLASS

            val sensitive = klass == InputType.TYPE_CLASS_TEXT && variation in setOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            )

            val noCorrect = variation in setOf(
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                InputType.TYPE_TEXT_VARIATION_URI,
                InputType.TYPE_TEXT_VARIATION_FILTER
            )

            val terminal = pkg.contains("termux", true) ||
                pkg.contains("juicessh", true) ||
                pkg.contains("connectbot", true) ||
                pkg.contains("termoneplus", true)

            return when {
                sensitive -> ImeContext(pkg, InputPolicy.SENSITIVE)
                noCorrect || terminal -> ImeContext(pkg, InputPolicy.NO_AUTOCORRECT)
                else -> ImeContext(pkg, InputPolicy.NORMAL)
            }
        }
    }
}
