package com.mapzone.mapzonealertview.core.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

object VoiceUtils {

    private const val TAG = "VoiceUtils"

    private var currentTrack: AudioTrack? = null
    private var currentPriority: Int = Int.MIN_VALUE

    @Synchronized
    fun playVoice(wav: ByteArray, trigger: Int, priority: Int) {
        if (currentTrack?.playState == AudioTrack.PLAYSTATE_PLAYING && priority < currentPriority) {
            return
        }
        stopCurrent()

        val header = parseWavHeader(wav) ?: run {
            Log.e(TAG, "Failed to parse WAV header, size=${wav.size}")
            return
        }
        val pcmData = wav.copyOfRange(header.dataOffset, wav.size)

        runCatching {
            val channelConfig = if (header.channels == 1) AudioFormat.CHANNEL_OUT_MONO
            else AudioFormat.CHANNEL_OUT_STEREO
            val encoding = if (header.bitsPerSample == 16) AudioFormat.ENCODING_PCM_16BIT
            else AudioFormat.ENCODING_PCM_8BIT

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(header.sampleRate)
                        .setEncoding(encoding)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(pcmData.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            val bytesPerFrame = (header.bitsPerSample / 8) * header.channels
            track.notificationMarkerPosition = pcmData.size / bytesPerFrame
            track.setPlaybackPositionUpdateListener(object :
                AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack) = releaseTrack(t)
                override fun onPeriodicNotification(t: AudioTrack) {}
            })

            track.write(pcmData, 0, pcmData.size)
            currentPriority = priority
            currentTrack = track
            track.play()
        }.onFailure { Log.e(TAG, "Failed to play voice", it) }
    }

    @Synchronized
    fun stop() = stopCurrent()

    @Synchronized
    private fun releaseTrack(track: AudioTrack) {
        runCatching { track.release() }
        if (currentTrack === track) {
            currentTrack = null
            currentPriority = Int.MIN_VALUE
        }
    }

    @Synchronized
    private fun stopCurrent() {
        runCatching {
            currentTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) stop()
                release()
            }
        }
        currentTrack = null
        currentPriority = Int.MIN_VALUE
    }

    private data class WavHeader(
        val sampleRate: Int,
        val channels: Int,
        val bitsPerSample: Int,
        val dataOffset: Int,
    )

    private fun parseWavHeader(wav: ByteArray): WavHeader? {
        if (wav.size < 44) return null
        val buf = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)

        val riff = ByteArray(4).also { buf.get(it) }
        if (String(riff) != "RIFF") return null
        buf.int
        val wave = ByteArray(4).also { buf.get(it) }
        if (String(wave) != "WAVE") return null

        var sampleRate = 0
        var channels = 0
        var bitsPerSample = 0
        var dataOffset = -1

        while (buf.remaining() >= 8) {
            val id = ByteArray(4).also { buf.get(it) }
            val chunkSize = buf.int

            when (String(id)) {
                "fmt " -> {
                    buf.short
                    channels = buf.short.toInt()
                    sampleRate = buf.int
                    buf.int
                    buf.short
                    bitsPerSample = buf.short.toInt()
                    val extra = chunkSize - 16
                    if (extra > 0) buf.position(buf.position() + minOf(extra, buf.remaining()))
                }

                "data" -> {
                    dataOffset = buf.position()
                    break
                }

                else -> buf.position(buf.position() + minOf(chunkSize, buf.remaining()))
            }
        }

        if (dataOffset < 0 || sampleRate == 0) return null
        return WavHeader(sampleRate, channels, bitsPerSample, dataOffset)
    }
}
