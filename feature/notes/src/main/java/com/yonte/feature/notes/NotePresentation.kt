package com.yonte.feature.notes

import androidx.compose.runtime.Immutable

@Immutable
data class NotePreview(
    val title: String,
    val body: String,
    val tags: List<String>,
)

fun notePreview(title: String, body: String): NotePreview {
    val tags = Regex("(?<!\\w)#([\\p{L}\\p{N}_-]+)")
        .findAll("$title $body")
        .map { it.groupValues[1].lowercase() }
        .distinct()
        .take(4)
        .toList()
    val cleanBody = body.lines()
        .map { it.trim().removePrefix("- [ ] ").removePrefix("- [x] ") }
        .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
        .orEmpty()
    return NotePreview(title.trim(), cleanBody, tags)
}
