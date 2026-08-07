package com.example.mangafireviewer.browser

import android.net.Uri
import java.net.URI
import java.util.Locale

sealed interface NavigationDecision {
    data object AllowInternal : NavigationDecision
    data object OpenExternal : NavigationDecision
    data object Block : NavigationDecision
}

object NavigationPolicy {
    private const val TRUSTED_HOST = "mangafire.to"

    fun classify(
        uri: Uri,
        isMainFrame: Boolean,
        hasUserGesture: Boolean,
    ): NavigationDecision = try {
        classifyParts(
            scheme = uri.scheme,
            host = uri.host,
            userInfo = uri.userInfo,
            port = uri.port,
            isMainFrame = isMainFrame,
            hasUserGesture = hasUserGesture,
        )
    } catch (_: RuntimeException) {
        NavigationDecision.Block
    }

    internal fun classifyRaw(
        rawUrl: String,
        isMainFrame: Boolean,
        hasUserGesture: Boolean,
    ): NavigationDecision = try {
        val uri = URI(rawUrl)
        classifyParts(
            scheme = uri.scheme,
            host = uri.host,
            userInfo = uri.rawUserInfo,
            port = uri.port,
            isMainFrame = isMainFrame,
            hasUserGesture = hasUserGesture,
        )
    } catch (_: Exception) {
        NavigationDecision.Block
    }

    private fun classifyParts(
        scheme: String?,
        host: String?,
        userInfo: String?,
        port: Int,
        isMainFrame: Boolean,
        hasUserGesture: Boolean,
    ): NavigationDecision {
        if (!userInfo.isNullOrEmpty()) return NavigationDecision.Block
        if (!scheme.equals("https", ignoreCase = true)) return NavigationDecision.Block
        if (port != -1 && port != 443) return NavigationDecision.Block

        val normalizedHost = host?.lowercase(Locale.ROOT)
            ?: return NavigationDecision.Block

        if (!isMainFrame) {
            return NavigationDecision.AllowInternal
        }

        if (normalizedHost == TRUSTED_HOST) {
            return NavigationDecision.AllowInternal
        }

        return if (hasUserGesture) {
            NavigationDecision.OpenExternal
        } else {
            NavigationDecision.Block
        }
    }
}
