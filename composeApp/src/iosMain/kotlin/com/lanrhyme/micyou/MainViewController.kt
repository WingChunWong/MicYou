package com.lanrhyme.micyou

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.lanrhyme.micyou.theme.ThemeMode

/**
 * iOS app entry point.
 *
 * Creates a ComposeUIViewController that hosts the Compose Multiplatform UI.
 * This is called from Swift/Obj-C code in the Xcode project.
 */
fun MainViewController() = ComposeUIViewController {
    val viewModel = remember { MainViewModel() }
    App(viewModel = viewModel)
}
