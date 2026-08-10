package com.example.mangafireviewer.browser

/**
 * Validates links delivered to the app by Android before they reach WebView.
 *
 * Intent filters are only a routing hint: another app can still target this
 * activity explicitly with arbitrary data. Keep this check separate from the
 * manifest so every incoming link is held to the same policy as in-WebView
 * main-frame navigation.
 */
object AppLinkPolicy {
    private const val MAX_URL_LENGTH = 8_192

    fun resolve(rawUrl: String?): String? {
        val candidate = rawUrl
            ?.takeIf { it.isNotBlank() }
            ?.takeIf { it.length <= MAX_URL_LENGTH }
            ?: return null

        val decision = NavigationPolicy.classifyRaw(
            rawUrl = candidate,
            isMainFrame = true,
            hasUserGesture = false,
        )

        return candidate.takeIf {
            decision == NavigationDecision.AllowInternal
        }
    }
}
