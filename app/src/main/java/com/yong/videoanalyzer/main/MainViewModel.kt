package com.yong.videoanalyzer.main

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yong.videoanalyzer.analyzer.VideoAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Main ViewModel
 */
class MainViewModel: ViewModel() {
    // UI State
    private val _uiState: MutableStateFlow<MainUiState> = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Selected video source, kept internally for later frame analysis.
    private var videoUri: Uri? = null

    /**
     * Update selected [uri] related data
     */
    fun onVideoSelected(uri: Uri, context: Context) {
        val videoFileName = context.queryDisplayName(uri)
        videoUri = uri
        _uiState.update { it.copy(videoFileName = videoFileName) }
    }

    /**
     * Update fps value (1 ~ 30)
     */
    fun onFramesPerSecondChanged(framesPerSecond: Int) {
        _uiState.update { it.copy(framesPerSecond = framesPerSecond) }
    }

    /**
     * Update scene change threshold value (0.0 ~ 1.0)
     */
    fun onSceneChangeThresholdChanged(sceneChangeThreshold: Float) {
        _uiState.update { it.copy(sceneChangeThreshold = sceneChangeThreshold) }
    }

    /**
     * Analyze the selected video and collect scene changes.
     * Does nothing when no video is selected or analysis is already running.
     */
    fun onBtnAnalyze(context: Context) {
        val uri = videoUri ?: return
        val currentState = _uiState.value
        if (currentState.isAnalyzing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, sceneChanges = emptyList()) }

            val startMs = SystemClock.elapsedRealtime()
            val sceneChanges = VideoAnalyzer(context).analyze(
                videoUri = uri,
                fps = currentState.framesPerSecond,
                threshold = currentState.sceneChangeThreshold,
            )
            val elapsedTimeMs = SystemClock.elapsedRealtime() - startMs

            _uiState.update {
                it.copy(
                    isAnalyzing = false,
                    sceneChanges = sceneChanges,
                    elapsedTimeMs = elapsedTimeMs,
                )
            }
        }
    }

    /**
     * Resolves the human-readable display name of [uri].
     */
    private fun Context.queryDisplayName(uri: Uri): String? {
        return contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if(nameIndex != -1 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else null
        }
    }
}