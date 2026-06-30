package com.yong.videoanalyzer.main

/**
 * UI State for Main ViewModel
 */
data class MainUiState(
    val videoFileName: String? = null,
    val framesPerSecond: Int = 1,
    val sceneChangeThreshold: Float = 0.5f,
)