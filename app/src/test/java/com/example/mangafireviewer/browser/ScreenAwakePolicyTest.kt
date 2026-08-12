package com.example.mangafireviewer.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenAwakePolicyTest {
    @Test
    fun ordinaryPageOutsideFullscreenAllowsDisplaySleep() {
        assertFalse(
            keepAwake(
                isReaderPage = false,
                isFullscreen = false,
            ),
        )
    }

    @Test
    fun readerPageKeepsDisplayAwakeWithoutFullscreen() {
        assertTrue(
            keepAwake(
                isReaderPage = true,
                isFullscreen = false,
            ),
        )
    }

    @Test
    fun fullscreenKeepsDisplayAwakeOutsideReader() {
        assertTrue(
            keepAwake(
                isReaderPage = false,
                isFullscreen = true,
            ),
        )
    }

    @Test
    fun leavingFullscreenDoesNotReleaseActiveReaderRequest() {
        val whileFullscreen = keepAwake(
            isReaderPage = true,
            isFullscreen = true,
        )
        val afterFullscreen = keepAwake(
            isReaderPage = true,
            isFullscreen = false,
        )

        assertTrue(whileFullscreen)
        assertTrue(afterFullscreen)
    }

    @Test
    fun leavingReaderDoesNotReleaseActiveFullscreenRequest() {
        val whileReading = keepAwake(
            isReaderPage = true,
            isFullscreen = true,
        )
        val afterReaderNavigation = keepAwake(
            isReaderPage = false,
            isFullscreen = true,
        )

        assertTrue(whileReading)
        assertTrue(afterReaderNavigation)
    }

    private fun keepAwake(
        isReaderPage: Boolean,
        isFullscreen: Boolean,
    ): Boolean = ScreenAwakePolicy.shouldKeepScreenAwake(
        isReaderPage = isReaderPage,
        isFullscreen = isFullscreen,
    )
}
