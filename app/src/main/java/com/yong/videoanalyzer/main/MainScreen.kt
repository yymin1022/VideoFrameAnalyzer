package com.yong.videoanalyzer.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val MIMETYPE_VIDEO = "video/*"

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

    val context = LocalContext.current

    // Launche system file picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.onVideoSelected(it, context) } }

    Column(
        modifier = modifier
            .padding(8.dp),
    ) {
        VideoSelect(
            modifier = Modifier,
            videoFileName = uiState.videoFileName,
            onBtnSelectVideo = { videoPickerLauncher.launch(arrayOf(MIMETYPE_VIDEO)) },
        )
    }
}

@Composable
private fun VideoSelect(
    modifier: Modifier = Modifier,
    videoFileName: String?,
    onBtnSelectVideo: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = onBtnSelectVideo,
        ) {
            Text("Select Video")
        }

        videoFileName?.let {
            Text(
                modifier = Modifier,
                text = "Selected [$it]",
            )
        }
    }
}
