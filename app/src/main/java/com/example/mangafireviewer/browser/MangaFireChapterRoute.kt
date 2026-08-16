package com.example.mangafireviewer.browser

import java.net.URI
import java.util.Locale

object MangaFireChapterRoute {
    private const val HOST = "mangafire.to"
    private val mangaSlug = Regex("^[a-z0-9]+(?:[.-][a-z0-9]+)*$")
    private val language = Regex("^[a-z]{2}(?:-[a-z]{2})?$")
    private val chapterSlug = Regex("^chapter-([a-z0-9]+(?:[.-][a-z0-9]+)*)$")

    fun parse(rawUrl: String, pageTitle: String?): LastReadChapter? {
        return try {
            val uri = URI(rawUrl)
            if (!uri.scheme.equals("https", ignoreCase = true)) return null
            if (!uri.host.equals(HOST, ignoreCase = true)) return null
            if (uri.rawUserInfo != null) return null
            if (uri.port != -1 && uri.port != 443) return null

            val parts = uri.path
                ?.split('/')
                ?.filter(String::isNotBlank)
                ?: return null
            if (parts.size != 4 || !parts[0].equals("read", ignoreCase = true)) return null

            val rawManga = parts[1].lowercase(Locale.ROOT)
            val rawLanguage = parts[2].lowercase(Locale.ROOT)
            val rawChapter = parts[3].lowercase(Locale.ROOT)
            if (!mangaSlug.matches(rawManga)) return null
            if (!language.matches(rawLanguage)) return null
            val chapterMatch = chapterSlug.matchEntire(rawChapter) ?: return null

            val chapter = "Chapter ${chapterMatch.groupValues[1].replace('-', ' ')}"
            LastReadChapter(
                url = "https://$HOST/read/$rawManga/$rawLanguage/$rawChapter",
                mangaTitle = titleFrom(pageTitle, rawManga),
                chapter = chapter,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun titleFrom(pageTitle: String?, slug: String): String {
        val title = pageTitle
            ?.substringBefore(" Manga, Chapter ")
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        if (title != null) return title.take(100)

        return slug
            .substringBeforeLast('.')
            .split('-')
            .joinToString(" ") { word ->
                word.replaceFirstChar { character -> character.titlecase(Locale.ROOT) }
            }
            .take(100)
    }
}
