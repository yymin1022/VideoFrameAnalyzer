package com.yong.videoanalyzer.main

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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