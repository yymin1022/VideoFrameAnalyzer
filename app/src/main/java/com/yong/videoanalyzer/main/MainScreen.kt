package com.yong.videoanalyzer.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yong.videoanalyzer.analyzer.SceneChange
import kotlin.math.roundToInt

private const val MIMETYPE_VIDEO = "video/*"

private const val FPS_MIN = 1
private const val FPS_MAX = 30

private const val THRESHOLD_MIN = 0.0f
private const val THRESHOLD_MAX = 1.0f

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
            .fillMaxSize()
            .padding(8.dp),
    ) {
        VideoSelect(
            modifier = Modifier,
            videoFileName = uiState.videoFileName,
            onBtnSelectVideo = { videoPickerLauncher.launch(arrayOf(MIMETYPE_VIDEO)) },
        )

        FpsSlider(
            modifier = Modifier
                .padding(top = 16.dp),
            fps = uiState.framesPerSecond,
            onFpsChanged = viewModel::onFramesPerSecondChanged,
        )

        ThresholdSlider(
            modifier = Modifier
                .padding(top = 16.dp),
            threshold = uiState.sceneChangeThreshold,
            onThresholdChanged = viewModel::onSceneChangeThresholdChanged,
        )

        AnalyzeButton(
            modifier = Modifier
                .padding(top = 16.dp),
            isAnalyzing = uiState.isAnalyzing,
            isVideoSelected = uiState.videoFileName != null,
            onBtnAnalyze = { viewModel.onBtnAnalyze(context) },
        )

        if (!uiState.isAnalyzing) {
            SceneChangeResult(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 16.dp),
                sceneChanges = uiState.sceneChanges,
            )
        }
    }
}

@Composable
private fun AnalyzeButton(
    modifier: Modifier = Modifier,
    isAnalyzing: Boolean,
    isVideoSelected: Boolean,
    onBtnAnalyze: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = onBtnAnalyze,
            enabled = isVideoSelected && !isAnalyzing,
        ) {
            Text("Start Analyze")
        }

        if (isAnalyzing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun SceneChangeResult(
    modifier: Modifier = Modifier,
    sceneChanges: List<SceneChange>,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        item {
            Text(
                modifier = Modifier,
                text = "Detected ${sceneChanges.size} scene changes",
            )
        }

        items(sceneChanges) { sceneChange ->
            SceneChangeItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                sceneChange = sceneChange,
            )
        }
    }
}

@Composable
private fun SceneChangeItem(
    modifier: Modifier = Modifier,
    sceneChange: SceneChange,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            modifier = Modifier
                .size(96.dp),
            bitmap = sceneChange.frame.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )

        Text(
            modifier = Modifier
                .padding(start = 8.dp),
            text = "${sceneChange.timestampMs} ms (similarity ${"%.2f".format(sceneChange.similarity)})",
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

@Composable
private fun FpsSlider(
    modifier: Modifier = Modifier,
    fps: Int,
    onFpsChanged: (Int) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Text(
            modifier = Modifier,
            text = "Frames per second: $fps",
        )

        Slider(
            modifier = Modifier,
            value = fps.toFloat(),
            onValueChange = { onFpsChanged(it.roundToInt()) },
            valueRange = FPS_MIN.toFloat()..FPS_MAX.toFloat(),
            steps = FPS_MAX - FPS_MIN - 1,
        )
    }
}

@Composable
private fun ThresholdSlider(
    modifier: Modifier = Modifier,
    threshold: Float,
    onThresholdChanged: (Float) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Text(
            modifier = Modifier,
            text = "Scene change threshold: ${"%.2f".format(threshold)}",
        )

        Slider(
            modifier = Modifier,
            value = threshold,
            onValueChange = onThresholdChanged,
            valueRange = THRESHOLD_MIN..THRESHOLD_MAX,
        )
    }
}
