package com.yong.videoanalyzer.main

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * Main Screen UI
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = MainViewModel(),
) {
    // UI State
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier,
    ) {
        Text("Main Screen")
    }
}