package com.example.mangafireviewer.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.mangafireviewer.BuildConfig
import java.net.URI
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface FullscreenDelegate {
    fun showFullscreenView(view: View)
    fun hideFullscreenView()
    fun setReaderScreenAwake(enabled: Boolean)
}

class MangaFireWebViewController(
    private val activity: ComponentActivity,
    private val fullscreenDelegate: FullscreenDelegate,
) {
    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    private val chromeClient = SecureChromeClient()

    @SuppressLint("SetJavaScriptEnabled")
    val webView: WebView = WebView(activity).apply {
        setBackgroundColor(Color.BLACK)
        overScrollMode = View.OVER_SCROLL_NEVER
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        WebViewSecurity.configure(this, enableDebugging = BuildConfig.DEBUG)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(
                this,
                MANGAFIRE_CUSTOMIZATION_SCRIPT,
                setOf(MANGAFIRE_ORIGIN),
            )
        }
        webViewClient = SecureWebViewClient()
        webChromeClient = chromeClient
        setDownloadListener { _, _, _, _, _ ->
            notifyUser("Downloads are disabled in this app.")
        }
    }

    fun loadHome() {
        clearFailure()
        webView.loadUrl(MANGAFIRE_HOME_URL)
    }

    fun loadAppLink(rawUrl: String): Boolean {
        val trustedUrl = AppLinkPolicy.resolve(rawUrl)
        if (trustedUrl == null) {
            notifyUser("Only secure MangaFire links can open in this app.")
            return false
        }

        clearFailure()
        webView.loadUrl(trustedUrl)
        return true
    }

    fun goBack(): Boolean {
        if (!webView.canGoBack()) return false
        clearFailure()
        webView.goBack()
        return true
    }

    fun reload() {
        clearFailure()
        webView.reload()
    }

    fun openExternally() {
        val current = _uiState.value.currentUrl
        val decision = NavigationPolicy.classifyRaw(
            rawUrl = current,
            isMainFrame = true,
            hasUserGesture = true,
        )
        if (decision == NavigationDecision.AllowInternal) {
            openExternalUri(current.toUri())
        } else {
            openExternalUri(MANGAFIRE_HOME_URL.toUri())
        }
    }

    fun clearBrowsingData(onComplete: () -> Unit = {}) {
        WebStorage.getInstance().deleteAllData()
        webView.clearCache(true)
        webView.clearFormData()
        webView.clearHistory()

        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            activity.runOnUiThread {
                notifyUser("Browsing data cleared.")
                loadHome()
                onComplete()
            }
        }
    }

    fun exitFullscreen(): Boolean = chromeClient.exitFullscreen()

    fun isFullscreen(): Boolean = _uiState.value.isFullscreen

    fun saveState(outState: Bundle) {
        webView.saveState(outState)
    }

    fun restoreState(savedState: Bundle): Boolean =
        webView.restoreState(savedState) != null

    fun onResume() {
        webView.onResume()
        webView.resumeTimers()
    }

    fun onPause() {
        CookieManager.getInstance().flush()
        webView.onPause()
        webView.pauseTimers()
    }

    fun destroy() {
        updateReaderMode(false)
        chromeClient.exitFullscreen()
        webView.stopLoading()
        webView.removeAllViews()
        webView.destroy()
    }

    private fun clearFailure() {
        _uiState.update { it.copy(failure = null) }
    }

    private fun notifyUser(message: String) {
        _messages.tryEmit(message)
    }

    private fun updateReaderMode(isReaderPage: Boolean) {
        val changed = _uiState.value.isReaderPage != isReaderPage
        if (changed) {
            _uiState.update { it.copy(isReaderPage = isReaderPage) }
            fullscreenDelegate.setReaderScreenAwake(isReaderPage)
        }
    }

    private fun openExternalUri(uri: Uri) {
        val rawUrl = uri.toString()
        val isSafeHttps = try {
            val parsed = URI(rawUrl)
            parsed.scheme.equals("https", ignoreCase = true) &&
                parsed.host != null &&
                parsed.rawUserInfo == null &&
                (parsed.port == -1 || parsed.port == 443)
        } catch (_: Exception) {
            false
        }

        if (!isSafeHttps) {
            notifyUser("That link was blocked.")
            return
        }

        try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(activity, uri)
        } catch (_: RuntimeException) {
            notifyUser("No browser is available to open this link.")
        }
    }

    private fun applyPageCustomizations(view: WebView) {
        val currentUrl = view.url ?: return
        val isTrustedPage = NavigationPolicy.classifyRaw(
            rawUrl = currentUrl,
            isMainFrame = true,
            hasUserGesture = false,
        ) == NavigationDecision.AllowInternal

        if (isTrustedPage) {
            view.evaluateJavascript(MANGAFIRE_CUSTOMIZATION_SCRIPT, null)
        }
    }

    private fun updateHistoryState(view: WebView) {
        val trustedUrl = view.url?.takeIf { url ->
            NavigationPolicy.classifyRaw(
                rawUrl = url,
                isMainFrame = true,
                hasUserGesture = false,
            ) == NavigationDecision.AllowInternal
        }
        val isReaderPage = ReaderPagePolicy.isReaderPage(trustedUrl)

        _uiState.update { state ->
            state.copy(
                currentUrl = trustedUrl ?: state.currentUrl,
                canGoBack = view.canGoBack(),
                isReaderPage = isReaderPage,
            )
        }
        fullscreenDelegate.setReaderScreenAwake(isReaderPage)
    }

    private inner class SecureWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean {
            return when (
                NavigationPolicy.classify(
                    uri = request.url,
                    isMainFrame = request.isForMainFrame,
                    hasUserGesture = request.hasGesture(),
                )
            ) {
                NavigationDecision.AllowInternal -> false
                NavigationDecision.OpenExternal -> {
                    openExternalUri(request.url)
                    true
                }
                NavigationDecision.Block -> {
                    if (request.isForMainFrame) {
                        Log.w(
                            LOG_TAG,
                            "main_frame_navigation_blocked " +
                                "scheme=${request.url.scheme ?: "none"} " +
                                "host=${request.url.host ?: "none"}",
                        )
                    }
                    true
                }
            }
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            _uiState.update { it.copy(progress = 0, failure = null) }
            updateHistoryState(view)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            _uiState.update { it.copy(progress = 100) }
            updateHistoryState(view)
            applyPageCustomizations(view)
        }

        override fun doUpdateVisitedHistory(
            view: WebView,
            url: String?,
            isReload: Boolean,
        ) {
            updateHistoryState(view)
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            if (!request.isForMainFrame) return
            updateReaderMode(false)
            Log.w(LOG_TAG, "main_frame_load_failed category=network code=${error.errorCode}")
            _uiState.update {
                it.copy(
                    failure = BrowserFailure(
                        kind = BrowserFailureKind.NETWORK,
                        title = "Page unavailable",
                        message = "Check your connection and try loading the page again.",
                    ),
                )
            }
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse,
        ) {
            if (!request.isForMainFrame) return
            updateReaderMode(false)
            Log.w(
                LOG_TAG,
                "main_frame_load_failed category=http status=${errorResponse.statusCode}",
            )
            _uiState.update {
                it.copy(
                    failure = BrowserFailure(
                        kind = BrowserFailureKind.HTTP,
                        title = "Website error",
                        message = "MangaFire returned HTTP ${errorResponse.statusCode}. Try again later.",
                    ),
                )
            }
        }

        override fun onReceivedSslError(
            view: WebView,
            handler: SslErrorHandler,
            error: SslError,
        ) {
            handler.cancel()
            updateReaderMode(false)
            Log.w(LOG_TAG, "main_frame_load_failed category=tls")
            _uiState.update {
                it.copy(
                    failure = BrowserFailure(
                        kind = BrowserFailureKind.TLS,
                        title = "Secure connection failed",
                        message = "The site certificate could not be verified. The connection was stopped.",
                    ),
                )
            }
        }

        @RequiresApi(27)
        override fun onSafeBrowsingHit(
            view: WebView,
            request: WebResourceRequest,
            threatType: Int,
            callback: SafeBrowsingResponse,
        ) {
            callback.backToSafety(true)
            Log.w(LOG_TAG, "main_frame_load_failed category=safe_browsing")
            if (request.isForMainFrame) {
                updateReaderMode(false)
                _uiState.update {
                    it.copy(
                        failure = BrowserFailure(
                            kind = BrowserFailureKind.UNSAFE_CONTENT,
                            title = "Unsafe page blocked",
                            message = "Android Safe Browsing identified this page as a potential threat.",
                        ),
                    )
                }
            }
        }
    }

    private inner class SecureChromeClient : WebChromeClient() {
        private var customView: View? = null
        private var customViewCallback: CustomViewCallback? = null

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            val boundedProgress = newProgress.coerceIn(0, 100)
            val previousProgress = _uiState.value.progress
            val shouldPublish = boundedProgress == 0 ||
                boundedProgress == 100 ||
                kotlin.math.abs(boundedProgress - previousProgress) >= 5

            if (shouldPublish) {
                _uiState.update { it.copy(progress = boundedProgress) }
            }
        }

        override fun onReceivedTitle(view: WebView, title: String?) {
            val safeTitle = title
                ?.trim()
                ?.take(80)
                ?.takeIf(String::isNotEmpty)
                ?: "MangaFire"
            _uiState.update { it.copy(title = safeTitle) }
        }

        override fun onShowCustomView(
            view: View,
            callback: CustomViewCallback,
        ) {
            if (customView != null) {
                callback.onCustomViewHidden()
                return
            }

            customView = view
            customViewCallback = callback
            fullscreenDelegate.showFullscreenView(view)
            _uiState.update { it.copy(isFullscreen = true) }
        }

        override fun onHideCustomView() {
            exitFullscreen()
        }

        fun exitFullscreen(): Boolean {
            val callback = customViewCallback ?: return false
            fullscreenDelegate.hideFullscreenView()
            customView = null
            customViewCallback = null
            _uiState.update { it.copy(isFullscreen = false) }
            callback.onCustomViewHidden()
            return true
        }

        override fun onPermissionRequest(request: PermissionRequest) {
            request.deny()
            notifyUser("Camera and microphone permissions are disabled.")
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback,
        ) {
            callback.invoke(origin, false, false)
            notifyUser("Location access is disabled.")
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams,
        ): Boolean {
            filePathCallback.onReceiveValue(null)
            notifyUser("File uploads are disabled.")
            return true
        }

        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            // Intentionally consume console output so remote pages cannot place
            // watch URLs, tokens, cookies, or credentials in application logs.
            return true
        }
    }

    private companion object {
        const val LOG_TAG = "MangaFireWebView"

        val MANGAFIRE_CUSTOMIZATION_SCRIPT =
            """
            (() => {
              if (window.__mangaFireViewerInstalled) return;
              window.__mangaFireViewerInstalled = true;

              const updateViewportHeight = () => {
                const candidates = [
                  window.visualViewport?.height,
                  window.innerHeight,
                  document.documentElement?.clientHeight,
                ].filter((height) => Number.isFinite(height) && height > 0);
                if (!candidates.length) return;
                const height = Math.round(Math.min(...candidates));
                document.documentElement.style.setProperty(
                  '--mangafire-app-height',
                  `${'$'}{height}px`,
                );
              };

              const installStyle = () => {
                const root = document.head || document.documentElement;
                if (!root || document.getElementById('mangafire-viewer-style')) return;
                const style = document.createElement('style');
                style.id = 'mangafire-viewer-style';
                style.textContent = `
                  html { overscroll-behavior: none !important; }
                  body { -webkit-tap-highlight-color: transparent; }
                  .modal__card {
                    box-sizing: border-box !important;
                    max-height: calc(var(--mangafire-app-height, 720px) - 32px) !important;
                  }
                  .reader {
                    --page-fit-height: var(--mangafire-app-height, 720px) !important;
                    min-height: var(--mangafire-app-height, 720px) !important;
                  }
                  .reader--state {
                    min-height: var(--mangafire-app-height, 720px) !important;
                  }
                  .reader__paged,
                  .reader-swiper,
                  .reader-swiper .swiper-zoom-container,
                  .reader-swiper__spread {
                    height: var(--mangafire-app-height, 720px) !important;
                  }
                  .reader-modal__panel {
                    max-height: calc(var(--mangafire-app-height, 720px) - 32px) !important;
                  }
                  .reader__fullscreen {
                    display: flex !important;
                  }
                  img[alt^='page '] { image-rendering: auto; }
                `;
                root.appendChild(style);
              };

              updateViewportHeight();
              installStyle();
              document.addEventListener('DOMContentLoaded', installStyle, { once: true });
              document.addEventListener('DOMContentLoaded', updateViewportHeight, { once: true });
              window.addEventListener('resize', updateViewportHeight, { passive: true });
              window.visualViewport?.addEventListener(
                'resize',
                updateViewportHeight,
                { passive: true },
              );
            })();
            """.trimIndent()

        const val MANGAFIRE_ORIGIN = "https://mangafire.to"
    }
}
