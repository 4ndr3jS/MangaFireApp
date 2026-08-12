package com.example.mangafireviewer.browser

const val MANGAFIRE_HOME_URL = "https://mangafire.to/"

data class BrowserUiState(
    val currentUrl: String = MANGAFIRE_HOME_URL,
    val title: String = "MangaFire",
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val isReaderPage: Boolean = false,
    val isFullscreen: Boolean = false,
    val failure: BrowserFailure? = null,
)

enum class BrowserFailureKind {
    NETWORK,
    HTTP,
    TLS,
    UNSAFE_CONTENT,
}

data class BrowserFailure(
    val kind: BrowserFailureKind,
    val title: String,
    val message: String,
)
