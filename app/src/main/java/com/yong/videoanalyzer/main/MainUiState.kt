package com.yong.videoanalyzer.main

import com.yong.videoanalyzer.analyzer.SceneChange

/**
 * UI State for Main ViewModel
 */
data class MainUiState(
    val videoFileName: String? = null,
    val framesPerSecond: Int = 1,
    val sceneChangeThreshold: Float = 0.5f,
    val isAnalyzing: Boolean = false,
    val sceneChanges: List<SceneChange> = emptyList(),
)