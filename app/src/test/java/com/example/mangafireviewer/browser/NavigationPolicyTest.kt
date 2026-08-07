package com.example.mangafireviewer.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationPolicyTest {
    @Test
    fun trustedHttpsMainFrameIsAllowed() {
        assertDecision(
            "https://mangafire.to/",
            expected = NavigationDecision.AllowInternal,
        )
        assertDecision(
            "https://MANGAFIRE.TO/watch/example?ep=1",
            expected = NavigationDecision.AllowInternal,
        )
        assertDecision(
            "https://mangafire.to/profile",
            hasUserGesture = true,
            expected = NavigationDecision.AllowInternal,
        )
    }

    @Test
    fun cleartextAndCustomSchemesAreBlocked() {
        assertDecision(
            "http://mangafire.to/home",
            expected = NavigationDecision.Block,
        )
        assertDecision(
            "javascript:alert(1)",
            expected = NavigationDecision.Block,
        )
        assertDecision(
            "content://mangafire.to/private",
            expected = NavigationDecision.Block,
        )
        assertDecision(
            "intent://mangafire.to/home",
            expected = NavigationDecision.Block,
        )
    }

    @Test
    fun lookalikeHostIsNeverTreatedAsInternal() {
        assertDecision(
            "https://mangafire.to.evil.example/watch",
            hasUserGesture = true,
            expected = NavigationDecision.OpenExternal,
        )
        assertDecision(
            "https://mangafire.to.evil.example/watch",
            hasUserGesture = false,
            expected = NavigationDecision.Block,
        )
    }

    @Test
    fun userInfoAndUnexpectedPortsAreBlocked() {
        assertDecision(
            "https://mangafire.to@evil.example/watch",
            expected = NavigationDecision.Block,
        )
        assertDecision(
            "https://user@mangafire.to/watch",
            expected = NavigationDecision.Block,
        )
        assertDecision(
            "https://mangafire.to:8443/watch",
            expected = NavigationDecision.Block,
        )
        assertDecision(
            "https://mangafire.to:443/watch",
            expected = NavigationDecision.AllowInternal,
        )
    }

    @Test
    fun malformedUrlsAreBlocked() {
        assertDecision(
            "not a url",
            expected = NavigationDecision.Block,
        )
        assertDecision(
            "https://",
            expected = NavigationDecision.Block,
        )
        assertDecision(
            "",
            expected = NavigationDecision.Block,
        )
    }

    @Test
    fun userInitiatedExternalHttpsOpensExternally() {
        assertDecision(
            "https://anilist.co/manga/140475",
            hasUserGesture = true,
            expected = NavigationDecision.OpenExternal,
        )
        assertDecision(
            "https://anilist.co/manga/140475",
            hasUserGesture = false,
            expected = NavigationDecision.Block,
        )
    }

    @Test
    fun secureSubframesAreAllowedButCleartextSubframesAreBlocked() {
        assertDecision(
            "https://o48.mfcdn1.xyz/chapter/page.webp",
            isMainFrame = false,
            expected = NavigationDecision.AllowInternal,
        )
        assertDecision(
            "http://o48.mfcdn1.xyz/chapter/page.webp",
            isMainFrame = false,
            expected = NavigationDecision.Block,
        )
    }

    private fun assertDecision(
        rawUrl: String,
        isMainFrame: Boolean = true,
        hasUserGesture: Boolean = false,
        expected: NavigationDecision,
    ) {
        assertEquals(
            expected,
            NavigationPolicy.classifyRaw(
                rawUrl = rawUrl,
                isMainFrame = isMainFrame,
                hasUserGesture = hasUserGesture,
            ),
        )
    }
}
