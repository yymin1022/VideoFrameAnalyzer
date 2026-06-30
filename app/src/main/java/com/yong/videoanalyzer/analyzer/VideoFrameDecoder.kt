package com.yong.videoanalyzer.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri

/**
 * Video Frame Decoder
 * - Decode a video with [MediaCodec]
 */
internal class VideoFrameDecoder(
    private val context: Context,
) {
    companion object {
        // Timeout when buffering from codec
        private const val DEQUEUE_TIMEOUT_US = 10_000L
    }

    /**
     * Decode video from [videoUri] and invoke [onFrame] for each sampled frame,
     *
     * @param intervalUs interval between each frame. other frames are skipped.
     * @param maxSize each frame is resized within [maxSize].
     * @param onFrame callback when a frame is processed.
     */
    fun decode(
        videoUri: Uri,
        intervalUs: Long,
        maxSize: Int,
        onFrame: (timestampMs: Long, frame: Bitmap) -> Unit,
    ) {
        // Codec / Extractor Instance
        var codec: MediaCodec? = null
        val extractor = MediaExtractor()

        try {
            // Open video file uri
            context.contentResolver.openFileDescriptor(videoUri, "r")?.use { pfd ->
                // Init extractor
                extractor.setDataSource(pfd.fileDescriptor)

                // Get video info with extractor
                val trackIndex = extractor.firstVideoTrack() ?: return
                extractor.selectTrack(trackIndex)

                val format = extractor.getTrackFormat(trackIndex)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: return
                format.setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible,
                )

                // Init codec
                codec = MediaCodec.createDecoderByType(mime).apply {
                    configure(format, null, null, 0)
                    start()
                }

                // Start decoding flow
                codec.decodeInternal(extractor, intervalUs, maxSize, onFrame)
            }
        } finally {
            // Cleanup
            codec?.run {
                runCatching { stop() }
                release()
            }
            extractor.release()
        }
    }

    /**
     * Find first available video track from [MediaExtractor] and return index
     */
    private fun MediaExtractor.firstVideoTrack(): Int? {
        for (i in 0 until trackCount) {
            val mime = getTrackFormat(i).getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("video/") == true) return i
        }
        return null
    }

    /**
     * Get video data with [extractor], and invoke [onFrame] with decoded frame
     *
     * @param intervalUs interval between each frame. other frames are skipped.
     * @param maxSize each frame is resized within [maxSize].
     */
    private fun MediaCodec.decodeInternal(
        extractor: MediaExtractor,
        intervalUs: Long,
        maxSize: Int,
        onFrame: (timestampMs: Long, frame: Bitmap) -> Unit,
    ) {
        val bufferInfo = MediaCodec.BufferInfo()
        var nextSampleUs = 0L

        var isInputEOS = false
        var isOutputEOS = false
        while (!isOutputEOS) {
            // Feed decoder input if not EOS
            if (!isInputEOS) {
                val inputBufIdx = dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                if (inputBufIdx >= 0) {
                    val inputBuffer = getInputBuffer(inputBufIdx)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)

                    if (sampleSize < 0) {
                        // Input EOS Occurred
                        queueInputBuffer(inputBufIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isInputEOS = true
                    } else {
                        queueInputBuffer(inputBufIdx, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            // Consume decoder output
            val outputBufIdx = dequeueOutputBuffer(bufferInfo, DEQUEUE_TIMEOUT_US)
            if (outputBufIdx >= 0) {
                // Output EOS Occurred
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    isOutputEOS = true
                }

                val presentationUs = bufferInfo.presentationTimeUs
                val keepFrame = bufferInfo.size > 0 && presentationUs >= nextSampleUs

                // Keep frame only when sample time is target time
                // - Target time is determined with intervalUs
                var frame: Bitmap? = null
                if (keepFrame) {
                    getOutputImage(outputBufIdx)?.use { image ->
                        frame = image.toScaledBitmap(maxSize)
                    }

                    // Get next sample time with intervalUs
                    do {
                        nextSampleUs += intervalUs
                    } while (nextSampleUs <= presentationUs)
                }

                // Release the buffer
                releaseOutputBuffer(outputBufIdx, false)

                // On frame callback
                frame?.let { onFrame(presentationUs / 1000L, it) }
            }
        }
    }
}
