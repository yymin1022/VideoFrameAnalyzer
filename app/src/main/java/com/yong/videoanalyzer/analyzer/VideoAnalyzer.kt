package com.yong.videoanalyzer.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.Embedding
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder.ImageEmbedderOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Detects scene change points in a video using MediaPipe's image embedding.
 *
 * Frames are pulled from [VideoFrameDecoder] (sequential decode) at a fixed rate,
 * each frame is converted to an embedding vector, and consecutive frames are
 * compared with cosine similarity. A frame is reported as a [SceneChange] when its
 * visual distance from the previous frame (`1 - similarity`) reaches the threshold.
 */
class VideoAnalyzer(
    private val context: Context,
    private val modelAssetPath: String = DEFAULT_MODEL_ASSET_PATH,
) {
    companion object {
        private const val TAG = "VideoAnalyzer"

        // Model file path in asset dir
        private const val DEFAULT_MODEL_ASSET_PATH = "image_embedder.tflite"

        // Maximum size of each frame
        private const val FRAME_MAX_SIZE = 320
    }

    private val frameDecoder = VideoFrameDecoder(context)

    /**
     * Analyzes [videoUri] and returns the detected scene changes in time order.
     *
     * @param videoUri Source video uri info
     * @param fps frame count per second (1 ~ 30)
     * @param threshold Visual-distance threshold (0.0 ~ 1.0)
     *        A higher value requires a bigger change between frames
     */
    suspend fun analyze(
        videoUri: Uri,
        fps: Int,
        threshold: Float,
    ): List<SceneChange> = withContext(Dispatchers.Default) {
        var embedder: ImageEmbedder? = null

        try {
            val activeEmbedder = createEmbedder()
            embedder = activeEmbedder

            // Sample one frame per this much presentation time
            val intervalUs = 1_000_000L / fps
            // Captured so the decode callback can honor coroutine cancellation
            val analyzeContext = coroutineContext

            val sceneChanges = mutableListOf<SceneChange>()
            var previousEmbedding: Embedding? = null

            frameDecoder.decode(videoUri, intervalUs, FRAME_MAX_SIZE) { timestampMs, frame ->
                analyzeContext.ensureActive()

                // Generate embedding of current frame
                val embedding = activeEmbedder.embedFrame(frame, timestampMs)

                // Whether this frame is kept as a scene change result
                var isSceneChange = false
                if (previousEmbedding == null) {
                    // First sampled frame is the start of the first scene
                    sceneChanges += SceneChange(timestampMs, similarity = 1.0f, frame)
                    isSceneChange = true
                } else {
                    // Compare previous / current embedding
                    val similarity = ImageEmbedder.cosineSimilarity(
                        previousEmbedding,
                        embedding,
                    ).toFloat()

                    // Check similarity cosine value with threshold
                    if (1.0f - similarity >= threshold) {
                        sceneChanges += SceneChange(timestampMs, similarity, frame)
                        isSceneChange = true
                    }
                }
                previousEmbedding = embedding

                // Keep the bitmap of scene changes for the UI, release the rest
                if (!isSceneChange) frame.recycle()
            }

            sceneChanges
        } catch (e: CancellationException) {
            // Keep coroutine cancellation propagating
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze video", e)
            emptyList()
        } finally {
            // Release instances
            embedder?.close()
        }
    }

    /**
     * Create new [ImageEmbedder] instance with options
     * - asset path: [modelAssetPath]
     * - run mode: [RunningMode.VIDEO]
     */
    private fun createEmbedder(): ImageEmbedder {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(modelAssetPath)
            .build()
        val options = ImageEmbedderOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.VIDEO)
            .setL2Normalize(true)
            .build()
        return ImageEmbedder.createFromOptions(context, options)
    }

    /**
     * Computes the [Embedding] of a single [bitmap] sampled at [timestampMs].
     */
    private fun ImageEmbedder.embedFrame(bitmap: Bitmap, timestampMs: Long): Embedding {
        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = embedForVideo(mpImage, timestampMs)
        return result.embeddingResult().embeddings().first()
    }
}
