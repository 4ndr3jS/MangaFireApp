package com.example.mangafireviewer.browser

import android.content.Context
import androidx.core.content.edit

class LastReadStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "mangafire_last_read",
        Context.MODE_PRIVATE,
    )

    fun load(): LastReadChapter? {
        val url = preferences.getString("url", null) ?: return null
        val chapter = MangaFireChapterRoute.parse(url, pageTitle = null) ?: run {
            clear()
            return null
        }
        val savedTitle = preferences.getString("title", null)
            ?.trim()
            ?.take(100)
            ?.takeIf(String::isNotEmpty)

        return if (savedTitle == null) chapter else chapter.copy(mangaTitle = savedTitle)
    }

    fun save(chapter: LastReadChapter) {
        preferences.edit {
            putString("url", chapter.url)
            putString("title", chapter.mangaTitle)
        }
    }

    fun clear() {
        preferences.edit { clear() }
    }
}
