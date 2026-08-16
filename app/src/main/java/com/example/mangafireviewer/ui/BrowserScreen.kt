package com.example.mangafireviewer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mangafireviewer.R
import com.example.mangafireviewer.browser.BrowserFailure
import com.example.mangafireviewer.browser.LastReadChapter
import com.example.mangafireviewer.browser.MangaFireWebViewController

@Composable
fun BrowserScreen(controller: MangaFireWebViewController) {
    val uiState by controller.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(controller) {
        controller.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AndroidView(
                factory = { controller.webView },
                modifier = Modifier.fillMaxSize(),
            )

            if (uiState.progress in 1..99) {
                LinearProgressIndicator(
                    progress = { uiState.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                )
            }

            uiState.failure?.let { failure ->
                FailurePanel(
                    failure = failure,
                    onRetry = controller::reload,
                    onGoHome = controller::loadHome,
                    onOpenExternally = controller::openExternally,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (!uiState.isReaderPage && uiState.failure == null) {
                uiState.continueReading?.let { chapter ->
                    ContinueReadingCard(
                        chapter = chapter,
                        onOpen = controller::continueReading,
                        onForget = controller::forgetSavedChapter,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(
    chapter: LastReadChapter,
    onOpen: () -> Unit,
    onForget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onOpen,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.continue_reading),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = chapter.mangaTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = chapter.chapter,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onForget) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_forget_chapter),
                )
            }
        }
    }
}

@Composable
private fun FailurePanel(
    failure: BrowserFailure,
    onRetry: () -> Unit,
    onGoHome: () -> Unit,
    onOpenExternally: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = failure.title,
                style = MaterialTheme.typography.headlineSmall,
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
            )
            Text(
                text = failure.message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Text(stringResource(R.string.action_retry))
            }
            OutlinedButton(
                onClick = onGoHome,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.action_home))
            }
            OutlinedButton(
                onClick = onOpenExternally,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.action_open_external))
            }
        }
    }
}
