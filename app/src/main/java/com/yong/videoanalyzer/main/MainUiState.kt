package com.yong.videoanalyzer.main

import com.yong.videoanalyzer.analyzer.SceneChange

/**
 * UI State for Main ViewModel
 */
data class MainUiState(
    // Video file
    val videoFileName: String? = null,

    // Analyze parameter
    val framesPerSecond: Int = 5,
    val sceneChangeThreshold: Float = 0.5f,

    // Analyze result
    val elapsedTimeMs: Long = 0L,
    val isAnalyzing: Boolean = false,
    val sceneChanges: List<SceneChange> = emptyList(),
)