package com.yong.videoanalyzer.analyzer

import android.graphics.Bitmap

/**
 * Scene Change Data
 *
 * @param timestampMs timestamp of scene change (ms)
 * @param similarity Cosine similarity to the previous sampled frame (0.0 ~ 1.0).
 *                   Lower values mean a bigger visual change.
 * @param frame The decoded video frame at this scene change.
 */
data class SceneChange(
    val timestampMs: Long,
    val similarity: Float,
    val frame: Bitmap,
)
