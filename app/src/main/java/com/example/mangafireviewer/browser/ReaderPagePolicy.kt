package com.example.mangafireviewer.browser

import java.net.URI
import java.util.Locale

/** Identifies trusted chapter-reader pages that benefit from an awake screen. */
object ReaderPagePolicy {
    private const val MAX_URL_LENGTH = 8_192
    private const val READER_SECTION = "read"

    fun isReaderPage(rawUrl: String?): Boolean {
        val candidate = rawUrl
            ?.takeIf { it.isNotBlank() }
            ?.takeIf { it.length <= MAX_URL_LENGTH }
            ?: return false

        val isTrusted = NavigationPolicy.classifyRaw(
            rawUrl = candidate,
            isMainFrame = true,
            hasUserGesture = false,
        ) == NavigationDecision.AllowInternal
        if (!isTrusted) return false

        val pathSegments = try {
            URI(candidate)
                .rawPath
                ?.split('/')
                ?.filter(String::isNotBlank)
                .orEmpty()
        } catch (_: Exception) {
            return false
        }

        return pathSegments.size >= 2 &&
            pathSegments.first().lowercase(Locale.ROOT) == READER_SECTION
    }
}
