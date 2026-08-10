package com.example.mangafireviewer.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLinkPolicyTest {
    @Test
    fun acceptsMangaFireHomePage() {
        assertAccepted("https://mangafire.to/")
        assertAccepted("https://mangafire.to")
    }

    @Test
    fun acceptsMangaAndChapterRoutes() {
        assertAccepted("https://mangafire.to/manga/example-title.abc12")
        assertAccepted("https://mangafire.to/read/example-title.abc12/en/chapter-10")
    }

    @Test
    fun preservesQueryAndFragmentForSiteRouting() {
        assertAccepted("https://mangafire.to/filter?keyword=one%20piece")
        assertAccepted("https://mangafire.to/read/example#page=12")
    }

    @Test
    fun hostComparisonIsCaseInsensitive() {
        assertAccepted("https://MANGAFIRE.TO/read/example")
        assertAccepted("HTTPS://mangafire.to/read/example")
    }

    @Test
    fun allowsOnlyTheExactTrustedHost() {
        assertRejected("https://www.mangafire.to/read/example")
        assertRejected("https://api.mangafire.to/read/example")
        assertRejected("https://mangafire.to.evil.example/read/example")
        assertRejected("https://evil-mangafire.to/read/example")
    }

    @Test
    fun rejectsLookalikeAndEncodedHosts() {
        assertRejected("https://mangafire.to%2eevil.example/read/example")
        assertRejected("https://mangafire%E3%80%82to/read/example")
        assertRejected("https://mangafire.tо/read/example")
    }

    @Test
    fun rejectsCleartextAndNonWebSchemes() {
        assertRejected("http://mangafire.to/read/example")
        assertRejected("intent://mangafire.to/read/example")
        assertRejected("javascript:alert(1)")
        assertRejected("file:///android_asset/example.html")
    }

    @Test
    fun rejectsCredentialsEvenOnTrustedHost() {
        assertRejected("https://user@mangafire.to/read/example")
        assertRejected("https://user:password@mangafire.to/read/example")
        assertRejected("https://mangafire.to@evil.example/read/example")
    }

    @Test
    fun acceptsDefaultTlsPortOnly() {
        assertAccepted("https://mangafire.to:443/read/example")
        assertRejected("https://mangafire.to:80/read/example")
        assertRejected("https://mangafire.to:8443/read/example")
    }

    @Test
    fun rejectsExternalHttpsLinks() {
        assertRejected("https://anilist.co/manga/123")
        assertRejected("https://example.com/")
    }

    @Test
    fun rejectsMissingAndBlankValues() {
        assertNull(AppLinkPolicy.resolve(null))
        assertRejected("")
        assertRejected("   ")
        assertRejected("\t\r\n")
    }

    @Test
    fun rejectsMalformedValues() {
        assertRejected("not a url")
        assertRejected("https://")
        assertRejected("https:///read/example")
        assertRejected("https://mangafire.to:invalid/read/example")
        assertRejected("https://mangafire.to/read path")
    }

    @Test
    fun rejectsUnreasonablyLongValues() {
        val oversizedUrl = buildString {
            append("https://mangafire.to/read/")
            repeat(8_192) {
                append('a')
            }
        }

        assertRejected(oversizedUrl)
    }

    @Test
    fun returnsTheOriginalUrlWithoutRewritingSiteState() {
        val original = "https://MANGAFIRE.TO/read/Title.Case?mode=paged#page=7"

        assertEquals(original, AppLinkPolicy.resolve(original))
    }

    private fun assertAccepted(url: String) {
        assertEquals(url, AppLinkPolicy.resolve(url))
    }

    private fun assertRejected(url: String) {
        assertNull(AppLinkPolicy.resolve(url))
    }
}
