package com.example.mangafireviewer.browser

/** Combines independent reasons that require Android to keep the display on. */
object ScreenAwakePolicy {
    fun shouldKeepScreenAwake(
        isReaderPage: Boolean,
        isFullscreen: Boolean,
    ): Boolean = isReaderPage || isFullscreen
}
