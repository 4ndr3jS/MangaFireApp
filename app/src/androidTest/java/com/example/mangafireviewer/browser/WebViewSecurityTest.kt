package com.example.mangafireviewer.browser

import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WebViewSecurityTest {
    private lateinit var webView: WebView

    @Before
    fun createWebView() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            webView = WebView(ApplicationProvider.getApplicationContext())
            WebViewSecurity.configure(webView, enableDebugging = false)
        }
    }

    @After
    fun destroyWebView() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            webView.destroy()
        }
    }

    @Test
    fun requiredSiteCapabilitiesAreEnabled() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertTrue(webView.settings.javaScriptEnabled)
            assertTrue(webView.settings.domStorageEnabled)
            assertTrue(webView.settings.safeBrowsingEnabled)
            assertTrue(CookieManager.getInstance().acceptCookie())
            assertTrue(CookieManager.getInstance().acceptThirdPartyCookies(webView))
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun dangerousFileAndWindowCapabilitiesAreDisabled() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertFalse(webView.settings.allowFileAccess)
            assertFalse(webView.settings.allowContentAccess)
            assertFalse(webView.settings.allowFileAccessFromFileURLs)
            assertFalse(webView.settings.allowUniversalAccessFromFileURLs)
            assertFalse(webView.settings.javaScriptCanOpenWindowsAutomatically)
            assertTrue(webView.settings.supportMultipleWindows())
            assertTrue(
                webView.settings.mixedContentMode == WebSettings.MIXED_CONTENT_NEVER_ALLOW,
            )
        }
    }
}
