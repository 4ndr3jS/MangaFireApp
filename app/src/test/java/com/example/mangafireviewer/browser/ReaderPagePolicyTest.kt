package com.example.mangafireviewer.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPagePolicyTest {
    @Test
    fun acceptsChapterReaderRoutes() {
        assertReader("https://mangafire.to/read/example-title.abc12/en/chapter-1")
        assertReader("https://mangafire.to/read/example-title.abc12/en/chapter-12.5")
    }

    @Test
    fun acceptsReaderQueryAndFragmentState() {
        assertReader("https://mangafire.to/read/example/en/chapter-2?mode=paged")
        assertReader("https://mangafire.to/read/example/en/chapter-2#page=14")
    }

    @Test
    fun sectionComparisonIsCaseInsensitive() {
        assertReader("https://MANGAFIRE.TO/READ/example/en/chapter-1")
        assertReader("HTTPS://mangafire.to/Read/example/en/chapter-1")
    }

    @Test
    fun acceptsExplicitDefaultTlsPort() {
        assertReader("https://mangafire.to:443/read/example/en/chapter-1")
    }

    @Test
    fun rejectsHomeCatalogAndTitleRoutes() {
        assertNotReader("https://mangafire.to/")
        assertNotReader("https://mangafire.to/home")
        assertNotReader("https://mangafire.to/filter?keyword=example")
        assertNotReader("https://mangafire.to/manga/example-title.abc12")
    }

    @Test
    fun rejectsAccountAndPreferenceRoutes() {
        assertNotReader("https://mangafire.to/profile")
        assertNotReader("https://mangafire.to/user/settings")
        assertNotReader("https://mangafire.to/login")
    }

    @Test
    fun rejectsBareReaderSectionWithoutChapterIdentity() {
        assertNotReader("https://mangafire.to/read")
        assertNotReader("https://mangafire.to/read/")
        assertNotReader("https://mangafire.to/read?mode=paged")
    }

    @Test
    fun rejectsMisleadingReaderPrefixes() {
        assertNotReader("https://mangafire.to/reader/example")
        assertNotReader("https://mangafire.to/read-later/example")
        assertNotReader("https://mangafire.to/manga/read/example")
    }

    @Test
    fun rejectsLookalikeAndSubdomainHosts() {
        assertNotReader("https://mangafire.to.evil.example/read/example")
        assertNotReader("https://evil-mangafire.to/read/example")
        assertNotReader("https://www.mangafire.to/read/example")
    }

    @Test
    fun rejectsExternalHttpsPages() {
        assertNotReader("https://example.com/read/example")
        assertNotReader("https://anilist.co/read/example")
    }

    @Test
    fun rejectsCleartextAndCustomSchemes() {
        assertNotReader("http://mangafire.to/read/example")
        assertNotReader("intent://mangafire.to/read/example")
        assertNotReader("javascript:alert(1)")
        assertNotReader("file:///read/example")
    }

    @Test
    fun rejectsCredentialsAndUnexpectedPorts() {
        assertNotReader("https://user@mangafire.to/read/example")
        assertNotReader("https://mangafire.to@evil.example/read/example")
        assertNotReader("https://mangafire.to:8443/read/example")
    }

    @Test
    fun rejectsMissingBlankAndMalformedValues() {
        assertFalse(ReaderPagePolicy.isReaderPage(null))
        assertNotReader("")
        assertNotReader("   ")
        assertNotReader("not a url")
        assertNotReader("https://")
        assertNotReader("https://mangafire.to/read title")
    }

    @Test
    fun rejectsUnreasonablyLongUrls() {
        val oversizedUrl = buildString {
            append("https://mangafire.to/read/")
            repeat(8_192) {
                append('a')
            }
        }

        assertNotReader(oversizedUrl)
    }

    private fun assertReader(url: String) {
        assertTrue(url, ReaderPagePolicy.isReaderPage(url))
    }

    private fun assertNotReader(url: String) {
        assertFalse(url, ReaderPagePolicy.isReaderPage(url))
    }
}
