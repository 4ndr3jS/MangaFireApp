package com.example.mangafireviewer.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MangaFireChapterRouteTest {
    @Test
    fun realChapterUrlCreatesSavedChapter() {
        assertEquals(
            LastReadChapter(
                url = "https://mangafire.to/read/my-hero-academiaa.kkvk9/en/chapter-121",
                mangaTitle = "My Hero Academia",
                chapter = "Chapter 121",
            ),
            parse(
                "https://mangafire.to/read/my-hero-academiaa.kkvk9/en/chapter-121",
                "My Hero Academia Manga, Chapter 121 | Read Online on MangaFire",
            ),
        )
    }

    @Test
    fun queryAndPageFragmentAreRemovedFromSavedLink() {
        val chapter = parse(
            "https://mangafire.to/read/example.abc12/en/chapter-4?mode=paged#page=7",
            "Example Manga, Chapter 4 | Read Online on MangaFire",
        )

        assertEquals(
            "https://mangafire.to/read/example.abc12/en/chapter-4",
            chapter?.url,
        )
    }

    @Test
    fun titleFallsBackToReadableMangaSlug() {
        val chapter = parse(
            "https://mangafire.to/read/my-example-manga.abc12/en/chapter-2",
            pageTitle = null,
        )

        assertEquals("My Example Manga", chapter?.mangaTitle)
    }

    @Test
    fun decimalAndNamedChaptersAreKept() {
        assertEquals(
            "Chapter 12.5",
            parse(
                "https://mangafire.to/read/example.abc12/en/chapter-12.5",
                pageTitle = null,
            )?.chapter,
        )
        assertEquals(
            "Chapter extra 1",
            parse(
                "https://mangafire.to/read/example.abc12/en/chapter-extra-1",
                pageTitle = null,
            )?.chapter,
        )
    }

    @Test
    fun nonChapterPagesAreIgnored() {
        assertNull(parse("https://mangafire.to/", null))
        assertNull(parse("https://mangafire.to/manga/example.abc12", null))
        assertNull(parse("https://mangafire.to/read/example.abc12/en", null))
    }

    @Test
    fun unsafeAndLookalikeUrlsAreIgnored() {
        assertNull(parse("http://mangafire.to/read/example.abc12/en/chapter-1", null))
        assertNull(parse("https://www.mangafire.to/read/example.abc12/en/chapter-1", null))
        assertNull(parse("https://mangafire.to.evil.test/read/example.abc12/en/chapter-1", null))
        assertNull(parse("https://user@mangafire.to/read/example.abc12/en/chapter-1", null))
        assertNull(parse("https://mangafire.to:8443/read/example.abc12/en/chapter-1", null))
    }

    @Test
    fun malformedChapterPartsAreIgnored() {
        assertNull(parse("not a url", null))
        assertNull(parse("https://mangafire.to/read/example.abc12/english/chapter-1", null))
        assertNull(parse("https://mangafire.to/read/example%2Fbad/en/chapter-1", null))
        assertNull(parse("https://mangafire.to/read/example.abc12/en/episode-1", null))
    }

    private fun parse(rawUrl: String, pageTitle: String?): LastReadChapter? =
        MangaFireChapterRoute.parse(rawUrl = rawUrl, pageTitle = pageTitle)
}
