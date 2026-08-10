package com.example.mangafireviewer

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isNotEmpty
import com.example.mangafireviewer.browser.FullscreenDelegate
import com.example.mangafireviewer.browser.MangaFireWebViewController
import com.example.mangafireviewer.browser.AppLinkPolicy
import com.example.mangafireviewer.ui.BrowserScreen
import com.example.mangafireviewer.ui.MangaFireViewerTheme

class MainActivity : ComponentActivity(), FullscreenDelegate {
    private lateinit var browserController: MangaFireWebViewController
    private lateinit var composeView: ComposeView
    private lateinit var fullscreenContainer: FrameLayout
    private var lastHandledAppLink: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        composeView = ComposeView(this).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
            )
        }
        fullscreenContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
        }

        root.addView(
            composeView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        root.addView(
            fullscreenContainer,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        setContentView(root)

        browserController = MangaFireWebViewController(
            activity = this,
            fullscreenDelegate = this,
        )

        composeView.setContent {
            MangaFireViewerTheme {
                BrowserScreen(browserController)
            }
        }

        lastHandledAppLink = savedInstanceState?.getString(KEY_LAST_HANDLED_APP_LINK)
        val restored = savedInstanceState
            ?.getBundle(KEY_WEBVIEW_STATE)
            ?.let(browserController::restoreState)
            ?: false

        val appLinkHandled = handleAppLink(
            incomingIntent = intent,
            notifyIfRejected = false,
            ignorePreviouslyHandled = restored,
        )
        if (!restored && !appLinkHandled) {
            browserController.loadHome()
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        browserController.exitFullscreen() -> Unit
                        browserController.goBack() -> Unit
                        else -> {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            },
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val webViewState = Bundle()
        browserController.saveState(webViewState)
        outState.putBundle(KEY_WEBVIEW_STATE, webViewState)
        lastHandledAppLink?.let { appLink ->
            outState.putString(KEY_LAST_HANDLED_APP_LINK, appLink)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAppLink(
            incomingIntent = intent,
            notifyIfRejected = true,
            ignorePreviouslyHandled = false,
        )
    }

    override fun onResume() {
        super.onResume()
        if (::browserController.isInitialized) {
            browserController.onResume()
        }
    }

    override fun onPause() {
        if (::browserController.isInitialized) {
            browserController.onPause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (::browserController.isInitialized) {
            browserController.destroy()
        }
        super.onDestroy()
    }

    override fun showFullscreenView(view: View) {
        if (fullscreenContainer.isNotEmpty()) return

        fullscreenContainer.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        composeView.visibility = View.GONE
        fullscreenContainer.visibility = View.VISIBLE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        hideSystemBars()
    }

    override fun hideFullscreenView() {
        fullscreenContainer.removeAllViews()
        fullscreenContainer.visibility = View.GONE
        composeView.visibility = View.VISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        showSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::browserController.isInitialized && browserController.isFullscreen()) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun showSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    private fun handleAppLink(
        incomingIntent: Intent?,
        notifyIfRejected: Boolean,
        ignorePreviouslyHandled: Boolean,
    ): Boolean {
        if (incomingIntent?.action != Intent.ACTION_VIEW) return false

        val rawUrl = incomingIntent.dataString
        val trustedUrl = AppLinkPolicy.resolve(rawUrl)
        if (trustedUrl == null) {
            if (notifyIfRejected && rawUrl != null) {
                browserController.loadAppLink(rawUrl)
            }
            return false
        }

        // A configuration change recreates the Activity with its original
        // Intent. The restored WebView may since have moved to another chapter,
        // so do not send it back to that original link a second time.
        if (ignorePreviouslyHandled && trustedUrl == lastHandledAppLink) return false

        val loaded = browserController.loadAppLink(trustedUrl)
        if (loaded) {
            lastHandledAppLink = trustedUrl
        }
        return loaded
    }

    private companion object {
        const val KEY_WEBVIEW_STATE = "mangafire_webview_state"
        const val KEY_LAST_HANDLED_APP_LINK = "mangafire_last_handled_app_link"
    }
}
