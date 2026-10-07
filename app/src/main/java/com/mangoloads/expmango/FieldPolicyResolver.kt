package com.mangoloads.expmango

import android.text.InputType
import android.view.inputmethod.EditorInfo

enum class FieldMode {
    NORMAL,
    EMAIL,
    URI,
    PASSWORD,
    PIN,
    OTP,
    CODE,
    TERMINAL,
    SEARCH,
    INCOGNITO
}

data class FieldPolicy(
    val mode: FieldMode,
    val canPredict: Boolean,
    val canAutocorrect: Boolean,
    val canLearn: Boolean,
    val showDomainChips: Boolean,
    val isPrivate: Boolean
)

object FieldPolicyResolver {

    fun resolve(mode: FieldMode): FieldPolicy = when (mode) {
        FieldMode.NORMAL -> FieldPolicy(
            mode = mode,
            canPredict = true,
            canAutocorrect = true,
            canLearn = true,
            showDomainChips = false,
            isPrivate = false
        )

        FieldMode.EMAIL -> FieldPolicy(
            mode = mode,
            canPredict = true,
            canAutocorrect = false, // Suppress normal spelling autocorrect in domain/email text
            canLearn = true,
            showDomainChips = true,
            isPrivate = false
        )

        FieldMode.URI -> FieldPolicy(
            mode = mode,
            canPredict = true,
            canAutocorrect = false, // Conservative, no spelling autocorrect
            canLearn = true,
            showDomainChips = false,
            isPrivate = false
        )

        FieldMode.PASSWORD, FieldMode.PIN, FieldMode.OTP -> FieldPolicy(
            mode = mode,
            canPredict = false,
            canAutocorrect = false,
            canLearn = false,
            showDomainChips = false,
            isPrivate = true
        )

        FieldMode.CODE, FieldMode.TERMINAL -> FieldPolicy(
            mode = mode,
            canPredict = true,
            canAutocorrect = false, // Conservative correction
            canLearn = false,
            showDomainChips = false,
            isPrivate = false
        )

        FieldMode.SEARCH -> FieldPolicy(
            mode = mode,
            canPredict = true,
            canAutocorrect = true,
            canLearn = true,
            showDomainChips = false,
            isPrivate = false
        )

        FieldMode.INCOGNITO -> FieldPolicy(
            mode = mode,
            canPredict = true,
            canAutocorrect = true,
            canLearn = false,
            showDomainChips = false,
            isPrivate = true
        )
    }

    fun fromEditorInfo(info: EditorInfo?, forcedIncognitoApps: Set<String> = emptySet()): FieldPolicy {
        val pkg = info?.packageName.orEmpty()
        if (forcedIncognitoApps.contains(pkg)) {
            return resolve(FieldMode.INCOGNITO)
        }

        val type = info?.inputType ?: 0
        val variation = type and InputType.TYPE_MASK_VARIATION
        val klass = type and InputType.TYPE_MASK_CLASS

        if (klass == InputType.TYPE_CLASS_NUMBER && (
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD ||
                variation == InputType.TYPE_NUMBER_FLAG_SIGNED
            )
        ) {
            return resolve(FieldMode.PIN)
        }

        if (klass == InputType.TYPE_CLASS_TEXT) {
            when (variation) {
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> return resolve(FieldMode.PASSWORD)

                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> return resolve(FieldMode.EMAIL)

                InputType.TYPE_TEXT_VARIATION_URI -> return resolve(FieldMode.URI)

                InputType.TYPE_TEXT_VARIATION_FILTER -> return resolve(FieldMode.SEARCH)
            }
        }

        val lowerPkg = pkg.lowercase()
        if (lowerPkg.contains("termux") || lowerPkg.contains("juicessh") || lowerPkg.contains("connectbot") || lowerPkg.contains("terminal")) {
            return resolve(FieldMode.TERMINAL)
        }

        return resolve(FieldMode.NORMAL)
    }
}
