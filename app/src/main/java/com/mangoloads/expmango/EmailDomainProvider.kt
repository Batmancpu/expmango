package com.mangoloads.expmango

import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class EmailDomainProvider {

    private val builtInDomains = listOf(
        "gmail.com",
        "outlook.com",
        "yahoo.com",
        "icloud.com",
        "proton.me",
        "protonmail.com",
        "hotmail.com"
    )

    private val learnedDomains = ConcurrentHashMap<String, Int>()

    init {
        // Initialize default weights for built-in domains
        builtInDomains.forEachIndexed { idx, domain ->
            learnedDomains[domain] = 100 - idx * 5
        }
    }

    fun learnDomain(domain: String) {
        val clean = domain.trim().lowercase(Locale.ROOT).removePrefix("@")
        if (clean.length > 3 && clean.contains(".")) {
            learnedDomains.compute(clean) { _, count -> (count ?: 10) + 20 }
        }
    }

    fun getDomainSuggestions(token: String, limit: Int = 3): List<String> {
        val atIndex = token.lastIndexOf('@')
        if (atIndex < 0) return emptyList()

        val prefix = token.substring(atIndex + 1).lowercase(Locale.ROOT)

        return learnedDomains.entries
            .asSequence()
            .filter { (domain, _) -> domain.startsWith(prefix) }
            .sortedByDescending { it.value }
            .map { it.key }
            .take(limit)
            .toList()
    }

    fun allDomains(): List<String> = learnedDomains.entries
        .sortedByDescending { it.value }
        .map { it.key }
}
